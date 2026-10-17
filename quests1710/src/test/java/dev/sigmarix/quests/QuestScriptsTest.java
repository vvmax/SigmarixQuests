package dev.sigmarix.quests;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;
public class QuestScriptsTest {
    private Path directory;
    @Before public void setup() throws IOException { directory=Files.createTempDirectory("quest-script-test"); }
    @After public void cleanup() throws IOException {
        QuestScripts.clear();
        try(java.util.stream.Stream<Path> paths=Files.walk(directory)) { for(Path file:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(file); }
    }
    private void script(String text) throws IOException { Files.write(directory.resolve("test.js"),text.getBytes(StandardCharsets.UTF_8)); }
    @Test public void javascriptRegistersAndExecutesCustomTasksAndRewards() throws Exception {
        script("quests.task('test:script', function(player, task) { return Number(task.count) === 7; });\nquests.reward('test:reward', function(player, reward) { reward.text = 'executed'; });");
        QuestScripts.load(directory.toFile()); QuestCore.Task task=new QuestCore.Task(); task.count=7;
        assertTrue(QuestHooks.TASKS.get("test:script").complete(null,task)); task.count=8; assertFalse(QuestHooks.TASKS.get("test:script").complete(null,task));
        QuestCore.Reward reward=new QuestCore.Reward(); QuestHooks.REWARDS.get("test:reward").grant(null,reward); assertEquals("executed",reward.text);
        QuestScripts.clear(); assertFalse(QuestHooks.TASKS.containsKey("test:script"));
    }
    @Test public void scriptEventsPreserveJavaListenersAndReloadDoesNotDuplicateHandlers() throws Exception {
        List<String> javaEvents=new ArrayList<String>(); java.util.function.Consumer<QuestEvents.Event> listener=e->javaEvents.add(e.quest);
        QuestEvents.on("quest_completed",listener);
        try {
            script("quests.on('quest_completed', function(event) { java.lang.System.setProperty('quests.script.test',event.quest); });");
            QuestScripts.load(directory.toFile()); QuestScripts.load(directory.toFile());
            QuestEvents.fire("quest_completed",UUID.randomUUID(),"script-event","");
            assertEquals("script-event",System.getProperty("quests.script.test")); assertEquals(Collections.singletonList("script-event"),javaEvents);
            QuestScripts.clear(); QuestEvents.fire("quest_completed",UUID.randomUUID(),"java-event",""); assertEquals(2,javaEvents.size());
        } finally { QuestEvents.off("quest_completed",listener); System.clearProperty("quests.script.test"); }
    }
    @Test public void failedRegistrationRemovesPartiallyInstalledScriptHandlers() throws Exception {
        script("quests.task('test:partial', function(p,t) { return true; }); throw new Error('bad config');");
        try { QuestScripts.load(directory.toFile()); fail(); } catch(IOException expected) {}
        assertFalse(QuestHooks.TASKS.containsKey("test:partial"));
    }
    @Test public void invalidReloadRestoresPreviousHandlersAndRemovesNewOnes() throws Exception {
        script("quests.task('test:stable',function(p,t){return true;});"); QuestScripts.load(directory.toFile());
        script("quests.task('test:new',function(p,t){return false;});");
        try { QuestScripts.reload(directory.toFile(),()->{throw new IOException("Book still needs test:stable");}); fail(); } catch(IOException expected) {}
        assertTrue(QuestHooks.TASKS.get("test:stable").complete(null,new QuestCore.Task())); assertFalse(QuestHooks.TASKS.containsKey("test:new"));
        QuestScripts.reload(directory.toFile(),()->{}); assertFalse(QuestHooks.TASKS.containsKey("test:stable")); assertTrue(QuestHooks.TASKS.containsKey("test:new"));
    }
}
