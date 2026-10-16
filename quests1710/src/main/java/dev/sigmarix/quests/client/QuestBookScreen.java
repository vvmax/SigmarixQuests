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
        buttonList.add(new GuiButton(0,width-65,8,57,20,"Close"));
        buttonList.add(new GuiButton(1,8,8,70,20,"Refresh"));
        buttonList.add(new GuiButton(2,84,8,110,20,"Chapter"));
        if(ClientProxy.snapshot.canEdit) buttonList.add(new GuiButton(3,200,8,70,20,"Edit book"));
        int buttonWidth=Math.max(32,(width-split()-24)/3);
        buttonList.add(new GuiButton(4,split()+6,height-54,buttonWidth,20,"Check"));
        buttonList.add(new GuiButton(5,split()+10+buttonWidth,height-54,buttonWidth,20,"Claim"));
        buttonList.add(new GuiButton(6,split()+14+buttonWidth*2,height-54,buttonWidth,20,"Repeat"));
        buttonList.add(new GuiButton(7,split()+6,height-30,Math.max(60,width-split()-12),18,"Tasks / start"));
        search=new GuiTextField(fontRendererObj,8,36,Math.min(240,split()-16),18); search.setMaxStringLength(128);
        refresh();
    }
    public void refresh() {
        if(ClientProxy.snapshot==null) return;
        for(Object object:buttonList) {
            GuiButton b=(GuiButton)object;
            if(b.id==2) b.displayString=chapter.isEmpty()?"All chapters":chapter+" ["+ClientProxy.snapshot.chapterStates.get(chapter)+"]";
            String state=ClientProxy.snapshot.states.get(selected);
            if(b.id==4) b.enabled="available".equals(state);
            if(b.id==5) b.enabled="complete".equals(state);
            if(b.id==6) { QuestCore.Quest q=find(selected); b.enabled="claimed".equals(state) && q!=null && q.repeatable; }
        }
    }
    private QuestCore.Quest find(String id) { for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) if(q.id.equals(id)) return q; return null; }
    private List<QuestCore.Quest> visible() {
        List<QuestCore.Quest> result=new ArrayList<QuestCore.Quest>();
        String filter=search.getText().toLowerCase(Locale.ROOT);
        for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) if((chapter.isEmpty() || chapter.equals(q.chapter)) && (q.title.toLowerCase(Locale.ROOT).contains(filter) || q.id.toLowerCase(Locale.ROOT).contains(filter))) result.add(q);
        return result;
    }
    private double qx(QuestCore.Quest q) { return q.x*48; }
    private double qy(QuestCore.Quest q) { return q.y*48; }
    private int sx(QuestCore.Quest q) { return (int)(panX+qx(q)*zoom); }
    private int sy(QuestCore.Quest q) { return 66+(int)(panY+qy(q)*zoom); }
    @Override public void drawScreen(int mouseX,int mouseY,float partial) {
        drawDefaultBackground(); drawRect(0,60,split(),height-8,0xEE17202A); drawRect(split()+1,60,width,height,0xEE26313D);
        search.drawTextBox();
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.book.title,width-split()-16),split()+8,37,0xFFFFFF);
        List<QuestCore.Quest> list=visible();
        // Scissor map nodes and edges so panning never overwrites controls.
        net.minecraft.client.gui.ScaledResolution scaled=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight);
        int factor=scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(0,8*factor,split()*factor,Math.max(0,height-68)*factor);
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glColor4f(.45f,.55f,.65f,1); GL11.glBegin(GL11.GL_LINES);
        Set<String> ids=new HashSet<String>(); for(QuestCore.Quest q:list) ids.add(q.id);
        Map<String,QuestCore.Quest> byId=new LinkedHashMap<String,QuestCore.Quest>(); for(QuestCore.Quest q:ClientProxy.snapshot.book.quests) byId.put(q.id,q);
        for(QuestCore.Quest q:list) for(String dep:q.dependencies) {
            try { for(String owner:QuestDependencies.owners(ClientProxy.snapshot.book,byId,dep,q.dependencyState.equals("started"))) { QuestCore.Quest d=find(owner); if(d!=null && ids.contains(owner)) { GL11.glVertex2d(sx(d)+12,sy(d)+12); GL11.glVertex2d(sx(q)+12,sy(q)+12); } } }
            catch(IllegalArgumentException ignored) { /* Hidden source quests are absent from a public snapshot. */ }
        }
        GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1,1,1,1);
        QuestCore.Quest hover=null;
        for(QuestCore.Quest q:list) {
            int x=sx(q),y=sy(q); String state=ClientProxy.snapshot.states.get(q.id);
            int color="claimed".equals(state)?0xFF3A784E:"complete".equals(state)?0xFFB28B29:"available".equals(state)?0xFF397CB2:0xFF50545A;
            if(q.id.equals(selected)) drawRect(x-3,y-3,x+27,y+27,0xFFFFFFFF);
            drawRect(x-1,y-1,x+25,y+25,color);
            try { Item item=(Item)Item.itemRegistry.getObject(q.icon); if(item!=null) { RenderHelper.enableGUIStandardItemLighting(); itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),new ItemStack(item),x+4,y+4); RenderHelper.disableStandardItemLighting(); } } catch(RuntimeException ignored) {}
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(q.title,100),x-12,y+29,0xEEEEEE);
            if(mouseX>=x-2 && mouseX<=x+26 && mouseY>=y-2 && mouseY<=y+26) hover=q;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawDetails();
        String message=ClientProxy.snapshot.message;
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(message,width-16),8,height-10,0xFFDD77);
        super.drawScreen(mouseX,mouseY,partial);
        if(hover!=null && mouseX<split() && mouseY>60 && mouseY<height-8) func_146283_a(Arrays.asList(hover.title,hover.id+" - "+ClientProxy.snapshot.states.get(hover.id)),mouseX,mouseY);
    }
    private void drawDetails() {
        QuestCore.Quest q=find(selected); int x=split()+8,max=width-split()-16;
        if(q==null) { fontRendererObj.drawSplitString("Select a quest. Drag the map; scroll to zoom. Operators: Shift+drag moves nodes. Use /quests team for teams.",x,76,max,0xDDDDDD); return; }
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
        lines.add("Rewards:"); for(QuestCore.Reward r:q.rewards) rewardLines(lines,r,"",max);
        int end=Math.max(1,(height-148)/11); detailsScroll=Math.max(0,Math.min(detailsScroll,Math.max(0,lines.size()-end)));
        for(int i=detailsScroll;i<lines.size() && i<detailsScroll+end;i++) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(lines.get(i),max),x,74+(i-detailsScroll)*11,0xEEEEEE);
    }
    private void rewardLines(List<String> lines,QuestCore.Reward r,String prefix,int max) {
        lines.addAll(fontRendererObj.listFormattedStringToWidth(prefix+r.type+" "+(r.item!=null?r.item:r.target==null?"":r.target)+" x"+r.count,max));
        for(int i=0;i<r.options.size();i++) rewardLines(lines,r.options.get(i),prefix+i+": ",max);
    }
    @Override protected void actionPerformed(GuiButton button) {
        QuestNetwork.Action action=new QuestNetwork.Action(); action.id=selected;
        switch(button.id) {
            case 0: mc.displayGuiScreen(null); return;
            case 1: ClientProxy.send(action); return;
            case 2:
                chapterIndex++; if(chapterIndex>=ClientProxy.snapshot.book.chapters.size()) chapterIndex=-1;
                chapter=chapterIndex<0?"":ClientProxy.snapshot.book.chapters.get(chapterIndex).id; refresh(); return;
            case 3: mc.displayGuiScreen(new QuestEditorScreen(this)); return;
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
        for(QuestCore.Quest q:visible()) if(x>=sx(q)-2 && x<=sx(q)+26 && y>=sy(q)-2 && y<=sy(q)+26) {
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
