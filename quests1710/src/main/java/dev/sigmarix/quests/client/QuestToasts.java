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
    private static volatile boolean reset;
    public static void clear() { QUEUE.clear(); reset=true; }
    private QuestNetwork.Toast current; private long since;
    public static void enqueue(String json) { QUEUE.offer(json); }
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(reset) { current=null; reset=false; }
        if(mc.theWorld==null) { QUEUE.clear(); current=null; return; }
        long now=Minecraft.getSystemTime();
        if(current!=null && now-since>5000) current=null;
        if(current==null) {
            String json=QUEUE.poll(); if(json==null) return;
            try { current=QuestService.JSON.fromJson(json,QuestNetwork.Toast.class); if(current==null || current.title==null || current.text==null || current.title.length()>8192 || current.text.length()>8192 || current.iconMeta<0 || current.iconNbt==null || current.iconNbt.length()>65536) { current=null; return; } }
            catch(RuntimeException e) { return; } since=now;
        }
        int x=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight).getScaledWidth()-224,y=36;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        mc.entityRenderer.setupOverlayRendering(); GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(x,y,x+220,y+46,0xEF18212B); drawRect(x,y,x+3,y+46,0xFF60B978);
        Item item=(Item)Item.itemRegistry.getObject(current.icon==null?"minecraft:book":current.icon);
        if(item!=null) { ItemStack stack=new ItemStack(item,1,current.iconMeta); try { stack.setTagCompound(QuestService.tag(current.iconNbt)); } catch(RuntimeException invalid) {} RenderHelper.enableGUIStandardItemLighting(); new net.minecraft.client.renderer.entity.RenderItem().renderItemAndEffectIntoGUI(mc.fontRenderer,mc.getTextureManager(),stack,x+10,y+13); RenderHelper.disableStandardItemLighting(); }
        mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(current.title,174),x+36,y+10,0xFFFFCC66);
        mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(QuestText.tr(current.text),174),x+36,y+26,0xFFEEEEEE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopAttrib();
    }
}
