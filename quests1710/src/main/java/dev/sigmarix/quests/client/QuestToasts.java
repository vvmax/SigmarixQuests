package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.sigmarix.quests.*;
import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
/** Bounded main-render-thread notification queue. No GUI screen is opened by a toast. */
public final class QuestToasts extends Gui {
    private static final ArrayBlockingQueue<String> QUEUE=new ArrayBlockingQueue<String>(32);
    private static final java.util.ArrayDeque<QuestNetwork.Toast> pending=new java.util.ArrayDeque<QuestNetwork.Toast>(),history=new java.util.ArrayDeque<QuestNetwork.Toast>();
    private static volatile boolean reset;
    public static java.util.List<QuestNetwork.Toast> history() { return new java.util.ArrayList<QuestNetwork.Toast>(history); }
    public static void disconnect() { clear(); history.clear(); }
    public static void pump() {
        String json; while((json=QUEUE.poll())!=null) try {
            QuestNetwork.Toast toast=QuestService.JSON.fromJson(json,QuestNetwork.Toast.class);
            if(toast==null || toast.title==null || toast.text==null || toast.title.length()>8192 || toast.text.length()>8192 || toast.iconMeta<0 || toast.iconNbt==null || toast.iconNbt.length()>65536) continue;
            if(toast.task!=null && (toast.task.type==null || toast.task.title==null || toast.task.title.length()>256)) continue;
            if(history.size()>=64) history.removeLast(); history.addFirst(toast); if(pending.size()>=32) pending.removeFirst(); pending.addLast(toast);
        } catch(RuntimeException invalid) {}
    }
    public static void clear() { QUEUE.clear(); pending.clear(); reset=true; }
    private QuestNetwork.Toast current; private long since;
    public static void enqueue(String json) { QUEUE.offer(json); }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(reset) { current=null; reset=false; }
        if(mc.theWorld==null) { disconnect(); current=null; return; }
        long now=Minecraft.getSystemTime();
        if(current!=null && now-since>5000) current=null;
        if(current==null) { current=pending.pollFirst(); if(current==null) return; since=now; }
        int x=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight).getScaledWidth()-224,y=36;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        mc.entityRenderer.setupOverlayRendering(); GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(x,y,x+220,y+46,0xEF18212B); drawRect(x,y,x+3,y+46,0xFF60B978);
        Item item=(Item)Item.itemRegistry.getObject(current.icon==null?"minecraft:book":current.icon);
        if(item!=null) { ItemStack stack=new ItemStack(item,1,current.iconMeta); try { stack.setTagCompound(QuestService.tag(current.iconNbt)); } catch(RuntimeException invalid) {} RenderHelper.enableGUIStandardItemLighting(); new net.minecraft.client.renderer.entity.RenderItem().renderItemAndEffectIntoGUI(mc.fontRenderer,mc.getTextureManager(),stack,x+10,y+13); RenderHelper.disableStandardItemLighting(); }
        mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(current.task==null?current.title:QuestTaskLabels.title(current.task),174),x+36,y+10,0xFFFFCC66);
        mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(QuestText.tr(current.text),174),x+36,y+26,0xFFEEEEEE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopAttrib();
    }
}
