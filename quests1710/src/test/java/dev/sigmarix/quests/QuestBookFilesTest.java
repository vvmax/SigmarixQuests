package dev.sigmarix.quests;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class QuestBookFilesTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private Path root()throws Exception {Path root=temporary.newFolder().toPath();Files.createDirectory(root.resolve("chapters"));Files.write(root.resolve("quests.json"),"{format:2,title:'External',chapterDirectory:'chapters',chapters:[],quests:[]}".getBytes(StandardCharsets.UTF_8));return root;}
    private void chapter(Path root,String file,String id)throws Exception {String json="{format:2,chapters:[{id:'"+id+"',title:'Глава'}],quests:[{id:'"+id+"_start',title:'Начало',chapter:'"+id+"',tasks:[{type:'check'}]}]}";Files.write(root.resolve("chapters/"+file+".json"),json.getBytes(StandardCharsets.UTF_8));}
    @Test public void externalChaptersLoadTogetherAndGuiSavesKeepThemSeparate()throws Exception {
        Path root=root();chapter(root,"01_first","first");chapter(root,"02_second","second");
        QuestBookFiles.Loaded loaded=QuestBookFiles.read(root.resolve("quests.json").toFile());loaded.book.validate();assertEquals(2,loaded.book.quests.size());
        QuestCore.Book invalid=QuestService.JSON.fromJson(QuestService.JSON.toJson(loaded.book),QuestCore.Book.class);invalid.chapterDirectory="other";
        try{QuestBookFiles.write(root.resolve("quests.json").toFile(),invalid,loaded);fail();}catch(java.io.IOException expected){}
        loaded.book.quests.get(0).title="Изменённый квест";
        // Moving a quest routes it to the file owning its new chapter.
        loaded.book.quests.get(0).chapter="second";
        QuestBookFiles.write(root.resolve("quests.json").toFile(),loaded.book,loaded);
        JsonObject main=new JsonParser().parse(new String(Files.readAllBytes(root.resolve("quests.json")),StandardCharsets.UTF_8)).getAsJsonObject();assertEquals(0,main.getAsJsonArray("quests").size());
        QuestBookFiles.Loaded again=QuestBookFiles.read(root.resolve("quests.json").toFile());again.book.validate();assertEquals(2,again.book.quests.size());assertEquals("Изменённый квест",again.book.quests.get(0).title);assertEquals("second",again.book.quests.get(0).chapter);
        assertTrue(Files.exists(root.resolve("chapters/01_first.json.bak")));assertTrue(Files.exists(root.resolve("quests.json.bak")));
    }
    @Test public void aFailedLaterFileRestoresEarlierDocuments()throws Exception {
        Path root=root();chapter(root,"a","a");chapter(root,"b","b");QuestBookFiles.Loaded loaded=QuestBookFiles.read(root.resolve("quests.json").toFile());byte[] before=Files.readAllBytes(root.resolve("chapters/a.json"));loaded.book.quests.get(0).title="Другое";
        Files.delete(root.resolve("chapters/b.json"));Files.createDirectory(root.resolve("chapters/b.json"));
        try{QuestBookFiles.write(root.resolve("quests.json").toFile(),loaded.book,loaded);fail();}catch(java.io.IOException expected){}
        assertArrayEquals(before,Files.readAllBytes(root.resolve("chapters/a.json")));
    }
    @Test public void rejectsDuplicateForeignAndUnsafeDocuments()throws Exception {
        Path root=root();chapter(root,"a","a");chapter(root,"b","a");
        try{QuestBookFiles.read(root.resolve("quests.json").toFile());fail();}catch(java.io.IOException expected){}
        Files.delete(root.resolve("chapters/b.json"));String wrong="{chapters:[{id:'b',title:'B'}],quests:[{id:'wrong',title:'Wrong',chapter:'a'}]}";Files.write(root.resolve("chapters/b.json"),wrong.getBytes(StandardCharsets.UTF_8));
        try{QuestBookFiles.read(root.resolve("quests.json").toFile());fail();}catch(java.io.IOException expected){}
        for(String path:new String[]{"..","../outside","/tmp","a/b"})try{QuestBookFiles.directory(path);fail(path);}catch(IllegalArgumentException expected){}
    }
    @Test public void suppliedSixModPackHasValidLinksAndNoConsumableTasks()throws Exception {
        Path config=Paths.get("../quest-packs/ru-six-mods-1.7.10/config/sigmarixquests/quests.json");
        QuestBookFiles.Loaded loaded=QuestBookFiles.read(config.toFile());loaded.book.validate();assertEquals(6,loaded.book.chapters.size());assertEquals(180,loaded.book.quests.size());
        String payload=QuestService.JSON.toJson(loaded.book);assertTrue(payload.getBytes(StandardCharsets.UTF_8).length<900000);assertTrue(QuestNetwork.encode(payload).length<QuestNetwork.MAX_COMPRESSED);
        for(QuestCore.Quest q:loaded.book.quests){assertTrue(q.title.matches(".*[А-Яа-яЁё].*"));assertFalse(q.tasks.get(0).consume);if(q.tasks.get(0).type.equals("check"))assertTrue(q.tasks.get(0).manual);}
    }
}
