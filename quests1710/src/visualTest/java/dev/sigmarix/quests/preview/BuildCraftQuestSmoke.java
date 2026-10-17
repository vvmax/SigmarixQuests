package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
/** Real BC 7.1.23 engine tick/output path; no direct calls to the quest receiver. */
public final class BuildCraftQuestSmoke {
    private static EntityPlayerMP player;
    private static World world;
    private static int x,y,z,ticks;
    public static void start(EntityPlayerMP owner) {
        player=owner; world=owner.worldObj; x=(int)Math.floor(owner.posX)+4; y=(int)Math.floor(owner.posY); z=(int)Math.floor(owner.posZ)+5; ticks=0;
        world.setBlock(x,y,z,QuestBlocks.devices[0]); QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); tile.owner=owner.getUniqueID(); tile.quest="smoke-mj"; tile.task="charge"; tile.bindingChanged();
        world.setBlock(x-1,y,z,buildcraft.BuildCraftCore.engineBlock,3,3);
        buildcraft.core.lib.engines.TileEngineBase engine=(buildcraft.core.lib.engines.TileEngineBase)world.getTileEntity(x-1,y,z);
        engine.orientation=ForgeDirection.EAST; engine.energy=500; engine.isRedstonePowered=true;
        world.setBlock(x-1,y-1,z,net.minecraft.init.Blocks.redstone_block); engine.markDirty();
    }
    public static boolean tick() {
        if(player==null) return false; ticks++;
        if(QuestsMod.service.progress().value(player.getUniqueID(),"smoke-mj","charge")>=1600000) {
            System.out.println("Quest BuildCraft PASS: real 7.1.23 engine delivered RF, credited 1600000 micro-MJ");
            world.setBlockToAir(x-1,y,z); world.setBlockToAir(x,y,z); world.setBlockToAir(x-1,y-1,z); player=null; return true;
        }
        if(ticks>400) throw new IllegalStateException("BuildCraft engine did not power the quest terminal");
        return false;
    }
}
