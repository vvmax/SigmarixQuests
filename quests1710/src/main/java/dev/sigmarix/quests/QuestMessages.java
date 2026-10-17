package dev.sigmarix.quests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.util.*;

/** Localizes legacy UI/status strings without modifying registry IDs or stored book data. */
public final class QuestMessages {
    private static final Map<String,String> KEYS=new LinkedHashMap<String,String>();
    private static final List<String> PREFIXES=new ArrayList<String>();
    static {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(QuestMessages.class.getResourceAsStream("/assets/sigmarixquests/lang/en_US.lang"),StandardCharsets.UTF_8))){
            String line;while((line=reader.readLine())!=null){int equals=line.indexOf('=');if(equals<=0 || line.startsWith("#"))continue;String key=line.substring(0,equals),value=line.substring(equals+1);if(key.startsWith("quests.text.") || key.startsWith("quests.ui."))KEYS.put(key.endsWith(".prefix")?value.substring(0,value.length()-2).replace("%%","%"):value,key);}
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
        for(String text:KEYS.keySet())if(text.endsWith(" ") && !text.trim().isEmpty())PREFIXES.add(text);
        Collections.sort(PREFIXES,(a,b)->Integer.compare(b.length(),a.length()));
    }
    private static String prefix(String text){for(String prefix:PREFIXES)if(text.startsWith(prefix))return prefix;return null;}
    public static String translate(String text){
        if(text==null || text.isEmpty())return text;
        int codes=0;while(codes+1<text.length() && text.charAt(codes)=='\u00a7')codes+=2;
        if(codes>0)return text.substring(0,codes)+translate(text.substring(codes));
        String key=KEYS.get(text);if(key!=null)return key.endsWith(".prefix")?StatCollector.translateToLocalFormatted(key,""):StatCollector.translateToLocal(key);
        String prefix=prefix(text);return prefix==null?text:StatCollector.translateToLocalFormatted(KEYS.get(prefix),translate(text.substring(prefix.length())));
    }
    public static IChatComponent component(String text){
        if(text==null)return new ChatComponentText("");
        String key=KEYS.get(text);if(key!=null)return key.endsWith(".prefix")?new ChatComponentTranslation(key,""):new ChatComponentTranslation(key);
        String prefix=prefix(text);return prefix==null?new ChatComponentText(text):new ChatComponentTranslation(KEYS.get(prefix),component(text.substring(prefix.length())));
    }
    private QuestMessages(){}
}
