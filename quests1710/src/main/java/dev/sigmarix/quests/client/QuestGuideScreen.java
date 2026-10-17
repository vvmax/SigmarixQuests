package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.net.URI;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
/** Resource-pack images, item illustrations and formatted text; links require confirmation. */
public final class QuestGuideScreen extends GuiScreen implements GuiYesNoCallback {
    private final GuiScreen parent; private final QuestCore.Quest quest;
    private int page,scroll;
    private String pendingUri="";
    private final Map<int[],String> links=new LinkedHashMap<int[],String>();
    private int pages() { return Math.max(1,quest.pages.size()+quest.richPages.size()+(quest.guidePage.isEmpty()?0:1)); }
    public QuestGuideScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; }
    @Override public void initGui() {
        buttonList.clear(); buttonList.add(new GuiButton(0,12,height-30,70,20,QuestText.tr("Back")));
        buttonList.add(new GuiButton(1,90,height-30,45,20,"<")); buttonList.add(new GuiButton(2,140,height-30,45,20,">"));
        if(!quest.guidePage.isEmpty()) buttonList.add(new GuiButton(5,width/2-80,height/2,160,20,QuestText.tr("Open in guide")));
        if(!quest.uri.isEmpty()) buttonList.add(new GuiButton(3,width-130,height-30,118,20,QuestText.tr("Open link...")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==0) mc.displayGuiScreen(parent);
        if(b.id==1) { page=Math.max(0,page-1); scroll=0; }
        if(b.id==2) { page=Math.min(pages()-1,page+1); scroll=0; }
        if(b.id==3) open(quest.uri);
        if(b.id==5) QuestGuides.open(quest.guidePage,this);
    }
    private void open(String uri) { if(QuestPresentation.link(uri) && !uri.isEmpty()) { pendingUri=uri; mc.displayGuiScreen(new GuiConfirmOpenLink(this,uri,4,false)); } }
    @Override public void confirmClicked(boolean yes,int id) {
        if(yes && id==4 && QuestPresentation.link(pendingUri)) try { java.awt.Desktop.getDesktop().browse(new URI(pendingUri)); } catch(Exception e) { cpw.mods.fml.common.FMLLog.warning("Could not open quest link: %s",e.toString()); }
        mc.displayGuiScreen(this);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj,quest.title+" - "+(page+1)+"/"+pages(),width/2,14,0xFFFFFF);
        links.clear();
        boolean external=!quest.guidePage.isEmpty() && page==pages()-1;
        for(Object raw:buttonList) { GuiButton button=(GuiButton)raw; if(button.id==5) { button.visible=external; button.enabled=external && QuestGuides.available(quest.guidePage); } }
        if(external) { drawCenteredString(fontRendererObj,fontRendererObj.trimStringToWidth(quest.guidePage,width-36),width/2,50,0xEEEEEE); super.drawScreen(x,y,partial); return; }
        if(page>=quest.pages.size() && !quest.richPages.isEmpty()) { rich(quest.richPages.get(page-quest.pages.size()),x,y); super.drawScreen(x,y,partial); return; }
        List<String> lines=fontRendererObj.listFormattedStringToWidth(quest.pages.isEmpty()?quest.description:quest.pages.get(page),width-36);
        int rows=Math.max(1,(height-76)/12); scroll=Math.max(0,Math.min(scroll,Math.max(0,lines.size()-rows)));
        for(int i=scroll;i<lines.size() && i<scroll+rows;i++) fontRendererObj.drawString(lines.get(i),18,38+(i-scroll)*12,0xEEEEEE);
        super.drawScreen(x,y,partial);
    }
    private void rich(QuestPresentation.GuidePage page,int mouseX,int mouseY) {
        int available=width-36,total=page.title.isEmpty()?0:24;
        for(QuestPresentation.GuideElement element:page.elements) total+=elementHeight(element,available);
        scroll=Math.max(0,Math.min(scroll,Math.max(0,(total-(height-80)+11)/12)));
        int y=38-scroll*12;
        ScaledResolution resolution=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight); int scale=resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(12*scale,42*scale,(width-24)*scale,Math.max(0,height-76)*scale);
        if(!page.title.isEmpty()) { fontRendererObj.drawString(page.title,18,y,0xFFFFFF); y+=24; }
        for(QuestPresentation.GuideElement element:page.elements) {
            int size=elementHeight(element,available);
            if(y+size>=34 && y<height-42) {
                int color=QuestPresentation.color(element.color);
                switch(element.type) {
                    case "text": fontRendererObj.drawSplitString(element.text,18,y,available,color); break;
                    case "image":
                        mc.getTextureManager().bindTexture(QuestImages.texture(element.resource)); GL11.glColor4f(1,1,1,1);
                        int w=Math.min(available,element.width),h=Math.max(1,element.height*w/element.width);
                        func_152125_a(18,y,0,0,256,256,w,h,256,256); break;
                    case "item":
                        net.minecraft.item.Item item=(net.minecraft.item.Item)net.minecraft.item.Item.itemRegistry.getObject(element.item);
                        if(item!=null) {
                            net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(item,element.count,element.meta); stack.setTagCompound(QuestService.tag(element.nbt));
                            GL11.glPushMatrix(); GL11.glTranslatef(18,y,0); GL11.glScalef(2,2,1); net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                            itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),stack,0,0); net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting(); GL11.glPopMatrix();
                            fontRendererObj.drawString(element.text.isEmpty()?stack.getDisplayName():element.text,58,y+10,color);
                        }
                        break;
                    case "link":
                        String caption=fontRendererObj.trimStringToWidth(element.text.isEmpty()?element.uri:element.text,available-12);
                        int right=Math.min(width-18,18+fontRendererObj.getStringWidth(caption)+12);
                        drawRect(18,y,right,y+20,0xFF284E6B);
                        fontRendererObj.drawString(caption,24,y+6,color);
                        links.put(new int[]{18,y,right,y+20},element.uri); break;
                }
            }
            y+=size;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glColor4f(1,1,1,1);
    }
    private int elementHeight(QuestPresentation.GuideElement element,int available) {
        if(element.type.equals("text")) return fontRendererObj.listFormattedStringToWidth(element.text,available).size()*fontRendererObj.FONT_HEIGHT+10;
        if(element.type.equals("image")) return Math.max(1,element.height*Math.min(available,element.width)/element.width)+10;
        return element.type.equals("item")?42:28;
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        super.mouseClicked(x,y,button);
        if(button==0 && y>=34 && y<height-42) for(Map.Entry<int[],String> entry:links.entrySet()) { int[] bounds=entry.getKey(); if(x>=bounds[0] && x<bounds[2] && y>=bounds[1] && y<bounds[3]) { open(entry.getValue()); return; } }
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); scroll+=wheel>0?-3:wheel<0?3:0; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
