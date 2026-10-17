package dev.sigmarix.quests.client;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
/** FTBLib's padded player face and two status borders. */
final class QuestPlayerHeads extends Gui {
    static void draw(String uuid,String name,int x,int y,int size,int border,boolean team){
        ResourceLocation texture;int th=64;boolean alex=false;try{alex=(UUID.fromString(uuid).hashCode()&1)!=0;}catch(IllegalArgumentException ignored){}
        texture=new ResourceLocation("sigmarixquests:textures/reference/"+(alex?"alex":"steve")+".png");
        Minecraft mc=Minecraft.getMinecraft();if(mc.theWorld!=null)for(Object raw:mc.theWorld.playerEntities)if(raw instanceof AbstractClientPlayer && ((AbstractClientPlayer)raw).getUniqueID().toString().equals(uuid)){texture=((AbstractClientPlayer)raw).getLocationSkin();th=32;break;}
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try{GL11.glTranslatef(0,0,200);int inset=team?1:4;
            if(team)QuestClassicUi.frame(x,y,size,size,border);else{int bx=x+2,by=y+2,bw=size-4;drawRect(bx+1,by,bx+bw-1,by+1,0xFF212121);drawRect(bx+1,by+bw-1,bx+bw-1,by+bw,0xFF212121);drawRect(bx,by+1,bx+1,by+bw-1,0xFF212121);drawRect(bx+bw-1,by+1,bx+bw,by+bw-1,0xFF212121);QuestClassicUi.frame(x+3,y+3,size-6,size-6,border);}mc.getTextureManager().bindTexture(texture);GL11.glColor4f(1,1,1,1);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);func_152125_a(x+inset,y+inset,8,8,8,8,size-inset*2,size-inset*2,64,th);func_152125_a(x+inset,y+inset,40,8,8,8,size-inset*2,size-inset*2,64,th);}finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
}
