package dev.sigmarix.quests;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
/** Bounded HTTP image decoding, independent of Minecraft and OpenGL. */
public final class QuestImageData {
    public static final int MAX_BYTES=2*1024*1024,MAX_DIMENSION=2048,TEXTURE_SIZE=512;
    public static BufferedImage fetch(String location,Proxy proxy) throws IOException {
        if(!QuestPresentation.link(location) || location.isEmpty()) throw new IOException("Expected HTTP(S) image URL");
        URL url=new URL(location); long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(12);
        for(int redirects=0;redirects<=4;redirects++) {
            HttpURLConnection connection=(HttpURLConnection)url.openConnection(proxy==null?Proxy.NO_PROXY:proxy);
            connection.setConnectTimeout(3000); connection.setReadTimeout(4000); connection.setInstanceFollowRedirects(false); connection.setRequestProperty("User-Agent","SigmarixQuests/1.7.10");
            try {
                int status=connection.getResponseCode(); if(System.nanoTime()>deadline) throw new IOException("Image download timed out");
                if(status==301 || status==302 || status==303 || status==307 || status==308) {
                    String target=connection.getHeaderField("Location"); if(target==null || redirects==4) throw new IOException("Image redirect limit");
                    URL next=new URL(url,target); if(!QuestPresentation.link(next.toExternalForm())) throw new IOException("Invalid image redirect"); url=next; continue;
                }
                if(status!=200 || connection.getContentLengthLong()>MAX_BYTES) throw new IOException("Image response rejected");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[8192];
                try(InputStream input=connection.getInputStream()) { int count; while((count=input.read(buffer))!=-1) { if(System.nanoTime()>deadline) throw new IOException("Image download timed out"); if(count>MAX_BYTES-bytes.size()) throw new IOException("Image exceeds download limit"); bytes.write(buffer,0,count); } }
                return decode(bytes.toByteArray());
            } finally { connection.disconnect(); }
        }
        throw new IOException("Image redirect limit");
    }
    public static BufferedImage decode(byte[] data) throws IOException {
        if(data.length>MAX_BYTES) throw new IOException("Image exceeds download limit");
        try(MemoryCacheImageInputStream input=new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(input); if(!readers.hasNext()) throw new IOException("Unknown image format"); ImageReader reader=readers.next();
            try {
                reader.setInput(input,true,true); int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || width>MAX_DIMENSION || height>MAX_DIMENSION) throw new IOException("Image dimensions exceed limit");
                ImageReadParam options=reader.getDefaultReadParam(); int sample=Math.max(1,Math.max(width,height)/TEXTURE_SIZE); options.setSourceSubsampling(sample,sample,0,0);
                BufferedImage image=reader.read(0,options); if(image==null) throw new IOException("Image could not be decoded");
                double scale=Math.min(1D,(double)TEXTURE_SIZE/Math.max(image.getWidth(),image.getHeight()));
                if(scale==1) return image;
                BufferedImage resized=new BufferedImage(Math.max(1,(int)(image.getWidth()*scale)),Math.max(1,(int)(image.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=resized.createGraphics(); try { graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR); graphics.drawImage(image,0,0,resized.getWidth(),resized.getHeight(),null); } finally { graphics.dispose(); image.flush(); }
                return resized;
            } finally { reader.dispose(); }
        } catch(RuntimeException ex) { throw new IOException("Invalid image data",ex); }
    }
    private QuestImageData() {}
}
