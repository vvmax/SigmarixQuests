package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
/** Native inventory chest: automatic item submission or virtual crate storage with vanilla inventory buffers. */
public final class QuestStorage extends QuestDevice {
    private ItemStack[] stacks=new ItemStack[54];
    private boolean crateOnly;
    private QuestCrateBank bank=new QuestCrateBank();
    public boolean crates() { return crateOnly; }
    public long reserved() { return bank.total(); }
    public long store(ItemStack stack,long amount) { if(!crateOnly) return 0; long accepted=bank.deposit(stack,amount); if(accepted>0) markDirty(); return accepted; }
    public java.util.List<ItemStack> parcels() { java.util.List<ItemStack> parcels=new java.util.ArrayList<ItemStack>(); for(QuestCrateBank.Entry entry:bank.entries()) parcels.add(QuestCrateParcel.create(entry.prototype,entry.count())); bank.clear(); markDirty(); return parcels; }
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
        super.updateEntity(); if(getWorldObj()==null || getWorldObj().isRemote) return;
        if(crateOnly) { if(++storageTicks%5==0) compact(); return; }
        if(++storageTicks%20!=0) return;
        net.minecraft.entity.player.EntityPlayerMP actor=player(); QuestService service=QuestsMod.service; if(owner==null || service==null || !bindingProfile.equals(service.progress().profile())) return;
        List<QuestCore.Quest> quests=new ArrayList<QuestCore.Quest>(service.quests.values()); if(quests.isEmpty()) return; int budget=4096;
        for(int scanned=0;scanned<quests.size() && budget>0;scanned++) {
            QuestCore.Quest definition=quests.get(cursor++%quests.size()); if(cursor>=quests.size()) cursor=0;
            if(!quest.isEmpty() && !definition.id.equals(quest) || !service.active(owner,definition)) continue;
            boolean accepted=false;
            for(QuestCore.Task task:definition.tasks) if(task.type.equals("item") && task.consume && !task.craftingOnly) for(int slot=0;slot<getSizeInventory() && budget>0;slot++) {
                ItemStack stack=stacks[slot]; if(stack==null) continue; budget--;
                if(service.acceptItems(owner,definition.id,task.id,stack,false)>0) { accepted=true; if(stack.stackSize<=0) stacks[slot]=null; markDirty(); }
            }
            if(accepted) { if(actor!=null) service.finish(actor,definition); else service.finishStored(owner,definition,getWorldObj().getTotalWorldTime()); }
        }
    }
    public void nextPage() {
        if(!crateOnly) throw new IllegalArgumentException("Only crate storage has pages");
        long total=bank.total(); java.util.List<ItemStack> variants=new java.util.ArrayList<ItemStack>();
        for(QuestCrateBank.Entry entry:bank.entries()) variants.add(entry.prototype);
        for(ItemStack stack:stacks) if(stack!=null) {
            if(Long.MAX_VALUE-total<stack.stackSize) throw new IllegalArgumentException("Extract some crates before paging this full bank"); total+=stack.stackSize;
            boolean found=false; for(ItemStack variant:variants) if(QuestCrateBank.same(stack,variant)) { found=true; break; } if(!found) variants.add(stack);
        }
        if(variants.size()>QuestCrateBank.MAX_TYPES) throw new IllegalArgumentException("Too many crate variants to change page");
        for(int slot=0;slot<getSizeInventory();slot++) if(stacks[slot]!=null) { bank.deposit(stacks[slot],stacks[slot].stackSize); stacks[slot]=null; }
        variants.sort((a,b)->variantKey(a).compareTo(variantKey(b))); page=(page+1)%Math.max(1,(variants.size()+52)/53);
        for(int i=0;i<53 && i+page*53<variants.size();i++) {
            ItemStack prototype=variants.get(i+page*53); int amount=(int)bank.withdraw(prototype,Math.min(64,prototype.getMaxStackSize()));
            if(amount>0) { stacks[i]=prototype.copy(); stacks[i].stackSize=amount; }
        }
        markDirty();
    }
    private int page;
    private static String variantKey(ItemStack stack) { return net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem())+"/"+stack.getItemDamage()+"/"+String.valueOf(stack.getTagCompound()); }
    /** Slot 53 stays an intake so more than 53 variants can be stored through hoppers. */
    public void compact() {
        if(!crateOnly) return;
        boolean changed=false; java.util.List<ItemStack> seen=new java.util.ArrayList<ItemStack>();
        for(int slot=0;slot<getSizeInventory();slot++) {
            ItemStack stack=stacks[slot]; if(stack==null) continue;
            boolean duplicate=false; for(ItemStack earlier:seen) if(QuestCrateBank.same(earlier,stack)) { duplicate=true; break; }
            if(slot==53 || duplicate) { int accepted=(int)bank.deposit(stack,stack.stackSize); if(accepted>0) { stack.stackSize-=accepted; changed=true; if(stack.stackSize<=0) stacks[slot]=null; } }
            else seen.add(stack);
        }
        for(QuestCrateBank.Entry entry:new java.util.ArrayList<QuestCrateBank.Entry>(bank.entries())) {
            int target=-1;
            for(int slot=0;slot<53;slot++) if(QuestCrateBank.same(stacks[slot],entry.prototype)) { target=slot; break; }
            if(target<0) for(int slot=0;slot<53;slot++) if(stacks[slot]==null) { target=slot; break; }
            if(target<0) continue;
            int current=stacks[target]==null?0:stacks[target].stackSize,limit=Math.min(64,entry.prototype.getMaxStackSize());
            int moved=(int)bank.withdraw(entry.prototype,Math.min(entry.count(),Math.max(0,limit-current)));
            if(moved>0) { if(stacks[target]==null) { stacks[target]=entry.prototype.copy(); stacks[target].stackSize=0; } stacks[target].stackSize+=moved; changed=true; }
        }
        if(changed) markDirty();
    }
    @Override public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag=new NBTTagCompound(); super.writeToNBT(tag); tag.setBoolean("crateOnly",crateOnly);
        return new net.minecraft.network.play.server.S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,tag);
    }
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,net.minecraft.network.play.server.S35PacketUpdateTileEntity packet) {
        NBTTagCompound tag=packet.func_148857_g(); super.readFromNBT(tag); crateOnly=tag.getBoolean("crateOnly");
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); bank=QuestCrateBank.read(tag.getTagList("crateBank",10)); crateOnly=tag.getBoolean("crateOnly"); page=Math.max(0,Math.min((QuestCrateBank.MAX_TYPES-1)/53,tag.getInteger("storagePage"))); stacks=new ItemStack[54]; NBTTagList entries=tag.getTagList("storage",10);
        for(int i=0;i<entries.tagCount();i++) { NBTTagCompound entry=entries.getCompoundTagAt(i); int slot=entry.getByte("Slot") & 255; if(slot<getSizeInventory()) stacks[slot]=ItemStack.loadItemStackFromNBT(entry); }
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setInteger("storagePage",page); tag.setBoolean("crateOnly",crateOnly); NBTTagList entries=new NBTTagList();
        for(int slot=0;slot<getSizeInventory();slot++) if(stacks[slot]!=null) { NBTTagCompound entry=new NBTTagCompound(); entry.setByte("Slot",(byte)slot); stacks[slot].writeToNBT(entry); entries.appendTag(entry); }
        tag.setTag("storage",entries); tag.setTag("crateBank",bank.write());
    }
}
