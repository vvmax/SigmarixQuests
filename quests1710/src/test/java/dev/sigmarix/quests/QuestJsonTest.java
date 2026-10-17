package dev.sigmarix.quests;
import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestJsonTest {
    @Test public void quantitiesDoNotWrapOrTruncate() {
        for(String json:new String[]{"{count:1.5}","{count:18446744073709551617}","{count:null}","{count:'64'}"}) {
            try { QuestService.JSON.fromJson(json,QuestCore.Task.class); fail(json); } catch(JsonParseException expected) {}
        }
        try { QuestService.JSON.fromJson("{x:4294967297}",QuestNetwork.Action.class); fail(); } catch(JsonParseException expected) {}
    }
    @Test(timeout=2000) public void hugeExponentsFailWithoutExpandingBigIntegers() {
        for(String value:new String[]{"1e999999999","1e-999999999","1e2147483647"}) try { QuestService.JSON.fromJson("{count:"+value+"}",QuestCore.Task.class); fail(); } catch(JsonParseException expected) {}
    }
    @Test public void exactScientificIntegersAndOrdinaryRoundtripsRemainValid() {
        QuestCore.Task task=QuestService.JSON.fromJson("{count:1e3}",QuestCore.Task.class); assertEquals(1000,task.count);
        task.id="valid"; task.type="check"; assertEquals(1000,QuestService.JSON.fromJson(QuestService.JSON.toJson(task),QuestCore.Task.class).count);
        assertEquals(1,new QuestCore.Task().count);
    }
}
