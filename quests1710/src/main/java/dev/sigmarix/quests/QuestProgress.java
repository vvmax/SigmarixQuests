package dev.sigmarix.quests;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldSavedData;

/** Overworld storage separates players by UUID and persists with the world. */
public final class QuestProgress extends WorldSavedData {
    private NBTTagCompound players = new NBTTagCompound();
    private NBTTagCompound teams = new NBTTagCompound(), membership = new NBTTagCompound();
    public QuestProgress() { super("sigmarixquests"); }
    public QuestProgress(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) { players = tag.getCompoundTag("players"); teams = tag.getCompoundTag("teams"); membership = tag.getCompoundTag("membership"); }
    @Override public void writeToNBT(NBTTagCompound tag) { tag.setTag("players", players); tag.setTag("teams", teams); tag.setTag("membership", membership); }
    private NBTTagCompound player(UUID id) {
        String key = group(id);
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

    public String group(UUID id) {
        String team = membership.getString(id.toString());
        return team.isEmpty() ? id.toString() : "team-" + team;
    }
    public String team(UUID id) { return membership.getString(id.toString()); }
    public String teamName(UUID id) { return teams.getCompoundTag(team(id)).getString("name"); }
    public boolean leader(UUID id) { return teams.getCompoundTag(team(id)).getString("leader").equals(id.toString()); }
    public void createTeam(UUID id, String name) {
        if (!team(id).isEmpty() || name == null || name.length() < 1 || name.length() > 32) throw new IllegalArgumentException("Already in team or invalid name");
        String key = UUID.randomUUID().toString();
        NBTTagCompound team = new NBTTagCompound(); team.setString("leader", id.toString()); team.setString("name", name);
        // Seed the new team with the creator's solo progress; solo data remains intact.
        players.setTag("team-" + key, player(id).copy());
        teams.setTag(key, team); membership.setString(id.toString(), key); markDirty();
    }
    public void invite(UUID leader, UUID target) {
        if (!leader(leader)) throw new IllegalArgumentException("Only team leader can invite");
        teams.getCompoundTag(team(leader)).setBoolean("invite-" + target, true); markDirty();
    }
    public void join(UUID id, UUID leader) {
        String key = team(leader); NBTTagCompound team = teams.getCompoundTag(key);
        if (!team(id).isEmpty() || key.isEmpty() || !team.getBoolean("invite-" + id)) throw new IllegalArgumentException("No invitation or already in team");
        team.removeTag("invite-" + id); membership.setString(id.toString(), key); markDirty();
    }
    public void leave(UUID id) {
        if (team(id).isEmpty()) throw new IllegalArgumentException("Not in a team");
        if (leader(id)) throw new IllegalArgumentException("Leader must disband the team");
        mergePersonal(id); membership.removeTag(id.toString()); markDirty();
    }
    public void kick(UUID leader, UUID target) {
        if (!leader(leader) || !team(leader).equals(team(target)) || leader.equals(target)) throw new IllegalArgumentException("Cannot kick player");
        mergePersonal(target); membership.removeTag(target.toString()); markDirty();
    }
    public void disband(UUID id) {
        if (!leader(id)) throw new IllegalArgumentException("Only leader can disband");
        String key = team(id);
        for (Object member : new HashSet<Object>(membership.func_150296_c()))
            if (key.equals(membership.getString(member.toString()))) { mergePersonal(UUID.fromString(member.toString())); membership.removeTag(member.toString()); }
        teams.removeTag(key); players.removeTag("team-" + key); markDirty();
    }
    public long value(UUID id, String quest, String task) { return player(id).getCompoundTag("tasks").getLong(quest + ":" + task); }
    public void add(UUID id, String quest, String task, long amount, long limit) {
        if (amount < 0 || limit < 0) throw new IllegalArgumentException("Negative progress");
        NBTTagCompound p = player(id), tasks = p.getCompoundTag("tasks");
        String key = quest + ":" + task; long old = Math.max(0, tasks.getLong(key));
        tasks.setLong(key, old >= limit ? limit : old + Math.min(amount, limit - old)); p.setTag("tasks", tasks); markDirty();
    }
    public long completedAt(UUID id, String quest) { return player(id).getCompoundTag("times").getLong(quest); }
    public void timestamp(UUID id, String quest, long time) {
        NBTTagCompound p = player(id), times = p.getCompoundTag("times"); times.setLong(quest, time); p.setTag("times", times); markDirty();
    }
    public void reset(UUID id, String quest) {
        NBTTagCompound p = player(id);
        NBTTagList previous = p.getTagList("completed", 8), next = new NBTTagList();
        for (int i=0; i<previous.tagCount(); i++) if (!quest.equals(previous.getStringTagAt(i))) next.appendTag(new NBTTagString(previous.getStringTagAt(i)));
        p.setTag("completed", next);
        p.getCompoundTag("claimed").removeTag(quest); p.getCompoundTag("times").removeTag(quest);
        for (String tag : new String[]{"tasks", "kills"}) {
            NBTTagCompound values = p.getCompoundTag(tag);
            for (Object key : new HashSet<Object>(values.func_150296_c())) if (key.toString().startsWith(quest + ":")) values.removeTag(key.toString());
        }
        markDirty();
    }
    private void mergePersonal(UUID id) {
        NBTTagCompound shared=player(id), solo=players.getCompoundTag(id.toString());
        NBTTagList old=solo.getTagList("completed",8), team=shared.getTagList("completed",8);
        Set<String> seen=new HashSet<String>(); for(int i=0;i<old.tagCount();i++) seen.add(old.getStringTagAt(i));
        for(int i=0;i<team.tagCount();i++) if(seen.add(team.getStringTagAt(i))) old.appendTag(new NBTTagString(team.getStringTagAt(i)));
        solo.setTag("completed",old);
        for(String field:new String[]{"claimed","tasks","kills","times"}) {
            NBTTagCompound previous=solo.getCompoundTag(field), incoming=shared.getCompoundTag(field);
            for(Object raw:incoming.func_150296_c()) {
                String key=raw.toString();
                if(field.equals("claimed")) previous.setBoolean(key,previous.getBoolean(key)||incoming.getBoolean(key));
                else if(field.equals("kills")) previous.setInteger(key,Math.max(previous.getInteger(key),incoming.getInteger(key)));
                else previous.setLong(key,Math.max(previous.getLong(key),incoming.getLong(key)));
            }
            solo.setTag(field,previous);
        }
        players.setTag(id.toString(),solo); markDirty();
    }
}