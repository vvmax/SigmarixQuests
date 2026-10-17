package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
/** Original reward overview geometry, backed by the bounded notification history. */
public final class QuestNotificationsScreen extends GuiScreen {
    private final GuiScreen parent;private int scroll;
    public QuestNotificationsScreen(GuiScreen parent){this.parent=parent;}
    @Override public void initGui(){buttonList.clear();String title=QuestText.tr("Close");int w=fontRendererObj.getStringWidth(title)+8;buttonList.add(new QuestFlatButton(0,(width-w)/2,height*2/3+16,w,20,title).left(4));}
    @Override protected void actionPerformed(GuiButton button){mc.displayGuiScreen(parent);}
    @Override public void drawScreen(int x,int y,float partial){
        GL11.glPushMatrix();GL11.glTranslated(width/2,height/5,0);GL11.glScaled(2,2,1);String title=QuestText.tr("Rewards");fontRendererObj.drawString(title,-fontRendererObj.getStringWidth(title)/2,0,0xFFFFFF);GL11.glPopMatrix();
        List<QuestNetwork.Toast> values=new ArrayList<QuestNetwork.Toast>();List<Integer> counts=new ArrayList<Integer>();Map<String,Integer> positions=new LinkedHashMap<String,Integer>();
        for(QuestNetwork.Toast toast:QuestToasts.history()){
            String key=toast.title+"\n"+toast.text+"\n"+toast.icon+":"+toast.iconMeta+toast.iconNbt;
            Integer index=positions.get(key);if(index==null){positions.put(key,values.size());values.add(toast);counts.add(1);}else counts.set(index,counts.get(index)+1);
        }
        int cols=Math.min(9,Math.max(1,values.size())),totalRows=(values.size()+cols-1)/cols,visible=Math.min(totalRows,Math.max(1,(height-90)/23));
        scroll=Math.max(0,Math.min(scroll,Math.max(0,totalRows-visible)));int gx=(width-cols*23+1)/2,gy=(height-visible*23+1)/2;
        QuestNetwork.Toast hover=null;
        for(int i=scroll*cols;i<values.size() && i<(scroll+visible)*cols;i++){
            QuestNetwork.Toast toast=values.get(i);int bx=gx+i%cols*23,by=gy+(i/cols-scroll)*23;
            QuestClassicUi.texture("ftbquests:textures/shapes/rsquare/outline.png",bx,by,22,22,0xFFFFFFFF);
            QuestClassicUi.item(mc,toast.icon,toast.iconMeta,toast.iconNbt,bx+3,by+3,16);
            int count=counts.get(i);if(count>1){String text=Integer.toString(count);GL11.glPushMatrix();GL11.glTranslatef(0,0,600);fontRendererObj.drawStringWithShadow(text,bx+22-fontRendererObj.getStringWidth(text),by+12,0xFFFF55);GL11.glPopMatrix();}
            if(x>=bx && x<bx+22 && y>=by && y<by+22)hover=toast;
        }
        super.drawScreen(x,y,partial);
        if(hover!=null)drawHoveringText(Arrays.asList(hover.task==null?hover.title:QuestTaskLabels.title(hover.task),"\u00a77"+QuestText.tr(hover.text)),x,y,fontRendererObj);
    }
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0)scroll+=wheel>0?-1:1;}
    @Override protected void keyTyped(char ch,int key){if(key==1)mc.displayGuiScreen(parent);else super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
