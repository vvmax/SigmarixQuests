package dev.sigmarix.quests;
import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestPresentationTest {
    private QuestCore.Book book() {
        QuestCore.Book b=new QuestCore.Book(); QuestCore.Quest q=new QuestCore.Quest(); q.id="a"; q.title="A";
        QuestCore.Task t=new QuestCore.Task(); t.type="check"; q.tasks.add(t); b.quests.add(q); return b;
    }
    @Test public void themeRulesMatchCombinedTagsChapterAndState() {
        QuestCore.Book b=book(); QuestCore.Quest q=b.quests.get(0); q.tags.add("important");
        QuestPresentation.Style rule=new QuestPresentation.Style(); rule.selector="quest #a .important state:available"; rule.color="FF123456"; rule.shape="diamond"; b.styles.add(rule); b.validate();
        assertEquals("FF123456",QuestPresentation.resolve(b,q,"available")[0]); assertEquals("diamond",QuestPresentation.resolve(b,q,"available")[2]);
        assertEquals(b.theme.locked,QuestPresentation.resolve(b,q,"locked")[0]);
    }
    @Test public void resourceAndUriValidationRejectExecutableAndTraversalInputs() {
        assertTrue(QuestPresentation.link("https://example.org/guide")); assertFalse(QuestPresentation.link("file:///etc/passwd")); assertFalse(QuestPresentation.link("javascript:alert(1)"));
        assertFalse(QuestPresentation.resource("demo:../secret")); assertTrue(QuestPresentation.resource("demo:textures/guide.png"));
    }
    @Test public void invalidSelectorsAndColorsFailBookValidation() {
        QuestCore.Book b=book(); b.theme.edge="oops"; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        b=book(); QuestPresentation.Style s=new QuestPresentation.Style(); s.selector="unsupported:state"; b.styles.add(s); try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void copiedQuestGetsNewIdAndDoesNotMutateOriginal() {
        JsonObject original=QuestEditing.template("quests").getAsJsonObject(),copy=QuestEditing.duplicate(original).getAsJsonObject();
        assertNotEquals(original.get("id"),copy.get("id")); copy.addProperty("title","Changed"); assertEquals("New quest",original.get("title").getAsString());
        assertTrue(copy.getAsJsonArray("tasks").get(0).getAsJsonObject().has("target"));
    }
    @Test public void draftReorderAndDeletePreserveRemainingObjects() {
        JsonArray a=new JsonArray(); a.add(new JsonPrimitive("a")); a.add(new JsonPrimitive("b")); a.add(new JsonPrimitive("c"));
        QuestEditing.move(a,0,1); assertEquals("b",a.get(0).getAsString()); QuestEditing.remove(a,1); assertEquals(2,a.size()); assertEquals("c",a.get(1).getAsString());
    }
    @Test public void colorConversionAcceptsRgbAndArgb() { assertEquals(0xFF112233,QuestPresentation.color("#112233")); assertEquals(0x80112233,QuestPresentation.color("80112233")); }
    @Test public void invalidNodeSizesAndIconDataAreRejected() {
        QuestCore.Book b=book(); b.quests.get(0).size=Double.NaN; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        b=book(); b.quests.get(0).size=9; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
}
