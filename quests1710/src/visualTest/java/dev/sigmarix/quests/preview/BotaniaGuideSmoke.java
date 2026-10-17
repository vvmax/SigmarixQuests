package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.lexicon.*;
import vazkii.botania.client.gui.lexicon.GuiLexiconEntry;
/** Calls the guide bridge against official Botania r1.8-249's real GUI and knowledge checks. */
public final class BotaniaGuideSmoke {
    public static void open() {
        ItemStack book=null; for(ItemStack item:Minecraft.getMinecraft().thePlayer.inventory.mainInventory) if(item!=null && item.getItem() instanceof ILexicon) book=item;
        if(book==null) throw new IllegalStateException("Official Botania lexicon did not synchronize");
        LexiconEntry chosen=null;
        for(LexiconEntry entry:BotaniaAPI.getAllEntries()) if(entry.isVisible()) {
            boolean unlocked=((ILexicon)book.getItem()).isKnowledgeUnlocked(book,entry.getKnowledgeType()); String path="botania:"+entry.getUnlocalizedName();
            if(!unlocked && QuestGuides.available(path)) throw new IllegalStateException("Locked Botania knowledge was exposed by guide link");
            if(unlocked && chosen==null) chosen=entry;
        }
        if(chosen==null || !QuestGuides.available("botania:"+chosen.getUnlocalizedName()) || !QuestGuides.open("botania:"+chosen.getUnlocalizedName(),Minecraft.getMinecraft().currentScreen)) throw new IllegalStateException("Official Botania guide could not be opened");
        if(!(Minecraft.getMinecraft().currentScreen instanceof GuiLexiconEntry) || ((GuiLexiconEntry)Minecraft.getMinecraft().currentScreen).getEntry()!=chosen) throw new IllegalStateException("Wrong native Botania guide page opened");
    }
}
