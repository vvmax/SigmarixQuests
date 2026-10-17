package dev.sigmarix.quests.preview;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import com.sun.net.httpserver.HttpServer;
/** Local synthetic image endpoint. Never included in production artifacts. */
final class PreviewImageServer {
    private static HttpServer server;
    static String start() throws IOException {
        BufferedImage image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++) for(int x=0;x<16;x++) image.setRGB(x,y,((x/4+y/4)%2==0)?0xFF22BBEE:0xFFFFAA22);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); ImageIO.write(image,"PNG",bytes); byte[] png=bytes.toByteArray();
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); server.createContext("/quest.png",exchange->{ try { exchange.sendResponseHeaders(200,png.length); exchange.getResponseBody().write(png); } finally { exchange.close(); } }); server.start();
        return "http://127.0.0.1:"+server.getAddress().getPort()+"/quest.png";
    }
    static void stop() { if(server!=null) server.stop(0); }
}
