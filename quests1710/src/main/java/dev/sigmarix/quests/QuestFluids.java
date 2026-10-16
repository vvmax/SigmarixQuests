package dev.sigmarix.quests;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.*;

/** Inventory preflight: all container mutations target copies until the quest is committed. */
public final class QuestFluids {
    public static boolean matches(QuestCore.Task task,FluidStack fluid) {
        return fluid!=null && fluid.amount>0 && fluid.getFluid().getName().equals(task.target) && QuestService.nbtMatches(task.nbt,task.nbtMode,fluid.tag);
    }
    public static boolean reserve(QuestCore.Task task,long remaining,ItemStack[] reserved,ItemStack[] result,List<ItemStack> empties) {
        for(int i=0;i<reserved.length && remaining>0;i++) {
            ItemStack stack=reserved[i]; if(stack==null) continue;
            if(stack.getItem() instanceof IFluidContainerItem) {
                // Dynamic filled containers normally have stack size one. Do not overwrite stacked NBT containers.
                if(stack.stackSize!=1) continue;
                IFluidContainerItem api=(IFluidContainerItem)stack.getItem();
                if(!matches(task,api.getFluid(stack))) continue;
                int request=(int)Math.min(Integer.MAX_VALUE,remaining);
                FluidStack preview=api.drain(stack,request,false);
                if(!matches(task,preview) || preview.amount>request) continue;
                FluidStack drained=api.drain(stack,preview.amount,true);
                if(!matches(task,drained) || drained.amount!=preview.amount) return false;
                if(task.consume) {
                    FluidStack actual=api.drain(result[i],drained.amount,true);
                    if(!matches(task,actual) || actual.amount!=drained.amount) return false;
                }
                if(stack.stackSize<=0) reserved[i]=null;
                if(task.consume && result[i].stackSize<=0) result[i]=null;
                remaining-=drained.amount;
            } else {
                FluidStack fluid=FluidContainerRegistry.getFluidForFilledItem(stack);
                if(!matches(task,fluid)) continue;
                int take=(int)Math.min(stack.stackSize,remaining/fluid.amount); if(take<=0) continue;
                ItemStack empty=FluidContainerRegistry.drainFluidContainer(stack);
                remaining-=(long)take*fluid.amount; stack.stackSize-=take; if(stack.stackSize<=0) reserved[i]=null;
                if(task.consume) {
                    result[i].stackSize-=take; if(result[i].stackSize<=0) result[i]=null;
                    if(empty!=null) for(int n=0;n<take;n++) empties.add(empty.copy());
                }
            }
        }
        return remaining==0;
    }
}
