package dev.sigmarix.quests.clientpack;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
/** Server script predicates cannot run in a remote client; they are not guessed locally. */
public final class QuestClientHooks {
    public static final Map<String,Predicate<ItemStack>> ITEM_FILTERS=new HashMap<String,Predicate<ItemStack>>() {
        private final Predicate<ItemStack> unavailable=stack->false;
        @Override public Predicate<ItemStack> get(Object key){Predicate<ItemStack> value=super.get(key);return value==null?unavailable:value;}
    };
}
