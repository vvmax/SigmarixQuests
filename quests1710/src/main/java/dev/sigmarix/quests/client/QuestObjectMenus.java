package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.gui.*;import net.minecraft.client.Minecraft;import net.minecraft.util.StatCollector;
/** Shared object context actions, including quick boolean/enum edits and scoped progress changes. */
final class QuestObjectMenus {
    private static String tr(String key){return StatCollector.translateToLocal(key);}
    static void open(final GuiScreen parent,final String questId,final String collection,final String id,int x,int y){
        final QuestObjectConfigScreen config=QuestObjectConfigScreen.prepare(parent,questId,collection,id);Minecraft mc=Minecraft.getMinecraft();config.setWorldAndResolution(mc,parent.width,parent.height);List<QuestContextScreen.Entry> menu=new ArrayList<QuestContextScreen.Entry>();
        for(final QuestConfigScreen.Entry entry:config.quickEntries()){
            boolean bool=entry.value.get().equals("true") || entry.value.get().equals("false");QuestContextScreen.Entry row=new QuestContextScreen.Entry(entry.label,bool?"accept":"",()->entry.edit.accept(0,config)){
                @Override void onClick(int button){entry.edit.accept(button,config);}
                @Override void drawIcon(int ix,int iy,int size,int color){if(entry.value.get().equals("true") || entry.value.get().equals("false"))QuestClassicUi.libraryIcon(entry.value.get().equals("true")?"accept":"accept_gray",ix,iy,size,color);}
            };row.tooltip.add(entry.value.get());menu.add(row);
        }
        if(!menu.isEmpty())menu.add(new QuestContextScreen.Entry("","",null));
        menu.add(new QuestContextScreen.Entry(tr("selectServer.edit"),"settings",()->mc.displayGuiScreen(QuestObjectConfigScreen.prepare(parent,questId,collection,id))));
        if(!questId.isEmpty())menu.add(new QuestContextScreen.Entry(tr("selectServer.delete"),"remove",()->{Runnable remove=()->remove(config.draft,questId,collection,id);if(GuiScreen.isShiftKeyDown())remove.run();else mc.displayGuiScreen(new GuiYesNo((yes,n)->{mc.displayGuiScreen(parent);if(yes)remove.run();},StatCollector.translateToLocalFormatted("delete_item",title(config.object)),"",0));}));
        final String reference=collection.equals("tasks")?"task:"+questId+"/"+id:collection.equals("rewards")?"reward:"+questId+"/"+id:questId.isEmpty()?"book:":questId;
        menu.add(new QuestContextScreen.Entry(tr("ftbquests.gui.reset_progress"),"refresh",()->progress(parent,reference,false)));
        if(!collection.equals("rewards"))menu.add(new QuestContextScreen.Entry(tr("ftbquests.gui.complete_instantly"),"check",()->progress(parent,reference,true)));
        QuestContextScreen.Entry copy=new QuestContextScreen.Entry(tr("ftbquests.gui.copy_id"),"info",()->GuiScreen.setClipboardString(reference));copy.tooltip.add(reference);menu.add(copy);
        mc.displayGuiScreen(new QuestContextScreen(parent,x,y,menu));
    }
    private static String title(Object object){if(object instanceof QuestCore.Quest)return ((QuestCore.Quest)object).title;if(object instanceof QuestCore.Chapter)return ((QuestCore.Chapter)object).title;if(object instanceof QuestCore.Task)return QuestTaskLabels.title((QuestCore.Task)object);if(object instanceof QuestCore.Reward)return QuestTaskLabels.reward((QuestCore.Reward)object);return "Quest Book";}
    private static void progress(GuiScreen parent,String reference,boolean complete){Minecraft mc=Minecraft.getMinecraft();final boolean dependencies=GuiScreen.isShiftKeyDown();mc.displayGuiScreen(new GuiYesNo((yes,id)->{mc.displayGuiScreen(parent);if(yes){QuestNetwork.Action action=new QuestNetwork.Action();action.action=complete?"progress_complete":"progress_reset";action.id=reference;action.text=dependencies?"dependencies":"";ClientProxy.send(action);}},tr(complete?"ftbquests.gui.complete_instantly_q":"ftbquests.gui.reset_progress_q"),"",0));}
    private static void remove(QuestCore.Book draft,String questId,String collection,String id){
        if(questId.startsWith("chapter:")){Set<String> chapters=new HashSet<String>();chapters.add(questId.substring(8));boolean changed;do{changed=false;for(QuestCore.Chapter chapter:draft.chapters)if(chapters.contains(chapter.parent) && chapters.add(chapter.id))changed=true;}while(changed);draft.chapters.removeIf(c->chapters.contains(c.id));draft.quests.removeIf(q->chapters.contains(q.chapter));}
        else if(collection.isEmpty())draft.quests.removeIf(q->q.id.equals(questId));else for(QuestCore.Quest q:draft.quests)if(q.id.equals(questId)){if(collection.equals("tasks"))q.tasks.removeIf(t->t.id.equals(id));else q.rewards.removeIf(r->r.id.equals(id));}
        Map<String,QuestCore.Quest> quests=new LinkedHashMap<String,QuestCore.Quest>();for(QuestCore.Quest q:draft.quests)quests.put(q.id,q);for(QuestCore.Quest q:draft.quests){Iterator<String> dependencies=q.dependencies.iterator();while(dependencies.hasNext())try{QuestDependencies.owners(draft,quests,dependencies.next());}catch(IllegalArgumentException missing){dependencies.remove();}q.minDependencies=Math.min(q.minDependencies,q.dependencies.size());}
        QuestObjectConfigScreen.send(draft,ClientProxy.snapshot.revision);
    }
    static void chapter(GuiScreen parent,String group){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="chapter_"+Long.toHexString(System.nanoTime());chapter.title="";chapter.parent=group;draft.chapters.add(chapter);long revision=ClientProxy.snapshot.revision;Minecraft.getMinecraft().displayGuiScreen(new QuestObjectConfigScreen(parent,draft,chapter,"chapter",()->QuestObjectConfigScreen.send(draft,revision)));}
    static void quest(GuiScreen parent,String chapter,double x,double y){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest_"+Long.toHexString(System.nanoTime());quest.title="";quest.chapter=chapter;quest.rewardMode="inherit";quest.x=x;quest.y=y;draft.quests.add(quest);long revision=ClientProxy.snapshot.revision;Minecraft.getMinecraft().displayGuiScreen(new QuestObjectConfigScreen(parent,draft,quest,"quest",()->QuestObjectConfigScreen.send(draft,revision)));}
}
