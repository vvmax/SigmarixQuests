package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.controllers.*;
import noppes.npcs.scripted.ScriptPlayer;
/** Uses real CustomNPCs controllers and player files, including a world restart. */
public final class NPCQuestSmoke {
    private static final int KEY=900001;
    public static void run(EntityPlayerMP player) throws Exception {
        QuestCategory category=new QuestCategory(); category.id=KEY; category.title="Quest adapter smoke";
        QuestController.instance.saveCategory(category);
        Quest npc=new Quest(); npc.id=KEY; npc.title="Adapter quest"; npc.category=category;
        QuestController.instance.saveQuest(KEY,npc);
        Faction faction=new Faction(); faction.id=KEY; faction.name="Adapter faction"; faction.defaultPoints=0;
        FactionController.getInstance().saveFaction(faction);
        QuestCore.Quest q=new QuestCore.Quest(); q.id="npc-adapter"; q.title="NPC adapter";
        QuestCore.Task check=new QuestCore.Task(); check.id="confirm"; check.type="check"; q.tasks.add(check);
        QuestCore.Reward quest=new QuestCore.Reward(); quest.type="npc_quest"; quest.target=""+KEY; q.rewards.add(quest);
        QuestCore.Reward points=new QuestCore.Reward(); points.type="npc_faction"; points.target=""+KEY; points.points=25; q.rewards.add(points);
        QuestCore.Reward mail=new QuestCore.Reward(); mail.type="npc_mail";
        mail.nbt="{Subject:\"Quest adapter\",Sender:\"Quest book\",Message:{pages:[\"Hello from the quest reward\"]},MailItems:[],MailQuest:-1}";
        QuestCore.MailItem gift=new QuestCore.MailItem(); gift.item="minecraft:diamond"; gift.count=2; mail.attachments.add(gift);
        q.rewards.add(mail);
        QuestService service=new QuestService(new java.io.File("unused-smoke.json")); service.quests=Collections.singletonMap(q.id,q);
        QuestCore.Book book=new QuestCore.Book(); book.quests.add(q); service.book=book; book.validate();
        if(!service.finish(player,q)) throw new IllegalStateException("NPC reward quest did not finish");
        int old=PlayerDataController.instance.getPlayerData(player).mailData.playermail.size();
        service.claim(player,q,Collections.<Integer>emptyList());
        ScriptPlayer api=new ScriptPlayer(player);
        QuestCore.Task task=new QuestCore.Task(); task.type="npc_quest"; task.target=""+KEY; task.checkActive=true;
        if(!QuestNPCs.task(player,task)) throw new IllegalStateException("NPC quest reward did not activate quest");
        task.checkActive=false;
        if(QuestNPCs.task(player,task)) throw new IllegalStateException("Active NPC quest reported as finished");
        api.finishQuest(KEY);
        if(!QuestNPCs.task(player,task)) throw new IllegalStateException("Finished NPC quest not detected");
        PlayerData data=PlayerDataController.instance.getPlayerData(player); data.dialogData.dialogsRead.add(KEY);
        task.type="npc_dialog"; if(!QuestNPCs.task(player,task)) throw new IllegalStateException("NPC dialog not detected");
        task.type="npc_faction"; task.points=25;
        if(!QuestNPCs.task(player,task)) throw new IllegalStateException("NPC faction reward not detected");
        task.comparison="at_most"; task.points=24;
        if(QuestNPCs.task(player,task)) throw new IllegalStateException("NPC faction comparison ignored");
        if(data.mailData.playermail.size()!=old+1) throw new IllegalStateException("NPC mail reward missing");
        try { service.claim(player,q,Collections.<Integer>emptyList()); throw new IllegalStateException("NPC reward duplicated"); }
        catch(IllegalArgumentException expected) {}
        PlayerDataController.instance.savePlayerData(data);
        verify(player);
        QuestCore.Book configuration=QuestService.JSON.fromJson(QuestService.JSON.toJson(QuestsMod.service.book),QuestCore.Book.class);
        QuestCore.Quest configured=new QuestCore.Quest(); configured.id="npc-config"; configured.title="CustomNPCs"; configured.x=-3; configured.y=-3;
        QuestCore.Task requirement=new QuestCore.Task(); requirement.type="npc_quest"; requirement.target=""+KEY; configured.tasks.add(requirement);
        QuestCore.Reward factionReward=new QuestCore.Reward(); factionReward.type="npc_faction"; factionReward.target=""+KEY; configured.rewards.add(factionReward); configuration.quests.add(configured);
        QuestsMod.service.save(configuration,QuestsMod.service.revision);
        System.out.println("Quest CustomNPCs PASS: quest activation/completion, dialog, faction comparisons, mail and one-time rewards");
    }
    public static void verify(EntityPlayerMP player) {
        ScriptPlayer api=new ScriptPlayer(player);
        PlayerData data=PlayerDataController.instance.getPlayerData(player);
        if(!api.hasFinishedQuest(KEY) || !api.hasReadDialog(KEY) || api.getFactionPoints(KEY)!=25) throw new IllegalStateException("NPC progress did not persist");
        boolean found=false;
        for(PlayerMail mail:data.mailData.playermail) if(mail.subject.equals("Quest adapter") && mail.message.getTagList("pages",8).getStringTagAt(0).equals("Hello from the quest reward") && mail.items[0]!=null && mail.items[0].getItem()==net.minecraft.init.Items.diamond && mail.items[0].stackSize==2) found=true;
        if(!found) throw new IllegalStateException("NPC mail did not persist with its body");
    }
}
