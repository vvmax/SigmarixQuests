package dev.sigmarix.quests.packaging;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import com.google.gson.*;

/** Fail-closed client package: replace server entry/state, then retain only client-reachable code. */
public final class ClientJarBuilder implements Opcodes {
    static final String P="dev/sigmarix/quests/";
    final Map<String,ClassNode> classes=new TreeMap<String,ClassNode>();
    final Map<String,byte[]> resources=new TreeMap<String,byte[]>();
    final Set<String> included=new TreeSet<String>(),methods=new HashSet<String>();
    final ArrayDeque<MethodRef> queue=new ArrayDeque<MethodRef>();
    static final class MethodRef { String owner;MethodNode method;MethodRef(String o,MethodNode m){owner=o;method=m;} }
    static byte[] read(InputStream in)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
    public static void main(String[] args)throws Exception {if(args[0].equals("--verify")){verify(Paths.get(args[1]));return;}if(args[0].equals("--mcp")){mcp(Paths.get(args[1]),Paths.get(args[2]),Paths.get(args[3]),Paths.get(args[4]));return;}new ClientJarBuilder().build(Paths.get(args[0]),Paths.get(args[1]),Paths.get(args[2]));}
    static void verify(Path jarPath)throws Exception {
        final Map<String,ClassNode> nodes=new HashMap<String,ClassNode>();
        try(JarFile jar=new JarFile(jarPath.toFile())){Enumeration<JarEntry> entries=jar.entries();while(entries.hasMoreElements()){JarEntry e=entries.nextElement();if(e.getName().endsWith(".class")){ClassNode n=new ClassNode();new ClassReader(read(jar.getInputStream(e))).accept(n,ClassReader.SKIP_DEBUG);nodes.put(n.name,n);}}}
        int checked=0;
        for(ClassNode node:new ArrayList<ClassNode>(nodes.values()))for(Object raw:node.methods){MethodNode method=(MethodNode)raw;for(AbstractInsnNode i=method.instructions.getFirst();i!=null;i=i.getNext()) {
            if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.owner.startsWith(P)){if(!member(nodes,call.owner,call.name,call.desc,true,new HashSet<String>()))throw new IllegalStateException("Missing method from client JAR: "+call.owner+"."+call.name+call.desc+" used by "+key(node.name,method));checked++;}}
            if(i instanceof FieldInsnNode){FieldInsnNode call=(FieldInsnNode)i;if(call.owner.startsWith(P)){if(!member(nodes,call.owner,call.name,call.desc,false,new HashSet<String>()))throw new IllegalStateException("Missing field from client JAR: "+call.owner+"."+call.name+" used by "+key(node.name,method));checked++;}}
        }}
        System.out.println("Client-only internal linkage verified: "+checked+" references");
    }
    static boolean member(Map<String,ClassNode> nodes,String owner,String name,String desc,boolean method,Set<String> visited)throws Exception {
        if(owner==null||!visited.add(owner))return false;ClassNode node=nodes.get(owner);
        if(node==null){if(owner.startsWith(P))return false;InputStream resource=ClientJarBuilder.class.getClassLoader().getResourceAsStream(owner+".class");if(resource==null)return false;node=new ClassNode();try(InputStream in=resource){new ClassReader(read(in)).accept(node,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG);}nodes.put(owner,node);}
        if(method){for(Object raw:node.methods){MethodNode m=(MethodNode)raw;if(m.name.equals(name)&&m.desc.equals(desc))return true;}}
        else for(Object raw:node.fields){FieldNode f=(FieldNode)raw;if(f.name.equals(name)&&f.desc.equals(desc))return true;}
        if(member(nodes,node.superName,name,desc,method,visited))return true;
        for(Object iface:node.interfaces)if(member(nodes,(String)iface,name,desc,method,visited))return true;return false;
    }
    // Development runtime uses MCP names; this changes only Minecraft member names.
    static void mcp(Path input,Path output,Path fields,Path methods)throws Exception {
        final Map<String,String> names=new HashMap<String,String>();
        for(Path csv:new Path[]{fields,methods})for(String line:Files.readAllLines(csv,java.nio.charset.StandardCharsets.UTF_8)){String[] parts=line.split(",",3);if(parts.length>=2)names.put(parts[0],parts[1]);}
        Remapper mapper=new Remapper(){public String mapMethodName(String owner,String name,String desc){return names.containsKey(name)?names.get(name):name;}public String mapFieldName(String owner,String name,String desc){return names.containsKey(name)?names.get(name):name;}};
        Files.createDirectories(output.getParent());try(JarFile jar=new JarFile(input.toFile());JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))){Enumeration<JarEntry> entries=jar.entries();while(entries.hasMoreElements()){
            JarEntry entry=entries.nextElement();if(entry.isDirectory())continue;byte[] data=read(jar.getInputStream(entry));
            if(entry.getName().endsWith(".class")){ClassWriter writer=new ClassWriter(0);new ClassReader(data).accept(new RemappingClassAdapter(writer,mapper),ClassReader.EXPAND_FRAMES);data=writer.toByteArray();}
            write(out,entry.getName(),data);
        }}
    }
    void build(Path input,Path output,Path reportFile)throws Exception {
        try(JarFile jar=new JarFile(input.toFile())) {Enumeration<JarEntry> entries=jar.entries();while(entries.hasMoreElements()) {
            JarEntry entry=entries.nextElement();if(entry.isDirectory())continue;byte[] data=read(jar.getInputStream(entry));
            if(entry.getName().endsWith(".class")){ClassNode node=new ClassNode();new ClassReader(data).accept(node,ClassReader.EXPAND_FRAMES);classes.put(node.name,node);}else resources.put(entry.getName(),data);
        }}
        Map<String,String> aliases=new HashMap<String,String>();
        for(String name:new ArrayList<String>(classes.keySet()))if(name.startsWith(P+"clientpack/QuestClient")) {
            String simple=name.substring((P+"clientpack/QuestClient").length());aliases.put(name,P+"Quest"+simple);
        }
        for(Map.Entry<String,String> alias:aliases.entrySet()) {
            ClassNode remapped=new ClassNode();classes.get(alias.getKey()).accept(new RemappingClassAdapter(remapped,new SimpleRemapper(aliases)));
            classes.put(alias.getValue(),remapped);
        }
        for(String name:aliases.keySet())classes.remove(name);
        patch();
        for(ClassNode node:classes.values()) {
            boolean root=aliases.containsValue(node.name) || node.name.startsWith(P+"client/") || node.name.equals(P+"QuestsMod") || node.name.equals(P+"QuestsMod$QuestBookItem") || node.name.equals(P+"QuestClientBootstrap")
                || node.name.startsWith(P+"QuestBlocks$") || node.name.equals(P+"QuestCrateParcel") || node.name.equals(P+"QuestDevice") || node.name.equals(P+"QuestStorage")
                || node.name.startsWith(P+"QuestDeviceContainer") || node.name.equals(P+"QuestStorageContainer") || node.name.equals(P+"QuestGuiHandler")
                || node.name.equals(P+"QuestNetwork$Payload") || node.name.equals(P+"QuestNetwork$RequestChunk") || node.name.equals(P+"QuestNetwork$ResponseChunk")
                || node.name.equals(P+"QuestNetwork$ResponseHandler") || node.name.equals(P+"QuestNetwork$ResponseChunkHandler") || node.name.equals(P+"QuestNetwork$NoticeHandler") || node.name.equals(P+"QuestNetwork$ClientRequestSink")
                || node.name.startsWith(P+"QuestJson$");
            if(root) {addClass(node.name);for(Object raw:node.methods)addMethod(node.name,(MethodNode)raw);}
        }
        while(!queue.isEmpty())scan(queue.poll());
        List<String> removed=new ArrayList<String>();int removedMethods=0,keptMethods=0;
        for(ClassNode node:classes.values()) {
            if(!included.contains(node.name)){removed.add(node.name.replace('/','.'));continue;}
            Iterator<?> it=node.methods.iterator();while(it.hasNext()){MethodNode m=(MethodNode)it.next();if(!methods.contains(key(node.name,m))) {it.remove();removedMethods++;}else keptMethods++;}
            Iterator<?> inner=node.innerClasses.iterator();while(inner.hasNext()){InnerClassNode n=(InnerClassNode)inner.next();if(n.name.startsWith(P)&&!included.contains(n.name))inner.remove();}
        }
        JsonObject report=new JsonObject();report.addProperty("artifact",output.getFileName().toString());report.addProperty("clientClasses",included.size());report.addProperty("retainedMethods",keptMethods);report.addProperty("removedMethods",removedMethods);report.addProperty("remoteServerVersion","0.28.1-SNAPSHOT");report.addProperty("singleplayerSupported",false);
        JsonArray removedNames=new JsonArray();for(String name:removed)removedNames.add(new JsonPrimitive(name));report.add("excludedClasses",removedNames);
        if(resources.containsKey("mcmod.info")) {
            JsonArray metadata=new JsonParser().parse(new String(resources.get("mcmod.info"),"UTF-8")).getAsJsonArray();
            JsonObject modInfo=metadata.get(0).getAsJsonObject();modInfo.addProperty("name","Sigmarix Quests (client)");modInfo.addProperty("description","Remote-client build. Use the full JAR on the server and for singleplayer.");
            resources.put("mcmod.info",new Gson().toJson(metadata).getBytes("UTF-8"));
        }
        Files.createDirectories(output.getParent());try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(output))) {
            for(Map.Entry<String,byte[]> entry:resources.entrySet())if(!entry.getKey().equals("example-quests.json") && !entry.getKey().startsWith("META-INF/"))write(out,entry.getKey(),entry.getValue());
            for(String name:included) {
                ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);classes.get(name).accept(writer);byte[] data=writer.toByteArray();
                String constants=new String(data,java.nio.charset.StandardCharsets.ISO_8859_1);
                for(String forbidden:new String[]{P+"QuestProgress",P+"QuestScripts",P+"QuestRewards",P+"QuestBookFiles$Loaded","net/minecraft/entity/player/EntityPlayerMP","net/minecraft/server/MinecraftServer"})
                    if(constants.contains(forbidden))throw new IllegalStateException("Server dependency remains in "+name+": "+forbidden);
                write(out,name+".class",data);
            }
            write(out,"META-INF/sigmarix-client-only.json",new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes("UTF-8"));
        }
        Files.createDirectories(reportFile.getParent());Files.write(reportFile,new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes("UTF-8"));
        System.out.println("Client-only JAR: "+included.size()+" classes, "+keptMethods+" methods; excluded "+removed.size()+" classes");
    }
    void patch() {
        ClassNode mod=classes.get(P+"QuestsMod");mod.fields.removeIf(raw->{FieldNode f=(FieldNode)raw;return !f.name.equals("instance")&&!f.name.equals("proxy");});
        mod.methods.removeIf(raw->{MethodNode m=(MethodNode)raw;return !Arrays.asList("<init>","preInit","init").contains(m.name);});
        MethodNode ctor=find(mod,"<init>");body(ctor);ctor.instructions.add(new VarInsnNode(ALOAD,0));ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(RETURN));
        MethodNode pre=find(mod,"preInit");body(pre);pre.instructions.add(new VarInsnNode(ALOAD,0));pre.instructions.add(new VarInsnNode(ALOAD,1));pre.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"QuestClientBootstrap","preInit","(L"+P+"QuestsMod;Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V",false));pre.instructions.add(new InsnNode(RETURN));
        MethodNode guard=new MethodNode(ACC_PUBLIC,"clientServerGuard","(Lcpw/mods/fml/common/event/FMLServerAboutToStartEvent;)V",null,null);guard.visibleAnnotations=new ArrayList<AnnotationNode>();guard.visibleAnnotations.add(new AnnotationNode("Lcpw/mods/fml/common/Mod$EventHandler;"));guard.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"QuestClientBootstrap","rejectServer","()V",false));guard.instructions.add(new InsnNode(RETURN));mod.methods.add(guard);
        keep(P+"QuestBlocks","<init>","<clinit>","registerContent","recipes");
        ClassNode block=classes.get(P+"QuestBlocks$DeviceBlock");
        block.methods.removeIf(raw->Arrays.asList("func_149695_a","func_149689_a","func_149749_a","removedByPlayer","onBlockExploded").contains(((MethodNode)raw).name));
        MethodNode activated=find(block,"func_149727_a");body(activated);activated.instructions.add(new VarInsnNode(ALOAD,0));activated.instructions.add(new FieldInsnNode(GETFIELD,block.name,"mode","I"));
        for(int i=1;i<=6;i++)activated.instructions.add(new VarInsnNode(i==1||i==5?ALOAD:ILOAD,i));
        activated.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"QuestClientBootstrap","activate","(ILnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;I)Z",false));activated.instructions.add(new InsnNode(IRETURN));
        for(String owner:new String[]{"QuestBlocks$LootCrateItem","QuestCrateParcel"}) {MethodNode m=find(classes.get(P+owner),"func_77659_a");body(m);m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new InsnNode(ARETURN));}
        remove(P+"QuestBlocks$LootCrateItem","open");remove(P+"QuestCrateParcel","create","deposit","consume");
        remove(P+"QuestDeviceContainer","func_75142_b");remove(P+"QuestStorageContainer","func_75142_b");
        MethodNode gui=find(classes.get(P+"QuestGuiHandler"),"getServerGuiElement");body(gui);gui.instructions.add(new InsnNode(ACONST_NULL));gui.instructions.add(new InsnNode(ARETURN));
        MethodNode settings=find(classes.get(P+"QuestLoot"),"settings");body(settings);settings.instructions.add(new VarInsnNode(ALOAD,0));settings.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"QuestClientBootstrap","lootSettings",settings.desc,false));settings.instructions.add(new InsnNode(ARETURN));
        remove(P+"QuestDeviceConfig","apply");
        keep(P+"QuestBookFiles","directory");
        remove(P+"QuestNetwork","init","sendResponse","toast","completion","status","taskToast","rewardToast","responseChunks");
    }
    void keep(String owner,String...names){classes.get(owner).methods.removeIf(raw->!Arrays.asList(names).contains(((MethodNode)raw).name));}
    void remove(String owner,String...names){classes.get(owner).methods.removeIf(raw->Arrays.asList(names).contains(((MethodNode)raw).name));}
    static MethodNode find(ClassNode node,String name){for(Object raw:node.methods)if(((MethodNode)raw).name.equals(name))return (MethodNode)raw;throw new IllegalStateException("Missing "+node.name+"."+name);}
    static void body(MethodNode m){m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;m.maxLocals=32;m.maxStack=32;}
    static String key(String owner,MethodNode m){return owner+"."+m.name+m.desc;}
    void addClass(String name){
        if(!classes.containsKey(name)||!included.add(name))return;if(name.indexOf('$')>=0)addClass(name.substring(0,name.indexOf('$')));ClassNode c=classes.get(name);addClass(c.superName);for(Object value:c.interfaces)addClass((String)value);
        for(Object raw:c.fields){FieldNode f=(FieldNode)raw;types(f.desc);types(f.signature);}
        for(Object raw:c.methods){MethodNode m=(MethodNode)raw;if(m.name.equals("<clinit>")||m.name.equals("<init>")&&m.desc.equals("()V")||(c.access&ACC_INTERFACE)!=0)addMethod(name,m);}
    }
    void types(String text){if(text==null)return;Matcher m=Pattern.compile("L(dev/sigmarix/quests/[^;<]+)").matcher(text);while(m.find())addClass(m.group(1));}
    void addMethod(String owner,MethodNode m){addClass(owner);if(methods.add(key(owner,m)))queue.add(new MethodRef(owner,m));}
    void call(String owner,String name,String desc){
        types(desc);ClassNode node=classes.get(owner);if(node==null)return;addClass(owner);
        for(Object raw:node.methods){MethodNode m=(MethodNode)raw;if(m.name.equals(name)&&m.desc.equals(desc)){addMethod(owner,m);return;}}
        if(node.superName!=null)call(node.superName,name,desc);
    }
    void handle(Handle h){addClass(h.getOwner());types(h.getDesc());if(h.getTag()>=H_INVOKEVIRTUAL)call(h.getOwner(),h.getName(),h.getDesc());}
    void scan(MethodRef ref){
        MethodNode m=ref.method;types(m.desc);types(m.signature);
        for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()) {
            if(n instanceof MethodInsnNode){MethodInsnNode i=(MethodInsnNode)n;call(i.owner,i.name,i.desc);}
            else if(n instanceof FieldInsnNode){FieldInsnNode i=(FieldInsnNode)n;addClass(i.owner);types(i.desc);if(i.owner.equals(P+"QuestsMod")&&i.name.equals("service"))throw new IllegalStateException("Client needs server service from "+key(ref.owner,m));}
            else if(n instanceof TypeInsnNode)addClass(((TypeInsnNode)n).desc);
            else if(n instanceof LdcInsnNode){Object value=((LdcInsnNode)n).cst;if(value instanceof Type){Type t=(Type)value;addClass(t.getInternalName());types(t.getDescriptor());}}
            else if(n instanceof InvokeDynamicInsnNode){InvokeDynamicInsnNode i=(InvokeDynamicInsnNode)n;types(i.desc);for(Object value:i.bsmArgs){if(value instanceof Handle)handle((Handle)value);else if(value instanceof Type)types(((Type)value).getDescriptor());}}
            else if(n instanceof MultiANewArrayInsnNode)types(((MultiANewArrayInsnNode)n).desc);
        }
    }
    static void write(JarOutputStream out,String name,byte[] data)throws IOException {JarEntry entry=new JarEntry(name);entry.setTime(0);out.putNextEntry(entry);out.write(data);out.closeEntry();}
}
