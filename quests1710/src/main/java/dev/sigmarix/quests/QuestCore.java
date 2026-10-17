package dev.sigmarix.quests;

import java.util.*;

/** Independent, versioned quest schema. No upstream source or assets. */
public final class QuestCore {
    public static final Set<String> TASK_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "check", "item", "kill", "dimension", "biome", "location", "xp", "stat", "achievement", "observation", "interaction", "fluid", "energy", "ic2_energy", "botania_mana", "buildcraft_mj", "emc", "npc_quest", "npc_dialog", "npc_faction", "gamestage", "skill", "custom")));
    public static final Set<String> REWARD_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "item", "xp", "xp_levels", "command", "choice", "random", "loot", "achievement", "toast", "npc_quest", "npc_faction", "npc_mail", "gamestage", "skill", "custom")));
    public static final class Book {
        public int format = 2;
        public boolean disableGui,hideFromItemBrowser;
        public double gridScale=.5;
        public String defaultQuestShape="square";
        public List<Reward> emergencyItems=new ArrayList<Reward>();
        public int emergencyCooldownSeconds=300;
        public boolean dropLootCrates;
        public QuestLoot.Weights noDrop=new QuestLoot.Weights(4000,600,0);
        public QuestPresentation.Theme theme=new QuestPresentation.Theme();
        public List<QuestPresentation.Style> styles=new ArrayList<QuestPresentation.Style>();
        public String title = "Quest Book";
        public List<Chapter> chapters = new ArrayList<Chapter>();
        public List<Quest> quests = new ArrayList<Quest>();
        public Map<String, LootSettings> lootSettings = new LinkedHashMap<String, LootSettings>();
        public Map<String, List<Reward>> lootTables = new LinkedHashMap<String, List<Reward>>();
        public Map<String, Quest> validate() {
            if (format < 1 || format > 2 || title == null || title.length() > 256 || quests == null || chapters == null || lootTables == null || lootSettings == null || quests.size() > 4096)
                throw new IllegalArgumentException("Invalid book header or size");
            if(emergencyItems==null || emergencyItems.size()>64 || emergencyCooldownSeconds<0 || emergencyCooldownSeconds>31536000 || noDrop==null) throw new IllegalArgumentException("Invalid emergency items or loot drop configuration");
            noDrop.validate();
            for(Reward reward:emergencyItems) { validateReward(reward,0,false); if(!reward.type.equals("item")) throw new IllegalArgumentException("Emergency rewards must be items"); }
            Set<String> chapterIds = new HashSet<String>();
            for (Chapter c : chapters) {
                if (c == null || !validId(c.id) || c.title == null || c.title.length() > 256 || !chapterIds.add(c.id))
                    throw new IllegalArgumentException("Invalid chapter");
            }
            for(Chapter chapter:chapters) {
                if(chapter.parent==null || chapter.description==null || chapter.description.length()>16384) throw new IllegalArgumentException("Invalid chapter metadata");
                Set<String> parents=new HashSet<String>(); Chapter current=chapter;
                while(current!=null) {
                    if(current.parent==null) throw new IllegalArgumentException("Invalid chapter parent");
                    if(!parents.add(current.id) || parents.size()>64) throw new IllegalArgumentException("Chapter group cycle/depth");
                    if(current.parent.isEmpty()) break;
                    if(!chapterIds.contains(current.parent)) throw new IllegalArgumentException("Unknown parent chapter: "+current.parent);
                    current=QuestPresentation.chapter(this,current.parent);
                }
            }
            Map<String, Quest> byId = new LinkedHashMap<String, Quest>();
            for (Quest q : quests) {
                if (q == null || !validId(q.id) || byId.put(q.id, q) != null) throw new IllegalArgumentException("Invalid or duplicate quest id");
                if (q.title == null || q.title.length() > 256 || q.description == null || q.description.length() > 16384 || q.subtitle==null || q.subtitle.length()>1024 || q.minWidth<0 || q.minWidth>4096 ||
                    q.dependencies == null || q.dependencyState==null || !Arrays.asList("completed","started").contains(q.dependencyState) || q.rewardMode==null || !Arrays.asList("team","individual").contains(q.rewardMode) || q.tasks == null || q.rewards == null || q.tasks.size() > 128 || q.rewards.size() > 128 ||
                    q.chapter == null || (!q.chapter.isEmpty() && !chapterIds.contains(q.chapter)) ||
                    !Double.isFinite(q.x) || !Double.isFinite(q.y) || Math.abs(q.x) > 100000 || Math.abs(q.y) > 100000 || q.cooldownSeconds < 0 || q.minDependencies<0 || q.minDependencies>q.dependencies.size())
                    throw new IllegalArgumentException("Incomplete or invalid quest: " + q.id);
                Set<String> taskIds = new HashSet<String>();
                for (int i = 0; i < q.tasks.size(); i++) {
                    Task t = q.tasks.get(i);
                    if (t == null || t.type == null || !TASK_TYPES.contains(t.type) || t.count < 1 || t.type.equals("emc") && t.count>QuestProjectE.MAX_EXACT || t.type.equals("skill") && t.count>Integer.MAX_VALUE || t.energyTier<1 || t.energyTier>8 || t.type.equals("xp") && t.count > Integer.MAX_VALUE || t.meta < 0 || t.radius < 0 || t.radius > 4096 || !Double.isFinite(t.radius) || !Double.isFinite(t.x) || !Double.isFinite(t.y) || !Double.isFinite(t.z))
                        throw new IllegalArgumentException("Invalid task: " + q.id);
                    QuestPresentation.metadata(t.title,t.description,t.icon,t.iconMeta,t.iconNbt);
                    if (t.id == null) t.id = "t" + i;
                    if (!validId(t.id) || !taskIds.add(t.id)) throw new IllegalArgumentException("Invalid task id: " + q.id);
                    if (Arrays.asList("item", "kill", "biome", "stat", "achievement", "observation", "interaction", "fluid", "custom", "npc_quest", "npc_dialog", "npc_faction", "gamestage", "skill").contains(t.type) && (t.target == null || t.target.isEmpty()) && !(t.type.equals("item") && (t.targets!=null && !t.targets.isEmpty() || t.filter!=null)))
                        throw new IllegalArgumentException("Missing task target: " + q.id);
                    if(!QuestRayMatcher.TYPES.contains(t.matchType)) throw new IllegalArgumentException("Invalid ray matcher type");
                    if(t.comparison==null || !Arrays.asList("at_least","at_most","equals","not_equals","greater","less").contains(t.comparison)) throw new IllegalArgumentException("Invalid comparison");
                    if(t.type.equals("gamestage") || t.type.equals("skill")) QuestAttributes.validateKey(t.target);
                    if(t.type.startsWith("npc_")) QuestNPCs.id(t.target);
                    if(t.targets==null || t.targets.size()>64 || t.nbtMode==null || !Arrays.asList("exact","subset","ignore").contains(t.nbtMode) || t.lookTicks<0 || t.lookTicks>1000000 || !Double.isFinite(t.width) || !Double.isFinite(t.height) || !Double.isFinite(t.depth) || t.width<0 || t.height<0 || t.depth<0)
                        throw new IllegalArgumentException("Invalid task options");
                    if(t.filter!=null) { if(!t.type.equals("item")) throw new IllegalArgumentException("Filters are only valid for item tasks"); t.filter.validate(false); }
                    if (t.nbt == null || t.target != null && t.target.length() > 1024) throw new IllegalArgumentException("Invalid target");
                }
                Set<String> rewardIds=new HashSet<String>();
                for(int i=0;i<q.rewards.size();i++) {
                    Reward r=q.rewards.get(i); if(r!=null && r.id==null) r.id="r"+i;
                    if(r==null || !validId(r.id) || !rewardIds.add(r.id)) throw new IllegalArgumentException("Invalid reward ID: "+q.id);
                    validateReward(r,0,false);
                }
            }
            for (Map.Entry<String, List<Reward>> e : lootTables.entrySet()) {
                if (!validId(e.getKey()) || e.getValue() == null || e.getValue().isEmpty() && (!lootSettings.containsKey(e.getKey()) || lootSettings.get(e.getKey())==null || lootSettings.get(e.getKey()).emptyWeight<=0) || e.getValue().size() > 128) throw new IllegalArgumentException("Invalid loot table");
                for (Reward r : e.getValue()) validateReward(r, 0, true);
            }
            Set<String> aliases=new HashSet<String>();
            for(Map.Entry<String,LootSettings> e:lootSettings.entrySet()) {
                LootSettings settings=e.getValue();
                if(settings!=null && (settings.drops==null || settings.name==null || settings.name.length()>256)) throw new IllegalArgumentException("Invalid crate presentation");
                if(settings!=null && (settings.alias==null || !settings.alias.isEmpty() && (!settings.alias.matches("[a-zA-Z0-9_.:/-]{1,128}") || !aliases.add(settings.alias) || lootTables.containsKey(settings.alias) && !settings.alias.equals(e.getKey())))) throw new IllegalArgumentException("Invalid or duplicate crate alias");
                if(settings!=null) { settings.drops.validate(); QuestPresentation.color(settings.color); }
                if(!lootTables.containsKey(e.getKey()) || settings==null || settings.rolls<1 || settings.rolls>64 || settings.emptyWeight<0 || settings.emptyWeight>1000000)
                    throw new IllegalArgumentException("Invalid loot settings: "+e.getKey());
            }
            for (Quest q : quests) for (Reward r : q.rewards) validateLootReferences(r, new HashSet<String>());
            for (List<Reward> table : lootTables.values()) for (Reward r : table) validateLootReferences(r, new HashSet<String>());
            Set<String> done = new HashSet<String>();
            for (String id : byId.keySet()) visit(id, byId, new HashSet<String>(), done, 0);
            QuestPresentation.validate(this);
            return byId;
        }
        private void validateReward(Reward r, int depth, boolean randomContext) {
            if (depth > 16 || r == null || r.type == null || !REWARD_TYPES.contains(r.type) || r.count < 1 || r.count > 4096 || r.meta < 0 || r.weight < 1 || r.weight > 1000000 || r.options == null || r.options.size() > 128 || r.mode==null || !Arrays.asList("inherit","team","individual").contains(r.mode) || r.nbt == null || r.text==null || r.text.length()>8192 || r.randomBonus<0 || r.randomBonus>4096 || r.count+r.randomBonus>4096)
                throw new IllegalArgumentException("Invalid reward");
            QuestPresentation.metadata(r.title,r.description,r.icon,r.iconMeta,r.iconNbt);
            if(r.type.equals("gamestage") || r.type.equals("skill")) QuestAttributes.validateKey(r.target);
            if((r.type.equals("skill") || r.type.equals("npc_faction")) && r.points<0) throw new IllegalArgumentException("Negative skill level");
            if(r.type.equals("npc_quest") || r.type.equals("npc_faction")) QuestNPCs.id(r.target);
            if(r.type.equals("npc_mail") && r.nbt.isEmpty()) throw new IllegalArgumentException("Mail reward requires native CustomNPCs mail NBT");
            if(r.attachments==null || r.attachments.size()>4 || !r.type.equals("npc_mail") && !r.attachments.isEmpty()) throw new IllegalArgumentException("Invalid mail attachments");
            Set<Integer> mailSlots=new HashSet<Integer>();
            for(MailItem entry:r.attachments) if(entry==null || entry.item==null || entry.item.isEmpty() || entry.nbt==null || entry.slot<0 || entry.slot>3 || !mailSlots.add(entry.slot) || entry.count<1 || entry.count>64 || entry.meta<0) throw new IllegalArgumentException("Invalid mail attachment");
            if(r.auto==null || !Arrays.asList("inherit","enabled","disabled","invisible").contains(r.auto) || (depth>0 || randomContext) && !r.auto.equals("inherit")) throw new IllegalArgumentException("Invalid reward auto-claim scope");
            if((depth>0 || randomContext) && !r.mode.equals("inherit")) throw new IllegalArgumentException("Reward scope belongs on quest reward roots");
            if ("item".equals(r.type) && (r.item == null || r.item.isEmpty())) throw new IllegalArgumentException("Missing reward item");
            if (Arrays.asList("command", "achievement", "loot", "custom", "toast", "npc_quest", "npc_faction", "gamestage", "skill").contains(r.type) && (r.target == null || r.target.isEmpty() || r.target.length() > 8192))
                throw new IllegalArgumentException("Missing reward target");
            if (("choice".equals(r.type) || "random".equals(r.type)) && r.options.isEmpty()) throw new IllegalArgumentException("Empty reward options");
            if(randomContext && r.type.equals("choice")) throw new IllegalArgumentException("Choice rewards cannot be nested in random loot");
            for (Reward option : r.options) validateReward(option, depth + 1, randomContext || r.type.equals("random"));
        }
        private void validateLootReferences(Reward r, Set<String> path) {
            if ("loot".equals(r.type)) {
                List<Reward> table = lootTables.get(r.target);
                if (table == null || !path.add(r.target) || path.size() > 16) throw new IllegalArgumentException("Unknown or cyclic loot table: " + r.target);
                for (Reward entry : table) validateLootReferences(entry, path);
                path.remove(r.target);
            }
            for (Reward child : r.options) validateLootReferences(child, path);
        }
        private void visit(String id, Map<String, Quest> map, Set<String> visiting, Set<String> done, int depth) {
            if (done.contains(id)) return;
            Quest q = map.get(id);
            if (q == null) throw new IllegalArgumentException("Unknown dependency: " + id);
            if (depth > 256 || !visiting.add(id)) throw new IllegalArgumentException("Dependency cycle/depth: " + id);
            for (String dep : q.dependencies) for(String owner:QuestDependencies.owners(this,map,dep,q.dependencyState.equals("started"))) visit(owner, map, visiting, done, depth + 1);
            visiting.remove(id); done.add(id);
        }
    }
    public static boolean validId(String id) { return id != null && id.matches("[a-zA-Z0-9_.-]{1,64}"); }
    public static final class LootSettings { public int rolls=1, emptyWeight; public String name="",color="FFFFFF",alias=""; public boolean glow; public QuestLoot.Weights drops=new QuestLoot.Weights(); }
    public static final class Chapter {
        public String id,title,description="",image="",parent="",defaultQuestShape="";
        public boolean alwaysInvisible;
        public List<QuestPresentation.MapImage> images=new ArrayList<QuestPresentation.MapImage>();
    }
    public static final class Quest {
        public String id, title, description = "", subtitle="", chapter = "", icon = "minecraft:book";
        public String dependencyState="completed", rewardMode="team", shape="", uri="",itemBrowser="inherit";
        public List<QuestPresentation.GuidePage> richPages=new ArrayList<QuestPresentation.GuidePage>();
        public List<String> tags=new ArrayList<String>(),pages=new ArrayList<String>();
        public double x, y, size=1;
        public int iconMeta,minWidth;
        public String iconNbt="";
        public boolean hideDependencyLines,hideTextUntilComplete,disableToast;
        public boolean hidden, optional, repeatable, autoComplete, autoClaim, anyDependency, anyTask;
        public int minDependencies;
        public int cooldownSeconds;
        public List<String> dependencies = new ArrayList<String>();
        public List<Task> tasks = new ArrayList<Task>();
        public List<Reward> rewards = new ArrayList<Reward>();
    }
    public static final class Task {
        public String title="",description="",icon="",iconNbt="";
        public int iconMeta;
        public boolean disableToast;
        public String id, type, target, nbt = "", nbtMode = "exact";
        public QuestItemFilter filter;
        public List<String> targets = new ArrayList<String>();
        public long count = 1;
        public int meta, dimension, energyTier=4;
        public boolean manual, checkActive;
        public int points=1;
        public String comparison="at_least",matchType="";
        public boolean consume, levels, ignoreDamage, craftingOnly, ignoreDimension, box, entity;
        public double width=1, height=1, depth=1;
        public int lookTicks;
        public boolean sticky = true;
        public double x, y, z, radius = 8;
    }
    public static final class MailItem { public String item,nbt=""; public int count=1,meta,slot; }
    public static final class Reward {
        public String title="",description="",icon="",iconNbt="";
        public int iconMeta;
        public String id, mode="inherit", auto="inherit";
        public String type = "item", item, target, nbt = "", text = "";
        public boolean asPlayer, onlyOne, remove;
        public boolean add=true;
        public int points=1;
        public int randomBonus;
        public int count = 1, meta, weight = 1;
        public List<MailItem> attachments=new ArrayList<MailItem>();
        public List<Reward> options = new ArrayList<Reward>();
    }
    public static boolean unlocked(Quest q, Set<String> completed) {
        if (q.dependencies.isEmpty()) return true;
        if(q.minDependencies>0) { int count=0; for(String dep:q.dependencies) if(completed.contains(dep)) count++; return count>=q.minDependencies; }
        if (!q.anyDependency) return completed.containsAll(q.dependencies);
        for (String dep : q.dependencies) if (completed.contains(dep)) return true;
        return false;
    }
}
