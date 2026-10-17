package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.lwjgl.opengl.GL11;
/** Personal server-persisted pins follow the active pack mode and synchronized percentages. */
public final class QuestPins extends Gui {
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event) {
        Minecraft mc=Minecraft.getMinecraft(); QuestNetwork.Snapshot snapshot=ClientProxy.snapshot;
        if(event.phase!=TickEvent.Phase.END || mc.theWorld==null || mc.currentScreen!=null || snapshot==null || snapshot.book==null || snapshot.pinned.isEmpty() || mc.gameSettings.hideGUI) return;
        ScaledResolution screen=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight); int x=screen.getScaledWidth()-194,y=90;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try {
            mc.entityRenderer.setupOverlayRendering(); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_LIGHTING);
            for(String id:snapshot.pinned) {
                QuestCore.Quest quest=null; for(QuestCore.Quest q:snapshot.book.quests) if(q.id.equals(id)) quest=q;
                if(quest==null) continue; int rows=Math.min(2,quest.tasks.size()),height=24+rows*10; if(y+height>screen.getScaledHeight()-8) break;
                int percent=Math.max(0,Math.min(100,snapshot.percentages.getOrDefault(id,0)));
                drawRect(x,y,x+188,y+height,0xC018212B); mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(quest.title,151),x+4,y+4,0xFFFFFF);
                mc.fontRenderer.drawString(percent+"%",x+157,y+4,0xFFFFCC66); drawRect(x+4,y+16,x+184,y+19,0xFF45505C); drawRect(x+4,y+16,x+4+180*percent/100,y+19,0xFF60B978);
                for(int i=0;i<rows;i++) { QuestCore.Task task=quest.tasks.get(i); String key=id+":"+task.id; boolean done=Boolean.TRUE.equals(snapshot.taskStates.get(key)); String label=(done?"[+] ":"[ ] ")+QuestTaskLabels.title(task)+" "+(done?task.count:snapshot.counters.getOrDefault(key,0L))+"/"+task.count; mc.fontRenderer.drawString(mc.fontRenderer.trimStringToWidth(label,178),x+5,y+23+i*10,done?0x91D6A1:0xDDDDDD); }
                y+=height+4;
            }
        } finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopAttrib(); }
    }
}
