package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;import java.util.function.*;
import net.minecraft.client.gui.GuiScreen;import net.minecraft.item.*;import net.minecraft.util.StatCollector;
/** Native reward table and optional physical crate properties, committed on Accept. */
final class QuestRewardTableSettingsScreen extends QuestConfigScreen {
    private final QuestCore.Book book;private final String id;private final QuestCore.LootSettings settings;
    QuestRewardTableSettingsScreen(GuiScreen parent,QuestCore.Book book,String id){super(parent);this.book=book;this.id=id;QuestCore.LootSettings current=book.lootSettings.get(id);settings=current==null?new QuestCore.LootSettings():QuestService.JSON.fromJson(QuestService.JSON.toJson(current),QuestCore.LootSettings.class);}
    protected String title(){return "FTB Quests";}
    private String tr(String key){return StatCollector.translateToLocal(key);}
    private Entry string(String group,String key,Supplier<String> get,Consumer<String> put){return new Entry(group,tr(key),()->"\""+get.get()+"\"",()->0xFFAA49,(b,g)->mc.displayGuiScreen(new QuestValueScreen(this,get.get(),0xFFAA49,256,put)));}
    private Entry number(String group,String key,IntSupplier get,IntConsumer put){return new Entry(group,tr(key),()->Integer.toString(get.getAsInt()),()->0xAA5AE8,(b,g)->mc.displayGuiScreen(new QuestValueScreen(this,Integer.toString(get.getAsInt()),0xAA5AE8,10,s->{int value=Integer.parseInt(s);if(value<0 || value>1000000)throw new IllegalArgumentException("Invalid weight");put.accept(value);})));}
    private Entry bool(String key,BooleanSupplier get,Consumer<Boolean> put){return new Entry("FTB Quests",tr(key),()->Boolean.toString(get.getAsBoolean()),()->get.getAsBoolean()?0x33AA33:0xD52834,(b,g)->put.accept(!get.getAsBoolean()));}
    private ItemStack icon(){Item item=(Item)Item.itemRegistry.getObject(settings.icon);if(item==null)return null;ItemStack stack=new ItemStack(item,1,settings.iconMeta);stack.setTagCompound(QuestService.tag(settings.iconNbt));return stack;}
    protected List<Entry> entries(){
        String root=tr("ftbquests.reward_table"),crate=root+" > "+tr("item.ftbquests.lootcrate.name"),drops=crate+" > "+tr("ftbquests.loot.entitydrops");List<Entry> rows=new ArrayList<Entry>();
        rows.add(string(root,"ftbquests.title",()->settings.name,s->settings.name=s));
        rows.add(new Entry(root,tr("ftbquests.icon"),()->icon()==null?QuestText.tr("Air"):icon().getDisplayName(),()->0x999999,(b,g)->mc.displayGuiScreen(new QuestItemSelectScreen(this,icon(),true,s->true,s->{settings.icon=s==null?"":(String)Item.itemRegistry.getNameForObject(s.getItem());settings.iconMeta=s==null?0:s.getItemDamage();settings.iconNbt=s!=null && s.hasTagCompound()?s.getTagCompound().toString():"";}))));
        rows.add(new Entry(root,tr("ftbquests.tags"),()->"...",()->0xFFAA49,(b,g)->mc.displayGuiScreen(new QuestValueListScreen<String>(this,tr("ftbquests.tags"),settings.tags,()->"",s->"\""+s+"\"",(parent,s,accept)->mc.displayGuiScreen(new QuestValueScreen(parent,s,0xFFAA49,256,accept)),0xFFAA49))));
        rows.add(number(root,"ftbquests.reward_table.empty_weight",()->settings.emptyWeight,v->settings.emptyWeight=v));
        rows.add(number(root,"ftbquests.reward_table.loot_size",()->settings.rolls,v->settings.rolls=v));
        Entry hide=bool("ftbquests.reward_table.hide_tooltip",()->settings.hideTooltip,v->settings.hideTooltip=v);rows.add(new Entry(root,hide.label,hide.value,hide.color,hide.edit));
        Entry use=bool("ftbquests.reward_table.use_title",()->settings.useTitle,v->settings.useTitle=v);rows.add(new Entry(root,use.label,use.value,use.color,use.edit));
        if(!settings.alias.isEmpty()){
            rows.add(string(crate,"ftbquests.reward_table.loot_crate.id",()->settings.alias,s->{if(!s.matches("[a-z0-9_]+"))throw new IllegalArgumentException("Invalid crate ID");settings.alias=s;}));
            rows.add(string(crate,"ftbquests.reward_table.loot_crate.item_name",()->settings.itemName,s->settings.itemName=s));
            rows.add(string(crate,"ftbquests.reward_table.loot_crate.color",()->"#"+settings.color.replace("#",""),s->{if(!s.matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException("Invalid color");settings.color=s;}));
            Entry glow=bool("ftbquests.reward_table.loot_crate.glow",()->settings.glow,v->settings.glow=v);rows.add(new Entry(crate,glow.label,glow.value,glow.color,glow.edit));
            rows.add(number(drops,"ftbquests.loot.entitytype.passive",()->settings.drops.passive,v->settings.drops.passive=v));
            rows.add(number(drops,"ftbquests.loot.entitytype.monster",()->settings.drops.monster,v->settings.drops.monster=v));
            rows.add(number(drops,"ftbquests.loot.entitytype.boss",()->settings.drops.boss,v->settings.drops.boss=v));
        }
        return rows;
    }
    protected void save(){if(settings.emptyWeight<0 || settings.emptyWeight>1000000 || settings.rolls<1 || settings.rolls>64)throw new IllegalArgumentException("Invalid loot settings");settings.drops.validate();QuestPresentation.color(settings.color);book.lootSettings.put(id,settings);}
}
