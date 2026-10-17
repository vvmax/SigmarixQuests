package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
/** Maps original state strings onto the properties represented by 1.7.10 metadata. */
public final class QuestBlockState {
    private static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black"};
    private static final String[] WOOD={"oak","spruce","birch","jungle","acacia","dark_oak"};
    private static final String[] FACING={"down","up","north","south","west","east"};
    public static final class State {public final Block block;public final int meta;State(Block block,int meta){this.block=block;this.meta=meta;}}
    public static Map<String,String> constraints(String text){Map<String,String> values=new LinkedHashMap<String,String>();if(text==null || text.length()>8192)throw new IllegalArgumentException("Invalid block properties");if(text.isEmpty())return values;for(String part:text.split(",",-1)){String[] pair=part.split("=",-1);if(pair.length!=2 || !pair[0].matches("[a-zA-Z0-9_]+") || !pair[1].matches("[a-zA-Z0-9_]+") || values.put(pair[0],pair[1])!=null)throw new IllegalArgumentException("Expected property=value pairs");}return values;}
    public static Map<String,String> properties(Block block,int meta){
        Map<String,String> values=new LinkedHashMap<String,String>();values.put("meta",Integer.toString(meta));
        if(block==Blocks.wool || block==Blocks.stained_glass || block==Blocks.stained_glass_pane || block==Blocks.stained_hardened_clay || block==Blocks.carpet)values.put("color",COLORS[meta&15]);
        if(block==Blocks.planks || block==Blocks.sapling){values.put(block==Blocks.sapling?"type":"variant",WOOD[Math.min(5,meta&7)]);if(block==Blocks.sapling)values.put("stage",Integer.toString(meta>>3&1));}
        if(block==Blocks.log || block==Blocks.log2){values.put("variant",WOOD[(block==Blocks.log2?4:0)+Math.min(block==Blocks.log2?1:3,meta&3)]);values.put("axis",new String[]{"y","x","z","none"}[meta>>2&3]);}
        if(block==Blocks.leaves || block==Blocks.leaves2){values.put("variant",WOOD[(block==Blocks.leaves2?4:0)+Math.min(block==Blocks.leaves2?1:3,meta&3)]);values.put("decayable",Boolean.toString((meta&4)==0));values.put("check_decay",Boolean.toString((meta&8)!=0));}
        if(block==Blocks.stone)values.put("variant","stone");if(block==Blocks.dirt)values.put("variant",meta==2?"podzol":meta==1?"coarse_dirt":"dirt");
        if(block==Blocks.sand)values.put("variant",meta==1?"red_sand":"sand");if(block==Blocks.sandstone)values.put("type",new String[]{"sandstone","chiseled_sandstone","smooth_sandstone"}[Math.min(meta,2)]);
        if(block==Blocks.stonebrick)values.put("variant",new String[]{"stonebrick","mossy_stonebrick","cracked_stonebrick","chiseled_stonebrick"}[meta&3]);
        if(block instanceof BlockStairs){values.put("facing",new String[]{"east","west","south","north"}[meta&3]);values.put("half",(meta&4)==0?"bottom":"top");values.put("shape","straight");}
        if(block instanceof BlockSlab){values.put("half",(meta&8)==0?"bottom":"top");if(block==Blocks.wooden_slab || block==Blocks.double_wooden_slab)values.put("variant",WOOD[Math.min(5,meta&7)]);}
        if(block==Blocks.furnace || block==Blocks.lit_furnace || block==Blocks.chest || block==Blocks.trapped_chest || block==Blocks.ender_chest || block==Blocks.ladder || block==Blocks.dispenser || block==Blocks.dropper || block==Blocks.hopper){values.put("facing",FACING[Math.min(5,meta&7)]);if(block==Blocks.hopper)values.put("enabled",Boolean.toString((meta&8)==0));if(block==Blocks.dispenser || block==Blocks.dropper)values.put("triggered",Boolean.toString((meta&8)!=0));}
        if(block==Blocks.piston || block==Blocks.sticky_piston){values.put("facing",FACING[Math.min(5,meta&7)]);values.put("extended",Boolean.toString((meta&8)!=0));}
        if(block==Blocks.torch || block==Blocks.redstone_torch || block==Blocks.unlit_redstone_torch)values.put("facing",new String[]{"up","east","west","south","north","up"}[Math.min(5,meta)]);
        if(block==Blocks.water || block==Blocks.flowing_water || block==Blocks.lava || block==Blocks.flowing_lava)values.put("level",Integer.toString(meta));if(block==Blocks.redstone_wire)values.put("power",Integer.toString(meta));
        if(block==Blocks.wheat || block==Blocks.carrots || block==Blocks.potatoes || block==Blocks.cocoa || block==Blocks.nether_wart || block==Blocks.cactus || block==Blocks.reeds)values.put("age",Integer.toString(block==Blocks.cocoa?meta>>2:meta));
        if(block==Blocks.farmland)values.put("moisture",Integer.toString(meta&7));if(block==Blocks.snow_layer)values.put("layers",Integer.toString((meta&7)+1));if(block==Blocks.portal)values.put("axis",meta==2?"z":"x");
        return values;
    }
    public static boolean matches(Block block,int meta,Map<String,String> expected){Map<String,String> actual=properties(block,meta);for(Map.Entry<String,String> entry:expected.entrySet())if(!entry.getValue().equals(actual.get(entry.getKey())))return false;return true;}
    public static State parse(String text){if(text==null || text.length()>8192)throw new IllegalArgumentException("Invalid block state");int start=text.indexOf('[');String id=start<0?text:text.substring(0,start);if(!Block.blockRegistry.containsKey(id))throw new IllegalArgumentException("Unknown block");Block block=(Block)Block.blockRegistry.getObject(id);if(start<0)return new State(block,0);if(!text.endsWith("]"))throw new IllegalArgumentException("Invalid block properties");Map<String,String> wanted=constraints(text.substring(start+1,text.length()-1));for(int meta=0;meta<16;meta++)if(matches(block,meta,wanted))return new State(block,meta);throw new IllegalArgumentException("Properties are not represented by this 1.7.10 block");}
    public static String format(Block block,int meta){String id=(String)Block.blockRegistry.getNameForObject(block);Map<String,String> values=properties(block,meta);values.remove("meta");if(values.isEmpty() && meta!=0)values.put("meta",Integer.toString(meta));if(values.isEmpty())return id;List<String> parts=new ArrayList<String>();for(Map.Entry<String,String> entry:values.entrySet())parts.add(entry.getKey()+"="+entry.getValue());return id+"["+String.join(",",parts)+"]";}
    private QuestBlockState(){}
}
