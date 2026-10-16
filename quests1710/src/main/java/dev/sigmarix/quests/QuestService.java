package dev.sigmarix.quests;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.command.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.*;

/** All mutations, inventory checks and rewards run on the server thread. */
public class QuestService {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public QuestCore.Book book = new QuestCore.Book();
    public Map<String, QuestCore.Quest> quests = Collections.emptyMap();
    public long revision;
    private final File config;
    public QuestService(File config) { this.config = config; }
    public void load() throws IOException {
        if(config.length()>900000) throw new IOException("Quest book exceeds 900 KB");
        byte[] source=Files.readAllBytes(config.toPath());
        QuestNetwork.guardJson(new String(source,StandardCharsets.UTF_8));
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(source), StandardCharsets.UTF_8)) {
            JsonObject root=new JsonParser().parse(reader).getAsJsonObject();
            QuestCore.Book next=JSON.fromJson(root,QuestCore.Book.class);
            if(!root.has("format")) for(int i=0;i<next.quests.size();i++) { next.quests.get(i).x=(i%6)*3; next.quests.get(i).y=(i/6)*2; }
            install(next);
        }
    }
    private void install(QuestCore.Book next) {
        Map<String, QuestCore.Quest> validated = validate(next);
        book = next; quests = validated; revision++;
    }
    public void save(QuestCore.Book next, long expectedRevision) throws IOException {
        if (expectedRevision != revision) throw new IllegalArgumentException("Book changed; refresh before editing");
        validate(next);
        byte[] bytes = JSON.toJson(next).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 900000) throw new IllegalArgumentException("Book too large (900 KB maximum)");
        Path target = config.toPath(), temporary = target.resolveSibling("quests.json.tmp");
        if (Files.exists(target)) Files.copy(target, target.resolveSibling("quests.json.bak"), StandardCopyOption.REPLACE_EXISTING);
        Files.write(temporary, bytes);
        try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        install(next);
    }
    private Map<String, QuestCore.Quest> validate(QuestCore.Book next) {
        if (next == null) throw new IllegalArgumentException("Empty book");
        Map<String, QuestCore.Quest> result = next.validate();
        for (QuestCore.Quest q : next.quests) {
            item(q.icon);
            for (QuestCore.Task t : q.tasks) {
                switch (t.type) {
                    case "item":
                        if(t.target!=null && !t.target.isEmpty()) validateItemTarget(t.target);
                        for(String target:t.targets) validateItemTarget(target);
                        tag(t.nbt); break;
                    case "kill": if (!EntityList.stringToClassMapping.containsKey(t.target)) throw new IllegalArgumentException("Unknown entity: " + t.target); break;
                    case "fluid": tag(t.nbt); if (!FluidRegistry.isFluidRegistered(t.target)) throw new IllegalArgumentException("Unknown fluid: " + t.target); break;
                    case "observation": case "interaction":
                        if(t.entity) { if(!EntityList.stringToClassMapping.containsKey(t.target)) throw new IllegalArgumentException("Unknown entity: "+t.target); }
                        else block(t.target); break;
                    case "stat": case "achievement": stat(t.target, t.type.equals("achievement")); break;
                    case "custom": if (!QuestHooks.TASKS.containsKey(t.target)) throw new IllegalArgumentException("Unregistered task hook: " + t.target); break;
                    default: break;
                }
            }
            for (QuestCore.Reward r : q.rewards) validateReward(r);
        }
        for (List<QuestCore.Reward> table : next.lootTables.values()) for (QuestCore.Reward r : table) validateReward(r);
        QuestNetwork.Snapshot wire=new QuestNetwork.Snapshot(); wire.book=next;
        for(QuestCore.Chapter c:next.chapters) wire.chapterStates.put(c.id,"not_started");
        for(QuestCore.Quest q:next.quests) { wire.states.put(q.id,"available"); for(QuestCore.Reward r:q.rewards) wire.rewardStates.put(q.id+":"+r.id,false); for(QuestCore.Task t:q.tasks) { wire.counters.put(q.id+":"+t.id,Long.MAX_VALUE); wire.taskStates.put(q.id+":"+t.id,false); } }
        try { QuestNetwork.encode(JSON.toJson(wire)); } catch(IOException e) { throw new IllegalArgumentException("Book cannot fit a safe network snapshot",e); }
        return result;
    }
    private void validateReward(QuestCore.Reward r) {
        if (r.type.equals("item")) { item(r.item); tag(r.nbt); }
        if (r.type.equals("achievement")) stat(r.target, true);
        if (r.type.equals("custom") && !QuestHooks.REWARDS.containsKey(r.target)) throw new IllegalArgumentException("Unregistered reward hook: " + r.target);
        for (QuestCore.Reward child : r.options) validateReward(child);
    }
    private static void validateItemTarget(String id) {
        if(id==null || id.isEmpty()) throw new IllegalArgumentException("Empty item target");
        if(id.startsWith("ore:")) { if(net.minecraftforge.oredict.OreDictionary.getOres(id.substring(4)).isEmpty()) throw new IllegalArgumentException("Unknown/empty ore dictionary entry: "+id); }
        else item(id);
    }
    public static Item item(String id) {
        if (id == null || !Item.itemRegistry.containsKey(id)) throw new IllegalArgumentException("Unknown item: " + id);
        return (Item) Item.itemRegistry.getObject(id);
    }
    public static Block block(String id) {
        if (id == null || !Block.blockRegistry.containsKey(id)) throw new IllegalArgumentException("Unknown block: " + id);
        return (Block) Block.blockRegistry.getObject(id);
    }
    public static StatBase stat(String id, boolean achievement) {
        StatBase s = StatList.func_151177_a(id);
        if (s == null || achievement && !(s instanceof Achievement)) throw new IllegalArgumentException("Unknown statistic/achievement: " + id);
        return s;
    }
    public static NBTTagCompound tag(String value) {
        if (value.isEmpty()) return null;
        try {
            QuestNetwork.guardJson(value);
            NBTBase tag = JsonToNBT.func_150315_a(value);
            if (tag instanceof NBTTagCompound) return (NBTTagCompound) tag;
        } catch (NBTException | IOException e) { throw new IllegalArgumentException("Invalid item NBT: " + e.getMessage()); }
        throw new IllegalArgumentException("Item NBT must be a compound");
    }
    public QuestProgress progress() {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        QuestProgress data = (QuestProgress) world.mapStorage.loadData(QuestProgress.class, "sigmarixquests");
        if (data == null) { data = new QuestProgress(); world.mapStorage.setData("sigmarixquests", data); }
        return data;
    }
    public static void say(ICommandSender sender, String message) { sender.addChatMessage(new ChatComponentText(message)); }
    public boolean active(EntityPlayerMP p, QuestCore.Quest q) {
        Set<String> done = progress().completed(p.getUniqueID());
        return !done.contains(q.id) && unlocked(p.getUniqueID(),q);
    }
    public QuestDependencies dependencies(final UUID id) {
        final QuestProgress data=progress();
        return new QuestDependencies(book,quests,new QuestDependencies.Progress() {
            public boolean completed(String quest) { return data.completed(id).contains(quest); }
            public boolean started(String quest) { return data.started(id,quest); }
            public boolean task(String quest,String task,boolean started) {
                QuestCore.Quest q=quests.get(quest); if(q==null) return false;
                for(QuestCore.Task t:q.tasks) if(t.id.equals(task)) {
                    boolean complete=data.taskDone(id,quest,task) || data.value(id,quest,task)>=t.count || !q.anyTask && data.completed(id).contains(quest);
                    return started ? complete || data.value(id,quest,task)>0 : complete;
                }
                return false;
            }
        });
    }
    public boolean unlocked(UUID id,QuestCore.Quest q) { return dependencies(id).unlocked(q); }
    public String chapterState(UUID id,String chapter) {
        QuestDependencies graph=dependencies(id);
        return graph.chapter(chapter,false)?"complete":graph.chapter(chapter,true)?"started":"not_started";
    }
    public void start(EntityPlayerMP p,QuestCore.Quest q) {
        if(!active(p,q)) throw new IllegalArgumentException("Quest is locked or complete"); progress().start(p.getUniqueID(),q.id);
    }
    public void confirm(EntityPlayerMP p,QuestCore.Quest q,String task) {
        if(!active(p,q)) throw new IllegalArgumentException("Quest is locked or complete");
        for(QuestCore.Task t:q.tasks) if(t.id.equals(task) && t.type.equals("check")) { progress().add(p.getUniqueID(),q.id,t.id,t.count,t.count); progress().markTask(p.getUniqueID(),q.id,t.id); return; }
        throw new IllegalArgumentException("Not a confirmation task");
    }
    public boolean individual(QuestCore.Quest q,QuestCore.Reward r) { return (r.mode.equals("inherit")?q.rewardMode:r.mode).equals("individual"); }
    private String rewardId(QuestCore.Reward r,int index) { return r.id==null?"r"+index:r.id; }
    public boolean claimed(UUID id,QuestCore.Quest q) {
        QuestProgress data=progress();
        if(q.rewards.isEmpty()) return data.claimed(id,q.id);
        for(int i=0;i<q.rewards.size();i++) if(!data.rewardClaimed(id,q.id,rewardId(q.rewards.get(i),i),individual(q,q.rewards.get(i)))) return false;
        return true;
    }
    public void credit(EntityPlayerMP p, String type, String target, long amount) {
        QuestProgress data = progress();
        for (QuestCore.Quest q : quests.values()) if (active(p, q)) for (QuestCore.Task t : q.tasks)
            if (t.type.equals(type) && (target == null || target.equals(t.target))) {
                if(type.equals("kill") && data.value(p.getUniqueID(),q.id,t.id)==0) data.add(p.getUniqueID(),q.id,t.id,data.kills(p.getUniqueID(),q.id,q.tasks.indexOf(t)),t.count);
                data.add(p.getUniqueID(), q.id, t.id, amount, t.count);
            }
    }
    public void creditTask(EntityPlayerMP p, String quest, String task, long amount) {
        QuestCore.Quest q = quests.get(quest);
        if (q == null || !active(p, q)) return;
        for (QuestCore.Task t : q.tasks) if (t.id.equals(task) && (t.type.equals("energy") || t.type.equals("fluid") || t.type.equals("custom")))
            progress().add(p.getUniqueID(), quest, task, amount, t.count);
    }
    public long counter(EntityPlayerMP p, QuestCore.Quest q, QuestCore.Task t) {
        long value = progress().value(p.getUniqueID(), q.id, t.id);
        // Preserve the first prototype's indexed kill progress when migrating definitions.
        if (t.type.equals("kill") && value == 0) {
            QuestCore.Quest canonical=quests.get(q.id); int index=0;
            if(canonical!=null) for(int i=0;i<canonical.tasks.size();i++) if(t.id.equals(canonical.tasks.get(i).id)) index=i;
            value=progress().kills(p.getUniqueID(),q.id,index);
        }
        return value;
    }
    private static ItemStack[] copy(ItemStack[] input) {
        ItemStack[] out = new ItemStack[input.length];
        for (int i=0; i<input.length; i++) out[i] = input[i] == null ? null : input[i].copy();
        return out;
    }
    public static boolean matches(ItemStack stack, QuestCore.Task t) {
        if(stack==null) return false;
        List<String> targets=new ArrayList<String>(t.targets); if(t.target!=null && !t.target.isEmpty()) targets.add(t.target);
        boolean matched=false;
        for(String target:targets) {
            if(target.startsWith("ore:")) {
                for(ItemStack ore:net.minecraftforge.oredict.OreDictionary.getOres(target.substring(4)))
                    if(stack.getItem()==ore.getItem() && (t.ignoreDamage || ore.getItemDamage()==net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE || stack.getItemDamage()==ore.getItemDamage())) matched=true;
            } else if(stack.getItem()==item(target) && (t.ignoreDamage || stack.getItemDamage()==t.meta)) matched=true;
        }
        if(!matched) return false;
        return nbtMatches(t.nbt,t.nbtMode,stack.getTagCompound());
    }
    public static boolean nbtMatches(String nbt,String mode,NBTTagCompound actual) {
        if(nbt.isEmpty() || mode.equals("ignore")) return true;
        NBTTagCompound expected=tag(nbt);
        return mode.equals("subset")?subset(expected,actual):expected.equals(actual);
    }
    private static boolean subset(NBTBase expected,NBTBase actual) {
        if(expected instanceof NBTTagCompound && actual instanceof NBTTagCompound) {
            NBTTagCompound e=(NBTTagCompound)expected,a=(NBTTagCompound)actual;
            for(Object key:e.func_150296_c()) if(!a.hasKey(key.toString()) || !subset(e.getTag(key.toString()),a.getTag(key.toString()))) return false;
            return true;
        }
        return expected.equals(actual);
    }
    private static void reduce(ItemStack[] inventory, int slot, int count) {
        inventory[slot].stackSize -= count;
        if (inventory[slot].stackSize <= 0) inventory[slot] = null;
    }
    private boolean ordinary(EntityPlayerMP p, QuestCore.Quest q, QuestCore.Task t) {
        if(t.sticky && counter(p,q,t)>=t.count) return true;
        switch (t.type) {
            case "check": return !t.manual || counter(p,q,t)>=t.count;
            case "kill": case "interaction": case "energy": return counter(p,q,t) >= t.count;
            case "custom": return counter(p,q,t) >= t.count || QuestHooks.TASKS.get(t.target).complete(p,t);
            case "dimension": return p.dimension == t.dimension;
            case "biome": return t.target.equalsIgnoreCase(p.worldObj.getBiomeGenForCoords(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posZ)).biomeName) ||
                t.target.equals(Integer.toString(p.worldObj.getBiomeGenForCoords(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posZ)).biomeID));
            case "location": return (t.ignoreDimension || p.dimension==t.dimension) && (t.box ? p.posX>=t.x && p.posX<t.x+t.width && p.posY>=t.y && p.posY<t.y+t.height && p.posZ>=t.z && p.posZ<t.z+t.depth : p.getDistanceSq(t.x,t.y,t.z)<=t.radius*t.radius);
            case "stat": return p.func_147099_x().writeStat(stat(t.target,false)) >= t.count;
            case "achievement": return p.func_147099_x().hasAchievementUnlocked((Achievement)stat(t.target,true));
            case "observation": return looking(p,t) && (t.lookTicks==0 || progress().value(p.getUniqueID(),q.id,t.id+"_look")>=t.lookTicks);
            default: return false;
        }
    }
    private boolean looking(EntityPlayerMP p,QuestCore.Task t) {
        double distance=Math.min(t.radius,64); MovingObjectPosition blockHit=p.rayTrace(distance,1);
        if(!t.entity) return blockHit!=null && blockHit.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK && p.worldObj.getBlock(blockHit.blockX,blockHit.blockY,blockHit.blockZ)==block(t.target) && (t.ignoreDamage || p.worldObj.getBlockMetadata(blockHit.blockX,blockHit.blockY,blockHit.blockZ)==t.meta);
        Vec3 eye=Vec3.createVectorHelper(p.posX,p.posY+p.getEyeHeight(),p.posZ),look=p.getLook(1),end=eye.addVector(look.xCoord*distance,look.yCoord*distance,look.zCoord*distance);
        double closest=blockHit==null?distance:eye.distanceTo(blockHit.hitVec); net.minecraft.entity.Entity target=null;
        for(Object raw:p.worldObj.getEntitiesWithinAABBExcludingEntity(p,p.boundingBox.addCoord(look.xCoord*distance,look.yCoord*distance,look.zCoord*distance).expand(1,1,1))) {
            net.minecraft.entity.Entity e=(net.minecraft.entity.Entity)raw; if(!e.canBeCollidedWith()) continue;
            double border=e.getCollisionBorderSize(); net.minecraft.util.AxisAlignedBB bounds=e.boundingBox.expand(border,border,border);
            MovingObjectPosition hit=bounds.calculateIntercept(eye,end); double d=bounds.isVecInside(eye)?0:hit==null?Double.MAX_VALUE:eye.distanceTo(hit.hitVec);
            if(d<closest) { closest=d; target=e; }
        }
        return target!=null && t.target.equals(EntityList.getEntityString(target));
    }
    public void observe(EntityPlayerMP p) {
        for(QuestCore.Quest q:quests.values()) if(active(p,q)) for(QuestCore.Task t:q.tasks) if(t.type.equals("observation") && t.lookTicks>0 && (!t.sticky || counter(p,q,t)<t.count)) {
            if(looking(p,t)) {
                progress().add(p.getUniqueID(),q.id,t.id+"_look",1,t.lookTicks);
                if(progress().value(p.getUniqueID(),q.id,t.id+"_look")>=t.lookTicks) progress().add(p.getUniqueID(),q.id,t.id,t.count,t.count);
            } else progress().add(p.getUniqueID(),q.id,t.id+"_look",0,0);
        }
    }
    public void crafted(EntityPlayerMP p,ItemStack output) {
        for(QuestCore.Quest q:quests.values()) if(active(p,q)) for(QuestCore.Task t:q.tasks)
            if(t.type.equals("item") && t.craftingOnly && matches(output,t)) progress().add(p.getUniqueID(),q.id,t.id,output.stackSize,t.count);
    }
    /** Latch visited locations/dimensions and observed state independently of final completion. */
    public void detect(EntityPlayerMP p) {
        for(QuestCore.Quest q:quests.values()) if(active(p,q)) for(QuestCore.Task t:q.tasks) {
            if(!t.sticky || progress().taskDone(p.getUniqueID(),q.id,t.id)) continue;
            boolean persistent=counter(p,q,t)>=t.count;
            boolean readOnly=!t.type.equals("check") && (!t.type.equals("item") && !t.type.equals("xp") && !t.type.equals("fluid") || !t.consume);
            if(!persistent && readOnly) {
                QuestCore.Quest one=JSON.fromJson(JSON.toJson(q),QuestCore.Quest.class); one.anyTask=false; one.tasks=Collections.singletonList(t);
                persistent=check(p,one,false);
            }
            if(persistent) progress().markTask(p.getUniqueID(),q.id,t.id);
        }
    }
    /** Preflight on copies; no consumption if any requirement is incomplete. */
    public boolean check(EntityPlayerMP p, QuestCore.Quest q, boolean commit) {
        if (!active(p,q)) return false;
        if(q.anyTask) {
            for(QuestCore.Task t:q.tasks) {
                QuestCore.Quest single=JSON.fromJson(JSON.toJson(q),QuestCore.Quest.class);
                single.anyTask=false; single.tasks=Collections.singletonList(t);
                if(check(p,single,commit)) return true;
            }
            return false;
        }
        ItemStack[] reserved = copy(p.inventory.mainInventory), result = copy(p.inventory.mainInventory);
        List<ItemStack> containers = new ArrayList<ItemStack>();
        int xp = XP.total(p.experienceLevel, p.experience), xpResult = xp, xpOriginal = xp;
        for (QuestCore.Task t : q.tasks) {
            if(t.sticky && progress().taskDone(p.getUniqueID(),q.id,t.id)) continue;
            if (t.type.equals("item")) {
                if(t.craftingOnly) { if(counter(p,q,t)<t.count) return false; else continue; }
                long remaining = Math.max(0,t.count-(t.consume||t.craftingOnly?counter(p,q,t):0));
                for (int i=0; i<reserved.length && remaining>0; i++) if (matches(reserved[i],t)) {
                    int take = (int)Math.min(remaining,reserved[i].stackSize); remaining -= take; reduce(reserved,i,take);
                    if (t.consume) reduce(result,i,take);
                }
                if (remaining != 0) return false;
            } else if (t.type.equals("fluid")) {
                if(!QuestFluids.reserve(t,Math.max(0,t.count-counter(p,q,t)),reserved,result,containers)) return false;
            } else if (t.type.equals("xp")) {
                long needed=Math.max(0,t.count-counter(p,q,t));
                int cost;
                if (t.levels) { int level = XP.level(xp); if (level<needed) return false; cost=XP.base(level)-XP.base(level-(int)needed); }
                else cost = (int)needed;
                if (xp<cost) return false; xp-=cost; if (t.consume) xpResult-=cost;
            } else if (!ordinary(p,q,t)) return false;
        }
        if (commit) {
            System.arraycopy(result,0,p.inventory.mainInventory,0,result.length);
            if(xpResult != xpOriginal) XP.set(p,xpResult);
            for (ItemStack empty : containers) give(p,empty);
            p.inventory.markDirty(); p.inventoryContainer.detectAndSendChanges();
            for(QuestCore.Task t:q.tasks) progress().markTask(p.getUniqueID(),q.id,t.id);
            progress().complete(p.getUniqueID(),q.id); progress().timestamp(p.getUniqueID(),q.id,p.worldObj.getTotalWorldTime());
        }
        return true;
    }
    public int acceptItems(EntityPlayerMP p,String quest,String task,ItemStack stack,boolean simulate) {
        QuestCore.Quest q=quests.get(quest);
        if(q==null || !active(p,q) || stack==null) return 0;
        for(QuestCore.Task t:q.tasks) if(t.id.equals(task) && t.type.equals("item") && t.consume && !t.craftingOnly && matches(stack,t)) {
            int take=(int)Math.min(stack.stackSize,Math.max(0,t.count-counter(p,q,t)));
            if(!simulate && take>0) { stack.stackSize-=take; progress().add(p.getUniqueID(),q.id,t.id,take,t.count); }
            return take;
        }
        return 0;
    }
    public long submit(EntityPlayerMP p,QuestCore.Quest q,String task) {
        if(!active(p,q)) throw new IllegalArgumentException("Quest is not active");
        for(QuestCore.Task t:q.tasks) if(t.id.equals(task)) {
            if(!t.type.equals("item") || !t.consume) throw new IllegalArgumentException("Use submit for consuming item tasks; fluids/RF use a terminal");
            long total=0;
            for(int i=0;i<p.inventory.mainInventory.length;i++) {
                ItemStack stack=p.inventory.mainInventory[i]; total+=acceptItems(p,q.id,task,stack,false);
                if(stack!=null && stack.stackSize<=0) p.inventory.mainInventory[i]=null;
            }
            p.inventory.markDirty(); p.inventoryContainer.detectAndSendChanges(); return total;
        }
        throw new IllegalArgumentException("Unknown task");
    }
    public boolean finish(EntityPlayerMP p,QuestCore.Quest q) {
        if(!check(p,q,true)) return false;
        if(q.autoClaim) {
            try { claim(p,q,Collections.<Integer>emptyList()); }
            catch(IllegalArgumentException e) { say(p,"Quest complete; collect reward manually: "+e.getMessage()); }
        }
        return true;
    }
    public static void give(EntityPlayerMP player, ItemStack stack) {
        if (!player.inventory.addItemStackToInventory(stack)) player.dropPlayerItemWithRandomChoice(stack,false);
    }
    private void flatten(QuestCore.Reward r,Iterator<Integer> choices,Random random,List<QuestCore.Reward> out,int[] budget,int depth) {
        if(depth>32 || ++budget[0]>4096) throw new IllegalArgumentException("Reward expansion exceeds safe limit");
        if(r.type.equals("choice")) {
            if(!choices.hasNext()) throw new IllegalArgumentException("Choose a reward option (zero-based index)");
            int index=choices.next(); if(index<0 || index>=r.options.size()) throw new IllegalArgumentException("Invalid reward choice");
            flatten(r.options.get(index),choices,random,out,budget,depth+1); return;
        }
        if(!r.type.equals("loot") && !r.type.equals("random")) { out.add(r); return; }
        QuestCore.LootSettings settings=r.type.equals("loot")?book.lootSettings.get(r.target):null;
        List<QuestCore.Reward> options=r.type.equals("loot")?book.lootTables.get(r.target):r.options;
        int empty=settings==null?0:settings.emptyWeight,rolls=settings==null?1:settings.rolls,total=empty;
        for(QuestCore.Reward o:options) total=Math.addExact(total,o.weight);
        if(total<=0) throw new IllegalArgumentException("Empty loot distribution");
        for(int roll=0;roll<rolls;roll++) {
            int pick=random.nextInt(total); if(pick<empty) continue; pick-=empty;
            for(QuestCore.Reward o:options) { pick-=o.weight; if(pick<0) { flatten(o,choices,random,out,budget,depth+1); break; } }
        }
    }
    public void claim(EntityPlayerMP p, QuestCore.Quest q, List<Integer> choices) { claim(p,q,choices,null); }
    public void claim(EntityPlayerMP p, QuestCore.Quest q, List<Integer> choices,java.util.function.Consumer<ItemStack> itemSink) {
        QuestProgress data=progress(); UUID id=p.getUniqueID();
        if (!data.completed(id).contains(q.id) || claimed(id,q)) throw new IllegalArgumentException("Reward unavailable");
        List<QuestCore.Reward> leaves=new ArrayList<QuestCore.Reward>(); Iterator<Integer> selected=choices.iterator();
        List<Integer> pending=new ArrayList<Integer>(); boolean allShared=true; int[] budget={0};
        for(int i=0;i<q.rewards.size();i++) {
            QuestCore.Reward r=q.rewards.get(i); boolean personal=individual(q,r); allShared&=!personal;
            if(data.rewardClaimed(id,q.id,rewardId(r,i),personal)) continue;
            List<QuestCore.Reward> expanded=new ArrayList<QuestCore.Reward>(); flatten(r,selected,p.getRNG(),expanded,budget,0);
            pending.add(i); leaves.addAll(expanded);
        }
        if(selected.hasNext()) throw new IllegalArgumentException("Too many reward choices");
        for(QuestCore.Reward r:leaves) validateReward(r);
        for(int index:pending) { QuestCore.Reward r=q.rewards.get(index); data.claimReward(id,q.id,rewardId(r,index),individual(q,r)); }
        if(allShared) data.claim(id,q.id);
        applyRewards(p,leaves,itemSink);
    }
    public void openLoot(EntityPlayerMP p,String table,ItemStack crate) {
        if(!book.lootTables.containsKey(table)) throw new IllegalArgumentException("Unknown loot table");
        QuestCore.Reward roll=new QuestCore.Reward(); roll.type="loot"; roll.target=table;
        List<QuestCore.Reward> leaves=new ArrayList<QuestCore.Reward>();
        flatten(roll,Collections.<Integer>emptyList().iterator(),p.getRNG(),leaves,new int[]{0},0);
        for(QuestCore.Reward r:leaves) validateReward(r);
        if(crate.stackSize<=0) throw new IllegalArgumentException("Empty crate stack");
        crate.stackSize--; applyRewards(p,leaves,null);
    }
    private void applyRewards(EntityPlayerMP p,List<QuestCore.Reward> leaves,java.util.function.Consumer<ItemStack> itemSink) {
        UUID id=p.getUniqueID();
        for(QuestCore.Reward r:leaves) switch(r.type) {
            case "item":
                Item reward=item(r.item); ItemStack prototype=new ItemStack(reward,1,r.meta); prototype.setTagCompound(tag(r.nbt));
                boolean exists=false; for(ItemStack stack:p.inventory.mainInventory) if(stack!=null && stack.isItemEqual(prototype) && ItemStack.areItemStackTagsEqual(stack,prototype)) exists=true;
                if(r.onlyOne && exists) break;
                int remaining=r.count+(r.randomBonus>0?p.getRNG().nextInt(r.randomBonus+1):0);
                while(remaining>0) { int count=Math.min(remaining,reward.getItemStackLimit()); ItemStack stack=new ItemStack(reward,count,r.meta); stack.setTagCompound(tag(r.nbt)); if(itemSink==null) give(p,stack); else itemSink.accept(stack); remaining-=count; } break;
            case "xp": p.addExperience(r.count); break;
            case "xp_levels": p.addExperienceLevel(r.count); break;
            case "command":
                String command=r.target.replace("{player}",p.getCommandSenderName()).replace("{uuid}",id.toString()).replace("{x}",Integer.toString(MathHelper.floor_double(p.posX))).replace("{y}",Integer.toString(MathHelper.floor_double(p.posY))).replace("{z}",Integer.toString(MathHelper.floor_double(p.posZ)));
                MinecraftServer.getServer().getCommandManager().executeCommand(r.asPlayer?p:MinecraftServer.getServer(),command); break;
            case "achievement": grantAchievement(p,(Achievement)stat(r.target,true),new HashSet<Achievement>()); break;
            case "toast": say(p,r.target); break;
            case "custom": QuestHooks.REWARDS.get(r.target).grant(p,r); break;
            default: throw new IllegalStateException("Unexpected flattened reward");
        }
        p.inventoryContainer.detectAndSendChanges();
    }
    private void grantAchievement(EntityPlayerMP p, Achievement a, Set<Achievement> visited) {
        if(!visited.add(a)) return;
        if(a.parentAchievement!=null) grantAchievement(p,a.parentAchievement,visited);
        p.triggerAchievement(a);
    }
    public void repeat(EntityPlayerMP p,QuestCore.Quest q) {
        QuestProgress data=progress(); UUID id=p.getUniqueID();
        if(!q.repeatable || !claimed(id,q) || p.worldObj.getTotalWorldTime()-data.completedAt(id,q.id)<(long)q.cooldownSeconds*20)
            throw new IllegalArgumentException("Quest cannot be repeated yet");
        if(!data.team(id).isEmpty() && !data.leader(id)) throw new IllegalArgumentException("Only the team leader can repeat a quest");
        for(UUID member:data.members(id)) if(!claimed(member,q)) throw new IllegalArgumentException("Team members still have unclaimed rewards");
        data.reset(id,q.id);
    }
    public static final class XP {
        public static int cap(int level) { return level>=30 ? 62+(level-30)*7 : level>=15 ? 17+(level-15)*3 : 17; }
        public static int base(int level) {
            if(level<0 || level>20000) throw new IllegalArgumentException("XP level outside supported range");
            long result=0; for(int i=0;i<level;i++) result+=cap(i);
            if(result>Integer.MAX_VALUE) throw new IllegalArgumentException("XP overflow"); return (int)result;
        }
        public static int total(int level,float fraction) { return Math.addExact(base(level),Math.min(cap(level)-1,Math.round(Math.max(0,Math.min(fraction,0.999999f))*cap(level)))); }
        public static int level(int points) { int level=0; while(points>=cap(level) && level<20000) { points-=cap(level); level++; } return level; }
        public static void set(EntityPlayerMP player,int points) {
            int level=level(points); player.experienceLevel=level; player.experienceTotal=points; player.experience=(float)(points-base(level))/cap(level);
        }
    }
}
