package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
public final class QuestDeviceScreen extends GuiContainer {
    private final QuestDeviceContainer container;
    private QuestDeviceConfigScreen properties;private boolean advanced;
    void showAdvanced(){advanced=true;properties=null;}
    private GuiTextField quest,task,owner;
    private GuiTextField[] detectorFields=new GuiTextField[0];
    public QuestDeviceScreen(EntityPlayer player,QuestDevice tile) { super(new QuestDeviceContainer(player,tile)); container=(QuestDeviceContainer)inventorySlots; xSize=280; ySize=192; }
    @Override public void initGui() {
        int mode=container.tile.getBlockType() instanceof QuestBlocks.DeviceBlock?((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode:0; xSize=mode==0 || mode==1 || mode==4 || mode==5 || mode==9?390:280;
        ySize=mode==3 || mode==6?228:192;
        super.initGui(); Keyboard.enableRepeatEvents(true); ClientProxy.send(new QuestNetwork.Action());
        if(mode==4 || mode==5) {
            detectorFields=new GuiTextField[4]; String[] values={Double.toString(container.tile.detectorRadius),Double.toString(container.tile.detectorOffsetY),Double.toString(container.tile.detectorHeight),Integer.toString(container.tile.detectorPower)};
            for(int i=0;i<4;i++) { detectorFields[i]=new GuiTextField(fontRendererObj,guiLeft+282,guiTop+24+i*30,96,16); detectorFields[i].setMaxStringLength(20); detectorFields[i].setText(values[i]); }
            buttonList.add(new QuestFlatButton(6,guiLeft+282,guiTop+148,96,20,"Apply detector"));
            buttonList.add(new QuestFlatButton(7,guiLeft+282,guiTop+170,96,20,"Notifications"));
        }
        if(mode==0 || mode==1) {
            buttonList.add(new QuestFlatButton(9,guiLeft+282,guiTop+24,96,20,mode==0?"Input only":"Fullscreen"));
            buttonList.add(new QuestFlatButton(10,guiLeft+282,guiTop+48,96,20,mode==0?"Held icon":"Hide icons"));
        }
        if(mode==0 || mode==1) {
            buttonList.add(new QuestFlatButton(11,guiLeft+282,guiTop+72,96,20,QuestText.tr("Use held skin")));
            buttonList.add(new QuestFlatButton(12,guiLeft+282,guiTop+96,96,20,QuestText.tr("Unbreakable")));
        }
        owner=null;
        if(mode==0 || mode==1 || mode==9) {
            owner=new GuiTextField(fontRendererObj,guiLeft+282,guiTop+140,96,16); owner.setMaxStringLength(64); owner.setText(container.tile.owner==null?"":container.tile.owner.toString());
            buttonList.add(new QuestFlatButton(13,guiLeft+282,guiTop+160,96,20,QuestText.tr("Set team")));
        }
        quest=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+24,210,16); quest.setMaxStringLength(160); quest.setText(container.tile.quest);
        task=new GuiTextField(fontRendererObj,guiLeft+56,guiTop+44,210,16); task.setMaxStringLength(64); task.setText(container.tile.task);
        if(mode!=6) { buttonList.add(new QuestFlatButton(0,guiLeft+184,guiTop+72,82,20,QuestText.tr("Bind"))); buttonList.add(new QuestFlatButton(1,guiLeft+184,guiTop+98,82,20,QuestText.tr("Unbind"))); }
        if(container.tile.getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode==9) buttonList.add(new QuestFlatButton(5,guiLeft+184,guiTop+46,82,20,"0/15 / analog"));
        if(mode==0 || mode==1) { buttonList.add(new QuestFlatButton(3,guiLeft+184,guiTop+150,40,20,"W+")); buttonList.add(new QuestFlatButton(4,guiLeft+226,guiTop+150,40,20,"H+")); }
        if(mode==3) buttonList.add(new QuestFlatButton(8,guiLeft+184,guiTop+150,82,20,"Reward >"));
        buttonList.add(new QuestFlatButton(2,guiLeft+184,guiTop+124,82,20,QuestText.tr("Quest book")));
        if(!advanced && mode!=6){properties=new QuestDeviceConfigScreen(this,container.tile);properties.setWorldAndResolution(mc,width,height);}
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
        if(b.id==9 || b.id==10) { action.action="screen_options"; int mode=((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode; action.choices=new int[]{mode==0?(b.id==9?2:3):(b.id==9?0:1)}; }
        if(b.id==11) { action.action="screen_skin"; action.choices=new int[]{isShiftKeyDown()?1:0}; }
        if(b.id==12) { action.action="screen_lock"; action.choices=new int[]{container.tile.indestructible?0:1}; }
        if(b.id==13) action.action="screen_owner";
        action.id=quest.getText(); action.text=task.getText(); action.x=container.tile.xCoord; action.y=container.tile.yCoord; action.z=container.tile.zCoord;
        if(b.id==6 || b.id==7) { action.action="detector_settings"; action.text=detectorFields[0].getText()+","+detectorFields[1].getText()+","+detectorFields[2].getText()+","+detectorFields[3].getText()+","+(b.id==7?!container.tile.detectorNotifications:container.tile.detectorNotifications); }
        if(b.id==13) action.text=owner.getText();
        ClientProxy.send(action);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GL11.glColor4f(1,1,1,1); drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF26313D);
        for(Object raw:container.inventorySlots) { net.minecraft.inventory.Slot slot=(net.minecraft.inventory.Slot)raw; drawRect(guiLeft+slot.xDisplayPosition-1,guiTop+slot.yDisplayPosition-1,guiLeft+slot.xDisplayPosition+17,guiTop+slot.yDisplayPosition+17,0xFF65717D); drawRect(guiLeft+slot.xDisplayPosition,guiTop+slot.yDisplayPosition,guiLeft+slot.xDisplayPosition+16,guiTop+slot.yDisplayPosition+16,0xFF101820); }
        if(!container.tile.opener()) { int left=container.tile.collector()?82:42; drawRect(guiLeft+left,guiTop+78,guiLeft+164,guiTop+88,0xFF101820); drawRect(guiLeft+left+1,guiTop+79,guiLeft+left+1+container.percent*(162-left)/100,guiTop+87,0xFF60B978); }
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        for(Object raw:buttonList) { GuiButton button=(GuiButton)raw; if(button.id==9) button.displayString=container.tile.getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode==0?"Input only: "+container.tile.screenInputOnly:"Fullscreen: "+container.tile.screenFullscreen; if(button.id==10 && ((QuestBlocks.DeviceBlock)container.tile.getBlockType()).mode==1) button.displayString="Hide icons: "+container.tile.screenHideIcons; if(button.id==12) button.displayString=QuestText.tr("Unbreakable")+": "+(container.tile.indestructible?"ON":"OFF"); if(button.id==7) button.displayString="Notifications: "+(container.tile.detectorNotifications?"ON":"OFF"); }
        if(owner!=null) fontRendererObj.drawString(QuestText.tr("Player / UUID"),282,128,0xEEEEEE);
        String[] detectorLabels={"Radius","Vertical offset","Height","Redstone threshold"}; for(int i=0;i<detectorFields.length;i++) fontRendererObj.drawString(detectorLabels[i],282,14+i*30,0xEEEEEE);
        fontRendererObj.drawString(QuestText.tr(container.tile.opener()?"Crate opener":"Quest device"),12,8,0xFFFFFF);
        if(container.tile.opener()) { fontRendererObj.drawString(QuestText.tr("Queued loot")+": "+container.queuedLoot,12,28,0xEEEEEE); fontRendererObj.drawString(QuestText.tr("Input"),12,60,0xEEEEEE); fontRendererObj.drawString(QuestText.tr("Outputs"),50,60,0xEEEEEE); }
        else { fontRendererObj.drawString(QuestText.tr("Quest"),12,28,0xEEEEEE); fontRendererObj.drawString(QuestText.tr(container.tile.collector()?"Reward":"Task"),12,48,0xEEEEEE); fontRendererObj.drawString(container.percent+"% / signal "+container.signal,42,66,0xEEEEEE); }
        if(!container.tile.collector() && !container.tile.opener()) fontRendererObj.drawString(container.tile.screenWidth+" x "+container.tile.screenHeight,184,174,0xB8CDD9);
        if(ClientProxy.snapshot!=null) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,260),12,ySize-8,0xFFDD77);
    }
    @Override public void drawScreen(int x,int y,float partial) { if(properties!=null){properties.drawScreen(x,y,partial);return;}super.drawScreen(x,y,partial); if(!container.tile.opener()) { quest.drawTextBox(); task.drawTextBox(); } for(GuiTextField field:detectorFields) field.drawTextBox(); if(owner!=null) owner.drawTextBox(); }
    @Override protected void keyTyped(char ch,int key) {
        if(properties!=null){properties.keyTyped(ch,key);return;}
        if(key==Keyboard.KEY_ESCAPE) { mc.thePlayer.closeScreen(); return; }
        if(owner!=null && owner.textboxKeyTyped(ch,key)) return;
        for(GuiTextField field:detectorFields) if(field.textboxKeyTyped(ch,key)) return;
        if(container.tile.opener() || !quest.textboxKeyTyped(ch,key) && !task.textboxKeyTyped(ch,key)) super.keyTyped(ch,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) { if(properties!=null){properties.mouseClicked(x,y,button);return;}super.mouseClicked(x,y,button); if(!container.tile.opener()) { quest.mouseClicked(x,y,button); task.mouseClicked(x,y,button); } for(GuiTextField field:detectorFields) field.mouseClicked(x,y,button); if(owner!=null) owner.mouseClicked(x,y,button); }
    @Override public void updateScreen() { if(properties!=null)properties.updateScreen();super.updateScreen(); quest.updateCursorCounter(); task.updateCursorCounter(); for(GuiTextField field:detectorFields) field.updateCursorCounter(); if(owner!=null) owner.updateCursorCounter(); }
    @Override public void handleMouseInput(){if(properties!=null)properties.handleMouseInput();else super.handleMouseInput();}
    @Override protected void mouseClickMove(int x,int y,int button,long time){if(properties!=null)properties.mouseClickMove(x,y,button,time);else super.mouseClickMove(x,y,button,time);}
    @Override protected void mouseMovedOrUp(int x,int y,int button){if(properties!=null)properties.mouseMovedOrUp(x,y,button);else super.mouseMovedOrUp(x,y,button);}
    @Override public void onGuiClosed() { super.onGuiClosed(); Keyboard.enableRepeatEvents(false); }
}
