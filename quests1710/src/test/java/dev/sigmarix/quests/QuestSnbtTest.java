package dev.sigmarix.quests;
import net.minecraft.nbt.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestSnbtTest {
    @org.junit.Test public void nativeWriterRoundTripsListsArraysAndEscapedKeysWithoutLegacyIndexes(){net.minecraft.nbt.NBTTagCompound original=QuestSnbt.parse("{\"a b\":\"He said \\\"yes\\\"\",list:[{id:1},{id:2}],bytes:[B;1b,-2b],ints:[I;3,4]}");String text=QuestSnbt.write(original);org.junit.Assert.assertEquals(original,QuestSnbt.parse(text));org.junit.Assert.assertFalse(text.contains(",}"));org.junit.Assert.assertFalse(text.contains("[0:"));org.junit.Assert.assertTrue(text.contains("[B;1B,-2B]"));org.junit.Assert.assertEquals(2,original.getTagList("list",10).tagCount());}
    @Test public void nestedTagsAndTypedArraysPreserveTypes() {
        NBTTagCompound c=QuestSnbt.parse("{name:'Привет', count:3L, nested:{enabled:true}, bytes:[B;1b,-2b], ints:[I;2,3], list:[{key:1},{key:2}]}");
        assertEquals("Привет",c.getString("name")); assertArrayEquals(new byte[]{1,-2},c.getByteArray("bytes")); assertArrayEquals(new int[]{2,3},c.getIntArray("ints")); assertEquals(2,c.getTagList("list",10).tagCount()); assertEquals(4,c.getTag("count").getId());
    }
    @Test public void malformedOrUnrepresentableTagsFailExplicitly() {
        for(String bad:new String[]{"{x:1,x:2}","{x:[1,2b]}","{x:[L;1L]}","{x:129b}","{x:[B;128]}","{x:1e999}","{x:1} trailing"}) {
            try { QuestSnbt.parse(bad); fail(bad); } catch(IllegalArgumentException expected) {}
        }
    }
    @Test public void quotedRegistryIdsAndEscapesWork() { NBTTagCompound c=QuestSnbt.parse("{id:minecraft:stone,text:\"line\\nnext\\u0021\",fraction:1.25f}"); assertEquals("minecraft:stone",c.getString("id")); assertEquals("line\nnext!",c.getString("text")); assertEquals(1.25,c.getFloat("fraction"),0); }
}
