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
    @BeforeClass public static void boot() { Bootstrap.func_151354_b(); }
    @Before public void setup() {
        player=mock(EntityPlayerMP.class);
        when(player.getUniqueID()).thenReturn(UUID.randomUUID());
        when(player.getRNG()).thenReturn(new Random(1));
        player.inventory=new InventoryPlayer(player);
        player.inventoryContainer=mock(Container.class);
        player.worldObj=mock(World.class);
        when(player.worldObj.getTotalWorldTime()).thenReturn(100L);
        data=new QuestProgress(); service=spy(new QuestService(new File("unused.json")));
        doReturn(data).when(service).progress();
        quest=new QuestCore.Quest(); quest.id="test"; quest.title="Test";
        service.quests=new LinkedHashMap<String,QuestCore.Quest>(); service.quests.put(quest.id,quest);
    }
    private QuestCore.Task task(String type,String target,long count,boolean consume) {
        QuestCore.Task t=new QuestCore.Task(); t.id="t"+quest.tasks.size(); t.type=type; t.target=target; t.count=count; t.consume=consume; quest.tasks.add(t); return t;
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
        doReturn(player).when(tile).player(); QuestsMod.service=service;
        try {
            assertEquals(100,tile.receiveEnergy(ForgeDirection.NORTH,200,true)); assertEquals(0,data.value(tile.owner,quest.id,energy.id));
            assertEquals(100,tile.receiveEnergy(ForgeDirection.NORTH,200,false)); assertEquals(100,data.value(tile.owner,quest.id,energy.id));
            assertEquals(0,tile.receiveEnergy(ForgeDirection.NORTH,10,false));
        } finally { QuestsMod.service=null; }
    }
    @Test public void fluidTerminalRejectsWrongFluidAndSimulationIsReadOnly() {
        QuestCore.Task fluid=task("fluid","water",1000,false);
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=fluid.id; tile.setWorldObj(player.worldObj);
        doReturn(player).when(tile).player(); QuestsMod.service=service;
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
    private QuestDevice euTile() {
        QuestDevice tile=spy(new QuestDevice()); tile.owner=player.getUniqueID(); tile.quest=quest.id; tile.task=quest.tasks.get(0).id; tile.setWorldObj(player.worldObj); doReturn(player).when(tile).player();
        when(player.worldObj.getBlock(0,0,0)).thenReturn(new QuestBlocks.DeviceBlock(0,"eu_test")); QuestsMod.service=service; return tile;
    }
    @Test public void euPacketsCreditOnlyAcceptedWholeUnitsAndRetainFractions() {
        task("ic2_energy",null,2,false); QuestDevice tile=euTile();
        try {
            assertEquals(0,tile.injectEnergy(ForgeDirection.UP,.4,32),0); assertEquals(0,data.value(player.getUniqueID(),quest.id,"t0"));
            assertEquals(1.6,tile.getDemandedEnergy(),1e-9); assertEquals(0,tile.injectEnergy(ForgeDirection.UP,.6,32),0);
            assertEquals(1,data.value(player.getUniqueID(),quest.id,"t0")); assertEquals(9,tile.injectEnergy(ForgeDirection.UP,10,32),0);
            assertEquals(2,data.value(player.getUniqueID(),quest.id,"t0")); assertEquals(0,tile.getDemandedEnergy(),0); assertEquals(5,tile.injectEnergy(ForgeDirection.UP,5,32),0);
        } finally { QuestsMod.service=null; }
    }
    @Test public void euFractionsSurviveNbtAndAreNotAppliedToAnotherTask() {
        task("ic2_energy",null,4,false); QuestDevice tile=euTile();
        try {
            tile.injectEnergy(ForgeDirection.UP,.5,32); cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(tile.getClass(),"sigmarixquests:test-eu"); NBTTagCompound saved=new NBTTagCompound(); tile.writeToNBT(saved);
            QuestDevice restored=spy(new QuestDevice()); restored.readFromNBT(saved); restored.setWorldObj(player.worldObj); doReturn(player).when(restored).player();
            assertEquals(3.5,restored.getDemandedEnergy(),0); restored.injectEnergy(ForgeDirection.UP,.5,32); assertEquals(1,data.value(player.getUniqueID(),quest.id,"t0"));
            QuestCore.Task another=task("ic2_energy",null,4,false); restored.task=another.id; assertEquals(4,restored.getDemandedEnergy(),0);
        } finally { QuestsMod.service=null; }
    }
    @Test public void euIsRejectedForOfflineOwnersWrongTasksAndImpossibleAmounts() {
        task("energy",null,4,false); QuestDevice tile=euTile();
        try {
            assertEquals(0,tile.getDemandedEnergy(),0); assertEquals(5,tile.injectEnergy(ForgeDirection.UP,5,32),0);
            quest.tasks.get(0).type="ic2_energy"; doReturn(null).when(tile).player(); assertEquals(0,tile.getDemandedEnergy(),0);
            doReturn(player).when(tile).player(); tile.injectEnergy(ForgeDirection.UP,Double.MAX_VALUE,32); assertEquals(0,data.value(player.getUniqueID(),quest.id,"t0"));
        } finally { QuestsMod.service=null; }
    }
}
