package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
/** The original searchable object selector, including type colors and 14px rows. */
final class QuestSelectObjectScreen extends QuestListScreen {
    static final int FILE=2,CHAPTER=4,QUEST=8,TASK=16,REWARD=64;
    static final class Selection {
        int type;String id,title;QuestCore.Quest quest;QuestCore.Task task;QuestCore.Reward reward;QuestCore.Chapter chapter;
        Selection(int type,String id,String title){this.type=type;this.id=id;this.title=title;}
    }
    private final int types;private final boolean nullable;private final Predicate<Selection> filter;private final Consumer<Selection> select;
    private List<Selection> objects=new ArrayList<Selection>();
    QuestSelectObjectScreen(GuiScreen parent,int types,boolean nullable,Predicate<Selection> filter,Consumer<Selection> select){super(parent,true,true,1,1,1);this.types=types;this.nullable=nullable;this.filter=filter;this.select=select;}
    @Override protected int rowHeight(){return 14;}
    @Override protected int minimumPanelWidth(){return 202;}
    @Override protected String title(){return StatCollector.translateToLocal("ftbquests.gui.select_quest_object");}
    private void append(Selection selection){if((selection.type&types)!=0 && filter.test(selection))objects.add(selection);}
    @Override protected List<Row> rows(){
        objects=new ArrayList<Selection>();QuestCore.Book book=ClientProxy.snapshot.book;
        append(new Selection(FILE,"book",book.title));
        for(QuestCore.Chapter chapter:book.chapters){Selection s=new Selection(CHAPTER,"chapter:"+chapter.id,chapter.title);s.chapter=chapter;append(s);}
        for(QuestCore.Quest quest:book.quests){Selection s=new Selection(QUEST,quest.id,quest.title);s.quest=quest;append(s);
            for(QuestCore.Task task:quest.tasks){s=new Selection(TASK,"task:"+quest.id+"/"+task.id,QuestTaskLabels.title(task));s.quest=quest;s.task=task;append(s);}
            for(QuestCore.Reward reward:quest.rewards){s=new Selection(REWARD,"reward:"+quest.id+"/"+reward.id,QuestTaskLabels.reward(reward));s.quest=quest;s.reward=reward;append(s);}
        }
        objects.sort(Comparator.comparingInt((Selection s)->s.type).thenComparing(s->net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(s.title),String.CASE_INSENSITIVE_ORDER));
        if(nullable)objects.add(0,new Selection(0,"",StatCollector.translateToLocal("ftbquests.null")));
        List<Row> rows=new ArrayList<Row>();for(int i=0;i<objects.size();i++){Selection s=objects.get(i);String type=s.type==FILE?"file":s.type==CHAPTER?"chapter":s.type==QUEST?"quest":s.type==TASK?"task":"reward",color=s.type==FILE?"\u00a7c":s.type==CHAPTER?"\u00a76":s.type==QUEST?"\u00a7a":s.type==TASK?"\u00a79":"\u00a7d";Row row=new Row(i,(s.type==0?"":color)+s.title,s.type==0?"":"object");if(s.type!=0){row.tooltip.add(s.title);row.tooltip.add("\u00a77ID: \u00a78"+s.id);row.tooltip.add("\u00a77"+StatCollector.translateToLocal("ftbquests.type")+": "+color+StatCollector.translateToLocal("ftbquests."+type));if(s.quest!=null)row.tooltip.add("\u00a77"+StatCollector.translateToLocal("ftbquests.quest")+": \u00a7a"+s.quest.title);}rows.add(row);}return rows;
    }
    @Override protected void drawRowIcon(Row row,int x,int y,int size){Selection s=objects.get(row.id);if(s.task!=null)QuestClassicUi.task(mc,s.task,false,x,y,size);else if(s.reward!=null)QuestClassicUi.reward(mc,s.reward,x,y,size);else if(s.quest!=null)QuestClassicUi.quest(mc,s.quest,x,y,size);else if(s.chapter!=null && !s.chapter.icon.isEmpty())QuestClassicUi.item(mc,s.chapter.icon,s.chapter.iconMeta,s.chapter.iconNbt,x,y,size);else QuestClassicUi.item(mc,"minecraft:book",0,"",x,y,size);}
    @Override protected void click(Row row,int button){mc.displayGuiScreen(parent);select.accept(objects.get(row.id));}
}
