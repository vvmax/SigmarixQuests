package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
public final class QuestIconScreen extends GuiScreen {
    private GuiTextField resource,name,color;
    private int slot;
    private String message="";
    @Override public void initGui() {
        ItemStack held=mc.thePlayer.getHeldItem(); if(held==null || !(held.getItem() instanceof QuestBlocks.CustomIconItem)) { mc.displayGuiScreen(null); return; }
        slot=mc.thePlayer.inventory.currentItem; Keyboard.enableRepeatEvents(true);
        resource=new GuiTextField(fontRendererObj,16,48,width-32,20); resource.setMaxStringLength(1024); resource.setText(QuestIcons.resource(held));
        name=new GuiTextField(fontRendererObj,16,94,width-32,20); name.setMaxStringLength(256); name.setText(held.hasTagCompound()?held.getTagCompound().getString("name"):"");
        color=new GuiTextField(fontRendererObj,16,140,100,20); color.setMaxStringLength(6); color.setText(String.format("%06X",QuestIcons.color(held)));
        buttonList.clear(); buttonList.add(new QuestFlatButton(0,16,height-32,100,20,QuestText.tr("Apply"))); buttonList.add(new QuestFlatButton(1,124,height-32,100,20,QuestText.tr("Cancel")));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==1) { mc.displayGuiScreen(null); return; }
        if(!color.getText().matches("[a-fA-F0-9]{6}") || !QuestPresentation.image(resource.getText()) || resource.getText().isEmpty()) { message=QuestText.tr("Invalid icon texture or color"); return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action="icon_edit"; action.text=resource.getText(); action.id=name.getText(); action.x=Integer.parseInt(color.getText(),16); action.y=slot; ClientProxy.send(action); mc.displayGuiScreen(null);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        QuestClassicUi.background(mc,width,height); drawCenteredString(fontRendererObj,QuestText.tr("Custom icon"),width/2,12,0xFFFFFF);
        fontRendererObj.drawString(QuestText.tr("Texture resource"),16,34,0xEEEEEE); fontRendererObj.drawString(QuestText.tr("Name"),16,80,0xEEEEEE); fontRendererObj.drawString("RRGGBB",16,126,0xEEEEEE);
        resource.drawTextBox(); name.drawTextBox(); color.drawTextBox(); fontRendererObj.drawString(message,16,height-48,0xFFDD77);
        ItemStack held=mc.thePlayer.getHeldItem(); if(held!=null) {
            ItemStack preview=held.copy(); NBTTagCompound tag=preview.hasTagCompound()?(NBTTagCompound)preview.getTagCompound().copy():new NBTTagCompound(); tag.setString("icon",resource.getText()); if(color.getText().matches("[a-fA-F0-9]{6}")) tag.setInteger("color",Integer.parseInt(color.getText(),16)); preview.setTagCompound(tag);
            GL11.glPushMatrix(); GL11.glTranslatef(width-76,126,0); GL11.glScalef(3,3,1); itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),preview,0,0); GL11.glPopMatrix();
        }
        super.drawScreen(x,y,partial);
    }
    @Override protected void keyTyped(char character,int key) { if(!resource.textboxKeyTyped(character,key) && !name.textboxKeyTyped(character,key) && !color.textboxKeyTyped(character,key)) super.keyTyped(character,key); }
    @Override protected void mouseClicked(int x,int y,int button) { super.mouseClicked(x,y,button); resource.mouseClicked(x,y,button); name.mouseClicked(x,y,button); color.mouseClicked(x,y,button); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
