package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
/** Independent resource-pack icon sprite for inventories, graph nodes and held items. */
public final class QuestIconRenderer implements IItemRenderer {
    @Override public boolean handleRenderType(ItemStack stack,ItemRenderType type) { return true; }
    @Override public boolean shouldUseRenderHelper(ItemRenderType type,ItemStack stack,ItemRendererHelper helper) { return helper==ItemRendererHelper.ENTITY_ROTATION || helper==ItemRendererHelper.ENTITY_BOBBING; }
    @Override public void renderItem(ItemRenderType type,ItemStack stack,Object... data) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            boolean inventory=type==ItemRenderType.INVENTORY; double size=inventory?16:1;
            if(type==ItemRenderType.ENTITY) { GL11.glScalef(.5f,.5f,.5f); GL11.glTranslatef(-.5f,0,0); }
            Minecraft.getMinecraft().getTextureManager().bindTexture(QuestImages.texture(QuestIcons.resource(stack)));
            int color=QuestIcons.color(stack); GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            double top=inventory?0:1,bottom=inventory?1:0; Tessellator tessellator=Tessellator.instance; tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0,0,0,0,top); tessellator.addVertexWithUV(0,size,0,0,bottom); tessellator.addVertexWithUV(size,size,0,1,bottom); tessellator.addVertexWithUV(size,0,0,1,top); tessellator.draw();
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
}
