package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.util.StatCollector;
/** The original team selector and team action menu are separate screens. */
public final class QuestTeamScreen extends QuestListScreen {
    private boolean inTeam;
    public QuestTeamScreen(GuiScreen parent){super(parent,ClientProxy.snapshot.teamId.isEmpty(),false,0,0,0);inTeam=!ClientProxy.snapshot.teamId.isEmpty();}
    static String tr(String key){return StatCollector.translateToLocal(key);}
    static String teamTitle(QuestNetwork.TeamEntry entry){return chatColor(entry.color)+(entry.title.isEmpty()?ownerName(entry.owner)+"'s Team":entry.title);}
    static String chatColor(int color){int[] values=QuestTeamCreateScreen.COLORS;String[] names={"9","b","a","e","6","c","d","d","5","7"};for(int i=0;i<values.length;i++)if(values[i]==color)return "\u00a7"+names[i];return "\u00a7f";}
    static String ownerName(String id){if(MinecraftOwner.id().equals(id))return net.minecraft.client.Minecraft.getMinecraft().thePlayer.getCommandSenderName();for(QuestNetwork.TeamPlayer p:ClientProxy.snapshot.teamPlayers)if(p.id.equals(id))return p.name;return id;}
    private static final class MinecraftOwner{static String id(){return net.minecraft.client.Minecraft.getMinecraft().thePlayer.getUniqueID().toString();}}
    @Override protected String title(){if(!inTeam)return tr("team_action.ftblib.select_team");QuestNetwork.TeamSettings s=ClientProxy.snapshot.teamSettings;String owner="";for(QuestNetwork.TeamEntry e:ClientProxy.snapshot.teams)if(e.id.equals(ClientProxy.snapshot.teamId))owner=e.owner;return chatColor(s.color)+(s.title.isEmpty()?ownerName(owner)+"'s Team":s.title);}
    @Override protected List<Row> rows(){
        List<Row> rows=new ArrayList<Row>();QuestNetwork.Snapshot snap=ClientProxy.snapshot;
        if(!inTeam){
            rows.add(new Row(10,tr("team_action.ftblib.create_team"),"add"));int i=100;
            for(QuestNetwork.TeamEntry team:snap.teams){Row row=new Row(i++,teamTitle(team),"");row.teamHead=true;row.playerId=team.owner;row.playerName=ownerName(team.owner);row.border=0xFF000000|team.color;row.enabled=!team.enemy;if(team.enemy)row.title="\u00a7c"+net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(row.title);else if(team.requesting)row.title="\u00a7b"+net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(row.title);else if(team.freeToJoin || team.invited)row.title="\u00a7a"+net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(row.title);if(!team.description.isEmpty())row.tooltip.add("\u00a7o"+team.description);row.tooltip.add("\u00a77"+tr(team.requesting?"ftblib.lang.team_status.requesting_invite":team.enemy?"ftblib.lang.team_status.enemy":team.invited || team.freeToJoin?"ftblib.lang.team.gui.join_team":"ftblib.lang.team.gui.request_invite").replace("%s",team.name));rows.add(row);}return rows;
        }
        Row settings=new Row(30,tr("gui.settings"),"settings");settings.enabled=snap.teamModerator;rows.add(settings);
        String[] types={"members","allies","moderators","enemies"},icons={"friends","star","shield","close"};
        for(int i=0;i<types.length;i++){Row row=new Row(31+i,tr("team_action.ftblib."+types[i]),icons[i]);row.enabled=i==2?snap.teamLeader && snap.teamMembers.size()>1:snap.teamModerator && !snap.teamPlayers.isEmpty();rows.add(row);}
        if(snap.teamLeader && snap.teamMembers.size()>1)rows.add(new Row(35,tr("team_action.ftblib.transfer_ownership"),"right"));else rows.add(new Row(14,tr("team_action.ftblib.leave"),"remove"));return rows;
    }
    @Override protected void click(Row row,int button){
        if(button!=0)return;
        if(row.id==10){mc.displayGuiScreen(new QuestTeamCreateScreen(this));return;}
        if(row.id>=100){int index=row.id-100;if(index<ClientProxy.snapshot.teams.size()){QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_select";action.text=ClientProxy.snapshot.teams.get(index).id;ClientProxy.send(action);}return;}
        if(row.id==30){mc.displayGuiScreen(new QuestTeamSettingsScreen(this));return;}
        if(row.id>=31 && row.id<=35){mc.displayGuiScreen(new QuestTeamPlayersScreen(this,row.id-31));return;}
        if(row.id==14)mc.displayGuiScreen(new GuiYesNo((yes,id)->{mc.displayGuiScreen(this);if(yes){QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_exit";ClientProxy.send(action);}},tr("team_action.ftblib.leave")+"?","\u00a7c"+tr("team_action.ftblib.leave.confirmation"),0));
    }
    @Override public void updateScreen(){if(inTeam!=!ClientProxy.snapshot.teamId.isEmpty()){mc.displayGuiScreen(new QuestTeamScreen(parent));return;}super.updateScreen();}
    @Override protected void keyTyped(char ch,int key){if(isCtrlKeyDown() && key==org.lwjgl.input.Keyboard.KEY_L){mc.displayGuiScreen(new QuestLeaderboardScreen(this));return;}super.keyTyped(ch,key);}
}
