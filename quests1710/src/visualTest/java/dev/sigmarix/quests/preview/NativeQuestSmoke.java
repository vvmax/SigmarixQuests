package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import java.util.*;
import javax.script.*;
import net.minecraft.entity.player.EntityPlayerMP;
/** Real server checks for native stages, skills, JavaScript callbacks and pack modes. */
public final class NativeQuestSmoke {
    private static String profile,original;
    private static int storageX,storageY,storageZ,offlineX,offlineY,offlineZ;
    private static UUID offlineOwner;
    public static int[] storagePosition() { return new int[]{storageX,storageY,storageZ}; }
    public static void run(EntityPlayerMP player) throws Exception {
        QuestService main=QuestsMod.service; UUID id=player.getUniqueID();
        original=main.progress().profile(); profile="smoke-"+UUID.randomUUID().toString(); main.createProfile(profile);
        long revision=main.revision; main.switchProfile(profile);
        if(main.revision<=revision) throw new IllegalStateException("Pack mode switch did not invalidate stale GUI actions");
        main.progress().complete(id,"mode-marker"); main.progress().claimReward(id,"mode-marker","root",true);
        main.switchProfile(original);
        if(main.progress().completed(id).contains("mode-marker") || main.progress().rewardClaimed(id,"mode-marker","root",true)) throw new IllegalStateException("Pack modes leaked progress or claims");
        main.switchProfile(profile);
        if(!main.progress().completed(id).contains("mode-marker")) throw new IllegalStateException("Pack mode lost progress");
        main.switchProfile(original);
        ScriptEngine engine=QuestScripts.engine();
        if(engine==null) throw new IllegalStateException("Native pack scripts unavailable");
        engine.put("quests",new QuestScripts.API());
        engine.eval("quests.task('preview:native',function(player,task){return quests.stage(player,'preview:stage') && quests.skill(player,'preview:skill')>=12;});"+
            "quests.reward('preview:native',function(player,reward){quests.setStage(player,'preview:stage',true);quests.setSkill(player,'preview:skill',12);});");
        QuestCore.Task task=new QuestCore.Task(); task.type="custom"; task.target="preview:native";
        if(QuestHooks.TASKS.get(task.target).complete(player,task)) throw new IllegalStateException("Unfinished native markers passed a script task");
        QuestHooks.REWARDS.get(task.target).grant(player,new QuestCore.Reward());
        if(!QuestHooks.TASKS.get(task.target).complete(player,task)) throw new IllegalStateException("Native scripted reward did not unlock its task");
        java.nio.file.Path reload=java.nio.file.Files.createTempDirectory("quest-native-reload");
        try {
            java.nio.file.Files.write(reload.resolve("test.js"),"quests.task('preview:reload',function(p,t){return false;});".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try { QuestScripts.reload(reload.toFile(),()->{throw new java.io.IOException("expected invalid book");}); throw new IllegalStateException("Invalid script reload accepted"); } catch(java.io.IOException expected) {}
            if(QuestHooks.TASKS.containsKey("preview:reload") || !QuestHooks.TASKS.get(task.target).complete(player,task)) throw new IllegalStateException("Failed script reload did not preserve the previous handlers");
        } finally { java.nio.file.Files.deleteIfExists(reload.resolve("test.js")); java.nio.file.Files.deleteIfExists(reload); }
        QuestCore.Book previous=main.book; QuestCore.Book utilities=new QuestCore.Book();
        QuestCore.Reward sticks=new QuestCore.Reward(); sticks.item="minecraft:stick"; sticks.count=2; utilities.emergencyItems.add(sticks);
        utilities.dropLootCrates=true; utilities.noDrop=new QuestLoot.Weights(0,0,0); utilities.lootTables.put("preview-drop",Collections.singletonList(sticks)); QuestCore.LootSettings drop=new QuestCore.LootSettings(); drop.drops.passive=1; utilities.lootSettings.put("preview-drop",drop);
        QuestCore.Quest chestQuest=new QuestCore.Quest(); chestQuest.id="storage-marker"; chestQuest.title="Chest smoke";
        QuestCore.Task input=new QuestCore.Task(); input.id="input"; input.type="item"; input.target="minecraft:iron_ingot"; input.count=2; input.consume=true; chestQuest.tasks.add(input); utilities.quests.add(chestQuest);
        QuestCore.Quest meter=new QuestCore.Quest(); meter.id="storage-meter"; meter.title="Progress detector"; QuestCore.Task power=new QuestCore.Task(); power.id="power"; power.type="energy"; power.count=100; meter.tasks.add(power); utilities.quests.add(meter);
        QuestCore.Quest offline=new QuestCore.Quest(); offline.id="offline-machine"; offline.title="Offline resource machine"; offline.optional=true; offline.autoClaim=true;
        QuestCore.Task offlineRf=new QuestCore.Task(); offlineRf.id="rf"; offlineRf.type="energy"; offlineRf.count=100; offline.tasks.add(offlineRf);
        QuestCore.Task offlineFluid=new QuestCore.Task(); offlineFluid.id="water"; offlineFluid.type="fluid"; offlineFluid.target="water"; offlineFluid.count=250; offline.tasks.add(offlineFluid);
        QuestCore.Task offlineItems=new QuestCore.Task(); offlineItems.id="items"; offlineItems.type="item"; offlineItems.target="minecraft:iron_ingot"; offlineItems.count=2; offlineItems.consume=true; offline.tasks.add(offlineItems);
        QuestCore.Reward offlineReward=new QuestCore.Reward(); offlineReward.id="reward"; offlineReward.item="minecraft:diamond"; offline.rewards.add(offlineReward); utilities.quests.add(offline);
        Map<String,QuestCore.Quest> previousQuests=main.quests; Map<String,QuestCore.Quest> utilityQuests=new LinkedHashMap<String,QuestCore.Quest>(); utilityQuests.put(chestQuest.id,chestQuest); utilityQuests.put(meter.id,meter); utilityQuests.put(offline.id,offline); main.quests=utilityQuests;
        DropObserver observer=new DropObserver(); net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(observer); main.book=utilities;
        try {
            storageX=(int)Math.floor(player.posX)+4; storageY=(int)Math.floor(player.posY); storageZ=(int)Math.floor(player.posZ)+4;
            net.minecraft.world.World world=player.worldObj;
            offlineOwner=UUID.randomUUID(); offlineX=(int)Math.floor(player.posX)+8; offlineY=(int)Math.floor(player.posY); offlineZ=(int)Math.floor(player.posZ)+2;
            world.setBlock(offlineX,offlineY,offlineZ,QuestBlocks.devices[0]); QuestDevice offlineMachine=(QuestDevice)world.getTileEntity(offlineX,offlineY,offlineZ); offlineMachine.owner=offlineOwner; offlineMachine.quest=offline.id; offlineMachine.task=offlineRf.id; offlineMachine.bindingChanged();
            if(offlineMachine.player()!=null || offlineMachine.receiveEnergy(net.minecraftforge.common.util.ForgeDirection.UP,100,false)!=100) throw new IllegalStateException("Offline RF terminal did not accept energy without an online owner");
            offlineMachine.task=offlineFluid.id; offlineMachine.bindingChanged(); if(offlineMachine.fill(net.minecraftforge.common.util.ForgeDirection.UP,new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER,250),true)!=250) throw new IllegalStateException("Offline fluid terminal did not accept its fluid");
            offlineMachine.task=offlineItems.id; offlineMachine.bindingChanged(); world.setBlock(offlineX,offlineY+1,offlineZ,net.minecraft.init.Blocks.hopper,0,3);
            net.minecraft.tileentity.TileEntityHopper feeder=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(offlineX,offlineY+1,offlineZ); feeder.setInventorySlotContents(0,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,2));
            for(int i=0;i<40;i++) { feeder.updateEntity(); offlineMachine.updateEntity(); }
            if(!main.progress().completed(offlineOwner).contains(offline.id) || main.progress().value(offlineOwner,offline.id,offlineItems.id)!=2 || offlineMachine.getStackInSlot(0)!=null || main.claimed(offlineOwner,offline)) throw new IllegalStateException("Offline hopper submission did not finish stored requirements or claimed player rewards early");
            if(main.progress().value(id,offline.id,offlineRf.id)!=0) throw new IllegalStateException("Offline machine changed the bystander's progress"); world.setBlockToAir(offlineX,offlineY+1,offlineZ);
            world.setBlock(storageX,storageY,storageZ,QuestBlocks.devices[7]); QuestStorage chest=(QuestStorage)world.getTileEntity(storageX,storageY,storageZ); chest.owner=id; chest.bindingChanged(); chest.setInventorySlotContents(26,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,2));
            for(int i=0;i<20;i++) chest.updateEntity();
            if(chest.getStackInSlot(26)!=null || !main.progress().completed(id).contains(chestQuest.id)) throw new IllegalStateException("Quest chest did not submit items and finish its quest");
            world.setBlockToAir(storageX,storageY,storageZ); world.setBlock(storageX,storageY,storageZ,QuestBlocks.devices[8]); QuestStorage storage=(QuestStorage)world.getTileEntity(storageX,storageY,storageZ); storage.owner=id; storage.bindingChanged(); storage.setInventorySlotContents(53,QuestLoot.crate(utilities,"preview-drop"));
            net.minecraft.item.ItemStack crate=QuestLoot.crate(utilities,"preview-drop"); storage.store(crate,5000000000L); storage.compact();
            if(storage.reserved()!=4999999937L || storage.getStackInSlot(0).stackSize!=64 || storage.getStackInSlot(53)!=null) throw new IllegalStateException("Virtual crate bank did not absorb intake or refill output");
            world.setBlock(storageX,storageY-1,storageZ,net.minecraft.init.Blocks.hopper,0,3);
            net.minecraft.tileentity.TileEntityHopper hopper=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(storageX,storageY-1,storageZ);
            for(int i=0;i<16;i++) hopper.updateEntity(); long pulled=0;
            for(int i=0;i<hopper.getSizeInventory();i++) if(hopper.getStackInSlot(i)!=null) { pulled+=hopper.getStackInSlot(i).stackSize; storage.store(hopper.getStackInSlot(i),hopper.getStackInSlot(i).stackSize); hopper.setInventorySlotContents(i,null); }
            if(pulled<=0) throw new IllegalStateException("Actual hopper could not extract virtual bank output"); storage.compact(); world.setBlockToAir(storageX,storageY-1,storageZ);
            if(storage.reserved()!=4999999937L) throw new IllegalStateException("Hopper roundtrip changed total stored crates");
            world.setBlock(storageX+3,storageY,storageZ,QuestBlocks.devices[8]); QuestStorage breakable=(QuestStorage)world.getTileEntity(storageX+3,storageY,storageZ); breakable.store(crate,256); breakable.compact(); world.setBlockToAir(storageX+3,storageY,storageZ);
            long sealed=0; int parcels=0;
            for(Object raw:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,net.minecraft.util.AxisAlignedBB.getBoundingBox(storageX+2,storageY-1,storageZ-1,storageX+5,storageY+2,storageZ+2))) {
                net.minecraft.entity.item.EntityItem entity=(net.minecraft.entity.item.EntityItem)raw; net.minecraft.item.ItemStack dropStack=entity.getEntityItem();
                if(dropStack.getItem() instanceof QuestCrateParcel) { parcels++; sealed+=QuestCrateParcel.count(dropStack); entity.setDead(); }
            }
            if(parcels!=1 || sealed!=192) throw new IllegalStateException("Breaking storage lost or duplicated its virtual reserve");
            world.setBlock(storageX+2,storageY,storageZ,QuestBlocks.devices[9]); QuestDevice detector=(QuestDevice)world.getTileEntity(storageX+2,storageY,storageZ); detector.owner=id; detector.quest="task:"+meter.id+"/"+power.id; detector.task=power.id; detector.analog=true; detector.bindingChanged(); main.creditTask(player,meter.id,power.id,50);
            for(int i=0;i<20;i++) detector.updateEntity(); if(detector.redstoneOutput!=8) throw new IllegalStateException("Progress detector did not emit analog half-progress");
            main.creditTask(player,meter.id,power.id,50); detector.analog=false;
            for(int i=0;i<20;i++) detector.updateEntity(); if(detector.redstoneOutput!=15) throw new IllegalStateException("Progress detector did not emit completion signal"); main.finish(player,meter); detector.quest="book:"; detector.bindingChanged(); for(int i=0;i<20;i++) detector.updateEntity(); if(detector.redstoneOutput!=15) throw new IllegalStateException("Whole-book progress detector binding failed"); world.setBlockToAir(storageX+2,storageY,storageZ);
            main.emergency(player); try { main.emergency(player); throw new IllegalStateException("Emergency cooldown bypassed"); } catch(IllegalArgumentException expected) {}
            net.minecraft.entity.passive.EntityPig pig=new net.minecraft.entity.passive.EntityPig(player.worldObj); pig.setPosition(player.posX+10,player.posY,player.posZ+10); player.worldObj.spawnEntityInWorld(pig);
            pig.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(player),1000);
            if(!observer.seen) throw new IllegalStateException("Actual creature death did not drop its configured crate");
        } finally { main.book=previous; main.quests=previousQuests; net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(observer); }
        final java.util.List<String> denied=new java.util.ArrayList<String>();
        net.minecraft.command.ICommandSender unprivileged=new net.minecraft.command.ICommandSender() {
            public String getCommandSenderName() { return "QuestNoOp"; }
            public net.minecraft.util.IChatComponent func_145748_c_() { return new net.minecraft.util.ChatComponentText("QuestNoOp"); }
            public void addChatMessage(net.minecraft.util.IChatComponent message) { denied.add(message.getUnformattedText()); }
            public boolean canCommandSenderUseCommand(int level,String command) { return level==0; }
            public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() { return player.getPlayerCoordinates(); }
            public net.minecraft.world.World getEntityWorld() { return player.worldObj; }
        };
        net.minecraft.server.MinecraftServer.getServer().getCommandManager().executeCommand(unprivileged,"/quests stage "+player.getCommandSenderName()+" native-click add");
        if(denied.isEmpty() || main.progress().stage(id,"native-click")) throw new IllegalStateException("Player command permissions were bypassed");
        System.out.println("Quest native PASS: real JavaScript task/reward, personal stages/skills, independent pack modes and claims, emergency cooldown, real creature crate drop, automatic quest chest, offline RF/fluid/hopper submission and restart, virtual crate storage beyond 32-bit counts, actual hopper extraction, sealed break drops and analog detector");
    }
    public static final class DropObserver {
        boolean seen;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void dropped(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
            for(net.minecraft.entity.item.EntityItem entity:event.drops) { net.minecraft.item.ItemStack stack=entity.getEntityItem(); if(stack!=null && stack.hasTagCompound() && stack.getTagCompound().getString("table").equals("preview-drop")) seen=true; }
        }
    }
    public static void verify(EntityPlayerMP player) throws Exception {
        QuestService main=QuestsMod.service; UUID id=player.getUniqueID();
        net.minecraft.tileentity.TileEntity offlineRaw=player.worldObj.getTileEntity(offlineX,offlineY,offlineZ);
        if(!(offlineRaw instanceof QuestDevice) || !offlineOwner.equals(((QuestDevice)offlineRaw).owner) || !main.progress().completed(offlineOwner).contains("offline-machine") || main.progress().value(offlineOwner,"offline-machine","rf")!=100 || main.progress().value(offlineOwner,"offline-machine","water")!=250 || main.progress().value(offlineOwner,"offline-machine","items")!=2) throw new IllegalStateException("Offline machine progress or binding did not survive world restart");
        net.minecraft.tileentity.TileEntity raw=player.worldObj.getTileEntity(storageX,storageY,storageZ);
        if(!(raw instanceof QuestStorage) || ((QuestStorage)raw).getSizeInventory()!=54 || ((QuestStorage)raw).reserved()!=4999999937L || ((QuestStorage)raw).getStackInSlot(0)==null || !((QuestStorage)raw).getStackInSlot(0).getTagCompound().getString("table").equals("preview-drop")) throw new IllegalStateException("Crate storage did not survive world restart");
        if(!main.progress().profile().equals(original) || !main.progress().stage(id,"preview:stage") || main.progress().skill(id,"preview:skill")!=12) throw new IllegalStateException("Native markers or selected pack mode did not survive restart");
        if(main.emergencyWait(player)<=0) throw new IllegalStateException("Emergency cooldown did not survive restart");
        main.switchProfile(profile);
        if(!main.progress().completed(id).contains("mode-marker") || !main.progress().rewardClaimed(id,"mode-marker","root",true)) throw new IllegalStateException("Alternate mode progress did not survive restart");
        main.switchProfile(original);
    }
}
