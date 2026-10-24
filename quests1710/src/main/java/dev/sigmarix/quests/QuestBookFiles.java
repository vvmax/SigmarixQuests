package dev.sigmarix.quests;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Optional external chapter documents, kept separate when the GUI saves the book. */
final class QuestBookFiles {
    static final class Loaded {
        QuestCore.Book book;
        final Map<String,Path> owners=new LinkedHashMap<String,Path>();
        final List<Path> fragments=new ArrayList<Path>();
    }
    static Loaded read(File config) throws IOException {
        Loaded result=new Loaded();JsonObject root=readObject(config.toPath());
        result.book=QuestService.JSON.fromJson(root,QuestCore.Book.class);
        if(!root.has("format")) for(int i=0;i<result.book.quests.size();i++){result.book.quests.get(i).x=(i%6)*3;result.book.quests.get(i).y=(i/6)*2;}
        String directory=result.book.chapterDirectory;
        if(directory==null || directory.isEmpty())return result;
        directory(directory);
        Path folder=config.toPath().toAbsolutePath().getParent().resolve(directory);
        if(!Files.isDirectory(folder))throw new IOException("Missing chapter directory: "+directory);
        List<Path> files=new ArrayList<Path>();
        try(DirectoryStream<Path> stream=Files.newDirectoryStream(folder,"*.json")){for(Path p:stream)files.add(p);}
        Collections.sort(files);if(files.size()>64)throw new IOException("Too many chapter files (64 maximum)");
        long size=config.length();
        for(Path file:files){size+=Files.size(file);if(size>QuestNetwork.MAX_BOOK_BYTES)throw new IOException("Quest book exceeds 16 MiB");
            JsonObject fragment=readObject(file);
            for(Map.Entry<String,JsonElement> e:fragment.entrySet())if(!Arrays.asList("format","chapters","quests").contains(e.getKey()))throw new IOException("Unsupported chapter file field: "+e.getKey());
            QuestCore.Book part=QuestService.JSON.fromJson(fragment,QuestCore.Book.class);
            for(QuestCore.Chapter chapter:part.chapters){if(result.owners.put(chapter.id,file)!=null)throw new IOException("Duplicate chapter file ID: "+chapter.id);result.book.chapters.add(chapter);}
            for(QuestCore.Quest quest:part.quests){if(!part.chapters.stream().anyMatch(c->c.id.equals(quest.chapter)))throw new IOException("Quest must belong to its chapter file: "+quest.id);result.book.quests.add(quest);}
            result.fragments.add(file);
        }
        return result;
    }
    private static JsonObject readObject(Path path)throws IOException {
        if(Files.size(path)>QuestNetwork.MAX_BOOK_BYTES)throw new IOException("Quest book exceeds 16 MiB");
        String text=new String(Files.readAllBytes(path),StandardCharsets.UTF_8);QuestNetwork.guardJson(text);
        try{return new JsonParser().parse(text).getAsJsonObject();}catch(RuntimeException e){throw new IOException("Invalid quest JSON: "+path.getFileName(),e);}
    }
    static void directory(String value){if(!value.matches("[a-zA-Z0-9_-]{1,64}"))throw new IllegalArgumentException("Invalid chapter directory");}
    static void write(File config,QuestCore.Book next,Loaded layout)throws IOException {
        if(layout!=null && !next.chapterDirectory.equals(layout.book.chapterDirectory))throw new IOException("Chapter directory cannot change in the GUI");
        JsonObject root=QuestService.JSON.toJsonTree(next).getAsJsonObject();
        Map<Path,JsonObject> documents=new LinkedHashMap<Path,JsonObject>();
        if(layout!=null && !layout.fragments.isEmpty() && !next.chapterDirectory.isEmpty()){
            for(Path file:layout.fragments){JsonObject part=new JsonObject();part.addProperty("format",2);part.add("chapters",new JsonArray());part.add("quests",new JsonArray());documents.put(file,part);}
            root.add("chapters",new JsonArray());root.add("quests",new JsonArray());
            for(QuestCore.Chapter chapter:next.chapters){Path file=layout.owners.get(chapter.id);JsonObject destination=documents.containsKey(file)?documents.get(file):root;destination.getAsJsonArray("chapters").add(QuestService.JSON.toJsonTree(chapter));}
            for(QuestCore.Quest quest:next.quests){Path file=layout.owners.get(quest.chapter);JsonObject destination=documents.containsKey(file)?documents.get(file):root;destination.getAsJsonArray("quests").add(QuestService.JSON.toJsonTree(quest));}
        }
        documents.put(config.toPath().toAbsolutePath(),root);
        Map<Path,byte[]> encoded=new LinkedHashMap<Path,byte[]>();long sourceBytes=0;
        for(Map.Entry<Path,JsonObject> e:documents.entrySet()) {
            byte[] bytes=QuestService.JSON.toJson(e.getValue()).getBytes(StandardCharsets.UTF_8);
            sourceBytes+=bytes.length;if(sourceBytes>QuestNetwork.MAX_BOOK_BYTES)throw new IOException("Quest book exceeds 16 MiB");
            encoded.put(e.getKey(),bytes);
        }
        Map<Path,byte[]> previous=new LinkedHashMap<Path,byte[]>();List<Path> changed=new ArrayList<Path>();
        try{
            for(Map.Entry<Path,JsonObject> e:documents.entrySet()){
                Path path=e.getKey();byte[] old=Files.exists(path)?Files.readAllBytes(path):null;previous.put(path,old);
                if(old!=null)Files.copy(path,path.resolveSibling(path.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
                replace(path,encoded.get(path));changed.add(path);
            }
        }catch(IOException failure){Collections.reverse(changed);for(Path path:changed)try{byte[] old=previous.get(path);if(old==null)Files.deleteIfExists(path);else replace(path,old);}catch(IOException rollback){failure.addSuppressed(rollback);}throw failure;}
    }
    private static void replace(Path path,byte[] bytes)throws IOException {
        Path temporary=path.resolveSibling(path.getFileName()+".tmp");
        try{Files.write(temporary,bytes);try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temporary);}
    }
}
