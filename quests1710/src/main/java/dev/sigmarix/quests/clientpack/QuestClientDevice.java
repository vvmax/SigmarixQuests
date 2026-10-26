package dev.sigmarix.quests.clientpack;
import dev.sigmarix.quests.*;
import java.util.UUID;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
/** Client tile state and inventory prediction; never ticks or accepts energy/fluids. */
@cpw.mods.fml.common.Optional.InterfaceList({@cpw.mods.fml.common.Optional.Interface(iface="cofh.api.energy.IEnergyReceiver",modid="CoFHAPI|energy"),@cpw.mods.fml.common.Optional.Interface(iface="ic2.api.energy.tile.IEnergySink",modid="IC2"),@cpw.mods.fml.common.Optional.Interface(iface="vazkii.botania.api.mana.IManaReceiver",modid="Botania")})
public class QuestClientDevice extends TileEntity implements net.minecraft.inventory.ISidedInventory,net.minecraftforge.fluids.IFluidHandler,cofh.api.energy.IEnergyReceiver,ic2.api.energy.tile.IEnergySink,vazkii.botania.api.mana.IManaReceiver {
    public UUID owner;
    public String quest="",task="",bindingProfile="default";
    public boolean signal,analog;
    private int lastRedstoneInput;
    public double detectorRadius=1.49,detectorOffsetY=1.01,detectorHeight=2.98;
    public int detectorPower=1;
    public boolean detectorNotifications=true;
    public int redstoneOutput;
    public int facing=2,screenPercent,screenWidth=1,screenHeight=1,coreX,coreY,coreZ;
    public boolean screenLinked,screenFullscreen,screenHideIcons,screenInputOnly;
    public net.minecraft.item.ItemStack screenInputIcon;
    public boolean indestructible;
    public String screenSkin="";
    public int screenSkinMeta;
    public String screenTitle="Quest screen";
    public long screenTaskProgress;

    protected net.minecraft.item.ItemStack input;
    protected final net.minecraft.item.ItemStack[] collectorItems=new net.minecraft.item.ItemStack[9];
    public int queuedLoot;
    public boolean opener(){return mode()==6;}
    public boolean collector(){return mode()==3;}
    protected int mode(){return getBlockType() instanceof QuestBlocks.DeviceBlock?((QuestBlocks.DeviceBlock)getBlockType()).mode:0;}
    public void configureDetector(double radius,double offset,double height,int power){detectorRadius=radius;detectorOffsetY=offset;detectorHeight=height;detectorPower=power;}
    public AxisAlignedBB detectorBounds(){return AxisAlignedBB.getBoundingBox(xCoord+.5-detectorRadius,yCoord+detectorOffsetY,zCoord+.5-detectorRadius,xCoord+.5+detectorRadius,yCoord+detectorOffsetY+detectorHeight,zCoord+.5+detectorRadius);}
    public AxisAlignedBB getRenderBoundingBox(){return AxisAlignedBB.getBoundingBox(xCoord-8,yCoord,zCoord-8,xCoord+9,yCoord+9,zCoord+9);}
    public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet){String old=screenSkin;int meta=screenSkinMeta;net.minecraft.item.ItemStack held=input;readFromNBT(packet.func_148857_g());input=held;if(worldObj!=null && (!old.equals(screenSkin)||meta!=screenSkinMeta))worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);}
    public int getSizeInventory(){return opener()?10:collector()?9:1;}
    public net.minecraft.item.ItemStack getStackInSlot(int slot){return collector()?slot>=0&&slot<9?collectorItems[slot]:null:opener()&&slot>0&&slot<=9?collectorItems[slot-1]:slot==0?input:null;}
    public void setInventorySlotContents(int slot,net.minecraft.item.ItemStack stack){if(collector()&&slot>=0&&slot<9)collectorItems[slot]=stack;else if(opener()&&slot>0&&slot<=9)collectorItems[slot-1]=stack;else if(slot==0)input=stack;}
    public net.minecraft.item.ItemStack decrStackSize(int slot,int count){net.minecraft.item.ItemStack stack=getStackInSlot(slot);if(stack==null||count<=0)return null;net.minecraft.item.ItemStack result=stack.splitStack(Math.min(count,stack.stackSize));if(stack.stackSize<=0)setInventorySlotContents(slot,null);return result;}
    public net.minecraft.item.ItemStack getStackInSlotOnClosing(int slot){net.minecraft.item.ItemStack stack=getStackInSlot(slot);setInventorySlotContents(slot,null);return stack;}
    public String getInventoryName(){return "container.sigmarixquests.device";}
    public boolean hasCustomInventoryName(){return false;}
    public int getInventoryStackLimit(){return 64;}
    public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player){return worldObj!=null && worldObj.getTileEntity(xCoord,yCoord,zCoord)==this && player.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64;}
    public void openInventory(){} public void closeInventory(){}
    public boolean isItemValidForSlot(int slot,net.minecraft.item.ItemStack stack){return slot==0&&stack!=null&&!collector()&&(!opener()||stack.getItem() instanceof QuestBlocks.LootCrateItem);}
    public int[] getAccessibleSlotsFromSide(int side){int[] slots=new int[getSizeInventory()];for(int i=0;i<slots.length;i++)slots[i]=i;return slots;}
    public boolean canInsertItem(int slot,net.minecraft.item.ItemStack stack,int side){return isItemValidForSlot(slot,stack);}
    public boolean canExtractItem(int slot,net.minecraft.item.ItemStack stack,int side){return collector()||opener()&&slot>0;}
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);queuedLoot=nbt.getInteger("queuedLoot");
        try { owner=UUID.fromString(nbt.getString("owner")); } catch(IllegalArgumentException e) { owner=null; }
        input=nbt.hasKey("input")?net.minecraft.item.ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("input")):null;
        detectorNotifications=!nbt.hasKey("detectorNotifications") || nbt.getBoolean("detectorNotifications");
        if(nbt.hasKey("detectorRadius")) try { configureDetector(nbt.getDouble("detectorRadius"),nbt.getDouble("detectorOffsetY"),nbt.getDouble("detectorHeight"),nbt.getInteger("detectorPower")); } catch(IllegalArgumentException ignored) {}
        analog=nbt.getBoolean("analog"); redstoneOutput=Math.max(0,Math.min(15,nbt.getInteger("redstoneOutput")));
        screenWidth=Math.max(1,Math.min(8,nbt.getInteger("screenWidth"))); screenHeight=Math.max(1,Math.min(8,nbt.getInteger("screenHeight")));
        indestructible=nbt.getBoolean("indestructible"); screenSkin=nbt.getString("screenSkin"); if(screenSkin.length()>256) screenSkin=""; screenSkinMeta=Math.max(0,Math.min(15,nbt.getInteger("screenSkinMeta")));
        screenFullscreen=nbt.getBoolean("screenFullscreen"); screenHideIcons=nbt.getBoolean("screenHideIcons"); screenInputOnly=nbt.getBoolean("screenInputOnly"); screenInputIcon=nbt.hasKey("screenInputIcon")?net.minecraft.item.ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("screenInputIcon")):null;
        screenLinked=nbt.getBoolean("screenLinked"); coreX=nbt.getInteger("coreX"); coreY=nbt.getInteger("coreY"); coreZ=nbt.getInteger("coreZ");
        facing=nbt.hasKey("facing")?nbt.getInteger("facing"):2; if(facing<2 || facing>5) facing=2;
        screenTaskProgress=Math.max(0,nbt.getLong("screenTaskProgress"));
        screenPercent=Math.max(0,Math.min(100,nbt.getInteger("screenPercent"))); screenTitle=nbt.hasKey("screenTitle")?nbt.getString("screenTitle"):"Quest screen";
        bindingProfile=nbt.hasKey("bindingProfile")?nbt.getString("bindingProfile"):"default";
        quest=nbt.getString("quest"); task=nbt.getString("task"); signal=nbt.getBoolean("signal");
    }
    private QuestCore.Task displayTask(String type){dev.sigmarix.quests.QuestNetwork.Snapshot view=dev.sigmarix.quests.client.ClientProxy.snapshot;if(view!=null)for(QuestCore.Quest q:view.book.quests)if(q.id.equals(quest))for(QuestCore.Task t:q.tasks)if(t.id.equals(task)&&t.type.equals(type))return t;return null;}
    private long displayRemaining(String type){QuestCore.Task definition=displayTask(type);return definition==null?0:Math.max(0,definition.count-screenTaskProgress);}
    public int fill(net.minecraftforge.common.util.ForgeDirection side,net.minecraftforge.fluids.FluidStack fluid,boolean commit){return 0;}
    public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.common.util.ForgeDirection side,net.minecraftforge.fluids.FluidStack fluid,boolean commit){return null;}
    public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.common.util.ForgeDirection side,int amount,boolean commit){return null;}
    public boolean canFill(net.minecraftforge.common.util.ForgeDirection side,net.minecraftforge.fluids.Fluid fluid){QuestCore.Task t=displayTask("fluid");return fluid!=null&&t!=null&&fluid.getName().equals(t.target)&&displayRemaining("fluid")>0;}
    public boolean canDrain(net.minecraftforge.common.util.ForgeDirection side,net.minecraftforge.fluids.Fluid fluid){return false;}
    public net.minecraftforge.fluids.FluidTankInfo[] getTankInfo(net.minecraftforge.common.util.ForgeDirection side){return new net.minecraftforge.fluids.FluidTankInfo[]{new net.minecraftforge.fluids.FluidTankInfo(null,(int)Math.min(Integer.MAX_VALUE,displayRemaining("fluid")))};}
    public boolean canConnectEnergy(net.minecraftforge.common.util.ForgeDirection side){return displayTask("energy")!=null||displayTask("buildcraft_mj")!=null;}
    public int receiveEnergy(net.minecraftforge.common.util.ForgeDirection side,int amount,boolean simulate){return 0;}
    public int getEnergyStored(net.minecraftforge.common.util.ForgeDirection side){return 0;}
    public int getMaxEnergyStored(net.minecraftforge.common.util.ForgeDirection side){return (int)Math.min(Integer.MAX_VALUE,displayRemaining("energy"));}
    @cpw.mods.fml.common.Optional.Method(modid="IC2") public boolean acceptsEnergyFrom(TileEntity emitter,net.minecraftforge.common.util.ForgeDirection side){return displayTask("ic2_energy")!=null;}
    @cpw.mods.fml.common.Optional.Method(modid="IC2") public double getDemandedEnergy(){return displayRemaining("ic2_energy");}
    @cpw.mods.fml.common.Optional.Method(modid="IC2") public int getSinkTier(){QuestCore.Task t=displayTask("ic2_energy");return t==null?4:t.energyTier;}
    @cpw.mods.fml.common.Optional.Method(modid="IC2") public double injectEnergy(net.minecraftforge.common.util.ForgeDirection side,double amount,double voltage){return amount;}
    @cpw.mods.fml.common.Optional.Method(modid="Botania") public int getCurrentMana(){return 0;}
    @cpw.mods.fml.common.Optional.Method(modid="Botania") public boolean isFull(){return displayRemaining("botania_mana")==0;}
    @cpw.mods.fml.common.Optional.Method(modid="Botania") public boolean canRecieveManaFromBursts(){return !isFull();}
    @cpw.mods.fml.common.Optional.Method(modid="Botania") public void recieveMana(int amount){}
}
