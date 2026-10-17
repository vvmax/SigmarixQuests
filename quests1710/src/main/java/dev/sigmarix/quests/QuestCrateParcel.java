package dev.sigmarix.quests;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
/** A broken storage drops bounded parcels instead of spawning millions of crate entities. */
public final class QuestCrateParcel extends Item {
    public QuestCrateParcel() { setUnlocalizedName("sigmarixquests.crate_parcel"); setTextureName("minecraft:minecart_chest"); setMaxStackSize(1); }
    public static ItemStack create(ItemStack prototype,long amount) {
        if(prototype==null || !(prototype.getItem() instanceof QuestBlocks.LootCrateItem) || amount<=0) throw new IllegalArgumentException("Invalid crate parcel");
        ItemStack parcel=new ItemStack(QuestBlocks.crateParcel); NBTTagCompound tag=new NBTTagCompound(),crate=new NBTTagCompound();
        ItemStack copy=prototype.copy(); copy.stackSize=1; copy.writeToNBT(crate); tag.setTag("Crate",crate); tag.setLong("Stored",amount); parcel.setTagCompound(tag); return parcel;
    }
    public static long count(ItemStack parcel) { return parcel!=null && parcel.getItem() instanceof QuestCrateParcel && parcel.hasTagCompound()?Math.max(0,parcel.getTagCompound().getLong("Stored")):0; }
    public static ItemStack prototype(ItemStack parcel) {
        ItemStack stack=parcel!=null && parcel.hasTagCompound()?ItemStack.loadItemStackFromNBT(parcel.getTagCompound().getCompoundTag("Crate")):null;
        if(stack==null || !(stack.getItem() instanceof QuestBlocks.LootCrateItem)) throw new IllegalArgumentException("Invalid sealed crate parcel"); stack.stackSize=1; return stack;
    }
    public static long deposit(ItemStack parcel,QuestStorage storage) {
        long count=count(parcel); if(count==0) return 0; long accepted=storage.store(prototype(parcel),count); consume(parcel,accepted); return accepted;
    }
    private static void consume(ItemStack parcel,long amount) { long left=count(parcel)-amount; parcel.getTagCompound().setLong("Stored",left); if(left==0) parcel.stackSize=0; }
    @Override public ItemStack onItemRightClick(ItemStack parcel,World world,EntityPlayer player) {
        if(world.isRemote) return parcel;
        try {
            long count=count(parcel); if(count<=0) throw new IllegalArgumentException("Empty crate parcel");
            ItemStack crates=prototype(parcel); int capacity=0;
            for(ItemStack slot:player.inventory.mainInventory) if(slot==null) capacity+=Math.min(64,crates.getMaxStackSize()); else if(QuestCrateBank.same(slot,crates)) capacity+=Math.max(0,Math.min(64,slot.getMaxStackSize())-slot.stackSize);
            crates.stackSize=(int)Math.min(count,Math.min(capacity,crates.getMaxStackSize())); int offered=crates.stackSize;
            if(offered==0) return parcel;
            player.inventory.addItemStackToInventory(crates); consume(parcel,offered-crates.stackSize); player.inventory.markDirty();
        } catch(RuntimeException ex) { QuestService.say(player,ex.getMessage()); }
        return parcel.stackSize<=0?null:parcel;
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,java.util.List lines,boolean advanced) {
        lines.add("Stored crates: "+count(stack));
        try { lines.add(prototype(stack).getDisplayName()); } catch(RuntimeException invalid) { lines.add("Invalid parcel"); }
    }
}
