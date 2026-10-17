package dev.sigmarix.quests;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
/** Cosmetic icon editing never accepts arbitrary item NBT from the client. */
public final class QuestIcons {
    public static final String DEFAULT="minecraft:textures/items/paper.png";
    public static String resource(ItemStack stack) {
        String icon=stack!=null && stack.hasTagCompound()?stack.getTagCompound().getString("icon"):"";
        return icon.length()<=1024 && !icon.isEmpty() && QuestPresentation.image(icon)?icon:DEFAULT;
    }
    public static int color(ItemStack stack) { return stack!=null && stack.hasTagCompound() && stack.getTagCompound().hasKey("color")?stack.getTagCompound().getInteger("color") & 0xFFFFFF:0xFFFFFF; }
    public static void edit(EntityPlayerMP player,String resource,String name,int color,int slot) {
        ItemStack held=player.getHeldItem();
        if(held==null || !(held.getItem() instanceof QuestBlocks.CustomIconItem) || slot!=player.inventory.currentItem) throw new IllegalArgumentException("Hold the custom icon being edited");
        if(resource==null || resource.isEmpty() || resource.length()>1024 || !QuestPresentation.image(resource) || name==null || name.length()>256 || color<0 || color>0xFFFFFF) throw new IllegalArgumentException("Invalid icon texture, name or RRGGBB color");
        NBTTagCompound tag=held.hasTagCompound()?(NBTTagCompound)held.getTagCompound().copy():new NBTTagCompound();
        tag.setString("icon",resource); tag.setString("name",name); tag.setInteger("color",color); held.setTagCompound(tag);
        player.inventory.markDirty(); player.inventoryContainer.detectAndSendChanges();
    }
    private QuestIcons() {}
}
