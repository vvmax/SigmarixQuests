package dev.sigmarix.quests;

import java.util.*;

/** Standalone quest definitions and validation; no upstream FTB code. */
public final class QuestCore {
    public static final class Book {
        public List<Quest> quests = new ArrayList<Quest>();
        public Map<String, Quest> validate() {
            Map<String, Quest> byId = new LinkedHashMap<String, Quest>();
            if (quests == null) throw new IllegalArgumentException("Missing quests");
            for (Quest q : quests) {
                if (q == null || q.id == null || !q.id.matches("[a-zA-Z0-9_.-]{1,64}") || byId.put(q.id, q) != null)
                    throw new IllegalArgumentException("Invalid or duplicate quest id");
                if (q.title == null || q.description == null || q.dependencies == null || q.tasks == null || q.rewards == null || q.tasks.isEmpty())
                    throw new IllegalArgumentException("Incomplete quest: " + q.id);
                for (Task t : q.tasks) {
                    if (t == null || t.type == null || t.count < 1) throw new IllegalArgumentException("Invalid task: " + q.id);
                    if (!Arrays.asList("check", "item", "kill", "dimension").contains(t.type)) throw new IllegalArgumentException("Unknown task: " + t.type);
                    if ((t.type.equals("item") || t.type.equals("kill")) && (t.target == null || t.target.isEmpty()))
                        throw new IllegalArgumentException("Missing task target: " + q.id);
                    if (t.meta < 0) throw new IllegalArgumentException("Negative metadata");
                }
                for (Reward r : q.rewards)
                    if (r == null || r.item == null || r.item.isEmpty() || r.count < 1 || r.count > 4096 || r.meta < 0)
                        throw new IllegalArgumentException("Invalid reward: " + q.id);
            }
            Set<String> done = new HashSet<String>();
            for (String id : byId.keySet()) visit(id, byId, new HashSet<String>(), done);
            return byId;
        }
        private void visit(String id, Map<String, Quest> map, Set<String> visiting, Set<String> done) {
            if (done.contains(id)) return;
            Quest q = map.get(id);
            if (q == null) throw new IllegalArgumentException("Unknown dependency: " + id);
            if (!visiting.add(id)) throw new IllegalArgumentException("Dependency cycle: " + id);
            for (String dep : q.dependencies) visit(dep, map, visiting, done);
            visiting.remove(id);
            done.add(id);
        }
    }
    public static final class Quest {
        public String id, title, description = "";
        public List<String> dependencies = new ArrayList<String>();
        public List<Task> tasks = new ArrayList<Task>();
        public List<Reward> rewards = new ArrayList<Reward>();
    }
    public static final class Task {
        public String type, target;
        public int count = 1, meta = 0, dimension = 0;
    }
    public static final class Reward {
        public String item;
        public int count = 1, meta = 0;
    }
    public static boolean unlocked(Quest q, Set<String> completed) { return completed.containsAll(q.dependencies); }
    /** Read-only inventory tasks: aggregate matching requirements to prevent reusing one stack twice. */
    public static Map<String, Long> requiredItems(Quest q) {
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        for (Task t : q.tasks) if ("item".equals(t.type)) {
            String key = t.target + "@" + t.meta;
            Long previous = result.get(key);
            result.put(key, (previous == null ? 0L : previous) + t.count);
        }
        return result;
    }
}
