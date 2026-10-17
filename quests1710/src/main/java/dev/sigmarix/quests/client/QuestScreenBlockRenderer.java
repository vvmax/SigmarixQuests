package dev.sigmarix.quests.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import dev.sigmarix.quests.*;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;

/** Delegates cosmetic skins to native block renderers, including transparent render passes. */
final class QuestScreenBlockRenderer implements ISimpleBlockRenderingHandler {
    private final int id;
    QuestScreenBlockRenderer(int id) { this.id=id; }
    @Override public int getRenderId() { return id; }
    @Override public boolean shouldRender3DInInventory(int modelId) { return true; }
    @Override public void renderInventoryBlock(Block block,int metadata,int modelId,RenderBlocks renderer) { renderer.renderBlockAsItem(Blocks.lapis_block,0,1); }
    @Override public boolean renderWorldBlock(IBlockAccess world,int x,int y,int z,Block block,int modelId,RenderBlocks renderer) {
        TileEntity raw=world.getTileEntity(x,y,z); Block skin=raw instanceof QuestDevice?QuestScreenSettings.skin((QuestDevice)raw):null;
        int pass=MinecraftForgeClient.getRenderPass();
        if(skin==null) return pass==0 && renderer.renderStandardBlock(block,x,y,z);
        if(skin==Blocks.air || !skin.canRenderInPass(pass) || skin.getRenderType()<0) return false;
        return new RenderBlocks(new QuestSkinAccess(world)).renderBlockByRenderType(skin,x,y,z);
    }
}
