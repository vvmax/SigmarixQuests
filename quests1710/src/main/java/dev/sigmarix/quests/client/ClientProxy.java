package dev.sigmarix.quests.client;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.*;
import java.util.concurrent.ArrayBlockingQueue;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.*;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    private final ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<String>(16);
    private final KeyBinding key=new KeyBinding("key.sigmarixquests.open",Keyboard.KEY_GRAVE,"key.categories.misc");
    private boolean opening;
    public static QuestNetwork.Snapshot snapshot;
    @Override public void init() {
        ClientRegistry.registerKeyBinding(key); FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override public String getCommandName() { return "questbook"; }
            @Override public String getCommandUsage(ICommandSender s) { return "/questbook"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public void processCommand(ICommandSender s,String[] args) { open(); }
        });
    }
    public static void send(QuestNetwork.Action action) { if(!action.action.equals("save") && snapshot!=null) action.revision=snapshot.revision; QuestNetwork.channel.sendToServer(new QuestNetwork.Request(QuestService.JSON.toJson(action))); }
    @Override public void open() { if(Minecraft.getMinecraft().thePlayer==null) return; opening=true; send(new QuestNetwork.Action()); }
    @Override public void receive(String json) { queue.offer(json); }
    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) { if(key.isPressed()) open(); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.theWorld==null) { queue.clear(); snapshot=null; opening=false; return; }
        String json;
        while((json=queue.poll())!=null) {
            try {
                QuestNetwork.Snapshot received=QuestService.JSON.fromJson(json,QuestNetwork.Snapshot.class);
                if(received==null) throw new IllegalArgumentException("Empty server response");
                if(received.book==null) {
                    if(snapshot!=null) snapshot.message=received.message;
                    if(mc.currentScreen instanceof QuestEditorScreen) ((QuestEditorScreen)mc.currentScreen).saved(received);
                    continue;
                }
                snapshot=received;
                if(opening) { opening=false; mc.displayGuiScreen(new QuestBookScreen()); }
                else if(mc.currentScreen instanceof QuestBookScreen) ((QuestBookScreen)mc.currentScreen).refresh();
                else if(mc.currentScreen instanceof QuestEditorScreen) ((QuestEditorScreen)mc.currentScreen).saved(snapshot);
            } catch(RuntimeException ex) { mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("Quest sync failed: "+ex.getMessage())); }
        }
    }
}
