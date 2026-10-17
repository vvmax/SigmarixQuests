package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.Minecraft;

/** Bounded main-thread cache; map reads never consume the mutation rate limit. */
public final class QuestWorldMaps {
    private static final class Entry { QuestWorldMap value; long requested; }
    private static final LinkedHashMap<String,Entry> entries=new LinkedHashMap<String,Entry>(64,.75f,true);
    private static long nextRequest;
    private static String key(int dimension,int x,int y,int z) { return dimension+":"+x+":"+y+":"+z; }
    public static void clear() { entries.clear(); nextRequest=0; }
    public static void receive(QuestWorldMap value) {
        value.validate(); Minecraft mc=Minecraft.getMinecraft();
        if(mc.theWorld==null || mc.theWorld.provider.dimensionId!=value.dimension) return;
        String key=key(value.dimension,value.x,value.y,value.z); Entry entry=entries.get(key);
        if(entry==null) { entry=new Entry(); entries.put(key,entry); }
        entry.value=value; entry.requested=mc.theWorld.getTotalWorldTime(); trim();
        if(ClientProxy.snapshot!=null && (ClientProxy.snapshot.revision!=value.revision || !ClientProxy.snapshot.profile.equals(value.profile))) ClientProxy.send(new QuestNetwork.Action());
    }
    public static QuestWorldMap get(QuestDevice tile) {
        Minecraft mc=Minecraft.getMinecraft(); if(mc.theWorld==null || ClientProxy.snapshot==null) return null;
        String key=key(mc.theWorld.provider.dimensionId,tile.xCoord,tile.yCoord,tile.zCoord); Entry entry=entries.get(key); boolean first=entry==null;
        if(first) { entry=new Entry(); entries.put(key,entry); trim(); }
        long time=mc.theWorld.getTotalWorldTime();
        boolean matches=entry.value!=null && entry.value.matches(tile,ClientProxy.snapshot.revision,ClientProxy.snapshot.profile);
        if(time>=nextRequest && (first || time-entry.requested>=40 || !matches && time-entry.requested>=4)) {
            QuestNetwork.Action action=new QuestNetwork.Action(); action.action="screen_view"; action.x=tile.xCoord; action.y=tile.yCoord; action.z=tile.zCoord; ClientProxy.send(action);
            entry.requested=time; nextRequest=time+5;
        }
        return matches?entry.value:null;
    }
    private static void trim() { while(entries.size()>64) entries.remove(entries.keySet().iterator().next()); }
    private QuestWorldMaps() {}
}
