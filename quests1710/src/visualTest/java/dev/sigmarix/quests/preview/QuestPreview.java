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
    private String renderMode="",captureName="";private int captureIndex;private String[] captureModes;
    private int ticks,stage,waiting,loginRetries,lastServerStep=-5;
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
    private boolean iconPassed,pinPassed,classicUiPassed,textFramePassed;
    private volatile boolean uiItemRequested;
    private volatile boolean openStorageRequested,commandVerified;
    private boolean bcRunning;
    private boolean emcRunning;
    private boolean manaRunning;
    private boolean euRunning;
    private int detectorDelay,deviceDelay,deviceX,deviceY,deviceZ;
    @Mod.EventHandler public void init(FMLInitializationEvent event) throws Exception {
        remoteImage=PreviewImageServer.start();
        java.util.List<String> expected=java.nio.file.Files.readAllLines(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/expected-mods.txt").toPath(),StandardCharsets.UTF_8);
        String[][] optional={{"IC2","ic2.api.energy.tile.IEnergySink"},{"Botania","vazkii.botania.api.mana.IManaReceiver"},{"ProjectE","moze_intel.projecte.playerData.Transmutation"},{"customnpcs","noppes.npcs.scripted.ScriptPlayer"},{"BuildCraft|Core","buildcraft.BuildCraftCore"},{"NotEnoughItems","codechicken.nei.api.API"}};
        for(String[] mod:optional) {
            boolean loaded=cpw.mods.fml.common.Loader.isModLoaded(mod[0]); if(loaded!=expected.contains(mod[0])) throw new IllegalStateException("Unexpected optional mod state: "+mod[0]+"="+loaded);
            if(!loaded) try { Class.forName(mod[1],false,getClass().getClassLoader()); throw new IllegalStateException("Compile-only API leaked into game runtime: "+mod[0]); } catch(ClassNotFoundException correct) {}
        }
        renderMode=new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/frame-only").exists()?"frame":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/table-only").exists()?"table":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/map-only").exists()?"map":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/file-only").exists()?"file":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/image-only").exists()?"image":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/fluid-only").exists()?"fluid":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/picker-only").exists()?"picker":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/card-only").exists()?"card":new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/catalog-only").exists()?"catalog":"";
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/parity-only").exists()){captureModes=new String[]{"map","map_hover","file","image","table","table_crate","card","fluid","picker","catalog"};captureName=captureModes[0];renderMode="map";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/values-only").exists()){captureModes=new String[]{"values_nbt","values_nbt-data","values_stack","values_stack-text","values_stack-empty","values_count","values_meta","values_name","values_invalid-count"};captureName=captureModes[0];renderMode="values";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/aux-only").exists()){captureModes=new String[]{"aux_tables-empty","aux_table-editor-empty","aux_tables-filled","aux_table-editor-filled","aux_choice-empty","aux_choice-filled","values_stack-empty","emergency_empty","emergency_filled","notices_empty","notices_filled"};captureName=captureModes[0];renderMode="aux";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/devices-only").exists()){captureModes=new String[]{"devices_task","devices_progress","devices_barrier","devices_collector","devices_redstone","devices_player","devices_progress-detector","devices_chest"};captureName=captureModes[0];renderMode="devices";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/context-only").exists()){captureModes=new String[]{"context","pinned_active","pinned_complete"};captureName="context";renderMode="context";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/toasts-only").exists()){captureModes=new String[]{"toast_custom","toast_long-title","toast_long-subtitle","toast_chapter","toast_task","toast_quest","toast_reward"};captureName=captureModes[0];renderMode="toast";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/valid-only").exists()){captureModes=new String[]{"valid_0","valid_1","valid_5","valid_20","toast_chapter"};captureName=captureModes[0];renderMode="valid";}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/world-only").exists()){captureModes=new String[]{"world_check-empty","world_check-full","world_energy-empty","world_energy-half","world_energy-full","world_item","world_input","worldmap_normal","worldmap_full","worldmap_icons","worldmap_empty"};if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/world-filter.txt").exists())captureModes=java.nio.file.Files.readAllLines(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/world-filter.txt").toPath(),StandardCharsets.UTF_8).toArray(new String[0]);captureName=captureModes[0];renderMode=captureName.split("_")[0];}
        if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/forms-only").exists()){captureModes=new String[]{"forms_quest","forms_chapter","forms_task-checkmark","forms_task-item","forms_task-fluid","forms_task-energy","forms_task-dimension","forms_task-biome","forms_task-location","forms_task-kill","forms_task-xp","forms_task-stat","forms_task-observation","forms_task-interaction","forms_task-achievement","forms_task-custom","forms_reward-item","forms_reward-choice","forms_reward-random","forms_reward-loot","forms_reward-command","forms_reward-custom","forms_reward-xp","forms_reward-xp_levels","forms_reward-achievement","forms_reward-toast"};if(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/forms-filter.txt").exists())captureModes=java.nio.file.Files.readAllLines(new File(Minecraft.getMinecraft().mcDataDir,"../build/preview/forms-filter.txt").toPath(),StandardCharsets.UTF_8).stream().map(name->"forms_"+name).toArray(String[]::new);captureName=captureModes[0];renderMode="forms";}
        if(renderMode.isEmpty())GuideSmoke.register();
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
        if(uiItemRequested) { uiItemRequested=false; net.minecraft.entity.player.EntityPlayerMP actor=(net.minecraft.entity.player.EntityPlayerMP)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(0); actor.inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(net.minecraft.init.Items.stick)); actor.inventory.markDirty(); actor.inventoryContainer.detectAndSendChanges(); }
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
        deviceX=x; deviceY=y; deviceZ=z; deviceDelay=0;
    }
    private void finishCapture(Minecraft mc){
        if(captureModes==null || ++captureIndex>=captureModes.length){mc.shutdown();return;}
        captureName=captureModes[captureIndex];renderMode=captureName.split("_")[0];ticks=29;fixture=new QuestNetwork.Snapshot();fixture.book=new QuestCore.Book();fixture.canEdit=true;mc.displayGuiScreen(null);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft(); mc.guiAchievement.func_146257_b(); mc.gameSettings.showInventoryAchievementHint=false; mc.gameSettings.pauseOnLostFocus=false; mc.gameSettings.renderDistanceChunks=2; mc.gameSettings.fancyGraphics=false; mc.gameSettings.enableVsync=false; mc.gameSettings.limitFramerate=30;
        if(testing) { smoke(mc); return; }
        ClientProxy.snapshot=fixture; ticks++;
        if(renderMode.equals("worldmap")){
            mc.gameSettings.guiScale=2;String mode=captureName.substring(9),name="world-map"+(mode.equals("normal")?"":"-"+mode);
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="chapter";chapter.title="World chapter";fixture.book.chapters.add(chapter);for(int i=0;i<(mode.equals("empty")?0:3);i++){QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest"+i;quest.title="Quest "+i;quest.chapter=chapter.id;quest.x=i*2;quest.icon="minecraft:"+new String[]{"apple","bread","book"}[i];fixture.book.quests.add(quest);}QuestNetwork.TeamEntry team=new QuestNetwork.TeamEntry();team.owner="00000000-0000-0000-0000-000000000001";team.title="Reference";team.color=0xFFFFFF;fixture.teams.clear();fixture.teams.add(team);}
            if(ticks==65){QuestDevice tile=new QuestDevice();tile.quest="chapter:chapter";tile.owner=java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");tile.screenFullscreen=mode.equals("full");tile.screenHideIcons=!mode.equals("icons");tile.screenPercent=mode.equals("empty")?100:50;QuestWorldMap progress=new QuestWorldMap();for(int i=0;i<fixture.book.quests.size();i++)progress.values.put("quest"+i,i*50);mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glTranslated(80,50,0);java.lang.reflect.Method draw=Class.forName("dev.sigmarix.quests.client.QuestWorldMapRenderer").getDeclaredMethod("draw",QuestDevice.class,int.class,int.class,QuestWorldMap.class,boolean.class);draw.setAccessible(true);draw.invoke(null,tile,128,128,progress,true);org.lwjgl.opengl.GL11.glPopMatrix();ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("world")){
            mc.gameSettings.guiScale=2;String name=captureName.replace('_','-');
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";quest.title="World quest";QuestCore.Task task=new QuestCore.Task();task.id="task";task.type=name.contains("energy")?"energy":name.contains("item")?"item":"check";task.title=task.type.equals("energy")?"Energy":task.type.equals("item")?"Apples":"Confirm";if(task.type.equals("energy"))task.count=1000;if(task.type.equals("item"))task.target="minecraft:apple";quest.tasks.add(task);fixture.book.quests.add(quest);}
            if(ticks==65){QuestDevice tile=new QuestDevice(){@Override public net.minecraft.block.Block getBlockType(){return QuestBlocks.devices[0];}};tile.quest="quest";tile.task="task";tile.facing=3;tile.screenTaskProgress=name.endsWith("full")?(name.contains("energy")?1000:1):name.endsWith("half")?500:0;if(name.contains("input")){tile.screenInputOnly=true;tile.screenInputIcon=new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple);}mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glTranslated(80,50,0);org.lwjgl.opengl.GL11.glScaled(128,128,1);org.lwjgl.opengl.GL11.glTranslated(0,1,0);org.lwjgl.opengl.GL11.glScaled(1,-1,1);if(name.contains("input")){net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=mc.getTextureMapBlocks().getAtlasSprite("ftbquests:screen_front_input");mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);net.minecraft.client.renderer.texture.TextureUtil.uploadTextureMipmap(sprite.getFrameTextureData(0),sprite.getIconWidth(),sprite.getIconHeight(),sprite.getOriginX(),sprite.getOriginY(),false,false);}new QuestScreenRenderer().renderTileEntityAt(tile,0,0,0,0);org.lwjgl.opengl.GL11.glPopMatrix();ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("valid")){
            mc.gameSettings.guiScale=2;int count=Integer.parseInt(captureName.substring(6));
            if(ticks==30){QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";QuestCore.Task task=new QuestCore.Task();task.id="task";task.type="item";task.title="Apples";task.consume=true;String[] ids={"apple","bread","iron_ingot","gold_ingot","diamond","coal","stick","paper","book","arrow","bone","flint","leather","wheat","egg","string","feather","redstone","glowstone_dust","emerald"};for(int i=0;i<count;i++)task.targets.add("minecraft:"+ids[i]);quest.tasks.add(task);fixture.states.put(quest.id,"available");mc.displayGuiScreen(new QuestItemTaskScreen(null,quest,task));}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"valid-items-"+count+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("toast")){
            mc.gameSettings.guiScale=2;String name=captureName.replace('_','-');
            if(ticks==65){QuestNetwork.Toast toast=new QuestNetwork.Toast();toast.title="Native toast";toast.text=name.startsWith("toast-long")?"A longer reward description that wraps onto several lines.":"A reward";toast.icon="minecraft:apple";
                if(name.equals("toast-chapter")){toast.completion="chapter";toast.title="Getting started";toast.text="";}else if(name.equals("toast-task")){toast.task=new QuestCore.Task();toast.task.type="check";toast.task.title="Confirm";toast.task.icon="minecraft:apple";}else if(name.equals("toast-quest")){toast.quest=new QuestCore.Quest();toast.quest.title="Native quest";toast.quest.icon="minecraft:apple";toast.completion="quest";}else if(name.equals("toast-reward")){toast.reward=new QuestCore.Reward();toast.reward.type="xp";toast.reward.count=100;}
                mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);java.lang.reflect.Method draw=Class.forName("dev.sigmarix.quests.client.QuestToasts").getDeclaredMethod("drawToast",Minecraft.class,QuestNetwork.Toast.class,int.class,int.class,long.class);draw.setAccessible(true);draw.invoke(null,mc,toast,267,0,name.endsWith("subtitle")?2000L:1000L);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("pinned")){
            mc.gameSettings.guiScale=2;boolean complete=captureName.endsWith("complete");
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";quest.title="Pinned quest";QuestCore.Task check=new QuestCore.Task();check.id="check";check.type="check";check.title="Confirm";QuestCore.Task fluid=new QuestCore.Task();fluid.id="fluid";fluid.type="fluid";fluid.title="Water";fluid.count=20000;quest.tasks.add(check);quest.tasks.add(fluid);fixture.book.quests.add(quest);fixture.pinned.add(quest.id);fixture.states.put(quest.id,complete?"complete":"available");fixture.percentages.put(quest.id,25);fixture.counters.put("quest:fluid",10000L);}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();java.lang.reflect.Method draw=Class.forName("dev.sigmarix.quests.client.QuestPins").getDeclaredMethod("draw",Minecraft.class,QuestNetwork.Snapshot.class);draw.setAccessible(true);draw.invoke(null,mc,fixture);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),captureName.replace('_','-')+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("context")){
            mc.gameSettings.guiScale=2;
            if(ticks==30){fixture.book=new QuestCore.Book();Class<?> directory=Class.forName("dev.sigmarix.quests.client.QuestRewardTablesScreen");java.lang.reflect.Constructor<?> parentCtor=directory.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class);parentCtor.setAccessible(true);net.minecraft.client.gui.GuiScreen parent=(net.minecraft.client.gui.GuiScreen)parentCtor.newInstance((Object)null);mc.displayGuiScreen(parent);Class<?> entry=Class.forName("dev.sigmarix.quests.client.QuestContextScreen$Entry");java.lang.reflect.Constructor<?> entryCtor=entry.getDeclaredConstructor(String.class,String.class,boolean.class,Runnable.class);entryCtor.setAccessible(true);java.util.List<Object> entries=new java.util.ArrayList<Object>();entries.add(entryCtor.newInstance("Settings","lib:settings",true,(Runnable)()->{}));entries.add(entryCtor.newInstance("Disabled","remove",false,(Runnable)()->{}));entries.add(entryCtor.newInstance("","",true,null));entries.add(entryCtor.newInstance("Information","lib:info",true,null));Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestContextScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,int.class,int.class,java.util.List.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(parent,160,90,entries));}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"context-menu-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("devices")){
            mc.gameSettings.guiScale=2;String name=captureName.substring(8);
            if(ticks==30){int deviceMode=name.equals("task")?0:name.equals("progress")?1:name.equals("barrier")?2:name.equals("collector")?3:name.equals("redstone")?4:name.equals("player")?5:name.equals("chest")?7:9;QuestDevice tile=new QuestDevice(){@Override public net.minecraft.block.Block getBlockType(){return QuestBlocks.devices[deviceMode];}};Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestDeviceConfigScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestDevice.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,tile));}
            if(ticks==65){ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"device-"+name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("emergency") || renderMode.equals("notices")){
            mc.gameSettings.guiScale=2;String name=renderMode.equals("notices")?captureName.replace("notices_","notifications-"):captureName.replace('_','-');
            if(ticks==30){fixture.book=new QuestCore.Book();if(name.endsWith("filled")){QuestCore.Reward item=new QuestCore.Reward();item.type="item";item.item="minecraft:apple";fixture.book.emergencyItems.add(item);}Class<?> editor=Class.forName("dev.sigmarix.quests.client."+(renderMode.equals("notices")?"QuestNotificationsScreen":"QuestEmergencyScreen"));java.lang.reflect.Constructor<?> ctor=renderMode.equals("notices")?editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,boolean.class):editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)(renderMode.equals("notices")?ctor.newInstance(null,true):ctor.newInstance((Object)null)));if(renderMode.equals("notices") && name.endsWith("filled")){QuestNetwork.Toast toast=new QuestNetwork.Toast();toast.title="Apple";toast.reward=new QuestCore.Reward();toast.reward.type="item";toast.reward.item="minecraft:apple";toast.reward.count=1234;java.lang.reflect.Method receive=editor.getDeclaredMethod("rewardReceived",QuestNetwork.Toast.class);receive.setAccessible(true);receive.invoke(mc.currentScreen,toast);}}
            if(ticks==65){if(renderMode.equals("emergency")){java.lang.reflect.Field field=mc.currentScreen.getClass().getDeclaredField("end");field.setAccessible(true);field.setLong(mc.currentScreen,System.currentTimeMillis()+59000);}mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("aux")){
            mc.gameSettings.guiScale=2;String name=captureName.substring(4);
            if(ticks==30){fixture.book=new QuestCore.Book();java.util.List<QuestCore.Reward> rewards=new java.util.ArrayList<QuestCore.Reward>();QuestCore.LootSettings settings=new QuestCore.LootSettings();if(name.endsWith("filled")){settings.name="Native rewards";settings.emptyWeight=1;QuestCore.Reward reward=new QuestCore.Reward();reward.id="reward";reward.type="xp";reward.count=100;reward.weight=3;rewards.add(reward);}if(!name.equals("tables-empty")){fixture.book.lootTables.put("table",rewards);fixture.book.lootSettings.put("table",settings);}
                if(name.startsWith("choice")){QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";QuestCore.Reward choice=new QuestCore.Reward();choice.id="choice";choice.type="choice";choice.options=rewards;quest.rewards.add(choice);Class<?> editor=Class.forName("dev.sigmarix.quests.client.RewardChoiceScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestCore.Quest.class,QuestCore.Reward.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,quest,choice));}else {Class<?> editor=Class.forName("dev.sigmarix.quests.client."+(name.startsWith("tables")?"QuestRewardTablesScreen":"QuestRewardTableEditScreen"));if(name.startsWith("tables")){java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance((Object)null));}else{java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestCore.Book.class,java.util.List.class,String.class,Runnable.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,fixture.book,rewards,"table",(Runnable)()->{}));}}}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("values")){
            mc.gameSettings.guiScale=2;String name=captureName.substring(7);
            if(ticks==30){String value="";int color=0x999999;java.util.function.Predicate<String> valid=text->true;
                if(name.equals("nbt"))value="null";
                else if(name.equals("nbt-data"))value=QuestSnbt.write(QuestSnbt.parse("{name:\"Native\",numbers:[I;1,2]}"));
                else if(name.equals("stack"))value=QuestSnbt.write(QuestStackText.write(new net.minecraft.item.ItemStack(net.minecraft.init.Items.apple)));
                else if(name.equals("stack-empty"))value=QuestSnbt.write(QuestStackText.write(null));
                else if(name.equals("stack-text"))value="minecraft:apple 3 0 {display:{Name:\"Native\"}}";
                else if(name.equals("count") || name.equals("invalid-count")){value=name.equals("count")?"1":"0";color=0xAA5AE8;valid=text->{try{int n=Integer.parseInt(text);return n>=1 && n<=64;}catch(Exception invalid){return false;}};}
                else if(name.equals("meta")){value="0";color=0xAA5AE8;}
                else if(name.equals("name"))color=0xFFAA49;
                Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestValueScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,String.class,int.class,int.class,java.util.function.Predicate.class,java.util.function.Consumer.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,value,color,32768,valid,(java.util.function.Consumer<String>)text->{}));}
            if(ticks==65){java.lang.reflect.Field field=mc.currentScreen.getClass().getDeclaredField("input");field.setAccessible(true);((net.minecraft.client.gui.GuiTextField)field.get(mc.currentScreen)).setFocused(false);mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"value-"+name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("frame")){
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="chapter";chapter.title="Getting started";fixture.book.chapters.add(chapter);QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";quest.chapter=chapter.id;quest.title="Classic quest view";quest.subtitle="Two columns, individual rewards";quest.description="Click the task, then claim each reward separately.";fixture.book.quests.add(quest);Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestTextEditorFrame");java.lang.reflect.Method open=editor.getDeclaredMethod("open",QuestCore.Quest.class);open.setAccessible(true);open.invoke(null,quest);}
            if(ticks==65)javax.swing.SwingUtilities.invokeAndWait(()->{try{for(java.awt.Frame frame:java.awt.Frame.getFrames())if(frame.isVisible() && frame.getClass().getName().equals("dev.sigmarix.quests.client.QuestTextEditorFrame")){for(String name:new String[]{"title","subtitle","description"}){java.lang.reflect.Field field=frame.getClass().getDeclaredField(name);field.setAccessible(true);((javax.swing.text.JTextComponent)field.get(frame)).getCaret().setVisible(false);}java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D graphics=image.createGraphics();frame.paint(graphics);graphics.dispose();javax.imageio.ImageIO.write(image,"png",new File(mc.mcDataDir,"../build/preview/desktop-text-editor.png"));frame.dispose();}}catch(Exception error){throw new RuntimeException(error);}});if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("forms")){
            mc.gameSettings.guiScale=2;String name=captureName.substring(6);
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="chapter";chapter.title="";fixture.book.chapters.add(chapter);QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest";quest.title="";quest.chapter=chapter.id;fixture.book.quests.add(quest);Object object;String scope;
                if(name.equals("quest")){object=quest;scope="quest";}else if(name.equals("chapter")){object=chapter;scope="chapter";}else if(name.startsWith("task-")){String type=name.substring(5).replace("checkmark","check");Class<?> menus=Class.forName("dev.sigmarix.quests.client.QuestCreationMenus");java.lang.reflect.Method task=menus.getDeclaredMethod("task",String.class);task.setAccessible(true);object=task.invoke(null,type);quest.tasks.add((QuestCore.Task)object);scope="task";}else{QuestCore.Reward reward=new QuestCore.Reward();reward.id="reward";reward.type=name.substring(7);reward.target=reward.type.equals("command")?"/say Hi, @team!":"";if(reward.type.equals("item"))reward.item="minecraft:apple";if(reward.type.equals("xp"))reward.count=100;quest.rewards.add(reward);object=reward;scope="reward";}
                Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestObjectConfigScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestCore.Book.class,Object.class,String.class,Runnable.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,fixture.book,object,scope,(Runnable)()->{}));}
            if(ticks==65)ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),name+"-config.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("table")){
            mc.gameSettings.guiScale=2;if(ticks==30){fixture.book=new QuestCore.Book();if((captureName.equals("table_crate") || new File(mc.mcDataDir,"../build/preview/table-crate").exists())){QuestCore.LootSettings settings=new QuestCore.LootSettings();settings.alias="common";fixture.book.lootSettings.put("table",settings);}Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestRewardTableSettingsScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestCore.Book.class,String.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,fixture.book,"table"));}
            if(ticks==65)ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),(captureName.equals("table_crate")?"table-crate-config-controlled.png":"table-config-controlled.png"),mc.displayWidth,mc.displayHeight,mc.getFramebuffer());if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("map")){
            mc.gameSettings.guiScale=2;if(ticks==30){fixture.book=new QuestCore.Book();fixture.book.title="Quest map fixture";fixture.canEdit=false;fixture.states.clear();for(int c=0;c<2;c++){QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="c"+c;chapter.title=c==0?"Getting started":"More quests";chapter.icon=c==0?"minecraft:book":"minecraft:paper";if(c==1 && new File(mc.mcDataDir,"../build/preview/map-group").exists())chapter.parent="c0";fixture.book.chapters.add(chapter);for(int i=0;i<(c==0?4:1);i++){int index=c==0?i:4;QuestCore.Quest q=new QuestCore.Quest();q.id=String.format(java.util.Locale.ROOT,"%08x",0x20000000+index);q.title=new String[]{"First quest","Second quest","Third quest","Extra quest","Last quest"}[index];q.chapter=chapter.id;q.x=index==0 || index==4?0:index==2?4:2;q.y=index==3?2:0;q.optional=index==3;if(index==1 || index==3)q.dependencies.add("20000000");if(index==2)q.dependencies.add("20000001");QuestCore.Task task=new QuestCore.Task();task.id="t"+index;task.type="check";task.manual=true;q.tasks.add(task);QuestCore.Reward reward=new QuestCore.Reward();reward.id="r"+index;reward.type="xp";reward.count=100;q.rewards.add(reward);fixture.book.quests.add(q);fixture.states.put(q.id,index==0 || index==4?"available":"locked");}}fixture.book.validate();mc.displayGuiScreen(new QuestBookScreen());}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();boolean group=new File(mc.mcDataDir,"../build/preview/map-group").exists();boolean hover=group || captureName.equals("map_hover") || new File(mc.mcDataDir,"../build/preview/map-hover").exists();mc.currentScreen.drawScreen(hover?8:-1,hover?8:-1,0);ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),group?"chapter-group-controlled.png":hover?"chapter-hover-controlled.png":"main-map-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("image") || renderMode.equals("file")){
            mc.gameSettings.guiScale=2;if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Chapter chapter=new QuestCore.Chapter();chapter.id="map";chapter.title="Map";fixture.book.chapters.add(chapter);QuestPresentation.MapImage image=new QuestPresentation.MapImage();chapter.images.add(image);Class<?> editor=Class.forName("dev.sigmarix.quests.client.QuestObjectConfigScreen");java.lang.reflect.Constructor<?> ctor=editor.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,QuestCore.Book.class,Object.class,String.class,Runnable.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,fixture.book,renderMode.equals("file")?fixture.book:image,renderMode.equals("file")?"file":"chapter.image",(Runnable)()->{}));}
            if(ticks==65)ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),renderMode+"-config-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("fluid")){
            mc.gameSettings.guiScale=2;if(ticks==30){Class<?> picker=Class.forName("dev.sigmarix.quests.client.QuestFluidSelectScreen");java.lang.reflect.Constructor<?> ctor=picker.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,boolean.class,java.util.function.Consumer.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,false,(java.util.function.Consumer<net.minecraftforge.fluids.Fluid>)v->{}));}
            if(ticks==65)ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"fluid-picker-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("picker")){
            mc.gameSettings.guiScale=2;if(ticks==30){Class<?> picker=Class.forName("dev.sigmarix.quests.client.QuestItemSelectScreen");java.lang.reflect.Constructor<?> ctor=picker.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class,net.minecraft.item.ItemStack.class,boolean.class,java.util.function.Predicate.class,java.util.function.Consumer.class);ctor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)ctor.newInstance(null,null,true,(java.util.function.Predicate<net.minecraft.item.ItemStack>)v->true,(java.util.function.Consumer<net.minecraft.item.ItemStack>)v->{}));}
            if(ticks==65)ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"item-picker-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("card")) {
            mc.gameSettings.guiScale=2;
            if(ticks==30){fixture.book=new QuestCore.Book();QuestCore.Quest quest=new QuestCore.Quest();quest.id="card";quest.title="Classic quest view";quest.subtitle="Two columns, individual rewards";quest.description="Click the task, then claim each reward separately.";for(int i=0;i<2;i++){QuestCore.Task task=new QuestCore.Task();task.id="t"+i;task.type="check";quest.tasks.add(task);QuestCore.Reward reward=new QuestCore.Reward();reward.id="r"+i;reward.type="xp";reward.count=100;quest.rewards.add(reward);}fixture.book.quests.add(quest);fixture.book.validate();fixture.states.put("card","locked");QuestBookScreen screen=new QuestBookScreen();mc.displayGuiScreen(screen);java.lang.reflect.Field selected=QuestBookScreen.class.getDeclaredField("selected");selected.setAccessible(true);selected.set(screen,"card");screen.refresh();}
            if(ticks==65){mc.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(-1,-1,0);File destination=new File(mc.mcDataDir,"../build/preview");destination.mkdirs();ScreenShotHelper.saveScreenshot(destination,"quest-card-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());}
            if(ticks==75)finishCapture(mc);return;
        }
        if(renderMode.equals("catalog")) {
            mc.gameSettings.guiScale=2;
            if(ticks==30) mc.displayGuiScreen(new QuestVisualCatalog());
            if(ticks==65) { File destination=new File(mc.mcDataDir,"../build/preview"); destination.mkdirs(); ScreenShotHelper.saveScreenshot(destination,"item-catalog-controlled.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); }
            if(ticks==75) finishCapture(mc);
            return;
        }
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
        Class<?> owner=screen.getClass();java.lang.reflect.Method method=null;while(owner!=null){try{method=owner.getDeclaredMethod("actionPerformed",net.minecraft.client.gui.GuiButton.class);break;}catch(NoSuchMethodException missing){owner=owner.getSuperclass();}}if(method==null)throw new NoSuchMethodException("actionPerformed");method.setAccessible(true);
        method.invoke(screen,new net.minecraft.client.gui.GuiButton(id,0,0,"fixture"));
    }
    private void clickCard(Minecraft mc,boolean reward,int index) throws Exception {
        java.lang.reflect.Field field=QuestBookScreen.class.getDeclaredField("card"); field.setAccessible(true); QuestViewLayout card=(QuestViewLayout)field.get(mc.currentScreen);
        java.lang.reflect.Method click=QuestBookScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class); click.setAccessible(true);
        click.invoke(mc.currentScreen,card.cellX(reward,index,2)+8,card.cellY(reward,index)+8,0);
    }
    private void action(String kind) {
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action=kind; action.id="welcome"; ClientProxy.send(action);
    }
    private int bread(Minecraft mc) {
        int count=0; for(net.minecraft.item.ItemStack stack:mc.thePlayer.inventory.mainInventory) if(stack!=null && stack.getItem()==net.minecraft.init.Items.bread) count+=stack.stackSize;
        return count;
    }
    private void smoke(Minecraft mc) throws Exception {
        if(waiting==20 && !restarted){for(String event:new String[]{"in","out","challenge_complete"}){net.minecraft.client.audio.SoundEventAccessorComposite sound=mc.getSoundHandler().getSound(new net.minecraft.util.ResourceLocation("sigmarixquests:toast."+event));if(sound==null)throw new IllegalStateException("Missing native toast sound: "+event);net.minecraft.client.audio.SoundPoolEntry entry=sound.func_148720_g();try(java.io.InputStream input=mc.getResourceManager().getResource(entry.getSoundPoolEntryLocation()).getInputStream()){if(input.read()!='O' || input.read()!='g' || input.read()!='g' || input.read()!='S')throw new IllegalStateException("Invalid native toast OGG: "+event);}}System.out.println("Quest native toast sounds PASS: three registered OGG assets");}

        if(++waiting%100==0) System.out.println("Quest smoke stage="+stage+" ticks="+waiting+" snapshot="+(ClientProxy.snapshot==null?"none":ClientProxy.snapshot.message)+" gui="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName())+" player="+(mc.thePlayer!=null));
        if(waiting>2400) throw new IllegalStateException("Quest smoke timeout at stage "+stage);
        if(stage==0 && !restarted && mc.thePlayer==null && mc.theWorld==null && mc.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu && waiting>=200) {
            java.lang.reflect.Field connection=Minecraft.class.getDeclaredField("myNetworkManager"); connection.setAccessible(true); net.minecraft.network.NetworkManager manager=(net.minecraft.network.NetworkManager)connection.get(mc);
            System.out.println("Quest preview initial login retry: manager="+(manager==null?"null":"open="+manager.isChannelOpen()+", handler="+manager.getNetHandler().getClass().getSimpleName()));
            if(loginRetries++>=1) throw new IllegalStateException("Initial integrated-server login failed twice");
            waiting=0; mc.launchIntegratedServer(worldName,"Quest smoke fixture",null); return;
        }
        if(mc.thePlayer==null || mc.theWorld==null || waiting%5!=0) return;
        java.lang.reflect.Field serverClock=QuestsMod.class.getDeclaredField("ticks"); serverClock.setAccessible(true); int serverNow=serverClock.getInt(QuestsMod.instance); if(serverNow<lastServerStep) lastServerStep=-5;
        if(serverNow-lastServerStep<5) return;
        long realNow=System.nanoTime(); if(realNow-lastSmokeStep<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(200)) return; lastSmokeStep=realNow; lastServerStep=serverNow;
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
                if(!"fixture_team".equals(snap.team) || waiting<15 || !(mc.currentScreen instanceof QuestTeamScreen))return;
                if(snap.teamColor!=0xB342FF)throw new IllegalStateException("Team palette color did not synchronize");QuestToasts.clear();mc.ingameGUI.getChatGUI().clearChatMessages();
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-teams.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,30);stage=103;waiting=0;break;
            case 28:
                if(waiting<15)return;ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-leaderboard.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Method back=net.minecraft.client.gui.GuiScreen.class.getDeclaredMethod("keyTyped",char.class,int.class);back.setAccessible(true);back.invoke(mc.currentScreen,(char)0,org.lwjgl.input.Keyboard.KEY_ESCAPE);press(mc.currentScreen,14);press(mc.currentScreen,0);stage=29;break;
            case 29: if(snap.team.isEmpty()) { teamPassed=true;QuestToasts.clear();mc.ingameGUI.getChatGUI().clearChatMessages();mc.displayGuiScreen(new QuestVisualCatalog());stage=101;waiting=0; } break;
            case 102: if(waiting<15)return;ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-create-team.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());press(mc.currentScreen,20);stage=27;waiting=0;break;
            case 103:if(waiting<15)return;ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-team-settings.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());press(mc.currentScreen,1);Class<?> board=Class.forName("dev.sigmarix.quests.client.QuestLeaderboardScreen");java.lang.reflect.Constructor<?> constructor=board.getDeclaredConstructor(net.minecraft.client.gui.GuiScreen.class);constructor.setAccessible(true);mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen)constructor.newInstance(mc.currentScreen));stage=28;waiting=0;break;
            case 101: if(waiting<15)return;ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-reference-items.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());mc.displayGuiScreen(null);stage=1;break;
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
                    java.nio.file.Files.write(new File(mc.mcDataDir,"../build/preview/smoke.txt").toPath(),"PASS: real client/server handshake, completion, one-time claim, inventory sync, editing, stale revision rejection, world restart persistence, manual task confirmation, start action, task dependency sync, graphical toast, guide rendering, external guide events and registry, pinned HUD and restart persistence, notification history, custom click callback, confirmed file action, permission-checked network command, object form save, native desktop text editor save, native device container, checked device binding, linked 2x2 world screen and synchronized progress, world chapter map, fullscreen/hideIcons, input-only held icon, configured public detectors, scripted task button, conditional shop hook, offline collectors and persistent crate-opener roll".concat(iconPassed?", custom icon editor, asynchronous HTTP textures and synchronized texture/name/tint":"").concat(euPassed?", real IC2 EnergyNet transfer":"").concat(manaPassed?", real Botania mana burst":"").concat(teamPassed?", team GUI, leaderboard and server-checked disband":"").concat(bcPassed?", real BuildCraft engine and RF/MJ conversion":"").concat(presentationPassed?", nested chapter groups, inherited shapes, map illustration link, empty quest and chapter-bound screen":"").concat(neiPassed?", real NEI recipes, uses, loot and GUI":"").concat(nativePassed?", native JavaScript, stages, skills, pack mode isolation and restart":"").concat(npcPassed?", real CustomNPCs tasks/rewards and restart persistence":"").concat(emcPassed?", real ProjectE wallet submission, sync and persistence":"").getBytes(StandardCharsets.UTF_8));
                    PreviewImageServer.stop(); mc.shutdown(); return;
                }
                if(!teamPassed) {
                    QuestToasts.clear();mc.ingameGUI.getChatGUI().clearChatMessages();
                    QuestTeamScreen teams=new QuestTeamScreen(new QuestBookScreen()); mc.displayGuiScreen(teams);
                    press(teams,10);
                    net.minecraft.client.gui.GuiScreen create=mc.currentScreen;java.lang.reflect.Field input=create.getClass().getDeclaredField("input");input.setAccessible(true);((net.minecraft.client.gui.GuiTextField)input.get(create)).setText("fixture_team");
                    java.lang.reflect.Method palette=create.getClass().getDeclaredMethod("mouseClicked",int.class,int.class,int.class);palette.setAccessible(true);palette.invoke(create,(create.width-162)/2+110,(create.height-118)/2+74,0);
                    stage=102;waiting=0;return;
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
                edit.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(original),QuestCore.Book.class); edit.book.title="Network edit smoke"; edit.book.theme.questSpacing=1.25; edit.book.theme.lineWidth=2;
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
                QuestCore.Quest ui=new QuestCore.Quest(); ui.id="smoke-classic-ui"; ui.title="Classic quest view"; ui.subtitle="Two columns, individual rewards"; ui.description="Click the task, then claim each reward separately."; ui.optional=true; ui.repeatable=true; ui.chapter="start"; ui.x=8; ui.y=5;
                QuestCore.Task uiTask=new QuestCore.Task(); uiTask.id="confirm"; uiTask.type="check"; uiTask.manual=true; ui.tasks.add(uiTask); QuestCore.Task uiItems=new QuestCore.Task(); uiItems.id="submit"; uiItems.type="item"; uiItems.target="minecraft:stick"; uiItems.consume=true; ui.tasks.add(uiItems);
                QuestCore.Reward uiXp=new QuestCore.Reward(); uiXp.id="xp"; uiXp.type="xp"; uiXp.count=2; ui.rewards.add(uiXp);
                QuestCore.Reward uiChoice=new QuestCore.Reward(); uiChoice.id="pick"; uiChoice.type="choice"; QuestCore.Reward uiOption=new QuestCore.Reward(); uiOption.type="xp"; uiOption.count=7; uiChoice.options.add(uiOption); ui.rewards.add(uiChoice); edit.book.quests.add(ui);
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
                if(!textFramePassed){if(snap==null)return;Class<?> frame=Class.forName("dev.sigmarix.quests.client.QuestTextEditorFrame");java.lang.reflect.Method open=frame.getDeclaredMethod("open",QuestCore.Quest.class);open.setAccessible(true);QuestCore.Quest welcome=snap.book.quests.stream().filter(q->q.id.equals("welcome")).findFirst().get();open.invoke(null,welcome);stage=105;waiting=0;return;}
                if(!presentationPassed) {
                    if(snap==null) return;
                    QuestToasts.clear(); QuestBookScreen map=new QuestBookScreen(); mc.displayGuiScreen(map); press(map,11); press(map,2);
                    java.lang.reflect.Field chosen=QuestBookScreen.class.getDeclaredField("selected"); chosen.setAccessible(true); chosen.set(map,"smoke-empty"); map.refresh(); press(map,1); stage=36; waiting=0; return;
                }
                if(!classicUiPassed) {
                    QuestToasts.clear(); uiItemRequested=true; QuestBookScreen uiBook=new QuestBookScreen(); mc.displayGuiScreen(uiBook); java.lang.reflect.Field selectedUi=QuestBookScreen.class.getDeclaredField("selected"); selectedUi.setAccessible(true); selectedUi.set(uiBook,"smoke-classic-ui"); uiBook.refresh(); stage=90; waiting=0; return;
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
                if(snap==null || !snap.message.startsWith("Error: Tasks incomplete")) { if(waiting>=40 && waiting%20==0) { QuestNetwork.Action retry=new QuestNetwork.Action(); retry.action="check"; retry.id="smoke-manual"; ClientProxy.send(retry); } return; }
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
                if(snap==null) return;
                if(!"claimed".equals(snap.states.get("smoke-manual"))) { if("complete".equals(snap.states.get("smoke-manual")) && waiting>=40 && waiting%20==0) { QuestNetwork.Action retryClaim=new QuestNetwork.Action(); retryClaim.action="claim"; retryClaim.id="smoke-manual"; ClientProxy.send(retryClaim); } return; }
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
                QuestImages.texture(remoteImage);if(!QuestImages.loaded(remoteImage)) return;
                net.minecraft.util.ResourceLocation cached=QuestImages.texture(remoteImage);
                int textureId=mc.getTextureManager().getTexture(cached).getGlTextureId(); QuestImages.clear();
                if(mc.getTextureManager().getTexture(cached)!=net.minecraft.client.renderer.texture.TextureUtil.missingTexture || org.lwjgl.opengl.GL11.glIsTexture(textureId)) throw new IllegalStateException("Remote texture cache did not release CPU/GPU storage");
                iconPassed=true; stage=1; break;
            case 38: if(QuestPresentation.chapter(snap.book,"start")!=null && snap.book.quests.stream().anyMatch(q->q.id.equals("welcome") && q.chapter.equals("start"))) { bookPrepared=true; stage=1; } break;
            case 36:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-empty-card.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,16); stage=95; waiting=0; break;
            case 95:
                if(waiting<10) return;
                if(snap.book.theme.questSpacing!=1.25 || snap.book.theme.lineWidth!=2) throw new IllegalStateException("Theme geometry lost in network snapshot");
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
                Class<?> nativeEditor=Class.forName("dev.sigmarix.quests.client.QuestObjectConfigScreen"); java.lang.reflect.Method openEditor=nativeEditor.getDeclaredMethod("edit",net.minecraft.client.gui.GuiScreen.class,String.class,String.class,String.class); openEditor.setAccessible(true); openEditor.invoke(null,null,"","",""); stage=16; waiting=0; break;
            case 16:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-object-editor.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                net.minecraft.client.gui.GuiScreen form=mc.currentScreen;
                java.lang.reflect.Field draftField=form.getClass().getDeclaredField("draft"); draftField.setAccessible(true); ((QuestCore.Book)draftField.get(form)).title="Form edit smoke";
                press(form,0); stage=17; break;
            case 17:
                if(snap==null || !snap.book.title.equals("Form edit smoke")) return;
                QuestNetwork.Action restore=new QuestNetwork.Action(); restore.action="save"; restore.revision=snap.revision; restore.book=original;
                pendingBookEdit=restore; ClientProxy.send(restore); lastBookAttempt=realNow; stage=7; waiting=0; break;
            case 7:
                if(snap==null) return;
                if(!snap.book.title.equals(original.title)) { if(realNow-lastBookAttempt>java.util.concurrent.TimeUnit.SECONDS.toNanos(1)) { System.out.println("Quest restore retry: current="+snap.book.title+", expected="+original.title+", message="+snap.message); pendingBookEdit.revision=snap.revision; ClientProxy.send(pendingBookEdit); lastBookAttempt=realNow; } return; }
                QuestToasts.clear(); mc.displayGuiScreen(null); mc.thePlayer.rotationYaw=-90; mc.thePlayer.rotationPitch=30; placeDevice=true; stage=32; waiting=0; break;
            case 32:
                if(waiting<30) return;
                if(mc.currentScreen!=null) throw new IllegalStateException("Device container opened before world screen preview");
                net.minecraft.tileentity.TileEntity rendered=mc.theWorld.getTileEntity(deviceX,deviceY,deviceZ);
                if(!(rendered instanceof QuestDevice) || ((QuestDevice)rendered).screenWidth!=2 || ((QuestDevice)rendered).screenHeight!=2 || ((QuestDevice)rendered).screenPercent!=100) throw new IllegalStateException("World screen dimensions/progress did not synchronize");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-world-screen.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer()); openProgressRequested=true; stage=18; break;
            case 18:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || snap==null) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-device.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                java.lang.reflect.Field configField=QuestDeviceScreen.class.getDeclaredField("properties");configField.setAccessible(true);net.minecraft.client.gui.GuiScreen config=(net.minecraft.client.gui.GuiScreen)configField.get(mc.currentScreen);
                java.lang.reflect.Field deviceDraft=config.getClass().getDeclaredField("draft");deviceDraft.setAccessible(true);((QuestDeviceConfig)deviceDraft.get(config)).quest="chapter:start";
                press(config,0);if(mc.currentScreen!=null)throw new IllegalStateException("Native device accept did not close the configuration");stage=104;waiting=0;break;
            case 104:
                if(snap==null || !snap.message.equals("Device settings saved"))return;
                openProgressRequested=true;stage=19;waiting=0;break;
            case 19:
                if(!(mc.currentScreen instanceof QuestDeviceScreen) || waiting<20 || snap==null)return;
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
            case 90:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-task-card.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                net.minecraft.client.gui.GuiScreen activeUi=mc.currentScreen; QuestBookScreen searchProbe=new QuestBookScreen(); mc.displayGuiScreen(searchProbe); press(searchProbe,22);
                java.lang.reflect.Field filterField=QuestBookScreen.class.getDeclaredField("search"); filterField.setAccessible(true); net.minecraft.client.gui.GuiTextField filter=(net.minecraft.client.gui.GuiTextField)filterField.get(searchProbe); filter.setText("no-match");
                java.lang.reflect.Method escape=QuestBookScreen.class.getDeclaredMethod("keyTyped",char.class,int.class); escape.setAccessible(true); escape.invoke(searchProbe,(char)0,org.lwjgl.input.Keyboard.KEY_ESCAPE);
                java.lang.reflect.Field searchVisible=QuestBookScreen.class.getDeclaredField("showSearch"); searchVisible.setAccessible(true); if(!filter.getText().isEmpty() || searchVisible.getBoolean(searchProbe)) throw new IllegalStateException("Closing search left a hidden map filter");
                mc.displayGuiScreen(activeUi); mc.gameSettings.guiScale=1; mc.displayGuiScreen(mc.currentScreen); stage=98; waiting=0; break;
            case 98:
                if(waiting<15) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-scale-1.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                mc.gameSettings.guiScale=2; mc.displayGuiScreen(mc.currentScreen); stage=99; waiting=0; break;
            case 99:
                if(waiting<15) return;
                clickCard(mc,false,0); stage=91; waiting=0; break;
            case 91:
                if(waiting<10 || !Boolean.TRUE.equals(snap.taskStates.get("smoke-classic-ui:confirm"))) return;
                if("complete".equals(snap.states.get("smoke-classic-ui"))) throw new IllegalStateException("Task confirmation consumed another task's inventory");
                QuestToasts.clear(); clickCard(mc,false,1); stage=96; waiting=0; break;
            case 96:
                if(waiting<15 || !(mc.currentScreen instanceof QuestItemTaskScreen)) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-items.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,1); stage=97; waiting=0; break;
            case 97:
                if(waiting<10 || !"complete".equals(snap.states.get("smoke-classic-ui"))) return;
                QuestToasts.clear(); clickCard(mc,true,0); stage=92; waiting=0; break;
            case 92:
                if(waiting<10 || !Boolean.TRUE.equals(snap.rewardStates.get("smoke-classic-ui:xp"))) return;
                if(Boolean.TRUE.equals(snap.rewardStates.get("smoke-classic-ui:pick"))) throw new IllegalStateException("Reward click claimed another root");
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-reward-card.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                clickCard(mc,true,1); stage=93; waiting=0; break;
            case 93:
                if(waiting<15 || !(mc.currentScreen instanceof RewardChoiceScreen)) return;
                ScreenShotHelper.saveScreenshot(new File(mc.mcDataDir,"../build/preview"),"quest-classic-choice.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
                press(mc.currentScreen,100); stage=94; waiting=0; break;
            case 94:
                if(waiting<10 || !"claimed".equals(snap.states.get("smoke-classic-ui"))) return;
                press(mc.currentScreen,6); stage=100; waiting=0; break;
            case 100:
                if(waiting<10 || !"available".equals(snap.states.get("smoke-classic-ui"))) return;
                if(Boolean.TRUE.equals(snap.taskStates.get("smoke-classic-ui:confirm"))) throw new IllegalStateException("Manual repeat retained task completion");
                classicUiPassed=true; mc.displayGuiScreen(null); stage=6; waiting=0; break;
            case 105:
                java.awt.Frame textFrame=null;for(java.awt.Frame frame:java.awt.Frame.getFrames())if(frame.isVisible() && frame.getClass().getName().equals("dev.sigmarix.quests.client.QuestTextEditorFrame"))textFrame=frame;if(textFrame==null)return;
                final java.awt.Frame editorFrame=textFrame;javax.swing.SwingUtilities.invokeAndWait(()->{try{java.lang.reflect.Field titleField=editorFrame.getClass().getDeclaredField("title");titleField.setAccessible(true);((javax.swing.JTextField)titleField.get(editorFrame)).setText("Desktop text smoke");javax.swing.JPanel panel=(javax.swing.JPanel)((javax.swing.JFrame)editorFrame).getContentPane();javax.swing.JPanel buttons=(javax.swing.JPanel)panel.getComponent(panel.getComponentCount()-1);((javax.swing.JButton)buttons.getComponent(1)).doClick();}catch(Exception error){throw new RuntimeException(error);}});stage=106;waiting=0;break;
            case 106:
                if(snap==null || snap.book.quests.stream().noneMatch(q->q.id.equals("welcome") && q.title.equals("Desktop text smoke")))return;
                javax.swing.SwingUtilities.invokeAndWait(()->{for(java.awt.Frame frame:java.awt.Frame.getFrames())if(frame.getClass().getName().equals("dev.sigmarix.quests.client.QuestTextEditorFrame"))frame.dispose();});textFramePassed=true;stage=6;waiting=0;break;
            default: throw new IllegalStateException("Unknown smoke stage");
        }

    }
}