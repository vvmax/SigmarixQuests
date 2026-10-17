package dev.sigmarix.quests.preview;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
/** Exercises a registered guide and a separately discovered Forge guide provider. */
public final class GuideSmoke {
    public static int eventOpens,indexOpens,clickOpens,shopOpens;
    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GuideSmoke());
        QuestGuides.register("",parent->{ indexOpens++; show(parent,"Guide index"); });
        QuestGuides.register("preview:registered",parent->show(parent,"Registered guide"));
        if(!QuestGuides.available("preview:registered") || !QuestGuides.available("preview:event") || QuestGuides.available("preview:missing")) throw new IllegalStateException("Guide discovery failed");
    }
    private static void show(GuiScreen parent,String title) { QuestCore.Quest guide=new QuestCore.Quest(); guide.title=title; guide.pages.add("An independent external guide provider opened this page."); Minecraft.getMinecraft().displayGuiScreen(new QuestGuideScreen(parent,guide)); }
    @SubscribeEvent public void check(QuestGuideEvent.Check event) { if(event.path.equals("preview:event")) event.available=true; }
    @SubscribeEvent public void open(QuestGuideEvent.Open event) { if(event.path.equals("preview:event") && !event.opened) { eventOpens++; event.opened=true; show(event.parent,"External guide event"); } }
    @SubscribeEvent public void clickCheck(QuestClickEvent.Check event) { if(event.type.equals("custom") && (event.value.equals("preview:click") || event.value.equals("ftbmoney:open_gui"))) event.available=true; }
    @SubscribeEvent public void clickOpen(QuestClickEvent.Open event) { if(event.type.equals("custom") && event.value.equals("ftbmoney:open_gui")) { shopOpens++; event.opened=true; show(event.parent,"External shop provider"); return; } if(event.type.equals("custom") && event.value.equals("preview:click")) { clickOpens++; event.opened=true; show(event.parent,"Custom quest click"); } }
    public static void registry(GuiScreen parent) {
        if(!QuestGuides.open("preview:registered",parent) || !(Minecraft.getMinecraft().currentScreen instanceof QuestGuideScreen)) throw new IllegalStateException("Registered guide failed to open");
    }
}
