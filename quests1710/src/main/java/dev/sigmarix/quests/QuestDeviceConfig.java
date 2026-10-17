package dev.sigmarix.quests;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
/** One validated device edit, applied after every field and reference has been checked. */
public final class QuestDeviceConfig {
    public String quest="",task="",owner="",skin="",inputItem="",inputNbt="";
    public int skinMeta,inputMeta,power=1;
    public double radius=1.49,offset=.01,height=2.98;
    public boolean inputOnly,fullscreen,hideIcons,indestructible,analog,notifications=true;
    public static QuestDeviceConfig read(QuestDevice tile){
        QuestDeviceConfig c=new QuestDeviceConfig();c.quest=tile.quest;c.task=tile.task;c.owner=tile.owner==null?"":tile.owner.toString();c.skin=tile.screenSkin;c.skinMeta=tile.screenSkinMeta;c.inputOnly=tile.screenInputOnly;c.fullscreen=tile.screenFullscreen;c.hideIcons=tile.screenHideIcons;c.indestructible=tile.indestructible;c.analog=tile.analog;c.notifications=tile.detectorNotifications;c.radius=tile.detectorRadius;c.offset=tile.detectorOffsetY;c.height=tile.detectorHeight;c.power=tile.detectorPower;
        if(tile.screenInputIcon!=null){c.inputItem=(String)Item.itemRegistry.getNameForObject(tile.screenInputIcon.getItem());c.inputMeta=tile.screenInputIcon.getItemDamage();c.inputNbt=tile.screenInputIcon.hasTagCompound()?tile.screenInputIcon.getTagCompound().toString():"";}return c;
    }
    public void apply(QuestService service,QuestDevice tile,EntityPlayerMP actor) throws java.io.IOException {
        int mode=QuestScreenSettings.mode(tile);boolean editor=QuestScreenSettings.editor(actor);
        if(tile.screenLinked || mode<0 || mode==6 || mode==8 || !tile.isUseableByPlayer(actor))throw new IllegalArgumentException("Device unavailable");
        if(quest==null || task==null || owner==null || skin==null || inputItem==null || inputNbt==null || quest.length()>160 || task.length()>64 || skin.length()>256 || inputItem.length()>256 || inputNbt.length()>8192 || skinMeta<0 || skinMeta>15 || inputMeta<0)throw new IllegalArgumentException("Invalid device settings");
        if((mode==4 || mode==5) && !editor)throw new IllegalArgumentException("Operator permission required for detectors");
        if(indestructible!=tile.indestructible && !editor || !owner.equals(tile.owner==null?"":tile.owner.toString()) && !editor)throw new IllegalArgumentException("Operator permission required");
        if(tile.indestructible && !editor && (!quest.equals(tile.quest) || !task.equals(tile.task) || !skin.equals(tile.screenSkin) || skinMeta!=tile.screenSkinMeta || fullscreen!=tile.screenFullscreen || hideIcons!=tile.screenHideIcons))throw new IllegalArgumentException("Screen is protected");
        if(!Double.isFinite(radius) || radius<.25 || radius>100 || !Double.isFinite(offset) || offset< -1000 || offset>1000 || !Double.isFinite(height) || height<.1 || height>1000 || power<1 || power>15)throw new IllegalArgumentException("Invalid detector settings");
        UUID nextOwner=owner.isEmpty()?null:UUID.fromString(owner);
        if(!quest.isEmpty()){
            if(mode==1 || mode==2 || mode==9){service.validateProgressReference(quest);if(!editor && !service.bindable(actor.getUniqueID(),quest))throw new IllegalArgumentException("Object is not visible");}
            else {QuestCore.Quest q=service.quests.get(quest);if(q==null || !editor && !service.visible(actor.getUniqueID(),q))throw new IllegalArgumentException("Unknown quest");
                if(mode==3)service.validateCollectorReward(q,task);
                else if(!task.isEmpty() && !q.tasks.stream().anyMatch(t->t.id.equals(task)))throw new IllegalArgumentException("Unknown task");
            }
        }else if(!task.isEmpty())throw new IllegalArgumentException("Task requires a quest");
        boolean screen=mode<=1;
        if(!screen && (!skin.equals(tile.screenSkin) || skinMeta!=tile.screenSkinMeta || inputOnly!=tile.screenInputOnly || fullscreen!=tile.screenFullscreen || hideIcons!=tile.screenHideIcons || mode!=7 && indestructible!=tile.indestructible || !inputItem.isEmpty()))throw new IllegalArgumentException("Setting does not apply to this device");
        if(screen && !skin.isEmpty()) {if(!Block.blockRegistry.containsKey(skin))throw new IllegalArgumentException("Unknown skin block");Block b=(Block)Block.blockRegistry.getObject(skin);if(b instanceof QuestBlocks.DeviceBlock || b.hasTileEntity(skinMeta) || b!=net.minecraft.init.Blocks.air && b.getRenderType()<0)throw new IllegalArgumentException("Invalid skin block");}
        ItemStack icon=null;if(!inputItem.isEmpty()){Item item=(Item)Item.itemRegistry.getObject(inputItem);if(item==null)throw new IllegalArgumentException("Unknown icon item");icon=new ItemStack(item,1,inputMeta);icon.setTagCompound(QuestService.tag(inputNbt));if(icon.hasTagCompound() && net.minecraft.nbt.CompressedStreamTools.compress(icon.getTagCompound()).length>4096)throw new IllegalArgumentException("Icon NBT is too large");}
        if(!Objects.equals(nextOwner,tile.owner)){if(mode!=0 && mode!=1 && mode!=4 && mode!=7 && mode!=9)throw new IllegalArgumentException("Team cannot be changed on this device");if(nextOwner==null)throw new IllegalArgumentException("Choose a team");}
        // All failure-prone validation is complete before the first mutation.
        if(!Objects.equals(nextOwner,tile.owner)){if(screen)QuestScreens.reassignOwner(tile,nextOwner);else tile.owner=nextOwner;}
        tile.quest=quest;tile.task=task;tile.analog=analog;if(mode==7)tile.indestructible=indestructible;
        if(screen){tile.screenSkin=skin;tile.screenSkinMeta=skinMeta;tile.screenInputOnly=inputOnly;tile.screenFullscreen=fullscreen;tile.screenHideIcons=hideIcons;tile.indestructible=indestructible;tile.screenInputIcon=icon;}
        if(mode==4 || mode==5){tile.configureDetector(radius,offset,height,power);tile.detectorNotifications=notifications;}
        tile.bindingChanged();if(screen)QuestScreens.presentationChanged(tile);else{tile.markDirty();if(tile.getWorldObj()!=null)tile.getWorldObj().markBlockForUpdate(tile.xCoord,tile.yCoord,tile.zCoord);}
    }
}
