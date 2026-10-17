package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.gui.GuiScreen;
/** Team settings retain the same groups, ordering and field types as FTBLib 1.12. */
final class QuestTeamSettingsScreen extends QuestConfigScreen {
    private final QuestNetwork.TeamSettings draft;
    QuestTeamSettingsScreen(GuiScreen parent){super(parent);draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.teamSettings),QuestNetwork.TeamSettings.class);}
    @Override protected String title(){return QuestText.tr("Settings")+" \u00a7r\u00a78#"+ClientProxy.snapshot.team;}
    private String tr(String key){return QuestTeamScreen.tr("team_config.ftblib."+key);}
    @Override protected List<Entry> entries(){
        List<Entry> entries=new ArrayList<Entry>();String main="FTB Library",display=main+" > "+tr("display");
        entries.add(new Entry(main,tr("free_to_join"),()->Boolean.toString(draft.freeToJoin),()->draft.freeToJoin?0x33AA33:0xD52834,(b,g)->draft.freeToJoin=!draft.freeToJoin));
        entries.add(new Entry(display,tr("display.color"),()->QuestTeamCreateScreen.colorName(colorIndex()),()->draft.color,(b,g)->{int index=(colorIndex()+(b==0?1:9))%10;draft.color=QuestTeamCreateScreen.COLORS[index];}));
        entries.add(new Entry(display,tr("display.desc"),()->draft.description,()->0xFFAA49,(b,g)->mc.displayGuiScreen(new QuestValueScreen(this,draft.description,0xFFAA49,1024,v->draft.description=v))));
        entries.add(new Entry(display,tr("display.fake_player_status"),()->QuestTeamScreen.tr("ftblib.lang.team_status."+draft.fakePlayerStatus),()->0x0094FF,(b,g)->{String[] values={"none","ally","member","mod"};int i=Arrays.asList(values).indexOf(draft.fakePlayerStatus);draft.fakePlayerStatus=values[(i+(b==0?1:3))%4];}));
        entries.add(new Entry(display,tr("display.title"),()->draft.title,()->0xFFAA49,(b,g)->mc.displayGuiScreen(new QuestValueScreen(this,draft.title,0xFFAA49,128,v->draft.title=v))));return entries;
    }
    private int colorIndex(){for(int i=0;i<QuestTeamCreateScreen.COLORS.length;i++)if(QuestTeamCreateScreen.COLORS[i]==draft.color)return i;return 0;}
    @Override protected void save(){QuestNetwork.Action action=new QuestNetwork.Action();action.action="team_settings";action.teamSettings=draft;ClientProxy.send(action);}
}
