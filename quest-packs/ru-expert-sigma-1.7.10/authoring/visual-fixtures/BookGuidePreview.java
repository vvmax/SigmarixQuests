package dev.sigmarix.quests;
import cpw.mods.fml.common.*;import cpw.mods.fml.common.event.*;import cpw.mods.fml.common.eventhandler.SubscribeEvent;import cpw.mods.fml.common.gameevent.TickEvent;
import dev.sigmarix.quests.client.*;import net.minecraft.client.*;import net.minecraft.client.gui.*;import net.minecraft.util.*;import java.io.*;import java.lang.reflect.*;
@Mod(modid="sigmabookpreview",name="Revised campaign visual QA fixture",version="1",dependencies="required-after:sigmarixquests")
public class BookGuidePreview {
 int ticks,stage;String[] names={"revised-ic2-map","revised-ae-map","revised-blood-guide","revised-reactor-guide"};QuestCore.Book book;QuestNetwork.Snapshot fixture;
 @Mod.EventHandler public void init(FMLInitializationEvent e){FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST) public void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END)return;Minecraft m=Minecraft.getMinecraft();ticks++;
  if(fixture!=null)ClientProxy.snapshot=fixture;
  if(ticks==60){book=QuestBookFiles.read(new File(System.getProperty("guide.book"))).book;book.validate();fixture=new QuestNetwork.Snapshot();fixture.book=book;ClientProxy.snapshot=fixture;for(QuestCore.Quest q:book.quests)fixture.states.put(q.id,"available");open(m);}
  if(ticks==120){
   m.getFramebuffer().bindFramebuffer(true);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);org.lwjgl.opengl.GL11.glClearColor(0,0,0,1);org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);m.entityRenderer.setupOverlayRendering();m.currentScreen.drawScreen(-1,-1,0);
   File out=new File(m.mcDataDir,"captures");out.mkdirs();ScreenShotHelper.saveScreenshot(out,names[stage]+".png",m.displayWidth,m.displayHeight,m.getFramebuffer());System.out.println("BOOK_GUIDE_CAPTURED "+names[stage]);
   stage++;if(stage==names.length){m.shutdown();ticks=200;return;}ticks=61;open(m);
  }
 }
 void open(Minecraft m)throws Exception{
  m.gameSettings.guiScale=2;
  if(stage<2){QuestBookScreen s=new QuestBookScreen();m.displayGuiScreen(s);Field c=QuestBookScreen.class.getDeclaredField("chapter");c.setAccessible(true);c.set(s,stage==0?"ic2":"ae2");Field z=QuestBookScreen.class.getDeclaredField("zoom");z.setAccessible(true);z.setDouble(s,0.62);Method center=QuestBookScreen.class.getDeclaredMethod("centerMap");center.setAccessible(true);center.invoke(s);s.refresh();}
  else {String id=stage==2?"bm_runes":"ic2_reactor";QuestCore.Quest q=null;for(QuestCore.Quest a:book.quests)if(a.id.equals(id))q=a;QuestGuideScreen s=new QuestGuideScreen(null,q);m.displayGuiScreen(s);Field page=QuestGuideScreen.class.getDeclaredField("page");page.setAccessible(true);page.setInt(s,q.pages.size());for(QuestPresentation.GuidePage p:q.richPages)for(QuestPresentation.GuideElement a:p.elements)if(a.type.equals("image"))try(InputStream in=m.getResourceManager().getResource(new ResourceLocation(a.resource)).getInputStream()){if(in.read()<0)throw new IllegalStateException("Empty image");}}
 }
}
