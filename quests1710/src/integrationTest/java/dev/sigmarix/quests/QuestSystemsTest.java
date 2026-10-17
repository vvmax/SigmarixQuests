package dev.sigmarix.quests;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import net.minecraft.init.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QuestSystemsTest {
    private EntityPlayerMP player;
    private QuestService service;
    private QuestProgress data;
    private QuestCore.Quest quest;
    private static QuestBlocks.LootCrateItem bankCrate;
    @BeforeClass public static void boot() { Bootstrap.func_151354_b(); bankCrate=new QuestBlocks.LootCrateItem(); cpw.mods.fml.common.registry.GameRegistry.registerItem(bankCrate,"test-bank-crate"); cpw.mods.fml.common.registry.GameRegistry.registerItem(QuestBlocks.crateParcel,"crate_parcel"); cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(QuestDevice.class,"sigmarixquests:test-device-base"); cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(QuestStorage.class,"sigmarixquests:test-storage"); }
    @Before public void setup() throws Exception {
        player=mock(EntityPlayerMP.class);
        when(player.getUniqueID()).thenReturn(UUID.randomUUID());
        when(player.getRNG()).thenReturn(new Random(1));
        player.inventory=new InventoryPlayer(player);
        player.inventoryContainer=mock(Container.class);
        player.worldObj=mock(World.class); java.lang.reflect.Field randomField=World.class.getDeclaredField("rand"); randomField.setAccessible(true); randomField.set(player.worldObj,new Random(1));
        when(player.worldObj.getTotalWorldTime()).thenReturn(100L);
        data=new QuestProgress(); service=spy(new QuestService(new File("unused.json")));
        doReturn(data).when(service).progress();
        quest=new QuestCore.Quest(); quest.id="test"; quest.title="Test";
        service.quests=new LinkedHashMap<String,QuestCore.Quest>(); service.quests.put(quest.id,quest);
    }
    private QuestCore.Task task(String type,String target,long count,boolean consume) {
        QuestCore.Task t=new QuestCore.Task(); t.id="t"+quest.tasks.size(); t.type=type; t.target=target; t.count=count; t.consume=consume; quest.tasks.add(t); return t;
    }
    @Test public void onlyTaskTerminalsAcceptResourceInputs() {
        QuestCore.Task resource=task("energy",null,100,false); QuestDevice tile=terminalTile(); doReturn(new QuestBlocks.DeviceBlock(3,"collector_resources")).when(tile).getBlockType();
        try {
            assertFalse(tile.canConnectEnergy(ForgeDirection.UP)); assertEquals(0,tile.receiveEnergy(ForgeDirection.UP,100,false));
            resource.type="fluid"; resource.target="water"; assertEquals(0,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.WATER,100),true)); assertEquals(0,data.value(tile.owner,quest.id,resource.id));
        } finally { QuestsMod.service=null; }
    }
    @Test public void offlineCollectorPreservesRootClaimsAndWaitsForLiveOnlyOneChecks() {
        UUID owner=UUID.randomUUID(); task("check",null,1,false); QuestCore.Reward reward=new QuestCore.Reward(); reward.id="items"; reward.item="minecraft:iron_ingot"; reward.count=3; reward.mode="individual"; quest.rewards.add(reward); QuestCore.Reward live=new QuestCore.Reward(); live.id="live"; live.item="minecraft:diamond"; live.onlyOne=true; quest.rewards.add(live); data.complete(owner,quest.id);
        List<ItemStack> output=new ArrayList<ItemStack>(); assertFalse(service.collectStored(owner,quest,"items",new Random(1),items->false,output::add)); assertFalse(data.rewardClaimed(owner,quest.id,"items",true));
        assertTrue(service.collectStored(owner,quest,"items",new Random(1),items->true,output::add)); assertEquals(3,output.get(0).stackSize); assertTrue(data.rewardClaimed(owner,quest.id,"items",true)); assertFalse(data.rewardClaimed(player.getUniqueID(),quest.id,"items",true)); assertFalse(service.claimed(owner,quest));
        assertFalse(service.collectStored(owner,quest,"live",new Random(1),items->true,output::add)); assertFalse(service.collectStored(owner,quest,"items",new Random(1),items->true,output::add)); assertEquals(1,output.size());
    }
    @Test public void automatedLootWaitsForPlayerBeforeRollingLiveEffects() {
        QuestCore.Reward item=new QuestCore.Reward(); item.item="minecraft:iron_ingot"; item.count=3; QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=5; service.book.lootTables.put("mixed",Arrays.asList(item,xp));
        ItemStack crate=new ItemStack(bankCrate,2); Random random=mock(Random.class); List<ItemStack> output=new ArrayList<ItemStack>();
        assertFalse(service.openLootAutomated(player.getUniqueID(),null,"mixed",crate,random,output::add)); verifyNoInteractions(random); assertEquals(2,crate.stackSize);
        when(random.nextInt(anyInt())).thenReturn(0); assertTrue(service.openLootAutomated(player.getUniqueID(),player,"mixed",crate,random,output::add)); assertEquals(1,crate.stackSize); assertEquals(3,output.get(0).stackSize); assertNull(player.inventory.mainInventory[0]);
        service.book.lootTables.put("xp",Collections.singletonList(xp)); assertTrue(service.openLootAutomated(player.getUniqueID(),player,"xp",crate,random,output::add)); verify(player).addExperience(5); assertEquals(0,crate.stackSize);
    }
    @Test public void oversizedAutomatedLootFailsBeforeConsumingItsCrate() {
        QuestCore.Reward item=new QuestCore.Reward(); item.item="minecraft:iron_ingot"; item.count=4096; service.book.lootTables.put("large",Collections.singletonList(item)); QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.rolls=9; service.book.lootSettings.put("large",settings); ItemStack crate=new ItemStack(bankCrate,1);
        try { service.openLootAutomated(player.getUniqueID(),null,"large",crate,new Random(1),stack->{fail();}); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(1,crate.stackSize);
    }
    @Test public void openerQueuesOneRollAcrossFullOutputsAndNbtReload() {
        QuestCore.Reward item=new QuestCore.Reward(); item.item="minecraft:iron_ingot"; item.count=4; item.nbt="{quality:7}"; service.book.lootTables.put("iron",Collections.singletonList(item));
        when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(6,"offline_opener")); QuestDevice tile=new QuestDevice(); tile.setWorldObj(player.worldObj); tile.owner=UUID.randomUUID(); QuestsMod.service=service;
        try {
            for(int slot=1;slot<=9;slot++) tile.setInventorySlotContents(slot,new ItemStack(Items.gold_ingot,64)); ItemStack crate=new ItemStack(bankCrate,2); crate.setTagCompound(QuestService.tag("{table:iron}")); tile.setInventorySlotContents(0,crate);
            assertEquals(10,tile.getSizeInventory()); assertTrue(tile.canInsertItem(0,crate,0)); assertFalse(tile.canExtractItem(0,crate,0)); assertTrue(tile.canExtractItem(1,tile.getStackInSlot(1),0));
            for(int i=0;i<40;i++) tile.updateEntity(); assertEquals(1,tile.getStackInSlot(0).stackSize); assertEquals(4,tile.queuedLoot);
            NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved); QuestDevice restored=new QuestDevice(); restored.setWorldObj(player.worldObj); restored.readFromNBT(saved);
            net.minecraft.network.play.server.S35PacketUpdateTileEntity description=(net.minecraft.network.play.server.S35PacketUpdateTileEntity)tile.getDescriptionPacket(); assertFalse(description.func_148857_g().hasKey("pendingLoot")); restored.onDataPacket(null,description); assertEquals(4,restored.queuedLoot);
            for(int slot=1;slot<=9;slot++) restored.setInventorySlotContents(slot,null); service.book.lootTables.clear(); for(int i=0;i<20;i++) restored.updateEntity();
            assertEquals(1,restored.getStackInSlot(0).stackSize); assertEquals(0,restored.queuedLoot); assertEquals(4,restored.getStackInSlot(1).stackSize); assertEquals(7,restored.getStackInSlot(1).getTagCompound().getInteger("quality")); assertTrue(restored.drainPendingLoot().isEmpty());
        } finally { QuestsMod.service=null; }
    }
    @Test public void partialRewardBufferInsertionConservesPendingItems() {
        ItemStack[] outputs={new ItemStack(Items.iron_ingot,62)}; ItemStack pending=new ItemStack(Items.iron_ingot,7);
        assertEquals(2,QuestRewardBuffer.offer(outputs,pending)); assertEquals(64,outputs[0].stackSize); assertEquals(7,pending.stackSize); assertEquals(0,QuestRewardBuffer.offer(outputs,pending));
    }
    @Test public void customTaskButtonsRequireServerCallbacksAndCannotReplayCompletedActions() {
        QuestCore.Task custom=task("custom","",3,false); custom.manual=true; service.confirm(player,quest,custom.id);
        assertEquals(0,data.value(player.getUniqueID(),quest.id,custom.id)); assertFalse(data.taskDone(player.getUniqueID(),quest.id,custom.id));
        final int[] clicks={0}; java.util.function.Consumer<QuestEvents.Event> listener=event->{ assertEquals(player.getUniqueID(),event.player); clicks[0]++; service.creditTask(event.player,event.quest,event.task,3); };
        QuestEvents.on("task_clicked",listener);
        try {
            service.confirm(player,quest,custom.id); assertEquals(3,data.value(player.getUniqueID(),quest.id,custom.id)); assertTrue(data.taskDone(player.getUniqueID(),quest.id,custom.id));
            try { service.confirm(player,quest,custom.id); fail(); } catch(IllegalArgumentException expected) {}
            QuestCore.Task disabled=task("custom","",1,false); try { service.confirm(player,quest,disabled.id); fail(); } catch(IllegalArgumentException expected) {}
            assertEquals(1,clicks[0]);
        } finally { QuestEvents.off("task_clicked",listener); }
    }
    @Test public void worldMapUsesOfflineOwnerProgressWithoutRevealingHiddenViewerGoals() throws Exception {
        QuestCore.Task energy=task("energy",null,100,false); QuestCore.Chapter group=new QuestCore.Chapter(); group.id="group"; group.title="Group"; QuestCore.Chapter child=new QuestCore.Chapter(); child.id="child"; child.title="Child"; child.parent=group.id;
        service.book.chapters.add(group); service.book.chapters.add(child); service.book.quests.add(quest); quest.chapter=child.id;
        QuestCore.Quest hidden=new QuestCore.Quest(); hidden.id="secret"; hidden.title="Secret"; hidden.chapter=child.id; hidden.hidden=true; hidden.dependencies.add(quest.id); service.book.quests.add(hidden); service.quests.put(hidden.id,hidden);
        net.minecraft.world.WorldProvider provider=mock(net.minecraft.world.WorldProvider.class); provider.dimensionId=7; java.lang.reflect.Field providerField=World.class.getDeclaredField("provider"); providerField.setAccessible(true); providerField.set(player.worldObj,provider);
        QuestDevice tile=new QuestDevice(); tile.setWorldObj(player.worldObj); tile.owner=UUID.randomUUID(); tile.quest="chapter:group"; service.creditTask(tile.owner,quest.id,energy.id,50);
        QuestWorldMap map=QuestWorldMap.create(service,player.getUniqueID(),tile); map.validate(); assertTrue(map.matches(tile,service.revision,"default")); assertFalse(map.matches(tile,service.revision+1,"default")); assertFalse(map.matches(tile,service.revision,"expert")); UUID savedOwner=tile.owner; tile.owner=UUID.randomUUID(); assertFalse(map.matches(tile,service.revision,"default")); tile.owner=savedOwner; assertEquals(7,map.dimension); assertEquals(Integer.valueOf(151),map.values.get(quest.id)); assertFalse(map.values.containsKey(hidden.id));
        data.complete(tile.owner,quest.id); map=QuestWorldMap.create(service,player.getUniqueID(),tile); assertEquals(Integer.valueOf(302),map.values.get(quest.id)); assertFalse(map.values.containsKey(hidden.id));
        tile.bindingProfile="expert"; assertTrue(QuestWorldMap.create(service,player.getUniqueID(),tile).values.isEmpty());
    }
    @Test public void screenPresentationOptionsPersistWithoutChangingResourceBindings() {
        QuestDevice tile=new QuestDevice(); tile.owner=UUID.randomUUID(); tile.quest="quest"; tile.task="task"; tile.screenFullscreen=true; tile.screenHideIcons=true; tile.screenInputOnly=true; tile.screenInputIcon=new ItemStack(Items.diamond,1,0);
        NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved); QuestDevice restored=new QuestDevice(); restored.readFromNBT(saved);
        assertTrue(restored.screenFullscreen); assertTrue(restored.screenHideIcons); assertTrue(restored.screenInputOnly); assertEquals(Items.diamond,restored.screenInputIcon.getItem()); assertEquals(tile.owner,restored.owner); assertEquals("task",restored.task);
    }
    @Test public void collectorClaimsOnlySelectedRootAndLeavesChoiceAvailable() {
        task("check",null,1,false); QuestCore.Reward item=new QuestCore.Reward(); item.id="item"; item.item="minecraft:iron_ingot"; item.count=2; quest.rewards.add(item);
        QuestCore.Reward choice=new QuestCore.Reward(); choice.id="choice"; choice.type="choice"; QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=7; choice.options.add(xp); quest.rewards.add(choice);
        assertTrue(service.finish(player,quest)); List<ItemStack> received=new ArrayList<ItemStack>();
        assertTrue(service.collect(player,quest,"item",stacks->true,received::add)); assertEquals(2,received.get(0).stackSize); assertFalse(service.claimed(player.getUniqueID(),quest)); assertFalse(data.claimed(player.getUniqueID(),quest.id));
        assertFalse(service.collect(player,quest,"item",stacks->true,received::add));
        service.claim(player,quest,Collections.singletonList(0)); verify(player).addExperience(7); assertTrue(service.claimed(player.getUniqueID(),quest)); assertEquals(1,received.size());
    }
    @Test public void fullCollectorDoesNotClaimOrRollRandomBonus() {
        task("check",null,1,false); QuestCore.Reward item=new QuestCore.Reward(); item.id="item"; item.item="minecraft:iron_ingot"; item.count=2; item.randomBonus=3; quest.rewards.add(item); service.finish(player,quest);
        try { service.collect(player,quest,"item",stacks->{ assertEquals(5,stacks.get(0).stackSize); return false; },stack->{fail();}); fail(); } catch(IllegalArgumentException expected) {}
        assertFalse(data.rewardClaimed(player.getUniqueID(),quest.id,"item",false)); assertFalse(service.claimed(player.getUniqueID(),quest));
        List<ItemStack> received=new ArrayList<ItemStack>(); assertTrue(service.collect(player,quest,"",stacks->true,received::add)); assertTrue(received.get(0).stackSize>=2); assertTrue(service.claimed(player.getUniqueID(),quest));
    }
    @Test public void collectorBufferPreservesNbtAndRejectsOverflowAtomically() {
        ItemStack[] buffer=new ItemStack[2]; ItemStack iron=new ItemStack(Items.iron_ingot,64); iron.setTagCompound(QuestService.tag("{quality:1}"));
        assertTrue(QuestRewardBuffer.add(buffer,iron)); ItemStack other=iron.copy(); other.setTagCompound(QuestService.tag("{quality:2}")); assertTrue(QuestRewardBuffer.add(buffer,other));
        assertFalse(QuestRewardBuffer.add(buffer,new ItemStack(Items.gold_ingot,1))); assertEquals(64,buffer[0].stackSize); assertEquals(64,buffer[1].stackSize);
        assertFalse(QuestRewardBuffer.fits(buffer,Collections.singletonList(iron))); assertEquals(64,iron.stackSize);
    }
    @Test public void collectorInventoryPersistsAndDescriptionCannotEraseContainerContents() {
        QuestDevice tile=new QuestDevice(); tile.setWorldObj(player.worldObj); when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(3,"collector_nbt"));
        tile.setInventorySlotContents(8,new ItemStack(Items.iron_ingot,12)); assertEquals(9,tile.getSizeInventory()); assertTrue(tile.canExtractItem(8,tile.getStackInSlot(8),0)); assertFalse(tile.canInsertItem(8,new ItemStack(Items.iron_ingot),0));
        NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved); QuestDevice restored=new QuestDevice(); restored.setWorldObj(player.worldObj); restored.readFromNBT(saved); assertEquals(12,restored.getStackInSlot(8).stackSize);
        net.minecraft.network.play.server.S35PacketUpdateTileEntity description=(net.minecraft.network.play.server.S35PacketUpdateTileEntity)tile.getDescriptionPacket(); assertFalse(description.func_148857_g().hasKey("collectorItems")); restored.onDataPacket(null,description); assertEquals(12,restored.getStackInSlot(8).stackSize);
        assertEquals(5,restored.decrStackSize(8,5).stackSize); assertEquals(7,restored.getStackInSlot(8).stackSize);
    }
    @Test public void customCountersCanBeSavedAndRemainIncompleteUntilCredited() throws Exception {
        QuestCore.Task counter=task("custom","",3,false); QuestCore.Book book=new QuestCore.Book(); book.quests.add(quest);
        java.nio.file.Path folder=java.nio.file.Files.createTempDirectory("quests-counter-test");
        try {
            QuestService configured=new QuestService(folder.resolve("quests.json").toFile()); configured.save(book,configured.revision);
            assertFalse(service.check(player,quest,false)); service.creditTask(player,quest.id,counter.id,3); assertTrue(service.check(player,quest,false));
            counter.target="missing:hook"; try { configured.save(book,configured.revision); fail(); } catch(IllegalArgumentException expected) {}
        } finally { java.nio.file.Files.deleteIfExists(folder.resolve("quests.json")); java.nio.file.Files.deleteIfExists(folder.resolve("quests.json.bak")); java.nio.file.Files.deleteIfExists(folder); }
    }
    @Test public void detectorNotificationScopePreservesEventsAndRestoresNestedState() {
        java.util.List<Boolean> seen=new ArrayList<Boolean>(); java.util.function.Consumer<QuestEvents.Event> listener=e->seen.add(QuestEvents.notificationsMuted()); QuestEvents.on("task_completed",listener);
        try {
            QuestEvents.withNotifications(false,()-> { QuestEvents.withNotifications(true,()->QuestEvents.fire("task_completed",player.getUniqueID(),quest.id,"task")); });
            assertEquals(Collections.singletonList(true),seen); assertFalse(QuestEvents.notificationsMuted());
            try { QuestEvents.withNotifications(false,()-> { throw new IllegalStateException("probe"); }); fail(); } catch(IllegalStateException expected) {}
            assertFalse(QuestEvents.notificationsMuted());
        } finally { QuestEvents.off("task_completed",listener); }
    }
    @Test public void detectorSettingsValidateAtomicallyAndSurviveNbt() {
        QuestDevice tile=new QuestDevice(); tile.xCoord=10; tile.yCoord=20; tile.zCoord=30;
        tile.configureDetector(3,-2,4,12);
        assertEquals(7.5,tile.detectorBounds().minX,0); assertEquals(18,tile.detectorBounds().minY,0); assertEquals(22,tile.detectorBounds().maxY,0);
        try { tile.configureDetector(4,0,Double.NaN,2); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(3,tile.detectorRadius,0); assertEquals(12,tile.detectorPower);
        NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved); QuestDevice restored=new QuestDevice(); restored.readFromNBT(saved);
        assertEquals(3,restored.detectorRadius,0); assertEquals(-2,restored.detectorOffsetY,0); assertEquals(4,restored.detectorHeight,0); assertEquals(12,restored.detectorPower);
        restored.readFromNBT(new NBTTagCompound()); assertTrue(Double.isFinite(restored.detectorRadius));
    }
    @Test public void redstoneDetectorUsesConfiguredThresholdAndOfflineOwner() {
        QuestCore.Task target=task("custom",null,9,false); QuestDevice tile=spy(new QuestDevice()); tile.owner=UUID.randomUUID(); tile.quest=quest.id; tile.task=target.id; tile.setWorldObj(player.worldObj);
        doReturn(new QuestBlocks.DeviceBlock(4,"threshold_detector")).when(tile).getBlockType(); doReturn(null).when(tile).player(); tile.configureDetector(1,0,1,12); QuestsMod.service=service;
        try {
            when(player.worldObj.getStrongestIndirectPower(0,0,0)).thenReturn(11); for(int i=0;i<20;i++) tile.updateEntity(); assertEquals(0,data.value(tile.owner,quest.id,target.id));
            when(player.worldObj.getStrongestIndirectPower(0,0,0)).thenReturn(12); for(int i=0;i<20;i++) tile.updateEntity(); assertEquals(9,data.value(tile.owner,quest.id,target.id)); assertTrue(data.completed(tile.owner).contains(quest.id));
        } finally { QuestsMod.service=null; }
    }
    @Test public void publicDetectorCreditsDetectedPlayersInsteadOfItsOfflineBuilder() {
        QuestCore.Task target=task("custom",null,7,false); QuestDevice tile=new QuestDevice(); tile.owner=UUID.randomUUID(); tile.quest=quest.id; tile.task=target.id; tile.setWorldObj(player.worldObj); QuestsMod.service=service;
        try {
            assertTrue(tile.detectPlayers(Collections.singletonList(player)));
            assertEquals(7,data.value(player.getUniqueID(),quest.id,target.id));
            assertEquals(0,data.value(tile.owner,quest.id,target.id));
            assertTrue(data.completed(player.getUniqueID()).contains(quest.id));
            assertFalse(tile.detectPlayers(Collections.emptyList()));
            tile.bindingProfile="another-profile";
            EntityPlayerMP other=mock(EntityPlayerMP.class); when(other.getUniqueID()).thenReturn(UUID.randomUUID());
            assertTrue(tile.detectPlayers(Collections.singletonList(other))); assertEquals(0,data.value(other.getUniqueID(),quest.id,target.id));
        } finally { QuestsMod.service=null; }
    }
    @Test public void incompleteTaskDoesNotConsumeEarlierRequirements() {
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);
        task("item","minecraft:iron_ingot",3,true); task("dimension",null,1,false).dimension=-1;
        assertFalse(service.check(player,quest,true));
        assertEquals(4,player.inventory.mainInventory[0].stackSize);
        assertTrue(data.completed(player.getUniqueID()).isEmpty());
    }
    @Test public void successfulConsumptionAndOneTimeClaim() {
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);
        task("item","minecraft:iron_ingot",3,true);
        QuestCore.Reward reward=new QuestCore.Reward(); reward.type="xp"; reward.count=5; quest.rewards.add(reward);
        assertTrue(service.check(player,quest,true)); assertEquals(1,player.inventory.mainInventory[0].stackSize);
        service.claim(player,quest,Collections.<Integer>emptyList()); verify(player).addExperience(5);
        try { service.claim(player,quest,Collections.<Integer>emptyList()); fail(); } catch(IllegalArgumentException expected) {}
        verify(player,times(1)).addExperience(5);
    }
    @Test public void readOnlyAndConsumingTasksCannotReuseSameItems() {
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);
        task("item","minecraft:iron_ingot",3,false); task("item","minecraft:iron_ingot",2,true);
        assertFalse(service.check(player,quest,true)); assertEquals(4,player.inventory.mainInventory[0].stackSize);
    }
    @Test public void nbtMismatchCannotCompleteItemTask() {
        ItemStack stack=new ItemStack(Items.iron_ingot,4); player.inventory.mainInventory[0]=stack;
        task("item","minecraft:iron_ingot",1,false).nbt="{quality:1}";
        assertFalse(service.check(player,quest,true));
        NBTTagCompound tag=new NBTTagCompound(); tag.setInteger("quality",1); stack.setTagCompound(tag);
        assertTrue(service.check(player,quest,false));
    }
    @Test public void itemCheckDoesNotRewriteExperience() {
        task("check",null,1,false); player.experienceLevel=7; player.experience=.313f; player.experienceTotal=500;
        assertTrue(service.check(player,quest,true)); assertEquals(.313f,player.experience,0); assertEquals(500,player.experienceTotal);
    }
    @Test public void insufficientXpDoesNotConsumeItems() {
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);
        task("item","minecraft:iron_ingot",3,true); task("xp",null,1,true);
        assertFalse(service.check(player,quest,true)); assertEquals(4,player.inventory.mainInventory[0].stackSize);
    }
    @Test public void fluidContainerConsumptionReturnsEmptyBucket() {
        player.inventory.mainInventory[0]=new ItemStack(Items.water_bucket);
        task("fluid","water",1000,true);
        assertTrue(service.check(player,quest,true));
        boolean empty=false; for(ItemStack s:player.inventory.mainInventory) if(s!=null && s.getItem()==Items.bucket) empty=true;
        assertTrue(empty);
    }
    @Test public void choicesAreValidatedBeforeClaimStateChanges() {
        task("check",null,1,false); service.check(player,quest,true);
        QuestCore.Reward choice=new QuestCore.Reward(); choice.type="choice";
        QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=7; choice.options.add(xp); quest.rewards.add(choice);
        try { service.claim(player,quest,Collections.singletonList(3)); fail(); } catch(IllegalArgumentException expected) {}
        assertFalse(data.claimed(player.getUniqueID(),quest.id));
        service.claim(player,quest,Collections.singletonList(0)); verify(player).addExperience(7);
    }
    @Test public void repeatHonorsCooldown() {
        task("check",null,1,false); quest.repeatable=true; quest.cooldownSeconds=10;
        service.check(player,quest,true); service.claim(player,quest,Collections.<Integer>emptyList());
        try { service.repeat(player,quest); fail(); } catch(IllegalArgumentException expected) {}
        when(player.worldObj.getTotalWorldTime()).thenReturn(301L);
        service.repeat(player,quest); assertFalse(data.completed(player.getUniqueID()).contains(quest.id));
    }
    @Test public void legacyKillCounterIsNotLostOnFirstNewKill() {
        task("kill","Zombie",4,false); data.kill(player.getUniqueID(),quest.id,0,4); data.kill(player.getUniqueID(),quest.id,0,4);
        service.credit(player,"kill","Zombie",1);
        assertEquals(3,service.counter(player,quest,quest.tasks.get(0)));
    }
    @Test public void rfSimulationDoesNotCreditAndRealAcceptanceIsBounded() {
        QuestCore.Task energy=task("energy",null,100,false);
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=energy.id; tile.setWorldObj(player.worldObj);
        doReturn(player).when(tile).player(); when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(0,"resource_test")); QuestsMod.service=service;
        try {
            assertEquals(100,tile.receiveEnergy(ForgeDirection.NORTH,200,true)); assertEquals(0,data.value(tile.owner,quest.id,energy.id));
            assertEquals(100,tile.receiveEnergy(ForgeDirection.NORTH,200,false)); assertEquals(100,data.value(tile.owner,quest.id,energy.id));
            assertEquals(0,tile.receiveEnergy(ForgeDirection.NORTH,10,false));
        } finally { QuestsMod.service=null; }
    }
    @Test public void fluidTerminalRejectsWrongFluidAndSimulationIsReadOnly() {
        QuestCore.Task fluid=task("fluid","water",1000,false);
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=fluid.id; tile.setWorldObj(player.worldObj);
        doReturn(player).when(tile).player(); when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(0,"resource_test")); QuestsMod.service=service;
        try {
            assertEquals(0,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.LAVA,1000),true));
            assertEquals(1000,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.WATER,2000),false));
            assertEquals(0,data.value(tile.owner,quest.id,fluid.id));
            assertEquals(1000,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.WATER,2000),true));
            assertEquals(1000,data.value(tile.owner,quest.id,fluid.id));
        } finally { QuestsMod.service=null; }
    }
    @Test public void xpLevelBoundariesRoundTrip() {
        for(int level:new int[]{0,1,14,15,16,29,30,31,100}) {
            int base=QuestService.XP.base(level);
            assertEquals(level,QuestService.XP.level(base));
            if(base>0) assertEquals(level-1,QuestService.XP.level(base-1));
            assertEquals(base+1,QuestService.XP.total(level,1f/QuestService.XP.cap(level)));
        }
    }

    @Test public void partialSubmissionSurvivesWorldDataRoundTrip() {
        QuestCore.Task t=task("item","minecraft:iron_ingot",8,true);
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,3);
        assertEquals(3,service.submit(player,quest,t.id)); assertNull(player.inventory.mainInventory[0]);
        assertFalse(service.check(player,quest,true));
        NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved); QuestProgress restored=new QuestProgress(); restored.readFromNBT(saved);
        doReturn(restored).when(service).progress();
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,5);
        assertTrue(service.check(player,quest,true)); assertNull(player.inventory.mainInventory[0]);
    }
    @Test public void orTasksConsumeOnlyTheSuccessfulBranch() {
        quest.anyTask=true; task("dimension",null,1,false).dimension=-1;
        task("item","minecraft:iron_ingot",2,true);
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,3);
        assertTrue(service.check(player,quest,true)); assertEquals(1,player.inventory.mainInventory[0].stackSize);
    }
    @Test public void itemTerminalSimulationIsReadOnlyAndBounded() {
        QuestCore.Task t=task("item","minecraft:iron_ingot",3,true); ItemStack input=new ItemStack(Items.iron_ingot,10);
        assertEquals(3,service.acceptItems(player,quest.id,t.id,input,true)); assertEquals(10,input.stackSize);
        assertEquals(3,service.acceptItems(player,quest.id,t.id,input,false)); assertEquals(7,input.stackSize);
        assertEquals(0,service.acceptItems(player,quest.id,t.id,input,false));
        assertTrue(service.check(player,quest,true));
    }
    @Test public void biomeLocationInteractionAndObservationUseServerState() {
        task("biome","Plains",1,false); task("location",null,1,false);
        task("interaction","minecraft:stone",1,false); QuestCore.Task observation=task("observation","minecraft:stone",1,false);
        when(player.worldObj.getBiomeGenForCoords(0,0)).thenReturn(net.minecraft.world.biome.BiomeGenBase.plains);
        when(player.rayTrace(8,1)).thenReturn(new net.minecraft.util.MovingObjectPosition(1,2,3,1,net.minecraft.util.Vec3.createVectorHelper(1,2,3)));
        when(player.worldObj.getBlock(1,2,3)).thenReturn(Blocks.stone);
        assertFalse(service.check(player,quest,false)); service.credit(player,"interaction","minecraft:stone",1);
        assertTrue(service.check(player,quest,false));
    }
    @Test public void statisticsAndAchievementRequirementsAreNotClientControlled() {
        task("stat","stat.jump",4,false); task("achievement","achievement.openInventory",1,false);
        net.minecraft.stats.StatisticsFile stats=mock(net.minecraft.stats.StatisticsFile.class); when(player.func_147099_x()).thenReturn(stats);
        when(stats.writeStat(net.minecraft.stats.StatList.jumpStat)).thenReturn(4);
        assertFalse(service.check(player,quest,false));
        when(stats.hasAchievementUnlocked(net.minecraft.stats.AchievementList.openInventory)).thenReturn(true);
        assertTrue(service.check(player,quest,false));
    }
    @Test public void independentChoicesGrantOnlySelectedLeaves() {
        task("check",null,1,false); service.check(player,quest,true);
        QuestCore.Reward choice=new QuestCore.Reward(); choice.type="choice";
        QuestCore.Reward a=new QuestCore.Reward(); a.type="xp"; a.count=3;
        QuestCore.Reward b=new QuestCore.Reward(); b.type="xp"; b.count=7; choice.options.add(a); choice.options.add(b);
        quest.rewards.add(choice); quest.rewards.add(choice);
        service.claim(player,quest,Arrays.asList(0,1)); verify(player).addExperience(3); verify(player).addExperience(7);
    }

    @Test public void detectedDimensionVisitPersistsAfterLeaving() {
        task("dimension",null,1,false).dimension=-1;
        player.dimension=-1; service.detect(player); player.dimension=0;
        assertTrue(service.check(player,quest,false));
    }
    @Test public void collectorRoutesItemRewardsThroughSink() {
        task("check",null,1,false); service.check(player,quest,true);
        QuestCore.Reward r=new QuestCore.Reward(); r.item="minecraft:iron_ingot"; r.count=3; quest.rewards.add(r);
        final List<ItemStack> output=new ArrayList<ItemStack>();
        service.claim(player,quest,Collections.<Integer>emptyList(),output::add);
        assertEquals(1,output.size()); assertEquals(3,output.get(0).stackSize);
        for(ItemStack stack:player.inventory.mainInventory) assertNull(stack);
    }
    @Test public void lootTablePicksAndGrantsItsRegisteredReward() {
        task("check",null,1,false); service.check(player,quest,true);
        QuestCore.Reward leaf=new QuestCore.Reward(); leaf.type="xp"; leaf.count=9;
        service.book.lootTables.put("table",Collections.singletonList(leaf));
        QuestCore.Reward reward=new QuestCore.Reward(); reward.type="loot"; reward.target="table"; quest.rewards.add(reward);
        service.claim(player,quest,Collections.<Integer>emptyList()); verify(player).addExperience(9);
    }
    @Test public void customTaskAndRewardHandlersRunOnAuthoritativeObjects() {
        QuestHooks.TASKS.put("test:task",(p,t)->p==player);
        final boolean[] granted={false}; QuestHooks.REWARDS.put("test:reward",(p,r)->granted[0]=p==player);
        try {
            task("custom","test:task",1,false);
            QuestCore.Reward reward=new QuestCore.Reward(); reward.type="custom"; reward.target="test:reward"; quest.rewards.add(reward);
            assertTrue(service.check(player,quest,true)); service.claim(player,quest,Collections.<Integer>emptyList()); assertTrue(granted[0]);
        } finally { QuestHooks.TASKS.remove("test:task"); QuestHooks.REWARDS.remove("test:reward"); }
    }

    @Test public void craftingOnlyTasksCannotBeSatisfiedFromExistingInventory() {
        QuestCore.Task t=task("item","minecraft:iron_ingot",3,false); t.craftingOnly=true;
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);
        assertFalse(service.check(player,quest,false));
        service.crafted(player,new ItemStack(Items.iron_ingot,3)); assertTrue(service.check(player,quest,false));
    }
    @Test public void alternativeItemsAndOreDictionaryAreAccepted() {
        QuestCore.Task t=task("item","minecraft:iron_ingot",4,true); t.targets.add("minecraft:gold_ingot");
        player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,2); player.inventory.mainInventory[1]=new ItemStack(Items.gold_ingot,2);
        assertTrue(service.check(player,quest,false));
        net.minecraftforge.oredict.OreDictionary.registerOre("testSigmarixIron",new ItemStack(Items.iron_ingot));
        t.target="ore:testSigmarixIron"; assertTrue(service.check(player,quest,true));
    }
    @Test public void subsetNbtAllowsExtraTagsButExactModeRejectsThem() {
        QuestCore.Task t=task("item","minecraft:iron_ingot",1,false); t.nbt="{quality:1}";
        ItemStack stack=new ItemStack(Items.iron_ingot); NBTTagCompound tag=new NBTTagCompound(); tag.setInteger("quality",1); tag.setString("extra","retained"); stack.setTagCompound(tag); player.inventory.mainInventory[0]=stack;
        assertFalse(service.check(player,quest,false)); t.nbtMode="subset"; assertTrue(service.check(player,quest,false));
    }
    @Test public void observationTimerRequiresConsecutiveServerTicks() {
        QuestCore.Task t=task("observation","minecraft:stone",1,false); t.lookTicks=3;
        net.minecraft.util.MovingObjectPosition hit=new net.minecraft.util.MovingObjectPosition(1,2,3,1,net.minecraft.util.Vec3.createVectorHelper(1,2,3));
        when(player.rayTrace(8,1)).thenReturn(hit); when(player.worldObj.getBlock(1,2,3)).thenReturn(Blocks.stone);
        service.observe(player); service.observe(player); assertFalse(service.check(player,quest,false));
        when(player.rayTrace(8,1)).thenReturn(null); service.observe(player);
        when(player.rayTrace(8,1)).thenReturn(hit); service.observe(player); service.observe(player); assertFalse(service.check(player,quest,false));
        service.observe(player); assertTrue(service.check(player,quest,false));
    }
    @Test public void boxLocationsRespectBoundsAndOptionalDimensionCheck() {
        QuestCore.Task t=task("location",null,1,false); t.box=true; t.x=1; t.y=1; t.z=1; t.width=2; t.height=2; t.depth=2; t.dimension=-1;
        player.posX=2; player.posY=2; player.posZ=2; assertFalse(service.check(player,quest,false));
        t.ignoreDimension=true; assertTrue(service.check(player,quest,false)); player.posX=3; assertFalse(service.check(player,quest,false));
    }
    @Test public void staleBookEditCannotOverwriteConfigAndInvalidReloadKeepsActiveBook() throws Exception {
        java.nio.file.Path folder=java.nio.file.Files.createTempDirectory("quests-book-test");
        File file=folder.resolve("quests.json").toFile();
        java.nio.file.Files.write(file.toPath(),"{\"quests\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        QuestService actual=new QuestService(file); actual.load(); long revision=actual.revision;
        QuestCore.Book update=new QuestCore.Book(); update.title="Edited"; actual.save(update,revision);
        assertTrue(folder.resolve("quests.json.bak").toFile().exists());
        try { actual.save(new QuestCore.Book(),revision); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals("Edited",actual.book.title);
        java.nio.file.Files.write(file.toPath(),"{broken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try { actual.load(); fail(); } catch(Exception expected) {}
        assertEquals("Edited",actual.book.title);
        java.nio.file.Files.deleteIfExists(file.toPath()); java.nio.file.Files.deleteIfExists(folder.resolve("quests.json.bak")); java.nio.file.Files.deleteIfExists(folder);
    }
    @Test public void manualConfirmationAndStartedDependenciesAreIndependent() {
        QuestCore.Task confirmation=task("check",null,1,false); confirmation.manual=true;
        QuestCore.Quest next=new QuestCore.Quest(); next.id="next"; next.dependencies.add("quest:test"); next.dependencyState="started";
        assertFalse(service.unlocked(player.getUniqueID(),next));
        service.start(player,quest); assertTrue(service.unlocked(player.getUniqueID(),next));
        assertFalse(service.check(player,quest,true));
        service.confirm(player,quest,confirmation.id); assertTrue(service.check(player,quest,true));
    }
    @Test public void taskDependencyUnlocksBeforeWholeQuestCompletes() {
        QuestCore.Task first=task("check",null,1,false); first.manual=true;
        task("dimension",null,1,false).dimension=-1;
        QuestCore.Quest next=new QuestCore.Quest(); next.id="next"; next.dependencies.add("task:test/"+first.id);
        assertFalse(service.unlocked(player.getUniqueID(),next)); service.confirm(player,quest,first.id);
        assertTrue(service.unlocked(player.getUniqueID(),next)); assertFalse(service.check(player,quest,true));
    }
    private EntityPlayerMP teammate() {
        EntityPlayerMP other=mock(EntityPlayerMP.class); when(other.getUniqueID()).thenReturn(UUID.randomUUID()); when(other.getRNG()).thenReturn(new Random(2));
        other.inventory=new InventoryPlayer(other); other.inventoryContainer=mock(Container.class); other.worldObj=player.worldObj;
        data.createTeam(player.getUniqueID(),"Test"); data.invite(player.getUniqueID(),other.getUniqueID()); data.join(other.getUniqueID(),player.getUniqueID()); return other;
    }
    private QuestCore.Reward xpReward(int amount,String mode) {
        QuestCore.Reward r=new QuestCore.Reward(); r.type="xp"; r.count=amount; r.mode=mode; r.id="r"+quest.rewards.size(); quest.rewards.add(r); return r;
    }
    @Test public void mixedRewardsPaySharedOnceAndIndividualForEachMember() {
        EntityPlayerMP other=teammate(); task("check",null,1,false); xpReward(3,"team"); xpReward(7,"individual");
        assertTrue(service.check(player,quest,true)); service.claim(player,quest,Collections.<Integer>emptyList());
        assertTrue(service.claimed(player.getUniqueID(),quest)); assertFalse(service.claimed(other.getUniqueID(),quest));
        service.claim(other,quest,Collections.<Integer>emptyList()); verify(other,never()).addExperience(3); verify(other).addExperience(7);
        verify(player).addExperience(3); verify(player).addExperience(7);
        try { service.claim(other,quest,Collections.<Integer>emptyList()); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void alreadyClaimedSharedChoicesDoNotRequireAnotherSelection() {
        EntityPlayerMP other=teammate(); task("check",null,1,false);
        QuestCore.Reward choice=new QuestCore.Reward(); choice.id="choice"; choice.type="choice"; choice.mode="team";
        QuestCore.Reward option=new QuestCore.Reward(); option.type="xp"; option.count=2; choice.options.add(option); quest.rewards.add(choice); xpReward(7,"individual");
        service.check(player,quest,true); service.claim(player,quest,Collections.singletonList(0));
        service.claim(other,quest,Collections.<Integer>emptyList()); verify(other).addExperience(7); verify(other,never()).addExperience(2);
    }
    @Test public void individualRewardsPersistAndRepeatWaitsForEveryMember() {
        EntityPlayerMP other=teammate(); task("check",null,1,false); xpReward(4,"individual"); quest.repeatable=true;
        service.check(player,quest,true); service.claim(player,quest,Collections.<Integer>emptyList());
        NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved); data=new QuestProgress(); data.readFromNBT(saved); doReturn(data).when(service).progress();
        assertTrue(service.claimed(player.getUniqueID(),quest)); assertFalse(service.claimed(other.getUniqueID(),quest));
        try { service.repeat(player,quest); fail(); } catch(IllegalArgumentException expected) {}
        service.claim(other,quest,Collections.<Integer>emptyList()); service.repeat(player,quest);
        service.check(player,quest,true); service.claim(player,quest,Collections.<Integer>emptyList()); verify(player,times(2)).addExperience(4);
    }
    @Test public void leavingTeamAfterRepeatDoesNotResurrectOldSoloCounters() {
        UUID member=UUID.randomUUID(); data.add(member,"test","t0",10,10); data.markTask(member,"test","t0"); data.complete(member,"test");
        data.createTeam(player.getUniqueID(),"Test"); data.invite(player.getUniqueID(),member); data.join(member,player.getUniqueID());
        data.reset(player.getUniqueID(),"test"); data.add(player.getUniqueID(),"test","t0",2,10); data.leave(member);
        assertEquals(2,data.value(member,"test","t0")); assertFalse(data.taskDone(member,"test","t0")); assertFalse(data.completed(member).contains("test"));
    }
    @Test public void chapterCompletionIgnoresOptionalQuestsAndRejectsUnknownChapters() {
        QuestCore.Chapter c=new QuestCore.Chapter(); c.id="chapter"; c.title="Chapter"; service.book.chapters.add(c); quest.chapter=c.id;
        QuestCore.Quest optional=new QuestCore.Quest(); optional.id="optional"; optional.chapter=c.id; optional.optional=true; service.quests.put(optional.id,optional);
        assertEquals("not_started",service.chapterState(player.getUniqueID(),c.id)); data.complete(player.getUniqueID(),quest.id);
        assertEquals("complete",service.chapterState(player.getUniqueID(),c.id)); assertFalse(service.dependencies(player.getUniqueID()).chapter("missing",false));
    }
    @Test public void lootRollCountAndEmptyResultsAreSupported() {
        QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=2;
        service.book.lootTables.put("roll",Collections.singletonList(xp)); QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.rolls=3; service.book.lootSettings.put("roll",settings);
        ItemStack crate=new ItemStack(Items.paper,2); service.openLoot(player,"roll",crate); verify(player,times(3)).addExperience(2); assertEquals(1,crate.stackSize);
        service.book.lootTables.put("empty",Collections.<QuestCore.Reward>emptyList()); settings=new QuestCore.LootSettings(); settings.emptyWeight=1; service.book.lootSettings.put("empty",settings);
        service.openLoot(player,"empty",crate); assertEquals(0,crate.stackSize); verify(player,times(3)).addExperience(2);
    }
    @Test public void lootExpansionLimitDoesNotConsumeCrateOrAwardPartialRewards() {
        QuestCore.Reward nested=new QuestCore.Reward(); nested.type="loot"; nested.target="inner";
        QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=2;
        service.book.lootTables.put("outer",Collections.singletonList(nested)); service.book.lootTables.put("inner",Collections.singletonList(xp));
        QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.rolls=64; service.book.lootSettings.put("outer",settings); service.book.lootSettings.put("inner",settings);
        ItemStack crate=new ItemStack(Items.paper); try { service.openLoot(player,"outer",crate); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(1,crate.stackSize); verify(player,never()).addExperience(anyInt());
    }
    private static final class TestTank extends net.minecraft.item.Item implements IFluidContainerItem {
        public FluidStack getFluid(ItemStack s) { return s.hasTagCompound()?FluidStack.loadFluidStackFromNBT(s.getTagCompound()):null; }
        public int getCapacity(ItemStack s) { return 4000; }
        public int fill(ItemStack s,FluidStack fluid,boolean commit) { if(commit) s.setTagCompound(fluid.writeToNBT(new NBTTagCompound())); return fluid.amount; }
        public FluidStack drain(ItemStack s,int amount,boolean commit) {
            FluidStack fluid=getFluid(s); if(fluid==null) return null; FluidStack out=fluid.copy(); out.amount=Math.min(amount,fluid.amount);
            if(commit) { fluid.amount-=out.amount; s.setTagCompound(fluid.amount==0?null:fluid.writeToNBT(new NBTTagCompound())); } return out;
        }
    }
    @Test public void dynamicFluidContainersDrainOnlyRequiredAmount() {
        TestTank tank=new TestTank(); ItemStack stack=new ItemStack(tank); tank.fill(stack,new FluidStack(FluidRegistry.WATER,2000),true); player.inventory.mainInventory[0]=stack;
        task("fluid","water",750,true); assertTrue(service.check(player,quest,true)); assertEquals(1250,tank.getFluid(player.inventory.mainInventory[0]).amount);
    }
    @Test public void failedFluidPreflightPreservesOriginalContainer() {
        TestTank tank=new TestTank(); ItemStack stack=new ItemStack(tank); tank.fill(stack,new FluidStack(FluidRegistry.WATER,2000),true); player.inventory.mainInventory[0]=stack;
        task("fluid","water",750,true); task("dimension",null,1,false).dimension=-1;
        assertFalse(service.check(player,quest,true)); assertEquals(2000,tank.getFluid(stack).amount);
    }
    @Test public void fluidNbtAndReadOnlyDynamicContainersAreRespected() {
        TestTank tank=new TestTank(); ItemStack stack=new ItemStack(tank); FluidStack water=new FluidStack(FluidRegistry.WATER,2000); water.tag=new NBTTagCompound(); water.tag.setInteger("quality",2); tank.fill(stack,water,true); player.inventory.mainInventory[0]=stack;
        QuestCore.Task t=task("fluid","water",750,false); t.nbt="{quality:1}"; assertFalse(service.check(player,quest,true));
        t.nbt="{quality:2}"; assertTrue(service.check(player,quest,true)); assertEquals(2000,tank.getFluid(player.inventory.mainInventory[0]).amount);
    }
    @Test public void automaticRootDoesNotMarkManualRootClaimed() {
        task("check",null,1,false); QuestCore.Reward auto=xpReward(3,"team"); auto.auto="enabled"; xpReward(7,"team");
        assertTrue(service.finish(player,quest)); verify(player).addExperience(3); verify(player,never()).addExperience(7);
        assertFalse(service.claimed(player.getUniqueID(),quest)); service.autoClaim(player,quest); verify(player,times(1)).addExperience(3);
        service.claim(player,quest,Collections.<Integer>emptyList()); verify(player).addExperience(7); assertTrue(service.claimed(player.getUniqueID(),quest));
    }
    @Test public void automaticIndividualRootsPayEachTeamMemberOnce() {
        EntityPlayerMP other=teammate(); task("check",null,1,false); QuestCore.Reward auto=xpReward(5,"individual"); auto.auto="enabled";
        assertTrue(service.finish(player,quest)); service.autoClaim(other,quest); service.autoClaim(other,quest);
        verify(player,times(1)).addExperience(5); verify(other,times(1)).addExperience(5);
    }
    @Test public void automaticRewardsCanCoexistWithUnchosenChoices() {
        task("check",null,1,false); quest.autoClaim=true; xpReward(3,"team");
        QuestCore.Reward choice=new QuestCore.Reward(); choice.id="choice"; choice.type="choice"; QuestCore.Reward option=new QuestCore.Reward(); option.type="xp"; option.count=7; choice.options.add(option); quest.rewards.add(choice);
        service.finish(player,quest); verify(player).addExperience(3); assertFalse(service.claimed(player.getUniqueID(),quest));
        service.claim(player,quest,Collections.singletonList(0)); verify(player).addExperience(7); verify(player,times(1)).addExperience(3);
    }
    @Test public void pythonMigratedWorldDataLoadsWithNativeForgeNbt() throws Exception {
        NBTTagCompound root;
        try(InputStream in=getClass().getResourceAsStream("/migrated-progress.dat")) { assertNotNull(in); root=net.minecraft.nbt.CompressedStreamTools.readCompressed(in); }
        QuestProgress migrated=new QuestProgress(); migrated.readFromNBT(root.getCompoundTag("data")); UUID member=UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertTrue(migrated.leader(member)); assertTrue(migrated.completed(member).contains("ftb_00000002"));
        assertEquals(4,migrated.value(member,"ftb_00000002","ftb_00000003")); assertTrue(migrated.rewardClaimed(member,"ftb_00000002","ftb_00000004",true));
    }
    @Test public void deviceContainerRejectsForeignPlayersAndRemovedTiles() {
        QuestDevice tile=new QuestDevice(); tile.owner=player.getUniqueID(); tile.setWorldObj(player.worldObj); when(player.worldObj.getTileEntity(0,0,0)).thenReturn(tile);
        QuestDeviceContainer container=new QuestDeviceContainer(player,tile); assertTrue(container.canInteractWith(player));
        EntityPlayerMP other=mock(EntityPlayerMP.class); when(other.getUniqueID()).thenReturn(UUID.randomUUID()); assertFalse(container.canInteractWith(other));
        when(other.canCommandSenderUseCommand(2,"quests.edit")).thenReturn(true); assertTrue(container.canInteractWith(other));
        when(player.worldObj.getTileEntity(0,0,0)).thenReturn(null); assertFalse(container.canInteractWith(player));
    }
    @Test public void deviceContainerChecksDistanceBeforeMovingInventory() {
        QuestDevice tile=new QuestDevice(); tile.owner=player.getUniqueID(); tile.setWorldObj(player.worldObj); when(player.worldObj.getTileEntity(0,0,0)).thenReturn(tile);
        tile.setInventorySlotContents(0,new ItemStack(Items.iron_ingot,4)); QuestDeviceContainer container=new QuestDeviceContainer(player,tile);
        when(player.getDistanceSq(.5,.5,.5)).thenReturn(100.0); assertNull(container.transferStackInSlot(player,0)); assertEquals(4,tile.getStackInSlot(0).stackSize);
        when(player.getDistanceSq(.5,.5,.5)).thenReturn(0.0); assertNotNull(container.transferStackInSlot(player,0)); assertNull(tile.getStackInSlot(0));
    }
    @Test public void convertedDefinitionAndAdvancedExamplePassProductionRegistryValidation() throws Exception {
        for(String resource:new String[]{"/imported-quests.json","/example-advanced-quests.json"}) {
            java.nio.file.Path file=java.nio.file.Files.createTempFile("quest-import-fixture",".json");
            try(InputStream in=getClass().getResourceAsStream(resource)) { assertNotNull(in); java.nio.file.Files.copy(in,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            try { QuestService imported=new QuestService(file.toFile()); imported.load(); assertFalse(imported.quests.isEmpty()); }
            finally { java.nio.file.Files.deleteIfExists(file); }
        }
    }
    @Test public void composedItemFiltersKeepDistinctMetadataAndNbtAlternatives() {
        QuestCore.Task t=task("item",null,2,true); t.filter=new QuestItemFilter(); t.filter.type="any"; t.filter.children.clear();
        QuestItemFilter one=new QuestItemFilter(); one.value="minecraft:iron_ingot"; one.nbt="{quality:1}";
        QuestItemFilter two=new QuestItemFilter(); two.value="minecraft:gold_ingot"; two.nbt="{quality:2}"; t.filter.children.add(one); t.filter.children.add(two); t.filter.validate(true);
        ItemStack wrong=new ItemStack(Items.gold_ingot,2); wrong.setTagCompound(QuestSnbt.parse("{quality:1}")); assertFalse(QuestService.matches(wrong,t));
        ItemStack valid=new ItemStack(Items.gold_ingot,2); valid.setTagCompound(QuestSnbt.parse("{quality:2}")); player.inventory.mainInventory[0]=valid;
        assertTrue(service.check(player,quest,true)); assertNull(player.inventory.mainInventory[0]);
    }
    @Test public void andNotModAndOreFiltersUseRealRegistries() {
        QuestItemFilter all=new QuestItemFilter(); all.type="all";
        QuestItemFilter mod=new QuestItemFilter(); mod.type="mod"; mod.value="minecraft"; all.children.add(mod);
        QuestItemFilter not=new QuestItemFilter(); not.type="not"; QuestItemFilter food=new QuestItemFilter(); food.type="food"; not.children.add(food); all.children.add(not); all.validate(true);
        assertTrue(all.matches(new ItemStack(Items.iron_ingot))); assertFalse(all.matches(new ItemStack(Items.bread)));
        QuestItemFilter ore=new QuestItemFilter(); ore.type="ore"; ore.value="ingotIron"; ore.validate(true); assertTrue(ore.matches(new ItemStack(Items.iron_ingot)));
    }
    @Test public void malformedFilterTreesFailValidation() {
        QuestItemFilter invalid=new QuestItemFilter(); invalid.type="not";
        try { invalid.validate(false); fail(); } catch(IllegalArgumentException expected) {}
        invalid.children.add(invalid); try { invalid.validate(false); fail(); } catch(IllegalArgumentException expected) {}
    }
    private QuestDevice terminalTile() {
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=quest.tasks.get(0).id; tile.setWorldObj(player.worldObj); doReturn(player).when(tile).player();
        when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(0,"eu_test")); QuestsMod.service=service; return tile;
    }
    @Test public void euPacketsCreditOnlyAcceptedWholeUnitsAndRetainFractions() {
        task("ic2_energy",null,2,false); QuestDevice tile=terminalTile();
        try {
            assertEquals(0,tile.injectEnergy(ForgeDirection.UP,.4,32),0); assertEquals(0,data.value(player.getUniqueID(),quest.id,"t0"));
            assertEquals(1.6,tile.getDemandedEnergy(),1e-9); assertEquals(0,tile.injectEnergy(ForgeDirection.UP,.6,32),0);
            assertEquals(1,data.value(player.getUniqueID(),quest.id,"t0")); assertEquals(9,tile.injectEnergy(ForgeDirection.UP,10,32),0);
            assertEquals(2,data.value(player.getUniqueID(),quest.id,"t0")); assertEquals(0,tile.getDemandedEnergy(),0); assertTrue(tile.acceptsEnergyFrom(null,ForgeDirection.UP)); assertEquals(5,tile.injectEnergy(ForgeDirection.UP,5,32),0);
        } finally { QuestsMod.service=null; }
    }
    @Test public void euFractionsSurviveNbtAndAreNotAppliedToAnotherTask() {
        task("ic2_energy",null,4,false); QuestDevice tile=terminalTile();
        try {
            tile.injectEnergy(ForgeDirection.UP,.5,32); cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(tile.getClass(),"sigmarixquests:test-eu"); NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved);
            QuestDevice restored=spy(new QuestDevice()); restored.readFromNBT(saved); restored.setWorldObj(player.worldObj); doReturn(player).when(restored).player();
            assertEquals(3.5,restored.getDemandedEnergy(),0); restored.injectEnergy(ForgeDirection.UP,.5,32); assertEquals(1,data.value(player.getUniqueID(),quest.id,"t0"));
            QuestCore.Task another=task("ic2_energy",null,4,false); restored.task=another.id; assertEquals(4,restored.getDemandedEnergy(),0);
        } finally { QuestsMod.service=null; }
    }
    @Test public void euRejectsWrongTasksMissingOwnersAndImpossibleAmounts() {
        task("energy",null,4,false); QuestDevice tile=terminalTile();
        try {
            assertEquals(0,tile.getDemandedEnergy(),0); assertTrue(tile.acceptsEnergyFrom(null,ForgeDirection.UP)); assertEquals(5,tile.injectEnergy(ForgeDirection.UP,5,32),0);
            quest.tasks.get(0).type="ic2_energy"; doReturn(null).when(tile).player(); assertEquals(4,tile.getDemandedEnergy(),0); tile.owner=null; assertEquals(0,tile.getDemandedEnergy(),0); tile.owner=player.getUniqueID();
            doReturn(player).when(tile).player(); tile.injectEnergy(ForgeDirection.UP,Double.MAX_VALUE,32); assertEquals(0,data.value(player.getUniqueID(),quest.id,"t0"));
        } finally { QuestsMod.service=null; }
    }
    @Test public void manaReceiverCapsCreditAndNeverExposesSpentManaAsStorage() {
        task("botania_mana",null,10,false); QuestDevice tile=terminalTile();
        try {
            assertTrue(tile.canRecieveManaFromBursts()); tile.recieveMana(4); assertEquals(4,data.value(player.getUniqueID(),quest.id,"t0"));
            assertEquals(0,tile.getCurrentMana()); tile.recieveMana(-4); assertEquals(4,data.value(player.getUniqueID(),quest.id,"t0"));
            tile.recieveMana(100); assertEquals(10,data.value(player.getUniqueID(),quest.id,"t0")); assertTrue(tile.isFull()); assertFalse(tile.canRecieveManaFromBursts());
            tile.recieveMana(100); assertEquals(10,data.value(player.getUniqueID(),quest.id,"t0")); assertTrue(service.check(player,quest,true));
        } finally { QuestsMod.service=null; }
    }
    @Test public void manaReceiverRejectsWrongTasksAndMissingOwnersButAcceptsOfflineProgress() {
        task("energy",null,10,false); QuestDevice tile=terminalTile();
        try {
            assertTrue(tile.isFull()); tile.recieveMana(10); assertEquals(0,data.value(player.getUniqueID(),quest.id,"t0"));
            quest.tasks.get(0).type="botania_mana"; doReturn(null).when(tile).player(); assertFalse(tile.isFull()); tile.recieveMana(10); assertEquals(10,data.value(player.getUniqueID(),quest.id,"t0")); tile.owner=null; assertTrue(tile.isFull());
        } finally { QuestsMod.service=null; }
    }
    private double[] wallet(double amount) {
        final double[] value={amount}; doAnswer(call->value[0]).when(service).emcBalance(player);
        doAnswer(call->{ value[0]=(Double)call.getArguments()[1]; return null; }).when(service).setEmc(eq(player),anyDouble()); return value;
    }
    @Test public void incompleteQuestDoesNotConsumeEmcOrInventory() {
        double[] value=wallet(100); player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,3);
        task("emc",null,64,true); task("item","minecraft:iron_ingot",2,true); task("dimension",null,1,false).dimension=-1;
        assertFalse(service.check(player,quest,true)); assertEquals(100,value[0],0); assertEquals(3,player.inventory.mainInventory[0].stackSize);
    }
    @Test public void emcAndItemsCommitTogetherOnce() {
        double[] value=wallet(100); player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,3);
        task("emc",null,64,true); task("item","minecraft:iron_ingot",2,true);
        assertTrue(service.check(player,quest,true)); assertEquals(36,value[0],0); assertEquals(1,player.inventory.mainInventory[0].stackSize);
        assertFalse(service.check(player,quest,true)); assertEquals(36,value[0],0);
    }
    @Test public void partialEmcSubmissionRetainsFractionsAndAvoidsDoubleSpending() {
        double[] value=wallet(12.5); QuestCore.Task t=task("emc",null,20,true);
        assertEquals(12,service.submit(player,quest,t.id)); assertEquals(.5,value[0],0); assertEquals(12,data.value(player.getUniqueID(),quest.id,t.id));
        assertFalse(service.check(player,quest,true)); assertEquals(.5,value[0],0);
        value[0]=10.5; assertTrue(service.check(player,quest,true)); assertEquals(2.5,value[0],0);
    }
    @Test public void emcRequirementsCannotReuseReservedFunds() {
        double[] value=wallet(10); task("emc",null,8,false); task("emc",null,3,true);
        assertFalse(service.check(player,quest,true)); assertEquals(10,value[0],0);
    }
    @Test public void emcPrecisionFailureDoesNotSpendOtherResources() {
        double[] value=wallet(Math.pow(2,60)); task("emc",null,1,true);
        try { service.check(player,quest,true); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(Math.pow(2,60),value[0],0); assertFalse(data.completed(player.getUniqueID()).contains(quest.id));
    }
    @Test public void sharedQuestDoesNotChargeAnotherMembersWalletAfterCompletion() {
        EntityPlayerMP other=teammate(); double[] value=wallet(100); task("emc",null,64,true);
        assertTrue(service.check(player,quest,true)); assertFalse(service.check(other,quest,true)); assertEquals(36,value[0],0); verify(service,never()).setEmc(eq(other),anyDouble());
    }
    @Test public void readOnlyEmcTaskDoesNotDebitWallet() {
        double[] value=wallet(100); task("emc",null,64,false); assertTrue(service.check(player,quest,true)); assertEquals(100,value[0],0); verify(service,never()).setEmc(eq(player),anyDouble());
    }
    @Test public void nativeStagesAndSkillsSurviveSaveWithoutBecomingTeamOwned() {
        UUID id=player.getUniqueID(),other=UUID.randomUUID();
        data.stage(id,"pack:bronze",true); data.skill(id,"pack:mining",12);
        data.createTeam(id,"Native markers"); data.invite(id,other); data.join(other,id);
        assertFalse(data.stage(other,"pack:bronze")); assertEquals(0,data.skill(other,"pack:mining"));
        NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved); QuestProgress reloaded=new QuestProgress(); reloaded.readFromNBT(saved);
        assertTrue(reloaded.stage(id,"pack:bronze")); assertEquals(12,reloaded.skill(id,"pack:mining"));
        reloaded.disband(id); assertTrue(reloaded.stage(id,"pack:bronze")); assertFalse(reloaded.stage(other,"pack:bronze"));
    }
    @Test public void stageAndSkillTasksRequireActualPersonalState() {
        task("gamestage","pack:bronze",1,false).sticky=false; task("skill","pack:mining",10,false).sticky=false;
        assertFalse(service.check(player,quest,false)); data.stage(player.getUniqueID(),"pack:bronze",true);
        assertFalse(service.check(player,quest,false)); data.skill(player.getUniqueID(),"pack:mining",10);
        assertTrue(service.check(player,quest,false)); data.stage(player.getUniqueID(),"pack:bronze",false);
        assertFalse(service.check(player,quest,false));
    }
    @Test public void markerRewardsAddRemoveAndSetLevels() {
        task("check",null,1,false); QuestCore.Reward stage=new QuestCore.Reward(); stage.type="gamestage"; stage.target="pack:unlocked";
        QuestCore.Reward skill=new QuestCore.Reward(); skill.type="skill"; skill.target="pack:mining"; skill.points=10;
        quest.rewards.add(stage); quest.rewards.add(skill); assertTrue(service.finish(player,quest)); service.claim(player,quest,Collections.<Integer>emptyList());
        assertTrue(data.stage(player.getUniqueID(),stage.target)); assertEquals(10,data.skill(player.getUniqueID(),skill.target));
        data.reset(player.getUniqueID(),quest.id); stage.remove=true; skill.add=false; skill.points=3;
        assertTrue(service.finish(player,quest)); service.claim(player,quest,Collections.<Integer>emptyList());
        assertFalse(data.stage(player.getUniqueID(),stage.target)); assertEquals(3,data.skill(player.getUniqueID(),skill.target));
    }
    @Test public void lifecycleEventsFireOncePerTransitionAndAgainAfterRepeat() {
        List<String> events=new ArrayList<String>(); java.util.function.Consumer<QuestEvents.Event> listener=e->events.add(e.type+":"+e.task);
        for(String type:QuestEvents.TYPES) QuestEvents.on(type,listener);
        try {
            QuestCore.Task manual=task("check",null,1,false); manual.manual=true;
            service.start(player,quest); service.start(player,quest); service.confirm(player,quest,manual.id); service.detect(player);
            assertTrue(service.finish(player,quest)); service.claim(player,quest,Collections.<Integer>emptyList());
            assertEquals(Arrays.asList("quest_started:","task_started:"+manual.id,"task_completed:"+manual.id,"quest_completed:"),events);
            data.reset(player.getUniqueID(),quest.id); service.confirm(player,quest,manual.id); assertTrue(service.finish(player,quest));
            assertEquals(2,Collections.frequency(events,"quest_completed:")); assertEquals(2,Collections.frequency(events,"task_started:"+manual.id));
        } finally { for(String type:QuestEvents.TYPES) QuestEvents.off(type,listener); }
    }
    @Test public void profilesIsolateCompletionCountersAndPersonalRewardsAcrossSave() {
        UUID id=player.getUniqueID(); data.complete(id,"same"); data.add(id,"same","task",5,10); data.claimReward(id,"same","root",true);
        data.profile("expert"); assertFalse(data.completed(id).contains("same")); assertEquals(0,data.value(id,"same","task")); assertFalse(data.rewardClaimed(id,"same","root",true));
        data.add(id,"same","task",2,10); NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved);
        QuestProgress reloaded=new QuestProgress(); reloaded.readFromNBT(saved); assertEquals("expert",reloaded.profile()); assertEquals(2,reloaded.value(id,"same","task"));
        reloaded.profile("default"); assertTrue(reloaded.completed(id).contains("same")); assertEquals(5,reloaded.value(id,"same","task")); assertTrue(reloaded.rewardClaimed(id,"same","root",true));
    }
    @Test public void teamCreateAndDisbandMergeEveryProfileWithoutMixingTheirProgress() {
        UUID id=player.getUniqueID(),other=UUID.randomUUID();
        data.add(id,"quest","task",3,10); data.profile("expert"); data.add(id,"quest","task",7,10);
        data.createTeam(id,"Across modes"); data.invite(id,other); data.join(other,id); assertEquals(7,data.value(other,"quest","task"));
        data.profile("default"); assertEquals(3,data.value(other,"quest","task")); data.add(other,"quest","task",1,10);
        data.disband(id); assertEquals(4,data.value(other,"quest","task")); data.profile("expert"); assertEquals(7,data.value(other,"quest","task")); assertEquals(7,data.value(id,"quest","task"));
    }
    @Test public void overflowingSkillRewardsFailBeforeClaimStateChanges() {
        task("check",null,1,false); data.skill(player.getUniqueID(),"pack:mining",Integer.MAX_VALUE-2);
        for(int i=0;i<2;i++) { QuestCore.Reward reward=new QuestCore.Reward(); reward.id="r"+i; reward.type="skill"; reward.target="pack:mining"; reward.points=2; quest.rewards.add(reward); }
        assertTrue(service.finish(player,quest)); try { service.claim(player,quest,Collections.<Integer>emptyList()); fail(); } catch(ArithmeticException expected) {}
        assertFalse(service.claimed(player.getUniqueID(),quest)); assertEquals(Integer.MAX_VALUE-2,data.skill(player.getUniqueID(),"pack:mining"));
    }
    @Test public void interactionsRespectBlockMetadataAndTileNbt() {
        QuestCore.Task click=task("interaction","minecraft:furnace",2,false); click.meta=2; click.nbtMode="subset"; click.nbt="{BurnTime:7s}";
        net.minecraft.tileentity.TileEntityFurnace furnace=new net.minecraft.tileentity.TileEntityFurnace(); furnace.furnaceBurnTime=7;
        when(player.worldObj.getBlock(1,2,3)).thenReturn(Blocks.furnace); when(player.worldObj.getTileEntity(1,2,3)).thenReturn(furnace); when(player.worldObj.getBlockMetadata(1,2,3)).thenReturn(3);
        service.interact(player,player.worldObj,1,2,3); assertEquals(0,data.value(player.getUniqueID(),quest.id,click.id));
        when(player.worldObj.getBlockMetadata(1,2,3)).thenReturn(2); service.interact(player,player.worldObj,1,2,3); assertEquals(1,data.value(player.getUniqueID(),quest.id,click.id));
        furnace.furnaceBurnTime=0; service.interact(player,player.worldObj,1,2,3); assertEquals(1,data.value(player.getUniqueID(),quest.id,click.id));
        click.matchType="block_entity_id"; click.target="Furnace"; click.nbt=""; service.interact(player,player.worldObj,1,2,3); assertEquals(2,data.value(player.getUniqueID(),quest.id,click.id));
    }
    @Test public void interactionEntityClassesRemainSeparateFromBlockMatchers() {
        net.minecraft.entity.passive.EntityPig pig=mock(net.minecraft.entity.passive.EntityPig.class);
        QuestCore.Task click=task("interaction",pig.getClass().getName(),1,false); click.matchType="entity_class";
        service.interact(player,pig); assertEquals(1,data.value(player.getUniqueID(),quest.id,click.id));
        QuestCore.Task block=task("interaction","minecraft:stone",1,false); block.matchType="block_id"; service.interact(player,pig); assertEquals(0,data.value(player.getUniqueID(),quest.id,block.id));
    }
    @Test public void leaderboardCountsKnownQuestsAndRanksTeamsOncePerMode() {
        UUID id=player.getUniqueID(),other=UUID.randomUUID(); data.complete(id,"known"); data.complete(id,"old-removed");
        data.createTeam(id,"Players"); data.invite(id,other); data.join(other,id);
        List<QuestNetwork.Rank> board=data.leaderboard(Collections.singleton("known")); assertEquals(1,board.size()); assertEquals("Players",board.get(0).name); assertEquals(1,board.get(0).completed);
        data.profile("expert"); assertTrue(data.leaderboard(Collections.singleton("known")).isEmpty());
    }
    @Test public void buildCraftBridgeSimulatesAndBoundsMicroMjProgress() {
        QuestCore.Task energy=task("buildcraft_mj",null,1600001,false);
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=energy.id; tile.setWorldObj(player.worldObj);
        doReturn(player).when(tile).player(); when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(0,"resource_test")); QuestsMod.service=service;
        try {
            assertEquals(17,tile.receiveEnergy(ForgeDirection.NORTH,100,true)); assertEquals(0,data.value(tile.owner,quest.id,energy.id));
            assertEquals(16,tile.receiveEnergy(ForgeDirection.NORTH,16,false)); assertEquals(1600000,data.value(tile.owner,quest.id,energy.id));
            assertEquals(1,tile.receiveEnergy(ForgeDirection.NORTH,10,false)); assertEquals(1600001,data.value(tile.owner,quest.id,energy.id));
            assertEquals(0,tile.receiveEnergy(ForgeDirection.NORTH,1,false));
        } finally { QuestsMod.service=null; }
    }
    @Test public void screenAssemblyRejectsAnotherOwnerBeforeChangingAnyPart() {
        QuestDevice root=spy(new QuestDevice()),part=spy(new QuestDevice()); net.minecraft.block.Block frame=new QuestBlocks.DeviceBlock(1,"test_frame");
        root.setWorldObj(player.worldObj); part.setWorldObj(player.worldObj); root.facing=part.facing=3; part.xCoord=1;
        root.owner=player.getUniqueID(); part.owner=UUID.randomUUID(); doReturn(frame).when(root).getBlockType(); doReturn(frame).when(part).getBlockType();
        when(player.worldObj.getTileEntity(0,0,0)).thenReturn(root); when(player.worldObj.getTileEntity(1,0,0)).thenReturn(part);
        try { QuestScreens.configure(root,2,1); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(1,root.screenWidth); assertFalse(part.screenLinked);
        part.owner=root.owner; root.quest=quest.id; QuestScreens.configure(root,2,1);
        assertEquals(root,QuestScreens.core(part)); assertTrue(QuestScreens.synchronize(part)); assertEquals(quest.id,part.quest);
        NBTTagCompound saved=new NBTTagCompound(); part.writeToNBT(saved); QuestDevice restored=new QuestDevice(); restored.readFromNBT(saved); assertTrue(restored.screenLinked); assertEquals(root.xCoord,restored.coreX);
        when(player.worldObj.blockExists(0,0,0)).thenReturn(true); when(player.worldObj.getTileEntity(0,0,0)).thenReturn(null);
        assertFalse(QuestScreens.synchronize(part)); assertFalse(part.screenLinked); assertEquals("",part.quest);
    }
    @Test public void emergencyItemsHavePersistentPersonalCooldownAcrossModes() {
        QuestCore.Reward item=new QuestCore.Reward(); item.item="minecraft:stick"; item.count=2; service.book.emergencyItems.add(item); service.book.emergencyCooldownSeconds=10;
        service.emergency(player); assertEquals(2,player.inventory.mainInventory[0].stackSize); assertEquals(10,service.emergencyWait(player));
        try { service.emergency(player); fail(); } catch(IllegalArgumentException expected) {}
        data.profile("expert"); assertEquals(10,service.emergencyWait(player));
        NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved); QuestProgress reloaded=new QuestProgress(); reloaded.readFromNBT(saved);
        assertEquals(200,reloaded.emergencyWait(player.getUniqueID(),100,200));
        when(player.worldObj.getTotalWorldTime()).thenReturn(300L); service.emergency(player); assertEquals(4,player.inventory.mainInventory[0].stackSize);
    }
    @Test public void crateDropsUseCreatureCategoriesAndNoDropWeights() {
        net.minecraft.entity.passive.EntityPig pig=mock(net.minecraft.entity.passive.EntityPig.class); net.minecraft.entity.monster.EntityZombie zombie=mock(net.minecraft.entity.monster.EntityZombie.class);
        QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.drops.passive=1; service.book.lootSettings.put("rare",settings); service.book.lootTables.put("rare",Collections.<QuestCore.Reward>emptyList()); service.book.dropLootCrates=true; service.book.noDrop=new QuestLoot.Weights(0,1,0);
        assertEquals("rare",QuestLoot.choose(service.book,pig,new Random(1))); assertNull(QuestLoot.choose(service.book,zombie,new Random(1)));
        service.book.dropLootCrates=false; assertNull(QuestLoot.choose(service.book,pig,new Random(1)));
    }
    @Test public void physicalStoragePreservesEveryInventorySlotAcrossSave() {
        QuestStorage storage=new QuestStorage(false); storage.setInventorySlotContents(0,new ItemStack(Items.iron_ingot,17)); storage.setInventorySlotContents(26,new ItemStack(Items.diamond,4));
        NBTTagCompound tag=new NBTTagCompound(); storage.writeToNBT(tag); QuestStorage reloaded=new QuestStorage(); reloaded.readFromNBT(tag);
        assertEquals(27,reloaded.getSizeInventory()); assertEquals(17,reloaded.getStackInSlot(0).stackSize); assertEquals(4,reloaded.decrStackSize(26,64).stackSize); assertNull(reloaded.getStackInSlot(26));
        QuestStorage crates=new QuestStorage(true); assertFalse(crates.isItemValidForSlot(0,new ItemStack(Items.iron_ingot))); assertEquals(54,crates.getSizeInventory());
    }
    @Test public void teammateLookingElsewhereDoesNotResetAnotherPlayersObservationTimer() {
        EntityPlayerMP other=teammate(); QuestCore.Task observe=task("observation","minecraft:stone",1,false); observe.lookTicks=3; observe.ignoreDamage=true;
        when(player.rayTrace(anyDouble(),anyFloat())).thenReturn(new net.minecraft.util.MovingObjectPosition(1,2,3,1,net.minecraft.util.Vec3.createVectorHelper(1,2,3)));
        when(player.worldObj.getBlock(1,2,3)).thenReturn(Blocks.stone);
        service.observe(player); service.observe(other); service.observe(player); assertFalse(service.check(player,quest,false));
        service.clearObservations(player.getUniqueID()); service.observe(player); assertFalse(service.check(player,quest,false));
        service.observe(player); service.observe(other); service.observe(player); assertTrue(service.check(player,quest,false)); assertTrue(service.check(other,quest,false));
    }
    @Test public void emptyQuestsCompleteNormallyButStillRequireDependencies() {
        quest.anyTask=true; quest.dependencies.add("earlier"); assertFalse(service.finish(player,quest));
        data.complete(player.getUniqueID(),"earlier"); assertTrue(service.finish(player,quest)); service.claim(player,quest,Collections.<Integer>emptyList()); assertTrue(service.claimed(player.getUniqueID(),quest));
    }
    @Test public void progressReferencesCoverTasksNestedChaptersAndTheWholeBook() {
        QuestCore.Chapter parent=new QuestCore.Chapter(); parent.id="parent"; parent.title="Parent"; QuestCore.Chapter child=new QuestCore.Chapter(); child.id="child"; child.title="Child"; child.parent="parent"; service.book.chapters.add(parent); service.book.chapters.add(child); quest.chapter="child";
        QuestCore.Task input=task("item","minecraft:iron_ingot",10,true); service.book.quests.add(quest);
        QuestCore.Quest other=new QuestCore.Quest(); other.id="other"; other.title="Other"; QuestCore.Task check=new QuestCore.Task(); check.id="check"; check.type="check"; other.tasks.add(check); service.quests.put(other.id,other); service.book.quests.add(other); service.book.validate();
        data.add(player.getUniqueID(),quest.id,input.id,5,10);
        assertEquals(.5,service.objectProgress(player.getUniqueID(),"task:test/"+input.id),0); assertEquals(.5,service.objectProgress(player.getUniqueID(),"chapter:parent"),0); assertEquals(.25,service.objectProgress(player.getUniqueID(),"book:"),0);
        assertFalse(service.objectComplete(player.getUniqueID(),"chapter:parent")); data.add(player.getUniqueID(),quest.id,input.id,5,10); assertTrue(service.objectComplete(player.getUniqueID(),"task:test/"+input.id)); assertFalse(service.objectComplete(player.getUniqueID(),quest.id));
        assertTrue(service.finish(player,quest)); assertTrue(service.objectComplete(player.getUniqueID(),"chapter:parent")); assertFalse(service.objectComplete(player.getUniqueID(),"book:"));
        assertTrue(service.finish(player,other)); assertTrue(service.objectComplete(player.getUniqueID(),"book:")); assertEquals("Parent",service.objectTitle("chapter:parent")); assertFalse(service.objectComplete(player.getUniqueID(),"chapter:missing"));
    }
    @Test public void teamMembersCanUseDevicesAndSupplyTasksWhilePersonalCollectorsWaitForOwner() {
        EntityPlayerMP other=teammate(); QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.setWorldObj(player.worldObj); when(player.worldObj.getTileEntity(0,0,0)).thenReturn(tile);
        doReturn(new QuestBlocks.DeviceBlock(0,"test_team_terminal")).when(tile).getBlockType(); QuestsMod.service=service;
        try {
            assertTrue(tile.isUseableByPlayer(other)); assertEquals(other,tile.onlineOwner(Collections.singletonList(other)));
            doReturn(new QuestBlocks.DeviceBlock(3,"test_personal_collector")).when(tile).getBlockType(); assertNull(tile.onlineOwner(Collections.singletonList(other))); assertEquals(player,tile.onlineOwner(Arrays.asList(player,other)));
            data.kick(player.getUniqueID(),other.getUniqueID()); assertFalse(tile.isUseableByPlayer(other));
        } finally { QuestsMod.service=null; }
    }
    @Test public void hiddenChapterAndLockedHiddenQuestCannotBeBoundByARegularViewer() {
        QuestCore.Chapter hidden=new QuestCore.Chapter(); hidden.id="hidden"; hidden.title="Hidden"; hidden.alwaysInvisible=true; service.book.chapters.add(hidden); quest.chapter="hidden";
        assertFalse(service.visible(player.getUniqueID(),quest)); assertFalse(service.bindable(player.getUniqueID(),"chapter:hidden"));
        hidden.alwaysInvisible=false; quest.hidden=true; quest.dependencies.add("earlier"); assertFalse(service.bindable(player.getUniqueID(),quest.id)); data.complete(player.getUniqueID(),"earlier"); assertTrue(service.bindable(player.getUniqueID(),quest.id));
    }
    @Test public void customIconEditsAreCosmeticAndRejectInvalidOrMovedItemsAtomically() {
        ItemStack icon=new ItemStack(new QuestBlocks.CustomIconItem()); player.inventory.mainInventory[0]=icon;
        when(player.getHeldItem()).thenReturn(icon);
        NBTTagCompound original=new NBTTagCompound(); original.setInteger("privateData",42); icon.setTagCompound(original);
        for(String resource:Arrays.asList("file:///tmp/icon.png","http://user:pass@example.com/icon.png","","../private.png")) {
            try { QuestIcons.edit(player,resource,"Name",0xFFCC44,0); fail(resource); } catch(IllegalArgumentException expected) {}
            assertEquals(original,icon.getTagCompound());
        }
        try { QuestIcons.edit(player,"minecraft:textures/items/diamond.png","Name",0xFFCC44,1); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(original,icon.getTagCompound());
        QuestIcons.edit(player,"minecraft:textures/items/diamond.png","Native icon",0xFFCC44,0);
        assertEquals(42,icon.getTagCompound().getInteger("privateData")); assertEquals("Native icon",icon.getTagCompound().getString("name"));
        assertEquals("minecraft:textures/items/diamond.png",QuestIcons.resource(icon)); assertEquals(0xFFCC44,QuestIcons.color(icon));
        assertFalse(original.hasKey("icon")); verify(player.inventoryContainer).detectAndSendChanges();
    }

    @Test public void legacyCrateAliasesMatchNativeCratesWithoutIgnoringRealTaskNbt() {
        QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.alias="rare"; settings.name="Rare prize"; settings.color="FFCC44"; settings.glow=true;
        service.book.lootSettings.put("table",settings); service.book.lootTables.put("table",Collections.singletonList(new QuestCore.Reward()));
        ItemStack actual=new ItemStack(new QuestBlocks.LootCrateItem()); NBTTagCompound tag=new NBTTagCompound(); tag.setString("table","table"); tag.setString("name",settings.name); tag.setInteger("color",0xFFCC44); tag.setBoolean("glow",true); actual.setTagCompound(tag);
        QuestsMod.service=service;
        try {
            assertEquals("table",QuestLoot.resolve(service.book,"rare")); assertEquals("rare",QuestLoot.table(QuestService.tag("{type:rare}")));
            assertTrue(QuestService.stackNbtMatches("{type:rare}","exact",actual));
            assertFalse(QuestService.stackNbtMatches("{type:other}","exact",actual));
            assertFalse(QuestService.stackNbtMatches("{type:rare,color:0}","exact",actual));
            tag.setInteger("quality",1); assertFalse(QuestService.stackNbtMatches("{type:rare}","exact",actual)); assertTrue(QuestService.stackNbtMatches("{type:rare}","subset",actual));
            assertEquals("Rare prize",actual.getDisplayName()); assertTrue(actual.getItem().hasEffect(actual,0));
            QuestCore.Reward xp=new QuestCore.Reward(); xp.type="xp"; xp.count=3; service.book.lootTables.put("table",Collections.singletonList(xp));
            ItemStack imported=new ItemStack(actual.getItem()); imported.setTagCompound(QuestService.tag("{type:rare}")); service.openLoot(player,"rare",imported); assertEquals(0,imported.stackSize); verify(player).addExperience(3);
        } finally { QuestsMod.service=null; }
    }

    @Test public void virtualCratesKeepExactVariantsAndLongCountsWithoutOverflow() {
        ItemStack first=new ItemStack(bankCrate),second=new ItemStack(bankCrate); first.setTagCompound(QuestService.tag("{table:first}")); second.setTagCompound(QuestService.tag("{table:second}"));
        QuestCrateBank bank=new QuestCrateBank(); assertEquals(5000000000L,bank.deposit(first,5000000000L)); assertEquals(2,bank.deposit(second,2));
        QuestCrateBank saved=QuestCrateBank.read(bank.write()); assertEquals(5000000002L,saved.total()); assertEquals(2,saved.withdraw(second,100)); assertEquals(0,saved.count(second)); assertEquals(64,saved.withdraw(first,64));
        long room=Long.MAX_VALUE-saved.total(); assertEquals(room,saved.deposit(first,Long.MAX_VALUE)); assertEquals(0,saved.deposit(second,1)); assertEquals(Long.MAX_VALUE,saved.total()); assertEquals(Long.MAX_VALUE,QuestCrateBank.read(saved.write()).total());
        try { saved.deposit(new ItemStack(Items.diamond),1); fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(Long.MAX_VALUE,saved.total());
    }
    @Test public void virtualStorageAbsorbsIntakeAndDuplicatesRefillsAndSurvivesSave() {
        ItemStack crate=new ItemStack(bankCrate); crate.setTagCompound(QuestService.tag("{table:first}")); QuestStorage storage=new QuestStorage(true);
        storage.store(crate,5000000000L); storage.compact(); assertEquals(64,storage.getStackInSlot(0).stackSize); assertEquals(4999999936L,storage.reserved());
        ItemStack second=crate.copy(); second.stackSize=64; storage.setInventorySlotContents(53,second); storage.compact(); assertNull(storage.getStackInSlot(53)); assertEquals(5000000000L,storage.reserved());
        assertEquals(64,storage.decrStackSize(0,64).stackSize); storage.compact(); assertEquals(4999999936L,storage.reserved()); assertEquals(64,storage.getStackInSlot(0).stackSize);
        NBTTagCompound tag=new NBTTagCompound(); storage.writeToNBT(tag); QuestStorage restored=new QuestStorage(); restored.readFromNBT(tag); assertEquals(storage.reserved(),restored.reserved()); assertEquals(64,restored.getStackInSlot(0).stackSize);
        java.util.List<ItemStack> parcels=restored.parcels(); assertEquals(1,parcels.size()); assertEquals(4999999936L,QuestCrateParcel.count(parcels.get(0))); assertEquals(0,restored.reserved()); assertEquals(64,restored.getStackInSlot(0).stackSize);
        QuestStorage other=new QuestStorage(true); assertEquals(4999999936L,QuestCrateParcel.deposit(parcels.get(0),other)); assertEquals(0,parcels.get(0).stackSize); assertEquals(4999999936L,other.reserved());
    }
    @Test public void crateParcelDoesNotLoseContentsWhenInventoryIsFullAndOnlyUnpacksWhatFits() {
        ItemStack crate=new ItemStack(bankCrate); crate.setTagCompound(QuestService.tag("{table:first}")); ItemStack parcel=QuestCrateParcel.create(crate,5000000000L);
        for(int i=0;i<player.inventory.mainInventory.length;i++) player.inventory.mainInventory[i]=new ItemStack(Items.stick,64);
        new QuestCrateParcel().onItemRightClick(parcel,player.worldObj,player); assertEquals(5000000000L,QuestCrateParcel.count(parcel));
        player.inventory.mainInventory[0]=crate.copy(); player.inventory.mainInventory[0].stackSize=60;
        new QuestCrateParcel().onItemRightClick(parcel,player.worldObj,player); assertEquals(64,player.inventory.mainInventory[0].stackSize); assertEquals(4999999996L,QuestCrateParcel.count(parcel));
        assertTrue(QuestCrateBank.same(crate,QuestCrateParcel.prototype(parcel)));
    }
    @Test public void storageCounterUsesAllFourWordsAndTileDescriptionDoesNotReplaceOpenBuffers() {
        QuestStorage storage=new QuestStorage(true); ItemStack crate=new ItemStack(bankCrate); storage.setInventorySlotContents(0,crate);
        QuestStorageContainer container=new QuestStorageContainer(player,storage); long expected=5000000123L;
        for(int i=0;i<4;i++) container.updateProgressBar(10+i,(short)(expected >>> (i*16))); assertEquals(expected,container.reserved);
        NBTTagCompound tag=new NBTTagCompound(); tag.setBoolean("crateOnly",true); storage.onDataPacket(null,new net.minecraft.network.play.server.S35PacketUpdateTileEntity(0,0,0,0,tag)); assertSame(crate,storage.getStackInSlot(0));
        storage.store(crate,expected); NBTTagCompound description=((net.minecraft.network.play.server.S35PacketUpdateTileEntity)storage.getDescriptionPacket()).func_148857_g(); assertFalse(description.hasKey("crateBank")); assertFalse(description.hasKey("storage"));
    }

    @Test public void virtualStoragePagesExposeVariantsBeyondTheVisibleSlotsWithoutChangingTotals() {
        QuestStorage storage=new QuestStorage(true);
        for(int i=0;i<60;i++) { ItemStack crate=new ItemStack(bankCrate); NBTTagCompound tag=new NBTTagCompound(); tag.setString("table",String.format("variant_%02d",i)); crate.setTagCompound(tag); storage.store(crate,100); }
        storage.compact(); long total=storage.reserved(); for(int i=0;i<54;i++) if(storage.getStackInSlot(i)!=null) total+=storage.getStackInSlot(i).stackSize; assertEquals(6000,total);
        storage.nextPage(); boolean last=false; total=storage.reserved();
        for(int i=0;i<54;i++) if(storage.getStackInSlot(i)!=null) { ItemStack crate=storage.getStackInSlot(i); last|=crate.getTagCompound().getString("table").equals("variant_59"); total+=crate.stackSize; }
        assertTrue(last); assertEquals(6000,total); storage.nextPage(); assertEquals("variant_00",storage.getStackInSlot(0).getTagCompound().getString("table"));
    }

    @Test public void clickCommandsAreReadFromVisibleServerDefinitions() {
        quest.customClick="command:/help"; assertEquals("/help",service.clickCommand(player.getUniqueID(),quest.id));
        try { service.clickCommand(player.getUniqueID(),"missing"); fail(); } catch(IllegalArgumentException expected) {}
        quest.hidden=true; quest.dependencies.add("earlier"); try { service.clickCommand(player.getUniqueID(),quest.id); fail(); } catch(IllegalArgumentException expected) {}
        data.complete(player.getUniqueID(),"earlier"); assertEquals("/help",service.clickCommand(player.getUniqueID(),quest.id));
        quest.customClick="custom:pack:action"; try { service.clickCommand(player.getUniqueID(),quest.id); fail(); } catch(IllegalArgumentException expected) {}
    }

    @Test public void pinsRemainPersonalAndModeSpecificAcrossSaveAndTeamChanges() {
        data.pins(player.getUniqueID(),Arrays.asList("first","second")); EntityPlayerMP other=teammate(); assertTrue(data.pins(other.getUniqueID()).isEmpty());
        data.profile("expert"); assertTrue(data.pins(player.getUniqueID()).isEmpty()); data.pins(player.getUniqueID(),Collections.singletonList("expert_quest")); data.profile("default");
        data.kick(player.getUniqueID(),other.getUniqueID()); assertEquals(Arrays.asList("first","second"),data.pins(player.getUniqueID()));
        NBTTagCompound saved=new NBTTagCompound(); data.writeToNBT(saved); QuestProgress restored=new QuestProgress(); restored.readFromNBT(saved); assertEquals(Arrays.asList("first","second"),restored.pins(player.getUniqueID())); restored.profile("expert"); assertEquals(Collections.singletonList("expert_quest"),restored.pins(player.getUniqueID()));
    }
    @Test public void pinningRejectsHiddenTargetsAndPrunesRemovedDefinitionsBeforeCapacityCheck() {
        data.pins(player.getUniqueID(),Collections.singletonList("removed")); service.pin(player.getUniqueID(),quest.id); assertEquals(Collections.singletonList(quest.id),data.pins(player.getUniqueID())); service.pin(player.getUniqueID(),quest.id); assertTrue(data.pins(player.getUniqueID()).isEmpty());
        quest.hidden=true; quest.dependencies.add("earlier"); try { service.pin(player.getUniqueID(),quest.id); fail(); } catch(IllegalArgumentException expected) {} assertTrue(data.pins(player.getUniqueID()).isEmpty());
        java.util.List<String> tooMany=new java.util.ArrayList<String>(); for(int i=0;i<17;i++) tooMany.add("q"+i); try { data.pins(player.getUniqueID(),tooMany); fail(); } catch(IllegalArgumentException expected) {} assertTrue(data.pins(player.getUniqueID()).isEmpty());
    }

    @Test public void offlineUuidSubmissionNeverInventsInventoryOrConsumableXp() {
        UUID recipient=UUID.randomUUID(); QuestCore.Task energy=task("energy",null,10,false),items=task("item","minecraft:iron_ingot",2,true),xp=task("xp",null,5,true);
        quest.dependencies.add("earlier"); service.creditTask(recipient,quest.id,energy.id,10); assertEquals(0,data.value(recipient,quest.id,energy.id)); data.complete(recipient,"earlier");
        service.creditTask(recipient,quest.id,energy.id,10); ItemStack input=new ItemStack(Items.iron_ingot,2);
        assertEquals(2,service.acceptItems(recipient,quest.id,items.id,input,true)); assertEquals(2,input.stackSize); assertEquals(0,data.value(recipient,quest.id,items.id));
        assertEquals(2,service.acceptItems(recipient,quest.id,items.id,input,false)); assertEquals(0,input.stackSize); assertFalse(service.finishStored(recipient,quest,100));
        quest.tasks.remove(xp); assertTrue(service.finishStored(recipient,quest,100)); assertFalse(service.finishStored(recipient,quest,101)); assertEquals(10,data.value(recipient,quest.id,energy.id)); assertTrue(data.completed(recipient).contains(quest.id));
        assertTrue(data.completed(player.getUniqueID()).isEmpty());
    }
    @Test public void offlineStoredCompletionKeepsAnyTaskAndNonStickyConditionsSeparate() {
        UUID recipient=UUID.randomUUID(); QuestCore.Task live=task("dimension",null,1,false); live.dimension=-1; live.sticky=false; QuestCore.Task energy=task("energy",null,4,false); quest.anyTask=true;
        service.creditTask(recipient,quest.id,energy.id,4); assertTrue(service.finishStored(recipient,quest,100)); assertFalse(data.taskDone(recipient,quest.id,live.id));
        QuestCore.Quest other=new QuestCore.Quest(); other.id="live"; other.title="Live condition"; other.tasks.add(live); service.quests.put(other.id,other); data.markTask(recipient,other.id,live.id);
        assertFalse(service.finishStored(recipient,other,100)); live.sticky=true; assertTrue(service.finishStored(recipient,other,100));
    }
    @Test public void offlineTerminalAcceptsRfFluidAndEuAndCompletesPersistedRequirements() {
        QuestCore.Task rf=task("energy",null,100,false),fluid=task("fluid","water",250,false),eu=task("ic2_energy",null,2,false); QuestDevice tile=terminalTile(); doReturn(null).when(tile).player();
        try {
            assertEquals(100,tile.receiveEnergy(ForgeDirection.UP,120,true)); assertEquals(0,data.value(tile.owner,quest.id,rf.id)); assertEquals(100,tile.receiveEnergy(ForgeDirection.UP,120,false));
            tile.task=fluid.id; assertEquals(250,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.WATER,500),true));
            tile.task=eu.id; assertEquals(0,tile.injectEnergy(ForgeDirection.UP,.4,32),0); assertEquals(1.6,tile.getDemandedEnergy(),1e-9); assertEquals(0,tile.injectEnergy(ForgeDirection.UP,1.6,32),0); assertEquals(2,data.value(tile.owner,quest.id,eu.id));
            for(int i=0;i<20;i++) tile.updateEntity(); assertTrue(data.completed(tile.owner).contains(quest.id)); assertEquals(0,tile.receiveEnergy(ForgeDirection.UP,100,false));
        } finally { QuestsMod.service=null; }
    }
    @Test public void offlineChestWaitsForItsBoundProfileAndThenConsumesItsOwnInventory() {
        QuestCore.Task input=task("item","minecraft:iron_ingot",2,true); QuestStorage chest=new QuestStorage(false); chest.owner=UUID.randomUUID(); chest.setWorldObj(player.worldObj); chest.bindingProfile="expert"; chest.setInventorySlotContents(26,new ItemStack(Items.iron_ingot,2)); when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(7,"offline_chest")); QuestsMod.service=service;
        try { for(int i=0;i<20;i++) chest.updateEntity(); assertEquals(2,chest.getStackInSlot(26).stackSize); chest.bindingProfile="default"; for(int i=0;i<20;i++) chest.updateEntity(); assertNull(chest.getStackInSlot(26)); assertTrue(data.completed(chest.owner).contains(quest.id)); assertEquals(2,data.value(chest.owner,quest.id,input.id)); assertTrue(data.completed(player.getUniqueID()).isEmpty()); }
        finally { QuestsMod.service=null; }
    }
    @Test public void nativeScriptApiCanCreditAnOfflineUuidWithoutCreatingAPlayerEntity() {
        QuestCore.Task energy=task("energy",null,5,false); UUID recipient=UUID.randomUUID(); QuestsMod.service=service;
        try { new QuestScripts.API().creditId(recipient.toString(),quest.id,energy.id,5); assertEquals(5,data.value(recipient,quest.id,energy.id)); assertEquals(0,data.value(player.getUniqueID(),quest.id,energy.id)); }
        finally { QuestsMod.service=null; }
    }

    @Test public void latchedFluidRequirementDoesNotConsumeMoreWhileOtherRequirementsWait() {
        QuestCore.Task fluid=task("fluid","water",250,false); task("dimension",null,1,false).dimension=-1; QuestDevice tile=terminalTile(); data.markTask(player.getUniqueID(),quest.id,fluid.id);
        try { assertTrue(service.active(player,quest)); assertEquals(0,tile.fill(ForgeDirection.UP,new FluidStack(FluidRegistry.WATER,250),true)); assertFalse(tile.canFill(ForgeDirection.UP,FluidRegistry.WATER)); assertEquals(0,data.value(player.getUniqueID(),quest.id,fluid.id)); }
        finally { QuestsMod.service=null; }
    }

}
