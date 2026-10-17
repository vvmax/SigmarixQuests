package dev.sigmarix.quests.client;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.recipe.TemplateRecipeHandler;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
/** Native NEI counterpart: visible quest inputs/results and weighted crate contents. */
public final class QuestNEI extends TemplateRecipeHandler {
    public static void refresh() {
        QuestCore.Book book=QuestLoot.clientBook; if(book==null) return;
        net.minecraft.item.Item item=cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","loot_crate"); List<ItemStack> variants=new ArrayList<ItemStack>();
        for(String table:book.lootTables.keySet()) variants.add(QuestLoot.crate(book,table));
        API.setItemListEntries(item,variants); codechicken.nei.ItemList.loadItems();
    }
    public static void register() { API.registerRecipeHandler(new QuestNEI()); API.registerUsageHandler(new QuestNEI()); }
    @Override public String getRecipeName() { return "Quests / loot"; }
    @Override public String getGuiTexture() { return "minecraft:textures/gui/container/inventory.png"; }
    @Override public int recipiesPerPage() { return 2; }
    private QuestCore.Book book() { return ClientProxy.snapshot==null?null:ClientProxy.snapshot.book; }
    @Override public void loadCraftingRecipes(String id,Object... results) {
        if(id.equals("sigmarixquests")) {
            QuestCore.Book book=book(); if(book!=null) for(QuestCore.Quest quest:book.quests) if(QuestPresentation.itemBrowser(book,quest)) arecipes.add(row(quest));
        } else super.loadCraftingRecipes(id,results);
    }
    @Override public void loadCraftingRecipes(ItemStack result) {
        QuestCore.Book book=book(); if(book==null) return;
        for(QuestCore.Quest quest:book.quests) if(QuestPresentation.itemBrowser(book,quest)) { Row row=row(quest); if(contains(row.outputs,result)) arecipes.add(row); }
    }
    @Override public void loadUsageRecipes(ItemStack ingredient) {
        QuestCore.Book book=book(); if(book==null) return;
        for(QuestCore.Quest quest:book.quests) if(QuestPresentation.itemBrowser(book,quest)) { Row row=row(quest); if(contains(row.inputs,ingredient)) arecipes.add(row); }
        if(ingredient.getItem()==cpw.mods.fml.common.registry.GameRegistry.findItem("sigmarixquests","loot_crate")) {
            String selected=QuestLoot.resolve(ClientProxy.snapshot.book,QuestLoot.table(ingredient.getTagCompound()));
            for(Map.Entry<String,List<QuestCore.Reward>> table:book.lootTables.entrySet()) if(selected.isEmpty() || selected.equals(table.getKey())) {
                QuestCore.LootSettings settings=book.lootSettings.get(table.getKey()); int total=settings==null?0:settings.emptyWeight;
                for(QuestCore.Reward reward:table.getValue()) total+=reward.weight;
                for(QuestCore.Reward reward:table.getValue()) {
                    List<PositionedStack> outputs=new ArrayList<PositionedStack>(); rewards(reward,outputs,0);
                    arecipes.add(new Row("Loot: "+table.getKey(),Collections.singletonList(new PositionedStack(ingredient.copy(),8,18)),outputs,"Weight "+reward.weight+"/"+total+"; rolls "+(settings==null?1:settings.rolls)));
                }
                if(settings!=null && settings.emptyWeight>0) arecipes.add(new Row("Loot: "+table.getKey(),Collections.singletonList(new PositionedStack(ingredient.copy(),8,18)),Collections.<PositionedStack>emptyList(),"Empty weight "+settings.emptyWeight+"/"+total));
            }
        }
    }
    private boolean contains(List<PositionedStack> list,ItemStack query) {
        for(PositionedStack stack:list) for(ItemStack option:stack.items) if(option.getItem()==query.getItem() && (option.getItemDamage()==OreDictionary.WILDCARD_VALUE || query.getItemDamage()==OreDictionary.WILDCARD_VALUE || option.getItemDamage()==query.getItemDamage())) return true;
        return false;
    }
    private Row row(QuestCore.Quest quest) {
        List<PositionedStack> inputs=new ArrayList<PositionedStack>(),outputs=new ArrayList<PositionedStack>();
        for(QuestCore.Task task:quest.tasks) if(task.type.equals("item")) {
            List<ItemStack> alternatives=new ArrayList<ItemStack>(); List<String> targets=new ArrayList<String>(task.targets); if(task.target!=null && !task.target.isEmpty()) targets.add(task.target);
            for(String target:targets) {
                if(target.startsWith("ore:")) { for(ItemStack ore:OreDictionary.getOres(target.substring(4))) alternatives.add(ore.copy()); }
                else { Item item=(Item)Item.itemRegistry.getObject(target); if(item!=null) { ItemStack stack=new ItemStack(item,(int)Math.min(64,task.count),task.ignoreDamage?OreDictionary.WILDCARD_VALUE:task.meta); stack.setTagCompound(QuestService.tag(task.nbt)); alternatives.add(stack); } }
            }
            if(!alternatives.isEmpty()) inputs.add(new PositionedStack(alternatives,8+(inputs.size()%3)*18,18+(inputs.size()/3)*18));
        }
        for(QuestCore.Reward reward:quest.rewards) rewards(reward,outputs,0);
        return new Row(quest.title,inputs,outputs,"Quest: "+quest.id+" (choices/loot shown as options)");
    }
    private void rewards(QuestCore.Reward reward,List<PositionedStack> output,int depth) {
        if(depth>16 || output.size()>=6) return;
        if(reward.type.equals("item")) {
            Item item=(Item)Item.itemRegistry.getObject(reward.item); if(item==null) return;
            ItemStack stack=new ItemStack(item,Math.min(64,reward.count),reward.meta); stack.setTagCompound(QuestService.tag(reward.nbt)); output.add(new PositionedStack(stack,112+(output.size()%3)*18,18+(output.size()/3)*18));
        } else if(reward.type.equals("loot") && book()!=null && book().lootTables.containsKey(reward.target)) for(QuestCore.Reward child:book().lootTables.get(reward.target)) rewards(child,output,depth+1);
        else for(QuestCore.Reward child:reward.options) rewards(child,output,depth+1);
    }
    @Override public void drawBackground(int index) { Gui.drawRect(0,0,166,64,0xFF26313D); }
    @Override public void drawExtras(int index) {
        Row row=(Row)arecipes.get(index); net.minecraft.client.gui.FontRenderer font=Minecraft.getMinecraft().fontRenderer;
        font.drawString(font.trimStringToWidth(row.title,158),4,4,0xFFFFFF); font.drawString("->",80,22,0xB8CDD9); font.drawString(font.trimStringToWidth(row.detail,158),4,54,0xB8CDD9);
    }
    private final class Row extends CachedRecipe {
        final String title,detail; final List<PositionedStack> inputs,outputs;
        Row(String title,List<PositionedStack> inputs,List<PositionedStack> outputs,String detail) { this.title=title; this.inputs=inputs; this.outputs=outputs; this.detail=detail; }
        @Override public PositionedStack getResult() { return outputs.isEmpty()?null:outputs.get(0); }
        @Override public List<PositionedStack> getIngredients() { return inputs.subList(0,Math.min(6,inputs.size())); }
        @Override public List<PositionedStack> getOtherStacks() { return outputs.size()<2?Collections.<PositionedStack>emptyList():outputs.subList(1,outputs.size()); }
    }
}
