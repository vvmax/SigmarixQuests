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
@Optional.InterfaceList({@Optional.Interface(iface="cofh.api.energy.IEnergyReceiver",modid="CoFHAPI|energy"), @Optional.Interface(iface="ic2.api.energy.tile.IEnergySink",modid="IC2")})
public class QuestDevice extends TileEntity implements IFluidHandler,cofh.api.energy.IEnergyReceiver,ic2.api.energy.tile.IEnergySink,net.minecraft.inventory.ISidedInventory {
    public UUID owner;
    public String quest="",task="";
    public boolean signal;
    private int ticks;
    private boolean euRegistered;
    private double euFraction;
    private String euBinding="";
    private net.minecraft.item.ItemStack input;
    public EntityPlayerMP player() {
        if(worldObj==null || worldObj.isRemote || owner==null || QuestsMod.service==null) return null;
        for(Object p:MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            if(((EntityPlayerMP)p).getUniqueID().equals(owner)) return (EntityPlayerMP)p;
        return null;
    }
    public QuestCore.Task definition(String type) {
        if(QuestsMod.service==null) return null;
        QuestCore.Quest q=QuestsMod.service.quests.get(quest);
        if(q!=null) for(QuestCore.Task t:q.tasks) if(t.id.equals(task) && t.type.equals(type)) return t;
        return null;
    }
    private long remaining(String type) {
        EntityPlayerMP p=player(); QuestCore.Task t=definition(type); QuestService s=QuestsMod.service;
        if(p==null || t==null || !s.active(p,s.quests.get(quest))) return 0;
        return Math.max(0,t.count-s.progress().value(owner,quest,task));
    }
    @Override public void updateEntity() {
        if(worldObj.isRemote) return;
        if(!euRegistered && euTerminal() && cpw.mods.fml.common.Loader.isModLoaded("IC2")) registerEU();
        if(QuestsMod.service==null || ++ticks%20!=0) return;
        QuestService s=QuestsMod.service; EntityPlayerMP player=player();
        QuestCore.Quest q=s.quests.get(quest); boolean next=false;
        int mode=((QuestBlocks.DeviceBlock)getBlockType()).mode;
        if(owner!=null && q!=null) next=s.progress().completed(owner).contains(quest);
        if(mode==5) next=!worldObj.getEntitiesWithinAABB(net.minecraft.entity.player.EntityPlayer.class,AxisAlignedBB.getBoundingBox(xCoord-8,yCoord-8,zCoord-8,xCoord+9,yCoord+9,zCoord+9)).isEmpty();
        if(mode==4) next=worldObj.isBlockIndirectlyGettingPowered(xCoord,yCoord,zCoord);
        try {
            if(player!=null && mode==6 && input!=null) {
                if(QuestBlocks.LootCrateItem.open(player,input)) { if(input.stackSize<=0) input=null; markDirty(); }
            }
            if(player!=null && q!=null) {
                if(mode==0 && input!=null) { s.acceptItems(player,quest,task,input,false); if(input.stackSize<=0) input=null; markDirty(); }
                if(mode==0 && s.active(player,q)) s.finish(player,q);
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
        quest=nbt.getString("quest"); task=nbt.getString("task"); signal=nbt.getBoolean("signal");
    }
    @Override public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt); nbt.setDouble("euFraction",euFraction); nbt.setString("euBinding",euBinding); if(input!=null) { NBTTagCompound stack=new NBTTagCompound(); input.writeToNBT(stack); nbt.setTag("input",stack); } if(owner!=null) nbt.setString("owner",owner.toString()); nbt.setString("quest",quest); nbt.setString("task",task); nbt.setBoolean("signal",signal);
    }
    @Override public Packet getDescriptionPacket() { NBTTagCompound nbt=new NBTTagCompound(); writeToNBT(nbt); return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,nbt); }
    @Override public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g()); }
    @Optional.Method(modid="IC2") private void registerEU() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileLoadEvent(this)); euRegistered=true;
    }
    @Optional.Method(modid="IC2") private void unregisterEU() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileUnloadEvent(this)); euRegistered=false;
    }
    @Override public void invalidate() { if(euRegistered && cpw.mods.fml.common.Loader.isModLoaded("IC2")) unregisterEU(); super.invalidate(); }
    @Override public void onChunkUnload() { if(euRegistered && cpw.mods.fml.common.Loader.isModLoaded("IC2")) unregisterEU(); super.onChunkUnload(); }
    private boolean euTerminal() { return worldObj!=null && !worldObj.isRemote && getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==0; }
    private String euKey() { return owner+":"+quest+":"+task; }
    @Override public boolean acceptsEnergyFrom(TileEntity emitter,ForgeDirection side) { return getDemandedEnergy()>0; }
    @Override public double getDemandedEnergy() {
        if(!euTerminal()) return 0;
        return Math.max(0,remaining("ic2_energy")-(euBinding.equals(euKey())?euFraction:0));
    }
    @Override public int getSinkTier() { QuestCore.Task t=definition("ic2_energy"); return t==null?4:t.energyTier; }
    @Override public double injectEnergy(ForgeDirection side,double amount,double voltage) {
        if(!Double.isFinite(amount) || amount<=0 || amount>9007199254740992D || !Double.isFinite(voltage) || voltage<0 || !euTerminal()) return amount;
        double accepted=Math.min(amount,getDemandedEnergy()); if(accepted<=0) return amount;
        if(!euBinding.equals(euKey())) { euFraction=0; euBinding=euKey(); }
        double total=euFraction+accepted; long whole=(long)Math.floor(total);
        if(whole>0) QuestsMod.service.creditTask(player(),quest,task,whole);
        euFraction=total-whole; markDirty(); return amount-accepted;
    }
    @Override public int fill(ForgeDirection from,FluidStack resource,boolean commit) {
        if(worldObj==null || worldObj.isRemote || resource==null || resource.amount<=0) return 0;
        QuestCore.Task t=definition("fluid"); if(t==null || !QuestFluids.matches(t,resource)) return 0;
        int accepted=(int)Math.min(resource.amount,remaining("fluid"));
        if(commit && accepted>0) QuestsMod.service.creditTask(player(),quest,task,accepted);
        return accepted;
    }
    @Override public FluidStack drain(ForgeDirection from,FluidStack resource,boolean commit) { return null; }
    @Override public FluidStack drain(ForgeDirection from,int amount,boolean commit) { return null; }
    @Override public boolean canFill(ForgeDirection from,Fluid fluid) { QuestCore.Task t=definition("fluid"); return t!=null && fluid!=null && fluid.getName().equals(t.target) && remaining("fluid")>0; }
    @Override public boolean canDrain(ForgeDirection from,Fluid fluid) { return false; }
    @Override public FluidTankInfo[] getTankInfo(ForgeDirection from) { return new FluidTankInfo[]{new FluidTankInfo(null,(int)Math.min(Integer.MAX_VALUE,remaining("fluid")))}; }
    @Override public boolean canConnectEnergy(ForgeDirection from) { return definition("energy")!=null; }
    @Override public int receiveEnergy(ForgeDirection from,int amount,boolean simulate) {
        if(worldObj==null || worldObj.isRemote || amount<=0) return 0;
        int accepted=(int)Math.min(amount,remaining("energy"));
        if(!simulate && accepted>0) QuestsMod.service.creditTask(player(),quest,task,accepted);
        return accepted;
    }
    @Override public int getEnergyStored(ForgeDirection from) { return 0; }
    @Override public int getMaxEnergyStored(ForgeDirection from) { return (int)Math.min(Integer.MAX_VALUE,remaining("energy")); }
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
    @Override public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer p) { return worldObj!=null && worldObj.getTileEntity(xCoord,yCoord,zCoord)==this && (owner!=null && owner.equals(p.getUniqueID()) || p.canCommandSenderUseCommand(2,"quests.edit")) && p.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64; }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public boolean isItemValidForSlot(int slot,net.minecraft.item.ItemStack stack) {
        if(slot!=0 || stack==null) return false;
        if(getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==6) return stack.getItem() instanceof QuestBlocks.LootCrateItem;
        if(!(getBlockType() instanceof QuestBlocks.DeviceBlock) || ((QuestBlocks.DeviceBlock)getBlockType()).mode!=0) return false;
        EntityPlayerMP p=player(); return p!=null && QuestsMod.service.acceptItems(p,quest,task,stack,true)>0;
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