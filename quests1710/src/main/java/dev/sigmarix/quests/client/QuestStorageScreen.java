package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.inventory.GuiChest;
/** Keeps normal chest interaction while showing the stock beyond visible inventory stacks. */
public final class QuestStorageScreen extends GuiChest {
    private final QuestStorage storage;
    private final QuestStorageContainer container;
    public QuestStorageScreen(EntityPlayer player,QuestStorage storage) {
        super(player.inventory,storage); this.storage=storage; container=new QuestStorageContainer(player,storage); inventorySlots=container;
    }
    @Override public void initGui() { super.initGui(); if(storage.crates()) buttonList.add(new net.minecraft.client.gui.GuiButton(10,guiLeft+xSize-22,guiTop+3,16,12,">")); }
    @Override protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
        if(button.id==10) { QuestNetwork.Action action=new QuestNetwork.Action(); action.action="storage_page"; action.x=storage.xCoord; action.y=storage.yCoord; action.z=storage.zCoord; ClientProxy.send(action); }
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        if(!storage.crates()) { super.drawGuiContainerForegroundLayer(x,y); return; }
        fontRendererObj.drawString(QuestText.tr("Crates"),8,6,0x404040);
        String text="+"+compact(container.reserved); fontRendererObj.drawString(text,xSize-28-fontRendererObj.getStringWidth(text),6,0x404040);
        fontRendererObj.drawString(net.minecraft.util.StatCollector.translateToLocal("container.inventory"),8,ySize-94,0x404040);
    }
    private static String compact(long count) {
        if(count<1000) return Long.toString(count);
        double value=count; String unit="";
        for(String candidate:new String[]{"K","M","B","T"}) { value/=1000; unit=candidate; if(value<1000) break; }
        return String.format(java.util.Locale.ROOT,"%.1f%s",value,unit);
    }
    @Override public void drawScreen(int x,int y,float partial) {
        super.drawScreen(x,y,partial);
        if(storage.crates() && x>=guiLeft && x<guiLeft+xSize && y>=guiTop && y<guiTop+18) drawHoveringText(java.util.Arrays.asList(QuestText.tr("Virtual reserve")+": "+container.reserved,QuestText.tr("Next types")+": >"),x,y,fontRendererObj);
    }
}
