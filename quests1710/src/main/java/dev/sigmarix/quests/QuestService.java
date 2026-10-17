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
    public static final Gson JSON = QuestJson.create();
    public QuestCore.Book book = new QuestCore.Book();
    public Map<String, QuestCore.Quest> quests = Collections.emptyMap();
    public long revision;
    private final Map<UUID,Map<String,Integer>> lookTimers=new HashMap<UUID,Map<String,Integer>>();
    private File config;
    private final File defaultConfig;
    public QuestService(File config) { this.config = this.defaultConfig = config; }
    private File profileFile(String key) {
        if(!QuestCore.validId(key)) throw new IllegalArgumentException("Invalid quest profile");
        return key.equals("default")?defaultConfig:new File(new File(defaultConfig.getParentFile(),"profiles"),key+".json");
    }
    public void reload() throws IOException { QuestScripts.reload(new File(defaultConfig.getParentFile(),"scripts"),this::load); }
    public void loadActiveProfile() throws IOException { config=profileFile(progress().profile()); load(); }
    public void switchProfile(String key) throws IOException {
        File source=profileFile(key); QuestService candidate=new QuestService(source); candidate.load();
        progress().profile(key); config=source; book=candidate.book; quests=candidate.quests; lookTimers.clear(); revision++;
    }
    public void createProfile(String key) throws IOException {
        File target=profileFile(key); Files.createDirectories(target.getParentFile().toPath());
        Files.write(target.toPath(),JSON.toJson(book).getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
    }
    public List<String> profiles() {
        List<String> result=new ArrayList<String>(); result.add("default");
        File[] files=new File(defaultConfig.getParentFile(),"profiles").listFiles((dir,name)->name.endsWith(".json"));
        if(files!=null) for(File file:files) { String name=file.getName().substring(0,file.getName().length()-5); if(QuestCore.validId(name) && !name.equals("default")) result.add(name); }
        Collections.sort(result); return result;
    }
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
        book = next; quests = validated; lookTimers.clear(); revision++;
    }
    public void save(QuestCore.Book next, long expectedRevision) throws IOException {
        if (expectedRevision != revision) throw new IllegalArgumentException("Book changed; refresh before editing");
        validate(next);
        byte[] bytes = JSON.toJson(next).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 900000) throw new IllegalArgumentException("Book too large (900 KB maximum)");
        Path target = config.toPath(), temporary = target.resolveSibling(target.getFileName()+".tmp");
        if (Files.exists(target)) Files.copy(target, target.resolveSibling(target.getFileName()+".bak"), StandardCopyOption.REPLACE_EXISTING);
        Files.write(temporary, bytes);
        try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        install(next);
    }
    private Map<String, QuestCore.Quest> validate(QuestCore.Book next) {
        if (next == null) throw new IllegalArgumentException("Empty book");
        Map<String, QuestCore.Quest> result = next.validate();
        for (QuestCore.Quest q : next.quests) {
            item(q.icon); tag(q.iconNbt);
            for(QuestPresentation.GuidePage page:q.richPages) for(QuestPresentation.GuideElement element:page.elements) if(element.type.equals("item")) { item(element.item); tag(element.nbt); }
            for (QuestCore.Task t : q.tasks) {
                if(!t.icon.isEmpty()) item(t.icon); tag(t.iconNbt);
                switch (t.type) {
                    case "item":
                        if(t.target!=null && !t.target.isEmpty()) validateItemTarget(t.target);
                        for(String target:t.targets) validateItemTarget(target);
                        tag(t.nbt); if(t.filter!=null) t.filter.validate(true); break;
                    case "npc_quest": case "npc_dialog": case "npc_faction": QuestNPCs.require(); break;
                    case "buildcraft_mj": if(!cpw.mods.fml.common.Loader.isModLoaded("BuildCraft|Core")) throw new IllegalArgumentException("MJ tasks require BuildCraft 7.1.23 (1.7.10 RF bridge)"); break;
                    case "emc": QuestProjectE.validateAPI(); break;
                    case "botania_mana": if(!cpw.mods.fml.common.Loader.isModLoaded("Botania")) throw new IllegalArgumentException("Mana tasks require Botania"); break;
                    case "ic2_energy": if(!cpw.mods.fml.common.Loader.isModLoaded("IC2")) throw new IllegalArgumentException("IC2 EU tasks require IndustrialCraft 2"); break;
                    case "kill": if (!EntityList.stringToClassMapping.containsKey(t.target)) throw new IllegalArgumentException("Unknown entity: " + t.target); break;
                    case "fluid": tag(t.nbt); if (!FluidRegistry.isFluidRegistered(t.target)) throw new IllegalArgumentException("Unknown fluid: " + t.target); break;
                    case "observation": case "interaction":
                        if(QuestRayMatcher.type(t).equals("entity_id")) { if(!EntityList.stringToClassMapping.containsKey(t.target)) throw new IllegalArgumentException("Unknown entity: "+t.target); }
                        else if(QuestRayMatcher.type(t).equals("block_id")) block(t.target); tag(t.nbt); break;
                    case "stat": case "achievement": stat(t.target, t.type.equals("achievement")); break;
                    case "custom": if (!QuestHooks.TASKS.containsKey(t.target)) throw new IllegalArgumentException("Unregistered task hook: " + t.target); break;
                    default: break;
                }
            }
            for (QuestCore.Reward r : q.rewards) validateReward(r);
        }
        for(QuestCore.Reward reward:next.emergencyItems) validateReward(reward);
        for (List<QuestCore.Reward> table : next.lootTables.values()) for (QuestCore.Reward r : table) validateReward(r);
        QuestNetwork.Snapshot wire=new QuestNetwork.Snapshot(); wire.book=next;
        for(QuestCore.Chapter c:next.chapters) wire.chapterStates.put(c.id,"not_started");
        for(QuestCore.Quest q:next.quests) { wire.states.put(q.id,"available"); for(QuestCore.Reward r:q.rewards) wire.rewardStates.put(q.id+":"+r.id,false); for(QuestCore.Task t:q.tasks) { wire.counters.put(q.id+":"+t.id,Long.MAX_VALUE); wire.taskStates.put(q.id+":"+t.id,false); } }
        for(QuestCore.Quest q:next.quests) if(wire.pinned.size()<16) { wire.pinned.add(q.id); wire.percentages.put(q.id,100); }
        try { String payload=JSON.toJson(wire); if(payload.getBytes(StandardCharsets.UTF_8).length>QuestNetwork.MAX_BYTES-32768 || QuestNetwork.encode(payload).length>QuestNetwork.MAX_COMPRESSED-16384) throw new IOException("Book needs room for live snapshot metadata"); } catch(IOException e) { throw new IllegalArgumentException("Book cannot fit a safe network snapshot",e); }
        return result;
    }
    private void validateReward(QuestCore.Reward r) {
        if(!r.icon.isEmpty()) item(r.icon); tag(r.iconNbt);
        if (r.type.startsWith("npc_")) QuestNPCs.validate(r);
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
        return QuestSnbt.parse(value);
    }

    public double emcBalance(EntityPlayerMP player) { return QuestProjectE.balance(player); }
    public void setEmc(EntityPlayerMP player,double value) { QuestProjectE.set(player,value); }
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
    public boolean visible(UUID viewer,QuestCore.Quest quest) { return QuestPresentation.visibleChapter(book,quest.chapter) && (!quest.hidden || progress().completed(viewer).contains(quest.id) || unlocked(viewer,quest)); }
    public boolean bindable(UUID viewer,String reference) {
        validateProgressReference(reference);
        if(reference.equals("book:")) return true;
        if(reference.startsWith("chapter:")) return QuestPresentation.visibleChapter(book,reference.substring(8));
        for(String owner:QuestDependencies.owners(book,quests,reference)) if(!visible(viewer,quests.get(owner))) return false;
        return true;
    }
    public void validateProgressReference(String reference) {
        if(reference==null || reference.length()>160) throw new IllegalArgumentException("Invalid progress reference");
        if(!reference.equals("book:")) QuestDependencies.owners(book,quests,reference);
    }
    public double objectProgress(UUID owner,String reference) {
        try { validateProgressReference(reference); } catch(IllegalArgumentException missing) { return 0; }
        if(reference.equals("book:") || reference.startsWith("chapter:")) {
            String chapter=reference.startsWith("chapter:")?reference.substring(8):""; double sum=0; int count=0;
            for(QuestCore.Quest quest:quests.values()) if(QuestCore.required(quest) && (chapter.isEmpty() || QuestPresentation.within(book,quest.chapter,chapter))) { sum+=questProgress(owner,quest); count++; }
            return count==0?1:sum/count;
        }
        if(reference.startsWith("task:")) {
            String[] parts=reference.substring(5).split("/",-1); QuestCore.Quest quest=quests.get(parts[0]);
            for(QuestCore.Task task:quest.tasks) if(task.id.equals(parts[1])) return taskProgress(owner,quest,task);
            return 0;
        }
        return questProgress(owner,quests.get(reference.startsWith("quest:")?reference.substring(6):reference));
    }
    private double questProgress(UUID owner,QuestCore.Quest quest) {
        if(progress().completed(owner).contains(quest.id)) return 1;
        double result=0;
        for(QuestCore.Task task:quest.tasks) { double value=taskProgress(owner,quest,task); result=quest.anyTask?Math.max(result,value):result+value; }
        return quest.anyTask?result:result/Math.max(1,quest.tasks.size());
    }
    private double taskProgress(UUID owner,QuestCore.Quest quest,QuestCore.Task task) {
        if(dependencies(owner).satisfied("task:"+quest.id+"/"+task.id,false)) return 1;
        long count=progress().value(owner,quest.id,task.id); if(count==0 && task.type.equals("kill")) count=progress().kills(owner,quest.id,quest.tasks.indexOf(task));
        return Math.min(1,Math.max(0,count/(double)task.count));
    }
    public boolean objectComplete(UUID owner,String reference) {
        try { validateProgressReference(reference); } catch(IllegalArgumentException missing) { return false; }
        if(!reference.equals("book:")) return dependencies(owner).satisfied(reference,false);
        for(QuestCore.Quest quest:quests.values()) if(QuestCore.required(quest) && !progress().completed(owner).contains(quest.id)) return false;
        return true;
    }
    public String objectTitle(String reference) {
        if(reference.equals("book:")) return book.title;
        if(reference.startsWith("chapter:")) { QuestCore.Chapter chapter=QuestPresentation.chapter(book,reference.substring(8)); return chapter==null?"Quest screen":chapter.title; }
        if(reference.startsWith("task:")) { String[] parts=reference.substring(5).split("/",-1); QuestCore.Quest quest=parts.length==2?quests.get(parts[0]):null; return quest==null?"Quest screen":quest.title+" / "+parts[1]; }
        QuestCore.Quest quest=quests.get(reference.startsWith("quest:")?reference.substring(6):reference); return quest==null?"Quest screen":quest.title;
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
        for (QuestCore.Task t : q.tasks) if (t.id.equals(task) && (t.type.equals("energy") || t.type.equals("ic2_energy") || t.type.equals("botania_mana") || t.type.equals("buildcraft_mj") || t.type.equals("fluid") || t.type.equals("custom")))
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
        boolean matched=targets.isEmpty() && t.filter!=null;
        for(String target:targets) {
            if(target.startsWith("ore:")) {
                for(ItemStack ore:net.minecraftforge.oredict.OreDictionary.getOres(target.substring(4)))
                    if(stack.getItem()==ore.getItem() && (t.ignoreDamage || ore.getItemDamage()==net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE || stack.getItemDamage()==ore.getItemDamage())) matched=true;
            } else if(stack.getItem()==item(target) && (t.ignoreDamage || stack.getItemDamage()==t.meta)) matched=true;
        }
        if(!matched || t.filter!=null && !t.filter.matches(stack)) return false;
        return stackNbtMatches(t.nbt,t.nbtMode,stack);
    }
    public static boolean stackNbtMatches(String nbt,String mode,ItemStack stack) {
        if(!(stack.getItem() instanceof QuestBlocks.LootCrateItem) || nbt.isEmpty() || mode.equals("ignore")) return nbtMatches(nbt,mode,stack.getTagCompound());
        QuestCore.Book book=QuestsMod.service==null?QuestLoot.clientBook:QuestsMod.service.book;
        NBTTagCompound expected=QuestLoot.canonical(tag(nbt),book),actual=QuestLoot.canonical(stack.getTagCompound(),book);
        if(actual!=null) for(String key:Arrays.asList("name","color","glow")) if(!expected.hasKey(key)) actual.removeTag(key);
        return mode.equals("subset")?subset(expected,actual):expected.equals(actual);
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
            case "kill": case "interaction": case "energy": case "ic2_energy": case "botania_mana": case "buildcraft_mj": return counter(p,q,t) >= t.count;
            case "custom": return counter(p,q,t) >= t.count || QuestHooks.TASKS.get(t.target).complete(p,t);
            case "gamestage": return progress().stage(p.getUniqueID(),t.target);
            case "skill": return progress().skill(p.getUniqueID(),t.target)>=t.count;
            case "npc_quest": case "npc_dialog": case "npc_faction": return QuestNPCs.task(p,t);
            case "dimension": return p.dimension == t.dimension;
            case "biome": return t.target.equalsIgnoreCase(p.worldObj.getBiomeGenForCoords(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posZ)).biomeName) ||
                t.target.equals(Integer.toString(p.worldObj.getBiomeGenForCoords(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posZ)).biomeID));
            case "location": return (t.ignoreDimension || p.dimension==t.dimension) && (t.box ? p.posX>=t.x && p.posX<t.x+t.width && p.posY>=t.y && p.posY<t.y+t.height && p.posZ>=t.z && p.posZ<t.z+t.depth : p.getDistanceSq(t.x,t.y,t.z)<=t.radius*t.radius);
            case "stat": return p.func_147099_x().writeStat(stat(t.target,false)) >= t.count;
            case "achievement": return p.func_147099_x().hasAchievementUnlocked((Achievement)stat(t.target,true));
            case "observation": return looking(p,t) && (t.lookTicks==0 || lookTimers.getOrDefault(p.getUniqueID(),Collections.<String,Integer>emptyMap()).getOrDefault(q.id+":"+t.id,0)>=t.lookTicks);
            default: return false;
        }
    }
    private boolean looking(EntityPlayerMP p,QuestCore.Task t) {
        double distance=Math.min(t.radius,64); MovingObjectPosition blockHit=p.rayTrace(distance,1);
        if(!QuestRayMatcher.entity(t)) return blockHit!=null && blockHit.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK && QuestRayMatcher.block(t,p.worldObj,blockHit.blockX,blockHit.blockY,blockHit.blockZ);
        Vec3 eye=Vec3.createVectorHelper(p.posX,p.posY+p.getEyeHeight(),p.posZ),look=p.getLook(1),end=eye.addVector(look.xCoord*distance,look.yCoord*distance,look.zCoord*distance);
        double closest=blockHit==null?distance:eye.distanceTo(blockHit.hitVec); net.minecraft.entity.Entity target=null;
        for(Object raw:p.worldObj.getEntitiesWithinAABBExcludingEntity(p,p.boundingBox.addCoord(look.xCoord*distance,look.yCoord*distance,look.zCoord*distance).expand(1,1,1))) {
            net.minecraft.entity.Entity e=(net.minecraft.entity.Entity)raw; if(!e.canBeCollidedWith()) continue;
            double border=e.getCollisionBorderSize(); net.minecraft.util.AxisAlignedBB bounds=e.boundingBox.expand(border,border,border);
            MovingObjectPosition hit=bounds.calculateIntercept(eye,end); double d=bounds.isVecInside(eye)?0:hit==null?Double.MAX_VALUE:eye.distanceTo(hit.hitVec);
            if(d<closest) { closest=d; target=e; }
        }
        return target!=null && QuestRayMatcher.entity(t,target);
    }
    public void interact(EntityPlayerMP player,net.minecraft.entity.Entity target) {
        for(QuestCore.Quest quest:quests.values()) if(active(player,quest)) for(QuestCore.Task task:quest.tasks)
            if(task.type.equals("interaction") && QuestRayMatcher.entity(task,target)) progress().add(player.getUniqueID(),quest.id,task.id,1,task.count);
    }
    public void interact(EntityPlayerMP player,net.minecraft.world.World world,int x,int y,int z) {
        for(QuestCore.Quest quest:quests.values()) if(active(player,quest)) for(QuestCore.Task task:quest.tasks)
            if(task.type.equals("interaction") && QuestRayMatcher.block(task,world,x,y,z)) progress().add(player.getUniqueID(),quest.id,task.id,1,task.count);
    }
    public void clearObservations(UUID id) { lookTimers.remove(id); }
    public void observe(EntityPlayerMP player) {
        Map<String,Integer> timers=lookTimers.computeIfAbsent(player.getUniqueID(),id->new HashMap<String,Integer>()); Set<String> active=new HashSet<String>();
        for(QuestCore.Quest quest:quests.values()) if(active(player,quest)) for(QuestCore.Task task:quest.tasks) if(task.type.equals("observation") && task.lookTicks>0 && (!task.sticky || counter(player,quest,task)<task.count)) {
            String key=quest.id+":"+task.id; active.add(key); boolean looking=looking(player,task);
            int ticks=looking?Math.min(task.lookTicks,timers.getOrDefault(key,0)+1):0; timers.put(key,ticks);
            if(looking) { progress().start(player.getUniqueID(),quest.id); progress().startTask(player.getUniqueID(),quest.id,task.id); }
            if(ticks>=task.lookTicks && task.sticky) progress().add(player.getUniqueID(),quest.id,task.id,task.count,task.count);
        }
        timers.keySet().retainAll(active);
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
            boolean readOnly=!t.type.equals("check") && (!t.type.equals("item") && !t.type.equals("xp") && !t.type.equals("fluid") && !t.type.equals("emc") || !t.consume);
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
        if(q.anyTask && !q.tasks.isEmpty()) {
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
        boolean emcLoaded=false; double emcOriginal=0,emcReserved=0,emcResult=0;
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
            } else if(t.type.equals("emc")) {
                long needed=Math.max(0,t.count-(t.consume?counter(p,q,t):0)); if(needed==0) continue;
                if(!emcLoaded) { emcOriginal=emcReserved=emcResult=emcBalance(p); QuestProjectE.checkBalance(emcOriginal); emcLoaded=true; }
                if(emcReserved<needed) return false;
                emcReserved=QuestProjectE.subtract(emcReserved,needed);
                if(t.consume) emcResult=QuestProjectE.subtract(emcResult,needed);
            } else if (t.type.equals("xp")) {
                long needed=Math.max(0,t.count-counter(p,q,t));
                int cost;
                if (t.levels) { int level = XP.level(xp); if (level<needed) return false; cost=XP.base(level)-XP.base(level-(int)needed); }
                else cost = (int)needed;
                if (xp<cost) return false; xp-=cost; if (t.consume) xpResult-=cost;
            } else if (!ordinary(p,q,t)) return false;
        }
        if (commit) {
            if(emcLoaded && emcResult!=emcOriginal) setEmc(p,emcResult);
            System.arraycopy(result,0,p.inventory.mainInventory,0,result.length);
            if(xpResult != xpOriginal) XP.set(p,xpResult);
            for (ItemStack empty : containers) give(p,empty);
            p.inventory.markDirty(); p.inventoryContainer.detectAndSendChanges();
            for(QuestCore.Task t:q.tasks) progress().markTask(p.getUniqueID(),q.id,t.id);
            progress().complete(p.getUniqueID(),q.id); if(!q.disableToast) QuestNetwork.toast(p,q.title,"Quest complete",q.icon,q.iconMeta,q.iconNbt); progress().timestamp(p.getUniqueID(),q.id,p.worldObj.getTotalWorldTime());
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
            if(t.type.equals("emc") && t.consume) {
                long remaining=Math.max(0,t.count-counter(p,q,t)); if(remaining==0) return 0;
                double balance=emcBalance(p); QuestProjectE.checkBalance(balance);
                long take=(long)Math.min(remaining,Math.floor(balance));
                if(take>0) { double result=QuestProjectE.subtract(balance,take); setEmc(p,result); progress().add(p.getUniqueID(),q.id,t.id,take,t.count); }
                return take;
            }
            if(!t.type.equals("item") || !t.consume) throw new IllegalArgumentException("Use submit for consuming item/EMC tasks; fluids and energy use a terminal");
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
        autoClaim(p,q);
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
    public void claim(EntityPlayerMP p, QuestCore.Quest q, List<Integer> choices,java.util.function.Consumer<ItemStack> itemSink) { claim(p,q,choices,itemSink,false); }
    private boolean hasChoice(QuestCore.Reward reward) {
        if(reward.type.equals("choice")) return true;
        for(QuestCore.Reward option:reward.options) if(hasChoice(option)) return true;
        return false;
    }
    public void autoClaim(EntityPlayerMP p,QuestCore.Quest q) {
        if(!progress().completed(p.getUniqueID()).contains(q.id) || claimed(p.getUniqueID(),q)) return;
        claim(p,q,Collections.<Integer>emptyList(),null,true);
    }
    private void claim(EntityPlayerMP p,QuestCore.Quest q,List<Integer> choices,java.util.function.Consumer<ItemStack> itemSink,boolean automatic) {
        QuestProgress data=progress(); UUID id=p.getUniqueID();
        if (!data.completed(id).contains(q.id) || claimed(id,q)) throw new IllegalArgumentException("Reward unavailable");
        List<QuestCore.Reward> leaves=new ArrayList<QuestCore.Reward>(); Iterator<Integer> selected=choices.iterator();
        List<Integer> pending=new ArrayList<Integer>(); boolean allShared=true; int[] budget={0};
        for(int i=0;i<q.rewards.size();i++) {
            QuestCore.Reward r=q.rewards.get(i); boolean personal=individual(q,r); allShared&=!personal;
            if(data.rewardClaimed(id,q.id,rewardId(r,i),personal)) continue;
            if(automatic && (!(r.auto.equals("enabled") || r.auto.equals("invisible") || r.auto.equals("inherit") && q.autoClaim) || hasChoice(r))) continue;
            List<QuestCore.Reward> expanded=new ArrayList<QuestCore.Reward>(); flatten(r,selected,p.getRNG(),expanded,budget,0);
            pending.add(i); leaves.addAll(expanded);
        }
        if(automatic && pending.isEmpty() && !(q.rewards.isEmpty() && q.autoClaim)) return;
        if(selected.hasNext()) throw new IllegalArgumentException("Too many reward choices");
        for(QuestCore.Reward r:leaves) validateReward(r);
        preflightRewardEffects(p,leaves);
        for(int index:pending) { QuestCore.Reward r=q.rewards.get(index); data.claimReward(id,q.id,rewardId(r,index),individual(q,r)); }
        if(allShared && (!automatic || pending.size()>0 && claimed(id,q) || q.rewards.isEmpty() && q.autoClaim)) data.claim(id,q.id);
        applyRewards(p,leaves,itemSink);
        for(int index:pending) QuestEvents.fire("reward_claimed",id,q.id,rewardId(q.rewards.get(index),index));
    }
    public long emergencyWait(EntityPlayerMP player) { return (progress().emergencyWait(player.getUniqueID(),player.worldObj.getTotalWorldTime(),book.emergencyCooldownSeconds*20L)+19)/20; }
    public void emergency(EntityPlayerMP player) {
        if(book.emergencyItems.isEmpty()) throw new IllegalArgumentException("Emergency items are not configured");
        long wait=emergencyWait(player); if(wait>0) throw new IllegalArgumentException("Emergency items available in "+wait+" seconds");
        for(QuestCore.Reward reward:book.emergencyItems) validateReward(reward);
        progress().emergency(player.getUniqueID(),player.worldObj.getTotalWorldTime()); applyRewards(player,book.emergencyItems,null);
    }
    public void pin(UUID player,String questId) {
        QuestCore.Quest quest=quests.get(questId); if(quest==null || !visible(player,quest)) throw new IllegalArgumentException("Quest is not visible");
        List<String> pinned=new ArrayList<String>(); for(String old:progress().pins(player)) if(quests.containsKey(old) && visible(player,quests.get(old))) pinned.add(old);
        if(!pinned.remove(questId)) pinned.add(questId); progress().pins(player,pinned);
    }
    public String clickCommand(UUID player,String questId) {
        QuestCore.Quest quest=quests.get(questId);
        if(quest==null || !visible(player,quest)) throw new IllegalArgumentException("Quest action is not visible");
        QuestClick click=new QuestClick(quest.customClick); if(!click.type.equals("command")) throw new IllegalArgumentException("Quest has no command action");
        return click.value.startsWith("/")?click.value:"/"+click.value;
    }
    public void openLoot(EntityPlayerMP p,String table,ItemStack crate) {
        table=QuestLoot.resolve(book,table);
        if(!book.lootTables.containsKey(table)) throw new IllegalArgumentException("Unknown loot table");
        QuestCore.Reward roll=new QuestCore.Reward(); roll.type="loot"; roll.target=table;
        List<QuestCore.Reward> leaves=new ArrayList<QuestCore.Reward>();
        flatten(roll,Collections.<Integer>emptyList().iterator(),p.getRNG(),leaves,new int[]{0},0);
        for(QuestCore.Reward r:leaves) validateReward(r);
        preflightRewardEffects(p,leaves);
        if(crate.stackSize<=0) throw new IllegalArgumentException("Empty crate stack");
        crate.stackSize--; applyRewards(p,leaves,null);
    }
    private void preflightRewardEffects(EntityPlayerMP player,List<QuestCore.Reward> leaves) {
        Map<String,Integer> levels=new HashMap<String,Integer>(),factions=new HashMap<String,Integer>();
        for(QuestCore.Reward reward:leaves) {
            if(reward.type.startsWith("npc_")) QuestNPCs.preflight(reward);
            if(reward.type.equals("skill")) {
                int old=levels.containsKey(reward.target)?levels.get(reward.target):progress().skill(player.getUniqueID(),reward.target);
                levels.put(reward.target,reward.add?Math.addExact(old,reward.points):reward.points);
            } else if(reward.type.equals("npc_faction")) {
                int old=factions.containsKey(reward.target)?factions.get(reward.target):QuestNPCs.points(player,reward.target);
                factions.put(reward.target,Math.addExact(old,reward.add?reward.points:-reward.points));
            }
        }
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
            case "gamestage": progress().stage(id,r.target,!r.remove); break;
            case "skill": progress().skill(id,r.target,r.add?Math.addExact(progress().skill(id,r.target),r.points):r.points); break;
            case "npc_quest": case "npc_faction": case "npc_mail": QuestNPCs.grant(p,r); break;
            case "xp": p.addExperience(r.count); break;
            case "xp_levels": p.addExperienceLevel(r.count); break;
            case "command":
                String command=r.target.replace("{player}",p.getCommandSenderName()).replace("{uuid}",id.toString()).replace("{x}",Integer.toString(MathHelper.floor_double(p.posX))).replace("{y}",Integer.toString(MathHelper.floor_double(p.posY))).replace("{z}",Integer.toString(MathHelper.floor_double(p.posZ)));
                MinecraftServer.getServer().getCommandManager().executeCommand(r.asPlayer?p:MinecraftServer.getServer(),command); break;
            case "achievement": grantAchievement(p,(Achievement)stat(r.target,true),new HashSet<Achievement>()); break;
            case "toast": QuestNetwork.toast(p,r.target,r.text,r.item==null?"minecraft:book":r.item); break;
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
