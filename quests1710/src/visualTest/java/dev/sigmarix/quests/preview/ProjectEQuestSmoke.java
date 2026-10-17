package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import net.minecraft.entity.player.EntityPlayerMP;
/** Uses the installed ProjectE wallet and native quest preflight/submit paths. */
public final class ProjectEQuestSmoke {
    private static EntityPlayerMP player;
    private static int ticks;
    public static void start(EntityPlayerMP owner) {
        player=owner; ticks=0; QuestProjectE.set(owner,100);
        QuestCore.Quest q=QuestsMod.service.quests.get("smoke-emc");
        if(QuestsMod.service.check(owner,q,true)) throw new IllegalStateException("Incomplete EMC quest completed");
        if(QuestProjectE.balance(owner)!=100) throw new IllegalStateException("Failed preflight consumed EMC");
    }
    public static boolean tick() {
        if(player==null) return false; ticks++;
        if(QuestsMod.service.progress().value(player.getUniqueID(),"smoke-emc","wallet")>=64) {
            if(QuestProjectE.balance(player)!=36) throw new IllegalStateException("EMC submission did not debit the wallet exactly");
            QuestCore.Quest q=QuestsMod.service.quests.get("smoke-emc"); q.tasks.get(1).dimension=0;
            if(!QuestsMod.service.check(player,q,true) || QuestProjectE.balance(player)!=36) throw new IllegalStateException("EMC submission was charged again on completion");
            System.out.println("Quest ProjectE PASS: failed preflight preserved 100 EMC; GUI submission spent 64; wallet retained 36"); player=null; return true;
        }
        if(ticks>400) throw new IllegalStateException("ProjectE submission did not arrive through the quest protocol");
        return false;
    }
}
