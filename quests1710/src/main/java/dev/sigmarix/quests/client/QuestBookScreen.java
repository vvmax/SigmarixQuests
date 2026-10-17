package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;

/** Fullscreen chapter tree and a centered, independently scrolling quest card. */
public final class QuestBookScreen extends QuestLibraryScreen implements GuiYesNoCallback {
    private static final QuestPresentation.Theme DEFAULT_THEME=new QuestPresentation.Theme();
    private GuiTextField search;
    private String selected="",chapter="",group="",pendingLink="",hoverChapter="",movingId="";
    private double panX,panY,zoom=1,layoutMinX=-40,layoutMinY=-30;private long lastShiftPress;
    private int lastX,lastY,detailsScroll,chapterIndex=-1,groupIndex=-1,refreshTicks,chapterScroll,hoverY,viewportWidth,viewportHeight;
    private boolean positioned,dragging,moving,showSearch,menu,hideCard;
    private final Set<String> selectedObjects=new LinkedHashSet<String>();
    private boolean placingSelection;private static boolean editorGrid;
    private QuestViewLayout card;
    private QuestPresentation.MapImage movingImage;private int movingImageIndex;private double imageStartX,imageStartY;
    private final List<Hit> chapterHits=new ArrayList<Hit>();
    private static final class Hit { String id; int x,y,w,h; Hit(String id,int x,int y,int w,int h) { this.id=id; this.x=x; this.y=y; this.w=w; this.h=h; } boolean contains(int px,int py) { return px>=x && px<x+w && py>=y && py<y+h; } }
    private QuestCore.Book book() { return ClientProxy.snapshot.book; }
    private QuestCore.Quest find(String id) { for(QuestCore.Quest q:book().quests) if(q.id.equals(id)) return q; return null; }
    private QuestFlatButton button(int id,int x,int y,int w,int h,String title,String glyph,boolean tab) {
        QuestFlatButton b=new QuestFlatButton(id,x,y,w,h,QuestText.tr(title)); if(!glyph.isEmpty()) b.glyph(glyph,tab); buttonList.add(b); return b;
    }
    @Override public void initGui() {
        if(positioned) { panX+=(width-viewportWidth)/2D; panY+=(height-viewportHeight)/2D; } viewportWidth=width; viewportHeight=height;
        String filter=search==null?"":search.getText(); search=new GuiTextField(QuestLibraryScreen.inputFont(fontRendererObj),24,4,Math.min(160,width-50),14); search.setMaxStringLength(128); search.setText(filter);
        if(!positioned) { positioned=true; if(!book().chapters.isEmpty()) chapter=book().chapters.get(0).id; centerMap(); }
        refresh();
    }
    private void centerMap() {
        List<QuestCore.Quest> list=visible(); double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=minX,maxY=maxX;
        for(QuestCore.Quest q:list) { minX=Math.min(minX,q.x-q.size/2); maxX=Math.max(maxX,q.x+q.size/2); minY=Math.min(minY,q.y-q.size/2); maxY=Math.max(maxY,q.y+q.size/2); }
        for(QuestPresentation.MapImage image:images()){minX=Math.min(minX,image.x-image.width/2);maxX=Math.max(maxX,image.x+image.width/2);minY=Math.min(minY,image.y-image.height/2);maxY=Math.max(maxY,image.y+image.height/2);}
        updateNodeBounds(list);panX=width/2D-(!Double.isFinite(minX)?0:(minX+maxX)/2*mapUnit()*zoom); panY=height/2D-(!Double.isFinite(minY)?0:(minY+maxY)/2*mapUnit()*zoom);
    }
    public void refresh() {
        if(ClientProxy.snapshot==null || search==null) return;
        if(book().disableGui && !ClientProxy.snapshot.canEdit) { mc.displayGuiScreen(null); return; }
        if(find(selected)==null) selected="";
        buttonList.clear();
        int top=1;
        if(QuestClicks.available("","custom:ftbmoney:open_gui")) button(15,0,top,20,18,"Shop","shop",true);
        int right=width-20;
        if(ClientProxy.snapshot.states.containsValue("complete")) { button(20,right,top,20,18,"Collect rewards","reward",true).tint=0xFFF8E71C; top+=18; }
        if(QuestGuides.available("")) { button(12,right,top,20,18,"Guides","book",true).tint=0xFFEF9E1A; top+=18; }
        if(!book().emergencyItems.isEmpty()) { button(10,right,top,20,18,"Help items","emergency_items",true); top+=18; }
        button(14,right,height-(ClientProxy.snapshot.canEdit?55:37),20,18,ClientProxy.snapshot.autoPin?"Automatic tracking: ON":"Automatic tracking: OFF","pin",true).tint=ClientProxy.snapshot.autoPin?0xFFF8BD1C:0xFFDBDBDB;
        button(21,right,height-(ClientProxy.snapshot.canEdit?37:19),20,18,"Support","support",true);
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
        if(q!=null && !hideCard) {
            List<QuestCore.Reward> rewards=rewards(q); List<String> lines=description(q);
            card=new QuestViewLayout(width,height,fontRendererObj.getStringWidth(QuestTaskLabels.quest(q)),q.minWidth,q.tasks.size()+(ClientProxy.snapshot.canEdit?1:0),rewards.size()+(ClientProxy.snapshot.canEdit?1:0),lines.size(),hasGuide(q),textHeight(q));
            buttonList.clear();
            button(16,card.x+card.width-14,card.y+2,12,12,"Close","close",true);
            button(13,card.x+card.width-26,card.y+2,12,12,ClientProxy.snapshot.pinned.contains(q.id)?"Unpin quest":"Pin quest","pin",true).tint=ClientProxy.snapshot.pinned.contains(q.id)?0xFFF8BD1C:0xFFDBDBDB;
            button(17,card.x,card.y+17,13,13,"Dependencies","left",true).enabled=!q.dependencies.isEmpty();
            button(18,card.x+card.width-13,card.y+17,13,13,"Dependants","right",true).enabled=!dependants(q).isEmpty();

        }
    }
    private List<QuestCore.Quest> visible() {
        List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>(); String filter=search==null?"":search.getText().toLowerCase(Locale.ROOT);
        for(QuestCore.Quest q:book().quests) if(QuestPresentation.within(book(),q.chapter,group) && (chapter.isEmpty() || q.chapter.equals(chapter)) && (filter.isEmpty() || QuestTaskLabels.quest(q).toLowerCase(Locale.ROOT).contains(filter) || q.id.toLowerCase(Locale.ROOT).contains(filter) || q.tags.toString().toLowerCase(Locale.ROOT).contains(filter))) result.add(q);
        return result;
    }
    private double mapUnit() { return 24+4*book().theme.questSpacing; }
    private void updateNodeBounds(List<QuestCore.Quest> list){double minX=Double.POSITIVE_INFINITY,minY=minX;for(QuestCore.Quest quest:list){minX=Math.min(minX,quest.x-quest.size/2);minY=Math.min(minY,quest.y-quest.size/2);}for(QuestPresentation.MapImage image:images()){minX=Math.min(minX,image.x-image.width/2);minY=Math.min(minY,image.y-image.height/2);}layoutMinX=(Double.isFinite(minX)?minX:0)-40;layoutMinY=(Double.isFinite(minY)?minY:0)-30;}
    private int nodeSize(QuestCore.Quest q){return Math.max(0,(int)(24*zoom*q.size));}
    private int nodeLeft(QuestCore.Quest q){double unit=mapUnit()*zoom,scroll=20-panX-layoutMinX*unit;return 20+(int)-scroll+(int)((q.x-layoutMinX)*unit-12*zoom*q.size);}
    private int nodeTop(QuestCore.Quest q){double unit=mapUnit()*zoom,scroll=1-panY-layoutMinY*unit;return 1+(int)-scroll+(int)((q.y-layoutMinY)*unit-12*zoom*q.size);}
    private double sx(QuestCore.Quest q) { return nodeLeft(q)+nodeSize(q)/2D; }
    private double sy(QuestCore.Quest q) { return nodeTop(q)+nodeSize(q)/2D; }
    private boolean hit(QuestCore.Quest quest,int x,int y){return QuestNodeRenderer.contains(mc,sx(quest),sy(quest),radius(quest),QuestPresentation.resolve(book(),quest,ClientProxy.snapshot.states.get(quest.id))[2],x,y);}
    private double radius(QuestCore.Quest q) { return nodeSize(q)/2D; }
    @Override public void drawScreen(int mx,int my,float partial) {
        if(movingImage!=null && mx>=0 && my>=0){movingImage.x=(mx-panX)/(mapUnit()*zoom);movingImage.y=(my-panY)/(mapUnit()*zoom);}
        QuestClassicUi.background(mc,width,height);drawRect(19,1,20,height-1,QuestClassicUi.BORDER);drawRect(1,1,19,height-1,0x44000000);drawRect(width-20,1,width-19,height-1,QuestClassicUi.BORDER);drawRect(width-19,1,width-1,height-1,0x44000000);
        List<QuestCore.Quest> list=visible();updateNodeBounds(list); QuestCore.Quest hover=null; QuestPresentation.MapImage imageHover=null;
        clip(20,0,width-40,height);
        QuestCore.Chapter definition=QuestPresentation.chapter(book(),chapter);
        if(definition!=null && !definition.image.isEmpty()) { mc.getTextureManager().bindTexture(QuestImages.texture(definition.image)); GL11.glColor4f(1,1,1,.5f); func_152125_a(20,0,0,0,256,256,width-40,height,256,256); GL11.glColor4f(1,1,1,1); }
        for(QuestPresentation.MapImage image:images()) {
            int iw=Math.max(1,(int)(image.width*24*zoom)),ih=Math.max(1,(int)(image.height*24*zoom));
            GL11.glPushMatrix(); GL11.glTranslated(imageX(image),imageY(image),0); GL11.glRotatef((float)image.rotation,0,0,1); GL11.glColor4f(1,1,1,(float)image.opacity); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(QuestImages.texture(image.resource));GL11.glScaled(iw/2D,ih/2D,1);net.minecraft.client.renderer.Tessellator imageQuad=net.minecraft.client.renderer.Tessellator.instance;imageQuad.startDrawingQuads();imageQuad.setColorRGBA_F(1,1,1,(float)image.opacity);imageQuad.addVertexWithUV(-1,1,0,0,1);imageQuad.addVertexWithUV(1,1,0,1,1);imageQuad.addVertexWithUV(1,-1,0,1,0);imageQuad.addVertexWithUV(-1,-1,0,0,0);imageQuad.draw(); GL11.glPopMatrix(); GL11.glColor4f(1,1,1,1); if(imageContains(image,mx,my)) imageHover=image;
        }
        String focus=selected;
        drawEdges(list,focus);int nodeIndex=0;
        for(QuestCore.Quest q:list) {
            double x=sx(q),y=sy(q),r=radius(q); String state=ClientProxy.snapshot.states.get(q.id); String[] style=QuestPresentation.resolve(book(),q,state);
            boolean over=hit(q,mx,my);
            int outline="complete".equals(state) || "claimed".equals(state)?0xC856FF56:"locked".equals(state)?0xFF999999:ClientProxy.snapshot.percentages.getOrDefault(q.id,0)>0?0xC800FFFF:0x96FFFFFF;
            if(!style[0].equals(defaultColor(state))) outline=QuestPresentation.color(style[0]);
            QuestNodeRenderer.node(mc,x,y,r,style[2],outline);
            double size=r*4/3;GL11.glPushMatrix();GL11.glTranslated(x-size/2,y-size/2,0);GL11.glScaled(size,size,1);QuestClassicUi.quest(mc,q,0,0,1,nodeIndex++>0);GL11.glPopMatrix(); GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER,.1F);
            if("locked".equals(state)) nodeOverlay(x,y,r,style[2],0x64000000);
            if(q.id.equals(selected) || selectedObjects.contains(q.id)){GL11.glPushMatrix();GL11.glTranslated(0,0,500);QuestNodeRenderer.highlight(mc,x,y,r,style[2],((int)(190+Math.sin(System.currentTimeMillis()*.003)*50)<<24)|0xFFFFFF);GL11.glPopMatrix();}
            if(over && card==null) { hover=q; nodeOverlay(x,y,r,style[2],0x64FFFFFF); }
            if("claimed".equals(state) || "complete".equals(state)){GL11.glPushMatrix();GL11.glTranslated(x,y-r,0);GL11.glScaled(r,r,1);QuestClassicUi.icon("claimed".equals(state)?"check":"alert",0,0,1,"claimed".equals(state)?0xFF65D529:0xFFFFDD55);GL11.glPopMatrix();}
        }
        drawEditorCursor(mx,my);unclip(); GL11.glPushMatrix(); GL11.glTranslated(0,0,300); drawChapters(mx,my); GL11.glPopMatrix();
        if(showSearch && card==null) search.drawTextBox();
        if(card!=null) { GL11.glPushMatrix(); GL11.glTranslated(0,0,500); drawCard(mx,my); GL11.glPopMatrix(); } else super.drawScreen(mx,my,partial);
        QuestClassicUi.frame(0,0,width,height,QuestClassicUi.BORDER);
        if(card==null) {
            if(hover!=null && hoverChapter.isEmpty() && mx>=20 && mx<width-20) { List<String> tip=new ArrayList<String>(); int progress=ClientProxy.snapshot.percentages.getOrDefault(hover.id,0);tip.add(QuestTaskLabels.quest(hover)+(progress>0 && progress<100?" §8"+progress+"%":"")); if(!hover.subtitle.isEmpty()) tip.add("\u00a77"+hover.subtitle); int unclaimed=0;for(QuestCore.Reward reward:rewards(hover))if("complete".equals(ClientProxy.snapshot.states.get(hover.id)) && !Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(hover.id+":"+reward.id)))unclaimed++;if(unclaimed>0)tip.add(net.minecraft.util.StatCollector.translateToLocalFormatted("ftbquests.gui.collect_rewards","§6"+unclaimed));if(hover.optional) tip.add("§7["+net.minecraft.util.StatCollector.translateToLocal("ftbquests.quest.optional")+"]"); drawHoveringText(tip,mx,my,fontRendererObj); }
            else if(imageHover!=null && mx>=20 && mx<width-20) { List<String> tip=imageHover.hover.isEmpty()?imageHover.uri.isEmpty()?Collections.<String>emptyList():Collections.singletonList(imageHover.uri):imageHover.hover; if(!tip.isEmpty()) drawHoveringText(tip,mx,my,fontRendererObj); }
        }
        if(card!=null && mx>=card.x+28 && mx<card.x+card.width-28 && my>=card.y && my<card.y+15) drawHoveringText(Arrays.asList(find(selected).title,"\u00a77"+QuestText.tr("Tasks / start")),mx,my,fontRendererObj);
        for(Object raw:buttonList) { QuestFlatButton b=(QuestFlatButton)raw; if(b.visible && mx>=b.xPosition && my>=b.yPosition && mx<b.xPosition+b.width && my<b.yPosition+b.height && !b.glyph.isEmpty()) drawHoveringText(Collections.singletonList(b.tooltip),mx,my,fontRendererObj); }
        if(ClientProxy.snapshot.message.startsWith("Error:")) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,width-48),24,height-11,0xFFFF8888);
    }
    private void nodeOverlay(double x,double y,double radius,String shape,int color) { GL11.glPushMatrix(); GL11.glTranslated(0,0,500); GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glAlphaFunc(GL11.GL_GREATER,.1F); QuestNodeRenderer.draw(mc,x,y,radius,shape,color); GL11.glPopMatrix(); }
    private String defaultColor(String state) { QuestPresentation.Theme t=DEFAULT_THEME; return "claimed".equals(state)?t.claimed:"complete".equals(state)?t.complete:"available".equals(state)?t.available:t.locked; }
    private static int visualId(String id){String code=id.startsWith("ftb_")?id.substring(4):id;return code.matches("[a-fA-F0-9]{8}")?(int)Long.parseLong(code,16):id.hashCode();}
    private void drawEdges(List<QuestCore.Quest> list,String focus) {
        if(book().theme.lineWidth==0) return;
        Map<String,QuestCore.Quest> byId=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest q:list) byId.put(q.id,q);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            QuestClassicUi.bindDependencies(mc); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glShadeModel(GL11.GL_SMOOTH);
            net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance; int edges=0;
            for(QuestCore.Quest q:list) if(!q.hideDependencyLines) for(String dependency:q.dependencies) try { for(String owner:QuestDependencies.owners(book(),byId,dependency,q.dependencyState.equals("started"))) {
                QuestCore.Quest from=byId.get(owner); if(from==null || ++edges>8192) continue;
                double dx=sx(q)-sx(from),dy=sy(q)-sy(from),length=Math.hypot(dx,dy); if(length<.01) continue;
                double half=2.04*zoom*book().theme.lineWidth,px=-dy/length*half,py=dx/length*half;
                String state=ClientProxy.snapshot.states.get(q.id); boolean done="complete".equals(state) || "claimed".equals(state),highlight=from.id.equals(focus) || q.id.equals(focus);
                int color=highlight?(from.id.equals(focus)?0xC8C800:0x00C8C8):done?0x64DC64:java.awt.Color.HSBtoRGB(visualId(from.id)/1000F,.2F,"locked".equals(state)?.3F:.8F)&0xFFFFFF;
                int alpha=highlight || done?255:180; if(!book().theme.edge.equals(DEFAULT_THEME.edge)) { int argb=QuestPresentation.color(book().theme.edge); color=argb&0xFFFFFF; alpha=argb>>>24; }
                int red=color>>16&255,green=color>>8&255,blue=color&255; double offset=highlight?-((System.currentTimeMillis()*.001D)%1D):0,u=length/(half*2);
                GL11.glPushMatrix();GL11.glTranslated(sx(q),sy(q),0);GL11.glRotatef((float)(Math.atan2(-dy,-dx)*180D/Math.PI),0,0,1);t.startDrawingQuads();
                t.setColorRGBA(red,green,blue,alpha);t.addVertexWithUV(0,-half,0,u+offset,0);t.addVertexWithUV(0,half,0,u+offset,1);
                t.setColorRGBA(red*3/4,green*3/4,blue*3/4,alpha);t.addVertexWithUV(length,half,0,offset,1);t.addVertexWithUV(length,-half,0,offset,0);t.draw();GL11.glPopMatrix();
            } } catch(IllegalArgumentException ignored) {}
        } finally { GL11.glPopAttrib(); GL11.glColor4f(1,1,1,1); }
    }
    private void drawChapters(int mx,int my) {
        chapterHits.clear(); int y=QuestClicks.available("","custom:ftbmoney:open_gui")?20:1,index=0;
        for(QuestCore.Chapter c:book().chapters) if(c.parent.isEmpty() && (ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))) {
            if(index++<chapterScroll) continue; if(y+18>height-20) break;
            chapterHits.add(new Hit(c.id,0,y,20,18)); boolean over=mx<20 && my>=y && my<y+18 && card==null;
            if(over) { hoverChapter=c.id; hoverY=y; }
            if(c.id.equals(chapter) || QuestPresentation.within(book(),chapter,c.id))drawRect(1,y,19,y+18,0x44000000);if(!c.id.equals(hoverChapter))chapterIcon(c,2,y+1); y+=18;
        }
        if(ClientProxy.snapshot.canEdit && y+18<height) { QuestClassicUi.icon("add",2,y+1,16,0xFFFFFFFF); chapterHits.add(new Hit("",0,y,20,18)); }
        if(card!=null || hoverChapter.isEmpty()) return;
        QuestCore.Chapter hovered=QuestPresentation.chapter(book(),hoverChapter); if(hovered==null) return;
        List<QuestCore.Chapter> expanded=new ArrayList<QuestCore.Chapter>(); expanded.add(hovered); for(QuestCore.Chapter c:book().chapters) if(c.parent.equals(hovered.id) && (ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))) expanded.add(c);
        int w=20;for(int i=0;i<expanded.size();i++)w=Math.max(w,fontRendererObj.getStringWidth(chapterTitle(expanded.get(i)))+28+(i==0?0:20));int top=Math.min(hoverY-1,height-expanded.size()*20-1);
        if(mx>=w || my<top || my>=top+expanded.size()*20){hoverChapter="";return;}
        for(int i=0;i<expanded.size();i++){
            QuestCore.Chapter c=expanded.get(i);int x=i==0?0:20,cy=top+i*20;boolean over=mx>=x && mx<w && my>=cy && my<cy+20;
            QuestClassicUi.background(mc,x,cy,w-x,20);if(over)drawRect(x,cy,w-1,cy+20,0x44000000);
            if(i==0)QuestClassicUi.frame(x,cy,w-x,20,QuestClassicUi.BORDER);else{drawRect(w-1,cy,w,cy+20,QuestClassicUi.BORDER);if(i==expanded.size()-1)drawRect(x,cy+19,w-1,cy+20,QuestClassicUi.BORDER);}
            chapterIcon(c,x+2,cy+2);fontRendererObj.drawStringWithShadow(chapterTitle(c),x+22,cy+6,over?0xFFFFFFA0:0xFFFFFFFF);chapterHits.add(new Hit(c.id,x,cy,w-x,20));
            if(over && !c.description.isEmpty()){List<String> tip=new ArrayList<String>();for(String line:c.description.split("\n"))tip.add("§7"+line);drawHoveringText(tip,mx,my,fontRendererObj);}
        }
    }
    private String chapterTitle(QuestCore.Chapter c){String title=c.title.isEmpty()?net.minecraft.util.StatCollector.translateToLocal("ftbquests.unnamed"):c.title;int sum=0,count=0;for(QuestCore.Quest quest:book().quests)if(QuestCore.required(quest) && QuestPresentation.within(book(),quest.chapter,c.id)){sum+=ClientProxy.snapshot.percentages.getOrDefault(quest.id,0);count++;}int percent=ClientProxy.snapshot.chapterPercentages.getOrDefault(c.id,count==0?0:sum/count);return title+(percent>0 && percent<100?" §2"+percent+"%":"");}
    private void chapterBaseIcon(QuestCore.Chapter chapter,int x,int y,int depth){
        if(depth>32)return;if(!chapter.icon.isEmpty()){QuestClassicUi.item(mc,chapter.icon,chapter.iconMeta,chapter.iconNbt,x,y,16);return;}
        List<Object> icons=new ArrayList<Object>();for(QuestCore.Quest quest:book().quests)if(quest.chapter.equals(chapter.id))icons.add(quest);for(QuestCore.Chapter child:book().chapters)if(child.parent.equals(chapter.id))icons.add(child);
        if(!icons.isEmpty()){Object icon=icons.get((int)(System.currentTimeMillis()/1000%icons.size()));if(icon instanceof QuestCore.Quest)QuestClassicUi.quest(mc,(QuestCore.Quest)icon,x,y,16);else chapterBaseIcon((QuestCore.Chapter)icon,x,y,depth+1);}
    }
    private void chapterIcon(QuestCore.Chapter c,int x,int y){
        chapterBaseIcon(c,x,y,0);boolean empty=true,children=false,unclaimed=false;for(QuestCore.Chapter child:book().chapters)if(child.parent.equals(c.id))children=true;
        for(QuestCore.Quest quest:book().quests)if(QuestPresentation.within(book(),quest.chapter,c.id)){empty=false;if("complete".equals(ClientProxy.snapshot.states.get(quest.id)))for(QuestCore.Reward reward:rewards(quest))if(!Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(quest.id+":"+reward.id)))unclaimed=true;}
        if(empty && !children){QuestClassicUi.icon("close",x+8,y+1,8,0xFFFFFFFF);return;}if(unclaimed)QuestClassicUi.icon("alert",x+11,y+1,6,0xFFFFFFFF);else if("complete".equals(ClientProxy.snapshot.chapterStates.get(c.id)))QuestClassicUi.icon("check",x+10,y,8,0xFFFFFFFF);
    }
    private List<QuestCore.Reward> rewards(QuestCore.Quest q) { List<QuestCore.Reward> result=new ArrayList<QuestCore.Reward>(); for(QuestCore.Reward r:q.rewards) if(ClientProxy.snapshot.canEdit || !(r.auto.equals("invisible") || r.auto.equals("inherit") && book().defaultAutoClaim.equals("invisible"))) result.add(r); return result; }
    private boolean hasGuide(QuestCore.Quest q) { return !q.pages.isEmpty() || !q.richPages.isEmpty() || !q.guidePage.isEmpty() || !q.uri.isEmpty(); }
    private List<String> description(QuestCore.Quest q) {
        List<String> lines=new ArrayList<String>(); int w=card==null?Math.max(180,Math.min(width-8,Math.max(201,Math.max(fontRendererObj.getStringWidth(QuestTaskLabels.quest(q))+50,q.minWidth))))-6:card.width-6;
        if(!q.subtitle.isEmpty()) lines.addAll(fontRendererObj.listFormattedStringToWidth("\u00a7o\u00a77"+q.subtitle,w));
        if(!q.description.isEmpty()) { if(!lines.isEmpty()) lines.add(""); lines.addAll(fontRendererObj.listFormattedStringToWidth(q.description,w)); } return lines;
    }
    private int textHeight(QuestCore.Quest q) {
        int w=Math.max(120,Math.min(width-2,Math.max(201,Math.max(fontRendererObj.getStringWidth(QuestTaskLabels.quest(q))+50,q.minWidth))|1))-6,total=0;
        if(!q.subtitle.isEmpty()) total+=fontRendererObj.listFormattedStringToWidth("\u00a7o\u00a77"+q.subtitle,w).size()*9-1;
        if(!q.description.isEmpty()){if(!q.subtitle.isEmpty())total+=7;total+=fontRendererObj.listFormattedStringToWidth(q.description,w).size()*9-1;}
        if(hasGuide(q)){if(!q.subtitle.isEmpty())total+=7;total+=13;}
        return total==0?0:total+1;
    }
    private void drawCard(int mx,int my) {
        QuestCore.Quest q=find(selected); if(q==null) return; List<QuestCore.Reward> rewards=rewards(q);
        drawRect(0,0,width,height,0x78212121); QuestClassicUi.context(mc,card.x,card.y,card.width,card.height);
        QuestClassicUi.quest(mc,q,card.x+2,card.y+2,12);
        String title=fontRendererObj.trimStringToWidth(QuestTaskLabels.quest(q),card.width-50); fontRendererObj.drawString(title,card.x+(card.width-fontRendererObj.getStringWidth(title))/2,card.y+4,0xFFAAAAAA);
        drawRect(card.x+1,card.y+15,card.x+card.width-1,card.y+16,QuestClassicUi.BORDER);
        int half=card.width/2; centered(QuestText.tr("Tasks"),card.x+half/2,card.y+20,0xFF5555FF); centered(QuestText.tr("Rewards"),card.x+half+half/2,card.y+20,0xFFFFAA00);
        int contentOffset=detailsScroll*9,divider=card.y+card.textTop-6-contentOffset;
        drawRect(card.x+half,card.y+16,card.x+half+1,Math.max(card.y+16,divider),QuestClassicUi.BORDER);
        if(divider>card.y+16) drawRect(card.x+1,divider,card.x+card.width-1,divider+1,QuestClassicUi.BORDER);
        clip(card.x+1,card.y+31,card.width-2,card.height-34);
        List<String> tip=null;
        for(int i=0;i<q.tasks.size();i++) {
            QuestCore.Task t=q.tasks.get(i); int x=card.cellX(false,i,q.tasks.size()),y=card.cellY(false,i)-contentOffset; boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(q.id+":"+t.id));
            cell(x,y,mx,my,!done && "available".equals(ClientProxy.snapshot.states.get(q.id))); String item=QuestClassicUi.taskItem(t);
            QuestClassicUi.task(mc,t,done,x+2,y+2,16);
            if(done) QuestClassicUi.icon("check",x+11,y+1,8,0xFFFFFFFF);
            else if(t.count>1 || t.consume) badge(Long.toString(t.count),x,y);
            if(overCell(x,y,mx,my)) { tip=new ArrayList<String>(); tip.add(QuestTaskLabels.title(t)); Long value=ClientProxy.snapshot.counters.get(q.id+":"+t.id); tip.add("\u00a77"+(done?t.count:value==null?0:value)+" / "+t.count); if(!t.description.isEmpty()) tip.addAll(fontRendererObj.listFormattedStringToWidth(t.description,220)); }
        }
        for(int i=0;i<rewards.size();i++) {
            QuestCore.Reward r=rewards.get(i); int x=card.cellX(true,i,rewards.size()),y=card.cellY(true,i)-contentOffset; boolean done=Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(q.id+":"+r.id));
            cell(x,y,mx,my,!done && "complete".equals(ClientProxy.snapshot.states.get(q.id))); QuestClassicUi.reward(mc,r,x+2,y+2,16);
            if(done) QuestClassicUi.icon("check",x+11,y+1,8,0xFFFFFFFF);
            else {
                if("complete".equals(ClientProxy.snapshot.states.get(q.id))) QuestClassicUi.icon("alert",x+11,y+1,8,0xFFFFFFFF);
                if(r.type.equals("item")) badge(Integer.toString(r.count)+(r.randomBonus>0?"-"+(r.count+r.randomBonus):""),x,y);
                else if(r.type.equals("xp") || r.type.equals("xp_levels")) badge("+"+r.count,x,y);
            }
            if(overCell(x,y,mx,my)) { tip=new ArrayList<String>(); tip.add(QuestTaskLabels.reward(r)); if(done) tip.add("\u00a77"+QuestText.tr("Claimed")); if(!r.description.isEmpty()) tip.addAll(fontRendererObj.listFormattedStringToWidth(r.description,220)); }
        }
        if(ClientProxy.snapshot.canEdit) {
            int tx=card.cellX(false,q.tasks.size(),q.tasks.size()+1),ty=card.cellY(false,q.tasks.size())-contentOffset;cell(tx,ty,mx,my,true);QuestClassicUi.icon("add",tx+2,ty+2,16,0xFFFFFFFF);
            int rx=card.cellX(true,rewards.size(),rewards.size()+1),ry=card.cellY(true,rewards.size())-contentOffset;cell(rx,ry,mx,my,true);QuestClassicUi.icon("add",rx+2,ry+2,16,0xFFFFFFFF);
        }
        if(!ClientProxy.snapshot.canEdit && q.tasks.isEmpty()) centered(QuestText.tr("No tasks"),card.x+half/2,card.y+36-contentOffset,0xFF999999);
        if(!ClientProxy.snapshot.canEdit && rewards.isEmpty()) centered(QuestText.tr("No rewards"),card.x+half+half/2,card.y+36-contentOffset,0xFF999999);
        String bodyColor=QuestPresentation.resolve(book(),q,ClientProxy.snapshot.states.get(q.id))[1]; int bodyText=bodyColor.equals(DEFAULT_THEME.text)?0xFFFFFFFF:QuestPresentation.color(bodyColor);
        int y=card.y+card.textTop-contentOffset;
        if(!q.subtitle.isEmpty()) {for(String line:(List<String>)fontRendererObj.listFormattedStringToWidth("\u00a7o\u00a77"+q.subtitle,card.width-6)){centered(line,card.x+card.width/2,y,0xFFAAAAAA);y+=9;}y--;}
        if(!q.description.isEmpty()){if(!q.subtitle.isEmpty())y+=7;for(String line:(List<String>)fontRendererObj.listFormattedStringToWidth(q.description,card.width-6)){fontRendererObj.drawString(line,card.x+3,y,bodyText);y+=9;}y--;}
        if(hasGuide(q)) {if(!q.subtitle.isEmpty())y+=7;String label=net.minecraft.util.StatCollector.translateToLocal("ftbquests.gui.open_in_guide");int gw=fontRendererObj.getStringWidth(label)+28,gx=card.x+(card.width-gw)/2;QuestClassicUi.item(mc,"minecraft:book",0,"",gx+2,y+2,8);fontRendererObj.drawStringWithShadow(label,gx+14,y+2,0xFFFFFFFF);}
        unclip(); super.drawScreen(mx,my,0);
        if(tip!=null && my>=card.y+31 && my<card.bodyBottom) drawHoveringText(tip,mx,my,fontRendererObj);
    }
    private void badge(String text,int x,int y) {
        GL11.glPushMatrix();try { GL11.glTranslatef(x+19-fontRendererObj.getStringWidth(text)/2F,y+15,500);GL11.glScalef(.5F,.5F,1);fontRendererObj.drawStringWithShadow(text,0,0,0xFFFFFFFF); } finally { GL11.glPopMatrix(); }
    }
    private void centered(String text,int x,int y,int color) { fontRendererObj.drawString(text,x-fontRendererObj.getStringWidth(text)/2,y,color); }
    private void cell(int x,int y,int mx,int my,boolean enabled) { if(overCell(x,y,mx,my)) { QuestClassicUi.frame(x,y,20,20,QuestClassicUi.BORDER); drawRect(x+2,y+2,x+18,y+18,enabled?0x44FFFFFF:0x44000000); } }
    private boolean overCell(int x,int y,int mx,int my) { return mx>=x && mx<x+20 && my>=y && my<y+20; }
    private void clip(int x,int y,int w,int h) { net.minecraft.client.gui.ScaledResolution s=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight); int f=s.getScaleFactor(); GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(x*f,(s.getScaledHeight()-y-h)*f,Math.max(0,w*f),Math.max(0,h*f)); }
    private void unclip() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
    @Override protected void actionPerformed(GuiButton b) {
        QuestNetwork.Action a=new QuestNetwork.Action(); a.id=selected;
        switch(b.id) {
            case 0: mc.displayGuiScreen(null); return;
            case 1: a.action="refresh"; break;
            case 2: List<QuestCore.Chapter> chapters=new ArrayList<QuestCore.Chapter>(); for(QuestCore.Chapter c:book().chapters) if(QuestPresentation.within(book(),c.id,group)) chapters.add(c); chapterIndex++; if(chapterIndex>=chapters.size()) chapterIndex=-1; chooseChapter(chapterIndex<0?"":chapters.get(chapterIndex).id); return;
            case 11: List<QuestCore.Chapter> groups=groups(); if(++groupIndex>=groups.size()) groupIndex=-1; group=groupIndex<0?"":groups.get(groupIndex).id; chooseChapter(""); return;
            case 3: settingsMenu(); return;
            case 4: a.action="check"; break;
            case 5: if(find(selected)==null) return; if(RewardChoiceScreen.needed(find(selected))) { mc.displayGuiScreen(new RewardChoiceScreen(this,find(selected))); return; } a.action="claim"; break;
            case 6: a.action="repeat"; break;
            case 7: if(find(selected)!=null) mc.displayGuiScreen(new QuestTaskScreen(this,find(selected))); return;
            case 8: if(find(selected)!=null) mc.displayGuiScreen(new QuestGuideScreen(this,find(selected))); return;
            case 9: mc.displayGuiScreen(new QuestTeamScreen(this)); return;
            case 10: mc.displayGuiScreen(new QuestEmergencyScreen(this));return;
            case 12: QuestGuides.open("",this); return;
            case 13: a.action="pin"; break;
            case 14:a.action="autopin";break;
            case 15: QuestClicks.open("","custom:ftbmoney:open_gui",this); return;
            case 16: selected=""; detailsScroll=0; refresh(); return;
            case 17: case 18: navigate(b.id==17); return;
            case 20:mc.displayGuiScreen(new QuestNotificationsScreen(this,true));a.action="claim_all";break;
            case 21: pendingLink="https://latvian.dev/supporting/";mc.displayGuiScreen(new GuiConfirmOpenLink(this,pendingLink,1000,false));return;
            case 22: showSearch=!showSearch; if(!showSearch) search.setText(""); menu=false; search.setFocused(showSearch); refresh(); return;
            default: return;
        }
        ClientProxy.send(a);
    }
    private String nativeText(String key) { return net.minecraft.util.StatCollector.translateToLocal("ftbquests."+key); }
    private void changeProgress(final String questId,final boolean complete) {
        mc.displayGuiScreen(new GuiYesNo((accepted,id)->{mc.displayGuiScreen(this);if(accepted){QuestNetwork.Action action=new QuestNetwork.Action();action.action=complete?"progress_complete":"progress_reset";action.id=questId;ClientProxy.send(action);}},nativeText(complete?"gui.complete_instantly_q":"gui.reset_progress_q"),"",0));
    }
    private void settingsMenu() {
        List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();
        entries.add(new QuestContextScreen.Entry(nativeText("gui.edit_file"),"theme:settings",()->QuestObjectConfigScreen.edit(this,"","","")));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.reset_progress"),"refresh",()->changeProgress("",false)));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.complete_instantly"),"check",()->changeProgress("",true)));
        entries.add(new QuestContextScreen.Entry(nativeText("reward_tables"),"money_bag",()->mc.displayGuiScreen(new QuestRewardTablesScreen(this))));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.save_as_file"),"download",()->exportBook(false)));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.export_spreadsheet"),"download",()->exportBook(true)));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.reload_theme"),"refresh",()->{mc.getTextureManager().onResourceManagerReload(mc.getResourceManager());refresh();}));
        entries.add(new QuestContextScreen.Entry(nativeText("gui.wiki"),"info",()->{pendingLink="https://faq.ftb.world/books/ftb-quests-112";mc.displayGuiScreen(new GuiConfirmOpenLink(this,pendingLink,1000,false));}));
        mc.displayGuiScreen(new QuestContextScreen(this,width-20,height-19,entries));
    }
    private void exportBook(boolean spreadsheet) {
        try {
            java.io.File directory=new java.io.File(mc.mcDataDir,"local/ftbquests/saved");directory.mkdirs();
            java.io.File destination=new java.io.File(directory,new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new java.util.Date())+(spreadsheet?".csv":".json"));
            StringBuilder content=new StringBuilder();
            if(spreadsheet){List<QuestCore.Chapter> chapters=book().chapters;int max=0;for(QuestCore.Chapter c:chapters){int n=0;for(QuestCore.Quest q:book().quests)if(q.chapter.equals(c.id))n++;max=Math.max(max,n);}for(int row=0;row<=max;row++){for(int column=0;column<chapters.size();column++){if(column>0)content.append(",,");QuestCore.Chapter c=chapters.get(column);String value=row==0?c.title:"";if(row>0){int n=0;for(QuestCore.Quest q:book().quests)if(q.chapter.equals(c.id) && ++n==row){value=QuestTaskLabels.quest(q);break;}}content.append('"').append(value.replace("\"","\"\"")).append("\",\"\"");}content.append('\n');}}
            else content.append(QuestService.JSON.toJson(book()));
            java.nio.file.Files.write(destination.toPath(),content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(net.minecraft.util.StatCollector.translateToLocalFormatted("ftbquests.gui.saved_as_file",destination.getPath())));
        }catch(java.io.IOException failure){mc.thePlayer.addChatMessage(dev.sigmarix.quests.QuestMessages.component(failure.getMessage()));}
    }
    private void chooseChapter(String id) { placingSelection=false;hideCard=false;selectedObjects.clear();chapter=id; selected=""; hoverChapter=""; detailsScroll=0; centerMap(); refresh(); }
    private List<QuestCore.Quest> dependants(QuestCore.Quest q) { List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>(); Map<String,QuestCore.Quest> all=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest other:book().quests) all.put(other.id,other); for(QuestCore.Quest other:book().quests) for(String ref:other.dependencies) try { if(QuestDependencies.owners(book(),all,ref,other.dependencyState.equals("started")).contains(q.id)) { result.add(other); break; } } catch(IllegalArgumentException ignored) {} return result; }
    private void navigate(boolean backwards) {
        QuestCore.Quest q=find(selected);if(q==null)return;List<QuestCore.Quest> candidates=dependants(q);
        if(backwards){candidates.clear();for(String ref:q.dependencies){QuestCore.Quest from=find(ref);if(from!=null)candidates.add(from);}}
        List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();
        for(final QuestCore.Quest target:candidates)entries.add(new QuestContextScreen.Entry(QuestTaskLabels.quest(target),"",()->{selected=target.id;chapter=target.chapter;detailsScroll=0;centerMap();refresh();}));
        if(!entries.isEmpty())mc.displayGuiScreen(new QuestContextScreen(this,backwards?card.x:card.x+card.width-13,card.y+30,entries));
    }
    @Override protected void mouseClicked(int x,int y,int mouseButton) {
        if(placingSelection){finishQuestMove(x,y,mouseButton==0);return;}
        if(movingImage!=null){finishImageMove(mouseButton==0);return;}

        for(Object raw:buttonList) { GuiButton b=(GuiButton)raw; if(b.visible && x>=b.xPosition && x<b.xPosition+b.width && y>=b.yPosition && y<b.yPosition+b.height) { super.mouseClicked(x,y,mouseButton); return; } }
        if(card!=null) {
            if(!card.contains(x,y)) { selected=""; detailsScroll=0; refresh(); return; }

            if(y<card.y+31 || y>=card.bodyBottom) return; QuestCore.Quest q=find(selected); if(q==null) return;
            if(mouseButton==1 && ClientProxy.snapshot.canEdit) {
                for(int i=0;i<q.tasks.size();i++)if(overCell(card.cellX(false,i,q.tasks.size()),card.cellY(false,i)-detailsScroll*9,x,y)){editObjectMenu(q,"tasks",q.tasks.get(i).id,x,y);return;}
                List<QuestCore.Reward> rr=rewards(q);for(int i=0;i<rr.size();i++)if(overCell(card.cellX(true,i,rr.size()),card.cellY(true,i)-detailsScroll*9,x,y)){editObjectMenu(q,"rewards",rr.get(i).id,x,y);return;}
            }
            if(mouseButton!=0)return;
            if(ClientProxy.snapshot.canEdit) {
                List<QuestCore.Reward> editableRewards=rewards(q);
                if(overCell(card.cellX(false,q.tasks.size(),q.tasks.size()+1),card.cellY(false,q.tasks.size())-detailsScroll*9,x,y)){QuestCreationMenus.task(this,q,x,y);return;}
                if(overCell(card.cellX(true,editableRewards.size(),editableRewards.size()+1),card.cellY(true,editableRewards.size())-detailsScroll*9,x,y)){QuestCreationMenus.questReward(this,q,x,y);return;}
            }
            for(int i=0;i<q.tasks.size();i++) if(overCell(card.cellX(false,i,q.tasks.size()),card.cellY(false,i)-detailsScroll*9,x,y)) { QuestCore.Task t=q.tasks.get(i); if(t.type.equals("item") && !isShiftKeyDown()) { mc.displayGuiScreen(new QuestItemTaskScreen(this,q,t)); return; } if(Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(q.id+":"+t.id)) && !"available".equals(ClientProxy.snapshot.states.get(q.id))) return; QuestNetwork.Action a=new QuestNetwork.Action(); a.id=q.id; a.text=t.id; a.action="click_task"; ClientProxy.send(a); return; }
            List<QuestCore.Reward> rs=rewards(q); for(int i=0;i<rs.size();i++) if(overCell(card.cellX(true,i,rs.size()),card.cellY(true,i)-detailsScroll*9,x,y)) { QuestCore.Reward r=rs.get(i); if(Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(q.id+":"+r.id)) || !"complete".equals(ClientProxy.snapshot.states.get(q.id))) return; if(r.type.equals("choice")) mc.displayGuiScreen(new RewardChoiceScreen(this,q,r)); else claim(q,r); return; }
            if(hasGuide(q) && y>=card.y+card.textTop+description(q).size()*9-detailsScroll*9) mc.displayGuiScreen(new QuestGuideScreen(this,q)); return;
        }
        if(showSearch) { search.mouseClicked(x,y,mouseButton); if(search.isFocused()) return; }
        for(int i=chapterHits.size()-1;i>=0;i--) if(chapterHits.get(i).contains(x,y)) { if(chapterHits.get(i).id.isEmpty())QuestObjectMenus.chapter(this,group);else if(mouseButton==1 && ClientProxy.snapshot.canEdit)QuestObjectMenus.chapterMenu(this,chapterHits.get(i).id,x,y);else chooseChapter(chapterHits.get(i).id); return; }
        if(mouseButton==1 && ClientProxy.snapshot.canEdit && x>=20 && x<width-20){for(QuestCore.Quest q:visible())if(hit(q,x,y)){if(!selectedObjects.isEmpty() && !selectedObjects.contains(q.id)){dependencyMenu(q,x,y);return;}questMenu(q,x,y);return;}List<QuestPresentation.MapImage> candidates=images();for(int i=candidates.size()-1;i>=0;i--)if(imageContains(candidates.get(i),x,y)){imageMenu(candidates.get(i),x,y);return;}QuestCreationMenus.at(this,chapter,(x-panX)/(mapUnit()*zoom),(y-panY)/(mapUnit()*zoom),x,y);return;}
        if(mouseButton==1 && !ClientProxy.snapshot.canEdit && x>=20 && x<width-20){for(QuestCore.Quest q:visible())if(hit(q,x,y)){if(selected.equals(q.id))selected="";else selected=q.id;hideCard=true;refresh();return;}}
        if(mouseButton==2 && ClientProxy.snapshot.canEdit && x>=20 && x<width-20){for(QuestCore.Quest quest:visible())if(hit(quest,x,y)){startQuestMove(quest,false);return;}}
        if(x<20 || x>=width-20 || mouseButton!=0) return;
        for(QuestCore.Quest q:visible()) if(hit(q,x,y)) { hideCard=false;if(ClientProxy.snapshot.canEdit && isCtrlKeyDown()){if(!selectedObjects.add(q.id))selectedObjects.remove(q.id);selected="";refresh();return;}selected=q.id; detailsScroll=0; if(ClientProxy.snapshot.canEdit && isShiftKeyDown()) { moving=true; movingId=q.id; selected=""; } else if(!q.customClick.isEmpty()) QuestClicks.open(q.id,q.customClick,this); lastX=x; lastY=y; refresh(); return; }
        List<QuestPresentation.MapImage> list=images(); for(int i=list.size()-1;i>=0;i--) { QuestPresentation.MapImage image=list.get(i); if((!image.click.isEmpty() || !image.uri.isEmpty()) && imageContains(image,x,y)) { if(!image.click.isEmpty()){QuestCore.Chapter c=QuestPresentation.chapter(book(),chapter);QuestClicks.open("image:"+chapter+":"+c.images.indexOf(image),image.click,this);return;}pendingLink=image.uri; mc.displayGuiScreen(new GuiConfirmOpenLink(this,image.uri,1000,false)); return; } }
        dragging=true; lastX=x; lastY=y;
    }
    private void startQuestMove(QuestCore.Quest quest,boolean single){if(single)selectedObjects.clear();selectedObjects.add(quest.id);selected="";hideCard=false;placingSelection=true;refresh();}
    private double[] selectionBounds(){double x=Double.POSITIVE_INFINITY,y=x;for(QuestCore.Quest quest:visible())if(selectedObjects.contains(quest.id)){x=Math.min(x,quest.x);y=Math.min(y,quest.y);}return new double[]{x,y};}
    private double[] pointerPosition(int x,int y){double px=(x-panX)/(mapUnit()*zoom),py=(y-panY)/(mapUnit()*zoom);if(!isShiftKeyDown()){double size=1;if(selectedObjects.size()==1){QuestCore.Quest quest=find(selectedObjects.iterator().next());if(quest!=null)size=quest.size;}px=QuestPresentation.snap(book(),px,size);py=QuestPresentation.snap(book(),py,size);}return new double[]{px,py};}
    private void finishQuestMove(int x,int y,boolean save){placingSelection=false;if(!save || x<20 || x>=width-20)return;double[] origin=selectionBounds(),destination=pointerPosition(x,y);if(!Double.isFinite(origin[0]))return;try{QuestObjectConfigScreen.send(QuestEditorActions.move(book(),selectedObjects,chapter,destination[0]-origin[0],destination[1]-origin[1]),ClientProxy.snapshot.revision);}catch(IllegalArgumentException error){ClientProxy.snapshot.message="Error: "+error.getMessage();}}
    private void drawEditorCursor(int mx,int my){if(!ClientProxy.snapshot.canEdit || mx<20 || mx>=width-20 || my<1 || my>=height-1)return;double[] point=pointerPosition(mx,my),origin=selectionBounds();
        if(placingSelection && Double.isFinite(origin[0])){for(QuestCore.Quest quest:visible())if(selectedObjects.contains(quest.id)){int x=(int)Math.round(panX+(point[0]+quest.x-origin[0])*mapUnit()*zoom),y=(int)Math.round(panY+(point[1]+quest.y-origin[1])*mapUnit()*zoom);QuestNodeRenderer.draw(mc,x,y,radius(quest),QuestPresentation.resolve(book(),quest,ClientProxy.snapshot.states.get(quest.id))[2],0x1EFFFFFF);}}
        else if(card==null){QuestCore.Chapter selected=QuestPresentation.chapter(book(),chapter);String shape=selected==null || selected.defaultQuestShape.isEmpty()?book().defaultQuestShape:selected.defaultQuestShape;int x=(int)Math.round(panX+point[0]*mapUnit()*zoom),y=(int)Math.round(panY+point[1]*mapUnit()*zoom);QuestNodeRenderer.draw(mc,x,y,(int)Math.round(12*zoom),shape,0x0AFFFFFF);if(editorGrid){GL11.glPushMatrix();GL11.glTranslated(0,0,1000);drawRect(x,y,x+1,y+1,0xFFFFFFFF);drawRect(20,y,width-20,y+1,0x1EFFFFFF);drawRect(x,1,x+1,height-1,0x1EFFFFFF);GL11.glPopMatrix();}}
        boolean unicode=fontRendererObj.getUnicodeFlag();fontRendererObj.setUnicodeFlag(true);GL11.glPushMatrix();try{GL11.glTranslated(0,0,1000);java.text.DecimalFormat format=new java.text.DecimalFormat("#0.00",java.text.DecimalFormatSymbols.getInstance(Locale.ROOT));fontRendererObj.drawStringWithShadow("X:"+(point[0]<0?"":" ")+format.format(point[0]),23,height-19,0xFFFFFFFF);fontRendererObj.drawStringWithShadow("Y:"+(point[1]<0?"":" ")+format.format(point[1]),23,height-11,0xFFFFFFFF);double cx=(width/2D-panX)/(mapUnit()*zoom),cy=(height/2D-panY)/(mapUnit()*zoom);fontRendererObj.drawStringWithShadow("CX:"+(cx<0?"":" ")+format.format(cx),width-50,height-19,0xFFFFFFFF);fontRendererObj.drawStringWithShadow("CY:"+(cy<0?"":" ")+format.format(cy),width-50,height-11,0xFFFFFFFF);}finally{GL11.glPopMatrix();fontRendererObj.setUnicodeFlag(unicode);}}
    private void questMenu(QuestCore.Quest quest,int x,int y){if(!selectedObjects.isEmpty()){List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();entries.add(new QuestContextScreen.Entry(nativeText("gui.add_reward_all"),"add",()->QuestCreationMenus.selectedRewards(this,new LinkedHashSet<String>(selectedObjects),x,y)));entries.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("selectServer.delete"),"remove",()->mc.displayGuiScreen(new GuiYesNo((yes,id)->{mc.displayGuiScreen(this);if(yes){QuestObjectConfigScreen.send(QuestEditorActions.delete(book(),selectedObjects),ClientProxy.snapshot.revision);selectedObjects.clear();}},net.minecraft.util.StatCollector.translateToLocalFormatted("delete_item",nativeText("quests")+" ["+selectedObjects.size()+"]"),"",0))));entries.add(new QuestContextScreen.Entry("","",null));for(String hint:Arrays.asList("Ctrl+A to select all quests","Ctrl+D to deselect all quests","Ctrl+Arrow Key to move selected quests"))entries.add(new QuestContextScreen.Entry("§7"+hint,"lib:info",null));mc.displayGuiScreen(new QuestContextScreen(this,x,y,entries));return;}
        List<QuestContextScreen.Entry> before=new ArrayList<QuestContextScreen.Entry>();before.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("gui.move"),"up",()->startQuestMove(quest,true)));before.add(new QuestContextScreen.Entry(nativeText("gui.edit_text"),"lib:info",()->QuestTextEditorFrame.open(quest)));before.add(new QuestContextScreen.Entry("","",null));QuestObjectMenus.open(this,quest.id,"","",x,y,before);
    }
    private void dependencyMenu(QuestCore.Quest target,int x,int y){List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();String[] keys={"add_dependencies","remove_dependencies","add_dependencies_self","remove_dependencies_self"};for(int i=0;i<4;i++){final boolean reverse=i>=2,add=i%2==0;entries.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("ftbquests.gui."+keys[i]),add?"add":"remove",()->{try{QuestObjectConfigScreen.send(QuestEditorActions.dependencies(book(),selectedObjects,target.id,reverse,add),ClientProxy.snapshot.revision);}catch(IllegalArgumentException error){ClientProxy.snapshot.message="Error: "+error.getMessage();}}));}mc.displayGuiScreen(new QuestContextScreen(this,x,y,entries));}
    private boolean editorKey(char character,int key){
        if(card!=null || showSearch && search.isFocused())return false;
        if(key==Keyboard.KEY_R && !isCtrlKeyDown()){editorGrid=!editorGrid;return true;}
        if(key==Keyboard.KEY_SPACE){centerMap();return true;}
        if(key==Keyboard.KEY_TAB){List<QuestCore.Chapter> chapters=new ArrayList<QuestCore.Chapter>();for(QuestCore.Chapter c:book().chapters)if(ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))chapters.add(c);if(!chapters.isEmpty()){int index=0;for(int i=0;i<chapters.size();i++)if(chapters.get(i).id.equals(chapter))index=i;chooseChapter(chapters.get((index+(isShiftKeyDown()?chapters.size()-1:1))%chapters.size()).id);}return true;}
        if(character>='1' && character<='9'){int index=character-'1';if(index<book().chapters.size()){QuestCore.Chapter c=book().chapters.get(index);if(ClientProxy.snapshot.canEdit || QuestPresentation.visibleChapter(book(),c.id))chooseChapter(c.id);}return true;}
        if(!ClientProxy.snapshot.canEdit || !isCtrlKeyDown())return false;
        if(key==Keyboard.KEY_A){for(QuestCore.Quest q:visible())selectedObjects.add(q.id);return true;}
        if(key==Keyboard.KEY_D){selectedObjects.clear();return true;}
        double step=isShiftKeyDown()?.1:.5,dx=key==Keyboard.KEY_LEFT?-step:key==Keyboard.KEY_RIGHT?step:0,dy=key==Keyboard.KEY_UP?-step:key==Keyboard.KEY_DOWN?step:0;
        if(dx!=0 || dy!=0){if(!selectedObjects.isEmpty())try{QuestObjectConfigScreen.send(QuestEditorActions.move(book(),selectedObjects,chapter,dx,dy),ClientProxy.snapshot.revision);}catch(IllegalArgumentException error){ClientProxy.snapshot.message="Error: "+error.getMessage();}return true;}return false;
    }
    private void imageMenu(QuestPresentation.MapImage image,int x,int y){QuestCore.Chapter c=QuestPresentation.chapter(book(),chapter);final int index=c.images.indexOf(image);List<QuestContextScreen.Entry> entries=new ArrayList<QuestContextScreen.Entry>();entries.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("selectServer.edit"),"settings",()->QuestObjectMenus.image(this,chapter,index)));entries.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("gui.move"),"up",()->{movingImage=image;movingImageIndex=index;imageStartX=image.x;imageStartY=image.y;}));entries.add(new QuestContextScreen.Entry(net.minecraft.util.StatCollector.translateToLocal("selectServer.delete"),"remove",()->QuestObjectMenus.deleteImage(this,chapter,index)));mc.displayGuiScreen(new QuestContextScreen(this,x,y,entries));}
    private void finishImageMove(boolean save){QuestPresentation.MapImage image=movingImage;movingImage=null;if(save){QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(book()),QuestCore.Book.class);QuestCore.Chapter c=QuestPresentation.chapter(draft,chapter);if(c!=null && movingImageIndex>=0 && movingImageIndex<c.images.size()){QuestPresentation.MapImage changed=c.images.get(movingImageIndex);changed.x=QuestPresentation.snap(draft,image.x,image.width);changed.y=QuestPresentation.snap(draft,image.y,image.height);QuestObjectConfigScreen.send(draft,ClientProxy.snapshot.revision);}}image.x=imageStartX;image.y=imageStartY;}
    private void editObjectMenu(final QuestCore.Quest quest,final String collection,final String objectId,int x,int y) {QuestObjectMenus.open(this,quest.id,collection,objectId,x,y);}
    private void claim(QuestCore.Quest q,QuestCore.Reward r) { QuestNetwork.Action a=new QuestNetwork.Action(); a.action="claim_reward"; a.id=q.id; a.text=r.id; ClientProxy.send(a); }
    private List<QuestCore.Chapter> groups() { Set<String> parents=new HashSet<String>(); for(QuestCore.Chapter c:book().chapters) if(!c.parent.isEmpty()) parents.add(c.parent); List<QuestCore.Chapter> result=new ArrayList<QuestCore.Chapter>(); for(QuestCore.Chapter c:book().chapters) if(parents.contains(c.id)) result.add(c); return result; }
    private List<QuestPresentation.MapImage> images() { QuestCore.Chapter c=QuestPresentation.chapter(book(),chapter); List<QuestPresentation.MapImage> result=new ArrayList<QuestPresentation.MapImage>(); if(c!=null) for(QuestPresentation.MapImage i:c.images) if(!i.dev || ClientProxy.snapshot.canEdit) result.add(i); return result; }
    private int imageLeft(QuestPresentation.MapImage image){double unit=mapUnit()*zoom,scroll=20-panX-layoutMinX*unit;return 20+(int)-scroll+(int)((image.x-layoutMinX)*unit-12*zoom*image.width);}
    private int imageTop(QuestPresentation.MapImage image){double unit=mapUnit()*zoom,scroll=1-panY-layoutMinY*unit;return 1+(int)-scroll+(int)((image.y-layoutMinY)*unit-12*zoom*image.height);}
    private double imageX(QuestPresentation.MapImage image) { return (int)(imageLeft(image)+(int)(image.width*24*zoom)/2D); }
    private double imageY(QuestPresentation.MapImage image) { return (int)(imageTop(image)+(int)(image.height*24*zoom)/2D); }
    private boolean imageContains(QuestPresentation.MapImage image,int x,int y) {if(image.click.isEmpty() && image.uri.isEmpty() && !ClientProxy.snapshot.canEdit)return false;int left=imageLeft(image),top=imageTop(image),w=(int)(image.width*24*zoom),h=(int)(image.height*24*zoom);return x>=left && y>=top && x<left+w && y<top+h;}
    @Override public void confirmClicked(boolean accepted,int id) { if(accepted && id==1000 && !pendingLink.isEmpty() && QuestPresentation.link(pendingLink)) try { java.awt.Desktop.getDesktop().browse(new java.net.URI(pendingLink)); } catch(Exception ex) { cpw.mods.fml.common.FMLLog.warning("Could not open map link: %s",ex.toString()); } mc.displayGuiScreen(this); }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed) { if(dragging) { panX+=x-lastX; panY+=y-lastY; } if(moving) { QuestCore.Quest q=find(movingId); if(q!=null) { q.x+=(x-lastX)/(mapUnit()*zoom); q.y+=(y-lastY)/(mapUnit()*zoom); } } lastX=x; lastY=y; }
    @Override protected void mouseMovedOrUp(int x,int y,int button) { super.mouseMovedOrUp(x,y,button); dragging=false; if(moving) { moving=false; QuestCore.Quest q=find(movingId); if(q!=null) { q.x=QuestPresentation.snap(book(),q.x,q.size); q.y=QuestPresentation.snap(book(),q.y,q.size); } QuestNetwork.Action a=new QuestNetwork.Action(); a.action="save"; a.revision=ClientProxy.snapshot.revision; a.book=book(); ClientProxy.send(a); } }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel==0) return; int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1; if(card!=null) { int total=card.textTop+description(find(selected)).size()*9+(hasGuide(find(selected))?22:0)+7; int max=Math.max(0,(total-card.height+8)/9); detailsScroll=Math.max(0,Math.min(max,detailsScroll+(wheel>0?-2:2))); } else if(x<20) chapterScroll=Math.max(0,Math.min(Math.max(0,book().chapters.size()-1),chapterScroll+(wheel>0?-1:1))); else { double old=zoom; zoom=Math.max(.25,Math.min(1.75,zoom+(wheel>0?.25:-.25))); panX=width/2D-(width/2D-panX)*zoom/old; panY=height/2D-(height/2D-panY)*zoom/old; } }
    private void objectSearch(){QuestSelectObjectScreen picker=new QuestSelectObjectScreen(this,QuestSelectObjectScreen.CHAPTER|QuestSelectObjectScreen.QUEST,false,s->true,s->{if(s.quest!=null){zoom=1.25;chooseChapter(s.quest.chapter);selected=s.quest.id;panX=width/2D-(s.quest.x+.5)*mapUnit()*zoom;panY=height/2D-(s.quest.y+.5)*mapUnit()*zoom;refresh();}else if(s.chapter!=null)chooseChapter(s.chapter.id);}).search(net.minecraft.util.StatCollector.translateToLocal("gui.search_box"));mc.displayGuiScreen(picker);picker.focusSearch();}
    @Override protected void keyTyped(char c,int key) { if(key==Keyboard.KEY_ESCAPE && placingSelection){placingSelection=false;return;}if(editorKey(c,key))return;if(key==Keyboard.KEY_LSHIFT || key==Keyboard.KEY_RSHIFT){long now=System.currentTimeMillis();if(now-lastShiftPress<=400){lastShiftPress=0;objectSearch();return;}lastShiftPress=now;}if(key==Keyboard.KEY_ESCAPE && movingImage!=null){finishImageMove(false);return;}if(key==Keyboard.KEY_ESCAPE && !selected.isEmpty()) { selected=""; detailsScroll=0; refresh(); return; } if(key==Keyboard.KEY_ESCAPE && (showSearch || menu)) { search.setText(""); showSearch=false; menu=false; refresh(); return; } if(isCtrlKeyDown() && key==Keyboard.KEY_T && find(selected)!=null){mc.displayGuiScreen(new QuestTaskScreen(this,find(selected)));return;} if(isCtrlKeyDown() && key==Keyboard.KEY_R && find(selected)!=null){QuestNetwork.Action action=new QuestNetwork.Action();action.action="repeat";action.id=selected;ClientProxy.send(action);return;} if(key==Keyboard.KEY_M && isCtrlKeyDown()){menu=!menu;refresh();return;} if(key==Keyboard.KEY_F && isCtrlKeyDown()) { showSearch=true; search.setFocused(true); return; } if(showSearch && search.textboxKeyTyped(c,key)) return; super.keyTyped(c,key); }
    @Override public void updateScreen() { if(search!=null) search.updateCursorCounter(); if(++refreshTicks%40==0 && !dragging && !moving && !placingSelection && movingImage==null) ClientProxy.send(new QuestNetwork.Action()); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
