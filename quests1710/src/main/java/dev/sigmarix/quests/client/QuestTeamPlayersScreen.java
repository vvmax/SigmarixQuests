package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
/** Original member, ally, moderator, enemy and ownership lists. */
final class QuestTeamPlayersScreen extends QuestListScreen {
    private final int kind;
    QuestTeamPlayersScreen(GuiScreen parent,int kind){super(parent,true,false,0,0,0);this.kind=kind;}
    @Override protected String title(){return QuestTeamScreen.tr("team_action.ftblib."+new String[]{"members","allies","moderators","enemies","transfer_ownership"}[kind]);}
    private boolean included(QuestNetwork.TeamPlayer p){if(p.status.equals("owner"))return false;boolean member=p.status.equals("member") || p.status.equals("mod");return kind==0?!p.status.equals("enemy"):kind==1?!member && !p.status.equals("enemy"):kind==3?p.status.equals("enemy") || p.status.equals("none"):member;}
    private List<QuestNetwork.TeamPlayer> players(){List<QuestNetwork.TeamPlayer> list=new ArrayList<QuestNetwork.TeamPlayer>();for(QuestNetwork.TeamPlayer p:ClientProxy.snapshot.teamPlayers)if(included(p))list.add(p);list.sort(Comparator.comparingInt((QuestNetwork.TeamPlayer p)->p.requesting?-1000:-rank(p.status)).thenComparing(p->p.name,String.CASE_INSENSITIVE_ORDER));return list;}
    private int rank(String s){return s.equals("mod")?80:s.equals("member")?50:s.equals("ally")?30:s.equals("invited")?10:s.equals("enemy")?1:0;}
    @Override protected List<Row> rows(){List<Row> out=new ArrayList<Row>();int i=100;for(QuestNetwork.TeamPlayer p:players()){Row r=new Row(i++,p.name,"");r.playerId=p.id;r.playerName=p.name;r.border=p.requesting?0xFFFFAA00:p.status.equals("mod") || p.status.equals("member")?0xFF00AA00:p.status.equals("invited")?0xFF5555FF:p.status.equals("ally")?0xFF00AAAA:p.status.equals("enemy")?0xFFFF5555:0xFF808080;if(kind==2)r.border=p.status.equals("mod")?0xFF00AA00:0xFF808080;if(kind==4)r.border=0xFF808080;r.tooltip.add(QuestTeamScreen.tr("ftblib.lang.team_status."+p.status));if(kind==0){String key=p.requesting?"requesting_invite":p.status.equals("member") || p.status.equals("mod")?"kick":p.status.equals("invited")?"cancel_invite":"invite";r.tooltip.add(QuestTeamScreen.tr("ftblib.lang.team.gui.members."+key));}out.add(r);}return out;}
    @Override protected void click(Row row,int button){List<QuestNetwork.TeamPlayer> list=players();int index=row.id-100;if(index<0 || index>=list.size())return;QuestNetwork.TeamPlayer p=list.get(index);String operation="";
        if(kind==0){if(p.requesting)operation=button==0?"invite":"deny_request";else if(p.status.equals("none") || p.status.equals("ally")){if(button==0)operation="invite";}else if(button!=0)operation=p.status.equals("invited")?"cancel_invite":"kick";}
        else if(kind==1)operation="ally";else if(kind==2)operation="moderator";else if(kind==3)operation="enemy";
        else{mc.displayGuiScreen(new GuiYesNo((yes,id)->{mc.displayGuiScreen(this);if(yes)send(p,"transfer");},title()+"?",mc.thePlayer.getCommandSenderName()+" => "+p.name,0));return;}
        if(!operation.isEmpty())send(p,operation);
    }
    private void send(QuestNetwork.TeamPlayer player,String operation){QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_player";action.id=player.id;action.text=operation;ClientProxy.send(action);}
}
