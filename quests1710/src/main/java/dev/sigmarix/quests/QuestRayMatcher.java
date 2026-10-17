package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
/** Native block/entity identity and NBT tests for observation and interaction tasks. */
public final class QuestRayMatcher {
    public static final Set<String> TYPES=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("","block_id","block_entity_id","block_entity_class","entity_id","entity_class")));
    public static String type(QuestCore.Task task) { return task.matchType.isEmpty()?(task.entity?"entity_id":"block_id"):task.matchType; }
    public static boolean entity(QuestCore.Task task) { return type(task).startsWith("entity_"); }
    public static boolean block(QuestCore.Task task,World world,int x,int y,int z) {
        if(entity(task)) return false;
        NBTTagCompound tag=null; String identity;
        switch(type(task)) {
            case "block_id": identity=net.minecraft.block.Block.blockRegistry.getNameForObject(world.getBlock(x,y,z)); break;
            case "block_entity_id": case "block_entity_class":
                TileEntity tile=world.getTileEntity(x,y,z); if(tile==null) return false;
                tag=new NBTTagCompound(); tile.writeToNBT(tag); identity=type(task).equals("block_entity_id")?tag.getString("id"):tile.getClass().getName(); break;
            default: return false;
        }
        if(!task.target.equals(identity) || task.properties.isEmpty() && !task.ignoreDamage && world.getBlockMetadata(x,y,z)!=task.meta) return false;
        if(type(task).equals("block_id") && !task.properties.isEmpty() && !QuestBlockState.matches(world.getBlock(x,y,z),world.getBlockMetadata(x,y,z),QuestBlockState.constraints(task.properties)))return false;
        if(!task.nbt.isEmpty() && tag==null) { TileEntity tile=world.getTileEntity(x,y,z); if(tile==null) return false; tag=new NBTTagCompound(); tile.writeToNBT(tag); }
        return QuestService.nbtMatches(task.nbt,task.nbtMode,tag);
    }
    public static boolean entity(QuestCore.Task task,Entity entity) {
        if(!entity(task)) return false;
        String identity=type(task).equals("entity_class")?entity.getClass().getName():net.minecraft.entity.EntityList.getEntityString(entity);
        if(!task.target.equals(identity)) return false;
        NBTTagCompound tag=null;
        if(!task.nbt.isEmpty()) { tag=new NBTTagCompound(); entity.writeToNBT(tag); }
        return QuestService.nbtMatches(task.nbt,task.nbtMode,tag);
    }
    private QuestRayMatcher() {}
}
