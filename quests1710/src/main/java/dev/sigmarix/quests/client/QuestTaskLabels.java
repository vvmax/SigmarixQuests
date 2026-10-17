package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.item.*;
/** Player labels use localized item names; IDs remain available in the operator editor. */
public final class QuestTaskLabels {
    private static String item(String id,int meta,String nbt) {
        Item item=id==null?null:(Item)Item.itemRegistry.getObject(id); if(item==null) return "";
        ItemStack stack=new ItemStack(item,1,meta); try { stack.setTagCompound(QuestService.tag(nbt)); } catch(RuntimeException invalid) {} return stack.getDisplayName();
    }
    public static String title(QuestCore.Task task) {
        if(!task.title.isEmpty()) return task.title;
        switch(task.type) {
            case "check": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.task.ftbquests.checkmark");
            case "item": String item=item(task.target,task.meta,task.nbt); return item.isEmpty()?QuestText.tr("Items"):item;
            case "fluid": net.minecraftforge.fluids.Fluid fluid=net.minecraftforge.fluids.FluidRegistry.getFluid(task.target); return fluid==null?QuestText.tr("Fluid"):fluid.getLocalizedName(new net.minecraftforge.fluids.FluidStack(fluid,1));
            case "energy": return QuestText.tr("Energy")+" (RF)";
            case "ic2_energy": return QuestText.tr("Energy")+" (EU)";
            case "buildcraft_mj": return QuestText.tr("Energy")+" (micro-MJ)";
            case "botania_mana": return QuestText.tr("Mana");
            case "emc": return "EMC";
            case "xp": return QuestText.tr(task.levels?"Experience levels":"Experience");
            case "kill": String entity="entity."+task.target+".name"; return QuestText.tr("Defeat")+" "+(net.minecraft.util.StatCollector.canTranslate(entity)?net.minecraft.util.StatCollector.translateToLocal(entity):task.target);
            case "dimension": return QuestText.tr("Visit dimension")+" "+task.dimension;
            case "biome": return QuestText.tr("Visit biome")+" "+task.target;
            case "location": return QuestText.tr("Visit location");
            case "observation": return QuestText.tr("Observe");
            case "interaction": return QuestText.tr("Interact");
            case "stat": return QuestText.tr("Statistic");
            case "achievement": return QuestText.tr("Achievement");
            case "npc_quest": return QuestText.tr("NPC quest");
            case "npc_dialog": return QuestText.tr("NPC dialog");
            case "npc_faction": return QuestText.tr("Faction standing");
            case "gamestage": return QuestText.tr("Stage")+" "+task.target;
            case "skill": return QuestText.tr("Skill")+" "+task.target;
            default: return QuestText.tr("Custom task");
        }
    }
    public static String reward(QuestCore.Reward reward) {
        if(!reward.title.isEmpty()) return reward.title;
        switch(reward.type) {
            case "item": String item=item(reward.item,reward.meta,reward.nbt); return (reward.count>1?(reward.randomBonus>0?reward.count+"-"+(reward.count+reward.randomBonus):Integer.toString(reward.count))+"x ":"")+(item.isEmpty()?QuestText.tr("Items"):item);
            case "xp": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.xp")+": §a+"+reward.count;
            case "xp_levels": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.xp_levels")+": §a+"+reward.count;
            case "choice": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.choice");
            case "random": case "loot": return QuestText.tr("Random reward");
            case "command": return QuestText.tr("Command");
            case "toast": return QuestText.tr("Notification");
            case "achievement": return QuestText.tr("Achievement");
            default: return QuestPresentation.title(reward);
        }
    }
    private QuestTaskLabels() {}
}
