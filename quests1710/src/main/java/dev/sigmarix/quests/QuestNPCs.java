package dev.sigmarix.quests;
import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayerMP;
/** Public API adapter for official CustomNPCs 1.7.10d. Optional classes live in a lazy adapter. */
public final class QuestNPCs {
    public static int id(String value) {
        if(value==null || !value.matches("[0-9]{1,10}")) throw new IllegalArgumentException("Expected a nonnegative CustomNPCs ID");
        try { return Integer.parseInt(value); } catch(NumberFormatException e) { throw new IllegalArgumentException("CustomNPCs ID exceeds integer range",e); }
    }
    public static void require() {
        if(!Loader.isModLoaded("customnpcs")) throw new IllegalArgumentException("NPC tasks and rewards require CustomNPCs 1.7.10d");
    }
    public static boolean task(EntityPlayerMP player,QuestCore.Task task) { require(); return Adapter.task(player,task); }
    public static void validate(QuestCore.Reward reward) { require(); Adapter.validate(reward); }
    public static int points(EntityPlayerMP player,String faction) { require(); return Adapter.points(player,id(faction)); }
    public static void preflight(QuestCore.Reward reward) { require(); Adapter.references(reward); }
    public static void grant(EntityPlayerMP player,QuestCore.Reward reward) { require(); Adapter.grant(player,reward); }
    private static final class Adapter {
        static int points(EntityPlayerMP player,int faction) { return new noppes.npcs.scripted.ScriptPlayer(player).getFactionPoints(faction); }
        static boolean task(EntityPlayerMP player,QuestCore.Task task) {
            noppes.npcs.scripted.ScriptPlayer api=new noppes.npcs.scripted.ScriptPlayer(player);
            int key=id(task.target);
            switch(task.type) {
                case "npc_quest": return task.checkActive?api.hasActiveQuest(key):api.hasFinishedQuest(key);
                case "npc_dialog": return api.hasReadDialog(key);
                case "npc_faction": return QuestAttributes.compare(api.getFactionPoints(key),task.points,task.comparison);
                default: throw new IllegalArgumentException("Unknown NPC task");
            }
        }
        static noppes.npcs.controllers.PlayerMail mail(QuestCore.Reward reward) {
            net.minecraft.nbt.NBTTagCompound source=QuestService.tag(reward.nbt);
            net.minecraft.nbt.NBTTagList entries=source.getTagList("MailItems",10);
            for(int i=0;i<entries.tagCount();i++) {
                net.minecraft.nbt.NBTTagCompound entry=entries.getCompoundTagAt(i);
                if(entry.getByte("Slot")<0 || entry.getByte("Slot")>=4 || net.minecraft.item.ItemStack.loadItemStackFromNBT(entry)==null) throw new IllegalArgumentException("Invalid legacy NPC mail item; use attachments with registry IDs");
            }
            noppes.npcs.controllers.PlayerMail mail=new noppes.npcs.controllers.PlayerMail(); mail.readNBT(source);
            for(QuestCore.MailItem entry:reward.attachments) {
                net.minecraft.item.Item item=QuestService.item(entry.item);
                if(entry.count>item.getItemStackLimit() || mail.items[entry.slot]!=null) throw new IllegalArgumentException("Invalid or occupied NPC mail attachment slot");
                mail.items[entry.slot]=new net.minecraft.item.ItemStack(item,entry.count,entry.meta); mail.items[entry.slot].setTagCompound(QuestService.tag(entry.nbt));
            }
            return mail;
        }
        static void validate(QuestCore.Reward reward) {
            if(reward.type.equals("npc_mail")) {
                noppes.npcs.controllers.PlayerMail mail=mail(reward);
                if(mail.subject==null || mail.subject.isEmpty() || mail.sender==null || mail.sender.isEmpty()) throw new IllegalArgumentException("NPC mail requires Subject and Sender");
            }
        }
        static void references(QuestCore.Reward reward) {
            if(reward.type.equals("npc_mail")) {
                noppes.npcs.controllers.PlayerMail mail=mail(reward);
                if(mail.questId>=0 && !noppes.npcs.controllers.QuestController.instance.quests.containsKey(mail.questId)) throw new IllegalArgumentException("Unknown NPC mail quest");
            } else if(reward.type.equals("npc_quest")) {
                if(!noppes.npcs.controllers.QuestController.instance.quests.containsKey(id(reward.target))) throw new IllegalArgumentException("Unknown NPC quest: "+reward.target);
            } else if(noppes.npcs.controllers.FactionController.getInstance().getFaction(id(reward.target))==null) throw new IllegalArgumentException("Unknown NPC faction: "+reward.target);
        }
        static void grant(EntityPlayerMP player,QuestCore.Reward reward) {
            noppes.npcs.scripted.ScriptPlayer api=new noppes.npcs.scripted.ScriptPlayer(player);
            if(reward.type.equals("npc_quest")) api.startQuest(id(reward.target));
            else if(reward.type.equals("npc_faction")) {
                int delta=reward.add?reward.points:Math.negateExact(reward.points);
                Math.addExact(api.getFactionPoints(id(reward.target)),delta);
                api.addFactionPoints(id(reward.target),delta);
            } else {
                noppes.npcs.controllers.PlayerMail mail=mail(reward);
                noppes.npcs.controllers.PlayerDataController.instance.addPlayerMessage(player.getCommandSenderName(),mail);
            }
        }
    }
    private QuestNPCs() {}
}
