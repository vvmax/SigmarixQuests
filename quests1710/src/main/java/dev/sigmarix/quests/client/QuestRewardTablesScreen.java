package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.util.StatCollector;
/** Searchable reward table directory with the reference 14px row geometry. */
final class QuestRewardTablesScreen extends QuestListScreen {
    private List<String> ids=new ArrayList<String>();
    QuestRewardTablesScreen(GuiScreen parent){super(parent,true,true,1,1,1);}
    @Override protected int rowHeight(){return 14;}
    @Override protected String title(){return StatCollector.translateToLocal("ftbquests.reward_tables");}
    @Override protected List<Row> rows(){
        ids=new ArrayList<String>(ClientProxy.snapshot.book.lootTables.keySet());List<Row> rows=new ArrayList<Row>();rows.add(new Row(0,StatCollector.translateToLocal("gui.add"),"add"));
        for(int i=0;i<ids.size();i++){String id=ids.get(i);QuestCore.LootSettings settings=ClientProxy.snapshot.book.lootSettings.get(id);String title=QuestTaskLabels.table(ClientProxy.snapshot.book,id);Row row=new Row(i+1,(settings!=null && !settings.alias.isEmpty()?"\u00a7e":"")+title,"money_bag");tableTooltip(row,id,settings);rows.add(row);}return rows;
    }
    private void tableTooltip(Row row,String id,QuestCore.LootSettings settings){
        QuestCore.Book book=ClientProxy.snapshot.book;int used=0;for(QuestCore.Quest quest:book.quests)for(QuestCore.Reward reward:quest.rewards)if((reward.type.equals("random") || reward.type.equals("choice")) && id.equals(reward.table))used++;
        if(used>0)row.tooltip.add("\u00a77"+StatCollector.translateToLocalFormatted("ftbquests.reward_table.used_in",used));
        if(settings!=null && settings.hideTooltip)return;
        List<QuestCore.Reward> rewards=new ArrayList<QuestCore.Reward>(book.lootTables.get(id));int empty=settings==null?0:settings.emptyWeight,total=empty;for(QuestCore.Reward reward:rewards)total+=reward.weight;
        if(empty>0)row.tooltip.add("\u00a77- "+StatCollector.translateToLocal("ftbquests.reward_table.nothing")+"\u00a78 ["+QuestRewardTableEditScreen.chanceString(empty,total)+"]");
        rewards.sort((a,b)->Integer.compare(a.weight,b.weight));for(int i=0;i<rewards.size();i++){if(i==10){row.tooltip.add("\u00a77- "+StatCollector.translateToLocalFormatted("ftbquests.reward_table.and_more",rewards.size()-10));break;}QuestCore.Reward reward=rewards.get(i);row.tooltip.add("\u00a77- "+QuestTaskLabels.reward(reward)+"\u00a78 ["+QuestRewardTableEditScreen.chanceString(reward.weight,total)+"]");}
    }
    @Override protected void drawRowIcon(Row row,int x,int y,int size){
        if(row.id==0){QuestClassicUi.libraryIcon("add",x,y,size,0xFFFFFFFF);return;}
        String id=ids.get(row.id-1);QuestCore.LootSettings settings=ClientProxy.snapshot.book.lootSettings.get(id);
        if(settings!=null && !settings.icon.isEmpty())QuestClassicUi.item(mc,settings.icon,settings.iconMeta,settings.iconNbt,x,y,size);
        else if(settings!=null && !settings.alias.isEmpty())QuestClassicUi.item(mc,"sigmarixquests:loot_crate",0,QuestLoot.crate(ClientProxy.snapshot.book,id).getTagCompound().toString(),x,y,size);
        else{List<QuestCore.Reward> rewards=ClientProxy.snapshot.book.lootTables.get(id);if(rewards.isEmpty())QuestClassicUi.libraryIcon("dice",x,y,size,0xFFFFFFFF);else QuestClassicUi.reward(mc,rewards.get((int)(System.currentTimeMillis()/1000%rewards.size())),x,y,size);}
    }
    private void update(String id,java.util.function.BiConsumer<QuestCore.Book,QuestCore.LootSettings> change){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.LootSettings settings=draft.lootSettings.get(id);if(settings==null){settings=new QuestCore.LootSettings();draft.lootSettings.put(id,settings);}change.accept(draft,settings);QuestObjectConfigScreen.send(draft,ClientProxy.snapshot.revision);mc.displayGuiScreen(this);}
    @Override protected void click(Row row,int button){
        if(row.id==0){mc.displayGuiScreen(new QuestValueScreen(this,"",0xFFAA49,256,value->{String id="table_"+java.util.UUID.randomUUID().toString().replace("-","");QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);draft.lootTables.put(id,new ArrayList<QuestCore.Reward>());QuestCore.LootSettings settings=new QuestCore.LootSettings();settings.name=value;draft.lootSettings.put(id,settings);QuestNetwork.Action action=new QuestNetwork.Action();action.action="save";action.book=draft;action.revision=ClientProxy.snapshot.revision;ClientProxy.send(action);}));return;}
        final String id=ids.get(row.id-1);List<QuestContextScreen.Entry> menu=new ArrayList<QuestContextScreen.Entry>();
        menu.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("gui.edit"),"settings",()->{QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);long revision=ClientProxy.snapshot.revision;mc.displayGuiScreen(new QuestRewardTableEditScreen(this,draft,draft.lootTables.get(id),id,()->QuestObjectConfigScreen.send(draft,revision)));}));
        menu.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("selectServer.delete"),"remove",()->mc.displayGuiScreen(new GuiYesNo((yes,n)->{if(yes)update(id,(draft,settings)->{draft.lootTables.remove(id);draft.lootSettings.remove(id);});else mc.displayGuiScreen(this);},StatCollector.translateToLocalFormatted("delete_item",id),"",0))));
        menu.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("ftbquests.gui.copy_id"),"info",()->{GuiScreen.setClipboardString(id);mc.displayGuiScreen(this);}));
        QuestCore.LootSettings current=ClientProxy.snapshot.book.lootSettings.get(id);
        menu.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("item.ftbquests.lootcrate.name"),current!=null && !current.alias.isEmpty()?"accept":"accept_gray",()->update(id,(draft,settings)->{
            if(!settings.alias.isEmpty()){settings.alias="";settings.drops=new QuestLoot.Weights();return;}
            String alias=QuestTaskLabels.table(draft,id).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]","_").replaceAll("_{2,}","_");if(alias.isEmpty())alias=id;String base=alias;int suffix=2;while(usedAlias(draft,alias,id))alias=base+"_"+(suffix++);settings.alias=alias;settings.glow=false;
            if(alias.equals("common")){settings.color="92999A";settings.drops=new QuestLoot.Weights(350,10,0);}else if(alias.equals("uncommon")){settings.color="37AA69";settings.drops=new QuestLoot.Weights(200,90,0);}else if(alias.equals("rare")){settings.color="0094FF";settings.drops=new QuestLoot.Weights(50,200,0);}else if(alias.equals("epic")){settings.color="8000FF";settings.drops=new QuestLoot.Weights(9,10,10);}else if(alias.equals("legendary")){settings.color="FFC147";settings.glow=true;settings.drops=new QuestLoot.Weights(1,1,190);}
        })));
        mc.displayGuiScreen(new QuestContextScreen(this,left+12,top+panelY+(row.id)*(rowHeight()+gap)-scroll,menu));
    }
    private static boolean usedAlias(QuestCore.Book book,String alias,String id){if(book.lootTables.containsKey(alias) && !alias.equals(id))return true;for(Map.Entry<String,QuestCore.LootSettings> entry:book.lootSettings.entrySet())if(!entry.getKey().equals(id) && alias.equals(entry.getValue().alias))return true;return false;}
}
