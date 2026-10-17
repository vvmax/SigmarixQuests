package dev.sigmarix.quests;
import com.google.gson.*;
import java.util.*;
/** Draft operations shared by object forms and tests. Server validation remains authoritative. */
public final class QuestEditing {
    public static JsonElement tree(Object value) { return new GsonBuilder().serializeNulls().create().toJsonTree(value); }
    public static JsonElement copy(JsonElement value) { return new JsonParser().parse(value.toString()); }
    public static JsonElement template(String field) {
        Object value;
        if(field.equals("quests")) { QuestCore.Quest q=new QuestCore.Quest(); q.id=id("quest"); q.title="New quest"; QuestCore.Task t=new QuestCore.Task(); t.type="check"; t.manual=true; t.id="confirm"; q.tasks.add(t); value=q; }
        else if(field.equals("chapters")) { QuestCore.Chapter c=new QuestCore.Chapter(); c.id=id("chapter"); c.title="New chapter"; value=c; }
        else if(field.equals("tasks")) { QuestCore.Task t=new QuestCore.Task(); t.id=id("task"); t.type="check"; t.manual=true; value=t; }
        else if(field.equals("rewards") || field.equals("options") || field.equals("lootEntries")) { QuestCore.Reward r=new QuestCore.Reward(); r.id=id("reward"); r.type="xp"; value=r; }
        else if(field.equals("richPages")) value=new QuestPresentation.GuidePage();
        else if(field.equals("elements")) value=new QuestPresentation.GuideElement();
        else if(field.equals("attachments")) { QuestCore.MailItem item=new QuestCore.MailItem(); item.item="minecraft:diamond"; value=item; }
        else if(field.equals("emergencyItems")) { QuestCore.Reward reward=new QuestCore.Reward(); reward.item="minecraft:bread"; value=reward; }
        else if(field.equals("children")) value=new QuestItemFilter();
        else if(field.equals("styles")) value=new QuestPresentation.Style();
        else value="";
        return tree(value);
    }
    public static String id(String prefix) { return prefix+"_"+UUID.randomUUID().toString().replace("-","").substring(0,12); }
    public static JsonElement duplicate(JsonElement source) {
        JsonElement result=copy(source);
        if(result.isJsonObject() && result.getAsJsonObject().has("id")) result.getAsJsonObject().addProperty("id",id("copy"));
        return result;
    }
    public static void move(JsonArray array,int index,int delta) {
        int target=index+delta; if(index<0 || index>=array.size() || target<0 || target>=array.size()) return;
        JsonElement value=array.get(index); set(array,index,array.get(target)); set(array,target,value);
    }
    public static void set(JsonArray array,int index,JsonElement value) {
        List<JsonElement> values=new ArrayList<JsonElement>(); for(JsonElement e:array) values.add(e); values.set(index,value); replace(array,values);
    }
    public static void remove(JsonArray array,int index) { List<JsonElement> values=new ArrayList<JsonElement>(); for(JsonElement e:array) values.add(e); values.remove(index); replace(array,values); }
    private static void replace(JsonArray array,List<JsonElement> values) { java.util.Iterator<JsonElement> i=array.iterator(); while(i.hasNext()) { i.next(); i.remove(); } for(JsonElement e:values) array.add(e); }
    public static List<String> keys(JsonObject object) { List<String> result=new ArrayList<String>(); for(Map.Entry<String,JsonElement> e:object.entrySet()) result.add(e.getKey()); return result; }
}
