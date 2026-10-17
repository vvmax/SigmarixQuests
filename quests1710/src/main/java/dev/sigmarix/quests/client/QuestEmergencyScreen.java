package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import net.minecraft.client.gui.*;import org.lwjgl.opengl.GL11;
/** Original fullscreen emergency-item countdown and centered item strip. */
final class QuestEmergencyScreen extends GuiScreen {
    private final GuiScreen parent;private final long end;private boolean sent;
    QuestEmergencyScreen(GuiScreen parent){this.parent=parent;end=System.currentTimeMillis()+Math.max(QuestTimer.emergency(ClientProxy.snapshot.book)*50L,ClientProxy.snapshot.emergencyWait*1000L);}
    @Override public void initGui(){String label=QuestText.tr(sent?"Close":"Cancel");int w=fontRendererObj.getStringWidth(label)+8;buttonList.clear();buttonList.add(new QuestFlatButton(0,(width-w)/2,height*2/3+16,w,20,label).left(4));}
    @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();long millis=Math.max(0,end-System.currentTimeMillis());if(millis==0 && !sent){sent=true;QuestNetwork.Action action=new QuestNetwork.Action();action.action="emergency";ClientProxy.send(action);initGui();}
        GL11.glPushMatrix();GL11.glTranslated(width/2,height/5,0);GL11.glScaled(2,2,1);String title=net.minecraft.util.StatCollector.translateToLocal("ftbquests.file.emergency_items");fontRendererObj.drawString(title,-fontRendererObj.getStringWidth(title)/2,0,0xFFFFFF);GL11.glPopMatrix();
        long seconds=millis==0?0:millis/1000+1;String time=seconds>=3600?(seconds>=86400?seconds/86400+"d ":"")+String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",seconds/3600%24,seconds/60%60,seconds%60):String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
        GL11.glPushMatrix();GL11.glTranslated(width/2,(int)(height/2.5),0);GL11.glScaled(4,4,1);int tx=-fontRendererObj.getStringWidth(time)/2;fontRendererObj.drawString(time,tx-1,0,0xFF000000);fontRendererObj.drawString(time,tx+1,0,0xFF000000);fontRendererObj.drawString(time,tx,-1,0xFF000000);fontRendererObj.drawString(time,tx,1,0xFF000000);fontRendererObj.drawString(time,tx,0,0xFFFFFF);GL11.glPopMatrix();
        java.util.List<QuestCore.Reward> items=ClientProxy.snapshot.book.emergencyItems;int start=(width-(items.size()*16+Math.max(0,items.size()-1)*7+6))/2;
        for(int i=0;i<items.size();i++){QuestCore.Reward r=items.get(i);int x=start+3+i*23,y=height*2/3-7;QuestClassicUi.texture("ftbquests:textures/shapes/rsquare/outline.png",x-3,y-3,22,22,0xFFFFFFFF);QuestClassicUi.reward(mc,r,x,y,16);if(mx>=x && mx<x+16 && my>=y && my<y+16)drawHoveringText(java.util.Collections.singletonList(QuestTaskLabels.reward(r)),mx,my,fontRendererObj);}super.drawScreen(mx,my,partial);
    }
    @Override protected void actionPerformed(GuiButton button){mc.displayGuiScreen(parent);}
    @Override protected void keyTyped(char ch,int key){if(key==1)mc.displayGuiScreen(parent);else super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
