package dev.sigmarix.quests;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
public final class QuestGuiHandler implements IGuiHandler {
    private QuestDevice tile(EntityPlayer player,World world,int x,int y,int z) {
        if(!world.blockExists(x,y,z) || !(world.getTileEntity(x,y,z) instanceof QuestDevice)) return null;
        QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); return world.isRemote || tile.isUseableByPlayer(player)?tile:null;
    }
    @Override public Object getServerGuiElement(int id,EntityPlayer player,World world,int x,int y,int z) {
        QuestDevice tile=tile(player,world,x,y,z); return id!=0 || tile==null?null:tile instanceof QuestStorage?new net.minecraft.inventory.ContainerChest(player.inventory,tile):new QuestDeviceContainer(player,tile);
    }
    @Override public Object getClientGuiElement(int id,EntityPlayer player,World world,int x,int y,int z) {
        QuestDevice tile=tile(player,world,x,y,z); return id==0 && tile!=null?QuestsMod.proxy.deviceScreen(player,tile):null;
    }
}
