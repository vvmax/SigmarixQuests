package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.gui.GuiScreen;import net.minecraft.util.StatCollector;
/** Original choice list; nested choices retain their server-validated selection trail. */
public final class RewardChoiceScreen extends QuestListScreen {
    private final QuestCore.Quest quest;private final QuestCore.Reward root;private QuestCore.Reward current;private final long revision;private final List<Integer> trail=new ArrayList<Integer>();
    RewardChoiceScreen(GuiScreen parent,QuestCore.Quest quest,QuestCore.Reward reward){super(parent,false,true,1,1,1);this.quest=quest;root=current=reward;revision=ClientProxy.snapshot.revision;}
    RewardChoiceScreen(GuiScreen parent,QuestCore.Quest quest){this(parent,quest,first(quest));}
    private static QuestCore.Reward first(QuestCore.Quest quest){for(QuestCore.Reward reward:quest.rewards)if(reward.type.equals("choice") && !Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(quest.id+":"+reward.id)))return reward;throw new IllegalArgumentException("No pending choice reward");}
    public static boolean needed(QuestCore.Quest quest){for(QuestCore.Reward reward:quest.rewards)if(reward.type.equals("choice") && !Boolean.TRUE.equals(ClientProxy.snapshot.rewardStates.get(quest.id+":"+reward.id)))return true;return false;}
    @Override protected String title(){return StatCollector.translateToLocal("ftbquests.reward.ftbquests.choice");}
    @Override protected List<Row> rows(){List<Row> rows=new ArrayList<Row>();for(int i=0;i<current.options.size();i++){QuestCore.Reward option=current.options.get(i);Row row=new Row(100+i,QuestTaskLabels.reward(option),"reward");if(!option.description.isEmpty())row.tooltip.addAll(Arrays.asList(option.description.split("\n")));rows.add(row);}return rows;}
    @Override protected Object revision(){return current;}
    @Override protected void drawRowIcon(Row row,int x,int y,int size){QuestClassicUi.reward(mc,current.options.get(row.id-100),x,y,size);}
    @Override protected void click(Row row,int button){if(revision!=ClientProxy.snapshot.revision){mc.displayGuiScreen(parent);return;}int index=row.id-100;trail.add(index);QuestCore.Reward chosen=current.options.get(index);if(chosen.type.equals("choice")){current=chosen;scroll=0;rebuild();return;}QuestNetwork.Action action=new QuestNetwork.Action();action.action="claim_reward";action.id=quest.id;action.text=root.id;action.choices=new int[trail.size()];for(int i=0;i<trail.size();i++)action.choices[i]=trail.get(i);ClientProxy.send(action);mc.displayGuiScreen(parent);}
    @Override protected void actionPerformed(net.minecraft.client.gui.GuiButton button){if(button.id==1){back();return;}super.actionPerformed(button);}
    private void back(){if(trail.isEmpty()){mc.displayGuiScreen(parent);return;}trail.remove(trail.size()-1);current=root;for(int i:trail)current=current.options.get(i);scroll=0;rebuild();}
    @Override protected void keyTyped(char ch,int key){if(key==1){back();return;}super.keyTyped(ch,key);}
}
