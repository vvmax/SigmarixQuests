package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;import net.minecraft.client.gui.*;import net.minecraft.util.StatCollector;
/** Weighted reward table form with the original compact header rows and reward context menu. */
final class QuestRewardTableEditScreen extends QuestListScreen {
    private final QuestCore.Book draft;private final List<QuestCore.Reward> original,working;private final String tableId;private final Runnable commit;
    QuestRewardTableEditScreen(GuiScreen parent,QuestCore.Book draft,List<QuestCore.Reward> original,String tableId,Runnable commit){super(parent,false,true,1,1,1);this.draft=draft;this.original=original;this.tableId=tableId;this.commit=commit;working=new ArrayList<QuestCore.Reward>();for(QuestCore.Reward reward:original)working.add(QuestService.JSON.fromJson(QuestService.JSON.toJson(reward),QuestCore.Reward.class));}
    private String tr(String key){return StatCollector.translateToLocal(key);}
    @Override protected String title(){return tr("ftbquests.reward_table");}
    @Override protected Object revision(){return working;}
    @Override protected List<Row> rows(){List<Row> rows=new ArrayList<Row>();String[] titles={tr("gui.settings"),tr("gui.accept"),tr("gui.add")},icons={"settings","accept","add"};for(int i=tableId==null?1:0;i<3;i++){Row row=new Row(-i-1,titles[i],icons[i]);row.height=12;rows.add(row);}Row spacer=new Row(-4,"","");spacer.height=1;spacer.spacer=true;spacer.enabled=false;rows.add(spacer);int total=0;for(QuestCore.Reward r:working)total+=r.weight;for(int i=0;i<working.size();i++){QuestCore.Reward reward=working.get(i);Row row=new Row(i,QuestTaskLabels.reward(reward),"reward");row.tooltip.add(QuestTaskLabels.reward(reward));row.tooltip.add(tr("ftbquests.reward_table.weight")+": "+reward.weight+"\u00a78 ["+String.format(java.util.Locale.ROOT,"%.2f%%",total==0?0:reward.weight*100D/total)+"]");rows.add(row);}return rows;}
    @Override protected void drawRowIcon(Row row,int x,int y,int size){if(row.id>=0)QuestClassicUi.reward(mc,working.get(row.id),x,y,size);else QuestClassicUi.libraryIcon(row.icon,x,y,size,0xFFFFFFFF);}
    @Override protected void click(Row row,int button){
        if(row.id==-1){if(tableId!=null)mc.displayGuiScreen(new QuestRewardTableSettingsScreen(this,draft,tableId));return;}
        if(row.id==-2){original.clear();original.addAll(working);commit.run();mc.displayGuiScreen(parent);return;}
        if(row.id==-3){QuestCreationMenus.reward(this,draft,working,()->rebuild());return;}
        if(row.id<0)return;final QuestCore.Reward reward=working.get(row.id);List<QuestContextScreen.Entry> menu=new ArrayList<QuestContextScreen.Entry>();
        menu.add(new QuestContextScreen.Entry(tr("selectServer.edit"),"settings",()->mc.displayGuiScreen(new QuestObjectConfigScreen(this,draft,reward,"reward",()->{},false))));
        menu.add(new QuestContextScreen.Entry(tr("ftbquests.reward_table.set_weight"),"settings",()->mc.displayGuiScreen(new QuestValueScreen(this,Integer.toString(reward.weight),0xAA5AE8,10,text->{int weight=Integer.parseInt(text);if(weight<1 || weight>1000000)throw new IllegalArgumentException("Invalid reward weight");reward.weight=weight;}))));
        menu.add(new QuestContextScreen.Entry(tr("selectServer.delete"),"remove",()->mc.displayGuiScreen(new GuiYesNo((yes,id)->{if(yes)working.remove(reward);mc.displayGuiScreen(this);},StatCollector.translateToLocalFormatted("delete_item",QuestTaskLabels.reward(reward)),"",0))));
        mc.displayGuiScreen(new QuestContextScreen(this,left+12,top+panelY,menu));
    }
}
