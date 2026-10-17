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
        for(int i=0;i<ids.size();i++){String id=ids.get(i);QuestCore.LootSettings settings=ClientProxy.snapshot.book.lootSettings.get(id);String title=settings==null || settings.name.isEmpty()?id:settings.name;rows.add(new Row(i+1,"\u00a7e"+title,"money_bag"));}return rows;
    }
    @Override protected void click(Row row,int button){
        if(row.id==0){mc.displayGuiScreen(new QuestValueScreen(this,"",0xFFAA49,128,value->{if(!QuestCore.validId(value) || ClientProxy.snapshot.book.lootTables.containsKey(value))throw new IllegalArgumentException("Invalid or existing table ID");QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);draft.lootTables.put(value,new ArrayList<QuestCore.Reward>());QuestCore.LootSettings settings=new QuestCore.LootSettings();settings.emptyWeight=1;draft.lootSettings.put(value,settings);QuestNetwork.Action action=new QuestNetwork.Action();action.action="save";action.book=draft;action.revision=ClientProxy.snapshot.revision;ClientProxy.send(action);}));return;}
        final String id=ids.get(row.id-1);List<QuestContextScreen.Entry> menu=new ArrayList<QuestContextScreen.Entry>();
        menu.add(new QuestContextScreen.Entry(StatCollector.translateToLocal("gui.edit"),"settings",()->{QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);long revision=ClientProxy.snapshot.revision;mc.displayGuiScreen(new QuestRewardTableEditScreen(this,draft,draft.lootTables.get(id),id,()->QuestObjectConfigScreen.send(draft,revision)));}));
        mc.displayGuiScreen(new QuestContextScreen(this,left+12,top+panelY+(row.id)*(rowHeight()+gap)-scroll,menu));
    }
}
