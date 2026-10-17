package dev.sigmarix.quests;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestViewLayoutTest {
    @Test public void cardFitsEverySupportedGuiScaleAndLongTitles() {
        for(int w:new int[]{320,427,640,1280}) for(int h:new int[]{240,360,720}) {
            QuestViewLayout card=new QuestViewLayout(w,h,800,900,40,80,300,true);
            assertTrue(card.x>=0); assertTrue(card.y>=0); assertTrue(card.x+card.width<=w); assertTrue(card.y+card.height<=h);
            assertTrue(card.contains(card.x,card.y)); assertFalse(card.contains(card.x+card.width,card.y));
        }
    }
    @Test public void cellsUseDisjointColumnsAndCenterIncompleteRows() {
        QuestViewLayout card=new QuestViewLayout(640,360,80,0,12,5,3,false);
        for(int i=0;i<12;i++) { int x=card.cellX(false,i,12); assertTrue(x>card.x); assertTrue(x+18<card.x+card.width/2); }
        for(int i=0;i<5;i++) { int x=card.cellX(true,i,5); assertTrue(x>card.x+card.width/2); assertTrue(x+18<card.x+card.width); }
        assertEquals(card.y+32,card.cellY(0)); assertEquals(card.cellY(0)+20,card.cellY(card.columns));
    }
    @Test public void referenceCardUsesMeasuredTextHeightAndCompactRows() {
        QuestViewLayout card=new QuestViewLayout(427,240,90,0,3,3,4,false,33);
        assertEquals(201,card.width);assertEquals(101,card.height);assertEquals(113,card.x);assertEquals(69,card.y);
        assertEquals(64,card.textTop);assertEquals(card.y+32,card.cellY(false,0));
        QuestCompactGrid five=new QuestCompactGrid(5,36);
        assertEquals(108,five.width);assertEquals(72,five.height);assertEquals(18,five.x(3));assertEquals(54,five.x(4));assertEquals(36,five.y(3));
        QuestCompactGrid ten=new QuestCompactGrid(10,20);
        assertEquals(10,ten.x(0));assertEquals(0,ten.x(3));assertEquals(10,ten.x(7));assertEquals(60,ten.height);
    }
    @Test public void shorterRewardColumnIsCenteredAndLargeListsKeepFourColumns() {
        QuestViewLayout card=new QuestViewLayout(427,240,50,0,12,1,0,false);
        assertEquals(card.y+52,card.cellY(true,0));assertEquals(card.y+32,card.cellY(false,0));
        QuestCompactGrid grid=new QuestCompactGrid(21,20);assertEquals(80,grid.width);assertEquals(120,grid.height);assertEquals(0,grid.x(20));assertEquals(100,grid.y(20));
    }
    @Test public void chapterIconsRoundTripAndRejectInvalidMetadata() {
        QuestCore.Book b=new QuestCore.Book(); QuestCore.Chapter c=new QuestCore.Chapter(); c.id="start"; c.title="Start"; c.icon="minecraft:diamond"; c.iconMeta=2; c.iconNbt="{name:classic}"; b.chapters.add(c); b.validate();
        com.google.gson.Gson json=new com.google.gson.Gson(); QuestCore.Book copy=json.fromJson(json.toJson(b),QuestCore.Book.class); copy.validate(); assertEquals(c.icon,copy.chapters.get(0).icon); assertEquals(c.iconNbt,copy.chapters.get(0).iconNbt);
        c.iconMeta=-1; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }

}
