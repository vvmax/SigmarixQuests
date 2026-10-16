package dev.sigmarix.quests.preview;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

/** Development-only fixture: renders a book in the title screen and exits. Never packaged in the mod. */
@Mod(modid="sigmarixquestpreview",name="Quest GUI preview fixture",version="1",dependencies="required-after:sigmarixquests")
public final class QuestPreview {
    private QuestNetwork.Snapshot fixture;
    private int ticks,stage,waiting;
    private boolean testing;
    private String worldName="QuestSmoke-"+System.currentTimeMillis();
    private QuestCore.Book original;
    private long oldRevision;
    private boolean restarted;
    private volatile boolean placeDevice,euRequested,euPassed;
    private boolean euRunning;
    private int deviceDelay,deviceX,deviceY,deviceZ;
    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        fixture=new QuestNetwork.Snapshot();
        try(Reader reader=new InputStreamReader(getClass().getResourceAsStream("/example-quests.json"),StandardCharsets.UTF_8)) {
            fixture.book=QuestService.JSON.fromJson(reader,QuestCore.Book.class);
        }
        fixture.book.validate(); fixture.canEdit=true;
        fixture.message="Development preview: fixture data, no server or world started";
        for(QuestCore.Quest q:fixture.book.quests) { fixture.states.put(q.id,q.id.equals("welcome")?"claimed":q.id.equals("nether")?"locked":"available"); }
        FMLCommonHandler.instance().bus().register(this);
    }
    @SubscribeEvent public void server(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        if(euRequested || euRunning) {
            try {
                Class<?> bridge=Class.forName("dev.sigmarix.quests.preview.Ic2QuestSmoke");
                if(euRequested) {
                    euRequested=false; euRunning=true;
                    net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
                    bridge.getMethod("start",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner);
                }
                if((Boolean)bridge.getMethod("tick").invoke(null)) { euPassed=true; euRunning=false; }
            } catch(Exception ex) { throw new IllegalStateException("IC2 smoke failed",ex); }
        }
        if(!placeDevice && deviceDelay==0) return;
        net.minecraft.server.MinecraftServer server=net.minecraft.server.MinecraftServer.getServer();
        net.minecraft.entity.player.EntityPlayerMP player=(net.minecraft.entity.player.EntityPlayerMP)server.getConfigurationManager().playerEntityList.get(0);
        if(deviceDelay>0) { if(--deviceDelay==0) player.openGui(QuestsMod.instance,0,player.worldObj,deviceX,deviceY,deviceZ); return; }
        placeDevice=false;
        net.minecraft.world.WorldServer world=server.worldServerForDimension(0); int x=(int)Math.floor(player.posX)+2,y=(int)Math.floor(player.posY),z=(int)Math.floor(player.posZ);
        world.setBlock(x,y,z,QuestBlocks.devices[1]); QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); tile.owner=player.getUniqueID(); tile.quest="welcome"; tile.markDirty(); world.markBlockForUpdate(x,y,z);
        deviceX=x; deviceY=y; deviceZ=z; deviceDelay=10;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft(); mc.gameSettings.pauseOnLostFocus=false; mc.gameSettings.renderDistanceChunks=2; mc.gameSettings.fancyGraphics=false; mc.gameSettings.enableVsync=false; mc.gameSettings.limitFramerate=30;
        if(testing) { smoke(mc); return; }
        ClientProxy.snapshot=fixture; ticks++;
        if(ticks==30) {
            mc.gameSettings.guiScale=2;
            QuestBookScreen screen=new QuestBookScreen(); mc.displayGuiScreen(screen);
            java.lang.reflect.Field selected=QuestBookScreen.class.getDeclaredField("selected"); selected.setAccessible(true); selected.set(screen,"stone"); screen.refresh();
        }
        if(ticks==65) {
            File destination=new File(mc.mcDataDir,"../build/preview"); destination.mkdirs();
            ScreenShotHelper.saveScreenshot(destination,"quest-book.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        }
        if(ticks==75) mc.displayGuiScreen(new QuestEditorScreen(null));
        if(ticks==90) {
            File destination=new File(mc.mcDataDir,"../build/preview"); destination.mkdirs();
            ScreenShotHelper.saveScreenshot(destination,"quest-editor.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            testing=true; ClientProxy.snapshot=null;
            net.minecraft.world.WorldSettings settings=new net.minecraft.world.WorldSettings(12345L,net.minecraft.world.WorldSettings.GameType.CREATIVE,true,false,net.minecraft.world.WorldType.FLAT).enableCommands();
            mc.launchIntegratedServer(worldName,"Quest smoke fixture",settings);
        }
    }
    private void action(String kind) {
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action=kind; action.id="welcome"; ClientProxy.send(action);
    }
    private int bread(Minecraft mc) {
        int count=0; for(net.minecraft.item.ItemStack stack:mc.thePlayer.inventory.mainInventory) if(stack!=null && stack.getItem()==net.minecraft.init.Items.bread) count+=stack.stackSize;
        return count;
    }
    private void smoke(Minecraft mc) throws Exception {
        if(++waiting%100==0) System.out.println("Quest smoke stage="+stage+" ticks="+waiting+" snapshot="+(ClientProxy.snapshot==null?"none":ClientProxy.snapshot.message));
        if(waiting>2400) throw new IllegalStateException("Quest smoke timeout at stage "+stage);
        if(mc.thePlayer==null || mc.theWorld==null || waiting%5!=0) return;
        QuestNetwork.Snapshot snap=ClientProxy.snapshot;
        switch(stage) {
            case 0: if(waiting<20) return; action("view"); stage=1; break;
            case 1:
                if(snap==null || snap.book==null) return;
                if(!snap.canEdit) throw new IllegalStateException("Integrated host lost edit permissions");
                if(restarted) {
                    if(!"claimed".equals(snap.states.get("welcome")) || bread(mc)!=4) throw new IllegalStateException("World persistence failed");
                    java.nio.file.Files.write(new File(mc.mcDataDir,"../build/preview/smoke.txt").toPath(),"PASS: real client/server handshake, completion, one-time claim, inventory sync, editing, stale revision rejection, world restart persistence, manual task confirmation, start action, task dependency sync, graphical toast, guide rendering, object form save, native device container, checked device binding".concat(euPassed?", real IC2 EnergyNet transfer":"").getBytes(StandardCharsets.UTF_8));
                    mc.shutdown(); return;
                }
                original=QuestService.JSON.fromJson(QuestService.JSON.toJson(snap.book),QuestCore.Book.class);
                action("check"); stage=2; break;
            case 2: if(snap!=null && "complete".equals(snap.states.get("welcome"))) { action("claim"); stage=3; } break;
            case 3: if(snap!=null && "claimed".equals(snap.states.get("welcome")) && bread(mc)==4) { action("claim"); stage=4; } break;
            case 4:
                if(snap==null || !snap.message.startsWith("Error: Reward unavailable")) return;
                if(bread(mc)!=4) throw new IllegalStateException("Repeated claim duplicated reward");
                oldRevision=snap.revision;
                QuestNetwork.Action edit=new QuestNetwork.Action(); edit.action="save"; edit.revision=snap.revision;
                edit.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(original),QuestCore.Book.class); edit.book.title="Network edit smoke";
                if(cpw.mods.fml.common.Loader.isModLoaded("IC2")) {
                    QuestCore.Quest eu=new QuestCore.Quest(); eu.id="smoke-eu"; eu.title="IC2 energy";
                    QuestCore.Task charge=new QuestCore.Task(); charge.id="charge"; charge.type="ic2_energy"; charge.count=16; eu.tasks.add(charge); edit.book.quests.add(eu);
                }
                QuestCore.Quest manual=new QuestCore.Quest(); manual.id="smoke-manual"; manual.title="Manual confirmation"; manual.shape="diamond"; manual.tags.add("important"); manual.pages.add("Guide page one\nA working independent quest guide."); manual.pages.add("Guide page two"); manual.uri="https://example.org/guide";
                QuestPresentation.Style style=new QuestPresentation.Style(); style.selector=".important state:available"; style.color="FF9B59B6"; edit.book.styles.add(style);
                QuestCore.Reward toast=new QuestCore.Reward(); toast.type="toast"; toast.target="Graphical reward"; toast.text="Native quest notification"; manual.rewards.add(toast);
                QuestCore.Task check=new QuestCore.Task(); check.id="confirm"; check.type="check"; check.manual=true; manual.tasks.add(check); edit.book.quests.add(manual);
                QuestCore.Quest dependent=new QuestCore.Quest(); dependent.id="smoke-dependent"; dependent.title="Task dependency"; dependent.dependencies.add("task:smoke-manual/confirm");
                QuestCore.Task next=new QuestCore.Task(); next.type="check"; dependent.tasks.add(next); edit.book.quests.add(dependent);
                ClientProxy.send(edit); stage=5; break;
            case 5:
                if(snap==null || snap.revision==oldRevision) return;
                if(!snap.book.title.equals("Network edit smoke")) throw new IllegalStateException("Editor did not save");
                QuestNetwork.Action stale=new QuestNetwork.Action(); stale.action="save"; stale.revision=oldRevision; stale.book=original;
                ClientProxy.send(stale); stage=6; break;
            case 6:
                if(snap==null || !snap.message.startsWith("Error: Book changed")) return;
                if(cpw.mods.fml.common.Loader.isModLoaded("IC2")) { euRequested=true; stage=20; break; }
                QuestNetwork.Action begin=new QuestNetwork.Action(); begin.action="start"; begin.id="smoke-manual"; ClientProxy.send(begin); stage=9; break;
            case 20:
                if(!euPassed) return;
                QuestNetwork.Action afterEU=new QuestNetwork.Action(); afterEU.action="start"; afterEU.id="smoke-manual"; ClientProxy.send(afterEU); stage=9; break;
            case 9:
                if(snap==null || !snap.message.equals("Quest started")) return;
                QuestNetwork.Action premature=new QuestNetwork.Action(); premature.action="check"; premature.id="smoke-manual"; ClientProxy.send(premature); stage=10; break;
            case 10:
                if(snap==null || !snap.message.startsWith("Error: Tasks incomplete")) return;
                QuestNetwork.Action confirm=new QuestNetwork.Action(); confirm.action="task"; confirm.id="smoke-manual"; confirm.text="confirm"; ClientProxy.send(confirm); stage=11; break;
            case 11:
                if(snap==null || !snap.message.equals("Task confirmed")) return;
                if(!Boolean.TRUE.equals(snap.taskStates.get("smoke-manual:confirm")) || !"available".equals(snap.states.get("smoke-dependent"))) throw new IllegalStateException("Task dependency did not unlock");
                mc.displayGuiScreen(new QuestTaskScreen(null,snap.book.quests.get(snap.book.quests.size()-2))); stage=12; waiting=0; break;
            case 12:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-tasks.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                QuestNetwork.Action finish=new QuestNetwork.Action(); finish.action="check"; finish.id="smoke-manual"; ClientProxy.send(finish); stage=13; break;
            case 13:
                if(snap==null || !"complete".equals(snap.states.get("smoke-manual"))) return;
                QuestNetwork.Action collect=new QuestNetwork.Action(); collect.action="claim"; collect.id="smoke-manual"; ClientProxy.send(collect); stage=14; break;
            case 14:
                if(snap==null || !"claimed".equals(snap.states.get("smoke-manual"))) return;
                mc.displayGuiScreen(new QuestGuideScreen(null,snap.book.quests.get(snap.book.quests.size()-2))); stage=15; waiting=0; break;
            case 15:
                if(waiting<20) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-guide-toast.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.displayGuiScreen(new QuestObjectScreen(null)); stage=16; waiting=0; break;
            case 16:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-object-editor.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                QuestObjectScreen form=(QuestObjectScreen)mc.currentScreen;
                java.lang.reflect.Field sessionField=QuestObjectScreen.class.getDeclaredField("session"); sessionField.setAccessible(true); Object session=sessionField.get(form);
                java.lang.reflect.Field rootField=session.getClass().getDeclaredField("root"); rootField.setAccessible(true); ((com.google.gson.JsonObject)rootField.get(session)).addProperty("title","Form edit smoke");
                java.lang.reflect.Method press=QuestObjectScreen.class.getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class); press.setAccessible(true); press.invoke(form,new net.minecraft.client.gui.GuiButton(0,0,0,"save")); stage=17; break;
            case 17:
                if(snap==null || !snap.book.title.equals("Form edit smoke")) return;
                QuestNetwork.Action restore=new QuestNetwork.Action(); restore.action="save"; restore.revision=snap.revision; restore.book=original;
                ClientProxy.send(restore); stage=7; break;
            case 7:
                if(snap==null || !snap.book.title.equals(original.title)) return;
                placeDevice=true; stage=18; waiting=0; break;
            case 18:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || snap==null) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-device.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Method bind=QuestDeviceScreen.class.getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class); bind.setAccessible(true); bind.invoke(mc.currentScreen,new net.minecraft.client.gui.GuiButton(0,0,0,"Bind")); stage=19; break;
            case 19:
                if(snap==null || !snap.message.equals("Device binding updated")) return;
                mc.thePlayer.closeScreen(); QuestBookScreen screen=new QuestBookScreen(); mc.displayGuiScreen(screen); stage=8; waiting=0; break;
            case 8:
                if(waiting<30) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-book-live.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.theWorld.sendQuittingDisconnectingPacket(); mc.loadWorld(null); restarted=true; stage=0; waiting=0; ClientProxy.snapshot=null;
                mc.launchIntegratedServer(worldName,"Quest smoke fixture",null); return;
            default: throw new IllegalStateException("Unknown smoke stage");
        }

    }
}