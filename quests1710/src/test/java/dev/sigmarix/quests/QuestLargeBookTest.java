package dev.sigmarix.quests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Validates authored data without changing the mod transport implementation. */
public class QuestLargeBookTest {
    private Path config() {return Paths.get("../quest-packs/ru-expert-sigma-1.7.10/config/sigmarixquests");}
    @Test public void revisedCampaignKeepsParallelBranchesAndIllustrationsAfterNativeRoundTrip() throws Exception {
        QuestCore.Book original=QuestBookFiles.read(config().resolve("quests.json").toFile()).book;
        QuestCore.Book book=QuestService.JSON.fromJson(QuestService.JSON.toJson(original),QuestCore.Book.class);book.validate();
        Map<String,QuestCore.Quest> byId=new HashMap<String,QuestCore.Quest>();
        int xp=0,paid=0,images=0;
        for(QuestCore.Quest q:book.quests){
            byId.put(q.id,q);
            for(QuestCore.Reward reward:q.rewards){assertEquals("xp",reward.type);assertEquals(3,reward.count);xp+=reward.count;paid++;}
            for(QuestPresentation.GuidePage page:q.richPages)for(QuestPresentation.GuideElement element:page.elements)if(element.type.equals("image")){
                String[] ref=element.resource.split(":",2);
                assertTrue(Files.isRegularFile(Paths.get("../quest-packs/ru-expert-sigma-1.7.10/resourcepacks/Sigma-Quest-Guides/assets",ref[0],ref[1])));images++;
            }
        }
        assertEquals(48,xp);assertEquals(16,paid);assertTrue(images>=9);
        assertTrue(byId.get("ae_order").dependencies.containsAll(Arrays.asList("ae_patterns","ae_assembler","ae_cpu")));
        for(String[] pair:new String[][]{{"ic2_macerator","ic2_extractor"},{"ic2_geothermal","ic2_solar"},{"gen_apiary","gen_template"}}){
            assertFalse(ancestors(pair[0],byId,new HashSet<String>()).contains(pair[1]));
            assertFalse(ancestors(pair[1],byId,new HashSet<String>()).contains(pair[0]));
        }
    }
    private Set<String> ancestors(String id,Map<String,QuestCore.Quest> byId,Set<String> found){
        for(String parent:byId.get(id).dependencies)if(found.add(parent))ancestors(parent,byId,found);return found;
    }
    @Test public void sigmaBookHasValidNativeSchemaAndCrossChapterDependencies() throws Exception {
        QuestBookFiles.Loaded loaded=QuestBookFiles.read(config().resolve("quests.json").toFile());
        loaded.book.validate();assertEquals(696,loaded.book.quests.size());assertEquals(53,loaded.book.chapters.size());
        assertEquals(46,loaded.fragments.size());
        for(QuestCore.Quest quest:loaded.book.quests) {
            assertTrue(quest.title.matches(".*[А-Яа-яЁё].*"));assertFalse(quest.repeatable);
            for(QuestCore.Task task:quest.tasks) {
                assertNotEquals("check",task.type);assertFalse(task.title.isEmpty());
                // Machine, infusion and AE recipes must not depend on the vanilla crafting event.
                assertFalse(task.craftingOnly);
            }
        }
    }
    @Test public void eachChapterFitsExistingFiniteWireLimitAndRoundTrips() throws Exception {
        try(DirectoryStream<Path> stream=Files.newDirectoryStream(config().resolve("chapters"),"*.json")) {
            for(Path file:stream) {
                QuestCore.Book chapter=QuestService.JSON.fromJson(new String(Files.readAllBytes(file),StandardCharsets.UTF_8),QuestCore.Book.class);
                String json=QuestService.JSON.toJson(chapter);
                assertTrue(file.getFileName().toString(),json.getBytes(StandardCharsets.UTF_8).length<900000);
                assertEquals(json,QuestNetwork.decode(QuestNetwork.encode(json)));
                QuestNetwork.RequestAssembly assembly=new QuestNetwork.RequestAssembly();String result=null;
                for(QuestNetwork.RequestChunk chunk:QuestNetwork.requestChunks(json)) {
                    assertTrue(chunk.data.length+9<32767);result=assembly.accept(chunk,0);
                }
                assertEquals(json,result);
            }
        }
    }
    @Test public void packRegistersEveryCustomCheckWithNativeJava8ScriptApi() throws Exception {
        try {
            QuestScripts.load(config().resolve("scripts").toFile());
            QuestCore.Book book=QuestBookFiles.read(config().resolve("quests.json").toFile()).book;
            for(QuestCore.Quest quest:book.quests) for(QuestCore.Task task:quest.tasks)
                if(task.type.equals("custom"))assertTrue(task.target,QuestHooks.TASKS.containsKey(task.target));
            assertEquals(4,QuestHooks.TASKS.size());
        } finally {QuestScripts.clear();}
    }
    @Test public void fullCampaignSavesReloadsAndTransfersOn027() throws Exception {
        QuestCore.Book book=QuestBookFiles.read(config().resolve("quests.json").toFile()).book;book.validate();
        String json=QuestService.JSON.toJson(book);
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length>1024*1024);
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length<QuestNetwork.MAX_BOOK_BYTES);
        QuestNetwork.Snapshot snapshot=new QuestNetwork.Snapshot();snapshot.book=book;
        for(QuestCore.Quest quest:book.quests) {
            snapshot.states.put(quest.id,"available");snapshot.percentages.put(quest.id,100);
            for(QuestCore.Task task:quest.tasks) {snapshot.counters.put(quest.id+":"+task.id,Long.MAX_VALUE);snapshot.taskStates.put(quest.id+":"+task.id,false);}
        }
        String wire=QuestService.JSON.toJson(snapshot);
        for(boolean response:new boolean[]{false,true}) {
            QuestNetwork.RequestAssembly assembly=new QuestNetwork.RequestAssembly();String result=null;
            List<QuestNetwork.RequestChunk> chunks=response?QuestNetwork.responseChunks(wire):QuestNetwork.requestChunks(wire);
            assertTrue(chunks.size()>1);
            for(QuestNetwork.RequestChunk chunk:chunks) {
                assertEquals(response,chunk instanceof QuestNetwork.ResponseChunk);
                io.netty.buffer.ByteBuf buffer=io.netty.buffer.Unpooled.buffer();
                chunk.toBytes(buffer);assertTrue(buffer.readableBytes()+1<32767);
                QuestNetwork.RequestChunk received=response?new QuestNetwork.ResponseChunk():new QuestNetwork.RequestChunk();
                received.fromBytes(buffer);buffer.release();result=assembly.accept(received,0);
            }
            assertEquals(wire,result);
        }
        Path temp=Files.createTempDirectory("sigma-book-027-");
        try {
            Files.createDirectory(temp.resolve("chapters"));
            Files.copy(config().resolve("quests.json"),temp.resolve("quests.json"));
            try(DirectoryStream<Path> files=Files.newDirectoryStream(config().resolve("chapters"),"*.json")) {
                for(Path file:files) Files.copy(file,temp.resolve("chapters").resolve(file.getFileName()));
            }
            QuestBookFiles.Loaded copy=QuestBookFiles.read(temp.resolve("quests.json").toFile());
            QuestBookFiles.write(temp.resolve("quests.json").toFile(),copy.book,copy);
            QuestCore.Book again=QuestBookFiles.read(temp.resolve("quests.json").toFile()).book;
            again.validate();assertEquals(json,QuestService.JSON.toJson(again));
        } finally {
            try(java.util.stream.Stream<Path> files=Files.walk(temp)) {
                Iterator<Path> it=files.sorted(Comparator.reverseOrder()).iterator();
                while(it.hasNext()) {Path file=it.next();assertTrue(file.toAbsolutePath().normalize().startsWith(temp.toAbsolutePath().normalize()));Files.delete(file);}
            }
        }
    }
    @Test public void campaignProgressAndOneQuestEditUse028DeltasAndRecoverBaseline() throws Exception {
        QuestCore.Book book=QuestBookFiles.read(config().resolve("quests.json").toFile()).book;
        QuestNetwork.Snapshot snapshot=new QuestNetwork.Snapshot();snapshot.book=book;snapshot.revision=1;
        QuestSnapshotSync.Server server=new QuestSnapshotSync.Server();QuestSnapshotSync.Client client=new QuestSnapshotSync.Client();UUID player=UUID.randomUUID();
        String initial=server.next(player,snapshot);
        QuestNetwork.Snapshot first=client.receive(QuestService.JSON.fromJson(initial,QuestSnapshotSync.Frame.class));
        assertEquals(QuestService.JSON.toJson(book),QuestService.JSON.toJson(first.book));
        snapshot.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(book),QuestCore.Book.class);
        snapshot.book.quests.get(350).description+=" Проверка дельты.";
        snapshot.states.put("start_wood","complete");snapshot.revision=2;
        String change=server.next(player,snapshot);assertTrue(change.length()<initial.length()/100);
        QuestSnapshotSync.Frame frame=QuestService.JSON.fromJson(QuestNetwork.decode(QuestNetwork.encode(change)),QuestSnapshotSync.Frame.class);
        assertNull(frame.full);assertNotNull(frame.delta);
        QuestNetwork.Snapshot restored=client.receive(frame);restored.book.validate();
        assertEquals(QuestService.JSON.toJson(snapshot.book),QuestService.JSON.toJson(restored.book));
        assertEquals("complete",restored.states.get("start_wood"));
        QuestNetwork.Action edit=new QuestNetwork.Action();edit.action="save";edit.revision=2;
        edit.book=QuestService.JSON.fromJson(QuestService.JSON.toJson(restored.book),QuestCore.Book.class);edit.book.quests.get(350).title+=" — проверка";
        QuestNetwork.Action wire=client.save(edit);assertNull(wire.book);assertNotNull(wire.bookDelta);
        QuestCore.Book edited=QuestJsonDelta.applyBook(restored.book,wire.bookDelta);edited.validate();
        assertEquals(QuestService.JSON.toJson(edit.book),QuestService.JSON.toJson(edited));
        QuestSnapshotSync.Client recovered=new QuestSnapshotSync.Client();
        try {recovered.receive(frame);fail("Missing baseline accepted");} catch(IllegalArgumentException expected) {}
        server.forget(player);
        QuestNetwork.Snapshot refreshed=recovered.receive(QuestService.JSON.fromJson(server.next(player,snapshot),QuestSnapshotSync.Frame.class));
        assertEquals(QuestService.JSON.toJson(snapshot.book),QuestService.JSON.toJson(refreshed.book));
    }
}
