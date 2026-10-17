package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
/** Virtual counts retain exact item metadata and NBT; live inventory stacks remain vanilla-sized. */
public final class QuestCrateBank {
    public static final int MAX_TYPES=4096;
    public static final class Entry {
        public final ItemStack prototype;
        private long count;
        private Entry(ItemStack prototype,long count) { this.prototype=prototype.copy(); this.prototype.stackSize=1; this.count=count; }
        public long count() { return count; }
    }
    private final List<Entry> entries=new ArrayList<Entry>();
    private Entry find(ItemStack stack) { for(Entry entry:entries) if(same(entry.prototype,stack)) return entry; return null; }
    public static boolean same(ItemStack a,ItemStack b) { return a!=null && b!=null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a,b); }
    public List<Entry> entries() { return Collections.unmodifiableList(entries); }
    public long count(ItemStack stack) { Entry entry=find(stack); return entry==null?0:entry.count; }
    public long total() { long total=0; for(Entry entry:entries) { if(Long.MAX_VALUE-total<entry.count) return Long.MAX_VALUE; total+=entry.count; } return total; }
    public long deposit(ItemStack stack,long amount) {
        if(stack==null || !(stack.getItem() instanceof QuestBlocks.LootCrateItem) || amount<0) throw new IllegalArgumentException("Only crates can enter virtual storage");
        if(amount==0) return 0;
        long room=Long.MAX_VALUE-total(); if(room==0) return 0;
        Entry entry=find(stack);
        if(entry==null) { if(entries.size()>=MAX_TYPES) return 0; entry=new Entry(stack,0); entries.add(entry); }
        long accepted=Math.min(amount,Math.min(Long.MAX_VALUE-entry.count,room)); entry.count+=accepted; return accepted;
    }
    public long withdraw(ItemStack stack,long amount) {
        if(amount<0) throw new IllegalArgumentException("Negative crate withdrawal");
        Entry entry=find(stack); if(entry==null) return 0;
        long taken=Math.min(amount,entry.count); entry.count-=taken; if(entry.count==0) entries.remove(entry); return taken;
    }
    public void clear() { entries.clear(); }
    public NBTTagList write() {
        NBTTagList list=new NBTTagList();
        for(Entry entry:entries) { NBTTagCompound tag=new NBTTagCompound(); entry.prototype.writeToNBT(tag); tag.setLong("Stored",entry.count); list.appendTag(tag); }
        return list;
    }
    public static QuestCrateBank read(NBTTagList list) {
        if(list.tagCount()>MAX_TYPES) throw new IllegalArgumentException("Too many stored crate variants");
        QuestCrateBank bank=new QuestCrateBank();
        for(int i=0;i<list.tagCount();i++) {
            NBTTagCompound tag=list.getCompoundTagAt(i); ItemStack stack=ItemStack.loadItemStackFromNBT(tag); long count=tag.getLong("Stored");
            if(count<=0 || stack==null || bank.deposit(stack,count)!=count) throw new IllegalArgumentException("Invalid virtual crate inventory");
        }
        return bank;
    }
}
