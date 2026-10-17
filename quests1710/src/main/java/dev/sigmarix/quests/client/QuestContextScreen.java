package dev.sigmarix.quests.client;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.opengl.GL11;
/** Reference context menu: 12 pixel rows, 8 pixel icons, and 3 pixel padding. */
final class QuestContextScreen extends QuestLibraryScreen {
    static class Entry {
        final String title,icon;final Runnable action;final boolean enabled;boolean closeMenu=true;java.util.function.BooleanSupplier enabledWhen;final List<String> tooltip=new ArrayList<String>();
        Entry(String title,String icon,Runnable action){this(title,icon,true,action);}
        boolean enabled(){return enabled && (enabledWhen==null || enabledWhen.getAsBoolean());}
        boolean separator(){return action==null && title.isEmpty();}
        void onClick(int button){action.run();}
        void drawIcon(int x,int y,int size,int color){if(icon.startsWith("lib:"))QuestClassicUi.libraryIcon(icon.substring(4),x,y,size,color);else if(icon.equals("settings") || icon.equals("download"))QuestClassicUi.libraryIcon(icon.equals("download")?"down":"settings",x,y,size,color);else QuestClassicUi.icon(icon.startsWith("theme:")?icon.substring(6):icon,x,y,size,color);}
        Entry(String title,String icon,boolean enabled,Runnable action){this.title=title;this.icon=icon;this.enabled=enabled;this.action=action;}
    }
    private final GuiScreen parent;private final List<Entry> entries;private final int anchorX,anchorY;
    private int left,top,w,h;private boolean icons;Runnable onRevisionChanged;private long revision;
    QuestContextScreen(GuiScreen parent,int x,int y,List<Entry> entries){this.parent=parent;anchorX=x;anchorY=y;this.entries=entries;}
    @Override public void initGui(){
        revision=ClientProxy.snapshot==null?0:ClientProxy.snapshot.revision;
        icons=false;for(Entry e:entries)if(!e.icon.isEmpty())icons=true;
        w=0;h=6;for(Entry e:entries){w=Math.max(w,fontRendererObj.getStringWidth(e.title)+(icons?14:4));h+=e.separator()?6:13;}if(!entries.isEmpty())h--;
        w+=6;left=Math.max(0,Math.min(anchorX,width-w));top=Math.max(0,Math.min(anchorY,height-h));
    }
    @Override public void drawScreen(int mx,int my,float partial){
        parent.drawScreen(-1,-1,partial);GL11.glPushMatrix();GL11.glTranslatef(0,0,900);
        Entry hovered=null;try{QuestClassicUi.context(mc,left,top,w,h);int y=top+3;
            for(Entry e:entries){if(e.separator()){drawRect(left+5,y+2,left+w-5,y+3,0x82FFFFFF);y+=6;continue;}
                boolean hover=mx>=left+3 && mx<left+w-3 && my>=y && my<y+12;
                if(hover) {hovered=e;QuestClassicUi.frame(left+3,y,w-6,12,QuestClassicUi.BORDER);drawRect(left+4,y+1,left+w-4,y+11,e.enabled()?0x44FFFFFF:0x44000000);}
                if(icons && !e.icon.isEmpty()){GL11.glPushMatrix();GL11.glTranslatef(0,0,-200);e.drawIcon(left+4,y+2,8,0xFFFFFFFF);GL11.glPopMatrix();}
                fontRendererObj.drawStringWithShadow(e.title,left+(icons?14:5),y+2,e.enabled()?0xFFFFFFFF:0xFF999999);y+=13;
            }
        }finally{GL11.glPopMatrix();}
        if(hovered!=null && !hovered.tooltip.isEmpty())drawHoveringText(hovered.tooltip,mx,my,fontRendererObj);
    }
    @Override protected void mouseClicked(int x,int y,int button){
        int cy=top+3;for(Entry e:entries){int rh=e.separator()?5:12;if(x>=left+3 && x<left+w-3 && y>=cy && y<cy+rh){if(e.enabled() && !e.separator()){if(e.closeMenu)mc.displayGuiScreen(parent);if(e.action!=null)e.onClick(button);}return;}cy+=rh+1;}
        if(x<left || x>=left+w || y<top || y>=top+h)mc.displayGuiScreen(parent);
    }
    @Override protected void keyTyped(char ch,int key){if(key==1)mc.displayGuiScreen(parent);}
    @Override public void updateScreen(){if(onRevisionChanged!=null && ClientProxy.snapshot!=null && revision!=ClientProxy.snapshot.revision){revision=ClientProxy.snapshot.revision;onRevisionChanged.run();}}
    @Override public boolean doesGuiPauseGame(){return false;}
}
