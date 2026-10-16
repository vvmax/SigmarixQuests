package dev.sigmarix.quests;

import java.util.*;

/** Independent, versioned quest schema. No upstream source or assets. */
public final class QuestCore {
    public static final Set<String> TASK_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "check", "item", "kill", "dimension", "biome", "location", "xp", "stat", "achievement", "observation", "interaction", "fluid", "energy", "custom")));
    public static final Set<String> REWARD_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "item", "xp", "xp_levels", "command", "choice", "random", "loot", "achievement", "toast", "custom")));
    public static final class Book {
        public int format = 2;
        public String title = "Quest Book";
        public List<Chapter> chapters = new ArrayList<Chapter>();
        public List<Quest> quests = new ArrayList<Quest>();
        public Map<String, List<Reward>> lootTables = new LinkedHashMap<String, List<Reward>>();
        public Map<String, Quest> validate() {
            if (format < 1 || format > 2 || title == null || title.length() > 256 || quests == null || chapters == null || lootTables == null || quests.size() > 4096)
                throw new IllegalArgumentException("Invalid book header or size");
            Set<String> chapterIds = new HashSet<String>();
            for (Chapter c : chapters) {
                if (c == null || !validId(c.id) || c.title == null || c.title.length() > 256 || !chapterIds.add(c.id))
                    throw new IllegalArgumentException("Invalid chapter");
            }
            Map<String, Quest> byId = new LinkedHashMap<String, Quest>();
            for (Quest q : quests) {
                if (q == null || !validId(q.id) || byId.put(q.id, q) != null) throw new IllegalArgumentException("Invalid or duplicate quest id");
                if (q.title == null || q.title.length() > 256 || q.description == null || q.description.length() > 16384 ||
                    q.dependencies == null || q.tasks == null || q.rewards == null || q.tasks.isEmpty() || q.tasks.size() > 128 || q.rewards.size() > 128 ||
                    q.chapter == null || (!q.chapter.isEmpty() && !chapterIds.contains(q.chapter)) ||
                    !Double.isFinite(q.x) || !Double.isFinite(q.y) || Math.abs(q.x) > 100000 || Math.abs(q.y) > 100000 || q.cooldownSeconds < 0 || q.minDependencies<0 || q.minDependencies>q.dependencies.size())
                    throw new IllegalArgumentException("Incomplete or invalid quest: " + q.id);
                Set<String> taskIds = new HashSet<String>();
                for (int i = 0; i < q.tasks.size(); i++) {
                    Task t = q.tasks.get(i);
                    if (t == null || t.type == null || !TASK_TYPES.contains(t.type) || t.count < 1 || t.type.equals("xp") && t.count > Integer.MAX_VALUE || t.meta < 0 || t.radius < 0 || t.radius > 4096 || !Double.isFinite(t.radius) || !Double.isFinite(t.x) || !Double.isFinite(t.y) || !Double.isFinite(t.z))
                        throw new IllegalArgumentException("Invalid task: " + q.id);
                    if (t.id == null) t.id = "t" + i;
                    if (!validId(t.id) || !taskIds.add(t.id)) throw new IllegalArgumentException("Invalid task id: " + q.id);
                    if (Arrays.asList("item", "kill", "biome", "stat", "achievement", "observation", "interaction", "fluid", "custom").contains(t.type) && (t.target == null || t.target.isEmpty()) && !(t.type.equals("item") && t.targets!=null && !t.targets.isEmpty()))
                        throw new IllegalArgumentException("Missing task target: " + q.id);
                    if(t.targets==null || t.targets.size()>64 || t.nbtMode==null || !Arrays.asList("exact","subset","ignore").contains(t.nbtMode) || t.lookTicks<0 || t.lookTicks>1000000 || !Double.isFinite(t.width) || !Double.isFinite(t.height) || !Double.isFinite(t.depth) || t.width<0 || t.height<0 || t.depth<0)
                        throw new IllegalArgumentException("Invalid task options");
                    if (t.nbt == null || t.target != null && t.target.length() > 1024) throw new IllegalArgumentException("Invalid target");
                }
                for (Reward r : q.rewards) validateReward(r, 0, false);
            }
            for (Map.Entry<String, List<Reward>> e : lootTables.entrySet()) {
                if (!validId(e.getKey()) || e.getValue() == null || e.getValue().isEmpty() || e.getValue().size() > 128) throw new IllegalArgumentException("Invalid loot table");
                for (Reward r : e.getValue()) validateReward(r, 0, true);
            }
            for (Quest q : quests) for (Reward r : q.rewards) validateLootReferences(r, new HashSet<String>());
            for (List<Reward> table : lootTables.values()) for (Reward r : table) validateLootReferences(r, new HashSet<String>());
            Set<String> done = new HashSet<String>();
            for (String id : byId.keySet()) visit(id, byId, new HashSet<String>(), done, 0);
            return byId;
        }
        private void validateReward(Reward r, int depth, boolean randomContext) {
            if (depth > 16 || r == null || r.type == null || !REWARD_TYPES.contains(r.type) || r.count < 1 || r.count > 4096 || r.meta < 0 || r.weight < 1 || r.weight > 1000000 || r.options == null || r.options.size() > 128 || r.nbt == null || r.text==null || r.text.length()>8192 || r.randomBonus<0 || r.randomBonus>4096 || r.count+r.randomBonus>4096)
                throw new IllegalArgumentException("Invalid reward");
            if ("item".equals(r.type) && (r.item == null || r.item.isEmpty())) throw new IllegalArgumentException("Missing reward item");
            if (Arrays.asList("command", "achievement", "loot", "custom", "toast").contains(r.type) && (r.target == null || r.target.isEmpty() || r.target.length() > 8192))
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
            for (String dep : q.dependencies) visit(dep, map, visiting, done, depth + 1);
            visiting.remove(id); done.add(id);
        }
    }
    public static boolean validId(String id) { return id != null && id.matches("[a-zA-Z0-9_.-]{1,64}"); }
    public static final class Chapter { public String id, title, description = ""; }
    public static final class Quest {
        public String id, title, description = "", chapter = "", icon = "minecraft:book";
        public double x, y;
        public boolean hidden, optional, repeatable, autoComplete, autoClaim, anyDependency, anyTask;
        public int minDependencies;
        public int cooldownSeconds;
        public List<String> dependencies = new ArrayList<String>();
        public List<Task> tasks = new ArrayList<Task>();
        public List<Reward> rewards = new ArrayList<Reward>();
    }
    public static final class Task {
        public String id, type, target, nbt = "", nbtMode = "exact";
        public List<String> targets = new ArrayList<String>();
        public long count = 1;
        public int meta, dimension;
        public boolean consume, levels, ignoreDamage, craftingOnly, ignoreDimension, box, entity;
        public double width=1, height=1, depth=1;
        public int lookTicks;
        public boolean sticky = true;
        public double x, y, z, radius = 8;
    }
    public static final class Reward {
        public String type = "item", item, target, nbt = "", text = "";
        public boolean asPlayer, onlyOne;
        public int randomBonus;
        public int count = 1, meta, weight = 1;
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
