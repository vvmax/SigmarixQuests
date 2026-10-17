package dev.sigmarix.quests.client;
import java.util.*;import java.util.function.*;
import net.minecraft.client.gui.*;import org.lwjgl.input.Mouse;import org.lwjgl.opengl.GL11;
/** FTBLib's fullscreen list editor: 12px values, per-row delete, and a final Add row. */
final class QuestValueListScreen<E> extends QuestLibraryScreen {
    interface Editor<E>{void open(GuiScreen parent,E initial,Consumer<E> accept);}
    private final GuiScreen parent;private final String title;private final List<E> source,draft;private final Supplier<E> fresh;private final Function<E,String> display;private final Editor<E> edit;private final int color;private int scroll;private Runnable accepted=()->{};
    QuestValueListScreen<E> onAccepted(Runnable callback){accepted=callback;return this;}
    QuestValueListScreen(GuiScreen parent,String title,List<E> source,Supplier<E> fresh,Function<E,String> display,Editor<E> edit,int color){this.parent=parent;this.title=title;this.source=source;draft=new ArrayList<E>(source);this.fresh=fresh;this.display=display;this.edit=edit;this.color=color;}
    @Override public void initGui(){buttonList.clear();buttonList.add(new QuestLibraryUi.IconButton(0,width-18,2,"accept"));buttonList.add(new QuestLibraryUi.IconButton(1,width-38,2,"cancel"));clamp();}
    private void clamp(){scroll=Math.max(0,Math.min(scroll,Math.max(0,(draft.size()+1)*12-height+20)));}
    @Override protected void actionPerformed(GuiButton button){if(button.id==0){source.clear();source.addAll(draft);accepted.run();}mc.displayGuiScreen(parent);}
    @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();drawRect(0,0,width,20,0x99333333);fontRendererObj.drawStringWithShadow("\u00a7l"+title,6,6,0xFFFFFF);int max=Math.max(0,(draft.size()+1)*12-height+20),track=height-20,thumb=max==0?track:Math.max(8,track*track/(max+track)),sy=20+(max==0?0:scroll*(track-thumb)/max);drawRect(width-16,20,width,height,0x46000000);if(max>0){drawRect(width-15,sy+1,width-1,sy+thumb-1,0x64FFFFFF);QuestClassicUi.frame(width-16,sy,16,thumb,0x64999999);}
        ScaledResolution resolution=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);int f=resolution.getScaleFactor();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(0,0,(width-16)*f,(resolution.getScaledHeight()-20)*f);
        try{for(int i=0;i<=draft.size();i++){int y=20+i*12-scroll;if(y+12<=20 || y>=height)continue;boolean over=mx<width-16 && my>=Math.max(20,y) && my<y+12;if(over)drawRect(0,y,width-16,y+12,0x21FFFFFF);if(i==draft.size())fontRendererObj.drawStringWithShadow("+ "+net.minecraft.util.StatCollector.translateToLocal("gui.add"),4,y+2,0xFFFFFF);else{int c=over?(Math.min(255,(color>>16&255)+60)<<16)|(Math.min(255,(color>>8&255)+60)<<8)|Math.min(255,(color&255)+60):color;String value=fontRendererObj.trimStringToWidth(display.apply(draft.get(i)),width-40);fontRendererObj.drawString(value,4,y+2,c);if(over){if(mx>=width-35)drawRect(width-35,y,width-16,y+12,0x21FFFFFF);fontRendererObj.drawString("[-]",width-32,y+2,0xFFFFFF);}}}}finally{GL11.glPopAttrib();}super.drawScreen(mx,my,partial);}
    @Override protected void mouseClicked(int x,int y,int button){if(y<20){super.mouseClicked(x,y,button);return;}if(x>=width-16){int max=Math.max(0,(draft.size()+1)*12-height+20);scroll=Math.max(0,Math.min(max,(y-20)*max/Math.max(1,height-20)));return;}int index=(y-20+scroll)/12;if(index<0 || index>draft.size())return;if(index==draft.size()){draft.add(fresh.get());clamp();return;}if(x>=width-35){draft.remove(index);clamp();}else edit.open(this,draft.get(index),value->draft.set(index,value));}
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll+=wheel>0?-20:20;clamp();}}
    @Override protected void keyTyped(char ch,int key){if(key==1)mc.displayGuiScreen(parent);else super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
