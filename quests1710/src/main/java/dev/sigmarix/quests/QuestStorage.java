package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
/** Native inventory chest: automatic item submission or dedicated physical crate storage. */
public final class QuestStorage extends QuestDevice {
    private ItemStack[] stacks=new ItemStack[54];
    private boolean crateOnly;
    private int storageTicks,cursor;
    public QuestStorage() {}
    public QuestStorage(boolean crates) { crateOnly=crates; }
    @Override public int getSizeInventory() { return crateOnly?54:27; }
    @Override public ItemStack getStackInSlot(int slot) { return slot>=0 && slot<getSizeInventory()?stacks[slot]:null; }
    @Override public ItemStack decrStackSize(int slot,int amount) {
        ItemStack stack=getStackInSlot(slot); if(stack==null || amount<=0) return null;
        ItemStack result=stack.splitStack(Math.min(amount,stack.stackSize)); if(stack.stackSize<=0) stacks[slot]=null; markDirty(); return result;
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { ItemStack stack=getStackInSlot(slot); if(stack!=null) { stacks[slot]=null; markDirty(); } return stack; }
    @Override public void setInventorySlotContents(int slot,ItemStack stack) {
        if(slot<0 || slot>=getSizeInventory() || stack!=null && !isItemValidForSlot(slot,stack)) return;
        if(stack!=null) stack.stackSize=Math.min(stack.stackSize,Math.min(64,stack.getMaxStackSize())); stacks[slot]=stack; markDirty();
    }
    @Override public String getInventoryName() { return crateOnly?"container.sigmarixquests.loot_crate_storage":"container.sigmarixquests.quest_chest"; }
    @Override public boolean hasCustomInventoryName() { return false; }
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) { return slot>=0 && slot<getSizeInventory() && stack!=null && (!crateOnly || stack.getItem() instanceof QuestBlocks.LootCrateItem); }
    @Override public int[] getAccessibleSlotsFromSide(int side) { int[] slots=new int[getSizeInventory()]; for(int i=0;i<slots.length;i++) slots[i]=i; return slots; }
    @Override public boolean canInsertItem(int slot,ItemStack stack,int side) { return isItemValidForSlot(slot,stack); }
    @Override public boolean canExtractItem(int slot,ItemStack stack,int side) { return slot>=0 && slot<getSizeInventory(); }
    @Override public void updateEntity() {
        super.updateEntity(); if(getWorldObj()==null || getWorldObj().isRemote || crateOnly || ++storageTicks%20!=0) return;
        net.minecraft.entity.player.EntityPlayerMP owner=player(); QuestService service=QuestsMod.service; if(owner==null || service==null) return;
        List<QuestCore.Quest> quests=new ArrayList<QuestCore.Quest>(service.quests.values()); if(quests.isEmpty()) return; int budget=4096;
        for(int scanned=0;scanned<quests.size() && budget>0;scanned++) {
            QuestCore.Quest definition=quests.get(cursor++%quests.size()); if(cursor>=quests.size()) cursor=0;
            if(!quest.isEmpty() && !definition.id.equals(quest) || !service.active(owner,definition)) continue;
            boolean accepted=false;
            for(QuestCore.Task task:definition.tasks) if(task.type.equals("item") && task.consume && !task.craftingOnly) for(int slot=0;slot<getSizeInventory() && budget>0;slot++) {
                ItemStack stack=stacks[slot]; if(stack==null) continue; budget--;
                if(service.acceptItems(owner,definition.id,task.id,stack,false)>0) { accepted=true; if(stack.stackSize<=0) stacks[slot]=null; markDirty(); }
            }
            if(accepted) service.finish(owner,definition);
        }
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); crateOnly=tag.getBoolean("crateOnly"); stacks=new ItemStack[54]; NBTTagList entries=tag.getTagList("storage",10);
        for(int i=0;i<entries.tagCount();i++) { NBTTagCompound entry=entries.getCompoundTagAt(i); int slot=entry.getByte("Slot") & 255; if(slot<getSizeInventory()) stacks[slot]=ItemStack.loadItemStackFromNBT(entry); }
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setBoolean("crateOnly",crateOnly); NBTTagList entries=new NBTTagList();
        for(int slot=0;slot<getSizeInventory();slot++) if(stacks[slot]!=null) { NBTTagCompound entry=new NBTTagCompound(); entry.setByte("Slot",(byte)slot); stacks[slot].writeToNBT(entry); entries.appendTag(entry); }
        tag.setTag("storage",entries);
    }
}
