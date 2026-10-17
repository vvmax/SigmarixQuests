package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Supplies skin metadata to ordinary 1.7 block renderers without changing the world. */
final class QuestSkinAccess implements IBlockAccess {
    private final IBlockAccess world;
    QuestSkinAccess(IBlockAccess world) { this.world=world; }
    private QuestDevice screen(int x,int y,int z) {
        Block block=world.getBlock(x,y,z); if(!(block instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)block).mode>1) return null;
        TileEntity raw=world.getTileEntity(x,y,z); return raw instanceof QuestDevice?(QuestDevice)raw:null;
    }
    @Override public Block getBlock(int x,int y,int z) { QuestDevice tile=screen(x,y,z); Block skin=tile==null?null:QuestScreenSettings.skin(tile); return skin==null?world.getBlock(x,y,z):skin; }
    @Override public int getBlockMetadata(int x,int y,int z) { QuestDevice tile=screen(x,y,z); return tile!=null && QuestScreenSettings.skin(tile)!=null?tile.screenSkinMeta:world.getBlockMetadata(x,y,z); }
    @Override public TileEntity getTileEntity(int x,int y,int z) { return world.getTileEntity(x,y,z); }
    @Override public int getLightBrightnessForSkyBlocks(int x,int y,int z,int light) { return world.getLightBrightnessForSkyBlocks(x,y,z,light); }
    @Override public int isBlockProvidingPowerTo(int x,int y,int z,int side) { return world.isBlockProvidingPowerTo(x,y,z,side); }
    @Override public boolean isAirBlock(int x,int y,int z) { return getBlock(x,y,z).isAir(this,x,y,z); }
    @Override public BiomeGenBase getBiomeGenForCoords(int x,int z) { return world.getBiomeGenForCoords(x,z); }
    @Override public int getHeight() { return world.getHeight(); }
    @Override public boolean extendedLevelsInChunkCache() { return world.extendedLevelsInChunkCache(); }
    @Override public boolean isSideSolid(int x,int y,int z,ForgeDirection side,boolean fallback) { return getBlock(x,y,z).isSideSolid(this,x,y,z,side); }
}
