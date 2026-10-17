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
        layer(mc,x,y,radius,shape,"shape",color);
    }
    public static void node(net.minecraft.client.Minecraft mc,int x,int y,int radius,String shape,int outline) {
        if(QuestPresentation.image(shape)){draw(mc,x,y,radius,shape,outline);return;}
        layer(mc,x,y,radius,shape,"shape",0xFF404040);
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
            GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f);
            func_152125_a(x-radius,y-radius,0,0,256,256,radius*2,radius*2,256,256);
        }finally{GL11.glPopAttrib();}
    }
    private QuestNodeRenderer() {}
}
