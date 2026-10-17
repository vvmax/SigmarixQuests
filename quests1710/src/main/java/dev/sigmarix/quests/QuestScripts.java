package dev.sigmarix.quests;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.*;
import javax.script.*;
import net.minecraft.entity.player.EntityPlayerMP;
/** Java 8 Nashorn pack scripts. Only local server configuration is evaluated. */
public final class QuestScripts {
    private static final Map<String,List<Consumer<QuestEvents.Event>>> listeners=new HashMap<String,List<Consumer<QuestEvents.Event>>>();
    private static final Set<String> tasks=new HashSet<String>(),rewards=new HashSet<String>(),filters=new HashSet<String>();
    public static void clear() {
        for(String key:tasks) QuestHooks.TASKS.remove(key);
        for(String key:rewards) QuestHooks.REWARDS.remove(key);
        for(String key:filters) QuestHooks.ITEM_FILTERS.remove(key);
        tasks.clear(); rewards.clear(); filters.clear();
        for(Map.Entry<String,List<Consumer<QuestEvents.Event>>> entry:listeners.entrySet()) for(Consumer<QuestEvents.Event> handler:entry.getValue()) QuestEvents.off(entry.getKey(),handler);
        listeners.clear();
    }
    public static ScriptEngine engine() throws IOException {
        try {
            // LaunchWrapper cannot discover the JDK engine reliably through its mod class loader.
            // Load the factory through the JDK/system loader, then expose mod classes to scripts.
            Class<?> type=Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory",true,ClassLoader.getSystemClassLoader());
            Object factory=type.newInstance();
            return (ScriptEngine)type.getMethod("getScriptEngine",ClassLoader.class).invoke(factory,QuestScripts.class.getClassLoader());
        } catch(ReflectiveOperationException | LinkageError ex) { throw new IOException("Quest scripts require Java 8 Nashorn",ex); }
    }
    public interface Validation { void run() throws IOException; }
    public static void reload(File directory,Validation validation) throws IOException {
        Map<String,QuestHooks.Task> previousTasks=new HashMap<String,QuestHooks.Task>();
        Map<String,QuestHooks.Reward> previousRewards=new HashMap<String,QuestHooks.Reward>();
        Map<String,Predicate<net.minecraft.item.ItemStack>> previousFilters=new HashMap<String,Predicate<net.minecraft.item.ItemStack>>();
        Map<String,List<Consumer<QuestEvents.Event>>> previousListeners=new HashMap<String,List<Consumer<QuestEvents.Event>>>();
        for(String key:tasks) previousTasks.put(key,QuestHooks.TASKS.get(key));
        for(String key:rewards) previousRewards.put(key,QuestHooks.REWARDS.get(key));
        for(String key:filters) previousFilters.put(key,QuestHooks.ITEM_FILTERS.get(key));
        for(Map.Entry<String,List<Consumer<QuestEvents.Event>>> entry:listeners.entrySet()) previousListeners.put(entry.getKey(),new ArrayList<Consumer<QuestEvents.Event>>(entry.getValue()));
        try { load(directory); validation.run(); }
        catch(IOException | RuntimeException ex) {
            clear(); QuestHooks.TASKS.putAll(previousTasks); QuestHooks.REWARDS.putAll(previousRewards); QuestHooks.ITEM_FILTERS.putAll(previousFilters);
            tasks.addAll(previousTasks.keySet()); rewards.addAll(previousRewards.keySet()); filters.addAll(previousFilters.keySet()); listeners.putAll(previousListeners);
            for(Map.Entry<String,List<Consumer<QuestEvents.Event>>> entry:previousListeners.entrySet()) for(Consumer<QuestEvents.Event> handler:entry.getValue()) QuestEvents.on(entry.getKey(),handler);
            throw ex;
        }
    }
    public static void load(File directory) throws IOException {
        clear();
        if(!directory.exists()) { Files.createDirectories(directory.toPath()); return; }
        File[] files=directory.listFiles((dir,name)->name.endsWith(".js"));
        if(files==null) throw new IOException("Cannot list quest scripts");
        if(files.length>128) throw new IOException("Too many quest scripts");
        Arrays.sort(files,Comparator.comparing(File::getName));
        try {
            for(File file:files) {
                if(file.length()>262144) throw new IOException("Quest script exceeds 256 KiB: "+file.getName());
                ScriptEngine engine=engine();
                if(engine==null) throw new IOException("Quest scripts require Java 8 Nashorn");
                engine.put("quests",new API()); engine.put(ScriptEngine.FILENAME,file.getName());
                try(Reader reader=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)) { engine.eval(reader); }
            }
        } catch(ScriptException | RuntimeException | IOException ex) { clear(); throw new IOException("Quest script registration failed: "+ex.getMessage(),ex); }
    }
    public static final class API {
        public void task(String key,Object function) { Callback call=new Callback(function); register(key,(player,task)->Boolean.TRUE.equals(call.invoke(player,task)),QuestHooks.TASKS,tasks); }
        public void reward(String key,Object function) { Callback call=new Callback(function); register(key,(player,reward)->call.invoke(player,reward),QuestHooks.REWARDS,rewards); }
        public void filter(String key,Object function) { Callback call=new Callback(function); register(key,stack->Boolean.TRUE.equals(call.invoke(stack)),QuestHooks.ITEM_FILTERS,filters); }
        public void on(String type,Object function) { Callback call=new Callback(function); Consumer<QuestEvents.Event> handler=event->call.invoke(event); QuestEvents.on(type,handler); listeners.computeIfAbsent(type,k->new ArrayList<Consumer<QuestEvents.Event>>()).add(handler); }
        public boolean stage(EntityPlayerMP player,String key) { return service().progress().stage(player.getUniqueID(),key); }
        public void setStage(EntityPlayerMP player,String key,boolean enabled) { service().progress().stage(player.getUniqueID(),key,enabled); }
        public int skill(EntityPlayerMP player,String key) { return service().progress().skill(player.getUniqueID(),key); }
        public void setSkill(EntityPlayerMP player,String key,int level) { service().progress().skill(player.getUniqueID(),key,level); }
        public void credit(EntityPlayerMP player,String quest,String task,long amount) { service().creditTask(player,quest,task,amount); }
        public void creditId(String playerUUID,String quest,String task,long amount) { service().creditTask(UUID.fromString(playerUUID),quest,task,amount); }
        public EntityPlayerMP player(UUID id) {
            for(Object raw:net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayerMP player=(EntityPlayerMP)raw; if(player.getUniqueID().equals(id)) return player;
            }
            return null;
        }
        private QuestService service() { if(QuestsMod.service==null) throw new IllegalStateException("Quest server is not running"); return QuestsMod.service; }
        private <T> void register(String key,T handler,Map<String,T> target,Set<String> owned) {
            QuestAttributes.validateKey(key);
            if(handler==null || target.containsKey(key)) throw new IllegalArgumentException("Duplicate or empty quest script handler: "+key);
            target.put(key,handler); owned.add(key);
        }
    }
    private static final class Callback {
        final Object function;
        final java.lang.reflect.Method call;
        Callback(Object function) {
            try {
                if(function==null || !Boolean.TRUE.equals(function.getClass().getMethod("isFunction").invoke(function))) throw new IllegalArgumentException("Expected a JavaScript function");
                this.function=function; call=function.getClass().getMethod("call",Object.class,Object[].class);
            } catch(ReflectiveOperationException ex) { throw new IllegalArgumentException("Expected a JavaScript function",ex); }
        }
        Object invoke(Object... arguments) {
            try { return call.invoke(function,null,arguments); }
            catch(java.lang.reflect.InvocationTargetException ex) { throw new IllegalStateException("Quest script callback failed",ex.getCause()); }
            catch(ReflectiveOperationException ex) { throw new IllegalStateException("Quest script callback failed",ex); }
        }
    }
    private QuestScripts() {}
}
