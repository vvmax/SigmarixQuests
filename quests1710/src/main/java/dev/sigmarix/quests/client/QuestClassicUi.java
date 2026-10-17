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

/** Classic widgets using the reference texture resources. */
public final class QuestClassicUi extends Gui {
    public static final int BORDER=0xFF1B1D1E, TEXT=0xFFFFFFFF;
    private static ResourceLocation background,dependency;
    private static final QuestPresentation.Theme defaults=new QuestPresentation.Theme();
    private static final RenderItem items=new RenderItem();
    public static void background(Minecraft mc,int width,int height) { background(mc,0,0,width,height); }
    public static void background(Minecraft mc,int x,int y,int width,int height) {
        if(ClientProxy.snapshot!=null && ClientProxy.snapshot.book!=null && !ClientProxy.snapshot.book.theme.background.equals(defaults.background)) { drawRect(x,y,x+width,y+height,QuestPresentation.color(ClientProxy.snapshot.book.theme.background)); return; }
        if(background==null) background=new ResourceLocation("ftblib:textures/gui/background_squares.png");
        GL11.glColor4f(1,1,1,1); mc.getTextureManager().bindTexture(background); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
        net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;
        t.startDrawingQuads(); t.addVertexWithUV(x,y+height,0,0,height/64D); t.addVertexWithUV(x+width,y+height,0,width/64D,height/64D); t.addVertexWithUV(x+width,y,0,width/64D,0); t.addVertexWithUV(x,y,0,0,0); t.draw();
    }
    public static void bindDependencies(Minecraft mc) {
        if(dependency==null) dependency=new ResourceLocation("ftbquests:textures/gui/dependency.png");
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
        if(task.type.equals("kill")) return "minecraft:diamond_sword";
        if(task.type.equals("stat"))return "minecraft:iron_sword";
        if(task.type.equals("achievement"))return "minecraft:wheat";
        if(task.type.equals("biome"))return "minecraft:sapling";
        if(task.type.equals("fluid")) return "minecraft:water_bucket";
        if(task.type.equals("dimension") || task.type.equals("location") || task.type.equals("biome")) return "minecraft:compass";
        return "";
    }
    public static void task(Minecraft mc,QuestCore.Task task,boolean done,int x,int y,int size) {
        if(task.icon.isEmpty()) {
            String type=task.type;
            if(type.equals("check")){texture("ftblib:textures/icons/accept"+(done?"":"_gray")+".png",x,y,size,size,0xFFFFFFFF);return;}
            String energy=type.equals("botania_mana")?"botania_mana":type.equals("ic2_energy")?"ic2":type.equals("energy") || type.equals("buildcraft_mj")?"fe":"";
            if(!energy.isEmpty()){
                texture("ftbquests:textures/tasks/"+energy+(energy.equals("fe")?"_empty":"_full")+".png",x,y,size,size,0xFFFFFFFF);
                texture("ftbquests:textures/tasks/"+energy+(energy.equals("fe")?"_full":"_empty")+".png",x,y,size,size,0xFFFFFFFF);return;
            }
            String glyph=type.equals("custom")?"color_hsb":type.equals("observation")?"art":type.equals("interaction")?"bell":"";
            if(!glyph.isEmpty()){icon(glyph,x,y,size,0xFFFFFFFF);return;}
            if(type.equals("fluid")){
                net.minecraftforge.fluids.Fluid fluid=net.minecraftforge.fluids.FluidRegistry.getFluid(task.target);
                if(fluid!=null)sprite(mc,fluid.getStillIcon(),x,y,size,fluid.getColor(new net.minecraftforge.fluids.FluidStack(fluid,1)));
                texture("ftbquests:textures/tasks/tank.png",x,y,size,size,0xFFFFFFFF);return;
            }
            if(type.equals("dimension")){sprite(mc,net.minecraft.init.Blocks.portal.getIcon(0,0),x,y,size,0xFFFFFF);return;}
        }
        String id=taskItem(task);if(id.isEmpty())icon("settings",x,y,size,0xFFFFFFFF);else item(mc,id,task.icon.isEmpty()?task.meta:task.iconMeta,task.icon.isEmpty()?task.nbt:task.iconNbt,x,y,size);
    }
    private static void sprite(Minecraft mc,net.minecraft.util.IIcon icon,int x,int y,int size,int color){
        if(icon==null)return;GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try{GL11.glTranslatef(0,0,200);mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.addVertexWithUV(x,y+size,0,icon.getMinU(),icon.getMaxV());t.addVertexWithUV(x+size,y+size,0,icon.getMaxU(),icon.getMaxV());t.addVertexWithUV(x+size,y,0,icon.getMaxU(),icon.getMinV());t.addVertexWithUV(x,y,0,icon.getMinU(),icon.getMinV());t.draw();
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    public static void reward(Minecraft mc,QuestCore.Reward reward,int x,int y,int size){
        if(reward.icon.isEmpty()){
            String glyph=reward.type.equals("choice")?"color_rgb":reward.type.equals("random")?"dice":reward.type.equals("loot")?"money_bag":reward.type.equals("custom")?"color_hsb":"";
            if(!glyph.isEmpty()){icon(glyph,x,y,size,0xFFFFFFFF);return;}
            if(reward.type.equals("command")){sprite(mc,net.minecraft.init.Blocks.command_block.getIcon(0,0),x,y,size,0xFFFFFF);return;}
        }
        item(mc,rewardItem(reward),reward.icon.isEmpty()?reward.meta:reward.iconMeta,reward.icon.isEmpty()?reward.nbt:reward.iconNbt,x,y,size);
    }
    public static String rewardItem(QuestCore.Reward reward) {
        if(!reward.icon.isEmpty()) return reward.icon;
        if(reward.type.equals("item")) return reward.item;
        if(reward.type.equals("xp") || reward.type.equals("xp_levels")) return "minecraft:experience_bottle";
        if(reward.type.equals("loot") || reward.type.equals("choice") || reward.type.equals("random")) return "minecraft:chest";
        if(reward.type.equals("command")) return "minecraft:command_block";
        if(reward.type.equals("achievement"))return "minecraft:wheat";
        if(reward.type.equals("toast"))return "minecraft:sign";
        return "minecraft:paper";
    }
    /** Small geometry icons remain sharp at every GUI scale. */
    public static void icon(String kind,int x,int y,int size,int color) {
        String resource=kind.equals("left")?"ftbquests:textures/gui/arrow_left.png":kind.equals("right")?"ftbquests:textures/gui/arrow_right.png":kind.equals("reward")?"ftbquests:textures/gui/collect_rewards.png":kind.equals("team")?"ftblib:textures/icons/friends.png":kind.equals("check")?"ftblib:textures/icons/check.png":kind.equals("add")?"ftblib:textures/icons/add.png":kind.equals("refresh")?"ftblib:textures/icons/refresh.png":kind.equals("book")?"ftbquests:textures/gui/guide.png":kind.equals("shop") || kind.equals("emergency_items") || kind.equals("guide") || kind.equals("info") || kind.equals("settings") || kind.equals("search") || kind.equals("close") || kind.equals("pin") || kind.equals("alert")?"ftbquests:textures/gui/"+kind+".png":"ftblib:textures/icons/"+kind+".png";
        texture(resource,x,y,size,size,color);
    }
    public static void texture(String resource,int x,int y,int w,int h,int color) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            GL11.glTranslated(0,0,200); Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(resource));
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f);
            func_152125_a(x,y,0,0,256,256,w,h,256,256);
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private QuestClassicUi() {}
}
