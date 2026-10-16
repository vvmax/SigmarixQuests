package dev.sigmarix.quests;
import java.util.*;
public final class QuestCoreTest {
    private static QuestCore.Quest quest(String id) {
        QuestCore.Quest q = new QuestCore.Quest(); q.id=id; q.title=id;
        QuestCore.Task t = new QuestCore.Task(); t.type="check"; q.tasks.add(t); return q;
    }
    private static void reject(QuestCore.Book book) {
        try { book.validate(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid book accepted");
    }
    @org.junit.Test public void regression() {
        QuestCore.Book b=new QuestCore.Book(); QuestCore.Quest a=quest("a"), c=quest("c");
        b.quests.add(a); b.quests.add(c); c.dependencies.add("a"); b.validate();
        if (QuestCore.unlocked(c, Collections.<String>emptySet())) throw new AssertionError("Locked quest unlocked");
        if (!QuestCore.unlocked(c, Collections.singleton("a"))) throw new AssertionError("Completed dependency ignored");
        a.dependencies.add("c"); reject(b); a.dependencies.clear();
        c.dependencies.add("missing"); reject(b); c.dependencies.remove("missing");
        b.quests.add(quest("a")); reject(b); b.quests.remove(2);
        QuestCore.Task t=new QuestCore.Task(); t.type="item"; t.target="minecraft:stone"; t.count=Integer.MAX_VALUE;
        a.tasks.add(t); a.tasks.add(t);
        if (QuestCore.requiredItems(a).get("minecraft:stone@0") != 4294967294L) throw new AssertionError("Counts not aggregated safely");
        t.count=0; reject(b); t.count=1; t.type="unknown"; reject(b);
        QuestProgress progress = new QuestProgress();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        progress.complete(first, "a"); progress.complete(first, "a"); progress.claim(first, "a");
        progress.kill(first, "c", 0, 2); progress.kill(first, "c", 0, 2); progress.kill(first, "c", 0, 2);
        net.minecraft.nbt.NBTTagCompound saved = new net.minecraft.nbt.NBTTagCompound();
        progress.writeToNBT(saved);
        QuestProgress restored = new QuestProgress(); restored.readFromNBT(saved);
        if (restored.completed(first).size() != 1 || !restored.claimed(first, "a") || restored.kills(first, "c", 0) != 2)
            throw new AssertionError("Progress did not survive NBT roundtrip");
        if (!restored.completed(second).isEmpty() || restored.claimed(second, "a") || restored.kills(second, "c", 0) != 0)
            throw new AssertionError("Progress leaked between players");
        System.out.println("NBT persistence, UUID isolation, reward claims, capped kills passed");
        System.out.println("QuestCore: dependencies, cycles, duplicate ids, invalid tasks, aggregate counts passed");
    }
}
