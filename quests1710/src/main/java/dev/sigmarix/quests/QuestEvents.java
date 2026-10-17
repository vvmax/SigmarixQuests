package dev.sigmarix.quests;
import java.util.*;
import java.util.function.Consumer;
/** Server-thread lifecycle events for Java adapters and native server scripts. */
public final class QuestEvents {
    private static final Map<String,List<Consumer<Event>>> listeners=new LinkedHashMap<String,List<Consumer<Event>>>();
    public static final Set<String> TYPES=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("quest_started","task_started","task_completed","quest_completed","reward_claimed")));
    public static final class Event {
        public final String type,quest,task;
        public final UUID player;
        Event(String type,UUID player,String quest,String task) { this.type=type; this.player=player; this.quest=quest; this.task=task; }
    }
    private static final ThreadLocal<Boolean> silent=new ThreadLocal<Boolean>();
    public static boolean notificationsMuted() { return Boolean.TRUE.equals(silent.get()); }
    public static void withNotifications(boolean enabled,Runnable action) {
        Boolean previous=silent.get(); silent.set(!enabled || Boolean.TRUE.equals(previous));
        try { action.run(); } finally { if(previous==null) silent.remove(); else silent.set(previous); }
    }
    public static void on(String type,Consumer<Event> handler) {
        if(!TYPES.contains(type) || handler==null) throw new IllegalArgumentException("Unknown quest lifecycle event: "+type);
        listeners.computeIfAbsent(type,k->new ArrayList<Consumer<Event>>()).add(handler);
    }
    public static void off(String type,Consumer<Event> handler) { List<Consumer<Event>> values=listeners.get(type); if(values!=null) values.remove(handler); }
    public static void clear() { listeners.clear(); }
    public static void fire(String type,UUID player,String quest,String task) {
        List<Consumer<Event>> handlers=listeners.get(type); if(handlers==null) return;
        Event event=new Event(type,player,quest,task);
        for(Consumer<Event> handler:new ArrayList<Consumer<Event>>(handlers)) try { handler.accept(event); }
        catch(RuntimeException ex) { cpw.mods.fml.common.FMLLog.warning("Quest event %s handler failed: %s",type,ex.toString()); }
    }
    private QuestEvents() {}
}
