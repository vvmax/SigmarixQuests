package dev.sigmarix.quests.client;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.*;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.*;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.*;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    private final QuestMessageQueue<String> queue=new QuestMessageQueue<String>(16,2L*QuestNetwork.MAX_BYTES,String::length);
    private final KeyBinding key=new KeyBinding("key.sigmarixquests.open",Keyboard.KEY_GRAVE,"key.categories.misc");
    private boolean opening;
    private int screenRenderType;
    @Override public int screenRenderType() { return screenRenderType; }
    private int syncTicks,pinTicks;
    private long neiRevision=Long.MIN_VALUE;
    public static QuestNetwork.Snapshot snapshot;
    @Override public void init() {
        QuestBotaniaGuide.register();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new QuestGuiSprites());
        ((net.minecraft.client.resources.IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new QuestIconAtlas());
        screenRenderType=cpw.mods.fml.client.registry.RenderingRegistry.getNextAvailableRenderId(); cpw.mods.fml.client.registry.RenderingRegistry.registerBlockHandler(new QuestScreenBlockRenderer(screenRenderType));
        if(cpw.mods.fml.common.Loader.isModLoaded("NotEnoughItems")) try { Class.forName("dev.sigmarix.quests.client.QuestNEI").getMethod("register").invoke(null); }
        catch(ReflectiveOperationException ex) { throw new IllegalStateException("NEI integration registration failed",ex); }
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","loot_crate"),new QuestModelItemRenderer(-1));
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(QuestBlocks.crateParcel,new QuestModelItemRenderer(-1));
        for(int i=0;i<QuestBlocks.devices.length;i++) net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(net.minecraft.item.Item.getItemFromBlock(QuestBlocks.devices[i]),new QuestModelItemRenderer(i));
        ClientRegistry.bindTileEntitySpecialRenderer(QuestDevice.class,new QuestScreenRenderer());
        FMLCommonHandler.instance().bus().register(new QuestToasts());
        FMLCommonHandler.instance().bus().register(new QuestPins());
        ClientRegistry.registerKeyBinding(key); FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override public String getCommandName() { return "questbook"; }
            @Override public String getCommandUsage(ICommandSender s) { return "/questbook"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public void processCommand(ICommandSender s,String[] args) { open(); }
        });
    }
    @Override public Object deviceScreen(net.minecraft.entity.player.EntityPlayer player,QuestDevice tile) { return tile instanceof QuestStorage?new QuestStorageScreen(player,(QuestStorage)tile):new QuestDeviceScreen(player,tile); }
    public static void send(QuestNetwork.Action action) { if(!action.action.equals("save") && snapshot!=null) action.revision=snapshot.revision; QuestNetwork.sendRequest(QuestService.JSON.toJson(action)); }
    @Override public void openIcon() { opening=false; Minecraft.getMinecraft().displayGuiScreen(new QuestIconScreen()); }
    @Override public void open() { if(Minecraft.getMinecraft().thePlayer==null) return; opening=true; send(new QuestNetwork.Action()); }
    @Override public void receive(String json) { queue.offer(json); }
    @Override public void notice(String json) { QuestToasts.enqueue(json); }
    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) { if(key.isPressed()) open(); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.theWorld==null) { QuestWorldMaps.clear(); QuestImages.clear(); QuestToasts.disconnect(); queue.clear(); snapshot=null; QuestLoot.clientBook=null; neiRevision=Long.MIN_VALUE; syncTicks=0; pinTicks=0; opening=false; return; }
        if(snapshot==null && mc.thePlayer!=null && ++syncTicks%20==0) send(new QuestNetwork.Action());
        QuestToasts.pump();
        if(snapshot!=null && (!snapshot.pinned.isEmpty() || snapshot.autoPin) && mc.currentScreen==null && ++pinTicks%40==0) send(new QuestNetwork.Action());
        String json;
        while((json=queue.poll())!=null) {
            try {
                QuestNetwork.Snapshot received=QuestService.JSON.fromJson(json,QuestNetwork.Snapshot.class);
                if(received==null) throw new IllegalArgumentException("Empty server response");
                if(received.screen!=null) { QuestWorldMaps.receive(received.screen); continue; }
                if(received.book==null) {
                    if(snapshot!=null) snapshot.message=received.message;
                    if(mc.currentScreen instanceof QuestEditorScreen) ((QuestEditorScreen)mc.currentScreen).saved(received);
                    if(mc.currentScreen instanceof QuestObjectScreen) ((QuestObjectScreen)mc.currentScreen).saved(received);
                    continue;
                }
                snapshot=received; QuestLoot.clientBook=received.book;
                if(neiRevision!=received.revision && cpw.mods.fml.common.Loader.isModLoaded("NotEnoughItems")) {
                    try { Class.forName("dev.sigmarix.quests.client.QuestNEI").getMethod("refresh").invoke(null); neiRevision=received.revision; }
                    catch(ReflectiveOperationException ex) { throw new IllegalStateException("NEI quest variants refresh failed",ex); }
                }
                if(opening) { opening=false; if(received.book.disableGui && !received.canEdit) mc.thePlayer.addChatMessage(dev.sigmarix.quests.QuestMessages.component("Quest GUI is disabled by this book")); else mc.displayGuiScreen(new QuestBookScreen()); }
                else if(mc.currentScreen instanceof QuestBookScreen) ((QuestBookScreen)mc.currentScreen).refresh();
                else if(mc.currentScreen instanceof QuestEditorScreen) ((QuestEditorScreen)mc.currentScreen).saved(snapshot);
                else if(mc.currentScreen instanceof QuestObjectScreen) ((QuestObjectScreen)mc.currentScreen).saved(snapshot);
            } catch(RuntimeException ex) { mc.thePlayer.addChatMessage(dev.sigmarix.quests.QuestMessages.component("Quest sync failed: "+ex.getMessage())); }
        }
    }
}
