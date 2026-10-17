package dev.sigmarix.quests;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestTimerTest {
    @Test public void nativeUnitsKeepTickPrecisionAndLegacyBookDurations(){assertEquals("5m",QuestTimer.format(6000));assertEquals(1728021,QuestTimer.parse("1d 1s 1t"));assertEquals("1d 1s 1t",QuestTimer.format(1728021));assertEquals(40,QuestTimer.parse("40"));QuestCore.Book book=new QuestCore.Book();book.emergencyCooldownSeconds=9;assertEquals(180,QuestTimer.emergency(book));book.emergencyCooldownTicks=1;assertEquals(1,QuestTimer.emergency(book));}
    @Test public void invalidAndOverflowDurationsAreRejected(){for(String value:new String[]{"","-1s","5q","9223372036854775807w"})try{QuestTimer.parse(value);fail(value);}catch(IllegalArgumentException | ArithmeticException expected){}}
}
