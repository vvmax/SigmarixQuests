package dev.sigmarix.quests;

import com.google.gson.*;
import java.util.*;

/** Per-viewer baselines contain only that viewer's already redacted snapshot. */
public final class QuestSnapshotSync {
    public static final class Frame {
        public boolean syncFrame=true;
        public long sequence,base;
        public JsonObject full;
        public QuestJsonDelta.Patch delta;
    }
    private static JsonObject normalize(QuestNetwork.Snapshot snapshot) {
        JsonObject result=QuestService.JSON.toJsonTree(snapshot).getAsJsonObject();
        QuestJsonDelta.normalizeBook(result.getAsJsonObject("book"));return result;
    }
    private static final class State { long sequence;JsonObject json;int characters; }
    public static final class Server {
        private final LinkedHashMap<UUID,State> viewers=new LinkedHashMap<UUID,State>(16,.75F,true);
        public void forget(UUID id) { viewers.remove(id); }
        public void clear() { viewers.clear(); }
        public String next(UUID id,QuestNetwork.Snapshot snapshot) {
            JsonObject next=normalize(snapshot);State previous=viewers.get(id);Frame frame=new Frame();
            frame.sequence=previous==null?1:previous.sequence+1;frame.full=next;
            String full=QuestService.JSON.toJson(frame),wire=full;
            if(previous!=null) {
                frame.full=null;frame.base=previous.sequence;frame.delta=QuestJsonDelta.diff(previous.json,next);
                String delta=QuestService.JSON.toJson(frame);if(frame.delta.changes.size()<=262144 && delta.length()<full.length())wire=delta;
            }
            State state=new State();state.sequence=frame.sequence;state.json=next;state.characters=full.length();viewers.put(id,state);
            long retained=0;for(State value:viewers.values())retained+=value.characters;
            Iterator<Map.Entry<UUID,State>> it=viewers.entrySet().iterator();
            while((retained>2L*QuestNetwork.MAX_BYTES || viewers.size()>16) && viewers.size()>1) { State old=it.next().getValue();retained-=old.characters;it.remove(); }
            return wire;
        }
    }
    public static final class Client {
        private JsonObject baseline;private long sequence;
        public void clear() { baseline=null;sequence=0; }
        public QuestNetwork.Snapshot receive(Frame frame) {
            if(frame==null || !frame.syncFrame || frame.sequence<=0)throw new IllegalArgumentException("Invalid quest sync frame");
            JsonObject next;
            if(frame.full!=null && frame.delta==null)next=frame.full;
            else {
                if(frame.full!=null || baseline==null || frame.base!=sequence || frame.sequence!=sequence+1)throw new IllegalArgumentException("Quest sync baseline unavailable");
                next=QuestJsonDelta.apply(baseline,frame.delta);
            }
            JsonObject restored=QuestJsonDelta.copy(next);QuestJsonDelta.restoreBook(restored.getAsJsonObject("book"));
            QuestNetwork.Snapshot snapshot=QuestService.JSON.fromJson(restored,QuestNetwork.Snapshot.class);
            if(snapshot.book==null)throw new IllegalArgumentException("Missing quest sync book");
            baseline=next;sequence=frame.sequence;return snapshot;
        }
        public QuestNetwork.Action save(QuestNetwork.Action action) {
            if(action.book==null || baseline==null || action.revision!=baseline.get("revision").getAsLong())return action;
            QuestJsonDelta.Patch patch=QuestJsonDelta.diff(baseline.getAsJsonObject("book"),QuestJsonDelta.book(action.book));
            if(patch.changes.size()>262144 || QuestService.JSON.toJson(patch).length()>=QuestService.JSON.toJson(action.book).length())return action;
            QuestNetwork.Action wire=QuestService.JSON.fromJson(QuestService.JSON.toJson(action),QuestNetwork.Action.class);
            wire.book=null;wire.bookDelta=patch;return wire;
        }
    }
}
