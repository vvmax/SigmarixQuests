package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;
/** Shared native quest shapes for the book and world displays. */
public final class QuestNodeRenderer extends Gui {
    public static void draw(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,int color) {
        if(QuestPresentation.image(shape)) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try { mc.getTextureManager().bindTexture(QuestImages.texture(shape)); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f); func_152125_a(x-radius,y-radius,0,0,256,256,radius*2,radius*2,256,256); }
            finally { GL11.glPopAttrib(); } return;
        }
        if(shape.equals("square")) { drawRect(x-radius,y-radius,x+radius,y+radius,color); return; }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT); GL11.glDisable(GL11.GL_CULL_FACE);
        int count=shape.equals("heart") || shape.equals("rsquare")?64:shape.equals("gear")?48:shape.equals("circle")?32:shape.equals("diamond")?4:shape.equals("pentagon")?5:shape.equals("hexagon")?6:8;
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f); GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2d(x,y);
        for(int i=0;i<=count;i++) {
            double angle=-Math.PI/2+Math.PI*2*i/count,dx=Math.cos(angle),dy=Math.sin(angle);
            if(shape.equals("rsquare")) { dx=Math.copySign(Math.sqrt(Math.abs(dx)),dx); dy=Math.copySign(Math.sqrt(Math.abs(dy)),dy); }
            else if(shape.equals("heart")) { double t=Math.PI*2*i/count; dx=Math.pow(Math.sin(t),3); dy=-(13*Math.cos(t)-5*Math.cos(2*t)-2*Math.cos(3*t)-Math.cos(4*t))/17D; }
            else if(shape.equals("gear")) { double gear=i%4<2?1:.76; dx*=gear; dy*=gear; }
            GL11.glVertex2d(x+dx*radius,y+dy*radius);
        }
        GL11.glEnd(); GL11.glColor4f(1,1,1,1); GL11.glDisable(GL11.GL_BLEND); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glPopAttrib();
    }
    private QuestNodeRenderer() {}
}
