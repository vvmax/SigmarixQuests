package dev.sigmarix.quests.client;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
/** Fullscreen FTBLib property editor: 20px title strip, 12px entries and group rows. */
abstract class QuestConfigScreen extends QuestLibraryScreen {
    static final class Entry {
        boolean enabled=true;final String group,label;final Supplier<String> value;final IntSupplier color;final BiConsumer<Integer,GuiScreen> edit;final List<String> info=new ArrayList<String>();
        Entry(String group,String label,Supplier<String> value,IntSupplier color,BiConsumer<Integer,GuiScreen> edit){this.group=group;this.label=label;this.value=value;this.color=color;this.edit=edit;}
    }
    private final GuiScreen parent;private List<Entry> all=new ArrayList<Entry>();private final Set<String> collapsed=new HashSet<String>();private final List<Object> displayed=new ArrayList<Object>();private int scroll;private String error="";private boolean dragging;
    QuestConfigScreen(GuiScreen parent){this.parent=parent;}
    protected void closed(boolean applied){}
    protected abstract String title();protected abstract List<Entry> entries();protected abstract void save();
    @Override public void initGui(){all=entries();rebuild();}
    private void rebuild(){Set<String> groups=new LinkedHashSet<String>();for(Entry entry:all)groups.add(entry.group);displayed.clear();String group=null;for(Entry entry:all){if(!entry.group.equals(group)){group=entry.group;if(groups.size()>1)displayed.add(group);}if(!collapsed.contains(group))displayed.add(entry);}scroll=Math.max(0,Math.min(scroll,Math.max(0,displayed.size()*12-height+20)));buttonList.clear();buttonList.add(new QuestLibraryUi.IconButton(0,width-18,2,"accept"));buttonList.add(new QuestLibraryUi.IconButton(1,width-38,2,"cancel"));if(groups.size()>1){buttonList.add(new QuestLibraryUi.IconButton(2,width-58,2,"add"));buttonList.add(new QuestLibraryUi.IconButton(3,width-78,2,"remove"));}}
    private void close(boolean apply){if(apply)try{save();}catch(RuntimeException ex){error=QuestText.tr(ex.getMessage());return;}closed(apply);mc.displayGuiScreen(parent);}
    @Override protected void actionPerformed(GuiButton button){if(button.id<=1){close(button.id==0);return;}if(button.id==2)collapsed.clear();else{for(Entry entry:all)collapsed.add(entry.group);}rebuild();}
    @Override public void drawScreen(int mx,int my,float partial){
        drawDefaultBackground();drawRect(0,0,width,20,0x99333333);fontRendererObj.drawStringWithShadow("\u00a7l"+title(),6,6,0xFFFFFF);
        int track=height-20,max=Math.max(0,displayed.size()*12-track),thumb=max==0?track:Math.max(8,track*track/(max+track)),sy=20+(max==0?0:scroll*(track-thumb)/max);
        drawRect(width-16,20,width,height,0x46000000);if(max>0){drawRect(width-15,sy+1,width-1,sy+thumb-1,0x64FFFFFF);QuestClassicUi.frame(width-16,sy,16,thumb,0x64999999);}
        ScaledResolution res=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);int f=res.getScaleFactor();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(0,0,(width-16)*f,(res.getScaledHeight()-20)*f);Entry hover=null;
        try{for(int i=0;i<displayed.size();i++){int y=20+i*12-scroll;if(y+12<=20 || y>=height)continue;Object value=displayed.get(i);boolean over=mx<width-16 && my>=Math.max(20,y) && my<y+12;
            if(value instanceof String){String group=(String)value;drawRect(0,y,width-16,y+12,0x99333333);fontRendererObj.drawString((collapsed.contains(group)?"\u00a7c[-] ":"\u00a7a[v] ")+"\u00a7r"+group,2,y+2,0xFFFFFF);if(over)drawRect(0,y,width-16,y+12,0x21FFFFFF);continue;}
            Entry entry=(Entry)value;if(over){hover=entry;drawRect(0,y,width-16,y+12,0x21FFFFFF);}fontRendererObj.drawString((entry.enabled?"":"\u00a77")+entry.label,4,y+2,0xFFFFFF,over);
            String text=entry.value.get();int tw=fontRendererObj.getStringWidth(text);if(tw>150){text=fontRendererObj.trimStringToWidth(text,150)+"...";tw=152;}int color=entry.color.getAsInt();if(over){color=(Math.min(255,(color>>16&255)+60)<<16)|(Math.min(255,(color>>8&255)+60)<<8)|Math.min(255,(color&255)+60);if(mx>width-16-tw-9)drawRect(width-16-tw-8,y,width-16,y+12,0x21FFFFFF);}fontRendererObj.drawString(text,width-tw-20,y+2,color);
        }}finally{GL11.glPopAttrib();}super.drawScreen(mx,my,partial);
        if(hover!=null){List<String> lines=new ArrayList<String>();lines.add("\u00a7n"+hover.label);lines.addAll(hover.info);drawHoveringText(lines,mx,my,fontRendererObj);}
        if(!error.isEmpty())drawCenteredString(fontRendererObj,error,width/2,height-12,0xFFFF5555);
    }
    @Override protected void mouseClicked(int x,int y,int button){if(y<20){super.mouseClicked(x,y,button);return;}if(x>=width-16){dragging=button==0;drag(y);return;}int i=(y-20+scroll)/12;if(i<0 || i>=displayed.size())return;Object value=displayed.get(i);if(value instanceof String){String group=(String)value;if(!collapsed.add(group))collapsed.remove(group);rebuild();}else{Entry entry=(Entry)value;if(entry.enabled)entry.edit.accept(button,this);rebuild();}}
    private void drag(int y){int track=height-20,max=Math.max(0,displayed.size()*12-track);scroll=Math.max(0,Math.min(max,(int)((y-20)/(double)Math.max(1,track)*max)));}
    @Override protected void mouseClickMove(int x,int y,int button,long time){if(dragging)drag(y);}
    @Override protected void mouseMovedOrUp(int x,int y,int button){super.mouseMovedOrUp(x,y,button);if(button==0)dragging=false;}
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll+=wheel>0?-20:20;rebuild();}}
    @Override protected void keyTyped(char ch,int key){if(key==1)close(true);else super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
