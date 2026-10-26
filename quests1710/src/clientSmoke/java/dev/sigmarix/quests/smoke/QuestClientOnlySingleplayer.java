package dev.sigmarix.quests.smoke;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.world.*;
@Mod(modid="sqsingleplayersmokefixture",version="1",acceptableRemoteVersions="*",dependencies="required-after:sigmarixquests")
public final class QuestClientOnlySingleplayer {
    private int ticks,worldTicks;
    @Mod.EventHandler public void init(FMLInitializationEvent e){FMLCommonHandler.instance().bus().register(this);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e)throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();mc.gameSettings.renderDistanceChunks=2;mc.gameSettings.limitFramerate=30;
        if(++ticks==40){net.minecraftforge.client.event.GuiOpenEvent menu=new net.minecraftforge.client.event.GuiOpenEvent(new GuiSelectWorld(new GuiMainMenu()));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(menu);if(!(menu.gui instanceof GuiSelectWorld))throw new IllegalStateException("Singleplayer menu blocked");mc.launchIntegratedServer("ClientIdle-"+System.currentTimeMillis(),"Client idle",new WorldSettings(731,WorldSettings.GameType.CREATIVE,true,false,WorldType.FLAT).enableCommands());}
        if(mc.theWorld!=null && mc.thePlayer!=null){worldTicks++;QuestsMod.proxy.open();QuestNetwork.Action action=new QuestNetwork.Action();ClientProxy.send(action);if(ClientProxy.snapshot!=null || mc.currentScreen instanceof QuestBookScreen || mc.currentScreen instanceof GuiErrorScreen)throw new IllegalStateException("Client mod active in singleplayer");if(worldTicks==100){Files.write(Paths.get(System.getProperty("sq.smoke.dir"),"PASS-SP.txt"),"PASS: singleplayer menu unchanged; integrated world loaded; client quest requests and book silently inactive; no warning screen".getBytes("UTF-8"));mc.shutdown();}}
        if(ticks>1500)throw new IllegalStateException("Singleplayer smoke timed out");
    }
}
