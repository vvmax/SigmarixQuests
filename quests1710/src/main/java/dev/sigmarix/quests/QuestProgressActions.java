package dev.sigmarix.quests;
import java.util.*;import net.minecraft.entity.player.EntityPlayerMP;
/** Operator progress actions retain object scope and never grant rewards themselves. */
public final class QuestProgressActions {
    public static void apply(QuestService service,EntityPlayerMP player,String reference,boolean complete,boolean dependencies){
        if(!QuestScreenSettings.editor(player))throw new IllegalArgumentException("Operator permission required");UUID id=player.getUniqueID();
        if(reference.startsWith("reward:")){String[] parts=reference.substring(7).split("/",2);QuestCore.Quest q=parts.length==2?service.quests.get(parts[0]):null;if(q==null || !q.rewards.stream().anyMatch(r->parts[1].equals(r.id)))throw new IllegalArgumentException("Unknown reward");if(!complete)service.progress().resetReward(id,q.id,parts[1]);return;}
        if(reference.startsWith("task:") && !dependencies){String[] parts=reference.substring(5).split("/",2);QuestCore.Quest q=parts.length==2?service.quests.get(parts[0]):null;if(q==null)throw new IllegalArgumentException("Unknown task");QuestCore.Task task=q.tasks.stream().filter(t->parts[1].equals(t.id)).findFirst().orElse(null);if(task==null)throw new IllegalArgumentException("Unknown task");if(complete){service.progress().add(id,q.id,task.id,task.count,task.count);service.progress().markTask(id,q.id,task.id);service.finishStored(id,q,player.worldObj.getTotalWorldTime());}else service.progress().resetTask(id,q.id,task.id);return;}
        LinkedHashSet<String> quests=new LinkedHashSet<String>();
        if(reference.isEmpty() || reference.equals("book:"))quests.addAll(service.quests.keySet());else if(reference.startsWith("chapter:")){String chapter=reference.substring(8);if(QuestPresentation.chapter(service.book,chapter)==null)throw new IllegalArgumentException("Unknown chapter");for(QuestCore.Quest q:service.book.quests)if(QuestPresentation.within(service.book,q.chapter,chapter))quests.add(q.id);}
        else{service.validateProgressReference(reference);quests.addAll(QuestDependencies.owners(service.book,service.quests,reference));}
        if(dependencies){ArrayDeque<String> pending=new ArrayDeque<String>(quests);while(!pending.isEmpty()){QuestCore.Quest q=service.quests.get(pending.removeFirst());for(String ref:q.dependencies)for(String owner:QuestDependencies.owners(service.book,service.quests,ref))if(quests.add(owner))pending.addLast(owner);}}
        for(String key:quests){QuestCore.Quest q=service.quests.get(key);if(!complete)service.progress().reset(id,key);else{for(QuestCore.Task task:q.tasks){service.progress().add(id,key,task.id,task.count,task.count);service.progress().markTask(id,key,task.id);}service.progress().complete(id,key);service.progress().timestamp(id,key,player.worldObj.getTotalWorldTime());}}
    }
    private QuestProgressActions(){}
}
