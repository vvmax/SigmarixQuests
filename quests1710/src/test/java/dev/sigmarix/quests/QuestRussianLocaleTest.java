package dev.sigmarix.quests;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestRussianLocaleTest {
    private Map<String,String> read(String domain,String lang)throws Exception {
        Map<String,String> values=new LinkedHashMap<String,String>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/assets/"+domain+"/lang/"+lang+".lang"),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){int i=line.indexOf('=');if(i>0 && !line.startsWith("#"))values.put(line.substring(0,i),line.substring(i+1));}}
        return values;
    }
    private List<String> formats(String s){List<String> list=new ArrayList<String>();Matcher m=Pattern.compile("%(?:[0-9]+\\$)?[sdif]").matcher(s);while(m.find())list.add(m.group());return list;}
    @Test public void everyNativeAndPortKeyHasRussianTextWithMatchingArguments()throws Exception {
        Map<String,String> russian=new LinkedHashMap<String,String>();for(String domain:Arrays.asList("ftbquests","ftblib","sigmarixquests"))russian.putAll(read(domain,"ru_RU"));
        for(String domain:Arrays.asList("ftbquests","ftblib","sigmarixquests"))for(Map.Entry<String,String> e:read(domain,"en_US").entrySet()){
            assertTrue("Missing Russian translation: "+e.getKey(),russian.containsKey(e.getKey()));
            assertEquals("Formatting arguments: "+e.getKey(),formats(e.getValue()),formats(russian.get(e.getKey())));
        }
    }
    @Test public void chatLocalizesTemplatesButPreservesUnknownRegistryIdentifiers(){
        assertTrue(QuestMessages.component("Error: Unknown item: example:untranslated_id") instanceof net.minecraft.util.ChatComponentTranslation);
        assertTrue(QuestMessages.component("available") instanceof net.minecraft.util.ChatComponentTranslation);
        assertTrue(QuestMessages.component("item") instanceof net.minecraft.util.ChatComponentTranslation);
        assertEquals("example:untranslated_id",QuestMessages.component("example:untranslated_id").getUnformattedText());
    }
}
