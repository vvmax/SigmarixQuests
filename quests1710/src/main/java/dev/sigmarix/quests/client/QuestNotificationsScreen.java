package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Bounded history is separate from the transient notification queue. */
public final class QuestNotificationsScreen extends GuiScreen {
    private final GuiScreen parent; private int scroll;
    public QuestNotificationsScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() { buttonList.clear(); buttonList.add(new QuestFlatButton(0,12,height-32,90,20,QuestText.tr("Back"))); }
    @Override protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(parent); }
    @Override public void drawScreen(int x,int y,float partial) {
        QuestClassicUi.background(mc,width,height); drawCenteredString(fontRendererObj,QuestText.tr("Notifications"),width/2,14,0xFFFFFF); java.util.List<QuestNetwork.Toast> history=QuestToasts.history();
        int rows=Math.max(1,(height-80)/28); scroll=Math.max(0,Math.min(scroll,Math.max(0,history.size()-rows)));
        for(int i=scroll;i<history.size() && i<scroll+rows;i++) { QuestNetwork.Toast toast=history.get(i); int top=38+(i-scroll)*28; fontRendererObj.drawString(fontRendererObj.trimStringToWidth(toast.task==null?toast.title:QuestTaskLabels.title(toast.task),width-32),16,top,0xFFFFCC66); fontRendererObj.drawString(fontRendererObj.trimStringToWidth(QuestText.tr(toast.text),width-32),16,top+11,0xEEEEEE); }
        super.drawScreen(x,y,partial);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) scroll+=wheel>0?-1:1; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
