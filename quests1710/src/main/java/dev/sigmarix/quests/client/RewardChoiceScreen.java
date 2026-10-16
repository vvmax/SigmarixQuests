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
    private static final class Entry {
        String path; QuestCore.Reward reward;
        Entry(String path,QuestCore.Reward reward) { this.path=path; this.reward=reward; }
    }
    RewardChoiceScreen(GuiScreen parent,QuestCore.Quest quest) { this.parent=parent; this.quest=quest; revision=ClientProxy.snapshot.revision; }
    public static boolean needed(QuestCore.Quest q) { for(QuestCore.Reward r:q.rewards) if(r.type.equals("choice")) return true; return false; }
    private int selected(Entry e) { Integer value=selections.get(e.path); return value==null?0:Math.max(0,Math.min(value,e.reward.options.size()-1)); }
    private void collect(QuestCore.Reward reward,String path) {
        if(!reward.type.equals("choice")) return;
        Entry entry=new Entry(path,reward); entries.add(entry);
        int choice=selected(entry); collect(reward.options.get(choice),path+"."+choice);
    }
    @Override public void initGui() { rebuild(); }
    private void rebuild() {
        entries.clear(); for(int i=0;i<quest.rewards.size();i++) collect(quest.rewards.get(i),Integer.toString(i));
        buttonList.clear(); int rows=Math.max(1,(height-94)/26); scroll=Math.max(0,Math.min(scroll,Math.max(0,entries.size()-rows)));
        for(int i=scroll;i<entries.size() && i<scroll+rows;i++) {
            Entry entry=entries.get(i); int pick=selected(entry); QuestCore.Reward option=entry.reward.options.get(pick);
            String label="Choice "+(i+1)+": "+pick+" / "+(entry.reward.options.size()-1)+" - "+option.type+" "+(option.item==null?"":option.item)+" x"+option.count;
            buttonList.add(new GuiButton(100+i,16,42+(i-scroll)*26,width-32,22,fontRendererObj.trimStringToWidth(label,width-48)));
        }
        buttonList.add(new GuiButton(0,16,height-36,130,22,"Claim selected"));
        buttonList.add(new GuiButton(1,width-96,height-36,80,22,"Back"));
    }
    @Override protected void actionPerformed(GuiButton b) {
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
        drawDefaultBackground(); drawCenteredString(fontRendererObj,"Select rewards: "+quest.title,width/2,14,0xFFFFFF);
        drawCenteredString(fontRendererObj,"Click each row to cycle its options",width/2,28,0xBBBBBB); super.drawScreen(x,y,partial);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) { scroll+=wheel>0?-1:1; rebuild(); } }
    @Override public boolean doesGuiPauseGame() { return false; }
}
