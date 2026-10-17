package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.gui.GuiScreen;import net.minecraft.util.StatCollector;
/** Original task/reward type menus open the corresponding typed creation form. */
final class QuestCreationMenus {
    private static String nativeType(String type){return type.equals("check")?"checkmark":type.equals("energy")?"forge_energy":type.equals("achievement")?"advancement":type;}
    private static List<String> ordered(Set<String> types,String...first){List<String> result=new ArrayList<String>();for(String type:first)if(types.contains(type))result.add(type);List<String> rest=new ArrayList<String>(types);rest.removeAll(result);Collections.sort(rest);result.addAll(rest);return result;}
    private static List<String> taskTypes(){
        List<String> types=new ArrayList<String>(Arrays.asList("item","fluid","energy","custom","xp","dimension","stat","kill","location","check","achievement","observation","interaction","biome"));
        if(cpw.mods.fml.common.Loader.isModLoaded("IC2"))types.add("ic2_energy");if(cpw.mods.fml.common.Loader.isModLoaded("Botania"))types.add("botania_mana");
        if(GuiScreen.isCtrlKeyDown())return ordered(QuestCore.TASK_TYPES,types.toArray(new String[0]));return types;
    }
    private static List<String> rewardTypes(){List<String> types=Arrays.asList("item","choice","random","loot","command","custom","xp","xp_levels","achievement","toast");return GuiScreen.isCtrlKeyDown()?ordered(QuestCore.REWARD_TYPES,types.toArray(new String[0])):types;}
    private static void typeIcon(String type,boolean reward,QuestCore.Task task,QuestCore.Reward prize,int x,int y,int size){
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();String library=type.equals("custom")?"color_hsb":reward && type.equals("choice")?"color_rgb":reward && type.equals("random")?"dice":reward && type.equals("loot")?"money_bag":!reward && type.equals("observation")?"art":!reward && type.equals("interaction")?"bell":"";
        if(!library.isEmpty())QuestClassicUi.libraryIcon(library,x,y,size,0xFFFFFFFF);else if(!reward && type.equals("kill"))QuestClassicUi.item(mc,"minecraft:diamond_sword",0,"",x,y,size);else if(type.equals("item"))QuestClassicUi.item(mc,"minecraft:diamond",0,"",x,y,size);else if(type.equals("achievement"))QuestClassicUi.item(mc,"minecraft:wheat",0,"",x,y,size);else if(reward)QuestClassicUi.reward(mc,prize,x,y,size);else QuestClassicUi.task(mc,task,false,x,y,size);
    }
    static void task(final GuiScreen parent,QuestCore.Quest selected,int x,int y){
        final QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);final QuestCore.Quest quest=draft.quests.stream().filter(q->q.id.equals(selected.id)).findFirst().get();final long revision=ClientProxy.snapshot.revision;List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();
        taskMenu(parent,draft,quest,revision,x,y,false);
    }
    static void at(GuiScreen parent,String chapter,double qx,double qy,int x,int y){if(chapter.isEmpty())return;QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest_"+Long.toHexString(System.nanoTime());quest.title="";quest.chapter=chapter;quest.rewardMode="inherit";quest.x=qx;quest.y=qy;draft.quests.add(quest);taskMenu(parent,draft,quest,ClientProxy.snapshot.revision,x,y,true);}
    private static void taskMenu(final GuiScreen parent,final QuestCore.Book draft,final QuestCore.Quest quest,final long revision,int x,int y,boolean creatingQuest){List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();
        for(final String type:taskTypes()){
            final QuestCore.Task prototype=task(type);String key="ftbquests.task.ftbquests."+nativeType(type);String title=StatCollector.canTranslate(key)?StatCollector.translateToLocal(key):type;
            entries.add(new QuestContextScreen.Entry(title,"task",()->{final QuestCore.Task task=task(type);quest.tasks.add(task);net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new QuestObjectConfigScreen(parent,draft,task,"task",()->QuestObjectConfigScreen.send(draft,revision)));}){@Override void drawIcon(int ix,int iy,int size,int color){typeIcon(type,false,prototype,null,ix,iy,size);}});
        }
        if(creatingQuest)entries.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("ftbquests.chapter.image"),"art",()->QuestObjectMenus.addImage(quest.chapter,quest.x,quest.y)));
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new QuestContextScreen(parent,x,y,entries));
    }
    private static QuestCore.Task task(String type){QuestCore.Task task=new QuestCore.Task();task.id="t"+Long.toHexString(System.nanoTime());task.type=type;
        if(type.equals("check"))task.manual=true;if(type.equals("item")){task.consumeMode="inherit";task.craftingMode="inherit";}
        if(type.equals("kill")){task.target="Zombie";task.count=100;}if(type.equals("biome"))task.target="";if(type.equals("dimension"))task.dimension=-1;if(Arrays.asList("energy","ic2_energy","botania_mana").contains(type))task.count=1000;if(type.equals("fluid")){task.target="water";task.count=1000;task.consume=true;}
        if(type.equals("location"))task.box=true;
        if(type.equals("xp")){task.levels=true;task.consume=true;}if(type.equals("observation") || type.equals("interaction")){task.target="";task.matchType="block_id";task.ignoreDamage=true;}
        if(type.equals("stat"))task.target="stat.mobKills";if(type.equals("achievement"))task.target="";if(type.equals("gamestage") || type.equals("skill"))task.target="new_stage";
        return task;
    }
    static void reward(final GuiScreen parent,final QuestCore.Book draft,final List<QuestCore.Reward> destination,final Runnable accepted){reward(parent,draft,destination,accepted,false,20,20);}
    static void questReward(final GuiScreen parent,QuestCore.Quest selected,int x,int y){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.Quest quest=draft.quests.stream().filter(q->q.id.equals(selected.id)).findFirst().get();long revision=ClientProxy.snapshot.revision;reward(parent,draft,quest.rewards,()->QuestObjectConfigScreen.send(draft,revision),true,x,y);}
    static void selectedRewards(GuiScreen parent,Set<String> ids,int x,int y){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);long revision=ClientProxy.snapshot.revision;List<QuestCore.Reward> prototype=new ArrayList<QuestCore.Reward>();reward(parent,draft,prototype,()->{QuestCore.Reward template=prototype.get(0);int index=0;for(QuestCore.Quest quest:draft.quests)if(ids.contains(quest.id)){QuestCore.Reward reward=QuestService.JSON.fromJson(QuestService.JSON.toJson(template),QuestCore.Reward.class);reward.id=template.id+"_"+(index++);quest.rewards.add(reward);}QuestObjectConfigScreen.send(draft,revision);},true,x,y);}
    private static void reward(final GuiScreen parent,final QuestCore.Book draft,final List<QuestCore.Reward> destination,final Runnable accepted,final boolean root,int x,int y){
        List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();for(final String type:rewardTypes()){
            if(!root && (type.equals("choice") || type.equals("random") || type.equals("loot")))continue;
            final QuestCore.Reward prototype=new QuestCore.Reward();prototype.type=type;String key="ftbquests.reward.ftbquests."+nativeType(type);String title=StatCollector.canTranslate(key)?StatCollector.translateToLocal(key):type;
            entries.add(new QuestContextScreen.Entry(title,"reward",()->{final QuestCore.Reward reward=new QuestCore.Reward();reward.id="r"+Long.toHexString(System.nanoTime());reward.type=type;reward.target=type.equals("command")?"/say Hi, @team!":"";if(type.equals("item"))reward.item="minecraft:apple";if(type.equals("xp"))reward.count=100;net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new QuestObjectConfigScreen(parent,draft,reward,"reward",()->{destination.add(reward);accepted.run();},false));}){@Override void drawIcon(int ix,int iy,int size,int color){typeIcon(type,true,null,prototype,ix,iy,size);}});
        }
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new QuestContextScreen(parent,x,y,entries));
    }
}
