package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;

/** Fullscreen chapter tree and a centered, independently scrolling quest card. */
public final class QuestBookScreen extends GuiScreen implements GuiYesNoCallback {
    private static final QuestPresentation.Theme DEFAULT_THEME=new QuestPresentation.Theme();
    private GuiTextField search;
    private String selected="",chapter="",group="",pendingLink="",hoverChapter="",movingId="";
    private double panX,panY,zoom=1;
    private int lastX,lastY,detailsScroll,chapterIndex=-1,groupIndex=-1,refreshTicks,chapterScroll,hoverY,viewportWidth,viewportHeight;
    private boolean positioned,dragging,moving,showSearch,menu;
    private QuestViewLayout card;
    private final List<Hit> chapterHits=new ArrayList<Hit>();
    private static final class Hit { String id; int x,y,w,h; Hit(String id,int x,int y,int w,int h) { this.id=id; this.x=x; this.y=y; this.w=w; this.h=h; } boolean contains(int px,int py) { return px>=x && px<x+w && py>=y && py<y+h; } }
    private QuestCore.Book book() { return ClientProxy.snapshot.book; }
    private QuestCore.Quest find(String id) { for(QuestCore.Quest q:book().quests) if(q.id.equals(id)) return q; return null; }
    private QuestFlatButton button(int id,int x,int y,int w,int h,String title,String glyph,boolean tab) {
        QuestFlatButton b=new QuestFlatButton(id,x,y,w,h,QuestText.tr(title)); if(!glyph.isEmpty()) b.glyph(glyph,tab); buttonList.add(b); return b;
    }
    @Override public void initGui() {
        if(positioned) { panX+=(width-viewportWidth)/2D; panY+=(height-viewportHeight)/2D; } viewportWidth=width; viewportHeight=height;
        String filter=search==null?"":search.getText(); search=new GuiTextField(fontRendererObj,24,4,Math.min(160,width-50),14); search.setMaxStringLength(128); search.setText(filter);
        if(!positioned) { positioned=true; if(!book().chapters.isEmpty()) chapter=book().chapters.get(0).id; centerMap(); }
        refresh();
    }
    private void centerMap() {
        List<QuestCore.Quest> list=visible(); double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=minX,maxY=maxX;
        for(QuestCore.Quest q:list) { minX=Math.min(minX,q.x); maxX=Math.max(maxX,q.x); minY=Math.min(minY,q.y); maxY=Math.max(maxY,q.y); }
        panX=width/2D-(list.isEmpty()?0:(minX+maxX)/2*mapUnit()*zoom); panY=height/2D-(list.isEmpty()?0:(minY+maxY)/2*mapUnit()*zoom);
    }
    public void refresh() {
        if(ClientProxy.snapshot==null || search==null) return;
        if(book().disableGui && !ClientProxy.snapshot.canEdit) { mc.displayGuiScreen(null); return; }
        if(find(selected)==null) selected="";
        buttonList.clear();
        int top=1;
        if(QuestClicks.available("","custom:ftbmoney:open_gui")) button(15,0,top,20,18,"Shop","reward",true);
        int right=width-20;
        if(ClientProxy.snapshot.states.containsValue("complete")) { button(20,right,top,20,18,"Collect rewards","reward",true).tint=0xFFF8E71C; top+=18; }
        if(QuestGuides.available("")) { button(12,right,top,20,18,"Guides","book",true).tint=0xFFEF9E1A; top+=18; }
        if(!book().emergencyItems.isEmpty()) { button(10,right,top,20,18,"Help items","alert",true).enabled=ClientProxy.snapshot.emergencyWait==0; top+=18; }
        button(14,right,height-55,20,18,"Pinned","pin",true);
        button(21,right,height-37,20,18,"More options","book",true);
        if(ClientProxy.snapshot.canEdit) button(3,right,height-19,20,18,"Edit book","settings",true);
        if(menu) {
            button(2,width-114,height-152,92,18,"All chapters","",false);
            button(11,width-114,height-133,92,18,"All groups","",false);
            button(9,width-114,height-114,92,18,"Teams","",false);
            button(22,width-114,height-95,92,18,"Search","",false);
            button(1,width-114,height-76,92,18,"Refresh","",false);
            button(0,width-114,height-57,92,18,"Close","",false);
        }
        card=null; QuestCore.Quest q=find(selected);
        if(q!=null) {
            List<QuestCore.Reward> rewards=rewards(q); List<String> lines=description(q);
            card=new QuestViewLayout(width,height,fontRendererObj.getStringWidth(q.title),q.minWidth,q.tasks.size(),rewards.size(),lines.size(),hasGuide(q));
            buttonList.clear();
            if("available".equals(ClientProxy.snapshot.states.get(q.id))) button(4,card.x+14,card.y+17,12,12,"Check","check",true);
            if(q.repeatable && !q.autoRepeat && "claimed".equals(ClientProxy.snapshot.states.get(q.id))) button(6,card.x+14,card.y+17,12,12,"Repeat","refresh",true);
            button(16,card.x+card.width-14,card.y+2,12,12,"Close","close",true);
            button(13,card.x+card.width-26,card.y+2,12,12,ClientProxy.snapshot.pinned.contains(q.id)?"Unpin quest":"Pin quest","pin",true).tint=ClientProxy.snapshot.pinned.contains(q.id)?0xFFF8BD1C:0xFFDBDBDB;
            button(17,card.x,card.y+17,13,13,"Dependencies","left",true).enabled=!q.dependencies.isEmpty();
            button(18,card.x+card.width-13,card.y+17,13,13,"Dependants","right",true).enabled=!dependants(q).isEmpty();

        }
    }
    private List<QuestCore.Quest> visible() {
        List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>(); String filter=search==null?"":search.getText().toLowerCase(Locale.ROOT);
        for(QuestCore.Quest q:book().quests) if(QuestPresentation.within(book(),q.chapter,group) && (chapter.isEmpty() || q.chapter.equals(chapter)) && (filter.isEmpty() || q.title.toLowerCase(Locale.ROOT).contains(filter) || q.id.toLowerCase(Locale.ROOT).contains(filter) || q.tags.toString().toLowerCase(Locale.ROOT).contains(filter))) result.add(q);
        return result;
    }
    private double mapUnit() { return 24+4*book().theme.questSpacing; }
    private int sx(QuestCore.Quest q) { return (int)Math.round(panX+q.x*mapUnit()*zoom); }
    private int sy(QuestCore.Quest q) { return (int)Math.round(panY+q.y*mapUnit()*zoom); }
    private int radius(QuestCore.Quest q) { return Math.max(3,(int)Math.round(12*q.size*zoom)); }
    @Override public void drawScreen(int mx,int my,float partial) {
        QuestClassicUi.background(mc,width,height);
        List<QuestCore.Quest> list=visible(); QuestCore.Quest hover=null; QuestPresentation.MapImage imageHover=null;
        clip(20,0,width-40,height);
        QuestCore.Chapter definition=QuestPresentation.chapter(book(),chapter);
        if(definition!=null && !definition.image.isEmpty()) { mc.getTextureManager().bindTexture(QuestImages.texture(definition.image)); GL11.glColor4f(1,1,1,.5f); func_152125_a(20,0,0,0,256,256,width-40,height,256,256); GL11.glColor4f(1,1,1,1); }
        for(QuestPresentation.MapImage image:images()) {
            int iw=Math.max(1,(int)(image.width*mapUnit()*zoom)),ih=Math.max(1,(int)(image.height*mapUnit()*zoom));
            GL11.glPushMatrix(); GL11.glTranslated(imageX(image),imageY(image),0); GL11.glRotated(image.rotation,0,0,1); GL11.glColor4f(1,1,1,(float)image.opacity); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(QuestImages.texture(image.resource)); func_152125_a(-iw/2,-ih/2,0,0,256,256,iw,ih,256,256); GL11.glPopMatrix(); GL11.glColor4f(1,1,1,1); if(imageContains(image,mx,my)) imageHover=image;
        }
        String focus=selected; if(card==null) for(QuestCore.Quest q:list) if(Math.abs(mx-sx(q))<=radius(q) && Math.abs(my-sy(q))<=radius(q)) { focus=q.id; break; }
        drawEdges(list,focus);
        for(QuestCore.Quest q:list) {
            int x=sx(q),y=sy(q),r=radius(q); String state=ClientProxy.snapshot.states.get(q.id); String[] style=QuestPresentation.resolve(book(),q,state);
            boolean over=Math.abs(mx-x)<=r && Math.abs(my-y)<=r;
            int outline="complete".equals(state) || "claimed".equals(state)?0xC856FF56:"locked".equals(state)?0xFF808080:0x96FFFFFF;
            if(!style[0].equals(defaultColor(state))) outline=QuestPresentation.color(style[0]);
            QuestNodeRenderer.draw(mc,x,y,r,style[2],outline); QuestNodeRenderer.draw(mc,x,y,Math.max(1,r-1),style[2],0xFF333333); QuestNodeRenderer.draw(mc,x,y,Math.max(1,r-2),style[2],0xFF909090);
            int size=Math.max(2,r*4/3); QuestClassicUi.item(mc,q.icon,q.iconMeta,q.iconNbt,x-size/2,y-size/2,size);
            if("locked".equals(state)) nodeOverlay(x,y,r,style[2],0x64000000);
            if(q.id.equals(selected)) nodeOverlay(x,y,r,style[2],((int)(190+Math.sin(System.currentTimeMillis()*.003)*50)<<24)|0xFFFFFF);
            if(over && card==null) { hover=q; nodeOverlay(x,y,r,style[2],0x64FFFFFF); }
            if("claimed".equals(state)) QuestClassicUi.icon("check",x,y-r,r,0xFF65D529); else if("complete".equals(state)) QuestClassicUi.icon("alert",x,y-r,r,0xFFFFDD55);
        }
        unclip(); GL11.glPushMatrix(); GL11.glTranslated(0,0,300); drawChapters(mx,my); GL11.glPopMatrix();
        if(showSearch && card==null) search.drawTextBox();
        if(card!=null) { GL11.glPushMatrix(); GL11.glTranslated(0,0,500); drawCard(mx,my); GL11.glPopMatrix(); } else super.drawScreen(mx,my,partial);
        if(card==null) {
            if(hover!=null && hoverChapter.isEmpty() && mx>=20 && mx<width-20) { List<String> tip=new ArrayList<String>(); tip.add(hover.title); if(!hover.subtitle.isEmpty()) tip.add("\u00a77"+hover.subtitle); if(hover.optional) tip.add("\u00a77"+QuestText.tr("Optional")); drawHoveringText(tip,mx,my,fontRendererObj); }
            else if(imageHover!=null && mx>=20 && mx<width-20) { List<String> tip=imageHover.hover.isEmpty()?imageHover.uri.isEmpty()?Collections.<String>emptyList():Collections.singletonList(imageHover.uri):imageHover.hover; if(!tip.isEmpty()) drawHoveringText(tip,mx,my,fontRendererObj); }
        }
        if(card!=null && mx>=card.x+28 && mx<card.x+card.width-28 && my>=card.y && my<card.y+15) drawHoveringText(Arrays.asList(find(selected).title,"\u00a77"+QuestText.tr("Tasks / start")),mx,my,fontRendererObj);
        for(Object raw:buttonList) { QuestFlatButton b=(QuestFlatButton)raw; if(b.visible && mx>=b.xPosition && my>=b.yPosition && mx<b.xPosition+b.width && my<b.yPosition+b.height && !b.glyph.isEmpty()) drawHoveringText(Collections.singletonList(b.tooltip),mx,my,fontRendererObj); }
        if(ClientProxy.snapshot.message.startsWith("Error:")) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,width-48),24,height-11,0xFFFF8888);
    }
    private void nodeOverlay(int x,int y,int radius,String shape,int color) { GL11.glPushMatrix(); GL11.glTranslated(0,0,150); QuestNodeRenderer.draw(mc,x,y,radius,shape,color); GL11.glPopMatrix(); }
    private String defaultColor(String state) { QuestPresentation.Theme t=DEFAULT_THEME; return "claimed".equals(state)?t.claimed:"complete".equals(state)?t.complete:"available".equals(state)?t.available:t.locked; }
    private void drawEdges(List<QuestCore.Quest> list,String focus) {
        if(book().theme.lineWidth==0) return;
        Map<String,QuestCore.Quest> byId=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest q:list) byId.put(q.id,q);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            QuestClassicUi.bindDependencies(mc); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glShadeModel(GL11.GL_SMOOTH);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance; t.startDrawingQuads(); int edges=0;
            for(QuestCore.Quest q:list) if(!q.hideDependencyLines) for(String dependency:q.dependencies) try { for(String owner:QuestDependencies.owners(book(),byId,dependency,q.dependencyState.equals("started"))) {
                QuestCore.Quest from=byId.get(owner); if(from==null || ++edges>8192) continue;
                double dx=sx(q)-sx(from),dy=sy(q)-sy(from),length=Math.hypot(dx,dy); if(length<.01) continue;
                double half=2.04*zoom*book().theme.lineWidth,px=-dy/length*half,py=dx/length*half;
                String state=ClientProxy.snapshot.states.get(from.id); boolean done="complete".equals(state) || "claimed".equals(state),highlight=from.id.equals(focus) || q.id.equals(focus);
                int color=highlight?(from.id.equals(focus)?0xC8C800:0x00C8C8):done?0x64DC64:0xFFFFFF;
                int alpha=255; if(!book().theme.edge.equals(DEFAULT_THEME.edge)) { int argb=QuestPresentation.color(book().theme.edge); color=argb&0xFFFFFF; alpha=argb>>>24; }
                int red=color>>16&255,green=color>>8&255,blue=color&255; double offset=highlight?(System.currentTimeMillis()%1200)/1200D:0,u=length/(half*2);
                t.setColorRGBA(red*3/4,green*3/4,blue*3/4,alpha); t.addVertexWithUV(sx(from)-px,sy(from)-py,0,-offset,0); t.addVertexWithUV(sx(from)+px,sy(from)+py,0,-offset,1);
                t.setColorRGBA(red,green,blue,alpha); t.addVertexWithUV(sx(q)+px,sy(q)+py,0,u-offset,1); t.addVertexWithUV(sx(q)-px,sy(q)-py,0,u-offset,0);
            } } catch(IllegalArgumentException ignored) {}
            t.draw();
        } finally { GL11.glPopAttrib(); GL11.glColor4f(1,1,1,1); }
    }
    private void drawChapters(int mx,int my) {
        chapterHits.clear(); int y=QuestClicks.available("","custom:ftbmoney:open_gui")?20:1,index=0;
        for(QuestCore.Chapter c:book().chapters) if(c.parent.isEmpty() && (ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))) {
            if(index++<chapterScroll) continue; if(y+18>height-20) break;
            chapterHits.add(new Hit(c.id,0,y,20,18)); boolean over=mx<20 && my>=y && my<y+18 && card==null;
            if(over) { hoverChapter=c.id; hoverY=y; }
            if(c.id.equals(chapter) || QuestPresentation.within(book(),chapter,c.id)) drawRect(1,y,19,y+18,0x44000000);
            chapterIcon(c,2,y+1); y+=18;
        }
        if(ClientProxy.snapshot.canEdit && y+18<height) { QuestClassicUi.icon("add",2,y+1,16,0xFFFFFFFF); chapterHits.add(new Hit("",0,y,20,18)); }
        if(card!=null || hoverChapter.isEmpty()) return;
        QuestCore.Chapter hovered=QuestPresentation.chapter(book(),hoverChapter); if(hovered==null) return;
        List<QuestCore.Chapter> expanded=new ArrayList<QuestCore.Chapter>(); expanded.add(hovered); for(QuestCore.Chapter c:book().chapters) if(c.parent.equals(hovered.id) && (ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))) expanded.add(c);
        int w=100; for(QuestCore.Chapter c:expanded) w=Math.max(w,Math.min(width-24,fontRendererObj.getStringWidth(c.title)+26)); int top=Math.max(0,Math.min(hoverY,height-expanded.size()*18-1));
        if(mx>=w || my<top || my>=top+expanded.size()*18) { hoverChapter=""; return; }
        for(int i=0;i<expanded.size();i++) { QuestCore.Chapter c=expanded.get(i); int x=i==0?0:20,cy=top+i*18; QuestClassicUi.panel(mc,x,cy,w-x,18); if(my>=cy && my<cy+18 && mx>=x) drawRect(x+1,cy+1,w-1,cy+17,0x44000000); chapterIcon(c,x+2,cy+1); fontRendererObj.drawString(fontRendererObj.trimStringToWidth(c.title,w-x-24),x+22,cy+5,0xFFFFFFFF); chapterHits.add(new Hit(c.id,x,cy,w-x,18)); }
    }
    private void chapterIcon(QuestCore.Chapter c,int x,int y) {
        String icon=c.icon.isEmpty()?"minecraft:book":c.icon; int meta=c.iconMeta; String nbt=c.iconNbt; for(QuestCore.Quest q:book().quests) if(c.icon.isEmpty() && q.chapter.equals(c.id)) { icon=q.icon; meta=q.iconMeta; nbt=q.iconNbt; break; }
        QuestClassicUi.item(mc,icon,meta,nbt,x,y,16);
        String state=ClientProxy.snapshot.chapterStates.get(c.id); if("complete".equals(state) || "claimed".equals(state)) QuestClassicUi.icon("check",x+9,y,8,0xFF65D529);
    }
    private List<QuestCore.Reward> rewards(QuestCore.Quest q) { List<QuestCore.Reward> result=new ArrayList<QuestCore.Reward>(); for(QuestCore.Reward r:q.rewards) if(ClientProxy.snapshot.canEdit || !r.auto.equals("invisible")) result.add(r); return result; }
    private boolean hasGuide(QuestCore.Quest q) { return !q.pages.isEmpty() || !q.richPages.isEmpty() || !q.guidePage.isEmpty() || !q.customClick.isEmpty() || !q.uri.isEmpty(); }
    private List<String> description(QuestCore.Quest q) {
        List<String> lines=new ArrayList<String>(); int w=card==null?Math.max(180,Math.min(width-8,Math.max(201,Math.max(fontRendererObj.getStringWidth(q.title)+50,q.minWidth))))-6:card.width-6;
        if(!q.subtitle.isEmpty()) lines.addAll(fontRendererObj.listFormattedStringToWidth("\u00a77\u00a7o"+q.subtitle,w));
        if(!q.description.isEmpty()) { if(!lines.isEmpty()) lines.add(""); lines.addAll(fontRendererObj.listFormattedStringToWidth(q.description,w)); } return lines;
    }
    private void drawCard(int mx,int my) {
        QuestCore.Quest q=find(selected); if(q==null) return; List<QuestCore.Reward> rewards=rewards(q);
        drawRect(0,0,width,height,0x78333333); QuestClassicUi.panel(mc,card.x,card.y,card.width,card.height);
        QuestClassicUi.item(mc,q.icon,q.iconMeta,q.iconNbt,card.x+2,card.y+2,12);
        String title=fontRendererObj.trimStringToWidth(q.title,card.width-50); fontRendererObj.drawString(title,card.x+(card.width-fontRendererObj.getStringWidth(title))/2,card.y+4,0xFFAAAAAA);
        drawRect(card.x+1,card.y+15,card.x+card.width-1,card.y+16,QuestClassicUi.BORDER);
        int half=card.width/2; centered(QuestText.tr("Tasks"),card.x+half/2,card.y+20,0xFF5555FF); centered(QuestText.tr("Rewards"),card.x+half+half/2,card.y+20,0xFFFFAA00);
        clip(card.x+1,card.y+31,card.width-2,card.height-34);
        int contentOffset=detailsScroll*9,divider=card.y+card.textTop-6-contentOffset;
        drawRect(card.x+half,card.y+16,card.x+half+1,Math.max(card.y+16,divider),QuestClassicUi.BORDER);
        if(divider>card.y+16) drawRect(card.x+1,divider,card.x+card.width-1,divider+1,QuestClassicUi.BORDER);
        List<String> tip=null;
        for(int i=0;i<q.tasks.size();i++) {
            QuestCore.Task t=q.tasks.get(i); int x=card.cellX(false,i,q.tasks.size()),y=card.cellY(i)-contentOffset; boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(q.id+":"+t.id));
            cell(x,y,mx,my,!done && "available".equals(ClientProxy.snapshot.states.get(q.id))); String item=QuestClassicUi.taskItem(t);
            if(item.isEmpty()) QuestClassicUi.icon(t.type.equals("check")?"check":"settings",x+1,y+1,16,done?0xFF65D529:0xFFFFFFFF); else QuestClassicUi.item(mc,item,t.icon.isEmpty()?t.meta:t.iconMeta,t.icon.isEmpty()?t.nbt:t.iconNbt,x+1,y+1,16);
            if(done) QuestClassicUi.icon("check",x+10,y-2,9,0xFF65D529);
            if(overCell(x,y,mx,my)) { tip=new ArrayList<String>(); tip.add(QuestTaskLabels.title(t)); Long value=ClientProxy.snapshot.counters.get(q.id+":"+t.id); tip.add("\u00a77"+(done?t.count:value==null?0:value)+" / "+t.count); if(!t.description.isEmpty()) tip.addAll(fontRendererObj.listFormattedStringToWidth(t.description,220)); }
        }
        for(int i=0;i<rewards.size();i++) {
            QuestCore.Reward r=rewards.get(i); int x=card.cellX(true,i,rewards.size()),y=card.cellY(i)-contentOffset; boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(q.id+":"+r.id));
            cell(x,y,mx,my,!done && "complete".equals(ClientProxy.snapshot.states.get(q.id))); QuestClassicUi.item(mc,QuestClassicUi.rewardItem(r),r.icon.isEmpty()?r.meta:r.iconMeta,r.icon.isEmpty()?r.nbt:r.iconNbt,x+1,y+1,16);
            if(done) { drawRect(x+1,y+1,x+17,y+17,0x66000000); QuestClassicUi.icon("check",x+10,y-2,9,0xFF65D529); }
            if(r.type.equals("item") && r.count>1) { String amount=Integer.toString(r.count); fontRendererObj.drawStringWithShadow(amount,x+18-fontRendererObj.getStringWidth(amount),y+11,0xFFFFFFFF); }
            if(overCell(x,y,mx,my)) { tip=new ArrayList<String>(); tip.add(QuestTaskLabels.reward(r)); if(done) tip.add("\u00a77"+QuestText.tr("Claimed")); if(!r.description.isEmpty()) tip.addAll(fontRendererObj.listFormattedStringToWidth(r.description,220)); }
        }
        if(q.tasks.isEmpty()) centered(QuestText.tr("No tasks"),card.x+half/2,card.y+36-contentOffset,0xFF999999);
        if(rewards.isEmpty()) centered(QuestText.tr("No rewards"),card.x+half+half/2,card.y+36-contentOffset,0xFF999999);
        String bodyColor=QuestPresentation.resolve(book(),q,ClientProxy.snapshot.states.get(q.id))[1]; int bodyText=bodyColor.equals(DEFAULT_THEME.text)?0xFFFFFFFF:QuestPresentation.color(bodyColor);
        int y=card.y+card.textTop-contentOffset; for(String line:description(q)) { if(line.startsWith("\u00a77\u00a7o")) centered(line,card.x+card.width/2,y,0xFFAAAAAA); else fontRendererObj.drawString(line,card.x+3,y,bodyText); y+=line.isEmpty()?7:9; }
        if(hasGuide(q)) { QuestClassicUi.frame(card.x+3,y+2,card.width-6,18,QuestClassicUi.BORDER); centered(QuestText.tr("Open in guide"),card.x+card.width/2,y+7,0xFFEF9E1A); }
        unclip(); super.drawScreen(mx,my,0);
        if(tip!=null && my>=card.y+31 && my<card.bodyBottom) drawHoveringText(tip,mx,my,fontRendererObj);
    }
    private void centered(String text,int x,int y,int color) { fontRendererObj.drawString(text,x-fontRendererObj.getStringWidth(text)/2,y,color); }
    private void cell(int x,int y,int mx,int my,boolean enabled) { if(overCell(x,y,mx,my)) { QuestClassicUi.frame(x,y,18,18,QuestClassicUi.BORDER); drawRect(x+1,y+1,x+17,y+17,enabled?0x44FFFFFF:0x44000000); } }
    private boolean overCell(int x,int y,int mx,int my) { return mx>=x && mx<x+18 && my>=y && my<y+18; }
    private void clip(int x,int y,int w,int h) { net.minecraft.client.gui.ScaledResolution s=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight); int f=s.getScaleFactor(); GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(x*f,(s.getScaledHeight()-y-h)*f,Math.max(0,w*f),Math.max(0,h*f)); }
    private void unclip() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
    @Override protected void actionPerformed(GuiButton b) {
        QuestNetwork.Action a=new QuestNetwork.Action(); a.id=selected;
        switch(b.id) {
            case 0: mc.displayGuiScreen(null); return;
            case 1: a.action="refresh"; break;
            case 2: List<QuestCore.Chapter> chapters=new ArrayList<QuestCore.Chapter>(); for(QuestCore.Chapter c:book().chapters) if(QuestPresentation.within(book(),c.id,group)) chapters.add(c); chapterIndex++; if(chapterIndex>=chapters.size()) chapterIndex=-1; chooseChapter(chapterIndex<0?"":chapters.get(chapterIndex).id); return;
            case 11: List<QuestCore.Chapter> groups=groups(); if(++groupIndex>=groups.size()) groupIndex=-1; group=groupIndex<0?"":groups.get(groupIndex).id; chooseChapter(""); return;
            case 3: mc.displayGuiScreen(new QuestObjectScreen(this)); return;
            case 4: a.action="check"; break;
            case 5: if(find(selected)==null) return; if(RewardChoiceScreen.needed(find(selected))) { mc.displayGuiScreen(new RewardChoiceScreen(this,find(selected))); return; } a.action="claim"; break;
            case 6: a.action="repeat"; break;
            case 7: if(find(selected)!=null) mc.displayGuiScreen(new QuestTaskScreen(this,find(selected))); return;
            case 8: if(find(selected)!=null) mc.displayGuiScreen(new QuestGuideScreen(this,find(selected))); return;
            case 9: mc.displayGuiScreen(new QuestTeamScreen(this)); return;
            case 10: a.action="emergency"; break;
            case 12: QuestGuides.open("",this); return;
            case 13: a.action="pin"; break;
            case 14: mc.displayGuiScreen(new QuestPinScreen(this)); return;
            case 15: QuestClicks.open("","custom:ftbmoney:open_gui",this); return;
            case 16: selected=""; detailsScroll=0; refresh(); return;
            case 17: case 18: navigate(b.id==17); return;
            case 20: for(QuestCore.Quest q:book().quests) if("complete".equals(ClientProxy.snapshot.states.get(q.id))) { selected=q.id; detailsScroll=0; refresh(); return; } return;
            case 21: menu=!menu; refresh(); return;
            case 22: showSearch=!showSearch; if(!showSearch) search.setText(""); menu=false; search.setFocused(showSearch); refresh(); return;
            default: return;
        }
        ClientProxy.send(a);
    }
    private void chooseChapter(String id) { chapter=id; selected=""; hoverChapter=""; detailsScroll=0; centerMap(); refresh(); }
    private List<QuestCore.Quest> dependants(QuestCore.Quest q) { List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>(); Map<String,QuestCore.Quest> all=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest other:book().quests) all.put(other.id,other); for(QuestCore.Quest other:book().quests) for(String ref:other.dependencies) try { if(QuestDependencies.owners(book(),all,ref,other.dependencyState.equals("started")).contains(q.id)) { result.add(other); break; } } catch(IllegalArgumentException ignored) {} return result; }
    private void navigate(boolean backwards) { QuestCore.Quest q=find(selected); if(q==null) return; List<QuestCore.Quest> candidates=dependants(q); if(backwards) { candidates.clear(); for(String ref:q.dependencies) { QuestCore.Quest from=find(ref); if(from!=null) candidates.add(from); } } if(!candidates.isEmpty()) { selected=candidates.get(0).id; chapter=candidates.get(0).chapter; detailsScroll=0; centerMap(); refresh(); } }
    @Override protected void mouseClicked(int x,int y,int mouseButton) {
        for(Object raw:buttonList) { GuiButton b=(GuiButton)raw; if(b.visible && x>=b.xPosition && x<b.xPosition+b.width && y>=b.yPosition && y<b.yPosition+b.height) { super.mouseClicked(x,y,mouseButton); return; } }
        if(card!=null) {
            if(!card.contains(x,y)) { selected=""; detailsScroll=0; refresh(); return; }
            if(mouseButton==0 && y>=card.y && y<card.y+15 && x>=card.x+28 && x<card.x+card.width-28) { mc.displayGuiScreen(new QuestTaskScreen(this,find(selected))); return; }
            if(mouseButton!=0 || y<card.y+31 || y>=card.bodyBottom) return; QuestCore.Quest q=find(selected); if(q==null) return;
            for(int i=0;i<q.tasks.size();i++) if(overCell(card.cellX(false,i,q.tasks.size()),card.cellY(i)-detailsScroll*9,x,y)) { QuestCore.Task t=q.tasks.get(i); if(t.type.equals("item") && !isShiftKeyDown()) { mc.displayGuiScreen(new QuestItemTaskScreen(this,q,t)); return; } if(Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(q.id+":"+t.id)) && !"available".equals(ClientProxy.snapshot.states.get(q.id))) return; QuestNetwork.Action a=new QuestNetwork.Action(); a.id=q.id; a.text=t.id; a.action="click_task"; ClientProxy.send(a); return; }
            List<QuestCore.Reward> rs=rewards(q); for(int i=0;i<rs.size();i++) if(overCell(card.cellX(true,i,rs.size()),card.cellY(i)-detailsScroll*9,x,y)) { QuestCore.Reward r=rs.get(i); if(Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(q.id+":"+r.id)) || !"complete".equals(ClientProxy.snapshot.states.get(q.id))) return; if(r.type.equals("choice")) mc.displayGuiScreen(new RewardChoiceScreen(this,q,r)); else claim(q,r); return; }
            if(hasGuide(q) && y>=card.y+card.textTop+description(q).size()*9-detailsScroll*9) mc.displayGuiScreen(new QuestGuideScreen(this,q)); return;
        }
        if(showSearch) { search.mouseClicked(x,y,mouseButton); if(search.isFocused()) return; }
        for(int i=chapterHits.size()-1;i>=0;i--) if(chapterHits.get(i).contains(x,y)) { if(chapterHits.get(i).id.isEmpty()) mc.displayGuiScreen(new QuestObjectScreen(this)); else chooseChapter(chapterHits.get(i).id); return; }
        if(x<20 || x>=width-20 || mouseButton!=0) return;
        for(QuestCore.Quest q:visible()) if(Math.abs(x-sx(q))<=radius(q) && Math.abs(y-sy(q))<=radius(q)) { selected=q.id; detailsScroll=0; if(ClientProxy.snapshot.canEdit && isShiftKeyDown()) { moving=true; movingId=q.id; selected=""; } else if(!q.customClick.isEmpty()) QuestClicks.open(q.id,q.customClick,this); lastX=x; lastY=y; refresh(); return; }
        List<QuestPresentation.MapImage> list=images(); for(int i=list.size()-1;i>=0;i--) { QuestPresentation.MapImage image=list.get(i); if(!image.uri.isEmpty() && imageContains(image,x,y)) { pendingLink=image.uri; mc.displayGuiScreen(new GuiConfirmOpenLink(this,image.uri,1000,false)); return; } }
        dragging=true; lastX=x; lastY=y;
    }
    private void claim(QuestCore.Quest q,QuestCore.Reward r) { QuestNetwork.Action a=new QuestNetwork.Action(); a.action="claim_reward"; a.id=q.id; a.text=r.id; ClientProxy.send(a); }
    private List<QuestCore.Chapter> groups() { Set<String> parents=new HashSet<String>(); for(QuestCore.Chapter c:book().chapters) if(!c.parent.isEmpty()) parents.add(c.parent); List<QuestCore.Chapter> result=new ArrayList<QuestCore.Chapter>(); for(QuestCore.Chapter c:book().chapters) if(parents.contains(c.id)) result.add(c); return result; }
    private List<QuestPresentation.MapImage> images() { QuestCore.Chapter c=QuestPresentation.chapter(book(),chapter); List<QuestPresentation.MapImage> result=new ArrayList<QuestPresentation.MapImage>(); if(c!=null) for(QuestPresentation.MapImage i:c.images) if(!i.dev || ClientProxy.snapshot.canEdit) result.add(i); return result; }
    private double imageX(QuestPresentation.MapImage image) { return panX+image.x*mapUnit()*zoom; }
    private double imageY(QuestPresentation.MapImage image) { return panY+image.y*mapUnit()*zoom; }
    private boolean imageContains(QuestPresentation.MapImage image,int x,int y) { double dx=x-imageX(image),dy=y-imageY(image),a=Math.toRadians(image.rotation),s=mapUnit()*zoom; return Math.abs(Math.cos(a)*dx+Math.sin(a)*dy)<=image.width*s/2 && Math.abs(-Math.sin(a)*dx+Math.cos(a)*dy)<=image.height*s/2; }
    @Override public void confirmClicked(boolean accepted,int id) { if(accepted && id==1000 && !pendingLink.isEmpty() && QuestPresentation.link(pendingLink)) try { java.awt.Desktop.getDesktop().browse(new java.net.URI(pendingLink)); } catch(Exception ex) { cpw.mods.fml.common.FMLLog.warning("Could not open map link: %s",ex.toString()); } mc.displayGuiScreen(this); }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed) { if(dragging) { panX+=x-lastX; panY+=y-lastY; } if(moving) { QuestCore.Quest q=find(movingId); if(q!=null) { q.x+=(x-lastX)/(mapUnit()*zoom); q.y+=(y-lastY)/(mapUnit()*zoom); } } lastX=x; lastY=y; }
    @Override protected void mouseMovedOrUp(int x,int y,int button) { super.mouseMovedOrUp(x,y,button); dragging=false; if(moving) { moving=false; QuestCore.Quest q=find(movingId); if(q!=null) { q.x=QuestPresentation.snap(book(),q.x,q.size); q.y=QuestPresentation.snap(book(),q.y,q.size); } QuestNetwork.Action a=new QuestNetwork.Action(); a.action="save"; a.revision=ClientProxy.snapshot.revision; a.book=book(); ClientProxy.send(a); } }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel==0) return; int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1; if(card!=null) { int total=card.textTop+description(find(selected)).size()*9+(hasGuide(find(selected))?22:0)+7; int max=Math.max(0,(total-card.height+8)/9); detailsScroll=Math.max(0,Math.min(max,detailsScroll+(wheel>0?-2:2))); } else if(x<20) chapterScroll=Math.max(0,Math.min(Math.max(0,book().chapters.size()-1),chapterScroll+(wheel>0?-1:1))); else { double old=zoom; zoom=Math.max(.25,Math.min(3,zoom+(wheel>0?.25:-.25))); panX=x-(x-panX)*zoom/old; panY=y-(y-panY)*zoom/old; } }
    @Override protected void keyTyped(char c,int key) { if(key==Keyboard.KEY_ESCAPE && !selected.isEmpty()) { selected=""; detailsScroll=0; refresh(); return; } if(key==Keyboard.KEY_ESCAPE && (showSearch || menu)) { search.setText(""); showSearch=false; menu=false; refresh(); return; } if(key==Keyboard.KEY_F && isCtrlKeyDown()) { showSearch=true; search.setFocused(true); return; } if(showSearch && search.textboxKeyTyped(c,key)) return; super.keyTyped(c,key); }
    @Override public void updateScreen() { if(search!=null) search.updateCursorCounter(); if(++refreshTicks%40==0 && !dragging && !moving) ClientProxy.send(new QuestNetwork.Action()); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
