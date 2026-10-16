package dev.sigmarix.quests;

import com.google.gson.GsonBuilder;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.command.*;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

@Mod(modid = "sigmarixquests", name = "Sigmarix Quests", version = "0.1.0", acceptableRemoteVersions = "*")
public final class QuestsMod {
    private File config;
    private Map<String, QuestCore.Quest> quests = Collections.emptyMap();
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) throws IOException {
        config = new File(event.getModConfigurationDirectory(), "sigmarixquests/quests.json");
        Files.createDirectories(config.toPath().getParent());
        if (!config.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/example-quests.json")) {
                Files.copy(in, config.toPath());
            }
        }
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void load() throws IOException {
        QuestCore.Book book;
        try (Reader reader = new InputStreamReader(new FileInputStream(config), StandardCharsets.UTF_8)) {
            book = new GsonBuilder().create().fromJson(reader, QuestCore.Book.class);
        }
        if (book == null) throw new IllegalArgumentException("Empty quest book");
        Map<String, QuestCore.Quest> next = book.validate();
        for (QuestCore.Quest q : next.values()) {
            for (QuestCore.Task t : q.tasks) {
                if ("item".equals(t.type)) item(t.target);
                if ("kill".equals(t.type) && !EntityList.stringToClassMapping.containsKey(t.target))
                    throw new IllegalArgumentException("Unknown entity: " + t.target);
            }
            for (QuestCore.Reward r : q.rewards) item(r.item);
        }
        quests = next;
    }
    private static Item item(String name) {
        Item item = (Item) Item.itemRegistry.getObject(name);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + name);
        return item;
    }
    @Mod.EventHandler public void starting(FMLServerStartingEvent event) throws IOException {
        load();
        event.registerServerCommand(new QuestCommand());
    }
    private QuestProgress progress() {
        WorldServer world = MinecraftServer.getServer().worldServerForDimension(0);
        QuestProgress data = (QuestProgress) world.mapStorage.loadData(QuestProgress.class, "sigmarixquests");
        if (data == null) { data = new QuestProgress(); world.mapStorage.setData("sigmarixquests", data); }
        return data;
    }
    @SubscribeEvent public void onDeath(LivingDeathEvent event) {
        if (event.entity.worldObj.isRemote || !(event.source.getEntity() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.source.getEntity();
        String entity = EntityList.getEntityString(event.entityLiving);
        QuestProgress data = progress();
        Set<String> done = data.completed(player.getUniqueID());
        for (QuestCore.Quest q : quests.values()) if (!done.contains(q.id) && QuestCore.unlocked(q, done)) {
            for (int i = 0; i < q.tasks.size(); i++) {
                QuestCore.Task t = q.tasks.get(i);
                if ("kill".equals(t.type) && t.target.equals(entity)) data.kill(player.getUniqueID(), q.id, i, t.count);
            }
        }
    }
    private static void say(ICommandSender sender, String message) { sender.addChatMessage(new ChatComponentText(message)); }
    private boolean ready(EntityPlayerMP player, QuestCore.Quest q, QuestProgress data) {
        Map<String, Long> required = QuestCore.requiredItems(q);
        for (Map.Entry<String, Long> requirement : required.entrySet()) {
            String key = requirement.getKey(); int at = key.lastIndexOf('@');
            Item target = item(key.substring(0, at)); int meta = Integer.parseInt(key.substring(at + 1));
            long count = 0;
            for (ItemStack stack : player.inventory.mainInventory)
                if (stack != null && stack.getItem() == target && stack.getItemDamage() == meta) count += stack.stackSize;
            if (count < requirement.getValue()) return false;
        }
        for (int i = 0; i < q.tasks.size(); i++) {
            QuestCore.Task t = q.tasks.get(i);
            if ("dimension".equals(t.type) && player.dimension != t.dimension) return false;
            if ("kill".equals(t.type) && data.kills(player.getUniqueID(), q.id, i) < t.count) return false;
        }
        return true;
    }
    private final class QuestCommand extends CommandBase {
        @Override public String getCommandName() { return "quests"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/quests [list|show <id>|check <id>|claim <id>|reload]"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public void processCommand(ICommandSender sender, String[] args) {
            if (args.length > 0 && args[0].equals("reload")) {
                if (!sender.canCommandSenderUseCommand(2, "quests.reload")) throw new CommandException("commands.generic.permission");
                try { load(); say(sender, "Quest book reloaded."); }
                catch (Exception e) { say(sender, "Reload failed; previous book kept: " + e.getMessage()); }
                return;
            }
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            QuestProgress data = progress(); UUID uuid = player.getUniqueID(); Set<String> done = data.completed(uuid);
            if (args.length == 0 || (args.length == 1 && args[0].equals("list"))) {
                for (QuestCore.Quest q : quests.values()) say(sender, q.id + " - " + q.title + " [" +
                    (done.contains(q.id) ? (data.claimed(uuid, q.id) ? "claimed" : "complete") : QuestCore.unlocked(q, done) ? "available" : "locked") + "]");
                say(sender, getCommandUsage(sender)); return;
            }
            if (args.length != 2) throw new WrongUsageException(getCommandUsage(sender));
            QuestCore.Quest q = quests.get(args[1]);
            if (q == null) throw new CommandException("Unknown quest: " + args[1]);
            if (args[0].equals("show")) {
                say(sender, q.title + ": " + q.description);
                say(sender, "Requires completed: " + q.dependencies);
                for (int i = 0; i < q.tasks.size(); i++) {
                    QuestCore.Task t = q.tasks.get(i);
                    say(sender, t.type + " " + (t.target == null ? "" : t.target) + " x" + t.count +
                        (t.type.equals("dimension") ? " dimension=" + t.dimension : "") +
                        (t.type.equals("kill") ? " progress=" + data.kills(uuid, q.id, i) : ""));
                }
                for (QuestCore.Reward r : q.rewards) say(sender, "Reward: " + r.item + " x" + r.count);
            } else if (args[0].equals("check")) {
                if (done.contains(q.id)) { say(sender, "Already complete."); return; }
                if (!QuestCore.unlocked(q, done)) { say(sender, "Dependencies incomplete."); return; }
                if (!ready(player, q, data)) { say(sender, "Tasks incomplete."); return; }
                data.complete(uuid, q.id); say(sender, "Quest complete: " + q.title + ". Use /quests claim " + q.id);
            } else if (args[0].equals("claim")) {
                if (!done.contains(q.id) || data.claimed(uuid, q.id)) { say(sender, "Reward unavailable."); return; }
                // All reward items validated before changing persistent state.
                List<ItemStack> rewards = new ArrayList<ItemStack>();
                for (QuestCore.Reward r : q.rewards) {
                    Item reward = item(r.item); int remaining = r.count;
                    while (remaining > 0) { int size = Math.min(remaining, reward.getItemStackLimit()); rewards.add(new ItemStack(reward, size, r.meta)); remaining -= size; }
                }
                data.claim(uuid, q.id);
                for (ItemStack reward : rewards) if (!player.inventory.addItemStackToInventory(reward)) player.dropPlayerItemWithRandomChoice(reward, false);
                player.inventoryContainer.detectAndSendChanges(); say(sender, "Rewards claimed.");
            } else throw new WrongUsageException(getCommandUsage(sender));
        }
    }
}
