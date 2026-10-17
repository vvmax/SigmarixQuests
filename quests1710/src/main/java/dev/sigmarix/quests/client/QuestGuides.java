package dev.sigmarix.quests.client;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
/** Native guide registry plus Forge events; an empty path addresses a guide index. */
public final class QuestGuides {
    private static final Map<String,Consumer<GuiScreen>> pages=new LinkedHashMap<String,Consumer<GuiScreen>>();
    public static void register(String path,Consumer<GuiScreen> handler) {
        if(path==null || path.length()>512 || handler==null || pages.containsKey(path)) throw new IllegalArgumentException("Invalid or duplicate guide path"); pages.put(path,handler);
    }
    public static void unregister(String path) { pages.remove(path); }
    public static boolean available(String path) {
        if(pages.containsKey(path)) return true;
        try { QuestGuideEvent.Check event=new QuestGuideEvent.Check(path); MinecraftForge.EVENT_BUS.post(event); return event.available; }
        catch(RuntimeException ex) { cpw.mods.fml.common.FMLLog.warning("Quest guide discovery failed: %s",ex.toString()); return false; }
    }
    public static boolean open(String path,GuiScreen parent) {
        try {
            Consumer<GuiScreen> handler=pages.get(path); if(handler!=null) { handler.accept(parent); return true; }
            QuestGuideEvent.Open event=new QuestGuideEvent.Open(path,parent); MinecraftForge.EVENT_BUS.post(event); if(event.opened) return true;
            Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(QuestText.tr("Guide is not available")+": "+path));
        } catch(RuntimeException ex) { Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("Guide: "+ex.getMessage())); }
        return false;
    }
    private QuestGuides() {}
}
