package dev.sigmarix.quests;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import net.minecraft.entity.player.EntityPlayerMP;

public final class QuestNetwork {
    public static final int MAX_BYTES=1024*1024, MAX_COMPRESSED=512*1024;
    public static SimpleNetworkWrapper channel;
    public static void init() {
        channel=NetworkRegistry.INSTANCE.newSimpleChannel("sigmarixquests");
        channel.registerMessage(RequestHandler.class, Request.class, 0, Side.SERVER);
        channel.registerMessage(ResponseHandler.class, Response.class, 1, Side.CLIENT);
    }
    public static byte[] encode(String text) throws IOException {
        byte[] raw=text.getBytes(StandardCharsets.UTF_8);
        if(raw.length>MAX_BYTES) throw new IOException("Quest payload too large");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)) { gzip.write(raw); }
        byte[] encoded=bytes.toByteArray();
        if(encoded.length>MAX_COMPRESSED) throw new IOException("Compressed quest payload too large");
        return encoded;
    }
    public static String decode(byte[] encoded) throws IOException {
        if(encoded.length>MAX_COMPRESSED) throw new IOException("Compressed quest payload too large");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(encoded))) {
            byte[] buffer=new byte[4096]; int count;
            while((count=gzip.read(buffer))!=-1) { if(bytes.size()+count>MAX_BYTES) throw new IOException("Inflated quest payload too large"); bytes.write(buffer,0,count); }
        }
        String text=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        guardJson(text); return text;
    }
    public static void guardJson(String text) throws IOException {
        int depth=0; boolean quoted=false,escape=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(quoted) { if(escape) escape=false; else if(c=='\\') escape=true; else if(c=='"') quoted=false; }
            else if(c=='"') quoted=true;
            else if(c=='{' || c=='[') { if(++depth>64) throw new IOException("Quest JSON nesting too deep"); }
            else if(c=='}' || c==']') { if(--depth<0) throw new IOException("Invalid quest JSON"); }
        }
        if(depth!=0 || quoted) throw new IOException("Incomplete quest JSON");
    }
    public static class Payload implements IMessage {
        public String json="";
        public Payload() {} public Payload(String json) { this.json=json; }
        @Override public void toBytes(ByteBuf buffer) {
            try { byte[] data=encode(json); buffer.writeInt(data.length); buffer.writeBytes(data); }
            catch(IOException e) { throw new IllegalArgumentException(e); }
        }
        @Override public void fromBytes(ByteBuf buffer) {
            int count=buffer.readInt();
            if(count<0 || count>MAX_COMPRESSED || count!=buffer.readableBytes()) throw new IllegalArgumentException("Invalid quest payload length");
            byte[] data=new byte[count]; buffer.readBytes(data);
            try { json=decode(data); } catch(IOException e) { throw new IllegalArgumentException(e); }
        }
    }
    public static final class Request extends Payload { public Request() {} public Request(String json) { super(json); } }
    public static final class Response extends Payload { public Response() {} public Response(String json) { super(json); } }
    public static final class RequestHandler implements IMessageHandler<Request,IMessage> {
        @Override public IMessage onMessage(Request message, MessageContext context) {
            final EntityPlayerMP player=context.getServerHandler().playerEntity;
            // Bounded queue. Neither Netty thread mutates the world nor client thread executes server code.
            QuestsMod.enqueue(player,message.json); return null;
        }
    }
    public static final class ResponseHandler implements IMessageHandler<Response,IMessage> {
        @Override public IMessage onMessage(Response message,MessageContext context) { QuestsMod.proxy.receive(message.json); return null; }
    }
    public static final class Action {
        public String action="view", id="", text="";
        public long revision;
        public int[] choices=new int[0];
        public QuestCore.Book book;
    }
    public static final class Snapshot {
        public QuestCore.Book book;
        public long revision;
        public boolean canEdit;
        public String message="", team="";
        public java.util.Map<String,String> states=new java.util.LinkedHashMap<String,String>();
        public java.util.Map<String,Long> counters=new java.util.LinkedHashMap<String,Long>();
    }
}
