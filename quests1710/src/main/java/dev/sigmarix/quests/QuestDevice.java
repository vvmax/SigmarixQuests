package dev.sigmarix.quests;

import cpw.mods.fml.common.Optional;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.*;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

/** Owned task terminal; fluid/RF acceptance credits only the bound active task. */
@Optional.InterfaceList({@Optional.Interface(iface="cofh.api.energy.IEnergyReceiver",modid="CoFHAPI|energy"), @Optional.Interface(iface="ic2.api.energy.tile.IEnergySink",modid="IC2"), @Optional.Interface(iface="vazkii.botania.api.mana.IManaReceiver",modid="Botania")})
public class QuestDevice extends TileEntity implements IFluidHandler,cofh.api.energy.IEnergyReceiver,ic2.api.energy.tile.IEnergySink,vazkii.botania.api.mana.IManaReceiver,net.minecraft.inventory.ISidedInventory {
    public UUID owner;
    public String quest="",task="",bindingProfile="default";
    public boolean signal,analog;
    public int redstoneOutput;
    public int facing=2,screenPercent,screenWidth=1,screenHeight=1,coreX,coreY,coreZ;
    public boolean screenLinked;
    public String screenTitle="Quest screen";
    private int ticks;
    private boolean euRegistered;
    private int euTier=-1;
    private double euFraction;
    private String euBinding="";
    private net.minecraft.item.ItemStack input;
    public EntityPlayerMP player() {
        if(!QuestScreens.synchronize(this)) return null;
        if(worldObj==null || worldObj.isRemote || owner==null || QuestsMod.service==null || !bindingProfile.equals(QuestsMod.service.progress().profile())) return null;
        return MinecraftServer.getServer()==null?null:onlineOwner(MinecraftServer.getServer().getConfigurationManager().playerEntityList);
    }
    public EntityPlayerMP onlineOwner(Iterable<?> online) {
        for(Object raw:online) if(((EntityPlayerMP)raw).getUniqueID().equals(owner)) return (EntityPlayerMP)raw;
        int mode=getBlockType() instanceof QuestBlocks.DeviceBlock?((QuestBlocks.DeviceBlock)getBlockType()).mode:0;
        if(mode==3 || mode==6 || owner==null || QuestsMod.service==null) return null;
        String group=QuestsMod.service.progress().group(owner);
        for(Object raw:online) if(group.equals(QuestsMod.service.progress().group(((EntityPlayerMP)raw).getUniqueID()))) return (EntityPlayerMP)raw;
        return null;
    }
    public QuestCore.Task definition(String type) {
        if(!QuestScreens.synchronize(this)) return null;
        if(QuestsMod.service==null || !bindingProfile.equals(QuestsMod.service.progress().profile())) return null;
        QuestCore.Quest q=QuestsMod.service.quests.get(quest);
        if(q!=null) for(QuestCore.Task t:q.tasks) if(t.id.equals(task) && t.type.equals(type)) return t;
        return null;
    }
    private long remaining(String type) {
        QuestCore.Task t=definition(type); QuestService s=QuestsMod.service;
        if(worldObj==null || worldObj.isRemote || owner==null || t==null || !s.active(owner,s.quests.get(quest))) return 0;
        if(t.sticky && s.progress().taskDone(owner,quest,task)) return 0;
        return Math.max(0,t.count-s.progress().value(owner,quest,task));
    }
    private void credit(long amount) {
        QuestService service=QuestsMod.service; EntityPlayerMP actor=player();
        if(actor!=null) service.creditTask(actor,quest,task,amount);
        else if(owner!=null) { service.creditTask(owner,quest,task,amount); if(bindingProfile.equals(service.progress().profile())) service.finishStored(owner,service.quests.get(quest),worldObj.getTotalWorldTime()); }
    }
    public int percent() {
        QuestService service=QuestsMod.service; if(service==null || owner==null || !bindingProfile.equals(service.progress().profile())) return 0;
        return (int)Math.round(100*service.objectProgress(owner,quest));
    }
    @Override public void updateEntity() {
        if(worldObj.isRemote) return;
        if(!QuestScreens.synchronize(this)) return;
        if(ticks%20==0) QuestScreens.validateAssembly(this);
        if(cpw.mods.fml.common.Loader.isModLoaded("IC2")) {
            if(euRegistered && euTier!=getSinkTier()) unregisterEU();
            if(!euRegistered && taskTerminal()) registerEU();
        }
        if(QuestsMod.service==null || ++ticks%20!=0) return;
        QuestService s=QuestsMod.service; EntityPlayerMP player=player();
        QuestCore.Quest q=bindingProfile.equals(s.progress().profile())?s.quests.get(quest):null; boolean next=false;
        int mode=((QuestBlocks.DeviceBlock)getBlockType()).mode;
        String title=bindingProfile.equals(s.progress().profile())?s.objectTitle(quest):"Quest screen"; int percent=percent();
        if(!screenTitle.equals(title) || screenPercent!=percent) { screenTitle=title; screenPercent=percent; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
        if(owner!=null && bindingProfile.equals(s.progress().profile())) next=s.objectComplete(owner,quest);
        if(mode==9) {
            int output=analog?(int)Math.round(percent*15/100D):next?15:0;
            if(redstoneOutput!=output) { redstoneOutput=output; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType()); }
        }
        if(mode==5) next=!worldObj.getEntitiesWithinAABB(net.minecraft.entity.player.EntityPlayer.class,AxisAlignedBB.getBoundingBox(xCoord-8,yCoord-8,zCoord-8,xCoord+9,yCoord+9,zCoord+9)).isEmpty();
        if(mode==4) next=worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord);
        try {
            if(player!=null && mode==6 && input!=null) {
                if(QuestBlocks.LootCrateItem.open(player,input)) { if(input.stackSize<=0) input=null; markDirty(); }
            }
            if(mode==0 && q!=null && owner!=null) {
                if(input!=null) { s.acceptItems(owner,quest,task,input,false); if(input.stackSize<=0) input=null; markDirty(); }
                if(player!=null) { if(s.active(player,q)) s.finish(player,q); } else s.finishStored(owner,q,worldObj.getTotalWorldTime());
            }
            if(player!=null && q!=null) {
                if(mode==3 && s.progress().completed(owner).contains(quest) && !s.claimed(owner,q)) s.claim(player,q,java.util.Collections.<Integer>emptyList(),this::output);
                if((mode==4 || mode==5) && next && definition("custom")!=null) s.creditTask(player,quest,task,1);
            }
        } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest device at %s,%s,%s: %s",xCoord,yCoord,zCoord,ex.toString()); }
        if(signal!=next) { signal=next; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType()); }
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        try { owner=UUID.fromString(nbt.getString("owner")); } catch(IllegalArgumentException e) { owner=null; }
        euFraction=nbt.getDouble("euFraction"); if(!Double.isFinite(euFraction) || euFraction<0 || euFraction>=1) euFraction=0; euBinding=nbt.getString("euBinding");
        input=nbt.hasKey("input")?net.minecraft.item.ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("input")):null;
        analog=nbt.getBoolean("analog"); redstoneOutput=Math.max(0,Math.min(15,nbt.getInteger("redstoneOutput")));
        screenWidth=Math.max(1,Math.min(8,nbt.getInteger("screenWidth"))); screenHeight=Math.max(1,Math.min(8,nbt.getInteger("screenHeight")));
        screenLinked=nbt.getBoolean("screenLinked"); coreX=nbt.getInteger("coreX"); coreY=nbt.getInteger("coreY"); coreZ=nbt.getInteger("coreZ");
        facing=nbt.hasKey("facing")?nbt.getInteger("facing"):2; if(facing<2 || facing>5) facing=2;
        screenPercent=Math.max(0,Math.min(100,nbt.getInteger("screenPercent"))); screenTitle=nbt.hasKey("screenTitle")?nbt.getString("screenTitle"):"Quest screen";
        bindingProfile=nbt.hasKey("bindingProfile")?nbt.getString("bindingProfile"):"default";
        quest=nbt.getString("quest"); task=nbt.getString("task"); signal=nbt.getBoolean("signal");
    }
    @Override public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt); nbt.setBoolean("analog",analog); nbt.setInteger("redstoneOutput",redstoneOutput); nbt.setInteger("screenWidth",screenWidth); nbt.setInteger("screenHeight",screenHeight); nbt.setBoolean("screenLinked",screenLinked); nbt.setInteger("coreX",coreX); nbt.setInteger("coreY",coreY); nbt.setInteger("coreZ",coreZ); nbt.setInteger("facing",facing); nbt.setInteger("screenPercent",screenPercent); nbt.setString("screenTitle",screenTitle); nbt.setString("bindingProfile",bindingProfile); nbt.setDouble("euFraction",euFraction); nbt.setString("euBinding",euBinding); if(input!=null) { NBTTagCompound stack=new NBTTagCompound(); input.writeToNBT(stack); nbt.setTag("input",stack); } if(owner!=null) nbt.setString("owner",owner.toString()); nbt.setString("quest",quest); nbt.setString("task",task); nbt.setBoolean("signal",signal);
    }
    @Override public AxisAlignedBB getRenderBoundingBox() { return AxisAlignedBB.getBoundingBox(xCoord-8,yCoord,zCoord-8,xCoord+9,yCoord+9,zCoord+9); }
    @Override public Packet getDescriptionPacket() { NBTTagCompound nbt=new NBTTagCompound(); writeToNBT(nbt); return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,nbt); }
    @Override public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g()); }
    @Optional.Method(modid="IC2") private void registerEU() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileLoadEvent(this)); euRegistered=true; euTier=getSinkTier();
    }
    @Optional.Method(modid="IC2") private void unregisterEU() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileUnloadEvent(this)); euRegistered=false;
    }
    @Override public void invalidate() { if(euRegistered && cpw.mods.fml.common.Loader.isModLoaded("IC2")) unregisterEU(); super.invalidate(); }
    @Override public void onChunkUnload() { if(euRegistered && cpw.mods.fml.common.Loader.isModLoaded("IC2")) unregisterEU(); super.onChunkUnload(); }
    public void bindingChanged() {
        if(QuestsMod.service!=null) bindingProfile=QuestsMod.service.progress().profile();
        if(euRegistered && cpw.mods.fml.common.Loader.isModLoaded("IC2")) unregisterEU();
        markDirty();
        if(worldObj!=null && !worldObj.isRemote) { worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType()); }
    }
    private boolean taskTerminal() { return worldObj!=null && !worldObj.isRemote && getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==0; }
    private String euKey() { return bindingProfile+":"+owner+":"+quest+":"+task; }
    @Override public boolean acceptsEnergyFrom(TileEntity emitter,ForgeDirection side) { return taskTerminal(); }
    @Override public double getDemandedEnergy() {
        if(screenLinked) { QuestDevice core=QuestScreens.core(this); return core==null?0:core.getDemandedEnergy(); }
        if(!taskTerminal()) return 0;
        return Math.max(0,remaining("ic2_energy")-(euBinding.equals(euKey())?euFraction:0));
    }
    @Override public int getSinkTier() { QuestCore.Task t=definition("ic2_energy"); return t==null?4:t.energyTier; }
    @Override public double injectEnergy(ForgeDirection side,double amount,double voltage) {
        if(screenLinked) { QuestDevice core=QuestScreens.core(this); return core==null?amount:core.injectEnergy(side,amount,voltage); }
        if(!Double.isFinite(amount) || amount<=0 || amount>9007199254740992D || !Double.isFinite(voltage) || voltage<0 || !taskTerminal()) return amount;
        double accepted=Math.min(amount,getDemandedEnergy()); if(accepted<=0) return amount;
        if(!euBinding.equals(euKey())) { euFraction=0; euBinding=euKey(); }
        double total=euFraction+accepted; long whole=(long)Math.floor(total);
        if(whole>0) credit(whole);
        euFraction=total-whole; markDirty(); return amount-accepted;
    }
    // Mana is consumed by the task, rather than kept as extractable storage.
    @Override public int getCurrentMana() { return 0; }
    @Override public boolean isFull() { return !taskTerminal() || remaining("botania_mana")==0; }
    @Override public boolean canRecieveManaFromBursts() { return !isFull(); }
    @Override public void recieveMana(int mana) {
        if(mana<=0 || isFull()) return;
        long accepted=Math.min(mana,remaining("botania_mana"));
        if(accepted>0) credit(accepted);
    }
    @Override public int fill(ForgeDirection from,FluidStack resource,boolean commit) {
        if(worldObj==null || worldObj.isRemote || resource==null || resource.amount<=0) return 0;
        QuestCore.Task t=definition("fluid"); if(t==null || !QuestFluids.matches(t,resource)) return 0;
        int accepted=(int)Math.min(resource.amount,remaining("fluid"));
        if(commit && accepted>0) credit(accepted);
        return accepted;
    }
    @Override public FluidStack drain(ForgeDirection from,FluidStack resource,boolean commit) { return null; }
    @Override public FluidStack drain(ForgeDirection from,int amount,boolean commit) { return null; }
    @Override public boolean canFill(ForgeDirection from,Fluid fluid) { QuestCore.Task t=definition("fluid"); return t!=null && fluid!=null && fluid.getName().equals(t.target) && remaining("fluid")>0; }
    @Override public boolean canDrain(ForgeDirection from,Fluid fluid) { return false; }
    @Override public FluidTankInfo[] getTankInfo(ForgeDirection from) { return new FluidTankInfo[]{new FluidTankInfo(null,(int)Math.min(Integer.MAX_VALUE,remaining("fluid")))}; }
    @Override public boolean canConnectEnergy(ForgeDirection from) { return definition("energy")!=null || definition("buildcraft_mj")!=null; }
    @Override public int receiveEnergy(ForgeDirection from,int amount,boolean simulate) {
        if(worldObj==null || worldObj.isRemote || amount<=0) return 0;
        boolean mj=definition("buildcraft_mj")!=null; long remaining=remaining(mj?"buildcraft_mj":"energy");
        long units=mj?remaining/100000+(remaining%100000==0?0:1):remaining;
        int accepted=(int)Math.min(amount,units);
        if(!simulate && accepted>0) credit(mj?Math.min(remaining,accepted*100000L):accepted);
        return accepted;
    }
    @Override public int getEnergyStored(ForgeDirection from) { return 0; }
    @Override public int getMaxEnergyStored(ForgeDirection from) { long remaining=remaining(definition("buildcraft_mj")!=null?"buildcraft_mj":"energy"); if(definition("buildcraft_mj")!=null) remaining=remaining/100000+(remaining%100000==0?0:1); return (int)Math.min(Integer.MAX_VALUE,remaining); }
    @Override public int getSizeInventory() { return 1; }
    @Override public net.minecraft.item.ItemStack getStackInSlot(int slot) { return slot==0?input:null; }
    @Override public net.minecraft.item.ItemStack decrStackSize(int slot,int amount) {
        if(slot!=0 || input==null || amount<=0) return null;
        net.minecraft.item.ItemStack taken=input.splitStack(Math.min(amount,input.stackSize)); if(input.stackSize<=0) input=null; markDirty(); return taken;
    }
    @Override public net.minecraft.item.ItemStack getStackInSlotOnClosing(int slot) { if(slot!=0) return null; net.minecraft.item.ItemStack stack=input; input=null; markDirty(); return stack; }
    @Override public void setInventorySlotContents(int slot,net.minecraft.item.ItemStack stack) { if(slot!=0) return; input=stack; if(input!=null) input.stackSize=Math.min(input.stackSize,getInventoryStackLimit()); markDirty(); }
    @Override public String getInventoryName() { return "Quest terminal"; }
    @Override public boolean hasCustomInventoryName() { return true; }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
        if(worldObj==null || worldObj.getTileEntity(xCoord,yCoord,zCoord)!=this || player.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)>64) return false;
        if(worldObj.isRemote) return true; // The server already authorizes opening and every configuration action.
        return owner!=null && (owner.equals(player.getUniqueID()) || QuestsMod.service!=null && QuestsMod.service.progress().group(owner).equals(QuestsMod.service.progress().group(player.getUniqueID()))) || player.canCommandSenderUseCommand(2,"quests.edit");
    }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public boolean isItemValidForSlot(int slot,net.minecraft.item.ItemStack stack) {
        if(slot!=0 || stack==null) return false;
        if(getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==6) return stack.getItem() instanceof QuestBlocks.LootCrateItem;
        if(!(getBlockType() instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)getBlockType()).mode!=0) return false;
        return worldObj!=null && !worldObj.isRemote && owner!=null && definition("item")!=null && QuestsMod.service.acceptItems(owner,quest,task,stack,true)>0;
    }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return new int[]{0}; }
    @Override public boolean canInsertItem(int slot,net.minecraft.item.ItemStack stack,int side) { return isItemValidForSlot(slot,stack); }
    @Override public boolean canExtractItem(int slot,net.minecraft.item.ItemStack stack,int side) { return false; }
    private void output(net.minecraft.item.ItemStack stack) {
        for(ForgeDirection side:ForgeDirection.VALID_DIRECTIONS) {
            TileEntity neighbor=worldObj.getTileEntity(xCoord+side.offsetX,yCoord+side.offsetY,zCoord+side.offsetZ);
            if(neighbor instanceof net.minecraft.inventory.IInventory) {
                stack=net.minecraft.tileentity.TileEntityHopper.func_145889_a((net.minecraft.inventory.IInventory)neighbor,stack,side.getOpposite().ordinal());
                if(stack==null || stack.stackSize<=0) return;
            }
        }
        worldObj.spawnEntityInWorld(new net.minecraft.entity.item.EntityItem(worldObj,xCoord+.5,yCoord+1,zCoord+.5,stack));
    }
}