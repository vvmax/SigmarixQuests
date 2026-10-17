package dev.sigmarix.quests;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import com.sun.net.httpserver.HttpServer;
import org.junit.*;
import static org.junit.Assert.*;
public class QuestImageDataTest {
    private HttpServer server; private String base; private byte[] png;
    private static byte[] png(int width,int height) throws IOException { BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB); image.setRGB(0,0,0xFF3388CC); ByteArrayOutputStream output=new ByteArrayOutputStream(); ImageIO.write(image,"PNG",output); return output.toByteArray(); }
    @Before public void start() throws Exception {
        png=png(32,16); server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); base="http://127.0.0.1:"+server.getAddress().getPort();
        server.createContext("/",exchange->{
            String path=exchange.getRequestURI().getPath();
            try {
                if(path.equals("/redirect") || path.equals("/loop") || path.equals("/file")) {
                    exchange.getResponseHeaders().set("Location",path.equals("/redirect")?"/image":path.equals("/file")?"file:///tmp/icon.png":"/loop"); exchange.sendResponseHeaders(302,-1);
                } else if(path.equals("/large")) exchange.sendResponseHeaders(200,QuestImageData.MAX_BYTES+1);
                else { byte[] body=path.equals("/image")?png:new byte[]{1,2,3}; exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); }
            } finally { exchange.close(); }
        }); server.start();
    }
    @After public void stop() { server.stop(0); }
    @Test public void httpImageAndRelativeRedirectDecodeWithoutChangingPixels() throws Exception {
        BufferedImage image=QuestImageData.fetch(base+"/redirect",Proxy.NO_PROXY); assertEquals(32,image.getWidth()); assertEquals(16,image.getHeight()); assertEquals(0xFF3388CC,image.getRGB(0,0));
    }
    @Test public void malformedImagesOversizedResponsesAndUnsafeRedirectsAreRejected() throws Exception {
        for(String path:new String[]{"/bad","/large","/loop","/file"}) try { QuestImageData.fetch(base+path,Proxy.NO_PROXY); fail(path); } catch(IOException expected) {}
        for(String url:new String[]{"file:///tmp/icon.png","https://user:pass@example.com/image","javascript:alert(1)"}) try { QuestImageData.fetch(url,Proxy.NO_PROXY); fail(url); } catch(IOException expected) {}
    }
    @Test public void dimensionsAreCheckedBeforePixelDecodeAndTexturesAreDownscaled() throws Exception {
        try { QuestImageData.decode(png(2049,1)); fail(); } catch(IOException expected) {}
        BufferedImage image=QuestImageData.decode(png(1024,512)); assertEquals(512,image.getWidth()); assertEquals(256,image.getHeight());
        try { QuestImageData.decode(new byte[QuestImageData.MAX_BYTES+1]); fail(); } catch(IOException expected) {}
    }
}
