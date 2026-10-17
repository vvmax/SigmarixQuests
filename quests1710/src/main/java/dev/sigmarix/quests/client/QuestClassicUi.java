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
    public static void background(Minecraft mc,int x,int y,int width,int height) { tiled(mc,x,y,width,height,0xDCFFFFFF); }
    private static void tiled(Minecraft mc,int x,int y,int width,int height,int color) {
        if(ClientProxy.snapshot!=null && ClientProxy.snapshot.book!=null && !ClientProxy.snapshot.book.theme.background.equals(defaults.background)) {drawRect(x,y,x+width,y+height,QuestPresentation.color(ClientProxy.snapshot.book.theme.background));return;}
        if(background==null)background=new ResourceLocation("ftblib:textures/gui/background_squares.png");
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try{mc.getTextureManager().bindTexture(background);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.setColorRGBA(color>>16&255,color>>8&255,color&255,color>>>24);
            t.addVertexWithUV(x,y+height,0,x/64D,(y+height)/64D);t.addVertexWithUV(x+width,y+height,0,(x+width)/64D,(y+height)/64D);t.addVertexWithUV(x+width,y,0,(x+width)/64D,y/64D);t.addVertexWithUV(x,y,0,x/64D,y/64D);t.draw();
        }finally{GL11.glPopAttrib();}
    }
    public static void bindDependencies(Minecraft mc) {
        if(dependency==null) dependency=new ResourceLocation("ftbquests:textures/gui/dependency.png");
        mc.getTextureManager().bindTexture(dependency); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
    }
    public static void frame(int x,int y,int width,int height,int color) {
        if(width<=1 || height<=1){drawRect(x,y,x+width,y+height,color);return;}
        drawRect(x,y,x+width,y+1,color); drawRect(x,y+height-1,x+width,y+height,color);
        drawRect(x,y+1,x+1,y+height-1,color); drawRect(x+width-1,y+1,x+width,y+height-1,color);
    }
    public static void panel(Minecraft mc,int x,int y,int width,int height) {
        background(mc,x,y,width,height); drawRect(x+1,y+1,x+width-1,y+height-1,ClientProxy.snapshot!=null && !ClientProxy.snapshot.book.theme.panel.equals(defaults.panel)?QuestPresentation.color(ClientProxy.snapshot.book.theme.panel):0x4A000000); frame(x,y,width,height,BORDER);
    }
    public static void context(Minecraft mc,int x,int y,int w,int h) {
        tiled(mc,x+1,y+1,w-2,h-2,0xFFB4B4B4);
        drawRect(x+1,y,x+w-1,y+1,BORDER);drawRect(x+1,y+h-1,x+w-1,y+h,BORDER);
        drawRect(x,y+1,x+1,y+h-1,BORDER);drawRect(x+w-1,y+1,x+w,y+h-1,BORDER);
    }
    public static void item(Minecraft mc,String id,int meta,String nbt,int x,int y,int size) {
        item(mc,id,meta,nbt,x,y,size,true);
    }
    public static void inventoryItem(Minecraft mc,String id,int meta,String nbt,int x,int y,int size){item(mc,id,meta,nbt,x,y,size,false);}
    private static void item(Minecraft mc,String id,int meta,String nbt,int x,int y,int size,boolean questIcon) {
        Item item=id==null?null:(Item)Item.itemRegistry.getObject(id); if(item==null) return;
        if(questIcon && item instanceof QuestBlocks.CustomIconItem) { ItemStack iconStack=new ItemStack(item,1,meta); try { iconStack.setTagCompound(QuestService.tag(nbt)); } catch(RuntimeException invalid) {} texture(QuestIcons.resource(iconStack),x,y,size,size,0xFF000000|QuestIcons.color(iconStack)); return; }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try { ItemStack stack=new ItemStack(item,1,meta); stack.setTagCompound(QuestService.tag(nbt)); GL11.glTranslated(x,y,100); GL11.glScaled(size/16D,size/16D,1); RenderHelper.enableGUIStandardItemLighting(); if(item==net.minecraft.init.Items.experience_bottle && !stack.isItemEnchanted())items.renderItemIntoGUI(mc.fontRenderer,mc.getTextureManager(),stack,0,0);else items.renderItemAndEffectIntoGUI(mc.fontRenderer,mc.getTextureManager(),stack,0,0); }
        catch(RuntimeException ignored) {} finally { RenderHelper.disableStandardItemLighting(); GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private static boolean libraryAlphaTest;
    public static void quest(Minecraft mc,QuestCore.Quest quest,int x,int y,int size,boolean alphaTest){boolean previous=libraryAlphaTest;libraryAlphaTest=alphaTest;try{quest(mc,quest,x,y,size);}finally{libraryAlphaTest=previous;}}
    public static void quest(Minecraft mc,QuestCore.Quest quest,int x,int y,int size) {
        if(!quest.icon.isEmpty())item(mc,quest.icon,quest.iconMeta,quest.iconNbt,x,y,size);
        else if(!quest.tasks.isEmpty()){QuestCore.Task task=quest.tasks.get((int)(System.currentTimeMillis()/1000%quest.tasks.size()));boolean done=ClientProxy.snapshot!=null && Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(quest.id+":"+task.id));task(mc,task,done,x,y,size);}
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
    private static void itemExamples(String id,int meta,String nbt,java.util.List<ItemStack> values){if(id==null || id.isEmpty() || values.size()>=256)return;if(id.startsWith("ore:")){for(ItemStack stack:net.minecraftforge.oredict.OreDictionary.getOres(id.substring(4)))if(values.size()<256)values.add(stack);return;}Item item=(Item)Item.itemRegistry.getObject(id);if(item!=null){ItemStack stack=new ItemStack(item,1,meta==32767?0:meta);stack.setTagCompound(QuestService.tag(nbt));values.add(stack);}}
    private static void itemExamples(dev.sigmarix.quests.QuestItemFilter filter,java.util.List<ItemStack> values,int depth){if(filter==null || depth>16 || values.size()>=256)return;if(filter.type.equals("item"))itemExamples(filter.value,filter.meta,filter.nbt,values);else if(filter.type.equals("ore"))itemExamples("ore:"+filter.value,0,"",values);for(dev.sigmarix.quests.QuestItemFilter child:filter.children)itemExamples(child,values,depth+1);}
    public static void task(Minecraft mc,QuestCore.Task task,boolean done,int x,int y,int size) {
        if(task.icon.isEmpty()) {
            String type=task.type;
            if(type.equals("item")){java.util.List<ItemStack> values=new java.util.ArrayList<ItemStack>();itemExamples(task.target,task.meta,task.nbt,values);for(String target:task.targets)itemExamples(target,task.meta,task.nbt,values);itemExamples(task.filter,values,0);if(!values.isEmpty()){ItemStack stack=values.get((int)(System.currentTimeMillis()/1000%values.size()));inventoryItem(mc,(String)Item.itemRegistry.getNameForObject(stack.getItem()),stack.getItemDamage(),stack.hasTagCompound()?stack.getTagCompound().toString():"",x,y,size);return;}}
            if(type.equals("kill")){for(Object value:net.minecraft.entity.EntityList.entityEggs.keySet())if(value instanceof Integer && task.target.equals(net.minecraft.entity.EntityList.getStringFromID(((Integer)value).intValue()))){inventoryItem(mc,"minecraft:spawn_egg",((Integer)value).intValue(),"",x,y,size);return;}}
            if(type.equals("check")){libraryIcon("accept"+(done?"":"_gray"),x,y,size,0xFFFFFFFF);return;}
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
    static void sprite(Minecraft mc,net.minecraft.util.IIcon icon,int x,int y,int size,int color){
        if(icon==null)return;GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try{GL11.glTranslatef(0,0,200);mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.addVertexWithUV(x,y+size,0,icon.getMinU(),icon.getMaxV());t.addVertexWithUV(x+size,y+size,0,icon.getMaxU(),icon.getMaxV());t.addVertexWithUV(x+size,y,0,icon.getMaxU(),icon.getMinV());t.addVertexWithUV(x,y,0,icon.getMinU(),icon.getMinV());t.draw();
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    public static void reward(Minecraft mc,QuestCore.Reward reward,int x,int y,int size){
        if(reward.icon.isEmpty()){
            String table=reward.type.equals("loot")?reward.target:reward.table;if(ClientProxy.snapshot!=null && table!=null && !table.isEmpty() && ClientProxy.snapshot.book.lootTables.containsKey(table)){tableIcon(mc,table,x,y,size,0);return;}
            String glyph=reward.type.equals("choice")?"color_rgb":reward.type.equals("random")?"dice":reward.type.equals("loot")?"money_bag":reward.type.equals("custom")?"color_hsb":"";
            if(!glyph.isEmpty()){icon(glyph,x,y,size,0xFFFFFFFF);return;}
            if(reward.type.equals("command")){sprite(mc,net.minecraft.init.Blocks.command_block.getIcon(0,0),x,y,size,0xFFFFFF);return;}
        }
        item(mc,rewardItem(reward),reward.icon.isEmpty()?reward.meta:reward.iconMeta,reward.icon.isEmpty()?reward.nbt:reward.iconNbt,x,y,size);
    }
    public static void tableIcon(Minecraft mc,String id,int x,int y,int size,int depth){
        QuestCore.Book book=ClientProxy.snapshot.book;QuestCore.LootSettings settings=book.lootSettings.get(id);
        if(settings!=null && !settings.icon.isEmpty()){item(mc,settings.icon,settings.iconMeta,settings.iconNbt,x,y,size);return;}
        if(settings!=null && !settings.alias.isEmpty()){item(mc,"sigmarixquests:loot_crate",0,QuestLoot.crate(book,id).getTagCompound().toString(),x,y,size);return;}
        java.util.List<QuestCore.Reward> rewards=book.lootTables.get(id);if(depth>=16 || rewards==null || rewards.isEmpty()){libraryIcon("dice",x,y,size,0xFFFFFFFF);return;}
        QuestCore.Reward selected=rewards.get((int)(System.currentTimeMillis()/1000%rewards.size()));String child=selected.type.equals("loot")?selected.target:selected.table;
        if(selected.icon.isEmpty() && child!=null && !child.isEmpty()){tableIcon(mc,child,x,y,size,depth+1);return;}reward(mc,selected,x,y,size);
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
        if(kind.equals("check")){
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
            try{GL11.glTranslated(x,y,200);GL11.glScaled(size/16D,size/16D,1);GL11.glDisable(GL11.GL_TEXTURE_2D);net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();
                int[][][] shapes={{{0,8},{6,14},{6,8},{3,5}},{{6,8},{6,14},{16,4},{13,1}},{{1,8},{6,13},{6,9},{3,6}},{{6,9},{6,13},{15,4},{13,2}}};
                for(int i=0;i<shapes.length;i++){t.setColorOpaque_I(i<2?0x408300:0x9BC600);for(int[] v:shapes[i])t.addVertex(v[0],v[1],0);}t.draw();
            }finally{GL11.glPopMatrix();GL11.glPopAttrib();}return;
        }
        if(kind.equals("add")){double scale=size/16D;GL11.glPushMatrix();GL11.glTranslated(x,y,200);GL11.glScaled(scale,scale,1);drawRect(6,2,10,14,0xFF408300);drawRect(2,6,14,10,0xFF408300);drawRect(7,3,9,13,0xFF9BC600);drawRect(3,7,13,9,0xFF9BC600);GL11.glPopMatrix();return;}
        String resource=kind.equals("left")?"ftbquests:textures/gui/arrow_left.png":kind.equals("right")?"ftbquests:textures/gui/arrow_right.png":kind.equals("reward")?"ftbquests:textures/gui/collect_rewards.png":kind.equals("team")?"ftblib:textures/icons/friends.png":kind.equals("check")?"ftblib:textures/icons/check.png":kind.equals("add")?"ftblib:textures/icons/add.png":kind.equals("refresh")?"ftblib:textures/icons/refresh.png":kind.equals("book")?"ftbquests:textures/gui/guide.png":kind.equals("shop") || kind.equals("emergency_items") || kind.equals("guide") || kind.equals("info") || kind.equals("settings") || kind.equals("search") || kind.equals("close") || kind.equals("pin") || kind.equals("alert")?"ftbquests:textures/gui/"+kind+".png":"ftblib:textures/icons/"+kind+".png";
        if(resource.startsWith("ftblib:textures/icons/") && !resource.endsWith("support.png"))libraryIcon(resource.substring("ftblib:textures/icons/".length(),resource.length()-4),x,y,size,color);else texture(resource,x,y,size,size,color);
    }
    public static void libraryIcon(String name,int x,int y,int size,int color){
        Minecraft mc=Minecraft.getMinecraft();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        try{GL11.glTranslated(0,0,200);if(libraryAlphaTest){GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);}else GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_LIGHTING);double[] uv=QuestIconAtlas.bind(name);if(uv==null)return;GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.setColorRGBA(color>>16&255,color>>8&255,color&255,color>>>24);t.addVertexWithUV(x,y+size,0,uv[0],uv[3]);t.addVertexWithUV(x+size,y+size,0,uv[2],uv[3]);t.addVertexWithUV(x+size,y,0,uv[2],uv[1]);t.addVertexWithUV(x,y,0,uv[0],uv[1]);t.draw();
        }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
    public static void texture(String resource,int x,int y,int w,int h,int color) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
        try {
            GL11.glTranslated(0,0,200);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_LIGHTING); Minecraft.getMinecraft().getTextureManager().bindTexture(QuestImages.texture(resource));
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            if(resource.endsWith("/support.png") || resource.endsWith("/pin.png") && w>=16){GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);}
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();t.setColorRGBA(color>>16&255,color>>8&255,color&255,color>>>24);
            t.addVertexWithUV(x,y+h,0,0,1);t.addVertexWithUV(x+w,y+h,0,1,1);t.addVertexWithUV(x+w,y,0,1,0);t.addVertexWithUV(x,y,0,0,0);t.draw();
        } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
    }
    private QuestClassicUi() {}
}
