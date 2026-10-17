package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Independently drawn classic widgets; no upstream textures are embedded. */
public final class QuestClassicUi extends Gui {
    public static final int BORDER=0xFF1B1D1E, TEXT=0xFFFFFFFF;
    private static ResourceLocation background,dependency;
    private static final QuestPresentation.Theme defaults=new QuestPresentation.Theme();
    private static final RenderItem items=new RenderItem();
    public static void background(Minecraft mc,int width,int height) { background(mc,0,0,width,height); }
    public static void background(Minecraft mc,int x,int y,int width,int height) {
        if(ClientProxy.snapshot!=null && ClientProxy.snapshot.book!=null && !ClientProxy.snapshot.book.theme.background.equals(defaults.background)) { drawRect(x,y,x+width,y+height,QuestPresentation.color(ClientProxy.snapshot.book.theme.background)); return; }
        if(background==null) {
            DynamicTexture texture=new DynamicTexture(64,64); int[] pixels=texture.getTextureData();
            java.util.Random noise=new java.util.Random(1710);
            for(int py=0;py<64;py++) for(int px=0;px<64;px++) {
                int shade=noise.nextInt(3)-1; if(px%4==0 || py%4==0) shade-=2;
                pixels[py*64+px]=0xFF000000|((65+shade)<<16)|((72+shade)<<8)|(82+shade);
            }
            texture.updateDynamicTexture(); background=mc.getTextureManager().getDynamicTextureLocation("quest_classic_grid",texture);
        }
        GL11.glColor4f(1,1,1,1); mc.getTextureManager().bindTexture(background); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
        net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;
        t.startDrawingQuads(); t.addVertexWithUV(x,y+height,0,0,height/64D); t.addVertexWithUV(x+width,y+height,0,width/64D,height/64D); t.addVertexWithUV(x+width,y,0,width/64D,0); t.addVertexWithUV(x,y,0,0,0); t.draw();
    }
    public static void bindDependencies(Minecraft mc) {
        if(dependency==null) {
            DynamicTexture texture=new DynamicTexture(32,16); int[] data=texture.getTextureData();
            for(int y=0;y<16;y++) for(int x=0;x<32;x++) { double center=30-Math.abs((y-7.5)/7.5)*30,distance=Math.abs(x-center); int alpha=(int)(255*Math.max(0,1-Math.max(0,distance-.6)/2.5)); data[y*32+x]=alpha<<24|0xFFFFFF; }
            texture.updateDynamicTexture(); dependency=mc.getTextureManager().getDynamicTextureLocation("quest_dependency_chevrons",texture);
        }
        mc.getTextureManager().bindTexture(dependency); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_CLAMP);
    }
    public static void frame(int x,int y,int width,int height,int color) {
        drawRect(x,y,x+width,y+1,color); drawRect(x,y+height-1,x+width,y+height,color);
        drawRect(x,y,x+1,y+height,color); drawRect(x+width-1,y,x+width,y+height,color);
    }
    public static void panel(Minecraft mc,int x,int y,int width,int height) {
        background(mc,x,y,width,height); drawRect(x+1,y+1,x+width-1,y+height-1,ClientProxy.snapshot!=null && !ClientProxy.snapshot.book.theme.panel.equals(defaults.panel)?QuestPresentation.color(ClientProxy.snapshot.book.theme.panel):0x4A000000); frame(x,y,width,height,BORDER);
    }
    public static void item(Minecraft mc,String id,int meta,String nbt,int x,int y,int size) {
        Item item=id==null?null:(Item)Item.itemRegistry.getObject(id); if(item==null) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try { ItemStack stack=new ItemStack(item,1,meta); stack.setTagCompound(QuestService.tag(nbt)); GL11.glTranslated(x,y,100); GL11.glScaled(size/16D,size/16D,1); RenderHelper.enableGUIStandardItemLighting(); items.renderItemAndEffectIntoGUI(mc.fontRenderer,mc.getTextureManager(),stack,0,0); }
        catch(RuntimeException ignored) {} finally { RenderHelper.disableStandardItemLighting(); GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    public static String taskItem(QuestCore.Task task) {
        if(!task.icon.isEmpty()) return task.icon;
        if(task.type.equals("item")) { if(task.target!=null && !task.target.isEmpty() && !task.target.startsWith("ore:")) return task.target; if(!task.targets.isEmpty()) return task.targets.get(0); }
        if(task.type.equals("xp") || task.type.equals("emc")) return "minecraft:experience_bottle";
        if(task.type.equals("kill")) return "minecraft:iron_sword";
        if(task.type.equals("fluid")) return "minecraft:water_bucket";
        if(task.type.equals("dimension") || task.type.equals("location") || task.type.equals("biome")) return "minecraft:compass";
        return "";
    }
    public static String rewardItem(QuestCore.Reward reward) {
        if(!reward.icon.isEmpty()) return reward.icon;
        if(reward.type.equals("item")) return reward.item;
        if(reward.type.equals("xp") || reward.type.equals("xp_levels")) return "minecraft:experience_bottle";
        if(reward.type.equals("loot") || reward.type.equals("choice") || reward.type.equals("random")) return "minecraft:chest";
        if(reward.type.equals("command")) return "minecraft:command_block";
        return "minecraft:paper";
    }
    /** Small geometry icons remain sharp at every GUI scale. */
    public static void icon(String kind,int x,int y,int size,int color) {
        GL11.glPushMatrix(); GL11.glTranslated(x,y,200); GL11.glScaled(size/16D,size/16D,1);
        if(kind.equals("close")) { for(int i=3;i<=12;i++) { drawRect(i,i,i+2,i+2,color); drawRect(15-i,i,17-i,i+2,color); } }
        else if(kind.equals("check")) { for(int i=0;i<4;i++) drawRect(2+i,8+i,4+i,10+i,color); for(int i=0;i<8;i++) drawRect(5+i,11-i,7+i,13-i,color); }
        else if(kind.equals("alert")) { drawRect(7,2,10,10,color); drawRect(7,12,10,15,color); }
        else if(kind.equals("pin")) { drawRect(5,2,12,4,color); drawRect(6,4,11,9,color); drawRect(3,9,14,11,color); drawRect(8,11,9,16,color); }
        else if(kind.equals("add")) { drawRect(7,2,10,15,color); drawRect(2,7,15,10,color); }
        else if(kind.equals("left") || kind.equals("right")) { for(int i=0;i<6;i++) { int cx=kind.equals("left")?4+i:10-i; drawRect(cx,7-i,cx+2,9+i,color); } }
        else if(kind.equals("settings")) { gear(4.7,4.7,4.6,9,color); gear(11.5,13,7.5,10,color); }
        else if(kind.equals("search")) { frame(2,2,9,9,color); drawRect(10,10,12,12,color); drawRect(12,12,15,15,color); }
        else if(kind.equals("book")) { drawRect(2,2,14,14,color); drawRect(3,3,7,12,0xFF414852); drawRect(8,3,13,12,0xFF414852); }
        else if(kind.equals("reward")) { frame(2,6,13,9,color); frame(1,4,15,3,color); drawRect(7,4,9,15,color); frame(4,1,4,4,color); frame(9,1,4,4,color); }
        else if(kind.equals("refresh")) { frame(3,3,10,10,color); drawRect(1,2,7,4,color); drawRect(1,2,3,8,color); }
        else if(kind.equals("team")) { drawRect(3,2,7,6,color); drawRect(9,3,13,7,color); drawRect(1,8,9,14,color); drawRect(9,9,15,14,color); }
        else { frame(2,2,12,12,color); drawRect(7,5,9,11,color); }
        GL11.glPopMatrix(); GL11.glColor4f(1,1,1,1);
    }
    private static void gear(double x,double y,double radius,int teeth,int color) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_CULL_FACE); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f); GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for(int i=0;i<=teeth*4;i++) { double a=i*Math.PI*2/(teeth*4),r=radius*(i%4<2?1:.78); GL11.glVertex2d(x+Math.cos(a)*r,Math.min(15.7,y+Math.sin(a)*r)); GL11.glVertex2d(x+Math.cos(a)*radius*.36,Math.min(15.7,y+Math.sin(a)*radius*.36)); }
        GL11.glEnd(); GL11.glPopAttrib();
    }
    private QuestClassicUi() {}
}
