package dev.sigmarix.quests;

import com.google.gson.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestSnapshotSyncTest {
    private QuestNetwork.Snapshot snapshot() {
        QuestNetwork.Snapshot snapshot=new QuestNetwork.Snapshot();snapshot.revision=1;snapshot.canEdit=true;snapshot.book=new QuestCore.Book();
        char[] text=new char[15000];Arrays.fill(text,'x');
        for(int i=0;i<80;i++) {
            QuestCore.Quest quest=new QuestCore.Quest();quest.id="quest_"+i;quest.title="Квест "+i;quest.description=new String(text);
            QuestCore.Task task=new QuestCore.Task();task.id="check";task.type="check";quest.tasks.add(task);snapshot.book.quests.add(quest);
            snapshot.states.put(quest.id,"available");snapshot.counters.put(quest.id+":check",0L);
        }
        return snapshot;
    }
    private QuestSnapshotSync.Frame frame(String json) { return QuestService.JSON.fromJson(json,QuestSnapshotSync.Frame.class); }
    @Test public void singleFieldSaveAndProgressSyncDoNotResendLargeBook() {
        QuestSnapshotSync.Server server=new QuestSnapshotSync.Server();QuestSnapshotSync.Client client=new QuestSnapshotSync.Client();UUID id=UUID.randomUUID();
        QuestNetwork.Snapshot first=snapshot();String initial=server.next(id,first);assertTrue(initial.length()>1024*1024);
        client.receive(frame(initial));
        QuestNetwork.Action action=new QuestNetwork.Action();action.action="save";action.revision=1;
        action.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(first.book),QuestCore.Book.class);
        action.book.quests.get(37).title="Новое название";
        QuestNetwork.Action edit=client.save(action);assertNull(edit.book);assertNotNull(edit.bookDelta);assertNotNull(action.book);
        assertTrue(QuestService.JSON.toJson(edit).length()<2048);
        QuestCore.Book saved=QuestJsonDelta.applyBook(first.book,edit.bookDelta);saved.validate();assertEquals("Новое название",saved.quests.get(37).title);
        assertEquals(first.book.quests.get(79).description,saved.quests.get(79).description);
        assertEquals("Квест 37",first.book.quests.get(37).title);
        first.book=saved;first.revision=2;first.counters.put("quest_37:check",1L);first.states.put("quest_37","complete");
        String update=server.next(id,first);assertNull(frame(update).full);assertTrue(update.length()<2048);
        QuestNetwork.Snapshot received=client.receive(frame(update));
        assertEquals(QuestService.JSON.toJson(first),QuestService.JSON.toJson(received));
        String unchanged=server.next(id,first);assertTrue(unchanged.length()<128);assertTrue(frame(unchanged).delta.changes.isEmpty());
        assertEquals(2,client.receive(frame(unchanged)).revision);
    }
    @Test public void questInsertDeleteReorderAndNestedFieldsKeepStableIds() {
        QuestCore.Book before=snapshot().book;QuestCore.Book after=QuestService.JSON.fromJson(QuestService.JSON.toJson(before),QuestCore.Book.class);
        after.quests.remove(17);Collections.reverse(after.quests);QuestCore.Quest added=new QuestCore.Quest();added.id="new";added.title="Новый";after.quests.add(3,added);
        after.theme.lineWidth=3;after.quests.get(5).tasks.get(0).count=3;
        QuestJsonDelta.Patch patch=QuestJsonDelta.diff(QuestJsonDelta.book(before),QuestJsonDelta.book(after));
        assertTrue(QuestService.JSON.toJson(patch).length()<5000);
        patch=QuestService.JSON.fromJson(QuestService.JSON.toJson(patch),QuestJsonDelta.Patch.class);
        QuestCore.Book restored=QuestJsonDelta.applyBook(before,patch);restored.validate();
        assertEquals(QuestService.JSON.toJson(after),QuestService.JSON.toJson(restored));
        assertEquals(80,before.quests.size());assertEquals("quest_0",before.quests.get(0).id);
    }
    @Test public void droppedDeltaAndReconnectRecoverWithFullSnapshot() {
        UUID id=UUID.randomUUID();QuestSnapshotSync.Server server=new QuestSnapshotSync.Server();QuestSnapshotSync.Client client=new QuestSnapshotSync.Client();QuestNetwork.Snapshot snapshot=snapshot();
        client.receive(frame(server.next(id,snapshot)));snapshot.message="Dropped";server.next(id,snapshot);
        snapshot.message="Latest";QuestSnapshotSync.Frame missed=frame(server.next(id,snapshot));
        try { client.receive(missed);fail(); } catch(IllegalArgumentException expected) {}
        server.forget(id);QuestSnapshotSync.Frame refresh=frame(server.next(id,snapshot));assertNotNull(refresh.full);
        assertEquals("Latest",client.receive(refresh).message);
        client.clear();snapshot.message="After reconnect";
        try { client.receive(frame(server.next(id,snapshot)));fail(); } catch(IllegalArgumentException expected) {}
        server.forget(id);assertEquals("After reconnect",client.receive(frame(server.next(id,snapshot))).message);
    }
    @Test public void viewerBaselinesAreIsolatedAndEvictionUsesFullSnapshots() {
        QuestSnapshotSync.Server server=new QuestSnapshotSync.Server();QuestNetwork.Snapshot snapshot=snapshot();UUID owner=UUID.randomUUID();
        snapshot.book.quests.get(0).description="SECRET COMMAND";server.next(owner,snapshot);
        QuestNetwork.Snapshot publicView=snapshot();publicView.canEdit=false;UUID viewer=UUID.randomUUID();
        String publicFrame=server.next(viewer,publicView);assertNotNull(frame(publicFrame).full);assertFalse(publicFrame.contains("SECRET COMMAND"));
        publicView.book.quests.remove(0);String hidden=server.next(viewer,publicView);assertNull(frame(hidden).full);assertFalse(hidden.contains("SECRET COMMAND"));
        for(int n=0;n<17;n++)server.next(UUID.randomUUID(),publicView);
        assertNotNull(frame(server.next(owner,snapshot)).full);
    }
    @Test public void staleEditorDoesNotBuildDeltaAgainstNewerBook() {
        QuestSnapshotSync.Server server=new QuestSnapshotSync.Server();QuestSnapshotSync.Client client=new QuestSnapshotSync.Client();QuestNetwork.Snapshot snapshot=snapshot();
        client.receive(frame(server.next(UUID.randomUUID(),snapshot)));
        QuestNetwork.Action stale=new QuestNetwork.Action();stale.action="save";stale.revision=0;stale.book=snapshot.book;
        assertSame(stale,client.save(stale));assertNull(stale.bookDelta);
    }
    @Test public void invalidPatchDoesNotMutateBaseline() {
        JsonObject before=new JsonParser().parse("{a:{b:1},c:2}").getAsJsonObject();
        QuestJsonDelta.Patch patch=new QuestJsonDelta.Patch();QuestJsonDelta.Change change=new QuestJsonDelta.Change();change.path=Arrays.asList("a","b");change.value=new JsonPrimitive(5);patch.changes.add(change);
        change=new QuestJsonDelta.Change();change.path=Arrays.asList("missing","bad");change.value=new JsonPrimitive(6);patch.changes.add(change);
        try { QuestJsonDelta.apply(before,patch);fail(); } catch(IllegalArgumentException expected) {}
        assertEquals(1,before.getAsJsonObject("a").get("b").getAsInt());
        QuestCore.Book book=snapshot().book;JsonObject normalized=QuestJsonDelta.book(book);normalized.getAsJsonArray("_questsOrder").add(new JsonPrimitive("quest_0"));
        try { QuestJsonDelta.restoreBook(normalized);fail(); } catch(IllegalArgumentException expected) {}
    }
}
