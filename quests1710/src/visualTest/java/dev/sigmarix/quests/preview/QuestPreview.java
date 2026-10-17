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
    private int ticks,stage,waiting,loginRetries;
    private boolean testing,teamPassed,neiPassed,presentationPassed,bookPrepared;
    private String worldName="QuestSmoke-"+System.currentTimeMillis();
    private QuestCore.Book original;
    private String remoteImage;
    private long oldRevision,lastSmokeStep,lastBookAttempt;
    private QuestNetwork.Action pendingBookEdit;
    private boolean restarted;
    private double skinViewX,skinViewY,skinViewZ;
    private volatile boolean detectorRequested,openCollectorRequested,inputRequested,openProgressRequested,openOpenerRequested,skinRequested;
    private volatile boolean placeDevice,euRequested,euPassed,manaRequested,manaPassed,emcRequested,emcReady,emcPassed;
    private volatile boolean nativeRequested,nativePassed,nativeVerify,nativeVerified;
    private volatile boolean npcRequested,npcPassed,npcVerify,npcVerified;
    private volatile boolean bcRequested,bcPassed;
    private volatile boolean iconRequested,iconReady;
    private boolean iconPassed,pinPassed;
    private volatile boolean openStorageRequested,commandVerified;
    private boolean bcRunning;
    private boolean emcRunning;
    private boolean manaRunning;
    private boolean euRunning;
    private int detectorDelay,deviceDelay,deviceX,deviceY,deviceZ;
    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        remoteImage=PreviewImageServer.start(); GuideSmoke.register();
        java.util.List<String> expected=java.nio.file.Files.readAllLines(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/expected-mods.txt").toPath(),StandardCharsets.UTF_8);
        String[][] optional={{"IC2","ic2.api.energy.tile.IEnergySink"},{"Botania","vazkii.botania.api.mana.IManaReceiver"},{"ProjectE","moze_intel.projecte.playerData.Transmutation"},{"customnpcs","noppes.npcs.scripted.ScriptPlayer"},{"BuildCraft|Core","buildcraft.BuildCraftCore"},{"NotEnoughItems","codechicken.nei.api.API"}};
        for(String[] mod:optional) {
            boolean loaded=cpw.mods.fml.common.Loader.isModLoaded(mod[0]); if(loaded!=expected.contains(mod[0])) throw new IllegalStateException("Unexpected optional mod state: "+mod[0]+"="+loaded);
            if(!loaded) try { Class.forName(mod[1],false,getClass().getClassLoader()); throw new IllegalStateException("Compile-only API leaked into game runtime: "+mod[0]); } catch(ClassNotFoundException correct) {}
        }
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
        if(QuestsMod.service!=null && net.minecraft.server.MinecraftServer.getServer()!=null && !net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.isEmpty()) {
            net.minecraft.entity.player.EntityPlayerMP actor=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            if(QuestsMod.service.progress().stage(actor.getUniqueID(),"native-click")) commandVerified=true;
        }
        if(openOpenerRequested) { openOpenerRequested=false; net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0); int[] position=NativeQuestSmoke.openerPosition(); owner.openGui(QuestsMod.instance,0,owner.worldObj,position[0],position[1],position[2]); }
        if(openCollectorRequested) {
            openCollectorRequested=false; net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            int[] position=NativeQuestSmoke.collectorPosition(); owner.openGui(QuestsMod.instance,0,owner.worldObj,position[0],position[1],position[2]);
        }
        if(openStorageRequested) {
            net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            int[] position=NativeQuestSmoke.storagePosition(); owner.openGui(QuestsMod.instance,0,owner.worldObj,position[0],position[1],position[2]); openStorageRequested=false;
        }
        if(iconRequested) {
            net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            owner.inventory.setInventorySlotContents(8,new net.minecraft.item.ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","custom_icon")));
            if(cpw.mods.fml.common.Loader.isModLoaded("Botania")) owner.inventory.setInventorySlotContents(7,new net.minecraft.item.ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("Botania","lexicon")));
            owner.inventory.currentItem=8; owner.inventoryContainer.detectAndSendChanges();
            owner.playerNetServerHandler.sendPacket(new net.minecraft.network.play.server.S09PacketHeldItemChange(8));
            iconRequested=false; iconReady=true;
        }
        if(nativeRequested || nativeVerify) {
            try {
                net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
                if(nativeRequested) { NativeQuestSmoke.run(owner); nativeRequested=false; nativePassed=true; }
                if(nativeVerify) { NativeQuestSmoke.verify(owner); nativeVerify=false; nativeVerified=true; }
            } catch(Exception ex) { throw new IllegalStateException("Native quest smoke failed",ex); }
        }
        if(npcRequested || npcVerify) {
            try {
                Class<?> bridge=Class.forName("dev.sigmarix.quests.preview.NPCQuestSmoke");
                net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
                if(npcRequested) { bridge.getMethod("run",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner); npcRequested=false; npcPassed=true; }
                if(npcVerify) { bridge.getMethod("verify",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner); npcVerify=false; npcVerified=true; }
            } catch(Exception ex) { throw new IllegalStateException("CustomNPCs smoke failed",ex); }
        }
        if(bcRequested || bcRunning) {
            try {
                Class<?> bridge=Class.forName("dev.sigmarix.quests.preview.BuildCraftQuestSmoke");
                if(bcRequested) { bcRequested=false; bcRunning=true; net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0); bridge.getMethod("start",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner); }
                if((Boolean)bridge.getMethod("tick").invoke(null)) { bcPassed=true; bcRunning=false; }
            } catch(Exception ex) { throw new IllegalStateException("BuildCraft smoke failed",ex); }
        }
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
        if(manaRequested || manaRunning) {
            try {
                Class<?> bridge=Class.forName("dev.sigmarix.quests.preview.BotaniaQuestSmoke");
                if(manaRequested) {
                    manaRequested=false; manaRunning=true;
                    net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
                    bridge.getMethod("start",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner);
                }
                if((Boolean)bridge.getMethod("tick").invoke(null)) { manaPassed=true; manaRunning=false; }
            } catch(Exception ex) { throw new IllegalStateException("Botania smoke failed",ex); }
        }
        if(emcRequested || emcRunning) {
            try {
                Class<?> bridge=Class.forName("dev.sigmarix.quests.preview.ProjectEQuestSmoke");
                if(emcRequested) {
                    emcRequested=false; emcRunning=true;
                    net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
                    bridge.getMethod("start",net.minecraft.entity.player.EntityPlayerMP.class).invoke(null,owner); emcReady=true;
                }
                if((Boolean)bridge.getMethod("tick").invoke(null)) { emcPassed=true; emcRunning=false; }
            } catch(Exception ex) { throw new IllegalStateException("ProjectE smoke failed",ex); }
        }
        if(skinRequested) {
            skinRequested=false; net.minecraft.entity.player.EntityPlayerMP actor=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            actor.inventory.setInventorySlotContents(6,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.wool,1,14)); actor.inventory.currentItem=6; actor.inventoryContainer.detectAndSendChanges(); actor.openContainer.detectAndSendChanges(); actor.playerNetServerHandler.sendPacket(new net.minecraft.network.play.server.S09PacketHeldItemChange(6));
        }
        if(openProgressRequested) { openProgressRequested=false; net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0); owner.openGui(QuestsMod.instance,0,owner.worldObj,deviceX,deviceY,deviceZ); }
        if(inputRequested) {
            inputRequested=false; net.minecraft.entity.player.EntityPlayerMP owner=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            owner.inventory.currentItem=8; owner.playerNetServerHandler.sendPacket(new net.minecraft.network.play.server.S09PacketHeldItemChange(8));
            owner.worldObj.setBlock(deviceX+4,deviceY,deviceZ,QuestBlocks.devices[0]); QuestDevice inputTile=(QuestDevice)owner.worldObj.getTileEntity(deviceX+4,deviceY,deviceZ); inputTile.owner=owner.getUniqueID(); inputTile.facing=4; inputTile.bindingChanged(); detectorDelay=30;
        }
        if(detectorRequested) {
            detectorRequested=false;
            net.minecraft.entity.player.EntityPlayerMP detectorPlayer=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            detectorPlayer.worldObj.setBlock(deviceX+4,deviceY,deviceZ,QuestBlocks.devices[5]);
            QuestDevice detectorTile=(QuestDevice)detectorPlayer.worldObj.getTileEntity(deviceX+4,deviceY,deviceZ); detectorTile.owner=detectorPlayer.getUniqueID(); detectorTile.bindingChanged();
            detectorDelay=30;
        }
        if(detectorDelay>0 && --detectorDelay==0) {
            net.minecraft.entity.player.EntityPlayerMP detectorPlayer=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0);
            detectorPlayer.openGui(QuestsMod.instance,0,detectorPlayer.worldObj,deviceX+4,deviceY,deviceZ);
        }
        if(!placeDevice && deviceDelay==0) return;
        net.minecraft.server.MinecraftServer server=net.minecraft.server.MinecraftServer.getServer();
        net.minecraft.entity.player.EntityPlayerMP player=(net.minecraft.entity.player.EntityPlayerMP)server.getConfigurationManager().playerEntityList.get(0);
        if(deviceDelay>0) { if(--deviceDelay==0) player.openGui(QuestsMod.instance,0,player.worldObj,deviceX,deviceY,deviceZ); return; }
        placeDevice=false;
        net.minecraft.world.WorldServer world=server.worldServerForDimension(0); int x=(int)Math.floor(player.posX)+2,y=(int)Math.floor(player.posY),z=(int)Math.floor(player.posZ);
        world.setBlock(x,y,z,QuestBlocks.devices[1]); QuestDevice tile=(QuestDevice)world.getTileEntity(x,y,z); tile.owner=player.getUniqueID(); tile.quest="welcome"; tile.facing=4; tile.markDirty(); world.markBlockForUpdate(x,y,z);
        player.setPositionAndUpdate(player.posX,player.posY,player.posZ); player.rotationYaw=-90; player.rotationPitch=30;
        for(int column=0;column<2;column++) for(int row=0;row<2;row++) if(column!=0 || row!=0) {
            world.setBlock(x,y+row,z+column,QuestBlocks.devices[1]); QuestDevice part=(QuestDevice)world.getTileEntity(x,y+row,z+column); part.owner=player.getUniqueID(); part.facing=4; part.markDirty();
        }
        QuestScreens.configure(tile,2,2);
        if(QuestScreens.core((QuestDevice)world.getTileEntity(x,y+1,z+1))!=tile) throw new IllegalStateException("Screen part did not link to its core");
        player.playerNetServerHandler.setPlayerLocation(player.posX,player.posY,z+1D,-90,17);
        deviceX=x; deviceY=y; deviceZ=z; deviceDelay=45;
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
    private void press(net.minecraft.client.gui.GuiScreen screen,int id) throws Exception {
        java.lang.reflect.Method method=screen.getClass().getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class); method.setAccessible(true);
        method.invoke(screen,new net.minecraft.client.gui.GuiButton(id,0,0,"fixture"));
    }
    private void action(String kind) {
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action=kind; action.id="welcome"; ClientProxy.send(action);
    }
    private int bread(Minecraft mc) {
        int count=0; for(net.minecraft.item.ItemStack stack:mc.thePlayer.inventory.mainInventory) if(stack!=null && stack.getItem()==net.minecraft.init.Items.bread) count+=stack.stackSize;
        return count;
    }
    private void smoke(Minecraft mc) throws Exception {
        if(++waiting%100==0) System.out.println("Quest smoke stage="+stage+" ticks="+waiting+" snapshot="+(ClientProxy.snapshot==null?"none":ClientProxy.snapshot.message)+" gui="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName())+" player="+(mc.thePlayer!=null));
        if(waiting>2400) throw new IllegalStateException("Quest smoke timeout at stage "+stage);
        if(stage==0 && !restarted && mc.thePlayer==null && mc.theWorld==null && mc.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu && waiting>=200) {
            java.lang.reflect.Field connection=Minecraft.class.getDeclaredField("myNetworkManager"); connection.setAccessible(true); net.minecraft.network.NetworkManager manager=(net.minecraft.network.NetworkManager)connection.get(mc);
            System.out.println("Quest preview initial login retry: manager="+(manager==null?"null":"open="+manager.isChannelOpen()+", handler="+manager.getNetHandler().getClass().getSimpleName()));
            if(loginRetries++>=1) throw new IllegalStateException("Initial integrated-server login failed twice");
            waiting=0; mc.launchIntegratedServer(worldName,"Quest smoke fixture",null); return;
        }
        if(mc.thePlayer==null || mc.theWorld==null || waiting%5!=0) return;
        long realNow=System.nanoTime(); if(realNow-lastSmokeStep<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(200)) return; lastSmokeStep=realNow;
        QuestNetwork.Snapshot snap=ClientProxy.snapshot;
        switch(stage) {
            case 0: if(waiting<20 || snap==null || snap.book==null) return;
                if(!nativePassed && !restarted) { nativeRequested=true; stage=26; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("customnpcs") && !npcPassed && !restarted) { npcRequested=true; stage=25; break; }
                action("view"); stage=1; break;
            case 26:
                if(nativePassed) { int[] position=NativeQuestSmoke.storagePosition(); if(!(mc.theWorld.getTileEntity(position[0],position[1],position[2]) instanceof QuestStorage)) return; QuestToasts.clear(); openStorageRequested=true; stage=42; waiting=0; } break;
            case 42:
                if(!(mc.currentScreen instanceof QuestStorageScreen) || waiting<15) return;
                if(!(mc.thePlayer.openContainer instanceof QuestStorageContainer) || ((QuestStorageContainer)mc.thePlayer.openContainer).reserved!=4999999937L) return;
                QuestToasts.clear(); ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-virtual-storage.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,10); stage=43; break;
            case 43:
                if(snap==null || !snap.message.equals("Storage page changed")) return;
                if(((QuestStorageContainer)mc.thePlayer.openContainer).reserved!=4999999937L) throw new IllegalStateException("Storage paging changed reserve count");
                mc.thePlayer.closeScreen(); stage=0; break;
            case 31:
                if(waiting<20) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-nei.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                neiPassed=true; mc.displayGuiScreen(null); stage=1; break;
            case 30: if(bcPassed) { QuestNetwork.Action afterBC=new QuestNetwork.Action(); afterBC.action="start"; afterBC.id="smoke-manual"; ClientProxy.send(afterBC); stage=9; } break;
            case 25: if(npcPassed) stage=0; break;
            case 27:
                if(!"Fixture team".equals(snap.team)) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-teams.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,1); stage=28; break;
            case 28:
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-leaderboard.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,1); press(mc.currentScreen,15); stage=29; break;
            case 29: if(snap.team.isEmpty()) { teamPassed=true; mc.displayGuiScreen(null); stage=1; } break;
            case 1:
                if(snap==null || snap.book==null) return;
                if(!snap.canEdit) throw new IllegalStateException("Integrated host lost edit permissions");
                if(restarted) {
                    if(nativePassed && !nativeVerified) { nativeVerify=true; return; }
                    if(npcPassed && !npcVerified) { npcVerify=true; return; }
                    if(emcPassed && QuestProjectE.balance(mc.thePlayer)!=36) throw new IllegalStateException("ProjectE EMC did not survive world restart");
                    if(pinPassed && (!snap.pinned.contains("welcome") || snap.percentages.getOrDefault("welcome",0)!=100)) throw new IllegalStateException("Pinned quest did not survive world restart");
                    if(!"claimed".equals(snap.states.get("welcome")) || bread(mc)!=4) throw new IllegalStateException("World persistence failed");
                    net.minecraft.tileentity.TileEntity savedDisplay=mc.theWorld.getTileEntity(deviceX,deviceY,deviceZ); if(!(savedDisplay instanceof QuestDevice)) return; QuestDevice display=(QuestDevice)savedDisplay;
                    if(!display.indestructible || !display.screenSkin.equals("minecraft:wool") || display.screenSkinMeta!=14 || !display.owner.toString().equals(NativeQuestSmoke.offlineOwnerId())) throw new IllegalStateException("Screen skin, protection or team did not survive restart");
                    java.nio.file.Files.write(new File(mc.mcDataDir,"../build/preview/smoke.txt").toPath(),"PASS: real client/server handshake, completion, one-time claim, inventory sync, editing, stale revision rejection, world restart persistence, manual task confirmation, start action, task dependency sync, graphical toast, guide rendering, external guide events and registry, pinned HUD and restart persistence, notification history, custom click callback, confirmed file action, permission-checked network command, object form save, native device container, checked device binding, linked 2x2 world screen and synchronized progress, world chapter map, fullscreen/hideIcons, input-only held icon, configured public detectors, scripted task button, conditional shop hook, offline collectors and persistent crate-opener roll".concat(iconPassed?", custom icon editor, asynchronous HTTP textures and synchronized texture/name/tint":"").concat(euPassed?", real IC2 EnergyNet transfer":"").concat(manaPassed?", real Botania mana burst":"").concat(teamPassed?", team GUI, leaderboard and server-checked disband":"").concat(bcPassed?", real BuildCraft engine and RF/MJ conversion":"").concat(presentationPassed?", nested chapter groups, inherited shapes, map illustration link, empty quest and chapter-bound screen":"").concat(neiPassed?", real NEI recipes, uses, loot and GUI":"").concat(nativePassed?", native JavaScript, stages, skills, pack mode isolation and restart":"").concat(npcPassed?", real CustomNPCs tasks/rewards and restart persistence":"").concat(emcPassed?", real ProjectE wallet submission, sync and persistence":"").getBytes(StandardCharsets.UTF_8));
                    PreviewImageServer.stop(); mc.shutdown(); return;
                }
                if(!teamPassed) {
                    QuestTeamScreen teams=new QuestTeamScreen(new QuestBookScreen()); mc.displayGuiScreen(teams);
                    java.lang.reflect.Field input=QuestTeamScreen.class.getDeclaredField("input"); input.setAccessible(true); ((net.minecraft.client.gui.GuiTextField)input.get(teams)).setText("Fixture team");
                    press(teams,10); stage=27; return;
                }
                if(!iconPassed) { iconRequested=true; stage=39; return; }
                if(!bookPrepared) {
                    QuestNetwork.Action prepare=new QuestNetwork.Action(); prepare.action="save"; prepare.revision=snap.revision; prepare.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(snap.book),QuestCore.Book.class);
                    if(QuestPresentation.chapter(prepare.book,"start")==null) { QuestCore.Chapter startChapter=new QuestCore.Chapter(); startChapter.id="start"; startChapter.title="Start"; prepare.book.chapters.add(startChapter); }
                    for(QuestCore.Quest entry:prepare.book.quests) if(entry.id.equals("welcome") || entry.id.equals("stone")) entry.chapter="start";
                    ClientProxy.send(prepare); stage=38; return;
                }
                if(cpw.mods.fml.common.Loader.isModLoaded("NotEnoughItems") && !neiPassed) {
                    Class.forName("dev.sigmarix.quests.preview.NEIQuestSmoke").getMethod("run").invoke(null); stage=31; waiting=0; return;
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
                if(cpw.mods.fml.common.Loader.isModLoaded("Botania")) {
                    QuestCore.Quest mana=new QuestCore.Quest(); mana.id="smoke-mana"; mana.title="Botania mana";
                    QuestCore.Task charge=new QuestCore.Task(); charge.id="charge"; charge.type="botania_mana"; charge.count=16; mana.tasks.add(charge); edit.book.quests.add(mana);
                }
                if(cpw.mods.fml.common.Loader.isModLoaded("ProjectE")) {
                    QuestCore.Quest emc=new QuestCore.Quest(); emc.id="smoke-emc"; emc.title="ProjectE EMC";
                    QuestCore.Task wallet=new QuestCore.Task(); wallet.id="wallet"; wallet.type="emc"; wallet.count=64; wallet.consume=true; emc.tasks.add(wallet);
                    QuestCore.Task other=new QuestCore.Task(); other.id="other"; other.type="dimension"; other.dimension=-1; emc.tasks.add(other); edit.book.quests.add(emc);
                }
                if(cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) {
                    QuestCore.Quest mj=new QuestCore.Quest(); mj.id="smoke-mj"; mj.title="BuildCraft MJ bridge";
                    QuestCore.Task charge=new QuestCore.Task(); charge.id="charge"; charge.type="buildcraft_mj"; charge.count=1600000; mj.tasks.add(charge); edit.book.quests.add(mj);
                }
                QuestCore.Chapter group=new QuestCore.Chapter(); group.id="smoke-group"; group.title="Grouped quests"; group.defaultQuestShape="hexagon"; edit.book.chapters.add(group);
                QuestCore.Chapter start=QuestPresentation.chapter(edit.book,"start"); start.parent=group.id;
                for(QuestCore.Quest entry:edit.book.quests) if(entry.chapter.equals("start")) entry.shape="";
                QuestPresentation.MapImage illustration=new QuestPresentation.MapImage(); illustration.x=1.5; illustration.y=.5; illustration.width=1.5; illustration.height=1.5; illustration.rotation=15; illustration.resource=remoteImage; illustration.uri="https://example.org/map-guide"; illustration.hover.add("Map illustration"); start.images.add(illustration);
                QuestCore.Quest empty=new QuestCore.Quest(); empty.id="smoke-empty"; empty.title="Dependency-only"; empty.subtitle="Empty quests still require their dependencies"; empty.minWidth=240; empty.chapter="start"; empty.x=6; empty.y=3; empty.dependencies.add("welcome"); edit.book.quests.add(empty);
                int shapeNumber=0; for(String shape:new String[]{"rsquare","heart","gear","minecraft:textures/items/diamond.png"}) { QuestCore.Quest shapeQuest=new QuestCore.Quest(); shapeQuest.id="shape_"+shapeNumber; shapeQuest.title=shape; shapeQuest.shape=shape; shapeQuest.optional=true; shapeQuest.chapter="start"; shapeQuest.x=shapeNumber++*1.2; shapeQuest.y=.95; QuestCore.Task shapeTask=new QuestCore.Task(); shapeTask.id="check"; shapeTask.type="check"; shapeQuest.tasks.add(shapeTask); edit.book.quests.add(shapeQuest); }
                QuestCore.Quest commandQuest=new QuestCore.Quest(); commandQuest.id="smoke-command-click"; commandQuest.title="Command action"; commandQuest.customClick="command:/quests stage "+mc.thePlayer.getCommandSenderName()+" native-click add"; edit.book.quests.add(commandQuest);
                QuestCore.Quest manual=new QuestCore.Quest(); manual.id="smoke-manual"; manual.title="Manual confirmation"; manual.guidePage="preview:event"; manual.customClick="custom:preview:click"; manual.shape="diamond"; manual.tags.add("important"); manual.pages.add("Guide page one\nA working independent quest guide."); manual.pages.add("Guide page two"); manual.uri="https://example.org/guide";
                QuestPresentation.GuidePage rich=new QuestPresentation.GuidePage(); rich.title="Rich guide page";
                QuestPresentation.GuideElement text=new QuestPresentation.GuideElement(); text.text="Formatted text, resource images, item illustrations and confirmed links."; rich.elements.add(text);
                QuestPresentation.GuideElement image=new QuestPresentation.GuideElement(); image.type="image"; image.resource=remoteImage; image.width=64; image.height=64; rich.elements.add(image);
                QuestPresentation.GuideElement itemIllustration=new QuestPresentation.GuideElement(); itemIllustration.type="item"; itemIllustration.item="minecraft:diamond"; rich.elements.add(itemIllustration);
                QuestPresentation.GuideElement link=new QuestPresentation.GuideElement(); link.type="link"; link.text="Open the guide"; link.uri="https://example.org/guide"; rich.elements.add(link); manual.richPages.add(rich);
                QuestPresentation.Style style=new QuestPresentation.Style(); style.selector=".important state:available"; style.color="FF9B59B6"; edit.book.styles.add(style);
                QuestCore.Reward toast=new QuestCore.Reward(); toast.type="toast"; toast.target="Graphical reward"; toast.text="Native quest notification"; manual.rewards.add(toast);
                QuestCore.Task check=new QuestCore.Task(); check.id="confirm"; check.type="check"; check.manual=true; check.title="Confirm with custom icon"; check.description="A named task with a resource-pack icon"; check.icon="sigmarixquests:custom_icon"; check.iconNbt="{icon:"+QuestService.JSON.toJson(remoteImage)+",color:16763972}"; manual.tasks.add(check); QuestCore.Task callback=new QuestCore.Task(); callback.id="callback"; callback.type="custom"; callback.target=""; callback.manual=true; callback.count=3; callback.title="Server script action"; manual.tasks.add(callback); edit.book.quests.add(manual);
                QuestCore.Quest dependent=new QuestCore.Quest(); dependent.id="smoke-dependent"; dependent.title="Task dependency"; dependent.dependencies.add("task:smoke-manual/confirm");
                QuestCore.Task next=new QuestCore.Task(); next.type="check"; dependent.tasks.add(next); edit.book.quests.add(dependent);
                pendingBookEdit=edit; lastBookAttempt=System.nanoTime(); ClientProxy.send(edit); stage=5; break;
            case 5:
                if(snap==null) return;
                if(snap.revision==oldRevision) { if(pendingBookEdit!=null && realNow-lastBookAttempt>java.util.concurrent.TimeUnit.SECONDS.toNanos(1)) { ClientProxy.send(pendingBookEdit); lastBookAttempt=realNow; } return; }
                if(!snap.book.title.equals("Network edit smoke")) throw new IllegalStateException("Editor did not save");
                QuestNetwork.Action stale=new QuestNetwork.Action(); stale.action="save"; stale.revision=oldRevision; stale.book=original;
                ClientProxy.send(stale); stage=6; break;
            case 6:
                if(!pinPassed) { if(snap==null || !snap.message.startsWith("Error: Book changed")) return; QuestBookScreen pinBook=new QuestBookScreen(); mc.displayGuiScreen(pinBook); java.lang.reflect.Field selectedPin=QuestBookScreen.class.getDeclaredField("selected"); selectedPin.setAccessible(true); selectedPin.set(pinBook,"welcome"); pinBook.refresh(); press(pinBook,13); stage=50; waiting=0; return; }
                if(!presentationPassed) {
                    if(snap==null) return;
                    QuestToasts.clear(); QuestBookScreen map=new QuestBookScreen(); mc.displayGuiScreen(map); press(map,11); press(map,2);
                    java.lang.reflect.Field chosen=QuestBookScreen.class.getDeclaredField("selected"); chosen.setAccessible(true); chosen.set(map,"smoke-empty"); map.refresh(); press(map,1); stage=36; waiting=0; return;
                }
                if(cpw.mods.fml.common.Loader.isModLoaded("IC2")) { euRequested=true; stage=20; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("Botania")) { manaRequested=true; stage=21; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("ProjectE")) { emcRequested=true; stage=22; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) { bcRequested=true; stage=30; break; }
                QuestNetwork.Action begin=new QuestNetwork.Action(); begin.action="start"; begin.id="smoke-manual"; ClientProxy.send(begin); stage=9; break;
            case 20:
                if(!euPassed) return;
                if(cpw.mods.fml.common.Loader.isModLoaded("Botania")) { manaRequested=true; stage=21; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("ProjectE")) { emcRequested=true; stage=22; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) { bcRequested=true; stage=30; break; }
                QuestNetwork.Action afterEU=new QuestNetwork.Action(); afterEU.action="start"; afterEU.id="smoke-manual"; ClientProxy.send(afterEU); stage=9; break;
            case 21:
                if(!manaPassed) return;
                if(cpw.mods.fml.common.Loader.isModLoaded("ProjectE")) { emcRequested=true; stage=22; break; }
                if(cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) { bcRequested=true; stage=30; break; }
                QuestNetwork.Action afterMana=new QuestNetwork.Action(); afterMana.action="start"; afterMana.id="smoke-manual"; ClientProxy.send(afterMana); stage=9; break;
            case 22:
                if(!emcReady || snap==null) return;
                QuestCore.Quest walletQuest=null; for(QuestCore.Quest q:snap.book.quests) if(q.id.equals("smoke-emc")) walletQuest=q;
                if(walletQuest==null) throw new IllegalStateException("EMC quest missing from snapshot");
                mc.displayGuiScreen(new QuestTaskScreen(null,walletQuest)); stage=23; waiting=0; break;
            case 23:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-emc.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Method submit=QuestTaskScreen.class.getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class); submit.setAccessible(true); submit.invoke(mc.currentScreen,new net.minecraft.client.gui.GuiButton(100,0,0,"submit")); stage=24; break;
            case 24:
                if(!emcPassed || snap==null || !snap.message.startsWith("Submitted: 64") || QuestProjectE.balance(mc.thePlayer)!=36) return;
                if(cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) { bcRequested=true; stage=30; break; }
                QuestNetwork.Action afterEMC=new QuestNetwork.Action(); afterEMC.action="start"; afterEMC.id="smoke-manual"; ClientProxy.send(afterEMC); stage=9; break;
            case 9:
                if(snap==null || !snap.message.equals("Quest started")) return;
                QuestNetwork.Action premature=new QuestNetwork.Action(); premature.action="check"; premature.id="smoke-manual"; ClientProxy.send(premature); stage=10; break;
            case 10:
                if(snap==null || !snap.message.startsWith("Error: Tasks incomplete")) return;
                QuestNetwork.Action confirm=new QuestNetwork.Action(); confirm.action="task"; confirm.id="smoke-manual"; confirm.text="confirm"; ClientProxy.send(confirm); stage=11; break;
            case 11:
                if(snap==null || !snap.message.equals("Task confirmed")) return;
                if(!Boolean.TRUE.equals(snap.taskStates.get("smoke-manual:confirm")) || !"available".equals(snap.states.get("smoke-dependent"))) throw new IllegalStateException("Task dependency did not unlock");
                QuestToasts.clear(); mc.displayGuiScreen(new QuestTaskScreen(null,snap.book.quests.get(snap.book.quests.size()-2))); stage=12; waiting=0; break;
            case 12:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-tasks.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,101); stage=72; waiting=0; break;
            case 72:
                if(snap==null || waiting<15 || !snap.message.equals("Task action sent") || !Boolean.TRUE.equals(snap.taskStates.get("smoke-manual:callback"))) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-task-action.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                QuestNetwork.Action finish=new QuestNetwork.Action(); finish.action="check"; finish.id="smoke-manual"; ClientProxy.send(finish); stage=13; break;
            case 13:
                if(snap==null || !"complete".equals(snap.states.get("smoke-manual"))) return;
                QuestNetwork.Action collect=new QuestNetwork.Action(); collect.action="claim"; collect.id="smoke-manual"; ClientProxy.send(collect); stage=14; break;
            case 14:
                if(snap==null || !"claimed".equals(snap.states.get("smoke-manual"))) return;
                QuestGuideScreen guide=new QuestGuideScreen(null,snap.book.quests.get(snap.book.quests.size()-2)); mc.displayGuiScreen(guide);
                java.lang.reflect.Field guidePage=QuestGuideScreen.class.getDeclaredField("page"); guidePage.setAccessible(true); guidePage.setInt(guide,2); stage=15; waiting=0; break;
            case 15:
                if(waiting<20) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-guide-toast.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Field guideScroll=QuestGuideScreen.class.getDeclaredField("scroll"); guideScroll.setAccessible(true); guideScroll.setInt(mc.currentScreen,50); stage=35; waiting=0; break;
            case 50:
                if(!snap.pinned.contains("welcome") || snap.percentages.getOrDefault("welcome",0)!=100) return;
                QuestToasts.clear(); mc.displayGuiScreen(null); stage=51; waiting=0; break;
            case 51:
                if(waiting<15) return; ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-pinned-hud.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.displayGuiScreen(new QuestPinScreen(new QuestBookScreen())); stage=52; waiting=0; break;
            case 52:
                if(waiting<10) return; ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-pins.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); press(mc.currentScreen,1); stage=53; waiting=0; break;
            case 53:
                if(waiting<10) return; if(QuestToasts.history().isEmpty()) throw new IllegalStateException("Notification history was lost when transient toasts were cleared");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-notifications.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                for(int i=0;i<71;i++) { QuestNetwork.Toast probe=new QuestNetwork.Toast(); probe.title="History probe "+i; QuestToasts.enqueue(QuestService.JSON.toJson(probe)); QuestToasts.pump(); }
                if(QuestToasts.history().size()!=64 || !QuestToasts.history().get(0).title.equals("History probe 70")) throw new IllegalStateException("Notification history was not bounded or ordered"); QuestToasts.clear(); if(QuestToasts.history().size()!=64) throw new IllegalStateException("Transient toast reset erased history");
                pinPassed=true; mc.displayGuiScreen(null); stage=6; break;
            case 39:
                if(!iconReady || mc.thePlayer.getHeldItem()==null || !(mc.thePlayer.getHeldItem().getItem() instanceof QuestBlocks.CustomIconItem)) return;
                QuestIconScreen iconEditor=new QuestIconScreen(); mc.displayGuiScreen(iconEditor);
                for(String[] field:new String[][]{{"resource",remoteImage},{"name","Native icon"},{"color","FFCC44"}}) {
                    java.lang.reflect.Field input=QuestIconScreen.class.getDeclaredField(field[0]); input.setAccessible(true); ((net.minecraft.client.gui.GuiTextField)input.get(iconEditor)).setText(field[1]);
                }
                stage=40; break;
            case 40:
                QuestToasts.clear();
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-custom-icon.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,0); stage=41; break;
            case 41:
                net.minecraft.item.ItemStack icon=mc.thePlayer.getHeldItem();
                if(icon==null || !icon.hasTagCompound() || !"Native icon".equals(icon.getTagCompound().getString("name"))) return;
                if(!QuestIcons.resource(icon).equals(remoteImage) || QuestIcons.color(icon)!=0xFFCC44) throw new IllegalStateException("Custom icon edit was not synchronized");
                if(!QuestImages.loaded(remoteImage)) return;
                net.minecraft.util.ResourceLocation cached=QuestImages.texture(remoteImage);
                int textureId=mc.getTextureManager().getTexture(cached).getGlTextureId(); QuestImages.clear();
                if(mc.getTextureManager().getTexture(cached)!=net.minecraft.client.renderer.texture.TextureUtil.missingTexture || org.lwjgl.opengl.GL11.glIsTexture(textureId)) throw new IllegalStateException("Remote texture cache did not release CPU/GPU storage");
                iconPassed=true; stage=1; break;
            case 38: if(QuestPresentation.chapter(snap.book,"start")!=null && snap.book.quests.stream().anyMatch(q->q.id.equals("welcome") && q.chapter.equals("start"))) { bookPrepared=true; stage=1; } break;
            case 36:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-grouped-map.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                QuestBookScreen map=(QuestBookScreen)mc.currentScreen; QuestPresentation.MapImage mapImage=QuestPresentation.chapter(snap.book,"start").images.get(0);
                java.lang.reflect.Method imageX=QuestBookScreen.class.getDeclaredMethod("imageX",QuestPresentation.MapImage.class),imageY=QuestBookScreen.class.getDeclaredMethod("imageY",QuestPresentation.MapImage.class); imageX.setAccessible(true); imageY.setAccessible(true);
                java.lang.reflect.Method mapClick=QuestBookScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class); mapClick.setAccessible(true); mapClick.invoke(map,((Double)imageX.invoke(map,mapImage)).intValue(),((Double)imageY.invoke(map,mapImage)).intValue(),0);
                if(!(mc.currentScreen instanceof net.minecraft.client.gui.GuiConfirmOpenLink)) throw new IllegalStateException("Map illustration skipped link confirmation"); map.confirmClicked(false,1000);
                QuestNetwork.Action emptyCheck=new QuestNetwork.Action(); emptyCheck.action="check"; emptyCheck.id="smoke-empty"; ClientProxy.send(emptyCheck); stage=37; break;
            case 37:
                if(!"complete".equals(snap.states.get("smoke-empty"))) return;
                presentationPassed=true; mc.displayGuiScreen(null); stage=6; break;
            case 35:
                if(waiting<10) return;
                QuestGuideScreen richGuide=(QuestGuideScreen)mc.currentScreen; java.lang.reflect.Field regions=QuestGuideScreen.class.getDeclaredField("links"); regions.setAccessible(true);
                java.util.Map<int[],String> links=(java.util.Map<int[],String>)regions.get(richGuide); if(links.isEmpty()) throw new IllegalStateException("Rich guide link was not rendered after scrolling");
                int[] bounds=links.keySet().iterator().next(); java.lang.reflect.Method click=QuestGuideScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class); click.setAccessible(true); click.invoke(richGuide,(bounds[0]+bounds[2])/2,(bounds[1]+bounds[3])/2,0);
                if(!(mc.currentScreen instanceof net.minecraft.client.gui.GuiConfirmOpenLink)) throw new IllegalStateException("Guide link skipped confirmation");
                richGuide.confirmClicked(false,4);
                java.lang.reflect.Field externalPage=QuestGuideScreen.class.getDeclaredField("page"); externalPage.setAccessible(true); externalPage.setInt(richGuide,3); stage=44; waiting=0; break;
            case 44:
                if(waiting<15) return; press(mc.currentScreen,5);
                if(GuideSmoke.eventOpens!=1 || !(mc.currentScreen instanceof QuestGuideScreen)) throw new IllegalStateException("Quest guide_page did not dispatch its external guide event");
                stage=45; waiting=0; break;
            case 45:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-external-guide.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                GuideSmoke.registry(mc.currentScreen); QuestBookScreen guideBook=new QuestBookScreen(); mc.displayGuiScreen(guideBook); press(guideBook,12); if(GuideSmoke.indexOpens!=1) throw new IllegalStateException("Global guide index did not open");
                if(cpw.mods.fml.common.Loader.isModLoaded("Botania")) { Class.forName("dev.sigmarix.quests.preview.BotaniaGuideSmoke").getMethod("open").invoke(null); stage=46; waiting=0; break; }
                stage=47; waiting=0; break;
            case 46:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-botania-guide.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); stage=47; waiting=0; break;
            case 47:
                QuestCore.Quest actionQuest=snap.book.quests.stream().filter(q->q.id.equals("smoke-manual")).findFirst().get(); QuestGuideScreen actionGuide=new QuestGuideScreen(null,actionQuest); mc.displayGuiScreen(actionGuide);
                java.lang.reflect.Field actionPage=QuestGuideScreen.class.getDeclaredField("page"); actionPage.setAccessible(true); actionPage.setInt(actionGuide,4); stage=48; waiting=0; break;
            case 48:
                if(waiting<10) return; press(mc.currentScreen,6); if(GuideSmoke.clickOpens!=1) throw new IllegalStateException("Custom quest click event did not fire");
                java.io.File local=java.io.File.createTempFile("quest-click-",".txt",mc.mcDataDir); java.nio.file.Files.write(local.toPath(),"Native click fixture".getBytes(StandardCharsets.UTF_8));
                if(!QuestClicks.open("smoke-manual","file:"+local.getCanonicalPath(),mc.currentScreen) || !(mc.currentScreen instanceof net.minecraft.client.gui.GuiYesNo)) throw new IllegalStateException("Local file action skipped confirmation");
                press(mc.currentScreen,1); java.nio.file.Files.delete(local.toPath());
                try { QuestClicks.file("../README.md"); throw new IllegalStateException("File action escaped Minecraft directory"); } catch(IllegalArgumentException correct) {}
                QuestCore.Quest commandDefinition=snap.book.quests.stream().filter(q->q.id.equals("smoke-command-click")).findFirst().get(); QuestClicks.open(commandDefinition.id,commandDefinition.customClick,mc.currentScreen); stage=49; waiting=0; break;
            case 49:
                if(!commandVerified || !"Command sent".equals(snap.message)) return;
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
                QuestToasts.clear(); mc.displayGuiScreen(null); mc.thePlayer.rotationYaw=-90; mc.thePlayer.rotationPitch=30; placeDevice=true; stage=32; waiting=0; break;
            case 32:
                if(waiting<30) return;
                if(mc.currentScreen!=null) throw new IllegalStateException("Device container opened before world screen preview");
                net.minecraft.tileentity.TileEntity rendered=mc.theWorld.getTileEntity(deviceX,deviceY,deviceZ);
                if(!(rendered instanceof QuestDevice) || ((QuestDevice)rendered).screenWidth!=2 || ((QuestDevice)rendered).screenHeight!=2 || ((QuestDevice)rendered).screenPercent!=100) throw new IllegalStateException("World screen dimensions/progress did not synchronize");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-world-screen.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); stage=18; break;
            case 18:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || snap==null) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-device.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Field progressReference=QuestDeviceScreen.class.getDeclaredField("quest"); progressReference.setAccessible(true); ((net.minecraft.client.gui.GuiTextField)progressReference.get(mc.currentScreen)).setText("chapter:start");
                java.lang.reflect.Method bind=QuestDeviceScreen.class.getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class); bind.setAccessible(true); bind.invoke(mc.currentScreen,new net.minecraft.client.gui.GuiButton(0,0,0,"Bind")); stage=19; break;
            case 19:
                if(snap==null || !snap.message.equals("Device binding updated")) return;
                QuestDeviceContainer progressContainer=(QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots;
                int requiredQuests=0,completedQuests=0;
                for(QuestCore.Quest entry:snap.book.quests) if(QuestCore.required(entry) && QuestPresentation.within(snap.book,entry.chapter,"start")) { requiredQuests++; if("complete".equals(snap.states.get(entry.id)) || "claimed".equals(snap.states.get(entry.id))) completedQuests++; }
                int expectedProgress=requiredQuests==0?100:(int)Math.round(completedQuests*100D/requiredQuests);
                if(!progressContainer.tile.quest.equals("chapter:start") || progressContainer.percent!=expectedProgress) return;
                mc.thePlayer.closeScreen(); QuestToasts.clear(); mc.gameSettings.hideGUI=true; stage=63; waiting=0; break;
            case 63:
                if(waiting<20) return;
                QuestDevice graphTile=(QuestDevice)mc.theWorld.getTileEntity(deviceX,deviceY,deviceZ); QuestWorldMap graph=QuestWorldMaps.get(graphTile);
                if(graph==null || !graph.reference.equals("chapter:start") || graph.values.isEmpty()) return;
                if(graph.values.getOrDefault("welcome",0)/101!=3) throw new IllegalStateException("World chapter map lost its owner's claimed state");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-world-map.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.gameSettings.hideGUI=false; openProgressRequested=true; stage=66; waiting=0; break;
            case 66:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20) return;
                press(mc.currentScreen,9); stage=64; waiting=0; break;
            case 64:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || !((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenFullscreen) return;
                press(mc.currentScreen,10); stage=65; waiting=0; break;
            case 65:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || !((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenHideIcons) return;
                skinRequested=true; stage=76; waiting=0; break;
            case 76:
                if(waiting<20 || mc.thePlayer.getHeldItem()==null || mc.thePlayer.getHeldItem().getItem()!=net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.wool)) return;
                press(mc.currentScreen,11); stage=77; waiting=0; break;
            case 77:
                if(waiting<20 || !"minecraft:wool".equals(((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenSkin)) return;
                if(((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenSkinMeta!=14 || mc.thePlayer.getHeldItem().stackSize!=1) throw new IllegalStateException("Screen skin metadata or held inventory changed");
                press(mc.currentScreen,12); stage=79; waiting=0; break;
            case 79:
                if(waiting<20 || !((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.indestructible) return;
                java.lang.reflect.Field ownerField=QuestDeviceScreen.class.getDeclaredField("owner"); ownerField.setAccessible(true); ((net.minecraft.client.gui.GuiTextField)ownerField.get(mc.currentScreen)).setText(NativeQuestSmoke.offlineOwnerId()); press(mc.currentScreen,13); stage=80; waiting=0; break;
            case 80:
                if(waiting<20 || !NativeQuestSmoke.offlineOwnerId().equals(((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.owner.toString())) return;
                QuestDevice screenPart=(QuestDevice)mc.theWorld.getTileEntity(deviceX,deviceY+1,deviceZ+1);
                if(!screenPart.indestructible || screenPart.screenSkinMeta!=14 || !screenPart.owner.equals(((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.owner)) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-screen-options.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.closeScreen(); QuestToasts.clear(); mc.gameSettings.hideGUI=true; stage=67; waiting=0; break;
            case 67:
                if(waiting<20) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-world-map-fullscreen.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                skinViewX=mc.thePlayer.posX; skinViewY=mc.thePlayer.posY; skinViewZ=mc.thePlayer.posZ; mc.thePlayer.setPosition(skinViewX,skinViewY,skinViewZ-3); mc.thePlayer.rotationYaw=-34; mc.thePlayer.rotationPitch=10; stage=78; waiting=0; break;
            case 78:
                if(waiting<20) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-screen-skin-world.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.setPosition(skinViewX,skinViewY,skinViewZ); mc.thePlayer.rotationYaw=-90; mc.thePlayer.rotationPitch=17; mc.gameSettings.hideGUI=false; detectorRequested=true; stage=60; waiting=0; break;
            case 60:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20) return;
                java.lang.reflect.Field settingsFields=QuestDeviceScreen.class.getDeclaredField("detectorFields"); settingsFields.setAccessible(true); net.minecraft.client.gui.GuiTextField[] fields=(net.minecraft.client.gui.GuiTextField[])settingsFields.get(mc.currentScreen);
                if(fields.length!=4) throw new IllegalStateException("Detector settings fields missing");
                String[] settingsValues={"2.5","-1","5","12"}; for(int i=0;i<4;i++) fields[i].setText(settingsValues[i]);
                press(mc.currentScreen,6); stage=61; waiting=0; break;
            case 61:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || snap==null || !snap.message.equals("Detector settings updated")) return;
                QuestDevice configuredDetector=((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile;
                if(configuredDetector.detectorRadius!=2.5 || configuredDetector.detectorOffsetY!=-1 || configuredDetector.detectorHeight!=5 || configuredDetector.detectorPower!=12) throw new IllegalStateException("Server detector settings did not synchronize");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-detector-settings.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.closeScreen(); inputRequested=true; stage=68; waiting=0; break;
            case 68:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20) return;
                press(mc.currentScreen,9); stage=69; waiting=0; break;
            case 69:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || !((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenInputOnly) return;
                press(mc.currentScreen,10); stage=70; waiting=0; break;
            case 70:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || ((QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots).tile.screenInputIcon==null) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-input-options.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.closeScreen(); openCollectorRequested=true; stage=62; waiting=0; break;
            case 62:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20) return;
                QuestDeviceContainer collectorContainer=(QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots;
                if(!collectorContainer.tile.collector() || collectorContainer.inventorySlots.size()!=45 || collectorContainer.tile.getStackInSlot(0)==null) throw new IllegalStateException("Nine-slot collector inventory did not synchronize");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-reward-collector.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.closeScreen(); openOpenerRequested=true; stage=75; waiting=0; break;
            case 75:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20) return;
                QuestDeviceContainer openerContainer=(QuestDeviceContainer)((QuestDeviceScreen)mc.currentScreen).inventorySlots;
                if(!openerContainer.tile.opener() || openerContainer.inventorySlots.size()!=46 || openerContainer.queuedLoot!=2) throw new IllegalStateException("Crate opener inventory or pending counter did not synchronize");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-crate-opener.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.thePlayer.closeScreen(); QuestBookScreen screen=new QuestBookScreen(); mc.displayGuiScreen(screen); stage=73; waiting=0; break;
            case 73:
                if(waiting<20) return;
                press(mc.currentScreen,15); stage=74; waiting=0; break;
            case 74:
                if(waiting<20) return;
                if(GuideSmoke.shopOpens<1 || !(mc.currentScreen instanceof QuestGuideScreen)) throw new IllegalStateException("Conditional shop button failed to open its provider");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-shop-hook.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.displayGuiScreen(new QuestBookScreen()); stage=8; waiting=0; break;
            case 8:
                if(waiting<30) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-book-live.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.theWorld.sendQuittingDisconnectingPacket(); mc.loadWorld(null); restarted=true; stage=0; waiting=0; ClientProxy.snapshot=null;
                mc.launchIntegratedServer(worldName,"Quest smoke fixture",null); return;
            default: throw new IllegalStateException("Unknown smoke stage");
        }

    }
}