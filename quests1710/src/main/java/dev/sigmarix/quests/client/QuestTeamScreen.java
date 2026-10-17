package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
/** Native team management and completion leaderboard; all changes are checked by the server. */
public final class QuestTeamScreen extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField input;
    private int ticks,scroll;
    private boolean leaderboard;
    private final String[] operations={"create","invite","join","kick","leave","disband"};
    private final String[] labels={"Create team","Invite player","Join leader","Kick player","Leave team","Disband team"};
    public QuestTeamScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() {
        buttonList.clear(); buttonList.add(new GuiButton(0,12,height-30,90,20,QuestText.tr("Back")));
        buttonList.add(new GuiButton(1,width-142,height-30,130,20,QuestText.tr("Leaderboard")));
        input=new GuiTextField(fontRendererObj,12,40,width-24,20); input.setMaxStringLength(32);
        for(int i=0;i<operations.length;i++) buttonList.add(new GuiButton(10+i,12+(i%3)*(width-24)/3,68+(i/3)*24,(width-30)/3,20,QuestText.tr(labels[i])));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==0) { mc.displayGuiScreen(parent); return; }
        if(button.id==1) { leaderboard=!leaderboard; scroll=0; return; }
        QuestNetwork.Action action=new QuestNetwork.Action(); action.action="team_"+operations[button.id-10]; action.text=input.getText(); ClientProxy.send(action);
    }
    @Override public void updateScreen() {
        input.updateCursorCounter();
        if(++ticks%40==0) ClientProxy.send(new QuestNetwork.Action());
        for(Object raw:buttonList) { GuiButton b=(GuiButton)raw; if(b.id>=10) { b.visible=!leaderboard; b.enabled=b.id==10?ClientProxy.snapshot.team.isEmpty():b.id==11 || b.id==13 || b.id==15?ClientProxy.snapshot.teamLeader:!ClientProxy.snapshot.team.isEmpty() || b.id==12; } }
    }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj,QuestText.tr(leaderboard?"Leaderboard":"Teams")+" — "+ClientProxy.snapshot.profile,width/2,15,0xFFFFFF);
        List<String> lines=new ArrayList<String>();
        if(leaderboard) {
            for(int i=0;i<ClientProxy.snapshot.leaderboard.size();i++) { QuestNetwork.Rank rank=ClientProxy.snapshot.leaderboard.get(i); lines.add((i+1)+". "+rank.name+" — "+rank.completed+"/"+rank.total); }
        } else {
            input.drawTextBox(); lines.add(QuestText.tr("Team")+": "+ClientProxy.snapshot.team);
            lines.addAll(ClientProxy.snapshot.teamMembers);
            if(!ClientProxy.snapshot.teamInvites.isEmpty()) { lines.add(QuestText.tr("Invitations")); lines.addAll(ClientProxy.snapshot.teamInvites); }
        }
        int top=leaderboard?42:124,rows=Math.max(1,(height-top-54)/12); scroll=Math.max(0,Math.min(scroll,Math.max(0,lines.size()-rows)));
        for(int i=scroll;i<lines.size() && i<scroll+rows;i++) fontRendererObj.drawString(fontRendererObj.trimStringToWidth(lines.get(i),width-24),12,top+(i-scroll)*12,0xEEEEEE);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientProxy.snapshot.message,width-24),12,height-46,0xFFDD77); super.drawScreen(x,y,partial);
    }
    @Override protected void mouseClicked(int x,int y,int button) { super.mouseClicked(x,y,button); if(!leaderboard) input.mouseClicked(x,y,button); }
    @Override protected void keyTyped(char character,int key) { if(!leaderboard && input.textboxKeyTyped(character,key)) return; super.keyTyped(character,key); }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); scroll+=wheel>0?-3:wheel<0?3:0; }
    @Override public boolean doesGuiPauseGame() { return false; }
}
