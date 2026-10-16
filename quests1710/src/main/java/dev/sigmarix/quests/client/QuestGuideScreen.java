package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.net.URI;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Text guide pages; external links always use Minecraft's confirmation dialog. */
public final class QuestGuideScreen extends GuiScreen implements GuiYesNoCallback {
    private final GuiScreen parent; private final QuestCore.Quest quest;
    private int page,scroll;
    public QuestGuideScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; }
    @Override public void initGui() {
        buttonList.clear(); buttonList.add(new GuiButton(0,12,height-30,70,20,QuestText.tr("Back")));
        buttonList.add(new GuiButton(1,90,height-30,45,20,"<")); buttonList.add(new GuiButton(2,140,height-30,45,20,">"));
        if(!quest.uri.isEmpty()) buttonList.add(new GuiButton(3,width-130,height-30,118,20,QuestText.tr("Open link...")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==0) mc.displayGuiScreen(parent);
        if(b.id==1) { page=Math.max(0,page-1); scroll=0; }
        if(b.id==2) { page=Math.min(Math.max(0,quest.pages.size()-1),page+1); scroll=0; }
        if(b.id==3) mc.displayGuiScreen(new GuiConfirmOpenLink(this,quest.uri,4,false));
    }
    @Override public void confirmClicked(boolean yes,int id) {
        if(yes && id==4 && QuestPresentation.link(quest.uri)) try { java.awt.Desktop.getDesktop().browse(new URI(quest.uri)); } catch(Exception e) { cpw.mods.fml.common.FMLLog.warning("Could not open quest link: %s",e.toString()); }
        mc.displayGuiScreen(this);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj,quest.title+" - "+(page+1)+"/"+Math.max(1,quest.pages.size()),width/2,14,0xFFFFFF);
        List<String> lines=fontRendererObj.listFormattedStringToWidth(quest.pages.isEmpty()?quest.description:quest.pages.get(page),width-36);
        int rows=Math.max(1,(height-76)/12); scroll=Math.max(0,Math.min(scroll,Math.max(0,lines.size()-rows)));
        for(int i=scroll;i<lines.size() && i<scroll+rows;i++) fontRendererObj.drawString(lines.get(i),18,38+(i-scroll)*12,0xEEEEEE);
        super.drawScreen(x,y,partial);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); scroll+=wheel>0?-3:wheel<0?3:0; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
