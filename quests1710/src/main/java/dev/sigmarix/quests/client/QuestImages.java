package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
/** Downloads run off-thread; texture creation/deletion stays on the client render thread. */
public final class QuestImages {
    private static final int MAX_CACHE=128;
    private static final ResourceLocation MISSING=new ResourceLocation("sigmarixquests:remote/missing");
    private static final ArrayDeque<Integer> free=new ArrayDeque<Integer>();
    static { for(int i=0;i<MAX_CACHE;i++) free.add(i); }
    private static final Map<String,Entry> cache=new LinkedHashMap<String,Entry>(16,.75f,true);
    private static final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(MAX_CACHE),r->{ Thread thread=new Thread(r,"Quest image download"); thread.setDaemon(true); return thread; },new ThreadPoolExecutor.AbortPolicy());
    private static final class Entry { final int slot; volatile BufferedImage image; volatile boolean failed; ResourceLocation texture; Entry(int slot) { this.slot=slot; } }
    public static ResourceLocation texture(String value) {
        if(Minecraft.getMinecraft().getTextureManager().getTexture(MISSING)==null) Minecraft.getMinecraft().getTextureManager().loadTexture(MISSING,TextureUtil.missingTexture);
        if(QuestPresentation.resource(value)) return value.isEmpty()?MISSING:new ResourceLocation(value);
        if(!QuestPresentation.link(value)) return MISSING;
        Entry entry=cache.get(value);
        if(entry==null) {
            if(cache.size()>=MAX_CACHE) { Iterator<Entry> old=cache.values().iterator(); Entry removed=old.next(); old.remove(); dispose(removed); free.add(removed.slot); }
            entry=new Entry(free.remove()); cache.put(value,entry); final Entry pending=entry; final java.net.Proxy proxy=Minecraft.getMinecraft().getProxy();
            try { workers.execute(()->{ try { pending.image=QuestImageData.fetch(value,proxy); } catch(Exception ex) { pending.failed=true; } }); }
            catch(RejectedExecutionException full) { entry.failed=true; }
        }
        if(entry.texture==null && entry.image!=null) {
            BufferedImage decoded=entry.image; entry.image=null;
            entry.texture=new ResourceLocation("sigmarixquests:remote/slot_"+entry.slot); Minecraft.getMinecraft().getTextureManager().loadTexture(entry.texture,new DynamicTexture(decoded)); decoded.flush();
        }
        return entry.texture==null?MISSING:entry.texture;
    }
    public static boolean loaded(String value) { Entry entry=cache.get(value); return entry!=null && entry.texture!=null; }
    public static void clear() { if(cache.isEmpty()) return; for(Entry entry:cache.values()) dispose(entry); cache.clear(); free.clear(); for(int i=0;i<MAX_CACHE;i++) free.add(i); workers.getQueue().clear(); }
    private static void dispose(Entry entry) { if(entry.texture!=null) { Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.texture); Minecraft.getMinecraft().getTextureManager().loadTexture(entry.texture,TextureUtil.missingTexture); entry.texture=null; } BufferedImage image=entry.image; entry.image=null; if(image!=null) image.flush(); }
    private QuestImages() {}
}
