package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Server-authoritative confirmation controls for individual tasks. */
public final class QuestTaskScreen extends QuestLibraryScreen {
    private final GuiScreen parent;
    private final QuestCore.Quest quest;
    private int scroll,refreshTicks;
    private final long revision;
    public QuestTaskScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; this.revision=ClientProxy.snapshot.revision; }
    @Override public void initGui() { rebuild(); }
    private void rebuild() {
        buttonList.clear(); int rows=Math.max(1,(height-90)/26); scroll=Math.max(0,Math.min(scroll,Math.max(0,quest.tasks.size()-rows)));
        for(int i=scroll;i<quest.tasks.size() && i<scroll+rows;i++) {
            QuestCore.Task task=quest.tasks.get(i); boolean submit=task.consume && (task.type.equals("emc") || task.type.equals("item") && !task.craftingOnly); boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(quest.id+":"+task.id));
            GuiButton button=new QuestFlatButton(100+i,12,42+(i-scroll)*26,width-24,22,fontRendererObj.trimStringToWidth(QuestTaskLabels.title(task)+QuestText.tr(done?" [done]":task.type.equals("check")?" - confirm":task.type.equals("custom") && task.manual?" - run action":submit?" - submit":""),width-40));
            button.enabled=!done && (task.type.equals("check") || task.type.equals("custom") && task.manual || submit) && "available".equals(ClientProxy.snapshot.states.get(quest.id)); buttonList.add(button);
        }
        buttonList.add(new QuestFlatButton(0,12,height-32,90,20,QuestText.tr("Back")));
        buttonList.add(new QuestFlatButton(1,width-122,height-32,110,20,QuestText.tr("Start quest")));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(ClientProxy.snapshot.revision!=revision) { mc.displayGuiScreen(parent); return; }
        if(button.id==0) { mc.displayGuiScreen(parent); return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.id=quest.id;
        if(button.id==1) action.action="start";
        else { QuestCore.Task task=quest.tasks.get(button.id-100); action.action=task.type.equals("check") || task.type.equals("custom") && task.manual?"task":"submit"; action.text=task.id; }
        ClientProxy.send(action);
    }
    @Override public void updateScreen() { if(ClientProxy.snapshot.revision!=revision) { mc.displayGuiScreen(parent); return; } rebuild(); if(++refreshTicks%40==0) ClientProxy.send(new QuestNetwork.Action()); }
    @Override public void drawScreen(int x,int y,float partial) { QuestClassicUi.background(mc,width,height); drawCenteredString(fontRendererObj,QuestTaskLabels.quest(quest),width/2,15,0xFFFFFF); drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,width-24),width/2,28,0xFFDD77); super.drawScreen(x,y,partial);
        for(Object raw:buttonList) { GuiButton button=(GuiButton)raw;
            if(button.id>=100) { QuestCore.Task task=quest.tasks.get(button.id-100); if(!task.icon.isEmpty()) {
                net.minecraft.item.Item item=(net.minecraft.item.Item)net.minecraft.item.Item.itemRegistry.getObject(task.icon);
                if(item!=null) { net.minecraft.item.ItemStack icon=new net.minecraft.item.ItemStack(item,1,task.iconMeta); icon.setTagCompound(QuestService.tag(task.iconNbt)); itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),icon,button.xPosition+button.width-20,button.yPosition+3); }
            } }
            if(button.id>=100 && x>=button.xPosition && x<button.xPosition+button.width && y>=button.yPosition && y<button.yPosition+button.height) {
            QuestCore.Task task=quest.tasks.get(button.id-100); if(!task.description.isEmpty()) drawHoveringText(fontRendererObj.listFormattedStringToWidth(task.description,Math.min(300,width-32)),x,y,fontRendererObj);
        } }
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) { scroll+=wheel>0?-1:1; rebuild(); } }
    @Override public boolean doesGuiPauseGame() { return false; }
}
