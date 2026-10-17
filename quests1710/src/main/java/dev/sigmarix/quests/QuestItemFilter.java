package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.item.*;
import net.minecraftforge.oredict.OreDictionary;
/** Composable filters use native 1.7 items/ores, without requiring the newer Item Filters mod. */
public final class QuestItemFilter {
    public static final Set<String> TYPES=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("all","any","not","item","ore","mod","nbt","food","block","damage","count","custom")));
    public String type="item",value="minecraft:stone",nbt="",nbtMode="exact";
    public int meta=-1,min,max=Integer.MAX_VALUE;
    public List<QuestItemFilter> children=new ArrayList<QuestItemFilter>();
    public void validate(boolean registries) { validate(registries,0,new int[]{0}); }
    private void validate(boolean registries,int depth,int[] nodes) {
        if(depth>16 || ++nodes[0]>1024 || !TYPES.contains(type) || value==null || value.length()>1024 || nbt==null || nbtMode==null || !Arrays.asList("exact","subset","ignore").contains(nbtMode) || children==null || children.size()>128 || meta<-1 || min<0 || max<min) throw new IllegalArgumentException("Invalid item filter");
        boolean composite=type.equals("all") || type.equals("any") || type.equals("not");
        if(composite?(children.isEmpty() || type.equals("not") && children.size()!=1):!children.isEmpty()) throw new IllegalArgumentException("Invalid filter children");
        if(Arrays.asList("item","ore","mod","custom").contains(type) && value.isEmpty() || type.equals("nbt") && nbt.isEmpty()) throw new IllegalArgumentException("Missing filter value");
        if(registries) {
            QuestService.tag(nbt);
            if(type.equals("item")) QuestService.item(value);
            if(type.equals("ore") && OreDictionary.getOres(value).isEmpty()) throw new IllegalArgumentException("Unknown ore filter: "+value);
            if(type.equals("custom") && !QuestHooks.ITEM_FILTERS.containsKey(value)) throw new IllegalArgumentException("Unknown custom item filter: "+value);
        }
        for(QuestItemFilter child:children) { if(child==null) throw new IllegalArgumentException("Null filter child"); child.validate(registries,depth+1,nodes); }
    }
    public boolean matches(ItemStack stack) {
        if(stack==null || stack.stackSize<=0) return false;
        switch(type) {
            case "all": for(QuestItemFilter child:children) if(!child.matches(stack)) return false; return true;
            case "any": for(QuestItemFilter child:children) if(child.matches(stack)) return true; return false;
            case "not": return !children.get(0).matches(stack);
            case "item": return stack.getItem()==QuestService.item(value) && (meta<0 || meta==stack.getItemDamage()) && QuestService.stackNbtMatches(nbt,nbtMode,stack);
            case "ore": for(ItemStack ore:OreDictionary.getOres(value)) if(OreDictionary.itemMatches(ore,stack,false)) return QuestService.stackNbtMatches(nbt,nbtMode,stack); return false;
            case "mod": String name=Item.itemRegistry.getNameForObject(stack.getItem()); return name!=null && name.startsWith(value+":");
            case "nbt": return QuestService.stackNbtMatches(nbt,nbtMode,stack);
            case "food": return stack.getItem() instanceof ItemFood;
            case "block": return stack.getItem() instanceof ItemBlock;
            case "damage": return stack.getItemDamage()>=min && stack.getItemDamage()<=max;
            case "count": return stack.stackSize>=min && stack.stackSize<=max;
            case "custom": return QuestHooks.ITEM_FILTERS.get(value).test(stack);
            default: return false;
        }
    }
}
