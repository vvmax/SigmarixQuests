package dev.sigmarix.quests;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
/** Vanilla chest transfers plus a lossless 64-bit reserve counter on the container channel. */
public final class QuestStorageContainer extends ContainerChest {
    public final QuestStorage storage;
    public long reserved;
    public QuestStorageContainer(EntityPlayer player,QuestStorage storage) { super(player.inventory,storage); this.storage=storage; }
    @Override public boolean canInteractWith(EntityPlayer player) { return storage.isUseableByPlayer(player); }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges(); reserved=storage.reserved();
        for(Object raw:crafters) for(int word=0;word<4;word++) ((ICrafting)raw).sendProgressBarUpdate(this,10+word,(int)(reserved >>> (word*16)) & 65535);
    }
    @Override public void updateProgressBar(int id,int value) {
        if(id>=10 && id<14) { int shift=(id-10)*16; reserved=(reserved & ~(65535L << shift)) | ((value & 65535L) << shift); }
        else super.updateProgressBar(id,value);
    }
}
