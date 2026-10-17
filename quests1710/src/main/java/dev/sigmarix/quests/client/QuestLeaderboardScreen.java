package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.gui.GuiScreen;
/** Port extension kept outside the original team action list. */
final class QuestLeaderboardScreen extends QuestListScreen {
    QuestLeaderboardScreen(GuiScreen parent){super(parent,false,false,0,0,0);}
    @Override protected String title(){return QuestText.tr("Leaderboard");}
    @Override protected List<Row> rows(){List<Row> rows=new ArrayList<Row>();int i=100;for(QuestNetwork.Rank rank:ClientProxy.snapshot.leaderboard){Row r=new Row(i++,rank.name+" — "+rank.completed+"/"+rank.total,"star");r.enabled=false;rows.add(r);}return rows;}
    @Override protected void click(Row row,int button){}
}
