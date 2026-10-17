package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
/** Cuboid models and separate tint layers used by the original devices and loot crate. */
final class QuestModelItemRenderer implements IItemRenderer {
    private final int mode;
    QuestModelItemRenderer(int mode){this.mode=mode;}
    @Override public boolean handleRenderType(ItemStack stack,ItemRenderType type){return true;}
    @Override public boolean shouldUseRenderHelper(ItemRenderType type,ItemStack stack,ItemRendererHelper helper){return helper==ItemRendererHelper.ENTITY_ROTATION || helper==ItemRendererHelper.ENTITY_BOBBING;}
    @Override public void renderItem(ItemRenderType type,ItemStack stack,Object... data){
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDepthFunc(GL11.GL_LEQUAL);
            boolean inventory=type==ItemRenderType.INVENTORY;
            if(inventory){GL11.glTranslatef(8,8,0);GL11.glScalef(10,-10,10);GL11.glRotatef(30,1,0,0);GL11.glRotatef(225,0,1,0);GL11.glTranslatef(-.5f,-.5f,-.5f);GL11.glDisable(GL11.GL_LIGHTING);}
            else if(type==ItemRenderType.ENTITY){GL11.glScalef(.5f,.5f,.5f);GL11.glTranslatef(-.5f,0,-.5f);}
            if(mode<0){
                box("items/lootcrate/",true,0xFFFFFF);
                box("items/lootcrate/",false,stack.getItem().getColorFromItemStack(stack,0));
            } else box("blocks/",false,0xFFFFFF);
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    private void box(String prefix,boolean outline,int color){
        boolean chest=mode<0 || mode==7;double a=chest?1/16D:0,b=chest?15/16D:1,h=chest?14/16D:1;
        String side,top,front;
        if(mode<0){side="side";top="top";front="front";if(outline){side+="_outline";top+="_outline";front+="_outline";}}
        else if(mode==7){side="chest_side";top="chest_top";front="chest_front";}
        else if(mode<=1){side="screen_side";top=side;front="screen_front";}
        else{side=new String[]{"","","barrier","reward_collector","redstone_detector","player_detector","loot_crate_opener","","loot_crate_storage","progress_detector"}[mode];top=side;front=side;}
        face(prefix+top,color,.5f,0,-1,0,new double[][]{{a,0,b},{a,0,a},{b,0,a},{b,0,b}},a,a,b,b);
        face(prefix+top,color,1,0,1,0,new double[][]{{a,h,a},{a,h,b},{b,h,b},{b,h,a}},a,a,b,b);
        face(prefix+front,color,.8f,0,0,-1,new double[][]{{b,0,a},{a,0,a},{a,h,a},{b,h,a}},a,1-h,b,1);
        face(prefix+side,color,.8f,0,0,1,new double[][]{{a,0,b},{b,0,b},{b,h,b},{a,h,b}},a,1-h,b,1);
        face(prefix+side,color,.6f,-1,0,0,new double[][]{{a,0,a},{a,0,b},{a,h,b},{a,h,a}},a,1-h,b,1);
        face(prefix+side,color,.6f,1,0,0,new double[][]{{b,0,b},{b,0,a},{b,h,a},{b,h,b}},a,1-h,b,1);
    }
    private void face(String texture,int color,float shade,float nx,float ny,float nz,double[][] points,double u1,double v1,double u2,double v2){
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("ftbquests:textures/"+texture+".png"));
        Tessellator t=Tessellator.instance;t.startDrawingQuads();t.setNormal(nx,ny,nz);t.setColorOpaque_F((color>>16&255)/255f*shade,(color>>8&255)/255f*shade,(color&255)/255f*shade);
        double[][] uv={{u2,v2},{u1,v2},{u1,v1},{u2,v1}};
        for(int i=0;i<4;i++)t.addVertexWithUV(points[i][0],points[i][1],points[i][2],uv[i][0],uv[i][1]);t.draw();
    }
}
