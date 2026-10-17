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
    private static int collectorX,collectorY,collectorZ,collectorStored,offlineCollectorX,offlineCollectorStored,openerX,openerY,openerZ,repeatX,repeatY,repeatZ;
    public static String offlineOwnerId() { return offlineOwner.toString(); }
    public static int[] openerPosition() { return new int[]{openerX,openerY,openerZ}; }
    public static int[] collectorPosition() { return new int[]{collectorX,collectorY,collectorZ}; }
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
            "quests.reward('preview:native',function(player,reward){quests.setStage(player,'preview:stage',true);quests.setSkill(player,'preview:skill',12);});"+
            "quests.on('task_clicked',function(event){if(String(event.quest)==='smoke-manual' && String(event.task)==='callback') quests.creditId(String(event.player),String(event.quest),String(event.task),3);});");
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
        QuestCore.Quest publicDetector=new QuestCore.Quest(); publicDetector.id="public-detector"; publicDetector.title="Public player detector"; publicDetector.optional=true;
        QuestCore.Task detected=new QuestCore.Task(); detected.id="presence"; detected.type="custom"; detected.count=5; publicDetector.tasks.add(detected); utilities.quests.add(publicDetector);
        QuestCore.Quest redstoneDetector=new QuestCore.Quest(); redstoneDetector.id="redstone-detector"; redstoneDetector.title="Redstone threshold"; redstoneDetector.optional=true;
        QuestCore.Task powered=new QuestCore.Task(); powered.id="powered"; powered.type="custom"; powered.count=3; redstoneDetector.tasks.add(powered); utilities.quests.add(redstoneDetector);
        QuestCore.Quest collectorQuest=new QuestCore.Quest(); collectorQuest.id="collector-machine"; collectorQuest.title="Selected reward collector"; collectorQuest.optional=true;
        QuestCore.Task collectorCheck=new QuestCore.Task(); collectorCheck.id="check"; collectorCheck.type="check"; collectorQuest.tasks.add(collectorCheck);
        QuestCore.Reward collectorReward=new QuestCore.Reward(); collectorReward.id="collected"; collectorReward.item="minecraft:iron_ingot"; collectorReward.count=70; collectorReward.randomBonus=3; collectorQuest.rewards.add(collectorReward);
        QuestCore.Reward uncollected=new QuestCore.Reward(); uncollected.id="manual-xp"; uncollected.type="xp"; uncollected.count=7; collectorQuest.rewards.add(uncollected); utilities.quests.add(collectorQuest);
        QuestCore.Quest repeating=new QuestCore.Quest(); repeating.id="repeat-machine"; repeating.title="Automatic repeat machine"; repeating.repeatable=true; repeating.optional=true;
        QuestCore.Task repeatingInput=new QuestCore.Task(); repeatingInput.id="input"; repeatingInput.type="item"; repeatingInput.target="minecraft:iron_ingot"; repeatingInput.count=2; repeatingInput.consume=true; repeating.tasks.add(repeatingInput);
        QuestCore.Reward repeatingOutput=new QuestCore.Reward(); repeatingOutput.id="gold"; repeatingOutput.item="minecraft:gold_ingot"; repeating.rewards.add(repeatingOutput); utilities.quests.add(repeating);
        QuestCore.Quest automatic=new QuestCore.Quest(); automatic.id="automatic-reset"; automatic.title="Automatic reward reset"; automatic.repeatable=true; automatic.autoRepeat=true; automatic.optional=true;
        QuestCore.Task automaticInput=new QuestCore.Task(); automaticInput.id="input"; automaticInput.type="item"; automaticInput.target="minecraft:iron_ingot"; automaticInput.consume=true; automatic.tasks.add(automaticInput);
        QuestCore.Reward automaticReward=new QuestCore.Reward(); automaticReward.id="xp"; automaticReward.type="xp"; automaticReward.count=1; automatic.rewards.add(automaticReward); utilities.quests.add(automatic);
        Map<String,QuestCore.Quest> previousQuests=main.quests; Map<String,QuestCore.Quest> utilityQuests=new LinkedHashMap<String,QuestCore.Quest>(); utilityQuests.put(chestQuest.id,chestQuest); utilityQuests.put(meter.id,meter); utilityQuests.put(offline.id,offline); utilityQuests.put(publicDetector.id,publicDetector); utilityQuests.put(redstoneDetector.id,redstoneDetector); utilityQuests.put(collectorQuest.id,collectorQuest); utilityQuests.put(repeating.id,repeating); utilityQuests.put(automatic.id,automatic); main.quests=utilityQuests;
        DropObserver observer=new DropObserver(); net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(observer); main.book=utilities;
        try {
            storageX=(int)Math.floor(player.posX)+4; storageY=(int)Math.floor(player.posY); storageZ=(int)Math.floor(player.posZ)+4;
            net.minecraft.world.World world=player.worldObj;
            int protectedX=(int)Math.floor(player.posX)+12,protectedY=(int)Math.floor(player.posY),protectedZ=(int)Math.floor(player.posZ)-12;
            world.setBlock(protectedX,protectedY,protectedZ,QuestBlocks.devices[0]); QuestDevice protectedScreen=(QuestDevice)world.getTileEntity(protectedX,protectedY,protectedZ); protectedScreen.owner=id; protectedScreen.bindingChanged(); QuestScreenSettings.appearance(protectedScreen,player,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.wool,1,14),false); QuestScreenSettings.protection(protectedScreen,player,true);
            world.newExplosion(null,protectedX+.5,protectedY+.5,protectedZ+.5,2,false,true); if(world.getTileEntity(protectedX,protectedY,protectedZ)!=protectedScreen) throw new IllegalStateException("Protected screen was destroyed by a real explosion"); world.setBlockToAir(protectedX,protectedY,protectedZ);
            offlineOwner=UUID.randomUUID(); offlineX=(int)Math.floor(player.posX)+8; offlineY=(int)Math.floor(player.posY); offlineZ=(int)Math.floor(player.posZ)+2;
            world.setBlock(offlineX,offlineY,offlineZ,QuestBlocks.devices[0]); QuestDevice offlineMachine=(QuestDevice)world.getTileEntity(offlineX,offlineY,offlineZ); offlineMachine.owner=offlineOwner; offlineMachine.quest=offline.id; offlineMachine.task=offlineRf.id; offlineMachine.bindingChanged();
            if(offlineMachine.player()!=null || offlineMachine.receiveEnergy(net.minecraftforge.common.util.ForgeDirection.UP,100,false)!=100) throw new IllegalStateException("Offline RF terminal did not accept energy without an online owner");
            offlineMachine.task=offlineFluid.id; offlineMachine.bindingChanged(); if(offlineMachine.fill(net.minecraftforge.common.util.ForgeDirection.UP,new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER,250),true)!=250) throw new IllegalStateException("Offline fluid terminal did not accept its fluid");
            offlineMachine.task=offlineItems.id; offlineMachine.bindingChanged(); world.setBlock(offlineX,offlineY+1,offlineZ,net.minecraft.init.Blocks.hopper,0,3);
            net.minecraft.tileentity.TileEntityHopper feeder=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(offlineX,offlineY+1,offlineZ); feeder.setInventorySlotContents(0,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,2));
            for(int i=0;i<40;i++) { feeder.updateEntity(); offlineMachine.updateEntity(); }
            if(!main.progress().completed(offlineOwner).contains(offline.id) || main.progress().value(offlineOwner,offline.id,offlineItems.id)!=2 || offlineMachine.getStackInSlot(0)!=null || main.claimed(offlineOwner,offline)) throw new IllegalStateException("Offline hopper submission did not finish stored requirements or claimed player rewards early");
            if(main.progress().value(id,offline.id,offlineRf.id)!=0) throw new IllegalStateException("Offline machine changed the bystander's progress"); world.setBlockToAir(offlineX,offlineY+1,offlineZ);
            { int detectorX=(int)Math.floor(player.posX)+2,detectorY=(int)Math.floor(player.posY),detectorZ=(int)Math.floor(player.posZ);
            world.setBlock(detectorX,detectorY,detectorZ,QuestBlocks.devices[5]); QuestDevice detector=(QuestDevice)world.getTileEntity(detectorX,detectorY,detectorZ); detector.owner=offlineOwner; detector.quest=publicDetector.id; detector.task=detected.id; detector.configureDetector(3,-2,6,15); detector.bindingChanged();
            for(int i=0;i<20;i++) detector.updateEntity();
            if(!main.progress().completed(id).contains(publicDetector.id) || main.progress().value(offlineOwner,publicDetector.id,detected.id)!=0) throw new IllegalStateException("Real public player detector credited its builder instead of the detected player");
            world.setBlockToAir(detectorX,detectorY,detectorZ); world.setBlock(detectorX,detectorY,detectorZ,QuestBlocks.devices[4]); detector=(QuestDevice)world.getTileEntity(detectorX,detectorY,detectorZ); detector.owner=offlineOwner; detector.quest=redstoneDetector.id; detector.task=powered.id; detector.configureDetector(3,-2,6,15); detector.bindingChanged();
            for(int i=0;i<20;i++) detector.updateEntity(); if(main.progress().completed(offlineOwner).contains(redstoneDetector.id)) throw new IllegalStateException("Unpowered redstone detector completed");
            world.setBlock(detectorX,detectorY+1,detectorZ,net.minecraft.init.Blocks.redstone_block); for(int i=0;i<20;i++) detector.updateEntity();
            if(!main.progress().completed(offlineOwner).contains(redstoneDetector.id)) throw new IllegalStateException("Real redstone threshold did not complete its offline owner's task");
            world.setBlockToAir(detectorX,detectorY+1,detectorZ); world.setBlockToAir(detectorX,detectorY,detectorZ); }
            collectorX=(int)Math.floor(player.posX)-3; collectorY=(int)Math.floor(player.posY); collectorZ=(int)Math.floor(player.posZ)+2;
            world.setBlock(collectorX,collectorY,collectorZ,QuestBlocks.devices[3]); QuestDevice collector=(QuestDevice)world.getTileEntity(collectorX,collectorY,collectorZ); collector.owner=id; collector.quest=collectorQuest.id; collector.task=collectorReward.id; collector.bindingChanged(); main.finish(player,collectorQuest);
            for(int slot=0;slot<9;slot++) collector.setInventorySlotContents(slot,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone,64));
            for(int i=0;i<20;i++) collector.updateEntity(); if(main.progress().rewardClaimed(id,collectorQuest.id,collectorReward.id,false)) throw new IllegalStateException("Full collector consumed a reward");
            for(int slot=0;slot<9;slot++) collector.setInventorySlotContents(slot,null); for(int i=0;i<20;i++) collector.updateEntity();
            int collected=0; for(int slot=0;slot<9;slot++) if(collector.getStackInSlot(slot)!=null) collected+=collector.getStackInSlot(slot).stackSize;
            if(collected<70 || collected>73 || !main.progress().rewardClaimed(id,collectorQuest.id,collectorReward.id,false) || main.claimed(id,collectorQuest)) throw new IllegalStateException("Selected collector reward or partial claim ledger failed");
            world.setBlock(collectorX,collectorY-1,collectorZ,net.minecraft.init.Blocks.hopper); net.minecraft.tileentity.TileEntityHopper collectorHopper=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(collectorX,collectorY-1,collectorZ);
            for(int i=0;i<16;i++) collectorHopper.updateEntity(); int extracted=0; for(int slot=0;slot<5;slot++) if(collectorHopper.getStackInSlot(slot)!=null) extracted+=collectorHopper.getStackInSlot(slot).stackSize;
            collectorStored=0; for(int slot=0;slot<9;slot++) if(collector.getStackInSlot(slot)!=null) collectorStored+=collector.getStackInSlot(slot).stackSize;
            if(extracted<=0 || collectorStored+extracted!=collected) throw new IllegalStateException("Real hopper lost collector outputs");
            for(int slot=0;slot<5;slot++) collectorHopper.setInventorySlotContents(slot,null); world.setBlockToAir(collectorX,collectorY-1,collectorZ);
            offlineCollectorX=collectorX-2; world.setBlock(offlineCollectorX,collectorY,collectorZ,QuestBlocks.devices[3]); QuestDevice offlineCollector=(QuestDevice)world.getTileEntity(offlineCollectorX,collectorY,collectorZ); offlineCollector.owner=offlineOwner; offlineCollector.quest=collectorQuest.id; offlineCollector.task=collectorReward.id; offlineCollector.bindingChanged(); main.finishStored(offlineOwner,collectorQuest,world.getTotalWorldTime());
            for(int i=0;i<20;i++) offlineCollector.updateEntity(); offlineCollectorStored=0; for(int slot=0;slot<9;slot++) if(offlineCollector.getStackInSlot(slot)!=null) offlineCollectorStored+=offlineCollector.getStackInSlot(slot).stackSize;
            if(offlineCollectorStored<70 || offlineCollectorStored>73 || !main.progress().rewardClaimed(offlineOwner,collectorQuest.id,collectorReward.id,false) || main.claimed(offlineOwner,collectorQuest)) throw new IllegalStateException("Offline collector did not preserve selected item and remaining live rewards");
            openerX=(int)Math.floor(player.posX)-3; openerY=(int)Math.floor(player.posY); openerZ=(int)Math.floor(player.posZ)-2;
            world.setBlock(openerX,openerY,openerZ,QuestBlocks.devices[6]); QuestDevice opener=(QuestDevice)world.getTileEntity(openerX,openerY,openerZ); opener.owner=offlineOwner; opener.bindingChanged(); for(int slot=1;slot<=9;slot++) opener.setInventorySlotContents(slot,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone,64));
            world.setBlock(openerX,openerY+1,openerZ,net.minecraft.init.Blocks.hopper); net.minecraft.tileentity.TileEntityHopper crateFeeder=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(openerX,openerY+1,openerZ); crateFeeder.setInventorySlotContents(0,QuestLoot.crate(utilities,"preview-drop"));
            for(int i=0;i<40;i++) { crateFeeder.updateEntity(); opener.updateEntity(); }
            if(opener.queuedLoot!=2 || opener.getStackInSlot(0)!=null || crateFeeder.getStackInSlot(0)!=null) throw new IllegalStateException("Offline crate opener did not persist exactly one roll behind full outputs"); world.setBlockToAir(openerX,openerY+1,openerZ);
            int brokenOpenerX=openerX+12; world.setBlock(brokenOpenerX,openerY,openerZ,QuestBlocks.devices[6]); QuestDevice brokenOpener=(QuestDevice)world.getTileEntity(brokenOpenerX,openerY,openerZ); brokenOpener.owner=offlineOwner; brokenOpener.bindingChanged(); for(int slot=1;slot<=9;slot++) brokenOpener.setInventorySlotContents(slot,new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone,64)); brokenOpener.setInventorySlotContents(0,QuestLoot.crate(utilities,"preview-drop")); for(int i=0;i<20;i++) brokenOpener.updateEntity(); if(brokenOpener.queuedLoot!=2) throw new IllegalStateException("Pending break fixture did not roll"); world.setBlockToAir(brokenOpenerX,openerY,openerZ);
            int pendingDrops=0,bufferDrops=0; for(Object raw:world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,net.minecraft.util.AxisAlignedBB.getBoundingBox(brokenOpenerX-1,openerY-1,openerZ-1,brokenOpenerX+2,openerY+2,openerZ+2))) { net.minecraft.entity.item.EntityItem entity=(net.minecraft.entity.item.EntityItem)raw; if(entity.getEntityItem().getItem()==net.minecraft.init.Items.stick) pendingDrops+=entity.getEntityItem().stackSize; if(entity.getEntityItem().getItem()==net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.cobblestone)) bufferDrops+=entity.getEntityItem().stackSize; entity.setDead(); }
            if(pendingDrops!=2 || bufferDrops!=576) throw new IllegalStateException("Breaking a blocked opener lost or duplicated rolled loot");
            player.inventory.setInventorySlotContents(5,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,1)); if(!main.finish(player,automatic)) throw new IllegalStateException("Automatic repeat input was not consumed"); main.claim(player,automatic,Collections.<Integer>emptyList());
            if(main.progress().cycle(id,automatic.id)!=1 || main.progress().completed(id).contains(automatic.id) || player.inventory.getStackInSlot(5)!=null) throw new IllegalStateException("Manual reward claim did not automatically reset its configured quest");
            repeatX=(int)Math.floor(player.posX)-7; repeatY=(int)Math.floor(player.posY); repeatZ=(int)Math.floor(player.posZ)+1;
            world.setBlock(repeatX,repeatY,repeatZ,QuestBlocks.devices[7]); QuestStorage repeatChest=(QuestStorage)world.getTileEntity(repeatX,repeatY,repeatZ); repeatChest.owner=offlineOwner; repeatChest.quest=repeating.id; repeatChest.bindingChanged();
            world.setBlock(repeatX+1,repeatY,repeatZ,QuestBlocks.devices[3]); QuestDevice repeatCollector=(QuestDevice)world.getTileEntity(repeatX+1,repeatY,repeatZ); repeatCollector.owner=offlineOwner; repeatCollector.quest=repeating.id; repeatCollector.task=repeatingOutput.id; repeatCollector.bindingChanged();
            world.setBlock(repeatX,repeatY+1,repeatZ,net.minecraft.init.Blocks.hopper); net.minecraft.tileentity.TileEntityHopper repeatFeeder=(net.minecraft.tileentity.TileEntityHopper)world.getTileEntity(repeatX,repeatY+1,repeatZ); repeatFeeder.setInventorySlotContents(0,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_ingot,4));
            for(int i=0;i<100;i++) { repeatFeeder.updateEntity(); repeatChest.updateEntity(); repeatCollector.updateEntity(); }
            if(main.progress().cycle(offlineOwner,repeating.id)!=2 || repeatCollector.getStackInSlot(0)==null || repeatCollector.getStackInSlot(0).stackSize!=2 || repeatCollector.getStackInSlot(0).getItem()!=net.minecraft.init.Items.gold_ingot || repeatFeeder.getStackInSlot(0)!=null || main.progress().completed(offlineOwner).contains(repeating.id)) throw new IllegalStateException("Offline hopper/chest/collector did not complete exactly two automatic repeat cycles"); world.setBlockToAir(repeatX,repeatY+1,repeatZ);


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
        System.out.println("Quest native PASS: real JavaScript task/reward, personal stages/skills, independent pack modes and claims, emergency cooldown, real creature crate drop, automatic quest chest, offline RF/fluid/hopper submission and restart, offline collectors, two automated repeat cycles, persisted crate-opener roll and full/broken outputs, virtual crate storage beyond 32-bit counts, actual hopper extraction, sealed break drops and analog detector");
    }
    public static final class DropObserver {
        boolean seen;
        @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void dropped(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
            for(net.minecraft.entity.item.EntityItem entity:event.drops) { net.minecraft.item.ItemStack stack=entity.getEntityItem(); if(stack!=null && stack.hasTagCompound() && stack.getTagCompound().getString("table").equals("preview-drop")) seen=true; }
        }
    }
    public static void verify(EntityPlayerMP player) throws Exception {
        QuestService main=QuestsMod.service; UUID id=player.getUniqueID();
        QuestDevice collector=(QuestDevice)player.worldObj.getTileEntity(collectorX,collectorY,collectorZ); int savedCollected=0;
        for(int slot=0;slot<9;slot++) if(collector.getStackInSlot(slot)!=null) savedCollected+=collector.getStackInSlot(slot).stackSize;
        if(savedCollected!=collectorStored || !main.progress().rewardClaimed(player.getUniqueID(),"collector-machine","collected",false)) throw new IllegalStateException("Collector inventory or selected claim did not survive restart");
        QuestDevice offlineCollector=(QuestDevice)player.worldObj.getTileEntity(offlineCollectorX,collectorY,collectorZ); int savedOfflineItems=0; for(int slot=0;slot<9;slot++) if(offlineCollector.getStackInSlot(slot)!=null) savedOfflineItems+=offlineCollector.getStackInSlot(slot).stackSize;
        if(savedOfflineItems!=offlineCollectorStored || !offlineOwner.equals(offlineCollector.owner)) throw new IllegalStateException("Offline collector outputs did not survive restart");
        QuestDevice opener=(QuestDevice)player.worldObj.getTileEntity(openerX,openerY,openerZ); if(opener.queuedLoot!=2 || opener.getStackInSlot(0)!=null || !offlineOwner.equals(opener.owner)) throw new IllegalStateException("Opener pending roll did not survive restart");
        for(int slot=1;slot<=9;slot++) opener.setInventorySlotContents(slot,null); for(int i=0;i<20;i++) opener.updateEntity(); if(opener.queuedLoot!=0) throw new IllegalStateException("Persisted loot did not drain after freeing outputs");
        player.worldObj.setBlock(openerX,openerY-1,openerZ,net.minecraft.init.Blocks.hopper); net.minecraft.tileentity.TileEntityHopper lootHopper=(net.minecraft.tileentity.TileEntityHopper)player.worldObj.getTileEntity(openerX,openerY-1,openerZ); for(int i=0;i<16;i++) lootHopper.updateEntity(); int lootTotal=0; for(int slot=1;slot<=9;slot++) if(opener.getStackInSlot(slot)!=null) lootTotal+=opener.getStackInSlot(slot).stackSize; int extractedLoot=0; for(int slot=0;slot<5;slot++) if(lootHopper.getStackInSlot(slot)!=null) { extractedLoot+=lootHopper.getStackInSlot(slot).stackSize; lootHopper.setInventorySlotContents(slot,null); }
        if(extractedLoot<1 || lootTotal+extractedLoot!=2) throw new IllegalStateException("Actual hopper lost persisted opener loot"); player.worldObj.setBlockToAir(openerX,openerY-1,openerZ);
        if(main.progress().cycle(player.getUniqueID(),"automatic-reset")!=1 || main.progress().completed(player.getUniqueID()).contains("automatic-reset")) throw new IllegalStateException("Automatic reward reset did not survive restart");
        QuestDevice repeatCollector=(QuestDevice)player.worldObj.getTileEntity(repeatX+1,repeatY,repeatZ);
        if(main.progress().cycle(offlineOwner,"repeat-machine")!=2 || repeatCollector.getStackInSlot(0)==null || repeatCollector.getStackInSlot(0).stackSize!=2 || main.progress().value(offlineOwner,"repeat-machine","input")!=0) throw new IllegalStateException("Automatic repeat generation or output did not survive restart");
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
