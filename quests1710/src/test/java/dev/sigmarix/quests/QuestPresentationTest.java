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
    @Test public void richGuidesValidateImagesLinksAndBoundedLayouts() {
        QuestCore.Book b=book(); QuestPresentation.GuidePage page=new QuestPresentation.GuidePage(); b.quests.get(0).richPages.add(page);
        QuestPresentation.GuideElement image=new QuestPresentation.GuideElement(); image.type="image"; image.resource="minecraft:textures/blocks/stone.png"; page.elements.add(image); b.validate();
        image.resource="minecraft:../secret"; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        image.resource="minecraft:textures/blocks/stone.png"; image.width=513; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        image.width=64; QuestPresentation.GuideElement link=new QuestPresentation.GuideElement(); link.type="link"; link.uri="javascript:alert(1)"; page.elements.add(link); try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        link.uri="https://example.org/guide"; b.validate();
    }
    @Test public void chapterGroupsInheritShapesAndRejectCycles() {
        QuestCore.Book b=book(); QuestCore.Chapter parent=new QuestCore.Chapter(); parent.id="parent"; parent.title="Parent"; parent.defaultQuestShape="circle";
        QuestCore.Chapter child=new QuestCore.Chapter(); child.id="child"; child.title="Child"; child.parent="parent"; b.chapters.add(parent); b.chapters.add(child); QuestCore.Quest q=b.quests.get(0); q.chapter="child"; b.validate();
        assertEquals("circle",QuestPresentation.shape(b,q)); assertTrue(QuestPresentation.within(b,"child","parent"));
        child.defaultQuestShape="diamond"; assertEquals("diamond",QuestPresentation.shape(b,q)); q.shape="pentagon"; assertEquals("pentagon",QuestPresentation.shape(b,q));
        parent.alwaysInvisible=true; assertFalse(QuestPresentation.visibleChapter(b,"child")); parent.parent="child";
        try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void mapIllustrationsValidateResourcesLinksAndFiniteGeometry() {
        QuestCore.Book b=book(); QuestCore.Chapter c=new QuestCore.Chapter(); c.id="map"; c.title="Map"; b.chapters.add(c); QuestPresentation.MapImage image=new QuestPresentation.MapImage(); c.images.add(image); b.validate();
        image.width=Double.NaN; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        image.width=1; image.uri="file:///secret"; try { b.validate(); fail(); } catch(IllegalArgumentException expected) {}
        image.uri="https://example.org/guide"; image.resource="https://example.org/image.png"; b.validate();
        assertTrue(QuestPresentation.itemBrowser(b,b.quests.get(0))); b.hideFromItemBrowser=true; assertFalse(QuestPresentation.itemBrowser(b,b.quests.get(0))); b.quests.get(0).itemBrowser="enabled"; assertTrue(QuestPresentation.itemBrowser(b,b.quests.get(0)));
    }
    @Test public void gridScaleSnapsMovementWithoutChangingStoredMapCoordinates() {
        QuestCore.Book b=book(); b.gridScale=.25; b.validate(); assertEquals(.75,QuestPresentation.snap(b,.7,1),0); assertEquals(1,QuestPresentation.snap(b,.8,2),0);
        b.gridScale=1D/32; b.validate(); assertFalse(QuestPresentation.resource("https://example.org/texture.png"));
    }
    @Test public void allClassicBuiltInShapesAndResourceMasksAreAcceptedWithoutOriginalAssets() {
        QuestCore.Book b=book(); for(String shape:java.util.Arrays.asList("rsquare","heart","gear","pack:shapes/star.png","https://example.org/mask.png")) { b.defaultQuestShape=shape; b.quests.get(0).shape=shape; b.validate(); }
    }

}
