package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import ic2.api.energy.prefab.BasicSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
/** Loaded reflectively only for -PquestIC2. Real IC2 EnergyNet moves power between adjacent tiles. */
public final class Ic2QuestSmoke {
    private static BasicSource source;
    private static World world;
    private static EntityPlayerMP player;
    private static java.util.UUID recipient;
    private static int x,y,z,ticks;
    public static void start(EntityPlayerMP owner) {
        player=owner; recipient=java.util.UUID.randomUUID(); world=owner.worldObj; x=(int)Math.floor(owner.posX)+3; y=(int)Math.floor(owner.posY); z=(int)Math.floor(owner.posZ)+3; ticks=0;
        world.setBlock(x,y,z,QuestBlocks.devices[0]); QuestDevice sink=(QuestDevice)world.getTileEntity(x,y,z); sink.owner=recipient; sink.quest=""; sink.task=""; sink.markDirty();
        world.setBlock(x-1,y,z,Blocks.stone); TileEntity parent=new TileEntity(); parent.setWorldObj(world); parent.xCoord=x-1; parent.yCoord=y; parent.zCoord=z;
        source=new BasicSource(parent,256,1); source.setEnergyStored(64); source.updateEntity();
    }
    public static boolean tick() {
        if(source==null) return false; source.updateEntity(); ticks++;
        if(ticks==6) { QuestDevice sink=(QuestDevice)world.getTileEntity(x,y,z); sink.quest="smoke-eu"; sink.task="charge"; sink.bindingChanged(); }
        if(QuestsMod.service.progress().value(recipient,"smoke-eu","charge")>=16) {
            if(source.getEnergyStored()>48.000001) throw new IllegalStateException("IC2 credited EU without drawing energy from source");
            if(QuestsMod.service.progress().value(player.getUniqueID(),"smoke-eu","charge")!=0) throw new IllegalStateException("Resource progress went to the online bystander instead of the offline owner");
            System.out.println("Quest IC2 EnergyNet PASS: 16 EU delivered to offline owner; source remaining="+source.getEnergyStored());
            source.invalidate(); source=null; world.setBlockToAir(x,y,z); world.setBlockToAir(x-1,y,z); return true;
        }
        if(ticks>200) throw new IllegalStateException("Real IC2 EnergyNet did not deliver EU: source="+source.getEnergyStored()+", sink="+((QuestDevice)world.getTileEntity(x,y,z)).getDemandedEnergy());
        return false;
    }
}
