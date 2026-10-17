package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.tileentity.TileEntity;
/** Owned rectangular screen assemblies. Parts share the core binding and stop when it disappears. */
public final class QuestScreens {
    private static int dx(QuestDevice core) { return core.facing==2?-1:core.facing==3?1:0; }
    private static int dz(QuestDevice core) { return core.facing==4?1:core.facing==5?-1:0; }
    private static QuestDevice at(QuestDevice core,int column,int row) {
        TileEntity tile=core.getWorldObj().getTileEntity(core.xCoord+dx(core)*column,core.yCoord+row,core.zCoord+dz(core)*column);
        return tile instanceof QuestDevice?(QuestDevice)tile:null;
    }
    private static boolean belongs(QuestDevice part,QuestDevice core) { return part.screenLinked && part.coreX==core.xCoord && part.coreY==core.yCoord && part.coreZ==core.zCoord; }
    public static QuestDevice core(QuestDevice tile) {
        if(!tile.screenLinked) return tile;
        if(tile.getWorldObj()==null) return null;
        TileEntity raw=tile.getWorldObj().getTileEntity(tile.coreX,tile.coreY,tile.coreZ);
        if(!(raw instanceof QuestDevice)) return null;
        QuestDevice core=(QuestDevice)raw;
        if(core.screenLinked || !Objects.equals(core.owner,tile.owner) || core.facing!=tile.facing || core.getBlockType()!=tile.getBlockType()) return null;
        int row=tile.yCoord-core.yCoord,column=dx(core)!=0?(tile.xCoord-core.xCoord)*dx(core):(tile.zCoord-core.zCoord)*dz(core);
        return row>=0 && row<core.screenHeight && column>=0 && column<core.screenWidth && at(core,column,row)==tile?core:null;
    }
    public static void configure(QuestDevice core,int width,int height) {
        if(core.screenLinked || core.owner==null || !(core.getBlockType() instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)core.getBlockType()).mode>1 || width<1 || width>8 || height<1 || height>8) throw new IllegalArgumentException("Configure an owned core screen, size 1..8");
        List<QuestDevice> next=new ArrayList<QuestDevice>();
        for(int row=0;row<height;row++) for(int column=0;column<width;column++) {
            QuestDevice part=at(core,column,row);
            if(part==null || part.getBlockType()!=core.getBlockType() || !Objects.equals(part.owner,core.owner) || part.facing!=core.facing || part.screenLinked && !belongs(part,core) || part!=core && (part.screenWidth!=1 || part.screenHeight!=1)) throw new IllegalArgumentException("Fill the rectangle upward and to the right with screens of the same type, owner and facing");
            next.add(part);
        }
        for(int row=0;row<core.screenHeight;row++) for(int column=0;column<core.screenWidth;column++) {
            QuestDevice part=at(core,column,row);
            if(part!=null && belongs(part,core) && !next.contains(part)) { part.screenLinked=false; part.quest=""; part.task=""; changed(part); }
        }
        core.screenWidth=width; core.screenHeight=height;
        for(QuestDevice part:next) {
            if(part!=core) { part.screenLinked=true; part.coreX=core.xCoord; part.coreY=core.yCoord; part.coreZ=core.zCoord; copy(core,part); }
            changed(part);
        }
    }
    private static void copy(QuestDevice core,QuestDevice part) {
        if(!part.quest.equals(core.quest) || !part.task.equals(core.task) || !part.bindingProfile.equals(core.bindingProfile)) {
            part.quest=core.quest; part.task=core.task; part.bindingProfile=core.bindingProfile; changed(part);
        }
    }
    public static boolean synchronize(QuestDevice tile) {
        if(!tile.screenLinked) return true;
        QuestDevice core=core(tile);
        if(core==null) {
            if(tile.getWorldObj()!=null && !tile.getWorldObj().isRemote && tile.getWorldObj().blockExists(tile.coreX,tile.coreY,tile.coreZ)) { tile.screenLinked=false; tile.quest=""; tile.task=""; changed(tile); }
            return false;
        }
        copy(core,tile); return true;
    }
    public static void validateAssembly(QuestDevice tile) {
        if(tile.screenLinked || tile.screenWidth==1 && tile.screenHeight==1) return;
        // An unloaded neighboring chunk is not a broken screen.
        for(int row=0;row<tile.screenHeight;row++) for(int column=0;column<tile.screenWidth;column++) if(!tile.getWorldObj().blockExists(tile.xCoord+dx(tile)*column,tile.yCoord+row,tile.zCoord+dz(tile)*column)) return;
        for(int row=0;row<tile.screenHeight;row++) for(int column=0;column<tile.screenWidth;column++) {
            QuestDevice part=at(tile,column,row);
            if(part==null || part!=tile && core(part)!=tile) { configure(tile,1,1); return; }
        }
    }
    private static void changed(QuestDevice tile) { tile.markDirty(); if(tile.getWorldObj()!=null && !tile.getWorldObj().isRemote) tile.getWorldObj().markBlockForUpdate(tile.xCoord,tile.yCoord,tile.zCoord); }
    private QuestScreens() {}
}
