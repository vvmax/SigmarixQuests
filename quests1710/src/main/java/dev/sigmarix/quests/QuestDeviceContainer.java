package dev.sigmarix.quests;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
/** Vanilla container protocol owns item movement; configuration uses checked quest actions. */
public final class QuestDeviceContainer extends Container {
    public final QuestDevice tile;
    public int percent,signal;
    public QuestDeviceContainer(EntityPlayer player,QuestDevice tile) {
        this.tile=tile;
        int deviceSlots=tile.getSizeInventory(),inventoryY=deviceSlots==9?142:106,hotbarY=deviceSlots==9?200:164;
        for(int index=0;index<deviceSlots;index++) addSlotToContainer(new Slot(tile,index,14+(index%3)*18,72+(index/3)*18) {
            @Override public boolean isItemValid(ItemStack stack) { return !tile.collector() && (tile.getWorldObj().isRemote || tile.isItemValidForSlot(0,stack)); }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlotToContainer(new Slot(player.inventory,col+row*9+9,14+col*18,inventoryY+row*18));
        for(int col=0;col<9;col++) addSlotToContainer(new Slot(player.inventory,col,14+col*18,hotbarY));
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUseableByPlayer(player); }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges(); int next=0;
        next=tile.percent();
        percent=next; signal=tile.signal?1:0;
        for(Object raw:crafters) { ICrafting viewer=(ICrafting)raw; viewer.sendProgressBarUpdate(this,0,percent); viewer.sendProgressBarUpdate(this,1,signal); }
    }
    @Override public void updateProgressBar(int id,int value) { if(id==0) percent=value; if(id==1) signal=value; }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=inventorySlots.size()) return null;
        Slot slot=(Slot)inventorySlots.get(index); if(!slot.getHasStack()) return null;
        ItemStack stack=slot.getStack(),copy=stack.copy();
        int deviceSlots=tile.getSizeInventory();
        if(index<deviceSlots) { if(!mergeItemStack(stack,deviceSlots,deviceSlots+36,true)) return null; }
        else if(!((Slot)inventorySlots.get(0)).isItemValid(stack) || !mergeItemStack(stack,0,deviceSlots,false)) return null;
        if(stack.stackSize==0) slot.putStack(null); else slot.onSlotChanged();
        if(stack.stackSize==copy.stackSize) return null; slot.onPickupFromSlot(player,stack); return copy;
    }
}
