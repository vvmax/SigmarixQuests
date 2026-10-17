package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** All pins remain manageable when the HUD has room for fewer rows. */
public final class QuestPinScreen extends GuiScreen {
    private final GuiScreen parent; private int scroll,ticks;
    public QuestPinScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() { rebuild(); }
    private void rebuild() {
        buttonList.clear(); if(ClientProxy.snapshot==null) return; int rows=Math.max(1,(height-82)/24); scroll=Math.max(0,Math.min(scroll,Math.max(0,ClientProxy.snapshot.pinned.size()-rows)));
        for(int i=scroll;i<ClientProxy.snapshot.pinned.size() && i<scroll+rows;i++) {
            String id=ClientProxy.snapshot.pinned.get(i),title=id; for(QuestCore.Quest quest:ClientProxy.snapshot.book.quests) if(quest.id.equals(id)) title=quest.title;
            buttonList.add(new GuiButton(100+i,12,36+(i-scroll)*24,width-24,20,fontRendererObj.trimStringToWidth(title+" "+ClientProxy.snapshot.percentages.getOrDefault(id,0)+"% - "+QuestText.tr("Unpin quest"),width-40)));
        }
        buttonList.add(new GuiButton(0,12,height-32,80,20,QuestText.tr("Back"))); buttonList.add(new GuiButton(1,width-144,height-32,132,20,QuestText.tr("Notifications")));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==0) { mc.displayGuiScreen(parent); return; } if(button.id==1) { mc.displayGuiScreen(new QuestNotificationsScreen(this)); return; }
        int index=button.id-100; if(index>=0 && index<ClientProxy.snapshot.pinned.size()) { QuestNetwork.Action action=new QuestNetwork.Action(); action.action="pin"; action.id=ClientProxy.snapshot.pinned.get(index); ClientProxy.send(action); }
    }
    @Override public void updateScreen() { if(ClientProxy.snapshot==null) { mc.displayGuiScreen(null); return; } rebuild(); if(++ticks%40==0) ClientProxy.send(new QuestNetwork.Action()); }
    @Override public void drawScreen(int x,int y,float partial) { drawDefaultBackground(); drawCenteredString(fontRendererObj,QuestText.tr("Pinned quests"),width/2,14,0xFFFFFF); super.drawScreen(x,y,partial); }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) { scroll+=wheel>0?-1:1; rebuild(); } }
    @Override public boolean doesGuiPauseGame() { return false; }
}
