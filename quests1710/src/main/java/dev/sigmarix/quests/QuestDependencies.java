package dev.sigmarix.quests;

import java.util.*;

/** Shared reference semantics for validation, server progress and client graph edges. */
public final class QuestDependencies {
    public interface Progress {
        boolean completed(String quest);
        boolean started(String quest);
        boolean task(String quest,String task,boolean started);
    }
    private final QuestCore.Book book;
    private final Map<String,QuestCore.Quest> quests;
    private final Progress progress;
    public QuestDependencies(QuestCore.Book book,Map<String,QuestCore.Quest> quests,Progress progress) {
        this.book=book; this.quests=quests; this.progress=progress;
    }
    public static List<String> owners(QuestCore.Book book,Map<String,QuestCore.Quest> quests,String reference) {
        return owners(book,quests,reference,false);
    }
    public static List<String> owners(QuestCore.Book book,Map<String,QuestCore.Quest> quests,String reference,boolean started) {
        if(reference==null) throw new IllegalArgumentException("Null dependency");
        if(reference.startsWith("chapter:")) {
            String id=reference.substring(8); boolean exists=false;
            for(QuestCore.Chapter c:book.chapters) if(c.id.equals(id)) exists=true;
            if(!exists) throw new IllegalArgumentException("Unknown chapter dependency: "+id);
            List<String> result=new ArrayList<String>();
            for(QuestCore.Quest q:quests.values()) if(QuestPresentation.within(book,q.chapter,id) && (started || QuestCore.required(q))) result.add(q.id);
            return result;
        }
        String id=reference.startsWith("quest:")?reference.substring(6):reference;
        if(reference.startsWith("task:")) {
            String[] parts=reference.substring(5).split("/",-1);
            if(parts.length!=2) throw new IllegalArgumentException("Task dependency must be task:quest/task");
            id=parts[0]; QuestCore.Quest q=quests.get(id); boolean exists=false;
            if(q!=null) for(QuestCore.Task t:q.tasks) if(t.id.equals(parts[1])) exists=true;
            if(!exists) throw new IllegalArgumentException("Unknown task dependency: "+reference);
        }
        if(!quests.containsKey(id)) throw new IllegalArgumentException("Unknown quest dependency: "+id);
        return Collections.singletonList(id);
    }
    public boolean chapter(String id,boolean started) {
        QuestCore.Chapter chapter=QuestPresentation.chapter(book,id);
        if(chapter==null) return false;
        if(chapter.alwaysInvisible) return true;
        int count=0;
        for(QuestCore.Quest q:quests.values()) if(q.chapter.equals(id) && QuestCore.required(q)) {
            count++;
            if(started && (progress.started(q.id) || progress.completed(q.id))) return true;
            if(!started && !progress.completed(q.id)) return false;
        }
        for(QuestCore.Chapter child:book.chapters) if(child.parent.equals(id)) {
            count++;
            boolean value=chapter(child.id,started);
            if(started && value) return true;
            if(!started && !value) return false;
        }
        // Native empty and optional-only groups are 100%, hence also started.
        return !started || count==0;
    }
    public boolean satisfied(String reference,boolean started) {
        if(reference.startsWith("chapter:")) return chapter(reference.substring(8),started);
        if(reference.startsWith("task:")) {
            String[] parts=reference.substring(5).split("/",-1);
            return parts.length==2 && progress.task(parts[0],parts[1],started);
        }
        String id=reference.startsWith("quest:")?reference.substring(6):reference;
        return started?progress.started(id):progress.completed(id);
    }
    public boolean unlocked(QuestCore.Quest q) {
        if(q.dependencies.isEmpty()) return true;
        int required=q.minDependencies>0?q.minDependencies:q.anyDependency?1:q.dependencies.size(),count=0;
        for(String reference:q.dependencies) if(satisfied(reference,q.dependencyState.equals("started")) && ++count>=required) return true;
        return false;
    }
}
