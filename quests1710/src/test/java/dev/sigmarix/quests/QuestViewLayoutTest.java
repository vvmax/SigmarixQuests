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
    @Test public void chapterIconsRoundTripAndRejectInvalidMetadata() {
        QuestCore.Book b=new QuestCore.Book(); QuestCore.Chapter c=new QuestCore.Chapter(); c.id="start"; c.title="Start"; c.icon="minecraft:diamond"; c.iconMeta=2; c.iconNbt="{name:classic}"; b.chapters.add(c); b.validate();
        com.google.gson.Gson json=new com.google.gson.Gson(); QuestCore.Book copy=json.fromJson(json.toJson(b),QuestCore.Book.class); copy.validate(); assertEquals(c.icon,copy.chapters.get(0).icon); assertEquals(c.iconNbt,copy.chapters.get(0).iconNbt);
        c.iconMeta=-1; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }

}
