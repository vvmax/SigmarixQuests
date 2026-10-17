package dev.sigmarix.quests;

import java.util.*;

/** Public world-screen progress, using the viewer's separately synchronized book definitions. */
public final class QuestWorldMap {
    public int dimension,x,y,z;
    public long revision;
    public String reference="",profile="",owner="";
    public Map<String,Integer> values=new LinkedHashMap<String,Integer>();
    public static QuestWorldMap create(QuestService service,UUID viewer,QuestDevice tile) {
        QuestWorldMap result=new QuestWorldMap(); result.x=tile.xCoord; result.y=tile.yCoord; result.z=tile.zCoord; result.dimension=tile.getWorldObj().provider.dimensionId; result.revision=service.revision; result.reference=tile.quest; result.profile=service.progress().profile(); result.owner=tile.owner==null?"":tile.owner.toString();
        if(tile.owner==null || !tile.bindingProfile.equals(service.progress().profile()) || !tile.quest.equals("book:") && !tile.quest.startsWith("chapter:")) return result;
        try { service.validateProgressReference(tile.quest); } catch(IllegalArgumentException removed) { return result; }
        String chapter=tile.quest.startsWith("chapter:")?tile.quest.substring(8):"";
        for(QuestCore.Quest quest:service.quests.values()) {
            if(!chapter.isEmpty() && !QuestPresentation.within(service.book,quest.chapter,chapter) || !service.visible(viewer,quest) || !service.visible(tile.owner,quest)) continue;
            boolean completed=service.progress().completed(tile.owner).contains(quest.id);
            int state=completed?(service.claimed(tile.owner,quest)?3:2):service.unlocked(tile.owner,quest)?1:0;
            int percent=(int)Math.round(100*service.objectProgress(tile.owner,quest.id));
            result.values.put(quest.id,state*101+percent);
        }
        return result;
    }
    public boolean matches(QuestDevice tile,long bookRevision,String activeProfile) {
        return revision==bookRevision && reference.equals(tile.quest) && profile.equals(activeProfile) && profile.equals(tile.bindingProfile) && owner.equals(tile.owner==null?"":tile.owner.toString());
    }
    public void validate() {
        if(profile==null || profile.length()>64 || owner==null || !owner.isEmpty() && !owner.matches("[0-9a-fA-F-]{36}") || reference==null || reference.length()>160 || values==null || values.size()>4096) throw new IllegalArgumentException("Invalid world quest map");
        for(Map.Entry<String,Integer> entry:values.entrySet()) if(!QuestCore.validId(entry.getKey()) || entry.getValue()==null || entry.getValue()<0 || entry.getValue()>403) throw new IllegalArgumentException("Invalid world quest map entry");
    }
}
