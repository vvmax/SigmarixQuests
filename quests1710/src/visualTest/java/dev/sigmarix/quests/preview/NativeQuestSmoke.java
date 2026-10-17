package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import java.util.*;
import javax.script.*;
import net.minecraft.entity.player.EntityPlayerMP;
/** Real server checks for native stages, skills, JavaScript callbacks and pack modes. */
public final class NativeQuestSmoke {
    private static String profile,original;
    private static int storageX,storageY,storageZ;
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
        Map<String,QuestCore.Quest> previousQuests=main.quests; Map<String,QuestCore.Quest> utilityQuests=new LinkedHashMap<String,QuestCore.Quest>(); utilityQuests.put(chestQuest.id,chestQuest); utilityQuests.put(meter.id,meter); main.quests=utilityQuests;
        DropObserver observer=new DropObserver(); net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(observer); main.book=utilities;
        try {
            storageX=(int)Math.floor(player.posX)+6; storageY=(int)Math.floor(player.posY); storageZ=(int)Math.floor(player.posZ)+6;
            net.minecraft.world.World world=player.worldObj;
            world.setBlock(storageX,storageY,storageZ,QuestBlocks.devices[7]); QuestStorage chest=(QuestStorage)world.getTileEntity(storageX,storageY,storageZ); chest.owner=id; chest.bindingChanged(); chest.setInventorySlotContents(26,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,2));
            for(int i=0;i<20;i++) chest.updateEntity();
            if(chest.getStackInSlot(26)!=null || !main.progress().completed(id).contains(chestQuest.id)) throw new IllegalStateException("Quest chest did not submit items and finish its quest");
            world.setBlockToAir(storageX,storageY,storageZ); world.setBlock(storageX,storageY,storageZ,QuestBlocks.devices[8]); QuestStorage storage=(QuestStorage)world.getTileEntity(storageX,storageY,storageZ); storage.owner=id; storage.bindingChanged(); storage.setInventorySlotContents(53,QuestLoot.crate(utilities,"preview-drop"));
            world.setBlock(storageX+2,storageY,storageZ,QuestBlocks.devices[9]); QuestDevice detector=(QuestDevice)world.getTileEntity(storageX+2,storageY,storageZ); detector.owner=id; detector.quest=meter.id; detector.task=power.id; detector.analog=true; detector.bindingChanged(); main.creditTask(player,meter.id,power.id,50);
            for(int i=0;i<20;i++) detector.updateEntity(); if(detector.redstoneOutput!=8) throw new IllegalStateException("Progress detector did not emit analog half-progress");
            main.creditTask(player,meter.id,power.id,50); main.finish(player,meter); detector.analog=false;
            for(int i=0;i<20;i++) detector.updateEntity(); if(detector.redstoneOutput!=15) throw new IllegalStateException("Progress detector did not emit completion signal"); world.setBlockToAir(storageX+2,storageY,storageZ);
            main.emergency(player); try { main.emergency(player); throw new IllegalStateException("Emergency cooldown bypassed"); } catch(IllegalArgumentException expected) {}
            net.minecraft.entity.passive.EntityPig pig=new net.minecraft.entity.passive.EntityPig(player.worldObj); pig.setPosition(player.posX+10,player.posY,player.posZ+10); player.worldObj.spawnEntityInWorld(pig);
            pig.attackEntityFrom(net.minecraft.util.DamageSource.causePlayerDamage(player),1000);
            if(!observer.seen) throw new IllegalStateException("Actual creature death did not drop its configured crate");
        } finally { main.book=previous; main.quests=previousQuests; net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(observer); }
        System.out.println("Quest native PASS: real JavaScript task/reward, personal stages/skills, independent pack modes and claims, emergency cooldown, real creature crate drop, automatic quest chest, crate storage and analog detector");
    }
    public static final class DropObserver {
        boolean seen;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void dropped(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
            for(net.minecraft.entity.item.EntityItem entity:event.drops) { net.minecraft.item.ItemStack stack=entity.getEntityItem(); if(stack!=null && stack.hasTagCompound() && stack.getTagCompound().getString("table").equals("preview-drop")) seen=true; }
        }
    }
    public static void verify(EntityPlayerMP player) throws Exception {
        QuestService main=QuestsMod.service; UUID id=player.getUniqueID();
        net.minecraft.tileentity.TileEntity raw=player.worldObj.getTileEntity(storageX,storageY,storageZ);
        if(!(raw instanceof QuestStorage) || ((QuestStorage)raw).getSizeInventory()!=54 || ((QuestStorage)raw).getStackInSlot(53)==null || !((QuestStorage)raw).getStackInSlot(53).getTagCompound().getString("table").equals("preview-drop")) throw new IllegalStateException("Crate storage did not survive world restart");
        if(!main.progress().profile().equals(original) || !main.progress().stage(id,"preview:stage") || main.progress().skill(id,"preview:skill")!=12) throw new IllegalStateException("Native markers or selected pack mode did not survive restart");
        if(main.emergencyWait(player)<=0) throw new IllegalStateException("Emergency cooldown did not survive restart");
        main.switchProfile(profile);
        if(!main.progress().completed(id).contains("mode-marker") || !main.progress().rewardClaimed(id,"mode-marker","root",true)) throw new IllegalStateException("Alternate mode progress did not survive restart");
        main.switchProfile(original);
    }
}
