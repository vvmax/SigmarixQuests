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
    public static final int MAX_BOOK_BYTES=16*1024*1024, MAX_BYTES=32*1024*1024, MAX_COMPRESSED=16*1024*1024;
    public static final long ASSEMBLY_TIMEOUT_MS=120000;
    public static SimpleNetworkWrapper channel;
    public static void init() {
        channel=NetworkRegistry.INSTANCE.newSimpleChannel("sigmarixquests");
        channel.registerMessage(RequestHandler.class, Request.class, 0, Side.SERVER);
        channel.registerMessage(ResponseHandler.class, Response.class, 1, Side.CLIENT);
        channel.registerMessage(NoticeHandler.class, Notice.class, 2, Side.CLIENT);
        channel.registerMessage(RequestChunkHandler.class, RequestChunk.class, 3, Side.SERVER);
        channel.registerMessage(ResponseChunkHandler.class, ResponseChunk.class, 4, Side.CLIENT);
    }
    public static void initClient() {
        channel=NetworkRegistry.INSTANCE.newSimpleChannel("sigmarixquests");
        ClientRequestSink sink=new ClientRequestSink();
        channel.registerMessage(sink,Request.class,0,Side.SERVER);
        channel.registerMessage(ResponseHandler.class,Response.class,1,Side.CLIENT);
        channel.registerMessage(NoticeHandler.class,Notice.class,2,Side.CLIENT);
        channel.registerMessage(new ClientChunkSink(),RequestChunk.class,3,Side.SERVER);
        channel.registerMessage(ResponseChunkHandler.class,ResponseChunk.class,4,Side.CLIENT);
    }
    public static class ClientRequestSink implements IMessageHandler<IMessage,IMessage> {
        public IMessage onMessage(IMessage message,MessageContext context) { throw new IllegalStateException("Client-only quest build cannot handle server actions"); }
    }
    public static final class ClientChunkSink extends ClientRequestSink {}
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
    // C17PacketCustomPayload has a 32767-byte limit, including the FML discriminator.
    public static final int CHUNK_BYTES=30000;
    public static java.util.List<RequestChunk> requestChunks(String json) { return chunks(json,false); }
    public static java.util.List<RequestChunk> responseChunks(String json) { return chunks(json,true); }
    private static java.util.List<RequestChunk> chunks(String json,boolean response) {
        try {
            byte[] encoded=encode(json);
            java.util.List<RequestChunk> chunks=new java.util.ArrayList<RequestChunk>();
            for(int offset=0;offset<encoded.length;offset+=CHUNK_BYTES) {
                RequestChunk chunk=response?new ResponseChunk():new RequestChunk();chunk.total=encoded.length;chunk.offset=offset;
                chunk.data=java.util.Arrays.copyOfRange(encoded,offset,Math.min(offset+CHUNK_BYTES,encoded.length));
                chunks.add(chunk);
            }
            return chunks;
        } catch(IOException e) { throw new IllegalArgumentException(e); }
    }
    public static void sendRequest(String json) {
        for(RequestChunk chunk:requestChunks(json)) channel.sendToServer(chunk);
    }
    public static void sendResponse(String json,EntityPlayerMP player) {
        for(RequestChunk chunk:responseChunks(json)) channel.sendTo((ResponseChunk)chunk,player);
    }
    public static class RequestChunk implements IMessage {
        public int total,offset; public byte[] data=new byte[0];
        public void toBytes(ByteBuf buffer) { validate();buffer.writeInt(total);buffer.writeInt(offset);buffer.writeBytes(data); }
        public void fromBytes(ByteBuf buffer) {
            if(buffer.readableBytes()<8 || buffer.readableBytes()>CHUNK_BYTES+8) throw new IllegalArgumentException("Invalid quest chunk size");
            total=buffer.readInt();offset=buffer.readInt();data=new byte[buffer.readableBytes()];buffer.readBytes(data);validate();
        }
        private void validate() {
            if(total<=0 || total>MAX_COMPRESSED || offset<0 || data.length==0 || data.length>CHUNK_BYTES || offset>total-data.length)
                throw new IllegalArgumentException("Invalid quest chunk bounds");
        }
    }
    public static final class RequestAssembly {
        private byte[] data; private int received; private long started;
        int retainedBytes() { return data==null?0:data.length; }
        boolean expired(long now) { return data==null || now-started>ASSEMBLY_TIMEOUT_MS; }
        public String accept(RequestChunk chunk,long now) throws IOException {
            chunk.validate();
            if(chunk.offset==0) { data=new byte[chunk.total];received=0;started=now; }
            if(data==null || now-started>ASSEMBLY_TIMEOUT_MS || data.length!=chunk.total || received!=chunk.offset) {
                data=null;throw new IOException("Invalid or expired quest chunk sequence");
            }
            System.arraycopy(chunk.data,0,data,received,chunk.data.length);received+=chunk.data.length;
            if(received<data.length) return null;
            byte[] complete=data;data=null;return decode(complete);
        }
    }
    public static final class ResponseChunk extends RequestChunk {}
    public static final class ResponseChunkHandler implements IMessageHandler<ResponseChunk,IMessage> {
        private final java.util.Map<net.minecraft.network.INetHandler,RequestAssembly> pending=new java.util.WeakHashMap<net.minecraft.network.INetHandler,RequestAssembly>();
        public synchronized IMessage onMessage(ResponseChunk message,MessageContext context) {
            net.minecraft.network.INetHandler connection=context.netHandler;
            RequestAssembly assembly=pending.get(connection);
            if(assembly==null) { assembly=new RequestAssembly();pending.put(connection,assembly); }
            try {
                String json=assembly.accept(message,System.currentTimeMillis());
                if(json!=null) { pending.remove(connection);QuestsMod.proxy.receive(json); }
            } catch(IOException | IllegalArgumentException e) { pending.remove(connection);throw new IllegalArgumentException("Invalid quest response",e); }
            return null;
        }
    }
    public static final class RequestChunkHandler implements IMessageHandler<RequestChunk,IMessage> {
        private final java.util.Map<EntityPlayerMP,RequestAssembly> pending=new java.util.WeakHashMap<EntityPlayerMP,RequestAssembly>();
        public synchronized IMessage onMessage(RequestChunk message,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().playerEntity;
            long now=System.currentTimeMillis();
            pending.entrySet().removeIf(entry->entry.getValue().expired(now));
            // A forged first chunk must not reserve an unbounded amount of server memory.
            if(message.offset==0) {
                long retained=0;for(java.util.Map.Entry<EntityPlayerMP,RequestAssembly> entry:pending.entrySet())
                    if(entry.getKey()!=player) retained+=entry.getValue().retainedBytes();
                if(retained+message.total>2L*MAX_COMPRESSED) return null;
            }
            RequestAssembly assembly=pending.get(player);
            if(assembly==null) { assembly=new RequestAssembly();pending.put(player,assembly); }
            try {
                String json=assembly.accept(message,System.currentTimeMillis());
                if(json!=null) { pending.remove(player);QuestsMod.enqueue(player,json); }
            } catch(IOException | IllegalArgumentException e) { pending.remove(player); }
            return null;
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
    public static final class Notice extends Payload { public Notice() {} public Notice(String json) { super(json); } }
    public static final class NoticeHandler implements IMessageHandler<Notice,IMessage> {
        @Override public IMessage onMessage(Notice message,MessageContext context) { QuestsMod.proxy.notice(message.json); return null; }
    }
    public static final class Toast {
        public boolean status;
        public String title="",text="",icon="minecraft:book",iconNbt="",completion="";
        public QuestCore.Quest quest;
        public int iconMeta;
        public QuestCore.Task task;
        public QuestCore.Reward reward;
    }
    public static void toast(EntityPlayerMP player,String title,String text,String icon) {
        toast(player,title,text,icon,0,"");
    }
    public static void toast(EntityPlayerMP player,String title,String text,String icon,int meta,String nbt) {
        if(channel==null || QuestEvents.notificationsMuted()) return;
        Toast toast=new Toast(); toast.title=title; toast.text=text; toast.icon=icon; toast.iconMeta=meta; toast.iconNbt=nbt;
        channel.sendTo(new Notice(QuestService.JSON.toJson(toast)),player);
    }
    public static void completion(EntityPlayerMP player,String kind,String title,String icon,int meta,String nbt,QuestCore.Quest source){if(channel==null || QuestEvents.notificationsMuted())return;Toast toast=new Toast();toast.completion=kind;toast.title=title;toast.text=kind.equals("quest")?"Quest complete":kind+" complete";toast.icon=icon;toast.iconMeta=meta;toast.iconNbt=nbt;if(source!=null){toast.quest=new QuestCore.Quest();toast.quest.id=source.id;toast.quest.title=source.title;toast.quest.icon=source.icon;toast.quest.iconMeta=source.iconMeta;toast.quest.iconNbt=source.iconNbt;if(!source.tasks.isEmpty())toast.quest.tasks.add(source.tasks.get(0));}channel.sendTo(new Notice(QuestService.JSON.toJson(toast)),player);}
    public static void status(EntityPlayerMP player,String key,long value){if(channel==null)return;Toast toast=new Toast();toast.status=true;toast.title=key;toast.text=Long.toString(value);channel.sendTo(new Notice(QuestService.JSON.toJson(toast)),player);}
    public static void taskToast(EntityPlayerMP player,QuestCore.Quest quest,QuestCore.Task task) {
        if(channel==null || QuestEvents.notificationsMuted()) return;
        Toast toast=new Toast(); toast.title=QuestPresentation.title(task); toast.text="Task complete";
        toast.task=QuestService.JSON.fromJson(QuestService.JSON.toJson(task),QuestCore.Task.class);
        toast.icon=task.icon.isEmpty()?quest.icon:task.icon; toast.iconMeta=task.icon.isEmpty()?quest.iconMeta:task.iconMeta; toast.iconNbt=task.icon.isEmpty()?quest.iconNbt:task.iconNbt;
        channel.sendTo(new Notice(QuestService.JSON.toJson(toast)),player);
    }
    public static void rewardToast(EntityPlayerMP player,QuestCore.Reward source){rewardToast(player,source,source.count);}
    public static void rewardToast(EntityPlayerMP player,QuestCore.Reward source,int deliveredCount){
        if(channel==null || QuestEvents.notificationsMuted() || source.type.equals("toast"))return;
        Toast toast=new Toast();toast.text="Reward collected";toast.reward=new QuestCore.Reward();QuestCore.Reward r=toast.reward;
        r.type=source.type;r.title=source.title;r.table=source.table;r.target=source.target;r.description=source.description;r.text=source.text;r.item=source.item;r.meta=source.meta;r.nbt=source.nbt;r.count=deliveredCount;r.randomBonus=0;r.icon=source.icon;r.iconMeta=source.iconMeta;r.iconNbt=source.iconNbt;
        toast.icon=source.icon.isEmpty()?(source.type.equals("item")?source.item:source.type.startsWith("xp")?"minecraft:experience_bottle":"minecraft:paper"):source.icon;toast.iconMeta=source.icon.isEmpty()?source.meta:source.iconMeta;toast.iconNbt=source.icon.isEmpty()?source.nbt:source.iconNbt;
        channel.sendTo(new Notice(QuestService.JSON.toJson(toast)),player);
    }
    public static final class Action {
        public String action="view", id="", text="";
        public long revision;
        public int[] choices=new int[0];
        public int x,y,z;
        public QuestCore.Book book;
        public QuestJsonDelta.Patch bookDelta;
        public TeamSettings teamSettings;
        public QuestDeviceConfig deviceSettings;
    }
    public static final class Rank { public String name; public int completed,total; }
    public static final class TeamSettings {
        public String title="",description="",fakePlayerStatus="none";
        public int color=0x0094FF;public boolean freeToJoin;
    }
    public static final class TeamPlayer {public String id="",name="",status="none";public boolean requesting,online;}
    public static final class TeamEntry {public String id="",name="",title="",description="",owner="";public int color=0x0094FF;public boolean freeToJoin,invited,requesting,enemy;}
    public static final class Snapshot {
        public QuestWorldMap screen;
        public QuestCore.Book book;
        public long revision;
        public long emergencyWait;
        public boolean canEdit,teamLeader,teamModerator,autoPin;
        public String teamId="";
        public TeamSettings teamSettings=new TeamSettings();
        public java.util.List<TeamPlayer> teamPlayers=new java.util.ArrayList<TeamPlayer>();
        public java.util.List<TeamEntry> teams=new java.util.ArrayList<TeamEntry>();
        public int teamColor=0x0094FF;
        public java.util.List<String> pinned=new java.util.ArrayList<String>();
        public java.util.Map<String,Integer> chapterPercentages=new java.util.LinkedHashMap<String,Integer>();
        public java.util.Map<String,Integer> percentages=new java.util.LinkedHashMap<String,Integer>();
        public java.util.List<String> teamMembers=new java.util.ArrayList<String>(),teamInvites=new java.util.ArrayList<String>();
        public java.util.List<Rank> leaderboard=new java.util.ArrayList<Rank>();
        public String message="", team="", profile="default";
        public java.util.Map<String,String> states=new java.util.LinkedHashMap<String,String>();
        public java.util.Map<String,String> chapterStates=new java.util.LinkedHashMap<String,String>();
        public java.util.Map<String,Boolean> rewardStates=new java.util.LinkedHashMap<String,Boolean>();
        public java.util.Map<String,Boolean> taskStates=new java.util.LinkedHashMap<String,Boolean>();
        public java.util.Map<String,Long> counters=new java.util.LinkedHashMap<String,Long>();
    }
}
