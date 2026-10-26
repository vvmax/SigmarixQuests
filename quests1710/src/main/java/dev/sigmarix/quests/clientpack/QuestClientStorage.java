package dev.sigmarix.quests.clientpack;
import dev.sigmarix.quests.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
/** Inventory display state only; the server owns the virtual crate bank. */
public final class QuestClientStorage extends QuestClientDevice {
    private ItemStack[] stacks=new ItemStack[54];private boolean crateOnly;
    public QuestClientStorage(){} public QuestClientStorage(boolean crates){crateOnly=crates;}
    public boolean crates(){return crateOnly;}
    public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet){super.readFromNBT(packet.func_148857_g());crateOnly=packet.func_148857_g().getBoolean("crateOnly");}
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

}
