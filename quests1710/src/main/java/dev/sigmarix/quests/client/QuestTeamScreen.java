package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Centered FTBLib-style action and player lists; mutations remain server checked. */
public final class QuestTeamScreen extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField input;
    private int ticks,scroll,left,top,panelWidth,mode;
    private String pending="";
    private int color=0x0094FF;
    private static final int[] COLORS={0x0094FF,0x00DDFF,0x23D34C,0xFFD000,0xFF9400,0xEA4B4B,0xE888C9,0xFF007F,0xB342FF,0xC0C0C0};
    private final String[] operations={"create","invite","join","kick","leave","disband"};
    private final String[] labels={"Create team","Invite player","Join leader","Kick player","Leave team","Disband team"};
    public QuestTeamScreen(GuiScreen parent) {this.parent=parent;}
    @Override public void initGui() {
        buttonList.clear(); panelWidth=100;
        for(String title:labels) panelWidth=Math.max(panelWidth,fontRendererObj.getStringWidth(QuestText.tr(title))+28);
        panelWidth=Math.max(panelWidth,fontRendererObj.getStringWidth(QuestText.tr("Leaderboard"))+28);
        panelWidth=Math.min(panelWidth,width-51); left=(width-panelWidth-39)/2;top=(height-158)/2;
        String previous=input==null?"":input.getText();
        input=new QuestLibraryTextField(fontRendererObj,left+12,top+12,panelWidth+9,12);input.setMaxStringLength(32);input.setEnableBackgroundDrawing(false);input.setText(previous);
        if(mode==2) {
            left=(width-162)/2;top=(height-118)/2;
            input=new QuestLibraryTextField(fontRendererObj,left+12,top+12,138,12);input.setEnableBackgroundDrawing(false);input.setMaxStringLength(32);input.setText(previous);input.setFocused(true);
            buttonList.add(new QuestLibraryUi.Button(0,left+8,top+94,71,16,QuestText.tr("Cancel"),""));
            buttonList.add(new QuestLibraryUi.Button(20,left+82,top+94,71,16,QuestText.tr("Accept"),""));return;
        }
        if(mode==3) {
            buttonList.add(new QuestLibraryUi.Button(0,left+9,top+119,panelWidth/2,20,QuestText.tr("Cancel"),""));
            buttonList.add(new QuestLibraryUi.Button(20,left+11+panelWidth/2,top+119,panelWidth/2-2,20,QuestText.tr("Accept"),""));return;
        }
        if(mode==4) {
            buttonList.add(new QuestLibraryUi.Button(0,left+9,top+119,panelWidth,20,QuestText.tr("Cancel"),""));return;
        }
        if(mode==1){buttonList.add(new QuestLibraryUi.Button(1,left+9,top+129,panelWidth,20,QuestText.tr("Back"),"back"));return;}
        String[] icons={"add","friends","accept","remove","back","bin"};
        for(int i=0;i<operations.length;i++) buttonList.add(new QuestLibraryUi.Button(10+i,left+9,top+9+i*20,panelWidth,20,QuestText.tr(labels[i]),icons[i]));
        buttonList.add(new QuestLibraryUi.Button(1,left+9,top+129,panelWidth,20,QuestText.tr("Leaderboard"),"star"));
    }
    private void change(int value) {mode=value;scroll=0;initGui();}
    private void send(String operation,String text) {QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_"+operation;action.text=text;action.x=color;ClientProxy.send(action);}
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==0){change(0);return;}
        if(button.id==1){change(mode==1?0:1);return;}
        if(button.id==20){send(pending,input.getText());change(0);return;}
        int index=button.id-10;if(index<0 || index>=operations.length)return;pending=operations[index];
        if(index==0){change(2);return;}
        if(index==1 || index==3){change(3);input.setFocused(true);return;}
        if(index==2){change(4);return;}
        mc.displayGuiScreen(new QuestConfirmScreen(this,QuestText.tr(labels[index])+"?",()->send(pending,"")));
    }
    @Override public void updateScreen() {
        input.updateCursorCounter();if(++ticks%40==0)ClientProxy.send(new QuestNetwork.Action());
        for(Object raw:buttonList) {GuiButton b=(GuiButton)raw;if(b.id>=10 && b.id<=15)b.enabled=b.id==10?ClientProxy.snapshot.team.isEmpty():b.id==11 || b.id==13 || b.id==15?ClientProxy.snapshot.teamLeader:b.id==12 || !ClientProxy.snapshot.team.isEmpty();}
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();QuestLibraryUi.window(left,top,mode==2?162:panelWidth+39,mode==2?118:158);
        String title=mode==1?QuestText.tr("Leaderboard"):mode==2?QuestText.tr("Create team"):mode==3?QuestText.tr(pending.equals("kick")?"Kick player":"Invite player"):mode==4?QuestText.tr("Invitations"):ClientProxy.snapshot.team.isEmpty()?mc.thePlayer.getCommandSenderName():ClientProxy.snapshot.team;
        if(mode!=2)drawCenteredString(fontRendererObj,title,width/2,top-11,mode==0 && !ClientProxy.snapshot.team.isEmpty()?ClientProxy.snapshot.teamColor:0xFFFFFF);
        if(mode==2 || mode==3){QuestLibraryUi.textBox(mode==2?left+8:left+9,top+8,mode==2?146:panelWidth,16);if(mode==2)input.setTextColor(color);input.drawTextBox();}
        if(mode==2)for(int i=0;i<COLORS.length;i++){
            int bx=left+8+(i%5)*30,by=top+32+(i/5)*30;QuestLibraryUi.slot(bx,by,25,25);
            drawRect(bx,by,bx+25,by+25,((color==COLORS[i] || x>=bx && x<bx+25 && y>=by && y<by+25)?200:100)<<24|COLORS[i]);
        }
        if(mode!=2) {
            if(mode!=3)QuestLibraryUi.slot(left+9,top+9,panelWidth,140);
            // Draw list buttons above their recessed panel.
            QuestLibraryUi.slot(left+panelWidth+15,top+8,16,142);
            QuestLibraryUi.patch("container_beacon",0,219,22,22,6,left+panelWidth+16,top+9,14,mode==0?140:20);
        }
        List<String> rows=new ArrayList<String>();
        if(mode==1)for(QuestNetwork.Rank rank:ClientProxy.snapshot.leaderboard)rows.add(rank.name+" — "+rank.completed+"/"+rank.total);
        if(mode==3)rows.addAll(ClientProxy.snapshot.teamMembers);
        if(mode==4)rows.addAll(ClientProxy.snapshot.teamInvites);
        int visible=mode==3?4:mode==1?6:7;scroll=Math.max(0,Math.min(scroll,Math.max(0,rows.size()-visible)));
        for(int i=scroll;i<rows.size() && i<scroll+visible;i++)new QuestLibraryUi.Button(100+i,left+9,top+(mode==3?34:9)+(i-scroll)*20,panelWidth,20,rows.get(i),"player").drawButton(mc,x,y);
        super.drawScreen(x,y,partial);
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        super.mouseClicked(x,y,button);if(mode==2 || mode==3)input.mouseClicked(x,y,button);
        if(button==0 && mode==2)for(int i=0;i<COLORS.length;i++){int bx=left+8+(i%5)*30,by=top+32+(i/5)*30;if(x>=bx && x<bx+25 && y>=by && y<by+25)color=COLORS[i];}
        if(button!=0 || mode!=3 && mode!=4)return;
        List<String> rows=mode==4?ClientProxy.snapshot.teamInvites:ClientProxy.snapshot.teamMembers;
        int index=(y-top-(mode==3?34:9))/20+scroll;
        if(x>=left+9 && x<left+9+panelWidth && y>=top+(mode==3?34:9) && index>=scroll && index<rows.size() && index<scroll+(mode==3?4:7)) {
            if(mode==4){send("join",rows.get(index));change(0);}else input.setText(rows.get(index));
        }
    }
    @Override protected void keyTyped(char character,int key) {
        if(key==1){if(mode!=0)change(0);else mc.displayGuiScreen(parent);return;}
        if((mode==2 || mode==3) && input.textboxKeyTyped(character,key))return;super.keyTyped(character,key);
    }
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();scroll+=wheel>0?-3:wheel<0?3:0;}
    @Override public boolean doesGuiPauseGame(){return false;}
}
