package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;import java.util.*;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.*;import org.lwjgl.opengl.GL11;
/** Original right-center text HUD, including automatic tracking and the theme scale. */
public final class QuestPins extends Gui {
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event){
        Minecraft mc=Minecraft.getMinecraft();QuestNetwork.Snapshot snap=ClientProxy.snapshot;if(event.phase!=TickEvent.Phase.END || mc.theWorld==null || mc.currentScreen!=null || snap==null || mc.gameSettings.hideGUI || snap.pinned.isEmpty() && !snap.autoPin)return;
        List<String> lines=new ArrayList<String>();for(QuestCore.Quest quest:snap.book.quests){String state=snap.states.get(quest.id);boolean complete="complete".equals(state) || "claimed".equals(state);if(snap.autoPin?!"available".equals(state):!snap.pinned.contains(quest.id))continue;
            if(!lines.isEmpty())lines.add("");lines.add("\u00a7l"+(complete?"\u00a7a":"")+mc.fontRenderer.trimStringToWidth(QuestTaskLabels.quest(quest),160)+(complete?"\u00a72 100%":" \u00a73"+snap.percentages.getOrDefault(quest.id,0)+"%"));
            if(!complete)for(QuestCore.Task task:quest.tasks)if(!Boolean.TRUE.equals(snap.taskStates.get(quest.id+":"+task.id)))lines.add("\u00a77"+mc.fontRenderer.trimStringToWidth(QuestTaskLabels.title(task),160)+" \u00a7a"+snap.counters.getOrDefault(quest.id+":"+task.id,0L)+"/"+task.count);
        }if(lines.isEmpty())return;int mw=0;for(String line:lines)mw=Math.max(mw,mc.fontRenderer.getStringWidth(line));ScaledResolution res=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);double scale=snap.book.theme.pinnedQuestSize;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        try{mc.entityRenderer.setupOverlayRendering();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glTranslated(res.getScaledWidth()-mw*scale-8,res.getScaledHeight()/2-lines.size()*4.5*scale,100);GL11.glScaled(scale,scale,1);drawRect(0,0,mw+8,lines.size()*9+8,0x64000000);for(int i=0;i<lines.size();i++)mc.fontRenderer.drawStringWithShadow(lines.get(i),4,i*9+4,0xFFFFFFFF);}finally{GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();}
    }
}
