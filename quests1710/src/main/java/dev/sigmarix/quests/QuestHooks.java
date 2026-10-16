package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
/** Register handlers during mod initialization; handlers execute on the server thread. */
public final class QuestHooks {
    public interface Task { boolean complete(EntityPlayerMP player, QuestCore.Task definition); }
    public interface Reward { void grant(EntityPlayerMP player, QuestCore.Reward definition); }
    public static final Map<String, Task> TASKS = new HashMap<String, Task>();
    public static final Map<String, Reward> REWARDS = new HashMap<String, Reward>();
    private QuestHooks() {}
}
