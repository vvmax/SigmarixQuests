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
    private GuiTextField[] detectorFields=new GuiTextField[0];
    public QuestDeviceScreen(EntityPlayer player,QuestDevice tile) { super(new QuestDeviceContainer(player,tile)); container=(QuestDeviceContainer)inventorySlots; xSize=280; ySize=192; }
    @Override public void initGui() {
        int mode=container.tile.getBlockType() instanceof QuestBlocks.DeviceBlock?((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode:0; xSize=mode==4 || mode==5?390:280;
        ySize=mode==3?228:192;
        super.initGui(); Keyboard.enableRepeatEvents(true); ClientProxy.send(new QuestNetwork.Action());
        if(mode==4 || mode==5) {
            detectorFields=new GuiTextField[4]; String[] values={Double.toString(container.tile.detectorRadius),Double.toString(container.tile.detectorOffsetY),Double.toString(container.tile.detectorHeight),Integer.toString(container.tile.detectorPower)};
            for(int i=0;i<4;i++) { detectorFields[i]=new GuiTextField(fontRendererObj,guiLeft+282,guiTop+24+i*30,96,16); detectorFields[i].setMaxStringLength(20); detectorFields[i].setText(values[i]); }
            buttonList.add(new GuiButton(6,guiLeft+282,guiTop+148,96,20,"Apply detector"));
            buttonList.add(new GuiButton(7,guiLeft+282,guiTop+170,96,20,"Notifications"));
        }
        quest=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+24,210,16); quest.setMaxStringLength(160); quest.setText(container.tile.quest);
        task=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+44,210,16); task.setMaxStringLength(64); task.setText(container.tile.task);
        buttonList.add(new GuiButton(0,guiLeft+184,guiTop+72,82,20,QuestText.tr("Bind")));
        buttonList.add(new GuiButton(1,guiLeft+184,guiTop+98,82,20,QuestText.tr("Unbind")));
        if(container.tile.getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode==9) buttonList.add(new GuiButton(5,guiLeft+184,guiTop+46,82,20,"0/15 / analog"));
        if(mode==0 || mode==1) { buttonList.add(new GuiButton(3,guiLeft+184,guiTop+150,40,20,"W+")); buttonList.add(new GuiButton(4,guiLeft+226,guiTop+150,40,20,"H+")); }
        if(mode==3) buttonList.add(new GuiButton(8,guiLeft+184,guiTop+150,82,20,"Reward >"));
        buttonList.add(new GuiButton(2,guiLeft+184,guiTop+124,82,20,QuestText.tr("Quest book")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==2) { mc.thePlayer.closeScreen(); QuestsMod.proxy.open(); return; }
        if(b.id==8) {
            if(ClientProxy.snapshot==null || ClientProxy.snapshot.book==null) return;
            java.util.List<String> rewards=new java.util.ArrayList<String>(); rewards.add("");
            for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) if(q.id.equals(quest.getText())) for(int i=0;i<q.rewards.size();i++) { QuestCore.Reward reward=q.rewards.get(i); if(reward.type.equals("item") && !reward.auto.equals("invisible")) rewards.add(reward.id==null?"r"+i:reward.id); }
            task.setText(rewards.get((rewards.indexOf(task.getText())+1)%rewards.size())); return;
        }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action=b.id==0?"bind_device":"unbind_device";
        if(b.id==3 || b.id==4) {
            action.action="screen_size"; int width=container.tile.screenWidth,height=container.tile.screenHeight;
            if(b.id==3) width=isShiftKeyDown()?Math.max(1,width-1):Math.min(8,width+1); else height=isShiftKeyDown()?Math.max(1,height-1):Math.min(8,height+1);
            action.choices=new int[]{width,height};
        }
        if(b.id==5) action.action="detector_mode";
        action.id=quest.getText(); action.text=task.getText(); action.x=container.tile.xCoord; action.y=container.tile.yCoord; action.z=container.tile.zCoord;
        if(b.id==6 || b.id==7) { action.action="detector_settings"; action.text=detectorFields[0].getText()+","+detectorFields[1].getText()+","+detectorFields[2].getText()+","+detectorFields[3].getText()+","+(b.id==7?!container.tile.detectorNotifications:container.tile.detectorNotifications); }
        ClientProxy.send(action);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GL11.glColor4f(1,1,1,1); drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF26313D);
        for(Object raw:container.inventorySlots) { net.minecraft.inventory.Slot slot=(net.minecraft.inventory.Slot)raw; drawRect(guiLeft+slot.xDisplayPosition-1,guiTop+slot.yDisplayPosition-1,guiLeft+slot.xDisplayPosition+17,guiTop+slot.yDisplayPosition+17,0xFF65717D); drawRect(guiLeft+slot.xDisplayPosition,guiTop+slot.yDisplayPosition,guiLeft+slot.xDisplayPosition+16,guiTop+slot.yDisplayPosition+16,0xFF101820); }
        drawRect(guiLeft+(container.tile.collector()?82:42),guiTop+78,guiLeft+164,guiTop+88,0xFF101820); drawRect(guiLeft+(container.tile.collector()?83:43),guiTop+79,guiLeft+(container.tile.collector()?83:43)+container.percent*(container.tile.collector()?80:120)/100,guiTop+87,0xFF60B978);
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        for(Object raw:buttonList) { GuiButton button=(GuiButton)raw; if(button.id==7) button.displayString="Notifications: "+(container.tile.detectorNotifications?"ON":"OFF"); }
        String[] detectorLabels={"Radius","Vertical offset","Height","Redstone threshold"}; for(int i=0;i<detectorFields.length;i++) fontRendererObj.drawString(detectorLabels[i],282,14+i*30,0xEEEEEE);
        fontRendererObj.drawString(QuestText.tr("Quest device"),12,8,0xFFFFFF); fontRendererObj.drawString(QuestText.tr("Quest"),12,28,0xEEEEEE); fontRendererObj.drawString(QuestText.tr(container.tile.collector()?"Reward":"Task"),12,48,0xEEEEEE);
        if(!container.tile.collector()) fontRendererObj.drawString(container.tile.screenWidth+" x "+container.tile.screenHeight,184,174,0xB8CDD9);
        fontRendererObj.drawString(container.percent+"% / signal "+container.signal,42,66,0xEEEEEE);
        if(ClientProxy.snapshot!=null) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,260),12,ySize-8,0xFFDD77);
    }
    @Override public void drawScreen(int x,int y,float partial) { super.drawScreen(x,y,partial); quest.drawTextBox(); task.drawTextBox(); for(GuiTextField field:detectorFields) field.drawTextBox(); }
    @Override protected void keyTyped(char ch,int key) {
        if(key==Keyboard.KEY_ESCAPE) { mc.thePlayer.closeScreen(); return; }
        for(GuiTextField field:detectorFields) if(field.textboxKeyTyped(ch,key)) return;
        if(!quest.textboxKeyTyped(ch,key) && !task.textboxKeyTyped(ch,key)) super.keyTyped(ch,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) { super.mouseClicked(x,y,button); quest.mouseClicked(x,y,button); task.mouseClicked(x,y,button); for(GuiTextField field:detectorFields) field.mouseClicked(x,y,button); }
    @Override public void updateScreen() { super.updateScreen(); quest.updateCursorCounter(); task.updateCursorCounter(); for(GuiTextField field:detectorFields) field.updateCursorCounter(); }
    @Override public void onGuiClosed() { super.onGuiClosed(); Keyboard.enableRepeatEvents(false); }
}
