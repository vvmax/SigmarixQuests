package dev.sigmarix.quests;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayerMP;
/** Native custom rewards notify server integrations even when no named adapter is configured. */
public final class QuestCustomRewardEvent extends Event {
    private final QuestCore.Reward reward;private final EntityPlayerMP player;private final boolean notify;
    public QuestCustomRewardEvent(QuestCore.Reward reward,EntityPlayerMP player,boolean notify){this.reward=reward;this.player=player;this.notify=notify;}
    public QuestCore.Reward getReward(){return reward;}public EntityPlayerMP getPlayer(){return player;}public boolean shouldNotify(){return notify;}
}
