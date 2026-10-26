package dev.sigmarix.quests.smoke;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.*;
@Mod(modid="sqclienthostfixture",name="Client-only compatibility host",version="1",acceptableRemoteVersions="*",dependencies="required-after:sigmarixquests")
public final class QuestClientOnlyHost {
    private int ticks;private boolean started,opened,installed,deviceOpened,storageOpened;
    private Path dir(){return Paths.get(System.getProperty("sq.smoke.dir"));}
    @Mod.EventHandler public void init(FMLInitializationEvent e){FMLCommonHandler.instance().bus().register(this);}
    @SubscribeEvent public void client(TickEvent.ClientTickEvent e)throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();mc.gameSettings.renderDistanceChunks=2;mc.gameSettings.limitFramerate=30;
        if(++ticks==40){started=true;mc.launchIntegratedServer("ThinClientHost-"+System.currentTimeMillis(),"Thin client host",new WorldSettings(581,WorldSettings.GameType.CREATIVE,true,false,WorldType.FLAT).enableCommands());}
        if(started && !opened && mc.theWorld!=null && mc.getIntegratedServer()!=null){mc.getIntegratedServer().setOnlineMode(false);String port=mc.getIntegratedServer().shareToLAN(WorldSettings.GameType.CREATIVE,true);Files.write(dir().resolve("port.txt"),port.getBytes("UTF-8"));opened=true;}
        if(Files.exists(dir().resolve("PASS.txt")))mc.shutdown();
        if(ticks>12000)throw new IllegalStateException("Client-only host timed out");
    }
    @SubscribeEvent public void server(TickEvent.ServerTickEvent e)throws Exception {
        if(e.phase!=TickEvent.Phase.END || QuestsMod.service==null)return;
        net.minecraft.server.MinecraftServer server=net.minecraft.server.MinecraftServer.getServer();
        if(!installed){QuestCore.Book book=new QuestCore.Book();book.title="Remote client-only compatibility";QuestCore.Quest q=new QuestCore.Quest();q.id="remote";q.title="Remote quest";q.icon="minecraft:diamond";QuestCore.Task t=new QuestCore.Task();t.id="check";t.type="check";t.manual=false;q.tasks.add(t);QuestCore.Reward r=new QuestCore.Reward();r.id="bread";r.type="item";r.item="minecraft:bread";r.count=3;q.rewards.add(r);book.quests.add(q);QuestsMod.service.save(book,QuestsMod.service.revision);installed=true;}
        for(Object raw:server.getConfigurationManager().playerEntityList){EntityPlayerMP player=(EntityPlayerMP)raw;if(!player.getCommandSenderName().equals("ThinTester"))continue;
            if(!player.canCommandSenderUseCommand(2,"quests.edit"))server.getConfigurationManager().func_152605_a(player.getGameProfile());
            if(!deviceOpened && Files.exists(dir().resolve("open-device"))){World world=player.worldObj;int x=(int)player.posX+2,y=(int)player.posY,z=(int)player.posZ;world.setBlock(x,y,z,QuestBlocks.devices[1]);QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z);tile.owner=player.getUniqueID();tile.quest="remote";tile.screenTitle="Remote client tile";tile.screenPercent=100;tile.markDirty();world.markBlockForUpdate(x,y,z);player.playerNetServerHandler.sendPacket(new net.minecraft.network.play.server.S23PacketBlockChange(x,y,z,world));player.playerNetServerHandler.sendPacket(tile.getDescriptionPacket());player.openGui(QuestsMod.instance,0,world,x,y,z);deviceOpened=true;}
            if(!storageOpened && Files.exists(dir().resolve("open-storage"))){World world=player.worldObj;int x=(int)player.posX+3,y=(int)player.posY,z=(int)player.posZ;world.setBlock(x,y,z,QuestBlocks.devices[7]);QuestStorage tile=(QuestStorage)world.getTileEntity(x,y,z);tile.owner=player.getUniqueID();tile.setInventorySlotContents(0,new ItemStack(Items.stick,23));tile.markDirty();world.markBlockForUpdate(x,y,z);player.playerNetServerHandler.sendPacket(new net.minecraft.network.play.server.S23PacketBlockChange(x,y,z,world));player.playerNetServerHandler.sendPacket(tile.getDescriptionPacket());player.openGui(QuestsMod.instance,0,world,x,y,z);storageOpened=true;}
        }
    }
}
