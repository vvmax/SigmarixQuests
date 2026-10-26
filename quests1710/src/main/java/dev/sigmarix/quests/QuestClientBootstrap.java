package dev.sigmarix.quests;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.*;
import net.minecraft.client.gui.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiOpenEvent;

/** Entry point used only by the separately assembled remote-client artifact. */
@SideOnly(Side.CLIENT)
public final class QuestClientBootstrap {
    public static void preInit(QuestsMod mod,FMLPreInitializationEvent event) {
        QuestNetwork.initClient();
        cpw.mods.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(mod,new QuestGuiHandler());
        GameRegistry.registerItem(new QuestsMod.QuestBookItem(),"quest_book");
        QuestBlocks.registerContent();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new QuestClientBootstrap());
    }
    public static void rejectServer() { throw new IllegalStateException("Client-only Sigmarix Quests: use the full JAR for singleplayer or a server"); }
    @SubscribeEvent public void open(GuiOpenEvent event) {
        if(event.gui instanceof GuiSelectWorld) event.gui=new GuiErrorScreen("Сборка для подключения к серверам","Для одиночной игры установите полную версию Sigmarix Quests.");
    }
    public static QuestCore.LootSettings lootSettings(ItemStack stack) {
        QuestCore.Book book=QuestLoot.clientBook;
        return book==null?null:book.lootSettings.get(QuestLoot.resolve(book,QuestLoot.table(stack.getTagCompound())));
    }
    public static boolean activate(int mode,World world,int x,int y,int z,EntityPlayer player,int side) {
        if(mode==0 || mode==1) {
            if(!(world.getTileEntity(x,y,z) instanceof QuestDevice))return false;
            QuestDevice core=QuestScreens.core((QuestDevice)world.getTileEntity(x,y,z));
            return core!=null && !((mode==1 || !player.isSneaking()) && side!=core.facing);
        }
        return !player.isSneaking() || mode==6 || mode==7 || mode==8;
    }
}
