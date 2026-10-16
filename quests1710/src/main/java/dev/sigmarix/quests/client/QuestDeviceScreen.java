package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
public final class QuestDeviceScreen extends GuiContainer {
    private final QuestDeviceContainer container;
    private GuiTextField quest,task;
    public QuestDeviceScreen(EntityPlayer player,QuestDevice tile) { super(new QuestDeviceContainer(player,tile)); container=(QuestDeviceContainer)inventorySlots; xSize=280; ySize=192; }
    @Override public void initGui() {
        super.initGui(); Keyboard.enableRepeatEvents(true); ClientProxy.send(new QuestNetwork.Action());
        quest=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+24,210,16); quest.setMaxStringLength(64); quest.setText(container.tile.quest);
        task=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+44,210,16); task.setMaxStringLength(64); task.setText(container.tile.task);
        buttonList.add(new GuiButton(0,guiLeft+184,guiTop+72,82,20,QuestText.tr("Bind")));
        buttonList.add(new GuiButton(1,guiLeft+184,guiTop+98,82,20,QuestText.tr("Unbind")));
        buttonList.add(new GuiButton(2,guiLeft+184,guiTop+124,82,20,QuestText.tr("Quest book")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==2) { mc.thePlayer.closeScreen(); QuestsMod.proxy.open(); return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action=b.id==0?"bind_device":"unbind_device";
        action.id=quest.getText(); action.text=task.getText(); action.x=container.tile.xCoord; action.y=container.tile.yCoord; action.z=container.tile.zCoord;
        ClientProxy.send(action);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GL11.glColor4f(1,1,1,1); drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF26313D);
        for(Object raw:container.inventorySlots) { net.minecraft.inventory.Slot slot=(net.minecraft.inventory.Slot)raw; drawRect(guiLeft+slot.xDisplayPosition-1,guiTop+slot.yDisplayPosition-1,guiLeft+slot.xDisplayPosition+17,guiTop+slot.yDisplayPosition+17,0xFF65717D); drawRect(guiLeft+slot.xDisplayPosition,guiTop+slot.yDisplayPosition,guiLeft+slot.xDisplayPosition+16,guiTop+slot.yDisplayPosition+16,0xFF101820); }
        drawRect(guiLeft+42,guiTop+78,guiLeft+164,guiTop+88,0xFF101820); drawRect(guiLeft+43,guiTop+79,guiLeft+43+container.percent*120/100,guiTop+87,0xFF60B978);
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        fontRendererObj.drawString(QuestText.tr("Quest device"),12,8,0xFFFFFF); fontRendererObj.drawString(QuestText.tr("Quest"),12,28,0xEEEEEE); fontRendererObj.drawString(QuestText.tr("Task"),12,48,0xEEEEEE);
        fontRendererObj.drawString(container.percent+"% / signal "+container.signal,42,66,0xEEEEEE);
        if(ClientProxy.snapshot!=null) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,260),12,184,0xFFDD77);
    }
    @Override public void drawScreen(int x,int y,float partial) { super.drawScreen(x,y,partial); quest.drawTextBox(); task.drawTextBox(); }
    @Override protected void keyTyped(char ch,int key) {
        if(key==Keyboard.KEY_ESCAPE) { mc.thePlayer.closeScreen(); return; }
        if(!quest.textboxKeyTyped(ch,key) && !task.textboxKeyTyped(ch,key)) super.keyTyped(ch,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) { super.mouseClicked(x,y,button); quest.mouseClicked(x,y,button); task.mouseClicked(x,y,button); }
    @Override public void updateScreen() { super.updateScreen(); quest.updateCursorCounter(); task.updateCursorCounter(); }
    @Override public void onGuiClosed() { super.onGuiClosed(); Keyboard.enableRepeatEvents(false); }
}
