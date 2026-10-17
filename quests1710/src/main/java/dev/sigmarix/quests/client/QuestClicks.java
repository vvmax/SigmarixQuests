package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.io.File;
import java.net.URI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraftforge.common.MinecraftForge;
/** Click actions share checked browser/file confirmation and ordinary player command permissions. */
public final class QuestClicks {
    public static boolean available(String quest,String text) {
        if(text.isEmpty()) return false;
        try { QuestClick click=new QuestClick(text);
        if(click.type.equals("guide")) return QuestGuides.available(click.value);
        if(java.util.Arrays.asList("http","https","command","file","curseforgepages").contains(click.type)) return true;
        QuestClickEvent.Check check=new QuestClickEvent.Check(quest,click.type,click.value); MinecraftForge.EVENT_BUS.post(check); return check.available; }
        catch(RuntimeException ex) { cpw.mods.fml.common.FMLLog.warning("Quest action discovery failed: %s",ex.toString()); return false; }
    }
    public static boolean open(String quest,String text,GuiScreen parent) {
        if(text.isEmpty()) return false;
        try {
            QuestClick click=new QuestClick(text); Minecraft mc=Minecraft.getMinecraft();
            if(click.type.equals("http") || click.type.equals("https")) { confirm(parent,text,null); return true; }
            if(click.type.equals("curseforgepages")) { int split=click.value.indexOf(':'); if(split<0) split=click.value.indexOf('/'); confirm(parent,"https://www.curseforge.com/minecraft/mc-mods/"+click.value.substring(0,split)+"/pages/"+click.value.substring(split+1),null); return true; }
            if(click.type.equals("file")) { File file=file(click.value); confirm(parent,file.toString(),file); return true; }
            if(click.type.equals("guide")) return QuestGuides.open(click.value,parent);
            if(click.type.equals("command")) {
                String command=click.value.startsWith("/")?click.value:"/"+click.value;
                if(net.minecraftforge.client.ClientCommandHandler.instance.executeCommand(mc.thePlayer,command)==0) { QuestNetwork.Action action=new QuestNetwork.Action(); action.action="click_command"; action.id=quest; ClientProxy.send(action); }
                return true;
            }
            QuestClickEvent.Open event=new QuestClickEvent.Open(quest,click.type,click.value,parent); MinecraftForge.EVENT_BUS.post(event); return event.opened;
        } catch(Exception ex) { Minecraft.getMinecraft().thePlayer.addChatMessage(dev.sigmarix.quests.QuestMessages.component("Quest action: "+ex.getMessage())); return false; }
    }
    public static File file(String path) throws java.io.IOException {
        File root=Minecraft.getMinecraft().mcDataDir.getCanonicalFile(),candidate=new File(path); File file=(candidate.isAbsolute()?candidate:new File(root,path)).getCanonicalFile();
        if(!file.toPath().startsWith(root.toPath()) || !file.exists() || !file.isDirectory() && !file.getName().toLowerCase(java.util.Locale.ROOT).matches(".*\\.(txt|json|snbt|nbt|png|jpg|jpeg|gif|pdf|md|log|cfg)")) throw new IllegalArgumentException("File link must refer to a data file or directory inside Minecraft");
        return file;
    }
    private static void confirm(GuiScreen parent,String value,File file) {
        GuiYesNoCallback callback=(yes,id)->{
            if(yes) try { if(file==null && QuestPresentation.link(value)) java.awt.Desktop.getDesktop().browse(new URI(value)); else if(file!=null) java.awt.Desktop.getDesktop().open(file(file.getPath())); } catch(Exception ex) { cpw.mods.fml.common.FMLLog.warning("Quest link failed: %s",ex.toString()); }
            Minecraft.getMinecraft().displayGuiScreen(parent);
        };
        if(file==null) Minecraft.getMinecraft().displayGuiScreen(new GuiConfirmOpenLink(callback,value,0,false));
        else Minecraft.getMinecraft().displayGuiScreen(new GuiYesNo(callback,QuestText.tr("Open local file?"),value,0));
    }
    private QuestClicks() {}
}
