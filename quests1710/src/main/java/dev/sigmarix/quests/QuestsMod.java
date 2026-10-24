package dev.sigmarix.quests;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.command.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mod(modid="sigmarixquests", name="Sigmarix Quests", version=BuildTags.VERSION, acceptableRemoteVersions="["+BuildTags.VERSION+"]")
public final class QuestsMod {
    @Mod.Instance("sigmarixquests") public static QuestsMod instance;
    @SidedProxy(clientSide="dev.sigmarix.quests.client.ClientProxy",serverSide="dev.sigmarix.quests.CommonProxy")
    public static CommonProxy proxy;
    public static QuestService service;
    private File config;
    private int ticks;
    private static final QuestMessageQueue<Pending> QUEUE=new QuestMessageQueue<Pending>(128,2L*QuestNetwork.MAX_BYTES,p->p.json.length());
    private static final Map<UUID,Long> LAST_REQUEST=new HashMap<UUID,Long>(),LAST_VIEW=new HashMap<UUID,Long>(),LAST_SCREEN=new HashMap<UUID,Long>();
    private static final Set<UUID> PENDING_VIEWS=new HashSet<UUID>();
    private static final Map<UUID,String> LAST_MESSAGE=new HashMap<UUID,String>();
    private static final class Pending {
        final UUID player; final String json;
        Pending(EntityPlayerMP player,String json) { this.player=player.getUniqueID(); this.json=json; }
    }
    public static void enqueue(EntityPlayerMP player,String json) { QUEUE.offer(new Pending(player,json)); }
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) throws IOException {
        config=new File(event.getModConfigurationDirectory(),"sigmarixquests/quests.json");
        Files.createDirectories(config.toPath().getParent());
        if(!config.exists()) try(InputStream in=getClass().getResourceAsStream("/example-quests.json")) { Files.copy(in,config.toPath()); }
        QuestNetwork.init();
        cpw.mods.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(instance,new QuestGuiHandler());
        GameRegistry.registerItem(new QuestBookItem(),"quest_book");
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        QuestBlocks.register();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); QuestBlocks.recipes(); }
    private final java.util.function.Consumer<QuestEvents.Event> taskToast=event -> {
        if(service==null || MinecraftServer.getServer()==null) return;
        QuestCore.Quest quest=service.quests.get(event.quest); if(quest==null || quest.disableToast) return;
        for(QuestCore.Task task:quest.tasks) if(task.id.equals(event.task) && !task.disableToast) {
            for(Object raw:MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayerMP player=(EntityPlayerMP)raw;
                if(service.progress().group(event.player).equals(service.progress().group(player.getUniqueID()))) QuestNetwork.taskToast(player,quest,task);
            }
            break;
        }
    };
    @Mod.EventHandler public void starting(FMLServerStartingEvent event) throws IOException {
        QUEUE.clear(); LAST_REQUEST.clear(); LAST_VIEW.clear(); LAST_SCREEN.clear(); PENDING_VIEWS.clear(); LAST_MESSAGE.clear(); ticks=0;
        QuestEvents.off("task_completed",taskToast); QuestEvents.on("task_completed",taskToast);
        QuestScripts.load(new File(config.getParentFile(),"scripts"));
        service=new QuestService(config); service.loadActiveProfile(); event.registerServerCommand(new QuestCommand());
    }
    @Mod.EventHandler public void stopped(FMLServerStoppedEvent event) { QuestEvents.off("task_completed",taskToast); service=null; QuestScripts.clear(); QUEUE.clear(); LAST_REQUEST.clear(); LAST_VIEW.clear(); LAST_SCREEN.clear(); PENDING_VIEWS.clear(); LAST_MESSAGE.clear(); }
    @SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void onDeath(LivingDeathEvent event) {
        if(service!=null && !event.entity.worldObj.isRemote && event.source.getEntity() instanceof EntityPlayerMP)
            service.credit((EntityPlayerMP)event.source.getEntity(),"kill",EntityList.getEntityString(event.entityLiving),1);
    }
    @SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void creatureLoot(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if(service==null || event.entityLiving.worldObj.isRemote || event.entityLiving instanceof net.minecraft.entity.player.EntityPlayer) return;
        String table=QuestLoot.choose(service.book,event.entityLiving,event.entityLiving.getRNG());
        if(table!=null) event.drops.add(new net.minecraft.entity.item.EntityItem(event.entityLiving.worldObj,event.entityLiving.posX,event.entityLiving.posY,event.entityLiving.posZ,QuestLoot.crate(service.book,table)));
    }
    @SubscribeEvent public void crafted(cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent event) {
        if(service!=null && event.player instanceof EntityPlayerMP) service.crafted((EntityPlayerMP)event.player,event.crafting);
    }
    @SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void interactEntity(net.minecraftforge.event.entity.player.EntityInteractEvent event) {
        if(service!=null && event.entityPlayer instanceof EntityPlayerMP && event.entityPlayer.getDistanceSqToEntity(event.target)<=64)
            service.interact((EntityPlayerMP)event.entityPlayer,event.target);
    }
    @SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void interact(PlayerInteractEvent event) {
        if(service!=null && !event.world.isRemote && event.entityPlayer instanceof EntityPlayerMP && event.action==PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            EntityPlayerMP player=(EntityPlayerMP)event.entityPlayer;
            // Distance checked on server; canceled interactions are not credited.
            if(player.getDistanceSq(event.x+.5,event.y+.5,event.z+.5)<=64)
                service.interact(player,event.world,event.x,event.y,event.z);
        }
    }
    @SubscribeEvent public void login(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) { if(service!=null) { service.clearObservations(event.player.getUniqueID()); PENDING_VIEWS.add(event.player.getUniqueID()); } }
    @SubscribeEvent public void logout(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) { UUID id=event.player.getUniqueID(); if(service!=null) service.clearObservations(id); LAST_REQUEST.remove(id); LAST_VIEW.remove(id); LAST_SCREEN.remove(id); PENDING_VIEWS.remove(id); LAST_MESSAGE.remove(id); }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || service==null) return;
        ticks++;
        for(int i=0;i<16;i++) {
            Pending pending=QUEUE.poll(); if(pending==null) break;
            EntityPlayerMP player=null;
            for(Object entry:MinecraftServer.getServer().getConfigurationManager().playerEntityList)
                if(((EntityPlayerMP)entry).getUniqueID().equals(pending.player)) { player=(EntityPlayerMP)entry; break; }
            if(player==null) continue;
            handle(player,pending.json);
        }
        for(Object entry:MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP viewer=(EntityPlayerMP)entry; UUID id=viewer.getUniqueID(); Long last=LAST_VIEW.get(id);
            if(PENDING_VIEWS.contains(id) && (last==null || ticks-last>=2)) { PENDING_VIEWS.remove(id); LAST_VIEW.put(id,(long)ticks); sendSnapshot(viewer,null); }
            try { service.observe((EntityPlayerMP)entry); } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest observation failed: %s",ex.toString()); }
        }
        if(ticks%20==0) for(Object entry:MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP p=(EntityPlayerMP)entry;
            try { service.detect(p); } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest detection failed: %s",ex.toString()); }
            for(QuestCore.Quest q:service.quests.values()) {
                if(service.progress().completed(p.getUniqueID()).contains(q.id)) { try { service.autoClaim(p,q); } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest auto-reward failed: %s",ex.toString()); } }
                if(q.autoComplete && service.active(p,q)) {
                try { if(service.finish(p,q)) { QuestService.say(p,"Quest complete: "+q.title); } }
                catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest auto-completion failed for %s: %s",q.id,ex.toString()); }
            }
                if(q.autoRepeat) { try { service.repeatAutomated(p.getUniqueID(),q,p.worldObj.getTotalWorldTime()); } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest repeat failed for %s: %s",q.id,ex.toString()); } }
            }
        }
    }
    private static boolean editor(EntityPlayerMP player) { return player.canCommandSenderUseCommand(2,"quests.edit"); }
    private void handle(EntityPlayerMP player,String json) {
        String message=""; boolean counted=false; String actionName="";
        try {
            QuestNetwork.Action action=QuestService.JSON.fromJson(json,QuestNetwork.Action.class);
            if(action!=null) actionName=action.action;
            if(action==null || action.action==null || action.id==null || action.choices==null || action.choices.length>128) throw new IllegalArgumentException("Invalid action");
            if(action.action.equals("screen_view")) {
                Long previous=LAST_SCREEN.get(player.getUniqueID()); if(previous!=null && ticks-previous<5) return; LAST_SCREEN.put(player.getUniqueID(),(long)ticks);
                if(player.getDistanceSq(action.x+.5,action.y+.5,action.z+.5)>4096 || !player.worldObj.blockExists(action.x,action.y,action.z)) return;
                net.minecraft.tileentity.TileEntity raw=player.worldObj.getTileEntity(action.x,action.y,action.z);
                if(!(raw instanceof QuestDevice) || !(raw.getBlockType() instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)raw.getBlockType()).mode!=1) return;
                QuestNetwork.Snapshot response=new QuestNetwork.Snapshot(); response.screen=QuestWorldMap.create(service,player.getUniqueID(),(QuestDevice)raw);
                QuestNetwork.sendResponse(QuestService.JSON.toJson(response),player); return;
            }
            if(action.action.equals("view") || action.action.equals("refresh")) { if(action.action.equals("refresh")) LAST_MESSAGE.remove(player.getUniqueID()); PENDING_VIEWS.add(player.getUniqueID()); return; }
            Long last=LAST_REQUEST.get(player.getUniqueID());
            if(last!=null && ticks-last<2) {
                QuestNetwork.Snapshot error=new QuestNetwork.Snapshot(); error.message="Error: Please wait before another action"; error.revision=service.revision;
                if("icon_edit".equals(actionName) || "storage_page".equals(actionName)) QuestService.say(player,error.message);
                QuestNetwork.sendResponse(QuestService.JSON.toJson(error),player); return;
            }
            LAST_REQUEST.put(player.getUniqueID(),(long)ticks); counted=true;
            if(!action.action.equals("icon_edit") && !action.action.equals("storage_page") && action.revision!=service.revision) throw new IllegalArgumentException("Book changed; refresh before acting");
            QuestCore.Quest quest=service.quests.get(action.id);
            List<Integer> choices=new ArrayList<Integer>(); for(int c:action.choices) choices.add(c);
            switch(action.action) {
                case "view": break;
                case "icon_edit": QuestIcons.edit(player,action.text,action.id,action.x,action.y); message="Icon updated"; QuestService.say(player,message); break;
                case "storage_page":
                    if(!(player.openContainer instanceof QuestStorageContainer)) throw new IllegalArgumentException("Open crate storage before changing its page");
                    QuestStorage storage=((QuestStorageContainer)player.openContainer).storage;
                    if(!storage.isUseableByPlayer(player) || storage.xCoord!=action.x || storage.yCoord!=action.y || storage.zCoord!=action.z) throw new IllegalArgumentException("Storage unavailable");
                    storage.nextPage(); message="Storage page changed"; break;
                case "pin": service.pin(player.getUniqueID(),action.id); message="Pinned quests updated"; break;
                case "click_command":
                    String command=service.clickCommand(player.getUniqueID(),action.id,editor(player));
                    MinecraftServer.getServer().getCommandManager().executeCommand(player,command); message="Command sent"; break;
                case "emergency": service.emergency(player); message="Emergency items received"; break;
                case "device_config":
                    if(action.deviceSettings==null || player.getDistanceSq(action.x+.5,action.y+.5,action.z+.5)>64 || !player.worldObj.blockExists(action.x,action.y,action.z))throw new IllegalArgumentException("Device unavailable");
                    net.minecraft.tileentity.TileEntity configured=player.worldObj.getTileEntity(action.x,action.y,action.z);
                    if(!(configured instanceof QuestDevice))throw new IllegalArgumentException("Device unavailable");
                    action.deviceSettings.apply(service,(QuestDevice)configured,player);message="Device settings saved";break;
                case "bind_device": case "unbind_device": case "screen_size": case "detector_mode": case "detector_settings": case "screen_options": case "screen_skin": case "screen_lock": case "screen_owner":
                    if(!(player.openContainer instanceof QuestDeviceContainer)) throw new IllegalArgumentException("Open the device before configuring it");
                    QuestDevice device=((QuestDeviceContainer)player.openContainer).tile;
                    if(device.xCoord!=action.x || device.yCoord!=action.y || device.zCoord!=action.z || !device.isUseableByPlayer(player)) throw new IllegalArgumentException("Device unavailable");
                    int deviceMode=((QuestBlocks.DeviceBlock)device.getBlockType()).mode;
                    if(deviceMode==6) throw new IllegalArgumentException("Crate openers use the crate loot table");
                    if((deviceMode==4 || deviceMode==5) && !editor(player)) throw new IllegalArgumentException("Operator permission required to configure task detectors");
                    if(action.action.equals("screen_skin")) {
                        if(action.choices.length!=1 || action.choices[0]<0 || action.choices[0]>1) throw new IllegalArgumentException("Invalid screen appearance");
                        QuestScreenSettings.appearance(device,player,player.getHeldItem(),action.choices[0]==1); message="Screen appearance updated"; break;
                    }
                    if(action.action.equals("screen_lock")) {
                        if(action.choices.length!=1 || action.choices[0]<0 || action.choices[0]>1) throw new IllegalArgumentException("Invalid protection setting");
                        QuestScreenSettings.protection(device,player,action.choices[0]==1); message="Screen protection updated"; break;
                    }
                    if(action.action.equals("screen_owner")) {
                        UUID owner;
                        try { owner=UUID.fromString(action.text); }
                        catch(IllegalArgumentException badId) { EntityPlayerMP target=MinecraftServer.getServer().getConfigurationManager().func_152612_a(action.text); if(target==null) throw new IllegalArgumentException("Use an online player name or UUID"); owner=target.getUniqueID(); }
                        QuestScreenSettings.owner(device,player,owner); message="Screen team updated"; break;
                    }
                    if(action.action.equals("screen_options")) {
                        if(device.indestructible && !editor(player)) throw new IllegalArgumentException("Operator permission required for protected screens");
                        if(deviceMode!=0 && deviceMode!=1 || device.screenLinked || action.choices.length!=1 || action.choices[0]<0 || action.choices[0]>3) throw new IllegalArgumentException("Invalid screen option");
                        int option=action.choices[0];
                        if(deviceMode==1 && option==0) device.screenFullscreen=!device.screenFullscreen;
                        else if(deviceMode==1 && option==1) device.screenHideIcons=!device.screenHideIcons;
                        else if(deviceMode==0 && option==2) device.screenInputOnly=!device.screenInputOnly;
                        else if(deviceMode==0 && option==3) {
                            net.minecraft.item.ItemStack icon=player.getHeldItem()==null?null:player.getHeldItem().copy();
                            if(icon!=null) { icon.stackSize=1; net.minecraft.nbt.NBTTagCompound encoded=new net.minecraft.nbt.NBTTagCompound(); icon.writeToNBT(encoded); if(encoded.toString().length()>8192 || net.minecraft.nbt.CompressedStreamTools.compress(encoded).length>4096) throw new IllegalArgumentException("Screen icon NBT is too large"); }
                            device.screenInputIcon=icon;
                        } else throw new IllegalArgumentException("Option does not apply to this screen");
                        QuestScreens.presentationChanged(device); message="Screen options updated"; break;
                    }
                    if(action.action.equals("detector_settings")) {
                        if(!editor(player) || deviceMode!=4 && deviceMode!=5) throw new IllegalArgumentException("Operator task detector required");
                        String[] settings=action.text.split(",",-1); if(settings.length!=5 || !settings[4].equals("true") && !settings[4].equals("false")) throw new IllegalArgumentException("Expected radius, offset, height, power");
                        device.configureDetector(Double.parseDouble(settings[0]),Double.parseDouble(settings[1]),Double.parseDouble(settings[2]),Integer.parseInt(settings[3])); device.detectorNotifications=Boolean.parseBoolean(settings[4]); device.markDirty(); player.worldObj.markBlockForUpdate(device.xCoord,device.yCoord,device.zCoord); message="Detector settings updated"; break;
                    }
                    if(action.action.equals("detector_mode")) { device.analog=!device.analog; device.markDirty(); player.worldObj.markBlockForUpdate(device.xCoord,device.yCoord,device.zCoord); message="Detector mode updated"; break; }
                    if(action.action.equals("screen_size")) {
                        if(action.choices.length!=2) throw new IllegalArgumentException("Expected screen width and height");
                        QuestScreens.configure(device,action.choices[0],action.choices[1]); message="Screen size updated"; break;
                    }
                    if(device.screenLinked) throw new IllegalArgumentException("Bind the core of this screen assembly");
                    if(action.action.equals("unbind_device")) { device.quest=""; device.task=""; }
                    else {
                        int mode=((QuestBlocks.DeviceBlock)device.getBlockType()).mode;
                        if(mode==1 || mode==2 || mode==9) {
                            service.validateProgressReference(action.id); if(!editor(player) && !service.bindable(player.getUniqueID(),action.id)) throw new IllegalArgumentException("Progress object is not visible"); device.quest=action.id; device.task=""; device.bindingChanged(); message="Device binding updated"; break;
                        }
                        if(quest==null || !editor(player) && !service.visible(player.getUniqueID(),quest)) throw new IllegalArgumentException("Unknown quest");
                        if(mode==3) { service.validateCollectorReward(quest,action.text); device.quest=quest.id; device.task=action.text; device.bindingChanged(); message="Device binding updated"; break; }
                        if(action.text==null || !action.text.isEmpty() && !quest.tasks.stream().anyMatch(t->t.id.equals(action.text))) throw new IllegalArgumentException("Unknown task");
                        device.quest=quest.id; device.task=action.text;
                    }
                    device.bindingChanged(); message="Device binding updated"; break;
                case "autopin":service.progress().autoPin(player.getUniqueID(),!service.progress().autoPin(player.getUniqueID()));message="Automatic tracking updated";break;
                case "team_settings":service.progress().teamSettings(player.getUniqueID(),action.teamSettings);message="Team settings saved";break;
                case "team_select":service.progress().selectTeam(player.getUniqueID(),action.text);message="Team selection updated";break;
                case "team_player":service.progress().teamPlayerAction(player.getUniqueID(),UUID.fromString(action.id),action.text);message="Team player updated";break;
                case "team_exit":service.progress().exitTeam(player.getUniqueID());message="Left team";break;
                case "team_create": case "team_invite": case "team_join": case "team_kick": case "team_leave": case "team_disband":
                    if(action.text==null || action.text.length()>(action.action.equals("team_create")?35:32))throw new IllegalArgumentException("Invalid team input");
                    if(action.action.equals("team_create") && (action.x<0 || action.x>0xFFFFFF))throw new IllegalArgumentException("Invalid team color");
                    new QuestCommand().team(player,player,new String[]{"team",action.action.substring(5),action.text});
                    if(action.action.equals("team_create"))service.progress().teamColor(player.getUniqueID(),action.x==0?0x0094FF:action.x);
                    message="Team updated"; break;
                case "start": if(quest==null) throw new IllegalArgumentException("Unknown quest"); service.start(player,quest); message="Quest started"; break;
                case "task": if(quest==null) throw new IllegalArgumentException("Unknown quest"); service.confirm(player,quest,action.text); message=quest.tasks.stream().anyMatch(t->t.id.equals(action.text) && t.type.equals("custom"))?"Task action sent":"Task confirmed"; break;
                case "submit": if(quest==null) throw new IllegalArgumentException("Unknown quest"); message="Submitted: "+service.submit(player,quest,action.text); break;
                case "check": if(quest==null || !service.finish(player,quest)) throw new IllegalArgumentException("Tasks incomplete or quest locked"); message="Quest complete"; break;
                case "click_task": service.clickTask(player,quest,action.text); message="Task updated"; break;
                case "claim_all":message="Rewards claimed: "+service.claimAll(player);break;
                case "claim_reward": service.claimReward(player,quest,action.text,choices); message="Reward claimed"; break;
                case "claim": if(quest==null) throw new IllegalArgumentException("Unknown quest"); service.claim(player,quest,choices); message="Rewards claimed"; break;
                case "repeat": if(quest==null) throw new IllegalArgumentException("Unknown quest"); service.repeat(player,quest); message="Quest reset"; break;
                case "progress_reset": case "progress_complete":
                    QuestProgressActions.apply(service,player,action.id,action.action.equals("progress_complete"),action.text.equals("dependencies"));message="Progress updated";break;
                case "save": if(!editor(player)) throw new IllegalArgumentException("Operator permission required"); service.save(action.book,action.revision); message="Book saved (backup written)"; break;
                default: throw new IllegalArgumentException("Unknown action");
            }
        } catch(Exception ex) {
            message="Error: "+ex.getMessage();
            if("icon_edit".equals(actionName) || "storage_page".equals(actionName)) QuestService.say(player,message);
            if(!counted) { QuestNetwork.Snapshot error=new QuestNetwork.Snapshot(); error.message=message; error.revision=service.revision; QuestNetwork.sendResponse(QuestService.JSON.toJson(error),player); return; }
        }
        PENDING_VIEWS.remove(player.getUniqueID()); LAST_VIEW.put(player.getUniqueID(),(long)ticks); sendSnapshot(player,message);
    }
    public static void sendSnapshot(EntityPlayerMP player,String message) {
        if(message==null) message=LAST_MESSAGE.getOrDefault(player.getUniqueID(),""); else LAST_MESSAGE.put(player.getUniqueID(),message);
        QuestNetwork.Snapshot snapshot=new QuestNetwork.Snapshot(); snapshot.profile=service.progress().profile(); snapshot.revision=service.revision; snapshot.canEdit=editor(player); snapshot.message=message;
        QuestProgress data=service.progress(); UUID id=player.getUniqueID(); Set<String> done=data.completed(id);
        snapshot.emergencyWait=service.emergencyWait(player); snapshot.team=data.teamName(id); snapshot.teamLeader=data.leader(id); snapshot.teamColor=data.teamColor(id);
        for(Object raw:net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList){EntityPlayerMP online=(EntityPlayerMP)raw;data.remember(online.getUniqueID(),online.getCommandSenderName());}
        snapshot.autoPin=data.autoPin(id);snapshot.teamId=data.team(id);snapshot.teamModerator=data.moderator(id);snapshot.teamSettings=data.teamSettings(id);snapshot.teams=data.publicTeams(id);snapshot.teamPlayers=data.teamPlayers(id);
        for(QuestNetwork.TeamPlayer entry:snapshot.teamPlayers)for(Object raw:net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList)if(((EntityPlayerMP)raw).getUniqueID().toString().equals(entry.id))entry.online=true;
        for(UUID member:data.members(id)) snapshot.teamMembers.add(playerName(member));
        for(UUID leader:data.invitations(id)) snapshot.teamInvites.add(playerName(leader));
        snapshot.leaderboard=data.leaderboard(service.quests.keySet());
        for(QuestNetwork.Rank rank:snapshot.leaderboard) try { rank.name=playerName(UUID.fromString(rank.name)); } catch(IllegalArgumentException teamName) {}
        for(QuestCore.Chapter chapter:service.book.chapters) {snapshot.chapterStates.put(chapter.id,service.chapterState(id,chapter.id));snapshot.chapterPercentages.put(chapter.id,service.objectPercent(id,"chapter:"+chapter.id));}
        snapshot.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(service.book),QuestCore.Book.class);
        if(!snapshot.canEdit) {
            Iterator<QuestCore.Quest> it=snapshot.book.quests.iterator();
            while(it.hasNext()) { QuestCore.Quest q=it.next(); if(q.hidden && !done.contains(q.id) && !service.unlocked(id,q) || !QuestPresentation.visibleChapter(service.book,q.chapter)) it.remove(); }
            snapshot.book.chapters.removeIf(chapter->!QuestPresentation.visibleChapter(service.book,chapter.id));
            for(QuestCore.Chapter chapter:snapshot.book.chapters) chapter.images.removeIf(image->image.dev);
            snapshot.chapterStates.keySet().retainAll(snapshot.book.chapters.stream().map(chapter->chapter.id).collect(java.util.stream.Collectors.toSet()));snapshot.chapterPercentages.keySet().retainAll(snapshot.chapterStates.keySet());
            // Command and custom implementation details are server-only for non-editors.
            for(List<QuestCore.Reward> table:snapshot.book.lootTables.values()) redact(table);
            for(QuestCore.Quest q:snapshot.book.quests) { redact(q.rewards); if(q.hideTextUntilComplete && !done.contains(q.id)) { q.guidePage=""; q.customClick=""; q.description=""; q.pages.clear(); q.richPages.clear(); q.uri=""; } }
        }
        Set<String> exposed=new HashSet<String>(); for(QuestCore.Quest q:snapshot.book.quests) exposed.add(q.id);
        for(String pin:data.pins(id)) if(exposed.contains(pin) && service.visible(id,service.quests.get(pin))) { snapshot.pinned.add(pin); snapshot.percentages.put(pin,service.objectPercent(id,pin)); }
        for(QuestCore.Quest q:snapshot.book.quests) {
            snapshot.percentages.put(q.id,service.objectPercent(id,q.id));
            snapshot.states.put(q.id,done.contains(q.id) ? service.claimed(id,q) ? "claimed" : "complete" : service.unlocked(id,q) ? "available" : "locked");
            for(int i=0;i<q.rewards.size();i++) { QuestCore.Reward r=q.rewards.get(i); snapshot.rewardStates.put(q.id+":"+r.id,data.rewardClaimed(id,q.id,r.id,service.individual(q,r))); }
            for(QuestCore.Task task:q.tasks) {
                snapshot.counters.put(q.id+":"+task.id,service.counter(player,q,task));
                snapshot.taskStates.put(q.id+":"+task.id,service.dependencies(id).satisfied("task:"+q.id+"/"+task.id,false));
            }
        }
        QuestNetwork.sendResponse(QuestService.JSON.toJson(snapshot),player);
    }
    private static String playerName(UUID id) {
        com.mojang.authlib.GameProfile profile=MinecraftServer.getServer().func_152358_ax().func_152652_a(id);
        return profile==null || profile.getName()==null?id.toString():profile.getName();
    }
    private static void redact(List<QuestCore.Reward> rewards) {
        for(QuestCore.Reward r:rewards) { if(r.type.equals("command") || r.type.equals("custom")) r.target="Server reward"; redact(r.options); }
    }
    public static final class QuestBookItem extends Item {
        public QuestBookItem() { setUnlocalizedName("sigmarixquests.book"); setTextureName("ftbquests:book"); setMaxStackSize(1); setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabMisc); }
        @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player) { if(world.isRemote) proxy.open(); return stack; }
    }
    private static final class QuestCommand extends CommandBase {
        @Override public String getCommandName() { return "quests"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/quests list|show/start/check/claim/repeat <id>|task <id> <task> [choices...]|reload|team|reset <id> <player>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public void processCommand(ICommandSender sender,String[] args) {
            try {
                if(args.length>0 && args[0].equals("reload")) {
                    if(!sender.canCommandSenderUseCommand(2,"quests.reload")) throw new IllegalArgumentException("Operator permission required");
                    service.reload(); QuestService.say(sender,"Quest book and scripts reloaded"); return;
                }
                if(args.length>0 && args[0].equals("mode")) {
                    if(args.length>1) {
                        if(!sender.canCommandSenderUseCommand(2,"quests.mode")) throw new IllegalArgumentException("Operator permission required");
                        if(args.length==3 && args[1].equals("create")) service.createProfile(args[2]);
                        else if(args.length==2) service.switchProfile(args[1]);
                        else throw new IllegalArgumentException("/quests mode [<id>|create <id>]");
                    }
                    QuestService.say(sender,"Quest mode: "+service.progress().profile()+"; available: "+service.profiles()); return;
                }
                if(args.length==3 && args[0].equals("reset")) {
                    if(!sender.canCommandSenderUseCommand(2,"quests.reset")) throw new IllegalArgumentException("Operator permission required");
                    if(!service.quests.containsKey(args[1])) throw new IllegalArgumentException("Unknown quest");
                    EntityPlayerMP target=getPlayer(sender,args[2]); service.progress().reset(target.getUniqueID(),args[1]); QuestService.say(sender,"Progress reset"); return;
                }
                if(args.length>0 && (args[0].equals("stage") || args[0].equals("skill"))) {
                    if(!sender.canCommandSenderUseCommand(2,"quests.attributes")) throw new IllegalArgumentException("Operator permission required");
                    if(args.length<3 || args.length>4) throw new IllegalArgumentException("/quests stage <player> <key> [add|remove] or skill <player> <key> [level]");
                    EntityPlayerMP target=getPlayer(sender,args[1]); QuestProgress attributes=service.progress();
                    if(args[0].equals("stage")) {
                        if(args.length==4) { if(!args[3].equals("add") && !args[3].equals("remove")) throw new IllegalArgumentException("Use add or remove"); attributes.stage(target.getUniqueID(),args[2],args[3].equals("add")); }
                        QuestService.say(sender,"Stage "+args[2]+": "+attributes.stage(target.getUniqueID(),args[2]));
                    } else {
                        if(args.length==4) attributes.skill(target.getUniqueID(),args[2],Integer.parseInt(args[3]));
                        QuestService.say(sender,"Skill "+args[2]+": "+attributes.skill(target.getUniqueID(),args[2]));
                    }
                    return;
                }
                EntityPlayerMP p=getCommandSenderAsPlayer(sender); QuestProgress data=service.progress(); UUID id=p.getUniqueID();
                if(args.length==0 || args[0].equals("list")) {
                    for(QuestCore.Quest q:service.quests.values()) {
                        Set<String> done=data.completed(id); boolean unlocked=service.unlocked(id,q);
                        if(!editor(p) && !service.visible(id,q)) continue;
                        String state=done.contains(q.id)?service.claimed(id,q)?"claimed":"complete":unlocked?"available":"locked";sender.addChatMessage(new ChatComponentText(q.id+" - "+q.title+" [").appendSibling(QuestMessages.component(state)).appendText("]"));
                    }
                    QuestService.say(sender,"GUI: /questbook (client mod), "+getCommandUsage(sender)); return;
                }
                if(args[0].equals("emergency")) { service.emergency(p); QuestService.say(sender,"Emergency items received"); return; }
                if(args[0].equals("team")) { team(sender,p,args); return; }
                if(args[0].equals("bind") || args[0].equals("unbind")) {
                    net.minecraft.util.MovingObjectPosition hit=p.rayTrace(6,1);
                    if(hit==null || hit.typeOfHit!=net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) throw new IllegalArgumentException("Look at a quest device");
                    net.minecraft.tileentity.TileEntity tile=p.worldObj.getTileEntity(hit.blockX,hit.blockY,hit.blockZ);
                    if(!(tile instanceof QuestDevice)) throw new IllegalArgumentException("Look at a quest device");
                    QuestDevice device=(QuestDevice)tile;
                    if(device.screenLinked) throw new IllegalArgumentException("Bind the core of this screen assembly");
                    if(!device.isUseableByPlayer(p)) throw new IllegalArgumentException("Only device owner, team member or operator can bind");
                    if(args[0].equals("unbind")) { device.quest=""; device.task=""; }
                    else {
                        int mode=((QuestBlocks.DeviceBlock)device.getBlockType()).mode;
                        if((mode==1 || mode==2 || mode==9) && args.length==2) { service.validateProgressReference(args[1]); if(!editor(p) && !service.bindable(id,args[1])) throw new IllegalArgumentException("Progress object is not visible"); device.quest=args[1]; device.task=""; device.bindingChanged(); QuestService.say(sender,"Device binding updated"); return; }
                        if(args.length<2 || args.length>3 || !service.quests.containsKey(args[1])) throw new IllegalArgumentException("/quests bind <quest> [task]");
                        if(!editor(p) && !service.visible(id,service.quests.get(args[1]))) throw new IllegalArgumentException("Unknown quest");
                        String task=args.length==3?args[2]:"";
                        if(!task.isEmpty()) { boolean found=false; for(QuestCore.Task t:service.quests.get(args[1]).tasks) if(t.id.equals(task)) found=true; if(!found) throw new IllegalArgumentException("Unknown task ID"); }
                        device.quest=args[1]; device.task=task;
                    }
                    device.bindingChanged(); QuestService.say(sender,"Device binding updated"); return;
                }
                if(args.length<2) throw new IllegalArgumentException(getCommandUsage(sender));
                QuestCore.Quest q=service.quests.get(args[1]); if(q==null) throw new IllegalArgumentException("Unknown quest");
                switch(args[0]) {
                    case "show":
                        if(!editor(p) && !service.visible(id,q)) throw new IllegalArgumentException("Unknown quest");
                        QuestService.say(sender,q.title+": "+q.description); QuestService.say(sender,"Dependencies: "+q.dependencies);
                        for(QuestCore.Task t:q.tasks) sender.addChatMessage(QuestMessages.component(t.type).appendText(" "+t.target+" x"+t.count+" ["+service.counter(p,q,t)+"]"));
                        for(QuestCore.Reward r:q.rewards) QuestService.say(sender,"Reward: "+r.type+" "+(r.item==null?"":r.item)+" x"+r.count); break;
                    case "start": service.start(p,q); QuestService.say(sender,"Quest started"); break;
                    case "task": if(args.length!=3) throw new IllegalArgumentException("/quests task <quest> <task>"); service.confirm(p,q,args[2]); QuestService.say(sender,"Task confirmed"); break;
                    case "submit": if(args.length!=3) throw new IllegalArgumentException("/quests submit <quest> <task>"); QuestService.say(sender,"Submitted: "+service.submit(p,q,args[2])); break;
                    case "check": if(!service.finish(p,q)) throw new IllegalArgumentException("Tasks incomplete or quest locked"); QuestService.say(sender,"Quest complete"); break;
                    case "claim":
                        List<Integer> choices=new ArrayList<Integer>(); for(int i=2;i<args.length;i++) choices.add(Integer.parseInt(args[i]));
                        service.claim(p,q,choices); QuestService.say(sender,"Rewards claimed"); break;
                    case "repeat": service.repeat(p,q); QuestService.say(sender,"Quest reset"); break;
                    default: throw new IllegalArgumentException(getCommandUsage(sender));
                }
            } catch(Exception e) { QuestService.say(sender,"Error: "+e.getMessage()); }
        }
        private void team(ICommandSender sender,EntityPlayerMP p,String[] args) {
            QuestProgress data=service.progress(); UUID id=p.getUniqueID();
            if(args.length<2 || args[1].equals("info")) { QuestService.say(sender,"Team: "+data.teamName(id)+"; /quests team create <name>|invite/join/kick <player>|leave|disband"); return; }
            switch(args[1]) {
                case "create": if(args.length!=3) throw new IllegalArgumentException("Specify team name"); data.createTeam(id,args[2]); break;
                case "invite": case "join": case "kick":
                    if(args.length!=3) throw new IllegalArgumentException("Specify online player");
                    EntityPlayerMP target=getPlayer(sender,args[2]);
                    if(args[1].equals("invite")) { data.invite(id,target.getUniqueID()); QuestService.say(target,"Quest team invitation from "+p.getCommandSenderName()+". /quests team join "+p.getCommandSenderName()); }
                    else if(args[1].equals("join")) data.join(id,target.getUniqueID()); else data.kick(id,target.getUniqueID()); break;
                case "leave": data.leave(id); break;
                case "disband": data.disband(id); break;
                default: throw new IllegalArgumentException("Unknown team operation");
            }
            QuestService.say(sender,"Team updated: "+data.teamName(id));
        }
    }
}
