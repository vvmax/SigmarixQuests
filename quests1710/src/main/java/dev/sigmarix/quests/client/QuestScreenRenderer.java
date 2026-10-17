package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;
/** Independent in-world quest display using server-synchronized title and progress. */
public final class QuestScreenRenderer extends TileEntitySpecialRenderer {
    @Override public void renderTileEntityAt(TileEntity raw,double x,double y,double z,float partial) {
        QuestDevice tile=(QuestDevice)raw;
        if(tile.screenLinked && QuestScreens.core(tile)!=null) return;
        if(!(tile.getBlockType() instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)tile.getBlockType()).mode>1) return;
        float lightX=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX,lightY=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            GL11.glTranslated(x+.5,y+.5,z+.5);
            GL11.glRotatef(tile.facing==2?180:tile.facing==4?270:tile.facing==5?90:0,0,1,0);
            GL11.glTranslated(-.46,tile.screenHeight-.54,.502); GL11.glScaled((tile.screenWidth-.08)/128,-(tile.screenHeight-.08)/128,.92/128);
            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,240,240);
            GL11.glColor4f(1,1,1,1);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            Gui.drawRect(0,0,128,128,0xFF152332); Gui.drawRect(0,0,128,3,0xFF62B5D6);
            FontRenderer font=Minecraft.getMinecraft().fontRenderer;
            font.drawSplitString(font.trimStringToWidth(tile.screenTitle,112),8,12,112,0xFFFFFF);
            font.drawString(QuestText.tr(((QuestBlocks.DeviceBlock)tile.getBlockType()).mode==0?"Task":"Progress"),8,42,0xB8CDD9);
            GL11.glPushMatrix(); GL11.glTranslatef(8,62,0); GL11.glScalef(2,2,1); font.drawString(tile.screenPercent+"%",0,0,0xFFFFFF); GL11.glPopMatrix();
            Gui.drawRect(8,96,120,108,0xFF0A131F); Gui.drawRect(9,97,9+tile.screenPercent*110/100,107,0xFF60B978);
            font.drawString(tile.signal?"✓":"...",8,114,0xB8CDD9);
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,lightX,lightY); }
    }
}
