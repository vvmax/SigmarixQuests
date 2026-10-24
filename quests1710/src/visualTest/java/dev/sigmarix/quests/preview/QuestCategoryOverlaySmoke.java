package dev.sigmarix.quests.preview;

import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.GL11;

/** Real framebuffer regression: covered nodes must not change a single popup pixel. */
public final class QuestCategoryOverlaySmoke {
    private static Field field(Class<?> type,String name)throws Exception { Field field=type.getDeclaredField(name);field.setAccessible(true);return field; }
    public static void setup(Minecraft mc,QuestNetwork.Snapshot snapshot)throws Exception {
        snapshot.book=new QuestCore.Book();snapshot.canEdit=true;snapshot.states.clear();snapshot.percentages.clear();
        String[] names={"Общие проекты и финальные испытания","Draconic Evolution","Avaritia и общие проекты","Электрические производственные испытания"};
        String[] icons={"minecraft:nether_star","minecraft:dragon_egg","minecraft:diamond_block","minecraft:enchanting_table"};
        for(int i=0;i<4;i++) { QuestCore.Chapter c=new QuestCore.Chapter();c.id="chapter_"+i;c.title=names[i];c.icon=icons[i];c.parent=i==0?"":"chapter_0";snapshot.book.chapters.add(c); }
        for(int i=0;i<8;i++) {
            QuestCore.Quest quest=new QuestCore.Quest();quest.id="node_"+i;quest.title="Covered quest "+i;quest.chapter="chapter_0";
            quest.icon=i%2==0?"minecraft:obsidian":"minecraft:enchanted_book";quest.x=2+i%4*2;quest.y=1+i/4;
            snapshot.book.quests.add(quest);snapshot.states.put(quest.id,"available");
        }
        snapshot.book.validate();mc.displayGuiScreen(new QuestBookScreen());
        field(QuestBookScreen.class,"panX").setDouble(mc.currentScreen,0);field(QuestBookScreen.class,"panY").setDouble(mc.currentScreen,0);
        ((Set<String>)field(QuestBookScreen.class,"selectedObjects").get(mc.currentScreen)).add("node_0");
    }
    private static BufferedImage capture(Minecraft mc,File destination,String name)throws Exception {
        mc.getFramebuffer().bindFramebuffer(true);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        mc.entityRenderer.setupOverlayRendering();mc.currentScreen.drawScreen(8,8,0);
        ScreenShotHelper.saveScreenshot(destination,name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
        return ImageIO.read(new File(new File(destination,"screenshots"),name));
    }
    public static void verify(Minecraft mc,QuestNetwork.Snapshot snapshot)throws Exception {
        File destination=new File(mc.mcDataDir,"../build/preview");destination.mkdirs();
        BufferedImage covered=capture(mc,destination,"category-overlay-covered.png");
        int popupWidth=0;
        for(Object hit:(List<?>)field(QuestBookScreen.class,"chapterHits").get(mc.currentScreen))
            if("chapter_0".equals(field(hit.getClass(),"id").get(hit)))popupWidth=Math.max(popupWidth,field(hit.getClass(),"w").getInt(hit));
        if(popupWidth<=20)throw new IllegalStateException("Category popup did not open");
        for(QuestCore.Quest quest:snapshot.book.quests)quest.x+=100;
        BufferedImage clean=capture(mc,destination,"category-overlay-clean.png");
        int scale=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight).getScaleFactor(),different=0,checked=0;
        for(int y=1;y<80*scale-1;y++)for(int x=20*scale+1;x<Math.min(popupWidth*scale-1,covered.getWidth());x++) {
            checked++;if(covered.getRGB(x,y)!=clean.getRGB(x,y))different++;
        }
        if(different!=0)throw new IllegalStateException("Map nodes leak into category popup: "+different+" pixels");
        for(QuestCore.Quest quest:snapshot.book.quests)quest.x-=100;
        capture(mc,destination,"category-overlay-fixed.png");
        Method click=QuestBookScreen.class.getDeclaredMethod("mouseClicked",int.class,int.class,int.class);click.setAccessible(true);click.invoke(mc.currentScreen,60,30,0);
        if(!"chapter_1".equals(field(QuestBookScreen.class,"chapter").get(mc.currentScreen)) || !"".equals(field(QuestBookScreen.class,"selected").get(mc.currentScreen)))
            throw new IllegalStateException("Category click reached the quest underneath");
        Files.write(new File(destination,"category-overlay-smoke.txt").toPath(),("PASS: "+checked+" popup pixels checked; 0 differ with overlapping quest items and selection; category click takes priority over underlying quest").getBytes(StandardCharsets.UTF_8));
    }
}
