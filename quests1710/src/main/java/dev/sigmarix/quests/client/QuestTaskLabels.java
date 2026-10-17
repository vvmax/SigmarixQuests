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
            case "item": String item=item(task.target,task.meta,task.nbt);if(item.isEmpty() && !task.targets.isEmpty())item=item(task.targets.get(0),task.meta,task.nbt);if(item.isEmpty() && task.filter!=null)item=filterName(task.filter);return (task.count>1?task.count+"x ":"")+(item.isEmpty()?QuestText.tr("Items"):item);
            case "fluid": net.minecraftforge.fluids.Fluid fluid=net.minecraftforge.fluids.FluidRegistry.getFluid(task.target); return volume(task.count)+" of "+(fluid==null?QuestText.tr("Fluid"):fluid.getLocalizedName(new net.minecraftforge.fluids.FluidStack(fluid,1)));
            case "energy": return formatted("task.ftbquests.forge_energy.text",fancy(task.count));
            case "ic2_energy": return formatted("task.ftbquests.ic2_energy.text",fancy(task.count));
            case "buildcraft_mj": return QuestText.tr("Energy")+" (micro-MJ)";
            case "botania_mana": return task.showNumbers?formatted("task.ftbquests.botania_mana.text",fancy(task.count)):tr("task.ftbquests.botania_mana.value");
            case "emc": return "EMC";
            case "xp": return tr(task.levels?"reward.ftbquests.xp_levels":"reward.ftbquests.xp")+": \u00a7c"+fancy(task.count);
            case "kill": String entity="entity."+task.target+".name"; return formatted("task.ftbquests.kill.title",fancy(task.count),net.minecraft.util.StatCollector.canTranslate(entity)?net.minecraft.util.StatCollector.translateToLocal(entity):task.target);
            case "dimension": return tr("task.ftbquests.dimension")+": \u00a72"+(task.dimension==0?net.minecraft.util.StatCollector.translateToLocal("createWorld.customize.preset.overworld"):task.dimension==-1?"Nether":task.dimension==1?"The End":Integer.toString(task.dimension));
            case "biome": return tr("task.ftbquests.biome")+": \u00a72"+task.target;
            case "location": return tr("task.ftbquests.location");
            case "observation": return tr("task.ftbquests.observation");
            case "interaction": return tr("task.ftbquests.interaction");
            case "stat": net.minecraft.stats.StatBase stat=net.minecraft.stats.StatList.func_151177_a(task.target);return stat==null?tr("task.ftbquests.stat"):stat.func_150951_e().getUnformattedText();
            case "achievement": net.minecraft.stats.StatBase advancement=net.minecraft.stats.StatList.func_151177_a(task.target);return tr("task.ftbquests.advancement")+(advancement==null?"":": \u00a7e"+advancement.func_150951_e().getUnformattedText());
            case "npc_quest": return QuestText.tr("NPC quest");
            case "npc_dialog": return QuestText.tr("NPC dialog");
            case "npc_faction": return QuestText.tr("Faction standing");
            case "gamestage": return QuestText.tr("Stage")+" "+task.target;
            case "skill": return QuestText.tr("Skill")+" "+task.target;
            default: return tr("task.ftbquests.custom");
        }
    }
    public static String quest(QuestCore.Quest quest){return quest.title!=null && !quest.title.isEmpty()?quest.title:quest.tasks.isEmpty()?net.minecraft.util.StatCollector.translateToLocal("ftbquests.unnamed"):title(quest.tasks.get(0));}
    public static String reward(QuestCore.Reward reward) {
        if(!reward.title.isEmpty()) return reward.title;
        if(ClientProxy.snapshot!=null && (reward.type.equals("choice") || reward.type.equals("random") || reward.type.equals("loot"))){String id=reward.type.equals("loot")?reward.target:reward.table;QuestCore.LootSettings settings=ClientProxy.snapshot.book.lootSettings.get(id);if(settings!=null && settings.useTitle)return table(ClientProxy.snapshot.book,id);}
        switch(reward.type) {
            case "item": String item=item(reward.item,reward.meta,reward.nbt); return (reward.count>1 || reward.randomBonus>0?(reward.randomBonus>0?reward.count+"-"+(reward.count+reward.randomBonus):Integer.toString(reward.count))+"x ":"")+(item.isEmpty()?QuestText.tr("Items"):item);
            case "xp": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.xp")+": §a+"+reward.count;
            case "xp_levels": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.xp_levels")+": §a+"+reward.count;
            case "choice": return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward.ftbquests.choice");
            case "random": return tr("reward.ftbquests.random");
            case "loot": return tr("reward.ftbquests.loot");
            case "command": return tr("reward.ftbquests.command");
            case "toast": return tr("reward.ftbquests.toast");
            case "achievement": return QuestText.tr("Achievement");
            default: return QuestPresentation.title(reward);
        }
    }
    public static String table(QuestCore.Book book,String id){QuestCore.LootSettings settings=book.lootSettings.get(id);if(settings!=null && !settings.name.isEmpty())return settings.name;java.util.List<QuestCore.Reward> entries=book.lootTables.get(id);if(entries!=null && entries.size()==1 && !java.util.Arrays.asList("random","choice","loot").contains(entries.get(0).type))return reward(entries.get(0));return net.minecraft.util.StatCollector.translateToLocal("ftbquests.reward_table");}
    private static String tr(String key){return net.minecraft.util.StatCollector.translateToLocal("ftbquests."+key);}
    private static String formatted(String key,Object... values){return net.minecraft.util.StatCollector.translateToLocalFormatted("ftbquests."+key,values);}
    private static String filterName(QuestItemFilter filter){if(filter.type.equals("item"))return item(filter.value,filter.meta,filter.nbt);java.util.List<String> names=new java.util.ArrayList<String>();for(QuestItemFilter child:filter.children){String name=filterName(child);if(!name.isEmpty())names.add(name);}return names.isEmpty()?"":"["+String.join(", ",names)+"]";}
    public static String progress(QuestCore.Task task,long value){if(task.type.equals("fluid"))return volume(value);if(task.type.equals("xp")){if(!task.levels && task.count<=Integer.MAX_VALUE)return Integer.toString(QuestService.XP.level((int)Math.min(Integer.MAX_VALUE,value)));return Long.toString(value);}return fancy(value);}
    public static String fancy(long value){if(value==Long.MAX_VALUE)return "2^63-1";double scale=value>=1000000000L?1e9:value>=1000000?1e6:value>=10000?1e3:1;java.text.DecimalFormat format=new java.text.DecimalFormat("0.00",java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT));String text=format.format(value/scale);if(text.endsWith(".00"))text=text.substring(0,text.length()-3);return text+(scale==1e9?"B":scale==1e6?"M":scale==1e3?"K":"");}
    public static String volume(long value){return (value<1000?Long.toString(value):value%1000==0?Long.toString(value/1000):Double.toString(value/1000D))+(value<1000?" mB":" B");}
    private QuestTaskLabels() {}
}
