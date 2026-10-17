package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
/** Weighted physical crates, including a no-drop outcome for each creature category. */
public final class QuestLoot {
    public static QuestCore.Book clientBook;
    public static final class Weights {
        public int passive,monster,boss;
        public Weights() {}
        public Weights(int passive,int monster,int boss) { this.passive=passive; this.monster=monster; this.boss=boss; }
        public int weight(EntityLivingBase entity) { return entity instanceof net.minecraft.entity.boss.IBossDisplayData?boss:entity instanceof net.minecraft.entity.monster.IMob?monster:passive; }
        public void validate() { if(passive<0 || monster<0 || boss<0 || passive>1000000 || monster>1000000 || boss>1000000) throw new IllegalArgumentException("Invalid creature drop weight"); }
    }
    public static ItemStack crate(QuestCore.Book book,String table) {
        if(!book.lootTables.containsKey(table)) throw new IllegalArgumentException("Unknown crate table");
        ItemStack stack=new ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","loot_crate"));
        QuestCore.LootSettings settings=book.lootSettings.get(table); NBTTagCompound tag=new NBTTagCompound(); tag.setString("table",table);
        if(settings!=null) { tag.setString("name",settings.name); tag.setInteger("color",QuestPresentation.color(settings.color)); tag.setBoolean("glow",settings.glow); }
        stack.setTagCompound(tag); return stack;
    }
    public static String choose(QuestCore.Book book,EntityLivingBase entity,Random random) {
        long total=book.noDrop.weight(entity);
        for(QuestCore.LootSettings settings:book.lootSettings.values()) total+=settings.drops.weight(entity);
        if(!book.dropLootCrates || total==0) return null;
        long roll=(long)(random.nextDouble()*total); if(roll<book.noDrop.weight(entity)) return null; roll-=book.noDrop.weight(entity);
        for(Map.Entry<String,QuestCore.LootSettings> entry:book.lootSettings.entrySet()) { int weight=entry.getValue().drops.weight(entity); if(roll<weight) return entry.getKey(); roll-=weight; }
        return null;
    }
    private QuestLoot() {}
}
