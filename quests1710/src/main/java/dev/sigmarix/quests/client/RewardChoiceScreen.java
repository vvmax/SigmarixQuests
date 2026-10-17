package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;

/** Independent selections for each choice reward, including choices nested inside a selected option. */
public final class RewardChoiceScreen extends GuiScreen {
    private final GuiScreen parent;
    private final QuestCore.Quest quest;
    private final long revision;
    private final Map<String,Integer> selections=new HashMap<String,Integer>();
    private final List<Entry> entries=new ArrayList<Entry>();
    private int scroll;
    private QuestCore.Reward single,current; private final List<Integer> trail=new ArrayList<Integer>();
    private int boxX,boxY,boxWidth,boxHeight;
    RewardChoiceScreen(GuiScreen parent,QuestCore.Quest quest,QuestCore.Reward reward) { this(parent,quest); single=reward; current=reward; }
    private static final class Entry {
        String path; QuestCore.Reward reward;
        Entry(String path,QuestCore.Reward reward) { this.path=path; this.reward=reward; }
    }
    RewardChoiceScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; revision=ClientProxy.snapshot.revision; }
    private static boolean pending(QuestCore.Quest q,QuestCore.Reward r) { return !Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(q.id+":"+r.id)); }
    public static boolean needed(QuestCore.Quest q) { for(QuestCore.Reward r:q.rewards) if(pending(q,r) && r.type.equals("choice")) return true; return false; }
    private int selected(Entry e) { Integer value=selections.get(e.path); return value==null?0:Math.max(0,Math.min(value,e.reward.options.size()-1)); }
    private void collect(QuestCore.Reward reward,String path) {
        if(!reward.type.equals("choice")) return;
        Entry entry=new Entry(path,reward); entries.add(entry);
        int choice=selected(entry); collect(reward.options.get(choice),path+"."+choice);
    }
    @Override public void initGui() { rebuild(); }
    private void rebuild() {
        if(single!=null) {
            buttonList.clear(); boxWidth=Math.min(width-12,280); int rows=Math.max(1,(height-64)/20); scroll=Math.max(0,Math.min(scroll,Math.max(0,current.options.size()-rows))); boxHeight=26+Math.min(rows,current.options.size())*20;
            boxX=(width-boxWidth)/2; boxY=(height-boxHeight)/2;
            for(int i=scroll;i<current.options.size() && i<scroll+rows;i++) buttonList.add(new QuestFlatButton(100+i,boxX+2,boxY+22+(i-scroll)*20,boxWidth-4,18,QuestTaskLabels.reward(current.options.get(i))).left(22));
            buttonList.add(new QuestFlatButton(1,boxX+boxWidth-14,boxY+2,12,12,QuestText.tr("Back")).glyph("close",true)); return;
        }
        entries.clear(); for(int i=0;i<quest.rewards.size();i++) if(pending(quest,quest.rewards.get(i))) collect(quest.rewards.get(i),Integer.toString(i));
        buttonList.clear(); int rows=Math.max(1,(height-94)/26); scroll=Math.max(0,Math.min(scroll,Math.max(0,entries.size()-rows)));
        for(int i=scroll;i<entries.size() && i<scroll+rows;i++) {
            Entry entry=entries.get(i); int pick=selected(entry); QuestCore.Reward option=entry.reward.options.get(pick);
            String label="Choice "+(i+1)+": "+pick+" / "+(entry.reward.options.size()-1)+" - "+QuestTaskLabels.reward(option);
            buttonList.add(new QuestFlatButton(100+i,16,42+(i-scroll)*26,width-32,22,fontRendererObj.trimStringToWidth(label,width-48)));
        }
        buttonList.add(new QuestFlatButton(0,16,height-36,130,22,QuestText.tr("Claim selected")));
        buttonList.add(new QuestFlatButton(1,width-96,height-36,80,22,QuestText.tr("Back")));
    }
    @Override protected void actionPerformed(GuiButton b) {
        if(single!=null) {
            if(revision!=ClientProxy.snapshot.revision) { mc.displayGuiScreen(parent); return; }
            if(b.id==1) { if(trail.isEmpty()) { mc.displayGuiScreen(parent); return; } trail.remove(trail.size()-1); current=single; for(int index:trail) current=current.options.get(index); scroll=0; rebuild(); return; }
            if(b.id<100 || b.id-100>=current.options.size()) return;
            int index=b.id-100; trail.add(index); QuestCore.Reward chosen=current.options.get(index);
            if(chosen.type.equals("choice")) { current=chosen; scroll=0; rebuild(); return; }
            QuestNetwork.Action a=new QuestNetwork.Action(); a.action="claim_reward"; a.id=quest.id; a.text=single.id; a.choices=new int[trail.size()]; for(int i=0;i<trail.size();i++) a.choices[i]=trail.get(i); ClientProxy.send(a); mc.displayGuiScreen(parent); return;
        }
        if(b.id==1) { mc.displayGuiScreen(parent); return; }
        if(b.id>=100) { Entry e=entries.get(b.id-100); selections.put(e.path,(selected(e)+1)%e.reward.options.size()); rebuild(); return; }
        if(b.id==0) {
            if(revision!=ClientProxy.snapshot.revision) { mc.displayGuiScreen(parent); return; }
            QuestNetwork.Action action=new QuestNetwork.Action(); action.action="claim"; action.id=quest.id; action.choices=new int[entries.size()];
            for(int i=0;i<entries.size();i++) action.choices[i]=selected(entries.get(i));
            ClientProxy.send(action); mc.displayGuiScreen(parent);
        }
    }
    @Override public void drawScreen(int x,int y,float partial) {
        if(single!=null) {
            QuestClassicUi.background(mc,width,height); QuestClassicUi.panel(mc,boxX,boxY,boxWidth,boxHeight); fontRendererObj.drawString(QuestText.tr("Choose a reward"),boxX+5,boxY+6,0xFFFFFFFF); super.drawScreen(x,y,partial);
            for(Object raw:buttonList) { GuiButton b=(GuiButton)raw; if(b.id>=100) { QuestCore.Reward option=current.options.get(b.id-100); QuestClassicUi.item(mc,QuestClassicUi.rewardItem(option),option.icon.isEmpty()?option.meta:option.iconMeta,option.icon.isEmpty()?option.nbt:option.iconNbt,b.xPosition+2,b.yPosition+1,16); if(x>=b.xPosition && x<b.xPosition+b.width && y>=b.yPosition && y<b.yPosition+b.height) drawHoveringText(Collections.singletonList(QuestTaskLabels.reward(option)),x,y,fontRendererObj); } } return;
        }
        QuestClassicUi.background(mc,width,height); drawCenteredString(fontRendererObj,"Select rewards: "+quest.title,width/2,14,0xFFFFFF);
        drawCenteredString(fontRendererObj,QuestText.tr("Click each row to cycle its options"),width/2,28,0xBBBBBB); super.drawScreen(x,y,partial);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) { scroll+=wheel>0?-1:1; rebuild(); } }
    @Override protected void keyTyped(char c,int key) { if(key==org.lwjgl.input.Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c,key); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
