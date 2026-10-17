package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
/** Loaded only after Forge finishes discovering optional APIs. Never packaged in the mod. */
public final class QuestPreviewDevices {
    public static QuestDevice create(final int mode){return new QuestDevice(){@Override public net.minecraft.block.Block getBlockType(){return QuestBlocks.devices[mode];}};}
}
