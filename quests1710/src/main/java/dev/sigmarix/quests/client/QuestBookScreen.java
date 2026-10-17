package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class QuestBookScreen extends GuiScreen {
    private GuiTextField search;
    private String selected="", chapter="";
    private double panX=40,panY=35,zoom=1;
    private int lastX,lastY,detailsScroll,chapterIndex=-1;
    private boolean dragging,moving;
    private int refreshTicks;
    private String movingId="";
    private int split() { return Math.max(180,width*3/5); }
    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0,width-65,8,57,20,QuestText.tr("Close")));
        buttonList.add(new GuiButton(1,8,8,70,20,QuestText.tr("Refresh")));
        buttonList.add(new GuiButton(2,84,8,110,20,QuestText.tr("Chapter")));
        if(ClientProxy.snapshot.canEdit) buttonList.add(new GuiButton(3,200,8,70,20,QuestText.tr("Edit book")));
        buttonList.add(new GuiButton(9,276,8,66,20,QuestText.tr("Teams")));
        int buttonWidth=Math.max(32,(width-split()-24)/3);
        buttonList.add(new GuiButton(8,split()+6,height-78,Math.max(60,width-split()-12),18,QuestText.tr("Guide / link")));
        buttonList.add(new GuiButton(4,split()+6,height-54,buttonWidth,20,QuestText.tr("Check")));
        buttonList.add(new GuiButton(5,split()+10+buttonWidth,height-54,buttonWidth,20,QuestText.tr("Claim")));
        buttonList.add(new GuiButton(6,split()+14+buttonWidth*2,height-54,buttonWidth,20,QuestText.tr("Repeat")));
        buttonList.add(new GuiButton(7,split()+6,height-30,Math.max(60,width-split()-12),18,QuestText.tr("Tasks / start")));
        buttonList.add(new GuiButton(10,split()-84,35,76,20,QuestText.tr("Help items")));
        search=new GuiTextField(fontRendererObj,8,36,Math.min(240,split()-104),18); search.setMaxStringLength(128);
        refresh();
    }
    public void refresh() {
        if(ClientProxy.snapshot==null) return;
        for(Object object:buttonList) {
            GuiButton b=(GuiButton)object;
            if(b.id==2) b.displayString=chapter.isEmpty()?QuestText.tr("All chapters"):chapter+" ["+ClientProxy.snapshot.chapterStates.get(chapter)+"]";
            String state=ClientProxy.snapshot.states.get(selected);
            if(b.id==10) b.enabled=!ClientProxy.snapshot.book.emergencyItems.isEmpty() && ClientProxy.snapshot.emergencyWait==0;
            if(b.id==8) { QuestCore.Quest q=find(selected); b.enabled=q!=null && (!q.pages.isEmpty() || !q.richPages.isEmpty() || !q.uri.isEmpty()); }
            if(b.id==4) b.enabled="available".equals(state);
            if(b.id==5) b.enabled="complete".equals(state);
            if(b.id==6) { QuestCore.Quest q=find(selected); b.enabled="claimed".equals(state) && q!=null && q.repeatable; }
        }
    }
    private QuestCore.Quest find(String id) { for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) if(q.id.equals(id)) return q; return null; }
    private List<QuestCore.Quest> visible() {
        List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>();
        String filter=search.getText().toLowerCase(Locale.ROOT);
        for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) if((chapter.isEmpty() || chapter.equals(q.chapter)) && (q.title.toLowerCase(Locale.ROOT).contains(filter) || q.id.toLowerCase(Locale.ROOT).contains(filter) || q.tags.toString().toLowerCase(Locale.ROOT).contains(filter))) result.add(q);
        return result;
    }
    private double qx(QuestCore.Quest q) { return q.x*48; }
    private double qy(QuestCore.Quest q) { return q.y*48; }
    private int sx(QuestCore.Quest q) { return (int)(panX+qx(q)*zoom); }
    private int sy(QuestCore.Quest q) { return 66+(int)(panY+qy(q)*zoom); }
    @Override public void drawScreen(int mouseX,int mouseY,float partial) {
        QuestPresentation.Theme theme=ClientProxy.snapshot.book.theme;
        drawDefaultBackground(); drawRect(0,60,split(),height-8,QuestPresentation.color(theme.background)); drawRect(split()+1,60,width,height,QuestPresentation.color(theme.panel));
        search.drawTextBox();
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.book.title,width-split()-16),split()+8,37,0xFFFFFF);
        List<QuestCore.Quest> list=visible();
        // Scissor map nodes and edges so panning never overwrites controls.
        net.minecraft.client.gui.ScaledResolution scaled=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight);
        int factor=scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(0,8*factor,split()*factor,Math.max(0,height-68)*factor);
        for(QuestCore.Chapter c:ClientProxy.snapshot.book.chapters) if(c.id.equals(chapter) && !c.image.isEmpty()) { mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation(c.image)); GL11.glColor4f(1,1,1,.5f); func_152125_a(0,60,0,0,256,256,split(),height-68,256,256); GL11.glColor4f(1,1,1,1); }
        GL11.glDisable(GL11.GL_TEXTURE_2D); int edge=QuestPresentation.color(theme.edge); GL11.glColor4f((edge>>16&255)/255f,(edge>>8&255)/255f,(edge&255)/255f,(edge>>>24)/255f); GL11.glBegin(GL11.GL_LINES);
        Set<String> ids=new HashSet<String>(); for(QuestCore.Quest q:list) ids.add(q.id);
        Map<String,QuestCore.Quest> byId=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) byId.put(q.id,q);
        for(QuestCore.Quest q:list) if(!q.hideDependencyLines) for(String dep:q.dependencies) {
            try { for(String owner:QuestDependencies.owners(ClientProxy.snapshot.book,byId,dep,q.dependencyState.equals("started"))) { QuestCore.Quest d=find(owner); if(d!=null && ids.contains(owner)) { GL11.glVertex2d(sx(d)+12,sy(d)+12); GL11.glVertex2d(sx(q)+12,sy(q)+12); } } }
            catch(IllegalArgumentException ignored) { /* Hidden source quests are absent from a public snapshot. */ }
        }
        GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1,1,1,1);
        QuestCore.Quest hover=null;
        for(QuestCore.Quest q:list) {
            int x=sx(q),y=sy(q); String state=ClientProxy.snapshot.states.get(q.id);
            String[] style=QuestPresentation.resolve(ClientProxy.snapshot.book,q,state);
            int color=QuestPresentation.color(style[0]);
            if(q.id.equals(selected)) node(x+12,y+12,(int)(13*q.size)+3,style[2],QuestPresentation.color(theme.selected));
            node(x+12,y+12,Math.max(1,(int)(13*q.size)),style[2],color);
            try { Item item=(Item)Item.itemRegistry.getObject(q.icon); if(item!=null) { RenderHelper.enableGUIStandardItemLighting(); ItemStack icon=new ItemStack(item,1,q.iconMeta); icon.setTagCompound(QuestService.tag(q.iconNbt)); GL11.glPushMatrix(); GL11.glTranslated(x+12,y+12,0); GL11.glScaled(q.size,q.size,1); itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),icon,-8,-8); GL11.glPopMatrix(); RenderHelper.disableStandardItemLighting(); } } catch(RuntimeException ignored) {}
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(q.title,100),x-12,y+16+(int)(13*q.size),QuestPresentation.color(style[1]));
            if(Math.abs(mouseX-x-12)<=13*q.size+2 && Math.abs(mouseY-y-12)<=13*q.size+2) hover=q;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawDetails();
        String message=ClientProxy.snapshot.message;
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(message,width-16),8,height-10,0xFFDD77);
        super.drawScreen(mouseX,mouseY,partial);
        if(hover!=null && mouseX<split() && mouseY>60 && mouseY<height-8) func_146283_a(Arrays.asList(hover.title,hover.id+" - "+ClientProxy.snapshot.states.get(hover.id)),mouseX,mouseY);
    }
    private void node(int x,int y,int radius,String shape,int color) {
        if(shape.equals("square")) { drawRect(x-radius,y-radius,x+radius,y+radius,color); return; }
        int count=shape.equals("circle")?32:shape.equals("diamond")?4:shape.equals("pentagon")?5:shape.equals("hexagon")?6:8;
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f); GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2d(x,y);
        for(int i=0;i<=count;i++) { double angle=-Math.PI/2+Math.PI*2*i/count; GL11.glVertex2d(x+Math.cos(angle)*radius,y+Math.sin(angle)*radius); }
        GL11.glEnd(); GL11.glColor4f(1,1,1,1); GL11.glDisable(GL11.GL_BLEND); GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
    private void drawDetails() {
        QuestCore.Quest q=find(selected); int x=split()+8,max=width-split()-16;
        if(q==null) { fontRendererObj.drawSplitString(QuestText.tr("Select a quest. Drag the map; scroll to zoom. Operators: Shift+drag moves nodes. Use /quests team for teams."),x,76,max,0xDDDDDD); return; }
        List<String> lines=new ArrayList<String>();
        lines.add(q.title); lines.add(q.id+" ["+ClientProxy.snapshot.states.get(q.id)+"]"); lines.add("Team: "+ClientProxy.snapshot.team);
        lines.addAll(fontRendererObj.listFormattedStringToWidth(q.description,max));
        lines.add("Dependencies ("+q.dependencyState+"): "+q.dependencies); lines.add("Tasks:");
        for(QuestCore.Task t:q.tasks) {
            Long value=ClientProxy.snapshot.counters.get(q.id+":"+t.id);
            String line=(Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(q.id+":"+t.id))?"[done] ":"")+t.id+" "+t.type+" "+(t.target==null?"":t.target)+" x"+t.count+(t.consume?" (consume)":"")+" ["+(value==null?0:value)+"]";
            if(t.type.equals("dimension") || t.type.equals("location")) line+=" dim="+t.dimension;
            lines.addAll(fontRendererObj.listFormattedStringToWidth(line,max));
        }
        lines.add("Rewards:"); for(QuestCore.Reward r:q.rewards) if(!r.auto.equals("invisible")) rewardLines(lines,r,"",max);
        int end=Math.max(1,(height-172)/11); detailsScroll=Math.max(0,Math.min(detailsScroll,Math.max(0,lines.size()-end)));
        for(int i=detailsScroll;i<lines.size() && i<detailsScroll+end;i++) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(lines.get(i),max),x,74+(i-detailsScroll)*11,QuestPresentation.color(ClientProxy.snapshot.book.theme.text));
    }
    private void rewardLines(List<String> lines,QuestCore.Reward r,String prefix,int max) {
        lines.addAll(fontRendererObj.listFormattedStringToWidth(prefix+r.type+" "+(r.item!=null?r.item:r.target==null?"":r.target)+" x"+r.count,max));
        for(int i=0;i<r.options.size();i++) rewardLines(lines,r.options.get(i),prefix+i+": ",max);
    }
    @Override protected void actionPerformed(GuiButton button) {
        QuestNetwork.Action action=new QuestNetwork.Action(); action.id=selected;
        switch(button.id) {
            case 10: action.action="emergency"; ClientProxy.send(action); return;
            case 9: mc.displayGuiScreen(new QuestTeamScreen(this)); return;
            case 0: mc.displayGuiScreen(null); return;
            case 1: ClientProxy.send(action); return;
            case 2:
                chapterIndex++; if(chapterIndex>=ClientProxy.snapshot.book.chapters.size()) chapterIndex=-1;
                chapter=chapterIndex<0?"":ClientProxy.snapshot.book.chapters.get(chapterIndex).id; refresh(); return;
            case 3: mc.displayGuiScreen(new QuestObjectScreen(this)); return;
            case 8: if(find(selected)!=null) mc.displayGuiScreen(new QuestGuideScreen(this,find(selected))); return;
            case 4: action.action="check"; break;
            case 5: if(RewardChoiceScreen.needed(find(selected))) { mc.displayGuiScreen(new RewardChoiceScreen(this,find(selected))); return; } action.action="claim"; break;
            case 6: action.action="repeat"; break;
            case 7: if(find(selected)!=null) mc.displayGuiScreen(new QuestTaskScreen(this,find(selected))); return;
            default: return;
        }
        ClientProxy.send(action);
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        super.mouseClicked(x,y,button); search.mouseClicked(x,y,button);
        if(x>=split() || y<60 || y>height-8 || button!=0) return;
        for(QuestCore.Quest q:visible()) if(Math.abs(x-sx(q)-12)<=13*q.size+2 && Math.abs(y-sy(q)-12)<=13*q.size+2) {
            selected=q.id; detailsScroll=0; refresh();
            if(ClientProxy.snapshot.canEdit && isShiftKeyDown()) { moving=true; movingId=q.id; }
            lastX=x; lastY=y; return;
        }
        dragging=true; lastX=x; lastY=y;
    }
    @Override protected void mouseClickMove(int x,int y,int button,long time) {
        if(dragging) { panX+=x-lastX; panY+=y-lastY; }
        if(moving) { QuestCore.Quest q=find(movingId); if(q!=null) { q.x+=(x-lastX)/(48*zoom); q.y+=(y-lastY)/(48*zoom); } }
        lastX=x; lastY=y;
    }
    @Override protected void mouseMovedOrUp(int x,int y,int button) {
        super.mouseMovedOrUp(x,y,button); dragging=false;
        if(moving) { moving=false; QuestNetwork.Action action=new QuestNetwork.Action(); action.action="save"; action.revision=ClientProxy.snapshot.revision; action.book=ClientProxy.snapshot.book; ClientProxy.send(action); }
    }
    @Override public void handleMouseInput() {
        super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel==0) return;
        int x=Mouse.getEventX()*width/mc.displayWidth;
        if(x>=split()) detailsScroll+=wheel>0?-2:2;
        else zoom=Math.max(.25,Math.min(3,zoom*(wheel>0?1.1:1/1.1)));
    }
    @Override protected void keyTyped(char character,int key) { if(!search.textboxKeyTyped(character,key)) super.keyTyped(character,key); }
    @Override public void updateScreen() { search.updateCursorCounter(); if(++refreshTicks%40==0 && !dragging && !moving) ClientProxy.send(new QuestNetwork.Action()); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
