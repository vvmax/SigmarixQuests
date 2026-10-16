package dev.sigmarix.quests;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldSavedData;

/** Overworld storage separates players by UUID and persists with the world. */
public final class QuestProgress extends WorldSavedData {
    private NBTTagCompound players = new NBTTagCompound();
    public QuestProgress() { super("sigmarixquests"); }
    public QuestProgress(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) { players = tag.getCompoundTag("players"); }
    @Override public void writeToNBT(NBTTagCompound tag) { tag.setTag("players", players); }
    private NBTTagCompound player(UUID id) {
        String key = id.toString();
        if (!players.hasKey(key)) players.setTag(key, new NBTTagCompound());
        return players.getCompoundTag(key);
    }
    public Set<String> completed(UUID id) {
        Set<String> result = new HashSet<String>();
        NBTTagList list = player(id).getTagList("completed", 8);
        for (int i = 0; i < list.tagCount(); i++) result.add(list.getStringTagAt(i));
        return result;
    }
    public void complete(UUID id, String quest) {
        if (completed(id).contains(quest)) return;
        NBTTagCompound p = player(id);
        NBTTagList list = p.getTagList("completed", 8);
        list.appendTag(new NBTTagString(quest));
        p.setTag("completed", list);
        markDirty();
    }
    public boolean claimed(UUID id, String quest) { return player(id).getCompoundTag("claimed").getBoolean(quest); }
    public void claim(UUID id, String quest) {
        NBTTagCompound p = player(id), claims = p.getCompoundTag("claimed");
        claims.setBoolean(quest, true); p.setTag("claimed", claims); markDirty();
    }
    public int kills(UUID id, String quest, int task) { return player(id).getCompoundTag("kills").getInteger(quest + ":" + task); }
    public void kill(UUID id, String quest, int task, int limit) {
        NBTTagCompound p = player(id), kills = p.getCompoundTag("kills");
        String key = quest + ":" + task;
        int old = kills.getInteger(key);
        if (old < limit) { kills.setInteger(key, old + 1); p.setTag("kills", kills); markDirty(); }
    }
}
