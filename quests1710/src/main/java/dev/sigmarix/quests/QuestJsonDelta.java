package dev.sigmarix.quests;

import com.google.gson.*;
import java.util.*;

/** Object-field patches; book lists are keyed by stable IDs to avoid shifting unchanged quests. */
public final class QuestJsonDelta {
    public static final class Change {
        public List<String> path=new ArrayList<String>();
        public boolean remove;
        public JsonElement value;
    }
    public static final class Patch { public List<Change> changes=new ArrayList<Change>(); }
    public static JsonObject copy(JsonObject value) { return new JsonParser().parse(value.toString()).getAsJsonObject(); }
    public static Patch diff(JsonObject before,JsonObject after) {
        Patch patch=new Patch();diff(before,after,new ArrayList<String>(),patch);return patch;
    }
    private static void diff(JsonObject before,JsonObject after,List<String> parent,Patch patch) {
        for(Map.Entry<String,JsonElement> entry:before.entrySet()) if(!after.has(entry.getKey())) {
            Change change=new Change();change.path.addAll(parent);change.path.add(entry.getKey());change.remove=true;patch.changes.add(change);
        }
        for(Map.Entry<String,JsonElement> entry:after.entrySet()) {
            JsonElement old=before.get(entry.getKey()),next=entry.getValue();if(next.equals(old))continue;
            List<String> path=new ArrayList<String>(parent);path.add(entry.getKey());
            if(old!=null && old.isJsonObject() && next.isJsonObject()) diff(old.getAsJsonObject(),next.getAsJsonObject(),path,patch);
            else { Change change=new Change();change.path=path;change.value=next;patch.changes.add(change); }
        }
    }
    public static JsonObject apply(JsonObject before,Patch patch) {
        if(patch==null || patch.changes==null || patch.changes.size()>262144)throw new IllegalArgumentException("Invalid quest delta");
        JsonObject result=copy(before);
        for(Change change:patch.changes) {
            if(change==null || change.path==null || change.path.isEmpty() || change.path.size()>64)throw new IllegalArgumentException("Invalid quest delta path");
            JsonObject target=result;
            for(int i=0;i<change.path.size();i++) {
                String key=change.path.get(i);if(key==null || key.length()>256)throw new IllegalArgumentException("Invalid quest delta key");
                if(i==change.path.size()-1) { if(change.remove)target.remove(key);else target.add(key,change.value==null?JsonNull.INSTANCE:change.value); }
                else { JsonElement child=target.get(key);if(child==null || !child.isJsonObject())throw new IllegalArgumentException("Missing quest delta parent");target=child.getAsJsonObject(); }
            }
        }
        return result;
    }
    public static JsonObject book(QuestCore.Book book) { return normalizeBook(QuestService.JSON.toJsonTree(book).getAsJsonObject()); }
    public static JsonObject normalizeBook(JsonObject book) {
        for(String name:new String[]{"quests","chapters"}) {
            JsonArray list=book.getAsJsonArray(name);JsonObject entries=new JsonObject();JsonArray order=new JsonArray();
            for(JsonElement value:list) {
                JsonObject object=value.getAsJsonObject();String id=object.get("id").getAsString();
                if(!QuestCore.validId(id) || entries.has(id))throw new IllegalArgumentException("Invalid quest delta object ID");
                entries.add(id,value);order.add(new JsonPrimitive(id));
            }
            book.add(name,entries);book.add("_"+name+"Order",order);
        }
        return book;
    }
    public static JsonObject restoreBook(JsonObject book) {
        for(String name:new String[]{"quests","chapters"}) {
            JsonObject entries=book.getAsJsonObject(name);JsonArray order=book.getAsJsonArray("_"+name+"Order"),list=new JsonArray();Set<String> seen=new HashSet<String>();
            for(JsonElement key:order) {
                String id=key.getAsString();JsonElement value=entries.get(id);
                if(value==null || !seen.add(id) || !id.equals(value.getAsJsonObject().get("id").getAsString()))throw new IllegalArgumentException("Invalid quest delta order");
                list.add(value);
            }
            if(seen.size()!=entries.entrySet().size())throw new IllegalArgumentException("Incomplete quest delta order");
            book.add(name,list);book.remove("_"+name+"Order");
        }
        return book;
    }
    public static QuestCore.Book applyBook(QuestCore.Book before,Patch patch) {
        return QuestService.JSON.fromJson(restoreBook(apply(book(before),patch)),QuestCore.Book.class);
    }
}
