package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
public final class QuestIconScreen extends QuestLibraryScreen {
    private GuiTextField resource,name,color;
    private int slot,left,top;
    private boolean advanced;
    private String message="";
    @Override public void initGui() {
        ItemStack held=mc.thePlayer.getHeldItem(); if(held==null || !(held.getItem() instanceof QuestBlocks.CustomIconItem)) { mc.displayGuiScreen(null); return; }
        slot=mc.thePlayer.inventory.currentItem; Keyboard.enableRepeatEvents(true);
        left=(width-230)/2;top=(height-(advanced?152:54))/2;
        resource=new QuestLibraryTextField(fontRendererObj,left+12,top+12,206,12);resource.setEnableBackgroundDrawing(false);resource.setMaxStringLength(1024);resource.setText(QuestIcons.resource(held));resource.setFocused(true);resource.setTextColor(0xFFAA49);
        name=new QuestLibraryTextField(fontRendererObj,left+12,top+56,206,12);name.setEnableBackgroundDrawing(false);name.setMaxStringLength(256);name.setText(held.hasTagCompound()?held.getTagCompound().getString("name"):"");
        color=new QuestLibraryTextField(fontRendererObj,left+12,top+100,92,12);color.setEnableBackgroundDrawing(false);color.setMaxStringLength(6);color.setText(String.format("%06X",QuestIcons.color(held)));
        int by=top+(advanced?128:30);
        buttonList.clear();buttonList.add(new QuestLibraryUi.Button(1,left+8,by,105,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(0,left+117,by,105,16,QuestText.tr("Accept"),""));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==1) { mc.displayGuiScreen(null); return; }
        if(!color.getText().matches("[a-fA-F0-9]{6}") || !QuestPresentation.image(resource.getText()) || resource.getText().isEmpty()) { message=QuestText.tr("Invalid icon texture or color"); return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action="icon_edit"; action.text=resource.getText(); action.id=name.getText(); action.x=Integer.parseInt(color.getText(),16); action.y=slot; ClientProxy.send(action); mc.displayGuiScreen(null);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();QuestLibraryUi.window(left,top,230,advanced?152:54);
        QuestLibraryUi.textBox(left+8,top+8,214,16);resource.drawTextBox();
        if(advanced){
            fontRendererObj.drawString(QuestText.tr("Name"),left+8,top+36,0x404040);QuestLibraryUi.textBox(left+8,top+52,214,16);name.drawTextBox();
            fontRendererObj.drawString("RRGGBB",left+8,top+80,0x404040);QuestLibraryUi.textBox(left+8,top+96,100,16);color.drawTextBox();
        }
        if(!message.isEmpty())drawCenteredString(fontRendererObj,message,width/2,top-12,0xFFFF7777);
        super.drawScreen(x,y,partial);
    }
    @Override protected void keyTyped(char character,int key) {
        if(isCtrlKeyDown() && key==Keyboard.KEY_E){String r=resource.getText(),n=name.getText(),c=color.getText();advanced=!advanced;initGui();resource.setText(r);name.setText(n);color.setText(c);return;}
        if(key==Keyboard.KEY_RETURN){actionPerformed((GuiButton)buttonList.get(1));return;}
        if(key==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(null);return;}
        if(!resource.textboxKeyTyped(character,key) && !(advanced && name.textboxKeyTyped(character,key)) && !(advanced && color.textboxKeyTyped(character,key)))super.keyTyped(character,key);
    }
    @Override protected void mouseClicked(int x,int y,int button){super.mouseClicked(x,y,button);resource.mouseClicked(x,y,button);if(advanced){name.mouseClicked(x,y,button);color.mouseClicked(x,y,button);}}
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
