package dev.sigmarix.quests;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldSavedData;

/** Overworld storage separates players by UUID and persists with the world. */
public final class QuestProgress extends WorldSavedData {
    private NBTTagCompound players = new NBTTagCompound();
    private NBTTagCompound personalRewards = new NBTTagCompound();
    private NBTTagCompound teams = new NBTTagCompound(), membership = new NBTTagCompound();
    public QuestProgress() { super("sigmarixquests"); }
    public QuestProgress(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) { players = tag.getCompoundTag("players"); teams = tag.getCompoundTag("teams"); membership = tag.getCompoundTag("membership"); personalRewards=tag.getCompoundTag("personalRewards"); }
    @Override public void writeToNBT(NBTTagCompound tag) { tag.setTag("players", players); tag.setTag("teams", teams); tag.setTag("membership", membership); tag.setTag("personalRewards",personalRewards); }
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
        start(id,quest); markDirty();
    }
    public boolean claimed(UUID id, String quest) {
        NBTTagCompound p=player(id),epochs=p.getCompoundTag("claimEpochs");
        return epochs.hasKey(quest)?epochs.getLong(quest)>=cycle(id,quest)+1:cycle(id,quest)==0 && p.getCompoundTag("claimed").getBoolean(quest);
    }
    public void claim(UUID id, String quest) {
        NBTTagCompound p = player(id), claims = p.getCompoundTag("claimed");
        claims.setBoolean(quest, true); p.setTag("claimed", claims);
        NBTTagCompound epochs=p.getCompoundTag("claimEpochs"); epochs.setLong(quest,cycle(id,quest)+1); p.setTag("claimEpochs",epochs); markDirty();
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
        tasks.setLong(key, old >= limit ? limit : old + Math.min(amount, limit - old)); p.setTag("tasks", tasks); if(amount>0 && limit>0) start(id,quest); markDirty();
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
        p.getCompoundTag("claimed").removeTag(quest); p.getCompoundTag("times").removeTag(quest); p.getCompoundTag("started").removeTag(quest); p.getCompoundTag("claimEpochs").removeTag(quest);
        NBTTagCompound cycles=p.getCompoundTag("cycles"); cycles.setLong(quest,Math.addExact(cycle(id,quest),1)); p.setTag("cycles",cycles);
        for (String tag : new String[]{"tasks", "kills", "taskDone"}) {
            NBTTagCompound values = p.getCompoundTag(tag);
            for (Object key : new HashSet<Object>(values.func_150296_c())) if (key.toString().startsWith(quest + ":")) values.removeTag(key.toString());
        }
        markDirty();
    }
    private void mergePersonal(UUID id) {
        NBTTagCompound shared=player(id),solo=players.getCompoundTag(id.toString());
        Set<String> oldDone=completedTags(solo),newDone=completedTags(shared),ids=new HashSet<String>(); ids.addAll(oldDone); ids.addAll(newDone);
        String[] fields={"claimed","tasks","kills","times","cycles","claimEpochs","rewardClaims","started","taskDone"};
        for(String field:fields) for(NBTTagCompound source:new NBTTagCompound[]{solo,shared}) for(Object raw:source.getCompoundTag(field).func_150296_c()) {
            String key=raw.toString(); int colon=key.indexOf(':'); ids.add(colon<0?key:key.substring(0,colon));
        }
        for(String quest:ids) {
            long previous=solo.getCompoundTag("cycles").getLong(quest),incoming=shared.getCompoundTag("cycles").getLong(quest);
            if(incoming<previous) continue;
            if(incoming>previous) {
                oldDone.remove(quest);
                for(String field:fields) {
                    NBTTagCompound values=solo.getCompoundTag(field);
                    for(Object raw:new HashSet<Object>(values.func_150296_c())) if(raw.toString().equals(quest) || raw.toString().startsWith(quest+":")) values.removeTag(raw.toString());
                }
            }
            if(newDone.contains(quest)) oldDone.add(quest);
            for(String field:fields) {
                NBTTagCompound target=solo.getCompoundTag(field),source=shared.getCompoundTag(field);
                for(Object raw:source.func_150296_c()) {
                    String key=raw.toString(); if(!key.equals(quest) && !key.startsWith(quest+":")) continue;
                    if(field.equals("claimed") || field.equals("started")) target.setBoolean(key,target.getBoolean(key)||source.getBoolean(key));
                    else if(field.equals("kills")) target.setInteger(key,Math.max(target.getInteger(key),source.getInteger(key)));
                    else target.setLong(key,Math.max(target.getLong(key),source.getLong(key)));
                }
                solo.setTag(field,target);
            }
        }
        NBTTagList completed=new NBTTagList(); for(String quest:oldDone) completed.appendTag(new NBTTagString(quest)); solo.setTag("completed",completed);
        players.setTag(id.toString(),solo); markDirty();
    }
    private static Set<String> completedTags(NBTTagCompound nbt) {
        Set<String> result=new HashSet<String>(); NBTTagList values=nbt.getTagList("completed",8);
        for(int i=0;i<values.tagCount();i++) result.add(values.getStringTagAt(i)); return result;
    }
    public long cycle(UUID id,String quest) { return Math.max(0,player(id).getCompoundTag("cycles").getLong(quest)); }
    public void start(UUID id,String quest) {
        NBTTagCompound p=player(id),started=p.getCompoundTag("started"); started.setBoolean(quest,true); p.setTag("started",started); markDirty();
    }
    public boolean started(UUID id,String quest) {
        NBTTagCompound p=player(id);
        if(completed(id).contains(quest) || p.getCompoundTag("started").getBoolean(quest)) return true;
        for(String field:new String[]{"tasks","kills"}) {
            NBTTagCompound values=p.getCompoundTag(field);
            for(Object raw:values.func_150296_c()) if(raw.toString().startsWith(quest+":") && (field.equals("kills")?values.getInteger(raw.toString()):values.getLong(raw.toString()))>0) return true;
        }
        return false;
    }
    public boolean taskDone(UUID id,String quest,String task) { return player(id).getCompoundTag("taskDone").getLong(quest+":"+task)>=cycle(id,quest)+1; }
    public void markTask(UUID id,String quest,String task) {
        NBTTagCompound p=player(id),done=p.getCompoundTag("taskDone"); done.setLong(quest+":"+task,cycle(id,quest)+1); p.setTag("taskDone",done); start(id,quest);
    }
    private NBTTagCompound personal(UUID id) {
        String key=id.toString(); if(!personalRewards.hasKey(key)) personalRewards.setTag(key,new NBTTagCompound());
        return personalRewards.getCompoundTag(key);
    }
    public boolean rewardClaimed(UUID id,String quest,String reward,boolean individual) {
        if(claimed(id,quest)) return true;
        NBTTagCompound ledger=individual?personal(id):player(id).getCompoundTag("rewardClaims");
        return ledger.getLong(quest+":"+reward)>=cycle(id,quest)+1;
    }
    public void claimReward(UUID id,String quest,String reward,boolean individual) {
        NBTTagCompound ledger=individual?personal(id):player(id).getCompoundTag("rewardClaims");
        ledger.setLong(quest+":"+reward,cycle(id,quest)+1);
        if(!individual) player(id).setTag("rewardClaims",ledger); markDirty();
    }
    public Set<UUID> members(UUID id) {
        Set<UUID> result=new HashSet<UUID>(); String team=team(id);
        if(team.isEmpty()) { result.add(id); return result; }
        for(Object raw:membership.func_150296_c()) if(team.equals(membership.getString(raw.toString()))) result.add(UUID.fromString(raw.toString()));
        return result;
    }
}