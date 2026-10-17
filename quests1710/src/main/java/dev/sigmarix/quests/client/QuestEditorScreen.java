package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.*;

/** Multiline JSON editor for every schema field, with optimistic concurrency and server validation. */
public final class QuestEditorScreen extends GuiScreen {
    private final GuiScreen parent;
    private String text,undo="",message="Ctrl+A/C/V, Ctrl+Z; edit JSON; Save validates on server";
    private int cursor,scroll,horizontal;
    private boolean selectAll,pending;
    private final long revision;
    public QuestEditorScreen(GuiScreen parent) { this(parent,ClientProxy.snapshot.book,ClientProxy.snapshot.revision); }
    public QuestEditorScreen(GuiScreen parent,QuestCore.Book draft,long revision) { this.parent=parent; text=QuestService.JSON.toJson(draft); this.revision=revision; }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true); buttonList.clear();
        buttonList.add(new QuestFlatButton(0,8,8,90,20,QuestText.tr("Save book")));
        buttonList.add(new QuestFlatButton(1,104,8,70,20,QuestText.tr("Back")));
        buttonList.add(new QuestFlatButton(2,180,8,100,20,QuestText.tr("Add quest")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(b.id==1) { mc.displayGuiScreen(parent); return; }
        if(b.id==2) {
            try {
                QuestCore.Book book=QuestService.JSON.fromJson(text,QuestCore.Book.class);
                QuestCore.Quest q=new QuestCore.Quest(); q.id="quest_"+System.currentTimeMillis(); q.title="New quest"; q.x=book.quests.size()%6*3; q.y=book.quests.size()/6*2;
                QuestCore.Task task=new QuestCore.Task(); task.type="check"; task.id="confirm"; q.tasks.add(task); book.quests.add(q);
                undo=text; text=QuestService.JSON.toJson(book); cursor=text.length(); ensureVisible();
            } catch(RuntimeException ex) { message="JSON error: "+ex.getMessage(); }
            return;
        }
        if(b.id==0 && !pending) {
            try {
                QuestNetwork.Action action=new QuestNetwork.Action(); action.action="save"; action.revision=revision;
                action.book=QuestService.JSON.fromJson(text,QuestCore.Book.class); action.book.validate();
                ClientProxy.send(action); pending=true; message=QuestText.tr("Saving...");
            } catch(RuntimeException ex) { message="Validation: "+ex.getMessage(); }
        }
    }
    public void saved(QuestNetwork.Snapshot snapshot) {
        if(!pending) return; pending=false; message=snapshot.message;
        if(snapshot.revision!=revision && !snapshot.message.startsWith("Error:")) mc.displayGuiScreen(parent);
    }
    private void insert(String value) {
        if(value==null) return;
        String next=selectAll?value:text.substring(0,cursor)+value+text.substring(cursor);
        if(next.length()>900000) { message="Maximum 900000 characters"; return; }
        undo=text; text=next; cursor=selectAll?value.length():cursor+value.length(); selectAll=false; ensureVisible();
    }
    private int lineStart() { return text.lastIndexOf('\n',Math.max(-1,cursor-1))+1; }
    private int lineEnd() { int end=text.indexOf('\n',cursor); return end<0?text.length():end; }
    private void ensureVisible() {
        int line=0; for(int i=0;i<cursor;i++) if(text.charAt(i)=='\n') line++;
        int rows=Math.max(1,(height-72)/11); if(line<scroll) scroll=line; if(line>=scroll+rows) scroll=line-rows+1;
        int pixel=fontRendererObj.getStringWidth(text.substring(lineStart(),cursor));
        if(pixel<horizontal) horizontal=pixel;
        if(pixel-horizontal>width-28) horizontal=pixel-width+28;
    }
    @Override protected void keyTyped(char ch,int key) {
        if(pending) { if(key==Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); return; }
        if(isCtrlKeyDown()) {
            if(key==Keyboard.KEY_A) { selectAll=true; return; }
            if(key==Keyboard.KEY_C) { if(selectAll) setClipboardString(text); return; }
            if(key==Keyboard.KEY_V) { insert(getClipboardString()); return; }
            if(key==Keyboard.KEY_Z) { String previous=text; text=undo; undo=previous; cursor=Math.min(cursor,text.length()); selectAll=false; ensureVisible(); return; }
        }
        if(key==Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if(key==Keyboard.KEY_RETURN || key==Keyboard.KEY_NUMPADENTER) { insert("\n"); return; }
        if(key==Keyboard.KEY_TAB) { insert("  "); return; }
        if(key==Keyboard.KEY_BACK || key==Keyboard.KEY_DELETE) {
            undo=text;
            if(selectAll) { text=""; cursor=0; }
            else if(key==Keyboard.KEY_BACK && cursor>0) { text=text.substring(0,cursor-1)+text.substring(cursor); cursor--; }
            else if(key==Keyboard.KEY_DELETE && cursor<text.length()) text=text.substring(0,cursor)+text.substring(cursor+1);
            selectAll=false; ensureVisible(); return;
        }
        if(key==Keyboard.KEY_LEFT) cursor=Math.max(0,cursor-1);
        else if(key==Keyboard.KEY_RIGHT) cursor=Math.min(text.length(),cursor+1);
        else if(key==Keyboard.KEY_HOME) cursor=isCtrlKeyDown()?0:lineStart();
        else if(key==Keyboard.KEY_END) cursor=isCtrlKeyDown()?text.length():lineEnd();
        else if(key==Keyboard.KEY_UP) { int start=lineStart(),col=cursor-start; if(start>0) { int prev=text.lastIndexOf('\n',start-2)+1; cursor=Math.min(start-1,prev+col); } }
        else if(key==Keyboard.KEY_DOWN) { int end=lineEnd(),col=cursor-lineStart(); if(end<text.length()) { int next=text.indexOf('\n',end+1); cursor=Math.min(next<0?text.length():next,end+1+col); } }
        else if(net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(ch)) { insert(Character.toString(ch)); return; }
        selectAll=false; ensureVisible();
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        super.mouseClicked(x,y,button);
        if(y<42 || y>height-28) return;
        String[] lines=text.split("\n",-1); int line=Math.min(lines.length-1,scroll+(y-44)/11),index=0;
        for(int i=0;i<line;i++) index+=lines[i].length()+1;
        int col=0,pixel=0; while(col<lines[line].length() && pixel+fontRendererObj.getCharWidth(lines[line].charAt(col))<x-12+horizontal) pixel+=fontRendererObj.getCharWidth(lines[line].charAt(col++));
        cursor=index+col; selectAll=false;
    }
    @Override public void drawScreen(int x,int y,float partial) {
        QuestClassicUi.background(mc,width,height); drawRect(6,38,width-6,height-24,selectAll?0xFF263E65:0xFF101820);
        String[] lines=text.split("\n",-1); int current=0,lineNo=0;
        for(int i=0;i<cursor;i++) { if(text.charAt(i)=='\n') { lineNo++; current=i+1; } }
        int rows=Math.max(1,(height-72)/11);
        for(int i=scroll;i<lines.length && i<scroll+rows;i++) fontRendererObj.drawString(visible(lines[i]),12,44+(i-scroll)*11,0xE8E8E8);
        if(lineNo>=scroll && lineNo<scroll+rows && (System.currentTimeMillis()/500)%2==0) {
            int cx=12+fontRendererObj.getStringWidth(text.substring(current,cursor))-horizontal; if(cx<width-8) drawRect(cx,43+(lineNo-scroll)*11,cx+1,53+(lineNo-scroll)*11,0xFFFFFFFF);
        }
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(message,width-16),8,height-16,0xFFDD77);
        super.drawScreen(x,y,partial);
    }
    private String visible(String line) {
        int index=0,pixels=0; while(index<line.length() && pixels<horizontal) pixels+=fontRendererObj.getCharWidth(line.charAt(index++));
        return fontRendererObj.trimStringToWidth(line.substring(index),width-24);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); scroll=Math.max(0,Math.min(Math.max(0,text.split("\n",-1).length-Math.max(1,(height-72)/11)),scroll+(wheel>0?-3:wheel<0?3:0))); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
