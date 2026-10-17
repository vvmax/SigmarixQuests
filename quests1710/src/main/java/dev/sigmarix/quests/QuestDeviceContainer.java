package dev.sigmarix.quests;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
/** Vanilla container protocol owns item movement; configuration uses checked quest actions. */
public final class QuestDeviceContainer extends Container {
    public final QuestDevice tile;
    public int percent,signal,queuedLoot;
    public QuestDeviceContainer(EntityPlayer player,QuestDevice tile) {
        this.tile=tile;
        int deviceSlots=tile.getSizeInventory(),inventoryY=deviceSlots>=9?142:106,hotbarY=deviceSlots>=9?200:164;
        for(int index=0;index<deviceSlots;index++) {
            final int localSlot=index; int output=tile.opener()?index-1:index;
            int slotX=tile.opener()?(index==0?14:50+(output%3)*18):14+(output%3)*18,slotY=tile.opener() && index==0?72:72+(output/3)*18;
            addSlotToContainer(new Slot(tile,index,slotX,slotY) {
                @Override public boolean isItemValid(ItemStack stack) { if(tile.opener()) return localSlot==0 && stack!=null && stack.getItem() instanceof QuestBlocks.LootCrateItem; return !tile.collector() && (tile.getWorldObj().isRemote || tile.isItemValidForSlot(localSlot,stack)); }
            });
        }
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlotToContainer(new Slot(player.inventory,col+row*9+9,14+col*18,inventoryY+row*18));
        for(int col=0;col<9;col++) addSlotToContainer(new Slot(player.inventory,col,14+col*18,hotbarY));
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUseableByPlayer(player); }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges(); int next=0;
        next=tile.percent();
        percent=next; signal=tile.signal?1:0; queuedLoot=tile.queuedLoot;
        for(Object raw:crafters) { ICrafting viewer=(ICrafting)raw; viewer.sendProgressBarUpdate(this,0,percent); viewer.sendProgressBarUpdate(this,1,signal); viewer.sendProgressBarUpdate(this,2,queuedLoot&65535); viewer.sendProgressBarUpdate(this,3,queuedLoot>>>16); }
    }
    @Override public void updateProgressBar(int id,int value) { if(id==0) percent=value; if(id==1) signal=value; if(id==2) queuedLoot=(queuedLoot&0xFFFF0000)|(value&65535); if(id==3) queuedLoot=(queuedLoot&65535)|((value&65535)<<16); }
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
