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
        DeviceBlock(int mode,String id) {
            super(Material.iron); this.mode=mode; setBlockName("sigmarixquests."+id); setBlockTextureName(mode==2?"minecraft:iron_block":mode==4?"minecraft:redstone_block":"minecraft:lapis_block");
            setHardness(3); setResistance(10); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc);
        }
        @Override public TileEntity createNewTileEntity(World world,int metadata) { return mode==7 || mode==8?new QuestStorage(mode==8):new QuestDevice(); }
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
        CustomIconItem() { setUnlocalizedName("sigmarixquests.custom_icon"); setTextureName("minecraft:paper"); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return stack.hasTagCompound() && !stack.getTagCompound().getString("name").isEmpty()?stack.getTagCompound().getString("name"):super.getItemStackDisplayName(stack); }
        @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player) { if(world.isRemote) QuestsMod.proxy.openIcon(); return stack; }
    }
    public static final class LootCrateItem extends Item {
        LootCrateItem() { setUnlocalizedName("sigmarixquests.loot_crate"); setTextureName("minecraft:minecart_chest"); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc); }
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
