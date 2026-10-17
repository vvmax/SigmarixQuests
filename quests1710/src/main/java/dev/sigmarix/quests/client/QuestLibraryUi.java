package dev.sigmarix.quests.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** FTBLib's default widget geometry, distinct from the quest book theme. */
final class QuestLibraryUi extends Gui {
    static void patch(String sheet,int u,int v,int sw,int sh,int border,int x,int y,int w,int h) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("sigmarixquests:textures/reference/"+sheet+".png"));
            GL11.glColor4f(1,1,1,1); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            int[] dx={x,x+border,x+w-border,x+w},dy={y,y+border,y+h-border,y+h};
            int[] sx={u,u+border,u+sw-border,u+sw},sy={v,v+border,v+sh-border,v+sh};
            Tessellator t=Tessellator.instance;t.startDrawingQuads();
            for(int row=0;row<3;row++) for(int col=0;col<3;col++) {
                t.addVertexWithUV(dx[col],dy[row+1],0,sx[col]/256D,sy[row+1]/256D);
                t.addVertexWithUV(dx[col+1],dy[row+1],0,sx[col+1]/256D,sy[row+1]/256D);
                t.addVertexWithUV(dx[col+1],dy[row],0,sx[col+1]/256D,sy[row]/256D);
                t.addVertexWithUV(dx[col],dy[row],0,sx[col]/256D,sy[row]/256D);
            } t.draw();
        } finally {GL11.glPopAttrib();}
    }
    static void ghost(String text,int x,int y){GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);try{GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);Minecraft.getMinecraft().fontRenderer.drawString("\u00a7o"+text,x,y,0x78FFFFFF);}finally{GL11.glPopAttrib();}}
    static void window(int x,int y,int w,int h) {patch("recipe_book",82,208,32,32,8,x,y,w,h);}
    static void slot(int x,int y,int w,int h) {patch("container_beacon",35,136,18,18,3,x-1,y-1,w+2,h+2);}
    static void textBox(int x,int y,int w,int h) {patch("container_enchanting_table",0,185,108,19,6,x,y,w,h);}
    static class Button extends GuiButton {
        final String icon;private boolean centered=true;
        Button(int id,int x,int y,int w,int h,String title,String icon) {super(id,x,y,w,h,title);this.icon=icon;}
        Button left(){centered=false;return this;}
        @Override public void drawButton(Minecraft mc,int mx,int my) {
            if(!visible)return; field_146123_n=mx>=xPosition && my>=yPosition && mx<xPosition+width && my<yPosition+height;
            patch("widgets",0,!enabled?46:field_146123_n?86:66,200,20,4,xPosition,yPosition,width,height);
            int color=!enabled?0xFFA0A0A0:field_146123_n?0xFFFFFFA0:0xFFFFFFFF;
            int off=(height-16)/2,tx=xPosition+4;
            if(!icon.isEmpty()) {QuestClassicUi.libraryIcon(icon,xPosition+off,yPosition+off,16,color);tx+=off+16;}
            String title=mc.fontRenderer.trimStringToWidth(displayString,width-(tx-xPosition)-4);
            if(icon.isEmpty() && centered)tx=xPosition+(width-mc.fontRenderer.getStringWidth(title))/2;
            mc.fontRenderer.drawStringWithShadow(title,tx,yPosition+(height-9+1)/2,color);
        }
    }
    private QuestLibraryUi() {}
}
