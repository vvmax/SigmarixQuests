package dev.sigmarix.quests.clientpack;
import dev.sigmarix.quests.*;
import com.google.gson.Gson;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
/** Shared parsing and display helpers only; no server quest service. */
public final class QuestClientService {
    public static final Gson JSON=QuestJson.create();
    public static Item item(String id) {
        if (id == null || !Item.itemRegistry.containsKey(id)) throw new IllegalArgumentException("Unknown item: " + id);
        return (Item) Item.itemRegistry.getObject(id);
    }

    public static Block block(String id) {
        if (id == null || !Block.blockRegistry.containsKey(id)) throw new IllegalArgumentException("Unknown block: " + id);
        return (Block) Block.blockRegistry.getObject(id);
    }

    public static NBTTagCompound tag(String value) {
        if (value.isEmpty()) return null;
        return QuestSnbt.parse(value);
    }

    public static boolean stackNbtMatches(String nbt,String mode,ItemStack stack) {
        if(!(stack.getItem() instanceof QuestBlocks.LootCrateItem) || nbt.isEmpty() || mode.equals("ignore")) return nbtMatches(nbt,mode,stack.getTagCompound());
        QuestCore.Book book=QuestLoot.clientBook;
        NBTTagCompound expected=QuestLoot.canonical(tag(nbt),book),actual=QuestLoot.canonical(stack.getTagCompound(),book);
        if(actual!=null) for(String key:Arrays.asList("name","color","glow")) if(!expected.hasKey(key)) actual.removeTag(key);
        return mode.equals("subset")?subset(expected,actual):expected.equals(actual);
    }

    public static boolean nbtMatches(String nbt,String mode,NBTTagCompound actual) {
        if(nbt.isEmpty() || mode.equals("ignore")) return true;
        NBTTagCompound expected=tag(nbt);
        return mode.equals("subset")?subset(expected,actual):expected.equals(actual);
    }

    private static boolean subset(NBTBase expected,NBTBase actual) {
        if(expected instanceof NBTTagCompound && actual instanceof NBTTagCompound) {
            NBTTagCompound e=(NBTTagCompound)expected,a=(NBTTagCompound)actual;
            for(Object key:e.func_150296_c()) if(!a.hasKey(key.toString()) || !subset(e.getTag(key.toString()),a.getTag(key.toString()))) return false;
            return true;
        }
        return expected.equals(actual);
    }
    public static final class XP {
        public static int cap(int level) { return level>=30 ? 62+(level-30)*7 : level>=15 ? 17+(level-15)*3 : 17; }
        public static int base(int level) {
            if(level<0 || level>20000) throw new IllegalArgumentException("XP level outside supported range");
            long result=0; for(int i=0;i<level;i++) result+=cap(i);
            if(result>Integer.MAX_VALUE) throw new IllegalArgumentException("XP overflow"); return (int)result;
        }
        public static int total(int level,float fraction) { return Math.addExact(base(level),Math.min(cap(level)-1,Math.round(Math.max(0,Math.min(fraction,0.999999f))*cap(level)))); }
        public static int level(int points) { int level=0; while(points>=cap(level) && level<20000) { points-=cap(level); level++; } return level; }
    }
}
