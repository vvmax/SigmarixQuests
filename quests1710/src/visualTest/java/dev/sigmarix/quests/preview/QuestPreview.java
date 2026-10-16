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
                    java.nio.file.Files.write(new File(mc.mcDataDir,"../build/preview/smoke.txt").toPath(),"PASS: real client/server handshake, completion, one-time claim, inventory sync, editing, stale revision rejection, world restart persistence".getBytes(StandardCharsets.UTF_8));
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
                ClientProxy.send(edit); stage=5; break;
            case 5:
                if(snap==null || snap.revision==oldRevision) return;
                if(!snap.book.title.equals("Network edit smoke")) throw new IllegalStateException("Editor did not save");
                QuestNetwork.Action stale=new QuestNetwork.Action(); stale.action="save"; stale.revision=oldRevision; stale.book=original;
                ClientProxy.send(stale); stage=6; break;
            case 6:
                if(snap==null || !snap.message.startsWith("Error: Book changed")) return;
                QuestNetwork.Action restore=new QuestNetwork.Action(); restore.action="save"; restore.revision=snap.revision; restore.book=original;
                ClientProxy.send(restore); stage=7; break;
            case 7:
                if(snap==null || !snap.book.title.equals(original.title)) return;
                QuestBookScreen screen=new QuestBookScreen(); mc.displayGuiScreen(screen); stage=8; waiting=0; break;
            case 8:
                if(waiting<30) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-book-live.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.theWorld.sendQuittingDisconnectingPacket(); mc.loadWorld(null); restarted=true; stage=0; waiting=0; ClientProxy.snapshot=null;
                mc.launchIntegratedServer(worldName,"Quest smoke fixture",null); return;
            default: throw new IllegalStateException("Unknown smoke stage");
        }

    }
}