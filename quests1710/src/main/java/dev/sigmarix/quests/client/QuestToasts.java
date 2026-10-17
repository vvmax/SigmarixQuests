package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.*;import java.util.*;import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.*;import net.minecraft.util.*;import org.lwjgl.opengl.GL11;
/** The 1.12 toast sheet, timing, five-slot stack and bounded notification history. */
public final class QuestToasts extends Gui {
    private static final ArrayBlockingQueue<String> QUEUE=new ArrayBlockingQueue<String>(32);
    private static final ArrayDeque<QuestNetwork.Toast> pending=new ArrayDeque<QuestNetwork.Toast>(),history=new ArrayDeque<QuestNetwork.Toast>();
    private static volatile boolean reset;private static final class Active{QuestNetwork.Toast toast;long since;int slot;boolean outSound,importantSound;Active(QuestNetwork.Toast t,long now,int slot){toast=t;since=now;this.slot=slot;}}
    private final List<Active> active=new ArrayList<Active>();
    public static List<QuestNetwork.Toast> history(){return new ArrayList<QuestNetwork.Toast>(history);}
    public static void disconnect(){clear();history.clear();}
    public static void clear(){QUEUE.clear();pending.clear();reset=true;}
    public static void enqueue(String json){QUEUE.offer(json);}
    public static void pump(){String json;while((json=QUEUE.poll())!=null)try{
        QuestNetwork.Toast toast=QuestService.JSON.fromJson(json,QuestNetwork.Toast.class);if(toast==null || toast.title==null || toast.text==null || toast.title.length()>8192 || toast.text.length()>8192 || toast.iconMeta<0 || toast.iconNbt==null || toast.iconNbt.length()>65536)continue;
        if(toast.status){Minecraft.getMinecraft().ingameGUI.func_110326_a(StatCollector.translateToLocalFormatted(toast.title,Long.parseLong(toast.text)),false);continue;}
        if(toast.task!=null && (toast.task.type==null || toast.task.title==null || toast.task.title.length()>256))continue;
        if(history.size()>=64)history.removeLast();history.addFirst(toast);
        GuiScreen screen=Minecraft.getMinecraft().currentScreen;if(toast.reward!=null && screen instanceof QuestNotificationsScreen && ((QuestNotificationsScreen)screen).rewardReceived(toast))continue;
        if(pending.size()>=32)pending.removeFirst();pending.addLast(toast);
    }catch(RuntimeException invalid){}}
    @SubscribeEvent public void render(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();if(reset){active.clear();reset=false;}if(mc.theWorld==null){disconnect();active.clear();return;}if(mc.gameSettings.hideGUI)return;
        long now=Minecraft.getSystemTime();active.removeIf(a->now-a.since>=6200);
        while(active.size()<5 && !pending.isEmpty()){int slot=0;for(;slot<5;slot++){boolean used=false;for(Active a:active)if(a.slot==slot)used=true;if(!used)break;}active.add(new Active(pending.removeFirst(),now,slot));sound(mc,"in");}
        if(active.isEmpty())return;ScaledResolution res=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        try{mc.entityRenderer.setupOverlayRendering();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            for(Active a:active){long age=now-a.since,delta=Math.max(0,age-600);if(age>=5600 && !a.outSound){sound(mc,"out");a.outSound=true;}if(delta>0 && !a.importantSound){a.importantSound=true;if("chapter".equals(a.toast.completion))sound(mc,"challenge_complete");}double transition=age<600?age/600D:age>5600?1-(age-5600)/600D:1;transition=Math.max(0,Math.min(1,transition));if(age<600)transition*=transition;else if(age>5600)transition=1-Math.pow(1-transition,2);int x=(int)(res.getScaledWidth()-160*transition),y=a.slot*32;
                drawToast(mc,a.toast,x,y,delta);
            }
        }finally{GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();}
    }
    private static void sound(Minecraft mc,String event){mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.func_147674_a(new ResourceLocation("sigmarixquests:toast."+event),1F));}
    static void drawToast(Minecraft mc,QuestNetwork.Toast t,int x,int y,long delta){
                mc.getTextureManager().bindTexture(new ResourceLocation("sigmarixquests:textures/reference/toasts.png"));GL11.glColor4f(1,1,1,1);new Gui().drawTexturedModalRect(x,y,0,0,160,32);
                int titleColor="chapter".equals(t.completion)?0xFF88FF:0xFFFF00;String title=t.completion!=null && !t.completion.isEmpty()?StatCollector.translateToLocal("ftbquests."+t.completion+".completed"):t.reward!=null?StatCollector.translateToLocal("ftbquests.reward.collected"):t.task!=null?StatCollector.translateToLocal("ftbquests.task.completed"):t.text.equals("Quest complete")?StatCollector.translateToLocal("ftbquests.quest.completed"):t.title;
                String subtitle=t.quest!=null?QuestTaskLabels.quest(t.quest):t.completion!=null && !t.completion.isEmpty()?(t.completion.equals("file") && t.title.isEmpty()?"FTB Quests":t.title):t.reward!=null?QuestTaskLabels.reward(t.reward):t.task!=null?QuestTaskLabels.title(t.task):t.text.equals("Quest complete")?t.title:t.text;
                title=QuestText.tr(title);subtitle=QuestText.tr(subtitle);
                List<String> lines=mc.fontRenderer.listFormattedStringToWidth(subtitle,125);
                if(lines.size()==1){mc.fontRenderer.drawString(title,x+30,y+7,0xFF000000|titleColor);mc.fontRenderer.drawString(lines.get(0),x+30,y+18,0xFFFFFFFF);}
                else if(delta<1500){int alpha=((int)(Math.max(0,Math.min(1,(1500-delta)/300F))*255)<<24)|0x04000000;mc.fontRenderer.drawString(title,x+30,y+11,alpha|titleColor);}
                else{int alpha=((int)(Math.max(0,Math.min(1,(delta-1500)/300F))*252)<<24)|0x04000000;int ty=y+16-lines.size()*mc.fontRenderer.FONT_HEIGHT/2;for(String line:lines){mc.fontRenderer.drawString(line,x+30,ty,alpha|0xFFFFFF);ty+=mc.fontRenderer.FONT_HEIGHT;}}
                if(t.quest!=null)QuestClassicUi.quest(mc,t.quest,x+8,y+8,16);else if(t.reward!=null)QuestClassicUi.reward(mc,t.reward,x+8,y+8,16);else if(t.task!=null)QuestClassicUi.task(mc,t.task,true,x+8,y+8,16);else QuestClassicUi.item(mc,t.icon,t.iconMeta,t.iconNbt,x+8,y+8,16);
    }

}
