package dev.sigmarix.quests.client;
import net.minecraft.client.gui.*;
/** Compact confirmation dialog shared by team actions. */
final class QuestConfirmScreen extends QuestLibraryScreen {
    private final GuiScreen parent;private final String title;private final Runnable accept;private int left,top,w;
    QuestConfirmScreen(GuiScreen parent,String title,Runnable accept){this.parent=parent;this.title=title;this.accept=accept;}
    @Override public void initGui(){w=Math.min(width-16,Math.max(160,fontRendererObj.getStringWidth(title)+16));left=(width-w)/2;top=(height-54)/2;buttonList.clear();buttonList.add(new QuestLibraryUi.Button(1,left+8,top+30,w/2-10,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(0,left+w/2+2,top+30,w/2-10,16,QuestText.tr("Accept"),""));}
    @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();QuestLibraryUi.window(left,top,w,54);drawCenteredString(fontRendererObj,title,width/2,top+10,0xFFFFFF);super.drawScreen(x,y,partial);}
    @Override protected void actionPerformed(GuiButton button){mc.displayGuiScreen(parent);if(button.id==0)accept.run();}
    @Override protected void keyTyped(char ch,int key){if(key==1)mc.displayGuiScreen(parent);else super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
