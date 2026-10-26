package dev.sigmarix.quests.smoke;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
@Mod(modid="sqclientsmokefixture",name="Client-only compatibility test",version="1",acceptableRemoteVersions="*",dependencies="required-after:sigmarixquests")
public final class QuestClientOnlySmoke {
    private int ticks,stage,wait;private boolean connected;
    private Path dir(){return Paths.get(System.getProperty("sq.smoke.dir"));}
    @Mod.EventHandler public void init(FMLInitializationEvent e)throws Exception {
        for(String name:new String[]{"QuestProgress","QuestScripts","QuestNetwork$RequestHandler","QuestSnapshotSync$Server"})try{Class.forName("dev.sigmarix.quests."+name,false,getClass().getClassLoader());throw new IllegalStateException("Server class leaked: "+name);}catch(ClassNotFoundException expected){}
        for(java.lang.reflect.Method method:QuestService.class.getDeclaredMethods())if(method.getName().equals("save")||method.getName().equals("claimReward"))throw new IllegalStateException("Server service leaked");
        FMLCommonHandler.instance().bus().register(this);
    }
    private void action(String action){QuestNetwork.Action a=new QuestNetwork.Action();a.action=action;a.id="remote";ClientProxy.send(a);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e)throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();mc.gameSettings.renderDistanceChunks=2;mc.gameSettings.limitFramerate=30;ticks++;
        if(ticks>9000)throw new IllegalStateException("Client-only test timed out at stage "+stage);
        if(!connected && ticks>40 && Files.exists(dir().resolve("port.txt"))){int port=Integer.parseInt(new String(Files.readAllBytes(dir().resolve("port.txt")),"UTF-8").trim());cpw.mods.fml.client.FMLClientHandler.instance().setupServerList();cpw.mods.fml.client.FMLClientHandler.instance().connectToServer(new GuiMainMenu(),new net.minecraft.client.multiplayer.ServerData("Thin client host","127.0.0.1:"+port));connected=true;}
        QuestNetwork.Snapshot snapshot=ClientProxy.snapshot;if(mc.thePlayer==null||snapshot==null||!"Remote client-only compatibility".equals(snapshot.book.title))return;
        if(++wait<12)return;wait=0;
        if(stage==0){QuestsMod.proxy.open();action("check");stage=1;}
        else if(stage==1 && "complete".equals(snapshot.states.get("remote"))){action("claim");stage=2;}
        else if(stage==2 && "claimed".equals(snapshot.states.get("remote"))){int bread=0;for(ItemStack stack:mc.thePlayer.inventory.mainInventory)if(stack!=null&&stack.getItem()==Items.bread)bread+=stack.stackSize;if(bread!=3)throw new IllegalStateException("Server reward inventory mismatch: "+bread);QuestNetwork.Action edit=new QuestNetwork.Action();edit.action="save";edit.revision=snapshot.revision;edit.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(snapshot.book),QuestCore.Book.class);edit.book.quests.get(0).title="Thin client delta edit";ClientProxy.send(edit);stage=3;}
        else if(stage==3 && "Thin client delta edit".equals(snapshot.book.quests.get(0).title)){mc.displayGuiScreen(null);Files.write(dir().resolve("open-device"),new byte[0]);stage=4;}
        else if(stage==4 && mc.currentScreen instanceof QuestDeviceScreen){if(!(mc.thePlayer.openContainer instanceof QuestDeviceContainer))throw new IllegalStateException("Wrong device container");QuestDeviceContainer container=(QuestDeviceContainer)mc.thePlayer.openContainer;if(container.percent!=100)return;mc.thePlayer.closeScreen();Files.write(dir().resolve("open-storage"),new byte[0]);stage=5;}
        else if(stage==5 && mc.currentScreen instanceof QuestStorageScreen){ItemStack stack=mc.thePlayer.openContainer.getSlot(0).getStack();if(stack==null)return;if(stack.getItem()!=Items.stick||stack.stackSize!=23)throw new IllegalStateException("Client inventory mirror mismatch");Files.write(dir().resolve("PASS.txt"),"PASS: isolated client-only production JAR connected over TCP to original full 0.28.1 JAR; no server classes; full snapshot; task completion; server reward and inventory sync; editor delta; device NBT/container progress; storage slots".getBytes("UTF-8"));mc.shutdown();}
    }
}
