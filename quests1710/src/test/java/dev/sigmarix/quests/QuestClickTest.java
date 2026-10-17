package dev.sigmarix.quests;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestClickTest {
    @Test public void clickProtocolsPreserveTheirPayloadAndRejectMalformedLinks() {
        QuestClick command=new QuestClick("command:/help"); assertEquals("command",command.type); assertEquals("/help",command.value);
        assertEquals("",new QuestClick("guide:").value); assertEquals("pack:action",new QuestClick("custom:pack:action").value); assertEquals("",new QuestClick("extension_id").type);
        for(String value:new String[]{"command:","command:/help\n/op player","https://user:pass@example.org","custom:bad","curseforgepages:../secret"}) try { new QuestClick(value); fail(value); } catch(IllegalArgumentException expected) {}
    }
    @Test public void repeatableAndClickQuestsDoNotBlockAggregateCompletion() {
        QuestCore.Quest quest=new QuestCore.Quest(); assertTrue(QuestCore.required(quest)); quest.repeatable=true; assertFalse(QuestCore.required(quest)); quest.repeatable=false; quest.customClick="custom:pack:action"; assertFalse(QuestCore.required(quest));
    }
}
