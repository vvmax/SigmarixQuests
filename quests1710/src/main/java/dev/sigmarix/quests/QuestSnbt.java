package dev.sigmarix.quests;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.nbt.*;
/** Bounded SNBT reader including typed byte/int arrays for cross-version imports. */
public final class QuestSnbt {
    private final String text; private int at,nodes;
    private QuestSnbt(String text) { if(text.length()>1048576) throw new IllegalArgumentException("SNBT size limit"); this.text=text; }
    public static NBTTagCompound parse(String text) {
        QuestSnbt parser=new QuestSnbt(text); NBTBase root=parser.value(0); parser.space();
        if(!(root instanceof NBTTagCompound) || parser.at!=text.length()) throw parser.error("Expected one compound");
        return (NBTTagCompound)root;
    }
    private IllegalArgumentException error(String reason) { return new IllegalArgumentException(reason+" at SNBT offset "+at); }
    private void space() { while(at<text.length() && Character.isWhitespace(text.charAt(at))) at++; }
    private boolean take(char c) { space(); if(at<text.length() && text.charAt(at)==c) { at++; return true; } return false; }
    private void need(char c) { if(!take(c)) throw error("Expected '"+c+"'"); }
    private String quoted() {
        char quote=text.charAt(at++); StringBuilder result=new StringBuilder();
        while(at<text.length()) {
            char c=text.charAt(at++); if(c==quote) return result.toString();
            if(c=='\\') {
                if(at>=text.length()) throw error("Unterminated escape"); c=text.charAt(at++);
                switch(c) {
                    case 'n': c='\n'; break; case 'r': c='\r'; break; case 't': c='\t'; break;
                    case 'u': if(at+4>text.length()) throw error("Invalid Unicode escape"); try { c=(char)Integer.parseInt(text.substring(at,at+4),16); } catch(NumberFormatException e) { throw error("Invalid Unicode escape"); } at+=4; break;
                    case '\\': case '"': case '\'': break; default: throw error("Invalid escape");
                }
            }
            result.append(c);
        }
        throw error("Unterminated string");
    }
    private String token(boolean key) {
        space(); if(at>=text.length()) throw error("Missing token");
        char c=text.charAt(at); if(c=='"' || c=='\'') return quoted();
        int start=at;
        while(at<text.length()) { c=text.charAt(at); if(Character.isWhitespace(c) || c==',' || c==']' || c=='}' || key && c==':') break; at++; }
        if(at==start) throw error("Empty token"); return text.substring(start,at);
    }
    private NBTBase value(int depth) {
        if(depth>64 || ++nodes>100000) throw error("SNBT complexity limit"); space();
        if(at>=text.length()) throw error("Unexpected end"); char c=text.charAt(at);
        if(c=='{') {
            at++; NBTTagCompound compound=new NBTTagCompound(); if(take('}')) return compound;
            while(true) {
                String key=token(true); if(key.isEmpty() || compound.hasKey(key)) throw error("Empty or duplicate key"); need(':'); compound.setTag(key,value(depth+1));
                if(take('}')) return compound; need(','); if(take('}')) return compound;
            }
        }
        if(c=='[') {
            at++; space(); char kind=0;
            if(at+1<text.length() && text.charAt(at+1)==';') { kind=Character.toUpperCase(text.charAt(at)); at+=2; if(kind!='B' && kind!='I') throw error("Only byte/int arrays exist in Minecraft 1.7.10"); }
            List<NBTBase> values=new ArrayList<NBTBase>();
            if(!take(']')) while(true) { values.add(value(depth+1)); if(take(']')) break; need(','); if(take(']')) break; }
            if(kind!=0) {
                int[] ints=new int[values.size()]; byte[] bytes=new byte[values.size()];
                for(int i=0;i<values.size();i++) {
                    NBTBase v=values.get(i); if(!(v instanceof NBTBase.NBTPrimitive)) throw error("Array requires integer values");
                    long n=((NBTBase.NBTPrimitive)v).func_150291_c();
                    if(v instanceof NBTTagFloat || v instanceof NBTTagDouble || kind=='B' && (n<-128 || n>127) || kind=='I' && (n<Integer.MIN_VALUE || n>Integer.MAX_VALUE)) throw error("Array value outside range");
                    ints[i]=(int)n; bytes[i]=(byte)n;
                }
                return kind=='B'?new NBTTagByteArray(bytes):new NBTTagIntArray(ints);
            }
            NBTTagList list=new NBTTagList(); byte type=values.isEmpty()?0:values.get(0).getId();
            for(NBTBase v:values) { if(v.getId()!=type) throw error("Mixed list types"); list.appendTag(v); } return list;
        }
        if(c=='"' || c=='\'') return new NBTTagString(quoted());
        String word=token(false),lower=word.toLowerCase(Locale.ROOT);
        if(lower.equals("true") || lower.equals("false")) return new NBTTagByte((byte)(lower.equals("true")?1:0));
        try {
            if(lower.matches("[+-]?\\d+b")) return new NBTTagByte(Byte.parseByte(word.substring(0,word.length()-1)));
            if(lower.matches("[+-]?\\d+s")) return new NBTTagShort(Short.parseShort(word.substring(0,word.length()-1)));
            if(lower.matches("[+-]?\\d+l")) return new NBTTagLong(Long.parseLong(word.substring(0,word.length()-1)));
            if(lower.matches("[+-]?\\d+")) return new NBTTagInt(Integer.parseInt(word));
            if(lower.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:e[+-]?\\d+)?[fd]?")) {
                char last=lower.charAt(lower.length()-1); boolean single=last=='f'; String number=last=='f' || last=='d'?word.substring(0,word.length()-1):word;
                double value=Double.parseDouble(number); if(!Double.isFinite(value) || single && !Float.isFinite((float)value)) throw error("Non-finite number");
                return single?new NBTTagFloat((float)value):new NBTTagDouble(value);
            }
        } catch(NumberFormatException e) { throw error("Number outside range"); }
        return new NBTTagString(word);
    }
}
