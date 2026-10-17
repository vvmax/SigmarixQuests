package dev.sigmarix.quests;

import java.util.List;
import net.minecraft.item.ItemStack;

/** Atomic insertion into a collector's finite output inventory. */
public final class QuestRewardBuffer {
    private static ItemStack[] copy(ItemStack[] inventory) {
        ItemStack[] result=new ItemStack[inventory.length];
        for(int i=0;i<result.length;i++) if(inventory[i]!=null) result[i]=inventory[i].copy();
        return result;
    }
    private static int insert(ItemStack[] inventory,ItemStack incoming) {
        int remaining=incoming.stackSize,limit=Math.min(64,incoming.getMaxStackSize());
        for(ItemStack stack:inventory) if(stack!=null && stack.isItemEqual(incoming) && ItemStack.areItemStackTagsEqual(stack,incoming)) {
            int take=Math.min(remaining,Math.max(0,limit-stack.stackSize)); stack.stackSize+=take; remaining-=take;
        }
        for(int i=0;i<inventory.length && remaining>0;i++) if(inventory[i]==null) {
            inventory[i]=incoming.copy(); inventory[i].stackSize=Math.min(remaining,limit); remaining-=inventory[i].stackSize;
        }
        return remaining;
    }
    public static int offer(ItemStack[] inventory,ItemStack incoming) { return incoming.stackSize-insert(inventory,incoming); }
    public static boolean fits(ItemStack[] inventory,List<ItemStack> incoming) {
        ItemStack[] trial=copy(inventory);
        for(ItemStack stack:incoming) if(insert(trial,stack)!=0) return false;
        return true;
    }
    public static boolean add(ItemStack[] inventory,ItemStack incoming) {
        ItemStack[] trial=copy(inventory); if(insert(trial,incoming)!=0) return false;
        System.arraycopy(trial,0,inventory,0,inventory.length); return true;
    }
    private QuestRewardBuffer() {}
}
