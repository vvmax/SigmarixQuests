package dev.sigmarix.quests;

import java.net.URI;
import java.util.*;

/** Portable presentation schema; resource locations are resolved from installed resource packs. */
public final class QuestPresentation {
    public static final Set<String> SHAPES=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("square","circle","diamond","hexagon","octagon","pentagon")));
    public static final class Theme {
        public String background="EE17202A",panel="EE26313D",text="FFEEEEEE",edge="FF738CA6",selected="FFFFFFFF";
        public String available="FF397CB2",complete="FFB28B29",claimed="FF3A784E",locked="FF50545A";
    }
    public static final class Style {
        public String selector="*",color="",textColor="",shape="";
    }
    public static int color(String value) {
        if(value==null || !value.matches("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) throw new IllegalArgumentException("Expected RRGGBB or AARRGGBB color");
        String hex=value.startsWith("#")?value.substring(1):value;
        return (int)Long.parseLong(hex.length()==6?"FF"+hex:hex,16);
    }
    public static boolean resource(String value) { return value!=null && (value.isEmpty() || value.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+") && !value.contains("..")); }
    public static boolean link(String value) {
        if(value==null) return false; if(value.isEmpty()) return true;
        try { URI uri=new URI(value); return value.length()<=2048 && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost()!=null && uri.getUserInfo()==null; }
        catch(Exception e) { return false; }
    }
    public static void validate(QuestCore.Book book) {
        if(book.theme==null || book.styles==null || book.styles.size()>128) throw new IllegalArgumentException("Invalid theme");
        Theme t=book.theme;
        for(String value:Arrays.asList(t.background,t.panel,t.text,t.edge,t.selected,t.available,t.complete,t.claimed,t.locked)) color(value);
        for(Style style:book.styles) {
            if(style==null || style.selector==null || style.selector.length()>512 || style.selector.trim().isEmpty()) throw new IllegalArgumentException("Invalid style selector");
            for(String token:style.selector.trim().split("\\s+")) if(!token.equals("*") && !token.equals("quest") && !(token.startsWith("#") && QuestCore.validId(token.substring(1))) && !(token.startsWith(".") && QuestCore.validId(token.substring(1))) && !(token.startsWith("chapter:") && QuestCore.validId(token.substring(8))) && !Arrays.asList("state:available","state:complete","state:claimed","state:locked").contains(token)) throw new IllegalArgumentException("Unsupported selector: "+token);
            if(style.color==null || style.textColor==null || style.shape==null || !style.shape.isEmpty() && !SHAPES.contains(style.shape)) throw new IllegalArgumentException("Invalid style");
            if(!style.color.isEmpty()) color(style.color); if(!style.textColor.isEmpty()) color(style.textColor);
        }
        for(QuestCore.Chapter chapter:book.chapters) if(!resource(chapter.image)) throw new IllegalArgumentException("Invalid chapter image resource");
        for(QuestCore.Quest q:book.quests) {
            if(!Double.isFinite(q.size) || q.size<0.0625 || q.size>8 || q.iconMeta<0 || q.iconNbt==null || q.tags==null || q.tags.size()>64 || q.pages==null || q.pages.size()>64 || !SHAPES.contains(q.shape) || !link(q.uri)) throw new IllegalArgumentException("Invalid quest presentation: "+q.id);
            for(String tag:q.tags) if(!QuestCore.validId(tag)) throw new IllegalArgumentException("Invalid quest tag");
            for(String page:q.pages) if(page==null || page.length()>32768) throw new IllegalArgumentException("Invalid guide page");
        }
    }
    public static boolean matches(String selector,QuestCore.Quest q,String state) {
        for(String token:selector.trim().split("\\s+")) {
            if(token.equals("*") || token.equals("quest")) continue;
            if(token.startsWith("#") && token.substring(1).equals(q.id)) continue;
            if(token.startsWith(".") && q.tags.contains(token.substring(1))) continue;
            if(token.startsWith("chapter:") && token.substring(8).equals(q.chapter)) continue;
            if(token.startsWith("state:") && token.substring(6).equals(state)) continue;
            return false;
        }
        return true;
    }
    public static String[] resolve(QuestCore.Book book,QuestCore.Quest q,String state) {
        Theme t=book.theme; String base="claimed".equals(state)?t.claimed:"complete".equals(state)?t.complete:"available".equals(state)?t.available:t.locked;
        String[] result={base,t.text,q.shape};
        for(Style style:book.styles) if(matches(style.selector,q,state)) {
            if(!style.color.isEmpty()) result[0]=style.color; if(!style.textColor.isEmpty()) result[1]=style.textColor; if(!style.shape.isEmpty()) result[2]=style.shape;
        }
        return result;
    }
}
