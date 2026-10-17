package dev.sigmarix.quests;
import java.util.*;
/** Private, validated editor transactions: failed dependency edits never change the live book. */
public final class QuestEditorActions {
    private static QuestCore.Book copy(QuestCore.Book book){return QuestService.JSON.fromJson(QuestService.JSON.toJson(book),QuestCore.Book.class);}
    public static QuestCore.Book move(QuestCore.Book book,Set<String> selected,String chapter,double dx,double dy){
        if(!Double.isFinite(dx) || !Double.isFinite(dy))throw new IllegalArgumentException("Invalid movement");QuestCore.Book draft=copy(book);
        for(QuestCore.Quest quest:draft.quests)if(selected.contains(quest.id) && quest.chapter.equals(chapter)){quest.x+=dx;quest.y+=dy;}
        draft.validate();return draft;
    }
    public static QuestCore.Book dependencies(QuestCore.Book book,Set<String> selected,String target,boolean reverse,boolean add){
        QuestCore.Book draft=copy(book);Map<String,QuestCore.Quest> quests=new LinkedHashMap<String,QuestCore.Quest>();for(QuestCore.Quest quest:draft.quests)quests.put(quest.id,quest);
        if(!quests.containsKey(target))throw new IllegalArgumentException("Unknown dependency quest");
        for(String id:selected){QuestCore.Quest quest=quests.get(reverse?target:id);String dependency=reverse?id:target;if(quest==null || !quests.containsKey(dependency) || quest.id.equals(dependency))throw new IllegalArgumentException("Invalid dependency selection");if(add){if(!quest.dependencies.contains(dependency))quest.dependencies.add(dependency);}else{quest.dependencies.remove(dependency);quest.minDependencies=Math.min(quest.minDependencies,quest.dependencies.size());}}
        draft.validate();return draft;
    }
    public static QuestCore.Book delete(QuestCore.Book book,Set<String> selected){QuestCore.Book draft=copy(book);draft.quests.removeIf(quest->selected.contains(quest.id));Map<String,QuestCore.Quest> quests=new LinkedHashMap<String,QuestCore.Quest>();for(QuestCore.Quest quest:draft.quests)quests.put(quest.id,quest);for(QuestCore.Quest quest:draft.quests){Iterator<String> dependencies=quest.dependencies.iterator();while(dependencies.hasNext())try{QuestDependencies.owners(draft,quests,dependencies.next());}catch(IllegalArgumentException missing){dependencies.remove();}quest.minDependencies=Math.min(quest.minDependencies,quest.dependencies.size());}draft.validate();return draft;}
    private QuestEditorActions(){}
}
