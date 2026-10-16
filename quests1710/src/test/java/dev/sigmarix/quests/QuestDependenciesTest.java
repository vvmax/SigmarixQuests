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
}
