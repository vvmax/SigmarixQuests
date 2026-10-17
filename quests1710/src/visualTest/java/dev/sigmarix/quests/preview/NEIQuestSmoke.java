package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import net.minecraft.item.ItemStack;
import net.minecraft.init.*;
import net.minecraft.nbt.NBTTagCompound;
/** Checks actual NEI handler registration, queries, loot weights and recipe GUI. */
public final class NEIQuestSmoke {
    public static void run() {
        if(!codechicken.nei.recipe.GuiCraftingRecipe.craftinghandlers.stream().anyMatch(h->h instanceof QuestNEI)) throw new IllegalStateException("Quest NEI crafting handler not registered");
        QuestNEI result=new QuestNEI(); result.loadCraftingRecipes(new ItemStack(Items.bread));
        if(result.numRecipes()!=1 || result.getResultStack(0).item.stackSize!=4) throw new IllegalStateException("NEI quest reward query failed");
        QuestNEI usage=new QuestNEI(); usage.loadUsageRecipes(new ItemStack(Blocks.cobblestone));
        if(usage.numRecipes()!=1 || usage.getIngredientStacks(0).get(0).item.stackSize!=16) throw new IllegalStateException("NEI quest ingredient query failed");
        QuestCore.Reward reward=new QuestCore.Reward(); reward.item="minecraft:diamond"; reward.weight=3;
        QuestCore.LootSettings settings=new QuestCore.LootSettings(); settings.emptyWeight=1;
        QuestCore.Book book=ClientProxy.snapshot.book; book.lootTables.put("preview",java.util.Collections.singletonList(reward)); book.lootSettings.put("preview",settings);
        try {
            ItemStack crate=new ItemStack(cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","loot_crate")); NBTTagCompound tag=new NBTTagCompound(); tag.setString("table","preview"); crate.setTagCompound(tag);
            QuestNEI loot=new QuestNEI(); loot.loadUsageRecipes(crate); if(loot.numRecipes()!=2 || loot.getResultStack(0).item.getItem()!=Items.diamond || loot.getResultStack(1)!=null) throw new IllegalStateException("NEI loot/empty weight query failed");
        } finally { book.lootTables.remove("preview"); book.lootSettings.remove("preview"); }
        if(!codechicken.nei.recipe.GuiCraftingRecipe.openRecipeGui("sigmarixquests")) throw new IllegalStateException("NEI quest recipe GUI did not open");
        System.out.println("Quest NEI PASS: actual recipe/usage registration, quest ingredient/reward queries, crate outcomes and empty weights");
    }
}
