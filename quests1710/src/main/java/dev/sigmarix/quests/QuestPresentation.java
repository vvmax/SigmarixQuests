package dev.sigmarix.quests;

import java.net.URI;
import java.util.*;

/** Portable presentation schema; resource locations are resolved from installed resource packs. */
public final class QuestPresentation {
    public static final Set<String> SHAPES=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("square","rsquare","circle","diamond","hexagon","octagon","pentagon","heart","gear")));
    public static final class MapImage {
        public String resource="minecraft:textures/blocks/stone.png",uri="";
        public List<String> hover=new ArrayList<String>();
        public double x,y,width=1,height=1,rotation,opacity=1;
        public boolean dev;
    }
    public static final class GuidePage { public String title=""; public List<GuideElement> elements=new ArrayList<GuideElement>(); }
    public static final class GuideElement {
        public String type="text",text="",resource="",item="minecraft:book",nbt="",uri="",color="FFEEEEEE";
        public int width=128,height=64,meta,count=1;
    }
    public static final class Theme {
        public String background="EE17202A",panel="EE26313D",text="FFEEEEEE",edge="FF738CA6",selected="FFFFFFFF";
        public String available="FF397CB2",complete="FFB28B29",claimed="FF3A784E",locked="FF50545A";
        public double questSpacing=1,lineWidth=1;
    }
    public static final class Style {
        public String selector="*",color="",textColor="",shape="";
    }
    public static boolean shape(String value) { return value!=null && value.length()<=2048 && (SHAPES.contains(value) || image(value)); }
    public static int color(String value) {
        if(value==null || !value.matches("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) throw new IllegalArgumentException("Expected RRGGBB or AARRGGBB color");
        String hex=value.startsWith("#")?value.substring(1):value;
        return (int)Long.parseLong(hex.length()==6?"FF"+hex:hex,16);
    }
    public static boolean resource(String value) { return value!=null && (value.isEmpty() || value.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+") && !value.contains("..") && !value.contains("://")); }
    public static boolean image(String value) { return value!=null && !value.isEmpty() && (resource(value) || link(value)); }
    public static boolean link(String value) {
        if(value==null) return false; if(value.isEmpty()) return true;
        try { URI uri=new URI(value); return value.length()<=2048 && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost()!=null && uri.getUserInfo()==null; }
        catch(Exception e) { return false; }
    }
    public static void metadata(String title,String description,String icon,int meta,String nbt) {
        if(title==null || title.length()>256 || description==null || description.length()>16384 || icon==null || icon.length()>256 || meta<0 || nbt==null || nbt.length()>65536) throw new IllegalArgumentException("Invalid task/reward presentation");
    }
    public static String title(QuestCore.Task task) { return task.title.isEmpty()?task.id+" - "+task.type:task.title; }
    public static String title(QuestCore.Reward reward) { return reward.title.isEmpty()?reward.type+" "+(reward.item!=null?reward.item:reward.target==null?"":reward.target)+" x"+reward.count:reward.title; }
    public static void validate(QuestCore.Book book) {
        if(!Double.isFinite(book.gridScale) || book.gridScale<1D/32 || book.gridScale>8 || !shape(book.defaultQuestShape)) throw new IllegalArgumentException("Invalid map scale or default shape");
        if(book.theme==null || book.styles==null || book.styles.size()>128) throw new IllegalArgumentException("Invalid theme");
        Theme t=book.theme;
        if(!Double.isFinite(t.questSpacing) || t.questSpacing<.125 || t.questSpacing>8 || !Double.isFinite(t.lineWidth) || t.lineWidth<0 || t.lineWidth>8) throw new IllegalArgumentException("Invalid theme geometry");
        for(String value:Arrays.asList(t.background,t.panel,t.text,t.edge,t.selected,t.available,t.complete,t.claimed,t.locked)) color(value);
        for(Style style:book.styles) {
            if(style==null || style.selector==null || style.selector.length()>512 || style.selector.trim().isEmpty()) throw new IllegalArgumentException("Invalid style selector");
            selector(style.selector,null,null,null);
            if(style.color==null || style.textColor==null || style.shape==null || !style.shape.isEmpty() && !shape(style.shape)) throw new IllegalArgumentException("Invalid style");
            if(!style.color.isEmpty()) color(style.color); if(!style.textColor.isEmpty()) color(style.textColor);
        }
        for(QuestCore.Chapter chapter:book.chapters) {
            if(chapter.image==null || !chapter.image.isEmpty() && !image(chapter.image) || chapter.defaultQuestShape==null || !chapter.defaultQuestShape.isEmpty() && !shape(chapter.defaultQuestShape) || chapter.images==null || chapter.images.size()>128) throw new IllegalArgumentException("Invalid chapter presentation");
            for(MapImage image:chapter.images) {
                if(image==null || !image(image.resource) || !link(image.uri) || !Double.isFinite(image.x) || !Double.isFinite(image.y) || Math.abs(image.x)>100000 || Math.abs(image.y)>100000 || !Double.isFinite(image.width) || !Double.isFinite(image.height) || image.width<.0625 || image.height<.0625 || image.width>128 || image.height>128 || !Double.isFinite(image.rotation) || Math.abs(image.rotation)>360000 || !Double.isFinite(image.opacity) || image.opacity<0 || image.opacity>1 || image.hover==null || image.hover.size()>32) throw new IllegalArgumentException("Invalid map illustration");
                for(String line:image.hover) if(line==null || line.length()>1024) throw new IllegalArgumentException("Invalid map illustration hover");
            }
        }
        for(QuestCore.Quest q:book.quests) {
            if(!Double.isFinite(q.size) || q.size<0.0625 || q.size>8 || q.iconMeta<0 || q.iconNbt==null || q.tags==null || q.tags.size()>64 || q.pages==null || q.pages.size()>64 || q.shape==null || !q.shape.isEmpty() && !shape(q.shape) || !Arrays.asList("inherit","enabled","disabled").contains(q.itemBrowser) || !link(q.uri)) throw new IllegalArgumentException("Invalid quest presentation: "+q.id);
            for(String tag:q.tags) if(!QuestCore.validId(tag)) throw new IllegalArgumentException("Invalid quest tag");
            if(q.richPages==null || q.richPages.size()>64) throw new IllegalArgumentException("Invalid rich guide pages");
            for(GuidePage page:q.richPages) {
                if(page==null || page.title==null || page.title.length()>256 || page.elements==null || page.elements.size()>128) throw new IllegalArgumentException("Invalid rich guide page");
                for(GuideElement element:page.elements) {
                    if(element==null || !Arrays.asList("text","image","item","link").contains(element.type) || element.text==null || element.text.length()>32768 || element.nbt==null || element.width<1 || element.width>512 || element.height<1 || element.height>512 || element.meta<0 || element.count<1 || element.count>64 || element.resource==null || !element.resource.isEmpty() && !image(element.resource) || !link(element.uri)) throw new IllegalArgumentException("Invalid rich guide element");
                    color(element.color);
                    if(element.type.equals("image") && element.resource.isEmpty() || element.type.equals("link") && element.uri.isEmpty() || element.type.equals("item") && (element.item==null || element.item.isEmpty())) throw new IllegalArgumentException("Missing guide element value");
                }
            }
            for(String page:q.pages) if(page==null || page.length()>32768) throw new IllegalArgumentException("Invalid guide page");
        }
    }
    public static double snap(QuestCore.Book book,double position,double size) { double step=book.gridScale*size; return Math.round(position/step)*step; }
    public static QuestCore.Chapter chapter(QuestCore.Book book,String id) { for(QuestCore.Chapter chapter:book.chapters) if(chapter.id.equals(id)) return chapter; return null; }
    public static boolean within(QuestCore.Book book,String id,String parent) {
        if(parent.isEmpty()) return true;
        for(int depth=0;depth<64;depth++) { if(id.equals(parent)) return true; QuestCore.Chapter chapter=chapter(book,id); if(chapter==null || chapter.parent.isEmpty()) return false; id=chapter.parent; }
        return false;
    }
    public static boolean visibleChapter(QuestCore.Book book,String id) {
        for(int depth=0;depth<64;depth++) { QuestCore.Chapter chapter=chapter(book,id); if(chapter==null) return true; if(chapter.alwaysInvisible) return false; if(chapter.parent.isEmpty()) return true; id=chapter.parent; }
        return false;
    }
    public static boolean itemBrowser(QuestCore.Book book,QuestCore.Quest quest) { return quest.itemBrowser.equals("enabled") || quest.itemBrowser.equals("inherit") && !book.hideFromItemBrowser; }
    public static String shape(QuestCore.Book book,QuestCore.Quest quest) {
        if(!quest.shape.isEmpty()) return quest.shape;
        QuestCore.Chapter current=chapter(book,quest.chapter);
        for(int depth=0;current!=null && depth<64;depth++) { if(!current.defaultQuestShape.isEmpty()) return current.defaultQuestShape; current=chapter(book,current.parent); }
        return book.defaultQuestShape;
    }
    /** Native #id/.tag syntax is retained; legacy theme #tag syntax has a different meaning. */
    public static boolean matches(String expression,QuestCore.Quest q,String state) { return selector(expression,null,q,state); }
    public static boolean matches(String expression,QuestCore.Book book,QuestCore.Quest q,String state) { return selector(expression,book,q,state); }
    private static boolean selector(String expression,QuestCore.Book book,QuestCore.Quest q,String state) {
        if(expression==null || expression.length()>512 || expression.trim().isEmpty()) throw new IllegalArgumentException("Invalid style selector");
        boolean any=false;
        for(String branch:expression.split("\\|",-1)) {
            if(branch.trim().isEmpty()) throw new IllegalArgumentException("Empty selector branch");
            boolean all=true;
            for(String term:branch.trim().split("\\s*&\\s*",-1)) {
                if(term.isEmpty()) throw new IllegalArgumentException("Empty selector term");
                for(String token:term.trim().split("\\s+")) {
                    int negations=0; while(token.startsWith("!")) { negations++; token=token.substring(1); }
                    boolean value;
                    if(token.equals("*") || token.equals("quest")) value=true;
                    else if(token.startsWith("#") && QuestCore.validId(token.substring(1))) value=q!=null && token.substring(1).equals(q.id);
                    else if(token.startsWith(".") && QuestCore.validId(token.substring(1))) value=q!=null && q.tags.contains(token.substring(1));
                    else if(token.startsWith("chapter:") && QuestCore.validId(token.substring(8))) value=q!=null && token.substring(8).equals(q.chapter);
                    else if(token.startsWith("within:") && QuestCore.validId(token.substring(7))) value=q!=null && book!=null && within(book,q.chapter,token.substring(7));
                    else if(Arrays.asList("state:available","state:complete","state:claimed","state:locked").contains(token)) value=token.substring(6).equals(state);
                    else throw new IllegalArgumentException("Unsupported selector: "+token);
                    all &= (negations&1)==0?value:!value;
                }
            }
            any |= all;
        }
        return any;
    }
    public static double mapUnit(QuestCore.Book book) { return 48*book.theme.questSpacing; }
    public static String[] resolve(QuestCore.Book book,QuestCore.Quest q,String state) {
        Theme t=book.theme; String base="claimed".equals(state)?t.claimed:"complete".equals(state)?t.complete:"available".equals(state)?t.available:t.locked;
        String[] result={base,t.text,shape(book,q)};
        for(Style style:book.styles) if(matches(style.selector,book,q,state)) {
            if(!style.color.isEmpty()) result[0]=style.color; if(!style.textColor.isEmpty()) result[1]=style.textColor; if(!style.shape.isEmpty()) result[2]=style.shape;
        }
        return result;
    }
}
