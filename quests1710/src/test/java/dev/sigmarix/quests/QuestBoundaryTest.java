package dev.sigmarix.quests;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuestBoundaryTest {
    @Test public void largeEditorSaveUsesVanillaSizedPackets() throws Exception {
        Random random=new Random(73);StringBuilder json=new StringBuilder("{\"text\":\"");
        for(int i=0;i<180000;i++) json.append((char)('a'+random.nextInt(26)));
        json.append("\"}");
        java.util.List<QuestNetwork.RequestChunk> chunks=QuestNetwork.requestChunks(json.toString());
        assertTrue(chunks.size()>1);QuestNetwork.RequestAssembly assembly=new QuestNetwork.RequestAssembly();String result=null;
        for(int i=0;i<chunks.size();i++) {
            io.netty.buffer.ByteBuf wire=io.netty.buffer.Unpooled.buffer();
            chunks.get(i).toBytes(wire);assertTrue(wire.readableBytes()+1<32767);
            QuestNetwork.RequestChunk decoded=new QuestNetwork.RequestChunk();decoded.fromBytes(wire);wire.release();
            result=assembly.accept(decoded,100+i);
            if(i<chunks.size()-1) assertNull(result);
        }
        assertEquals(json.toString(),result);
        try { assembly.accept(chunks.get(1),200);fail(); } catch(IOException expected) {}
        assembly.accept(chunks.get(0),0);
        try { assembly.accept(chunks.get(1),30001);fail(); } catch(IOException expected) {}
        assembly.accept(chunks.get(0),40000);
        try { assembly.accept(chunks.get(2),40001);fail(); } catch(IOException expected) {}
        for(QuestNetwork.RequestChunk chunk:chunks) result=assembly.accept(chunk,50000);
        assertEquals(json.toString(),result);
    }
    @Test public void largestWorldMapFitsBoundedWireAndRejectsExtraEntries() throws Exception {
        QuestWorldMap map=new QuestWorldMap(); map.reference="book:";
        for(int i=0;i<4096;i++) map.values.put(String.format("%064x",i),403);
        map.validate(); String json=QuestService.JSON.toJson(map); assertEquals(json,QuestNetwork.decode(QuestNetwork.encode(json)));
        map.values.put("overflow",0); try { map.validate(); fail(); } catch(IllegalArgumentException expected) {}
        map.values.remove("overflow"); map.values.put(String.format("%064x",0),404); try { map.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void invitationAndLeadershipAreEnforced() {
        QuestProgress p=new QuestProgress(); UUID owner=UUID.randomUUID(),member=UUID.randomUUID(),intruder=UUID.randomUUID();
        p.createTeam(owner,"Builders");
        try { p.join(member,owner); fail(); } catch(IllegalArgumentException expected) {}
        try { p.invite(intruder,member); fail(); } catch(IllegalArgumentException expected) {}
        p.invite(owner,member); p.join(member,owner);
        p.complete(member,"shared"); assertTrue(p.completed(owner).contains("shared"));
        try { p.disband(member); fail(); } catch(IllegalArgumentException expected) {}
        NBTTagCompound nbt=new NBTTagCompound(); p.writeToNBT(nbt);
        QuestProgress loaded=new QuestProgress(); loaded.readFromNBT(nbt);
        assertEquals(loaded.group(owner),loaded.group(member)); assertTrue(loaded.leader(owner));
    }
    @Test public void leavingAndRecreatingTeamsDoesNotResetClaims() {
        QuestProgress p=new QuestProgress(); UUID leader=UUID.randomUUID(),member=UUID.randomUUID();
        p.complete(leader,"q"); p.createTeam(leader,"One"); p.invite(leader,member); p.join(member,leader); p.claim(leader,"q");
        p.leave(member); assertTrue(p.claimed(member,"q"));
        p.disband(leader); p.createTeam(leader,"Two"); assertTrue(p.claimed(leader,"q"));
        p.createTeam(member,"Three"); assertTrue(p.claimed(member,"q"));
    }
    @Test public void kickPreservesEarnedProgress() {
        QuestProgress p=new QuestProgress(); UUID leader=UUID.randomUUID(),member=UUID.randomUUID();
        p.createTeam(leader,"One"); p.invite(leader,member); p.join(member,leader); p.complete(leader,"q"); p.claim(leader,"q");
        p.kick(leader,member); assertEquals("",p.team(member)); assertTrue(p.claimed(member,"q")); assertTrue(p.completed(member).contains("q"));
    }
    @Test public void wireRoundTripAndNestingGuard() throws Exception {
        String json="{\"title\":\"Привет [\\\"мир\\\"]\",\"list\":[1,2]}";
        assertEquals(json,QuestNetwork.decode(QuestNetwork.encode(json)));
        StringBuilder nested=new StringBuilder(); for(int i=0;i<65;i++) nested.append('['); for(int i=0;i<65;i++) nested.append(']');
        try { QuestNetwork.guardJson(nested.toString()); fail(); } catch(IOException expected) {}
    }
    @Test public void inflatedPayloadIsBounded() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(GZIPOutputStream zip=new GZIPOutputStream(bytes)) { zip.write(new byte[QuestNetwork.MAX_BYTES+1]); }
        try { QuestNetwork.decode(bytes.toByteArray()); fail(); } catch(IOException expected) {}
    }
    @Test public void namedRandomTableCyclesAndMissingTablesAreRejected() {
        QuestCore.Book book=new QuestCore.Book();QuestCore.Reward random=new QuestCore.Reward();random.type="random";random.table="loop";book.lootTables.put("loop",Collections.singletonList(random));
        try{book.validate();fail();}catch(IllegalArgumentException expected){}
        random.table="missing";try{book.validate();fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void lootCyclesAreRejected() {
        QuestCore.Book book=new QuestCore.Book(); QuestCore.Reward r=new QuestCore.Reward(); r.type="loot"; r.target="loop";
        book.lootTables.put("loop",Collections.singletonList(r));
        try { book.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void unknownTaskNeverSilentlyCompletes() {
        QuestCore.Book book=new QuestCore.Book(); QuestCore.Quest q=new QuestCore.Quest(); q.id="q"; q.title="q";
        QuestCore.Task t=new QuestCore.Task(); t.type="unsupported"; q.tasks.add(t); book.quests.add(q);
        try { book.validate(); fail(); } catch(IllegalArgumentException expected) {}
    }
    @Test public void minimumDependencyCountWorks() {
        QuestCore.Quest q=new QuestCore.Quest(); q.dependencies=Arrays.asList("a","b","c"); q.minDependencies=2;
        assertFalse(QuestCore.unlocked(q,Collections.singleton("a")));
        assertTrue(QuestCore.unlocked(q,new HashSet<String>(Arrays.asList("a","c"))));
    }
}
