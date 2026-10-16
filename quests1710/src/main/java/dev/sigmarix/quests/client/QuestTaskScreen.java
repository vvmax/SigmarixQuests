package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Server-authoritative confirmation controls for individual tasks. */
public final class QuestTaskScreen extends GuiScreen {
    private final GuiScreen parent;
    private final QuestCore.Quest quest;
    private int scroll;
    public QuestTaskScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; }
    @Override public void initGui() { rebuild(); }
    private void rebuild() {
        buttonList.clear(); int rows=Math.max(1,(height-90)/26); scroll=Math.max(0,Math.min(scroll,Math.max(0,quest.tasks.size()-rows)));
        for(int i=scroll;i<quest.tasks.size() && i<scroll+rows;i++) {
            QuestCore.Task task=quest.tasks.get(i); boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(quest.id+":"+task.id));
            GuiButton button=new GuiButton(100+i,12,42+(i-scroll)*26,width-24,22,fontRendererObj.trimStringToWidth(task.id+" - "+task.type+(done?" [done]":task.type.equals("check")?" - confirm":""),width-40));
            button.enabled=!done && task.type.equals("check") && "available".equals(ClientProxy.snapshot.states.get(quest.id)); buttonList.add(button);
        }
        buttonList.add(new GuiButton(0,12,height-32,90,20,"Back"));
        buttonList.add(new GuiButton(1,width-122,height-32,110,20,"Start quest"));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==0) { mc.displayGuiScreen(parent); return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.id=quest.id;
        if(button.id==1) action.action="start";
        else { action.action="task"; action.text=quest.tasks.get(button.id-100).id; }
        ClientProxy.send(action);
    }
    @Override public void updateScreen() { rebuild(); }
    @Override public void drawScreen(int x,int y,float partial) { drawDefaultBackground(); drawCenteredString(fontRendererObj,quest.title,width/2,15,0xFFFFFF); drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,width-24),width/2,28,0xFFDD77); super.drawScreen(x,y,partial); }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) { scroll+=wheel>0?-1:1; rebuild(); } }
    @Override public boolean doesGuiPauseGame() { return false; }
}
