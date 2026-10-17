package dev.sigmarix.quests;
import java.util.*;import net.minecraft.item.*;import net.minecraft.nbt.NBTTagCompound;
/** Native FTBLib item text, with string registry IDs and legacy numeric NBT support. */
public final class QuestStackText {
    private static final Map<String,String> aliases=new HashMap<String,String>();
    static{String[][] names={{"book","quest_book"},{"lootcrate","loot_crate"},{"custom_icon","custom_icon"},{"screen","task_screen"},{"progress_screen","progress_screen"},{"progress_detector","progress_detector"},{"detector","redstone_detector"},{"chest","quest_chest"},{"barrier","quest_barrier"},{"reward_collector","reward_collector"},{"loot_crate_storage","loot_crate_storage"},{"loot_crate_opener","loot_crate_opener"}};for(String[] name:names)aliases.put("ftbquests:"+name[0],"sigmarixquests:"+name[1]);}
    public static NBTTagCompound write(ItemStack stack){NBTTagCompound nbt=new NBTTagCompound();nbt.setString("id",stack==null?"minecraft:air":String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem())));nbt.setByte("Count",(byte)(stack==null?1:stack.stackSize));nbt.setShort("Damage",(short)(stack==null?0:stack.getItemDamage()));if(stack!=null && stack.hasTagCompound())nbt.setTag("tag",stack.getTagCompound().copy());return nbt;}
    private static int integer(String value,int fallback){try{return Integer.parseInt(value);}catch(NumberFormatException invalid){return fallback;}}
    public static ItemStack parse(String input,boolean single){
        String value=input.trim();if(value.isEmpty() || value.equals("-") || value.equals("minecraft:air"))return null;
        ItemStack stack;
        if(value.startsWith("{")){
            NBTTagCompound nbt=QuestService.tag(value);if(nbt==null)return null;if(nbt.hasKey("ForgeCaps",10) && !nbt.getCompoundTag("ForgeCaps").hasNoTags())throw new IllegalArgumentException("Forge 1.7.10 has no item capability system");
            if(nbt.getByte("Count")<=0)nbt.setByte("Count",(byte)1);
            if(nbt.hasKey("id",8)){String id=nbt.getString("id");int meta=nbt.getShort("Damage");if(id.equals("ftbquests:detector")){id=meta==1?"sigmarixquests:player_detector":"sigmarixquests:redstone_detector";meta=0;}if(id.equals("minecraft:air"))return null;if(id.indexOf(':')<0)id="minecraft:"+id;Item item=(Item)Item.itemRegistry.getObject(aliases.getOrDefault(id,id));if(item==null)throw new IllegalArgumentException("Unknown item: "+id);stack=new ItemStack(item,nbt.getByte("Count"),meta);if(nbt.hasKey("tag",10))stack.setTagCompound((NBTTagCompound)nbt.getCompoundTag("tag").copy());}
            else stack=ItemStack.loadItemStackFromNBT(nbt);
        }else{
            String[] parts=value.split(" ",4);String id=parts[0];int count=parts.length>1?integer(parts[1],1):1,meta=parts.length>2?(parts[2].startsWith("*")?32767:integer(parts[2],0)):0;
            if(id.equals("ftbquests:detector")){id=meta==1?"sigmarixquests:player_detector":"sigmarixquests:redstone_detector";meta=0;}if(id.indexOf(':')<0)id="minecraft:"+id;Item item=(Item)Item.itemRegistry.getObject(aliases.getOrDefault(id,id));if(item==null)throw new IllegalArgumentException("Unknown item: "+id);if(count<=0)return null;stack=new ItemStack(item,count,meta);if(parts.length>3)stack.setTagCompound(QuestService.tag(parts[3]));
        }
        if(stack==null || stack.stackSize<=0)return null;if(single && stack.stackSize>1)throw new IllegalArgumentException("Only one item is allowed");return stack;
    }
    private QuestStackText(){}
}
