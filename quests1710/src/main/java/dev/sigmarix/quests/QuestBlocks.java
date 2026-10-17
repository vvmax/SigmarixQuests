package dev.sigmarix.quests;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;

public final class QuestBlocks {
    public static final Block[] devices=new Block[10];
    public static final QuestCrateParcel crateParcel=new QuestCrateParcel();
    public static void register() {
        String[] ids={"task_screen","progress_screen","quest_barrier","reward_collector","redstone_detector","player_detector","loot_crate_opener","quest_chest","loot_crate_storage","progress_detector"};
        for(int i=0;i<ids.length;i++) { devices[i]=new DeviceBlock(i,ids[i]); GameRegistry.registerBlock(devices[i],ids[i]); }
        GameRegistry.registerTileEntity(QuestDevice.class,"sigmarixquests:device");
        GameRegistry.registerTileEntity(QuestStorage.class,"sigmarixquests:storage");
        GameRegistry.registerItem(new LootCrateItem(),"loot_crate");
        GameRegistry.registerItem(new CustomIconItem(),"custom_icon");
        GameRegistry.registerItem(crateParcel,"crate_parcel");
        QuestHooks.TASKS.put("sigmarix:redstone",(player,task)->false);
        QuestHooks.TASKS.put("sigmarix:player",(player,task)->false);
        QuestHooks.REWARDS.put("sigmarix:notice",(player,reward)->QuestService.say(player,reward.text));
    }
    public static void recipes() {
        GameRegistry.addShapelessRecipe(new ItemStack(GameRegistry.findItem("sigmarixquests","custom_icon")),net.minecraft.init.Items.paper,net.minecraft.init.Items.redstone);
        Item book=GameRegistry.findItem("sigmarixquests","quest_book");
        GameRegistry.addShapelessRecipe(new ItemStack(book),net.minecraft.init.Items.book);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[0]),net.minecraft.init.Items.book,net.minecraft.init.Items.iron_ingot,net.minecraft.init.Items.iron_ingot,net.minecraft.init.Items.iron_ingot,net.minecraft.init.Items.iron_ingot,net.minecraft.init.Blocks.glass,net.minecraft.init.Items.redstone);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[1]),net.minecraft.init.Items.book,net.minecraft.init.Items.comparator,net.minecraft.init.Blocks.glass);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[2]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.iron_block,net.minecraft.init.Blocks.iron_block,net.minecraft.init.Blocks.iron_block,net.minecraft.init.Blocks.iron_block);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[3]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.chest,net.minecraft.init.Blocks.hopper);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[4]),net.minecraft.init.Items.book,net.minecraft.init.Items.comparator,net.minecraft.init.Items.redstone);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[5]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.stone_pressure_plate,net.minecraft.init.Items.redstone);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[7]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.chest,net.minecraft.init.Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[8]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.chest,net.minecraft.init.Blocks.chest);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[9]),net.minecraft.init.Items.book,net.minecraft.init.Items.comparator,net.minecraft.init.Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(devices[6]),net.minecraft.init.Items.book,net.minecraft.init.Blocks.dispenser,net.minecraft.init.Blocks.hopper);
    }
    public static final class DeviceBlock extends BlockContainer {
        public final int mode;
        private int chunkRenderPass;
        public int chunkRenderPass(){return chunkRenderPass;}
        DeviceBlock(int mode,String id) {
            super(Material.iron); this.mode=mode; setBlockName("sigmarixquests."+id); setBlockTextureName(mode==2?"minecraft:iron_block":mode==4?"minecraft:redstone_block":"minecraft:lapis_block");
            if(mode==7)setBlockBounds(1/16F,0,1/16F,15/16F,14/16F,15/16F);
            setHardness(3); setResistance(10); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc);
        }
        @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        private net.minecraft.util.IIcon[] textures;
        @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister registry) {
            String[] names=mode<=1?new String[]{"screen_side","screen_front","screen_front_input"}:mode==2?new String[]{"barrier","barrier_completed"}:mode==7?new String[]{"chest_side","chest_top","chest_front"}:new String[]{new String[]{"","","","reward_collector","redstone_detector","player_detector","loot_crate_opener","","loot_crate_storage","progress_detector"}[mode]};
            textures=new net.minecraft.util.IIcon[names.length];for(int i=0;i<names.length;i++){String name="ftbquests:"+names[i];if(mode==2 && registry instanceof net.minecraft.client.renderer.texture.TextureMap){net.minecraft.client.renderer.texture.TextureMap atlas=(net.minecraft.client.renderer.texture.TextureMap)registry;net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=atlas.getTextureExtry(name);if(sprite==null){sprite=new dev.sigmarix.quests.client.QuestAnimatedSprite(name);atlas.setTextureEntry(name,sprite);}textures[i]=sprite;}else textures[i]=registry.registerIcon(name);}blockIcon=textures[0];
        }
        @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public net.minecraft.util.IIcon getIcon(int side,int meta) {
            if(textures==null)return blockIcon;
            return textures[mode<=1?(side==2?1:0):mode==7?(side<2?1:side==2?2:0):0];
        }
        @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public net.minecraft.util.IIcon getIcon(IBlockAccess world,int x,int y,int z,int side) {
            TileEntity raw=world.getTileEntity(x,y,z);QuestDevice tile=raw instanceof QuestDevice?(QuestDevice)raw:null;
            if(mode==2 && tile!=null && tile.signal)return textures[1];
            if(mode<=1)return textures[side==(tile==null?2:tile.facing)?1:0];
            if(mode==7)return textures[side<2?1:side==(tile==null?2:tile.facing)?2:0];
            return getIcon(side,world.getBlockMetadata(x,y,z));
        }
        @Override public TileEntity createNewTileEntity(World world,int metadata) { return mode==7 || mode==8?new QuestStorage(mode==8):new QuestDevice(); }
        @Override public int getRenderType() { return QuestsMod.proxy!=null?QuestsMod.proxy.screenRenderType():0; }
        @Override public boolean isOpaqueCube() { return mode>2 && mode!=7; }
        @Override public boolean renderAsNormalBlock() { return mode>2 && mode!=7; }
        @Override public boolean isNormalCube() { return mode<=1?!canProvidePower():super.isNormalCube(); }
        @Override public boolean isNormalCube(IBlockAccess world,int x,int y,int z) { return mode<=1?!canProvidePower():super.isNormalCube(world,x,y,z); }
        @Override public boolean canRenderInPass(int pass) { chunkRenderPass=pass;return mode==2?pass==1:mode<=1?pass==0 || pass==1:super.canRenderInPass(pass); }
        @Override public boolean isSideSolid(IBlockAccess world,int x,int y,int z,net.minecraftforge.common.util.ForgeDirection side) { return mode<=1 || super.isSideSolid(world,x,y,z,side); }
        @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
        public int getRenderBlockPass(){return mode==2?1:0;}
        private boolean protectedScreen(IBlockAccess world,int x,int y,int z) {
            if(mode>1) return false; TileEntity raw=world.getTileEntity(x,y,z); if(!(raw instanceof QuestDevice)) return false;
            QuestDevice tile=(QuestDevice)raw,core=QuestScreens.core(tile); return (core==null?tile:core).indestructible;
        }
        @Override public float getBlockHardness(World world,int x,int y,int z) { return protectedScreen(world,x,y,z)?-1:super.getBlockHardness(world,x,y,z); }
        @Override public boolean removedByPlayer(World world,EntityPlayer player,int x,int y,int z,boolean harvest) { return (!protectedScreen(world,x,y,z) || QuestScreenSettings.editor(player)) && super.removedByPlayer(world,player,x,y,z,harvest); }
        @Override public float getExplosionResistance(net.minecraft.entity.Entity entity,World world,int x,int y,int z,double ex,double ey,double ez) { return protectedScreen(world,x,y,z)?Float.MAX_VALUE:super.getExplosionResistance(entity,world,x,y,z,ex,ey,ez); }
        @Override public void onBlockExploded(World world,int x,int y,int z,net.minecraft.world.Explosion explosion) { if(!protectedScreen(world,x,y,z)) super.onBlockExploded(world,x,y,z,explosion); }
        @Override public boolean canEntityDestroy(IBlockAccess world,int x,int y,int z,net.minecraft.entity.Entity entity) { return !protectedScreen(world,x,y,z) && super.canEntityDestroy(world,x,y,z,entity); }
        @Override public void onBlockPlacedBy(World world,int x,int y,int z,EntityLivingBase entity,ItemStack stack) {
            if(entity instanceof EntityPlayer) { QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); tile.owner=((EntityPlayer)entity).getUniqueID(); int[] facing={2,5,3,4}; tile.facing=facing[net.minecraft.util.MathHelper.floor_double(entity.rotationYaw*4/360D+.5D)&3]; tile.bindingChanged(); }
        }
        @Override public boolean onBlockActivated(World world,int x,int y,int z,EntityPlayer p,int side,float hitX,float hitY,float hitZ) {
            if(mode==8 && p.getHeldItem()!=null && p.getHeldItem().getItem() instanceof QuestCrateParcel) {
                if(!world.isRemote) { QuestStorage storage=(QuestStorage)world.getTileEntity(x,y,z); if(storage!=null && storage.isUseableByPlayer(p)) {
                    try { QuestCrateParcel.deposit(p.getHeldItem(),storage); if(p.getHeldItem().stackSize<=0) p.inventory.setInventorySlotContents(p.inventory.currentItem,null); p.inventory.markDirty(); } catch(RuntimeException ex) { QuestService.say(p,ex.getMessage()); }
                } }
                return true;
            }
            if(mode==0 || mode==1){QuestDevice clicked=(QuestDevice)world.getTileEntity(x,y,z);QuestDevice core=clicked==null?null:QuestScreens.core(clicked);if(core==null)return false;if((mode==1 || !p.isSneaking()) && side!=core.facing)return false;
                if(mode==1 && !p.isSneaking()){if(!world.isRemote && !core.indestructible && QuestsMod.service!=null && core.owner!=null){java.util.List<QuestCore.Chapter> chapters=new java.util.ArrayList<QuestCore.Chapter>();for(QuestCore.Chapter chapter:QuestsMod.service.book.chapters)if(QuestPresentation.visibleChapter(QuestsMod.service.book,chapter.id))chapters.add(chapter);if(!chapters.isEmpty()){int current=-1;for(int i=0;i<chapters.size();i++)if(core.quest.equals("chapter:"+chapters.get(i).id))current=i;core.quest="chapter:"+chapters.get((current+1)%chapters.size()).id;core.task="";core.bindingChanged();}}return true;}
                if(!world.isRemote && core.isUseableByPlayer(p)){world.markBlockForUpdate(core.xCoord,core.yCoord,core.zCoord);p.openGui(QuestsMod.instance,0,world,core.xCoord,core.yCoord,core.zCoord);}return true;
            }
            if(p.isSneaking()) return false;
            if(mode==6 && p.getHeldItem()!=null && p.getHeldItem().getItem() instanceof LootCrateItem) { if(!world.isRemote) LootCrateItem.open(p,p.getHeldItem()); return true; }
            if(!world.isRemote) { QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); if(tile!=null && tile.isUseableByPlayer(p)) { world.markBlockForUpdate(x,y,z); p.openGui(QuestsMod.instance,0,world,x,y,z); } else QuestService.say(p,"Only device owner or operator can open this device"); }
            return true;
        }
        @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world,int x,int y,int z) {
            QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z);
            return mode==2 && tile!=null && tile.signal ? null : super.getCollisionBoundingBoxFromPool(world,x,y,z);
        }
        @Override public void breakBlock(World world,int x,int y,int z,Block block,int meta) {
            TileEntity tile=world.getTileEntity(x,y,z);
            if(!world.isRemote && tile instanceof QuestStorage) for(ItemStack parcel:((QuestStorage)tile).parcels()) world.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(world,x+.5,y+.5,z+.5,parcel));
            if(!world.isRemote && tile instanceof QuestDevice) {
                if(tile instanceof QuestStorage) for(int slot=0;slot<((QuestStorage)tile).getSizeInventory();slot++) { ItemStack stack=((QuestStorage)tile).getStackInSlotOnClosing(slot); if(stack!=null) world.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(world,x+.5,y+.5,z+.5,stack)); }
                for(ItemStack stack:((QuestDevice)tile).drainDeviceItems()) world.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(world,x+.5,y+.5,z+.5,stack));
            }
            super.breakBlock(world,x,y,z,block,meta);
        }
        @Override public boolean canProvidePower() { return mode==1 || mode==5 || mode==9; }
        @Override public int isProvidingWeakPower(IBlockAccess world,int x,int y,int z,int side) {
            TileEntity tile=world.getTileEntity(x,y,z); if(mode==9 && tile instanceof QuestDevice) return ((QuestDevice)tile).redstoneOutput; return (mode==1 || mode==5) && tile instanceof QuestDevice && ((QuestDevice)tile).signal?15:0;
        }
    }
    public static final class CustomIconItem extends Item {
        CustomIconItem() { setMaxStackSize(1); setUnlocalizedName("sigmarixquests.custom_icon"); setTextureName("minecraft:painting"); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return stack.hasTagCompound() && !stack.getTagCompound().getString("name").isEmpty()?stack.getTagCompound().getString("name"):super.getItemStackDisplayName(stack); }
        @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player) { if(world.isRemote) QuestsMod.proxy.openIcon(); return stack; }
    }
    public static final class LootCrateItem extends Item {
        LootCrateItem() { setUnlocalizedName("sigmarixquests.loot_crate"); setTextureName("ftbquests:lootcrate/front_outline"); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc); }
        @Override public String getItemStackDisplayName(ItemStack stack) { QuestCore.LootSettings settings=QuestLoot.settings(stack); if(settings!=null && !settings.name.isEmpty()) return settings.name; return stack.hasTagCompound() && !stack.getTagCompound().getString("name").isEmpty()?stack.getTagCompound().getString("name"):super.getItemStackDisplayName(stack); }
        @Override public int getColorFromItemStack(ItemStack stack,int pass) { QuestCore.LootSettings settings=QuestLoot.settings(stack); if(settings!=null) return QuestPresentation.color(settings.color); return stack.hasTagCompound() && stack.getTagCompound().hasKey("color")?stack.getTagCompound().getInteger("color"):0xFFFFFF; }
        @Override public boolean hasEffect(ItemStack stack,int pass) { QuestCore.LootSettings settings=QuestLoot.settings(stack); if(settings!=null) return settings.glow; return stack.hasTagCompound() && stack.getTagCompound().getBoolean("glow"); }
        @Override public void getSubItems(Item item,net.minecraft.creativetab.CreativeTabs tab,java.util.List list) {
            if(QuestLoot.clientBook!=null) for(String table:QuestLoot.clientBook.lootTables.keySet()) list.add(QuestLoot.crate(QuestLoot.clientBook,table));
            else list.add(new ItemStack(item));
        }
        @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player) { if(!world.isRemote) open(player,stack); return stack; }
        static boolean open(EntityPlayer p,ItemStack stack) {
            if(!(p instanceof EntityPlayerMP) || stack==null || !(stack.getItem() instanceof LootCrateItem) || QuestsMod.service==null) return false;
            try {
                if(!stack.hasTagCompound()) throw new IllegalArgumentException("Loot crate needs NBT {table:table_id}");
                QuestsMod.service.openLoot((EntityPlayerMP)p,QuestLoot.table(stack.getTagCompound()),stack);
            } catch(RuntimeException ex) { QuestService.say(p,"Loot crate: "+ex.getMessage()); }
            return true;
        }
    }
}
