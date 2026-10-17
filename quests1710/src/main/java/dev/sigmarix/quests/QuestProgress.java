package dev.sigmarix.quests;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldSavedData;

/** Overworld storage separates players by UUID and persists with the world. */
public final class QuestProgress extends WorldSavedData {
    private NBTTagCompound profiles=new NBTTagCompound();
    private String activeProfile="default";
    private NBTTagCompound attributes=new NBTTagCompound(),directory=new NBTTagCompound();
    private NBTTagCompound players = new NBTTagCompound();
    private NBTTagCompound personalRewards = new NBTTagCompound();
    private NBTTagCompound teams = new NBTTagCompound(), membership = new NBTTagCompound();
    public QuestProgress() { super("sigmarixquests"); }
    public QuestProgress(String name) { super(name); }
    @Override public void readFromNBT(NBTTagCompound tag) {
        directory=tag.getCompoundTag("directory"); attributes=tag.getCompoundTag("attributes"); teams=tag.getCompoundTag("teams"); membership=tag.getCompoundTag("membership"); profiles=tag.getCompoundTag("profiles");
        if(!profiles.hasKey("default")) { NBTTagCompound legacy=new NBTTagCompound(); legacy.setTag("players",tag.getCompoundTag("players")); legacy.setTag("personalRewards",tag.getCompoundTag("personalRewards")); profiles.setTag("default",legacy); }
        String selected=tag.getString("profile"); activeProfile=QuestCore.validId(selected)?selected:"default"; selectProfile(activeProfile);
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        storeProfile(); NBTTagCompound legacy=profiles.getCompoundTag("default");
        tag.setTag("directory",directory); tag.setTag("attributes",attributes); tag.setTag("players",legacy.getCompoundTag("players")); tag.setTag("personalRewards",legacy.getCompoundTag("personalRewards"));
        tag.setTag("teams",teams); tag.setTag("membership",membership); tag.setTag("profiles",profiles); tag.setString("profile",activeProfile);
    }
    private void storeProfile() {
        NBTTagCompound state=new NBTTagCompound(); state.setTag("players",players); state.setTag("personalRewards",personalRewards); profiles.setTag(activeProfile,state);
    }
    private void selectProfile(String key) {
        NBTTagCompound state=profiles.getCompoundTag(key); players=state.getCompoundTag("players"); personalRewards=state.getCompoundTag("personalRewards"); activeProfile=key;
    }
    public String profile() { return activeProfile; }
    public void profile(String key) {
        if(!QuestCore.validId(key)) throw new IllegalArgumentException("Invalid quest profile");
        storeProfile(); selectProfile(key); markDirty();
    }
    private void allProfiles(Runnable action) {
        storeProfile(); String selected=activeProfile;
        try { for(Object raw:new HashSet<Object>(profiles.func_150296_c())) { selectProfile(raw.toString()); action.run(); storeProfile(); } }
        finally { selectProfile(selected); }
    }
    private NBTTagCompound attributes(UUID id) {
        String key=id.toString(); if(!attributes.hasKey(key)) attributes.setTag(key,new NBTTagCompound()); return attributes.getCompoundTag(key);
    }
    public List<String> pins(UUID id) {
        NBTTagList list=attributes(id).getCompoundTag("pins").getTagList(activeProfile,8); List<String> result=new ArrayList<String>();
        for(int i=0;i<list.tagCount() && result.size()<16;i++) { String quest=list.getStringTagAt(i); if(QuestCore.validId(quest) && !result.contains(quest)) result.add(quest); }
        return result;
    }
    public boolean autoPin(UUID id){return attributes(id).getCompoundTag("autoPins").getBoolean(activeProfile);}
    public void autoPin(UUID id,boolean enabled){NBTTagCompound p=attributes(id),modes=p.getCompoundTag("autoPins");modes.setBoolean(activeProfile,enabled);p.setTag("autoPins",modes);markDirty();}
    public void pins(UUID id,Collection<String> quests) {
        if(quests==null || quests.size()>16) throw new IllegalArgumentException("At most 16 quests can be pinned");
        NBTTagList list=new NBTTagList(); Set<String> unique=new HashSet<String>();
        for(String quest:quests) { if(!QuestCore.validId(quest) || !unique.add(quest)) throw new IllegalArgumentException("Invalid pinned quest"); list.appendTag(new NBTTagString(quest)); }
        NBTTagCompound player=attributes(id),all=player.getCompoundTag("pins"); all.setTag(activeProfile,list); player.setTag("pins",all); markDirty();
    }
    public long emergencyWait(UUID id,long now,long cooldownTicks) {
        NBTTagCompound data=attributes(id); if(!data.hasKey("emergencyAt")) return 0;
        long previous=data.getLong("emergencyAt"),elapsed=now>=previous?now-previous:0;
        return Math.max(0,cooldownTicks-elapsed);
    }
    public void emergency(UUID id,long now) { attributes(id).setLong("emergencyAt",now); markDirty(); }
    public boolean stage(UUID id,String key) { QuestAttributes.validateKey(key); return attributes(id).getCompoundTag("stages").getBoolean(key); }
    public void stage(UUID id,String key,boolean enabled) {
        QuestAttributes.validateKey(key); NBTTagCompound p=attributes(id),values=p.getCompoundTag("stages");
        if(enabled) values.setBoolean(key,true); else values.removeTag(key); p.setTag("stages",values); markDirty();
    }
    public int skill(UUID id,String key) { QuestAttributes.validateKey(key); return Math.max(0,attributes(id).getCompoundTag("skills").getInteger(key)); }
    public void skill(UUID id,String key,int level) {
        QuestAttributes.validateKey(key); if(level<0) throw new IllegalArgumentException("Negative skill level");
        NBTTagCompound p=attributes(id),values=p.getCompoundTag("skills"); values.setInteger(key,level); p.setTag("skills",values); markDirty();
    }
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
        start(id,quest); markDirty(); QuestEvents.fire("quest_completed",id,quest,"");
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

    public List<UUID> invitations(UUID id) {
        List<UUID> result=new ArrayList<UUID>();
        for(Object key:teams.func_150296_c()) { NBTTagCompound team=teams.getCompoundTag(key.toString()); if(team.getBoolean("invite-"+id)) result.add(UUID.fromString(team.getString("leader"))); }
        return result;
    }
    public List<QuestNetwork.Rank> leaderboard(Set<String> quests) {
        List<QuestNetwork.Rank> result=new ArrayList<QuestNetwork.Rank>();
        for(Object raw:players.func_150296_c()) {
            String key=raw.toString(); boolean shared=key.startsWith("team-");
            if(!shared && !membership.getString(key).isEmpty() || shared && !teams.hasKey(key.substring(5))) continue;
            QuestNetwork.Rank rank=new QuestNetwork.Rank(); rank.name=shared?teams.getCompoundTag(key.substring(5)).getString("name"):key;
            rank.total=quests.size(); for(String done:completedTags(players.getCompoundTag(key))) if(quests.contains(done)) rank.completed++;
            result.add(rank);
        }
        result.sort(Comparator.comparingInt((QuestNetwork.Rank rank)->rank.completed).reversed().thenComparing(rank->rank.name));
        return new ArrayList<QuestNetwork.Rank>(result.subList(0,Math.min(50,result.size())));
    }
    public String group(UUID id) {
        String team = membership.getString(id.toString());
        return team.isEmpty() ? id.toString() : "team-" + team;
    }
    public String team(UUID id) { return membership.getString(id.toString()); }
    public String teamName(UUID id) { return teams.getCompoundTag(team(id)).getString("name"); }
    public int teamColor(UUID id) {NBTTagCompound entry=teams.getCompoundTag(team(id));return entry.hasKey("color")?entry.getInteger("color"):0x0094FF;}
    public void teamColor(UUID id,int color) {if(!leader(id) || color<0 || color>0xFFFFFF)throw new IllegalArgumentException("Only team leader can set a valid team color");teams.getCompoundTag(team(id)).setInteger("color",color);markDirty();}
    public boolean leader(UUID id) { return teams.getCompoundTag(team(id)).getString("leader").equals(id.toString()); }
    public boolean moderator(UUID id){return leader(id) || !team(id).isEmpty() && teams.getCompoundTag(team(id)).getBoolean("mod-"+id);}
    public void remember(UUID id,String name){if(name!=null && name.length()<=32 && !name.equals(directory.getString(id.toString()))){directory.setString(id.toString(),name);markDirty();}}
    public QuestNetwork.TeamSettings teamSettings(UUID id){
        NBTTagCompound t=teams.getCompoundTag(team(id));QuestNetwork.TeamSettings settings=new QuestNetwork.TeamSettings();settings.color=teamColor(id);settings.title=t.getString("title");settings.description=t.getString("description");settings.freeToJoin=t.getBoolean("freeToJoin");settings.fakePlayerStatus=t.hasKey("fakePlayerStatus")?t.getString("fakePlayerStatus"):"none";return settings;
    }
    public void teamSettings(UUID id,QuestNetwork.TeamSettings settings){
        if(!moderator(id))throw new IllegalArgumentException("Only team moderators can edit settings");
        if(settings==null || settings.title==null || settings.title.length()>128 || settings.description==null || settings.description.length()>1024 || settings.color<0 || settings.color>0xFFFFFF || !Arrays.asList("none","ally","member","mod").contains(settings.fakePlayerStatus))throw new IllegalArgumentException("Invalid team settings");
        NBTTagCompound t=teams.getCompoundTag(team(id));t.setString("title",settings.title);t.setString("description",settings.description);t.setInteger("color",settings.color);t.setBoolean("freeToJoin",settings.freeToJoin);t.setString("fakePlayerStatus",settings.fakePlayerStatus);markDirty();
    }
    public String teamStatus(UUID actor,UUID target){return statusForTeam(team(actor),target);}
    private String statusForTeam(String key,UUID target){
        if(key.isEmpty())return "none";NBTTagCompound t=teams.getCompoundTag(key);
        if(t.getString("leader").equals(target.toString()))return "owner";
        if(key.equals(team(target)))return t.getBoolean("mod-"+target)?"mod":"member";
        if(t.getBoolean("enemy-"+target))return "enemy";if(t.getBoolean("ally-"+target))return "ally";if(t.getBoolean("invite-"+target))return "invited";return "none";
    }
    public List<QuestNetwork.TeamPlayer> teamPlayers(UUID actor){
        List<QuestNetwork.TeamPlayer> out=new ArrayList<QuestNetwork.TeamPlayer>();Set<String> ids=new HashSet<String>();ids.addAll(directory.func_150296_c());ids.addAll(membership.func_150296_c());
        for(String key:ids){UUID target;try{target=UUID.fromString(key);}catch(IllegalArgumentException bad){continue;}if(target.equals(actor))continue;
            QuestNetwork.TeamPlayer entry=new QuestNetwork.TeamPlayer();entry.id=key;entry.name=directory.getString(key);if(entry.name.isEmpty())entry.name=key;entry.status=teamStatus(actor,target);entry.requesting=teams.getCompoundTag(team(actor)).getBoolean("request-"+target);out.add(entry);
        }
        out.sort(Comparator.comparing((QuestNetwork.TeamPlayer v)->v.name,String.CASE_INSENSITIVE_ORDER));return out;
    }
    public List<QuestNetwork.TeamEntry> publicTeams(UUID actor){
        List<QuestNetwork.TeamEntry> out=new ArrayList<QuestNetwork.TeamEntry>();
        for(Object raw:teams.func_150296_c()){
            String key=raw.toString();NBTTagCompound t=teams.getCompoundTag(key);QuestNetwork.TeamEntry entry=new QuestNetwork.TeamEntry();entry.id=key;entry.name=t.getString("name");entry.title=t.getString("title");entry.description=t.getString("description");entry.owner=t.getString("leader");entry.color=t.hasKey("color")?t.getInteger("color"):0x0094FF;entry.freeToJoin=t.getBoolean("freeToJoin");entry.invited=t.getBoolean("invite-"+actor);entry.requesting=t.getBoolean("request-"+actor);entry.enemy=t.getBoolean("enemy-"+actor);out.add(entry);
        }
        out.sort(Comparator.comparing((QuestNetwork.TeamEntry v)->v.name,String.CASE_INSENSITIVE_ORDER));return out;
    }
    public void selectTeam(UUID actor,String key){
        if(!team(actor).isEmpty() || !teams.hasKey(key))throw new IllegalArgumentException("Already in a team or unknown team");NBTTagCompound t=teams.getCompoundTag(key);
        if(t.getBoolean("enemy-"+actor))throw new IllegalArgumentException("Team has marked you as an enemy");
        if(t.getBoolean("freeToJoin") || t.getBoolean("invite-"+actor)){membership.setString(actor.toString(),key);t.removeTag("invite-"+actor);t.removeTag("request-"+actor);t.removeTag("ally-"+actor);t.removeTag("mod-"+actor);}
        else t.setBoolean("request-"+actor,true);markDirty();
    }
    public void teamPlayerAction(UUID actor,UUID target,String operation){
        if(!directory.hasKey(target.toString()) && !membership.hasKey(target.toString()))throw new IllegalArgumentException("Unknown player");
        if(!moderator(actor) || actor.equals(target))throw new IllegalArgumentException("Only moderators can manage other players");
        NBTTagCompound t=teams.getCompoundTag(team(actor));String status=teamStatus(actor,target);
        if(status.equals("owner"))throw new IllegalArgumentException("Cannot alter the owner");
        switch(operation){
            case "invite":
                if(status.equals("enemy") || status.equals("member") || status.equals("mod"))throw new IllegalArgumentException("Player cannot be invited");
                t.removeTag("ally-"+target);t.setBoolean("invite-"+target,true);if(t.getBoolean("request-"+target) && team(target).isEmpty()){membership.setString(target.toString(),team(actor));t.removeTag("invite-"+target);t.removeTag("request-"+target);}break;
            case "cancel_invite":t.removeTag("invite-"+target);break;
            case "deny_request":t.removeTag("request-"+target);break;
            case "kick":
                if(!team(actor).equals(team(target)))throw new IllegalArgumentException("Player is not a member");
                mergePersonal(target);membership.removeTag(target.toString());t.removeTag("mod-"+target);t.setBoolean("request-"+target,true);break;
            case "ally":case "enemy":
                if(status.equals("member") || status.equals("mod"))throw new IllegalArgumentException("Cannot change member relationship");
                boolean previous=t.getBoolean(operation+"-"+target);t.removeTag("ally-"+target);t.removeTag("enemy-"+target);t.removeTag("invite-"+target);if(!previous)t.setBoolean(operation+"-"+target,true);break;
            case "moderator":
                if(!leader(actor) || !team(actor).equals(team(target)))throw new IllegalArgumentException("Only owner can change member roles");
                t.setBoolean("mod-"+target,!t.getBoolean("mod-"+target));break;
            case "transfer":
                if(!leader(actor) || !team(actor).equals(team(target)))throw new IllegalArgumentException("Only owner can transfer to a member");
                t.setString("leader",target.toString());t.removeTag("mod-"+target);t.removeTag("mod-"+actor);break;
            default:throw new IllegalArgumentException("Unknown team player action");
        }markDirty();
    }
    public void exitTeam(UUID actor){if(leader(actor)){if(members(actor).size()!=1)throw new IllegalArgumentException("Transfer ownership before leaving");disband(actor);}else leave(actor);}
    public void createTeam(UUID id, String name) {
        if (!team(id).isEmpty() || name == null || name.length() < 1 || name.length() > 35) throw new IllegalArgumentException("Already in team or invalid name");
        String key = UUID.randomUUID().toString();
        NBTTagCompound team = new NBTTagCompound(); team.setString("leader", id.toString()); team.setString("name", name);
        // Seed the new team with the creator's solo progress; solo data remains intact.
        allProfiles(()->players.setTag("team-" + key,player(id).copy()));
        teams.setTag(key, team); membership.setString(id.toString(), key); markDirty();
    }
    public void invite(UUID leader, UUID target) {
        if (!leader(leader)) throw new IllegalArgumentException("Only team leader can invite");
        teams.getCompoundTag(team(leader)).setBoolean("invite-" + target, true); markDirty();
    }
    public void join(UUID id, UUID leader) {
        String key = team(leader); NBTTagCompound team = teams.getCompoundTag(key);
        if (!team(id).isEmpty() || key.isEmpty() || !team.getBoolean("invite-" + id)) throw new IllegalArgumentException("No invitation or already in team");
        team.removeTag("invite-" + id);team.removeTag("request-"+id);team.removeTag("ally-"+id);team.removeTag("enemy-"+id);team.removeTag("mod-"+id); membership.setString(id.toString(), key); markDirty();
    }
    public void leave(UUID id) {
        if (team(id).isEmpty()) throw new IllegalArgumentException("Not in a team");
        if (leader(id)) throw new IllegalArgumentException("Leader must disband the team");
        mergePersonal(id);teams.getCompoundTag(team(id)).removeTag("mod-"+id); membership.removeTag(id.toString()); markDirty();
    }
    public void kick(UUID leader, UUID target) {
        if (!leader(leader) || !team(leader).equals(team(target)) || leader.equals(target)) throw new IllegalArgumentException("Cannot kick player");
        mergePersonal(target);teams.getCompoundTag(team(leader)).removeTag("mod-"+target); membership.removeTag(target.toString()); markDirty();
    }
    public void disband(UUID id) {
        if (!leader(id)) throw new IllegalArgumentException("Only leader can disband");
        String key = team(id);
        for (Object member : new HashSet<Object>(membership.func_150296_c()))
            if (key.equals(membership.getString(member.toString()))) { mergePersonal(UUID.fromString(member.toString())); membership.removeTag(member.toString()); }
        teams.removeTag(key); allProfiles(()->players.removeTag("team-"+key)); markDirty();
    }
    public long value(UUID id, String quest, String task) { return player(id).getCompoundTag("tasks").getLong(quest + ":" + task); }
    public void add(UUID id, String quest, String task, long amount, long limit) {
        if (amount < 0 || limit < 0) throw new IllegalArgumentException("Negative progress");
        NBTTagCompound p = player(id), tasks = p.getCompoundTag("tasks");
        String key = quest + ":" + task; long old = Math.max(0, tasks.getLong(key));
        tasks.setLong(key, old >= limit ? limit : old + Math.min(amount, limit - old)); p.setTag("tasks", tasks); if(amount>0 && limit>0) { start(id,quest); startTask(id,quest,task); } markDirty();
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
        for (String tag : new String[]{"tasks", "kills", "taskDone", "taskStarted"}) {
            NBTTagCompound values = p.getCompoundTag(tag);
            for (Object key : new HashSet<Object>(values.func_150296_c())) if (key.toString().startsWith(quest + ":")) values.removeTag(key.toString());
        }
        markDirty();
    }
    public void resetTask(UUID id,String quest,String task){
        NBTTagCompound p=player(id);NBTTagList previous=p.getTagList("completed",8),next=new NBTTagList();for(int i=0;i<previous.tagCount();i++)if(!quest.equals(previous.getStringTagAt(i)))next.appendTag(new NBTTagString(previous.getStringTagAt(i)));p.setTag("completed",next);
        for(String tag:new String[]{"tasks","kills","taskDone","taskStarted"})p.getCompoundTag(tag).removeTag(quest+":"+task);p.getCompoundTag("times").removeTag(quest);
        boolean anyStarted=false;for(Object key:p.getCompoundTag("taskStarted").func_150296_c())if(key.toString().startsWith(quest+":")){anyStarted=true;break;}if(!anyStarted)p.getCompoundTag("started").removeTag(quest);markDirty();
    }
    public void resetReward(UUID id,String quest,String reward){
        NBTTagCompound p=player(id);p.getCompoundTag("claimed").removeTag(quest);p.getCompoundTag("claimEpochs").removeTag(quest);p.getCompoundTag("rewardClaims").removeTag(quest+":"+reward);
        for(UUID member:members(id))personal(member).removeTag(quest+":"+reward);markDirty();
    }
    private void mergePersonal(UUID id) { allProfiles(()->mergePersonalProfile(id)); }
    private void mergePersonalProfile(UUID id) {
        NBTTagCompound shared=player(id),solo=players.getCompoundTag(id.toString());
        Set<String> oldDone=completedTags(solo),newDone=completedTags(shared),ids=new HashSet<String>(); ids.addAll(oldDone); ids.addAll(newDone);
        String[] fields={"claimed","tasks","kills","times","cycles","claimEpochs","rewardClaims","started","taskDone","taskStarted"};
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
        NBTTagCompound p=player(id),started=p.getCompoundTag("started");
        if(started.getBoolean(quest)) return;
        started.setBoolean(quest,true); p.setTag("started",started); markDirty(); QuestEvents.fire("quest_started",id,quest,"");
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
    public void startTask(UUID id,String quest,String task) {
        NBTTagCompound p=player(id),started=p.getCompoundTag("taskStarted"); String key=quest+":"+task;
        if(started.getLong(key)>=cycle(id,quest)+1) return;
        started.setLong(key,cycle(id,quest)+1); p.setTag("taskStarted",started); markDirty(); QuestEvents.fire("task_started",id,quest,task);
    }
    public void markTask(UUID id,String quest,String task) {
        if(taskDone(id,quest,task)) return;
        start(id,quest); startTask(id,quest,task);
        NBTTagCompound p=player(id),done=p.getCompoundTag("taskDone"); done.setLong(quest+":"+task,cycle(id,quest)+1); p.setTag("taskDone",done); markDirty();
        QuestEvents.fire("task_completed",id,quest,task);
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