package dev.sigmarix.quests;

import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraftforge.fluids.*;

/** Checked appearance/protection/team operations, independent of the client renderer. */
public final class QuestScreenSettings {
    public static int mode(QuestDevice tile) { return tile.getBlockType() instanceof QuestBlocks.DeviceBlock?((QuestBlocks.DeviceBlock)tile.getBlockType()).mode:-1; }
    private static void screen(QuestDevice tile) { int mode=mode(tile); if(tile.screenLinked || mode<0 || mode>1) throw new IllegalArgumentException("Configure the core of a task or progress screen"); }
    public static boolean editor(EntityPlayer player) { return player.canCommandSenderUseCommand(2,"quests.edit"); }
    public static Block skin(QuestDevice tile) {
        if(tile.screenSkin.isEmpty() || !Block.blockRegistry.containsKey(tile.screenSkin)) return null;
        Block block=(Block)Block.blockRegistry.getObject(tile.screenSkin);
        return block instanceof QuestBlocks.DeviceBlock || block.hasTileEntity(tile.screenSkinMeta)?null:block;
    }
    public static void appearance(QuestDevice tile,EntityPlayer actor,ItemStack held,boolean invisible) {
        screen(tile); if(tile.indestructible && !editor(actor)) throw new IllegalArgumentException("Operator permission required for protected screens");
        Block block=null; int metadata=0;
        if(invisible) block=Blocks.air;
        else if(held!=null) {
            if(held.getItem() instanceof ItemBlock) { block=Block.getBlockFromItem(held.getItem()); metadata=((ItemBlock)held.getItem()).getMetadata(held.getItemDamage()); }
            else { FluidStack fluid=FluidContainerRegistry.getFluidForFilledItem(held); if(fluid==null && held.getItem() instanceof IFluidContainerItem) fluid=((IFluidContainerItem)held.getItem()).getFluid(held); if(fluid!=null) block=fluid.getFluid().getBlock(); }
            if(block==null || block==Blocks.air || block instanceof QuestBlocks.DeviceBlock || metadata<0 || metadata>15 || block.hasTileEntity(metadata) || block.getRenderType()<0) throw new IllegalArgumentException("Use an ordinary building block or a placed-fluid container");
        }
        tile.screenSkin=block==null?"":(String)Block.blockRegistry.getNameForObject(block); tile.screenSkinMeta=metadata; QuestScreens.presentationChanged(tile);
    }
    public static void protection(QuestDevice tile,EntityPlayer actor,boolean locked) {
        screen(tile); if(!editor(actor)) throw new IllegalArgumentException("Operator permission required"); tile.indestructible=locked; QuestScreens.presentationChanged(tile);
    }
    public static void owner(QuestDevice tile,EntityPlayer actor,UUID owner) {
        int mode=mode(tile); if(!editor(actor) || owner==null) throw new IllegalArgumentException("Operator permission required");
        if(tile.screenLinked || mode!=0 && mode!=1 && mode!=9) throw new IllegalArgumentException("Configure the core screen or progress detector");
        if(mode==9) tile.owner=owner; else QuestScreens.reassignOwner(tile,owner);
        tile.bindingChanged(); QuestScreens.presentationChanged(tile);
    }
    private QuestScreenSettings() {}
}
