package dev.sigmarix.quests;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestDependenciesTest {
    private QuestCore.Book book() {
        QuestCore.Book b=new QuestCore.Book(); QuestCore.Chapter chapter=new QuestCore.Chapter(); chapter.id="first"; chapter.title="First"; b.chapters.add(chapter);
        for(String id:Arrays.asList("a","b")) { QuestCore.Quest q=new QuestCore.Quest(); q.id=id; q.title=id; q.chapter=id.equals("a")?"first":""; QuestCore.Task t=new QuestCore.Task(); t.type="check"; q.tasks.add(t); b.quests.add(q); } return b;
    }
    @Test public void taskAndChapterReferencesValidate() {
        QuestCore.Book b=book(); b.quests.get(1).dependencies.add("task:a/t0"); b.validate(); b.quests.get(1).dependencies.clear(); b.quests.get(1).dependencies.add("chapter:first"); b.validate();
    }
    @Test public void unknownTaskIsRejected() {
        QuestCore.Book b=book(); b.quests.get(1).dependencies.add("task:a/missing"); try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void chapterDependencyCyclesAreRejected() {
        QuestCore.Book b=book(); b.quests.get(0).dependencies.add("chapter:first"); try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void mixedReferenceCyclesAreRejected() {
        QuestCore.Book b=book(); b.quests.get(0).dependencies.add("quest:b"); b.quests.get(1).dependencies.add("task:a/t0"); try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void nestedRewardScopeIsRejected() {
        QuestCore.Book b=book(); QuestCore.Reward r=new QuestCore.Reward(); r.type="choice"; QuestCore.Reward child=new QuestCore.Reward(); child.type="xp"; child.mode="individual"; r.options.add(child); b.quests.get(0).rewards.add(r);
        try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void optionalOnlyChapterHasNoRequiredOwners() {
        QuestCore.Book b=book(); b.quests.get(0).optional=true; Map<String,QuestCore.Quest> map=b.validate(); assertTrue(QuestDependencies.owners(b,map,"chapter:first").isEmpty());
    }
    @Test public void startedChapterCycleIncludesOptionalMembers() {
        QuestCore.Book b=book(); QuestCore.Quest q=b.quests.get(0); q.optional=true; q.dependencyState="started"; q.dependencies.add("chapter:first");
        try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void chapterStatesIgnoreOptionalProgressAndCountCompletedEmptyChildren() {
        QuestCore.Book b=book(); QuestCore.Quest q=b.quests.get(0);
        Set<String> started=new HashSet<String>(),complete=new HashSet<String>();
        QuestDependencies graph=new QuestDependencies(b,b.validate(),new QuestDependencies.Progress(){
            public boolean completed(String id){return complete.contains(id);}
            public boolean started(String id){return started.contains(id);}
            public boolean task(String q,String t,boolean s){return false;}
        });
        assertFalse(graph.chapter("first",true)); assertFalse(graph.chapter("first",false));
        QuestCore.Chapter empty=new QuestCore.Chapter();empty.id="empty";empty.title="Empty";empty.parent="first";b.chapters.add(empty);
        assertTrue(graph.chapter("empty",true));assertTrue(graph.chapter("empty",false));
        assertTrue(graph.chapter("first",true));assertFalse(graph.chapter("first",false));
        q.optional=true;
        assertTrue(graph.chapter("first",false));assertTrue(graph.chapter("first",true));
        empty.parent="";q.optional=false;empty.alwaysInvisible=true;
        QuestCore.Quest hidden=new QuestCore.Quest();hidden.id="hidden";hidden.title="Hidden";hidden.chapter="empty";b.quests.add(hidden);
        graph=new QuestDependencies(b,b.validate(),new QuestDependencies.Progress(){
            public boolean completed(String id){return complete.contains(id);}
            public boolean started(String id){return started.contains(id);}
            public boolean task(String q,String t,boolean s){return false;}
        });
        assertTrue(graph.chapter("empty",false));assertTrue(graph.chapter("empty",true));
        assertFalse(graph.chapter("missing",true));
    }
}
