package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;
/** Shared native quest shapes for the book and world displays. */
public final class QuestNodeRenderer extends Gui {
    private static final java.util.Map<String,java.awt.image.BufferedImage> hitMasks=new java.util.HashMap<String,java.awt.image.BufferedImage>();
    public static boolean contains(net.minecraft.client.Minecraft mc,int centerX,int centerY,int radius,String shape,int x,int y){
        if(x<centerX-radius || y<centerY-radius || x>=centerX+radius || y>=centerY+radius)return false;
        if(QuestPresentation.image(shape))return true;
        if(!hitMasks.containsKey(shape)){try(java.io.InputStream stream=mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("ftbquests:textures/shapes/"+shape+"/shape.png")).getInputStream()){hitMasks.put(shape,javax.imageio.ImageIO.read(stream));}catch(java.io.IOException missing){hitMasks.put(shape,null);}}
        java.awt.image.BufferedImage mask=hitMasks.get(shape);if(mask==null)return true;int px=(int)((x-centerX+radius)/(double)(radius*2)*mask.getWidth()),py=(int)((y-centerY+radius)/(double)(radius*2)*mask.getHeight());return (mask.getRGB(px,py)>>>24)>=5;
    }
    public static void draw(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,int color) {
        if(QuestPresentation.image(shape)) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try { mc.getTextureManager().bindTexture(QuestImages.texture(shape)); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f); func_152125_a(x-radius,y-radius,0,0,256,256,radius*2,radius*2,256,256); }
            finally { GL11.glPopAttrib(); } return;
        }
        layer(mc,x,y,radius,shape,"shape",color);
    }
    public static void node(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,int outline) {
        if(QuestPresentation.image(shape)){draw(mc,x,y,radius,shape,outline);return;}
        layer(mc,x,y,radius,shape,"shape",0xFF212121);
        layer(mc,x,y,radius,shape,"background",0x96FFFFFF);
        layer(mc,x,y,radius,shape,"outline",outline);
    }
    public static void highlight(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,int color){
        if(QuestPresentation.image(shape)){draw(mc,x,y,radius,shape,color);return;}
        layer(mc,x,y,radius,shape,"outline",color);layer(mc,x,y,radius,shape,"background",color);
    }
    private static void layer(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,String layer,int color) {
        if(!java.util.Arrays.asList("square","rsquare","circle","diamond","pentagon","hexagon","octagon","heart","gear").contains(shape))shape="square";
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("ftbquests:textures/shapes/"+shape+"/"+layer+".png"));
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.setColorRGBA(color>>16&255,color>>8&255,color&255,color>>>24);
            t.addVertexWithUV(x-radius,y+radius,0,0,1);t.addVertexWithUV(x+radius,y+radius,0,1,1);t.addVertexWithUV(x+radius,y-radius,0,1,0);t.addVertexWithUV(x-radius,y-radius,0,0,0);t.draw();
        }finally{GL11.glPopAttrib();}
    }
    private QuestNodeRenderer() {}
}
