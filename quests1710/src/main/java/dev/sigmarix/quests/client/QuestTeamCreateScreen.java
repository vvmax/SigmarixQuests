package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
/** The 162x118 original creation form, including its ten dye colors. */
final class QuestTeamCreateScreen extends GuiScreen {
    static final int[] COLORS={0x0094FF,0x00DDFF,0x23D34C,0xFFD000,0xFF9400,0xEA4B4B,0xE888C9,0xFF007F,0xB342FF,0xC0C0C0};
    private final GuiScreen parent;private GuiTextField input;private int left,top,color=0x0094FF;
    QuestTeamCreateScreen(GuiScreen parent){this.parent=parent;}
    @Override public void initGui(){Keyboard.enableRepeatEvents(true);String previous=input==null?mc.thePlayer.getCommandSenderName().toLowerCase(java.util.Locale.ROOT):input.getText();left=(width-162)/2;top=(height-118)/2;input=new QuestLibraryTextField(fontRendererObj,left+12,top+12,138,12);input.setMaxStringLength(35);input.setText(previous);input.setFocused(true);buttonList.clear();buttonList.add(new QuestLibraryUi.Button(0,left+8,top+94,71,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(20,left+82,top+94,71,16,QuestText.tr("Accept"),""));}
    @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();QuestLibraryUi.window(left,top,162,118);QuestLibraryUi.textBox(left+8,top+8,146,16);input.setTextColor(color);input.drawTextBox();int hover=-1;for(int i=0;i<COLORS.length;i++){int bx=left+8+i%5*30,by=top+32+i/5*30;boolean over=x>=bx && x<bx+25 && y>=by && y<by+25;QuestLibraryUi.slot(bx,by,25,25);drawRect(bx,by,bx+25,by+25,((over || color==COLORS[i]?200:100)<<24)|COLORS[i]);if(over)hover=i;}super.drawScreen(x,y,partial);if(hover>=0){String[] names={"Blue","Cyan","Green","Yellow","Orange","Red","Pink","Magenta","Purple","Gray"};drawHoveringText(java.util.Collections.singletonList(colorName(hover)),x,y,fontRendererObj);}}
    static String colorName(int index){return net.minecraft.util.StatCollector.translateToLocal("item.fireworksCharge."+new String[]{"blue","cyan","green","yellow","orange","red","pink","magenta","purple","gray"}[index]);}
    @Override protected void actionPerformed(GuiButton button){if(button.id==20 && !input.getText().isEmpty()){QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_create";action.text=input.getText();action.x=color;ClientProxy.send(action);}mc.displayGuiScreen(parent);}
    @Override protected void mouseClicked(int x,int y,int button){super.mouseClicked(x,y,button);input.mouseClicked(x,y,button);if(button==0)for(int i=0;i<COLORS.length;i++){int bx=left+8+i%5*30,by=top+32+i/5*30;if(x>=bx && x<bx+25 && y>=by && y<by+25)color=COLORS[i];}}
    @Override protected void keyTyped(char ch,int key){if(key==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(parent);return;}if(key==Keyboard.KEY_RETURN){actionPerformed((GuiButton)buttonList.get(1));return;}if(input.textboxKeyTyped(ch,key)){String clean=input.getText().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]","");if(!clean.equals(input.getText()))input.setText(clean);}}
    @Override public void updateScreen(){input.updateCursorCounter();}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
