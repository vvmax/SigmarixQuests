package dev.sigmarix.quests.client;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
/** Optional official Botania r1.8-249 lexicon navigation. No GTNH API is used. */
public final class QuestBotaniaGuide {
    public static void register() { if(Loader.isModLoaded("Botania")) MinecraftForge.EVENT_BUS.register(new Adapter()); }
    public static final class Adapter {
        private ItemStack book() {
            if(Minecraft.getMinecraft().thePlayer==null) return null;
            for(ItemStack stack:Minecraft.getMinecraft().thePlayer.inventory.mainInventory) if(stack!=null && stack.getItem() instanceof vazkii.botania.api.lexicon.ILexicon) return stack;
            return null;
        }
        private boolean index(String path) { return path.isEmpty() || path.equals("botania:") || path.equals("botania:index"); }
        private vazkii.botania.api.lexicon.LexiconEntry entry(String path,ItemStack book) {
            if(!path.startsWith("botania:")) return null; String name=path.substring(8);
            for(vazkii.botania.api.lexicon.LexiconEntry entry:vazkii.botania.api.BotaniaAPI.getAllEntries()) {
                String id=entry.getUnlocalizedName();
                if((id.equals(name) || id.equals("botania.entry."+name)) && entry.isVisible() && ((vazkii.botania.api.lexicon.ILexicon)book.getItem()).isKnowledgeUnlocked(book,entry.getKnowledgeType())) return entry;
            }
            return null;
        }
        @SubscribeEvent public void check(QuestGuideEvent.Check event) { ItemStack book=book(); if(book!=null && (index(event.path) || entry(event.path,book)!=null)) event.available=true; }
        @SubscribeEvent public void open(QuestGuideEvent.Open event) {
            if(event.opened) return; ItemStack book=book(); if(book==null) return;
            vazkii.botania.api.lexicon.LexiconEntry entry=entry(event.path,book); if(entry==null && !index(event.path)) return;
            vazkii.botania.common.item.ItemLexicon.openBook(Minecraft.getMinecraft().thePlayer,book,Minecraft.getMinecraft().theWorld,false);
            vazkii.botania.client.gui.lexicon.GuiLexicon gui=entry==null?new vazkii.botania.client.gui.lexicon.GuiLexicon():new vazkii.botania.client.gui.lexicon.GuiLexiconEntry(entry,event.parent);
            vazkii.botania.client.gui.lexicon.GuiLexicon.stackUsed=book; vazkii.botania.client.gui.lexicon.GuiLexicon.currentOpenLexicon=gui;
            Minecraft.getMinecraft().displayGuiScreen(gui); event.opened=true;
        }
    }
    private QuestBotaniaGuide() {}
}
