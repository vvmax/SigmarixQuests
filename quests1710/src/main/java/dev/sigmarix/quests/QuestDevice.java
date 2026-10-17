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
    private int ticks;
    private boolean euRegistered;
    private int euTier=-1;
    private double euFraction;
    private String euBinding="";
    private net.minecraft.item.ItemStack input;
    private final net.minecraft.item.ItemStack[] collectorItems=new net.minecraft.item.ItemStack[9];
    private final java.util.List<net.minecraft.item.ItemStack> pendingLoot=new java.util.ArrayList<net.minecraft.item.ItemStack>();
    public int queuedLoot;
    public long storedOutputCount(){long count=0;for(net.minecraft.item.ItemStack stack:collectorItems)if(stack!=null)count+=stack.stackSize;for(net.minecraft.item.ItemStack stack:pendingLoot)count+=stack.stackSize;return count;}
    public boolean opener() { return worldObj!=null && getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==6; }
    private int outputIndex(int slot) { return collector()?(slot>=0 && slot<9?slot:-1):opener() && slot>=1 && slot<=9?slot-1:-1; }
    public boolean collector() { return worldObj!=null && getBlockType() instanceof QuestBlocks.DeviceBlock && ((QuestBlocks.DeviceBlock)getBlockType()).mode==3; }
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
        if(!taskTerminal()) return 0;
        QuestCore.Task t=definition(type); QuestService s=QuestsMod.service;
        if(worldObj==null || worldObj.isRemote || owner==null || t==null || !s.active(owner,s.quests.get(quest))) return 0;
        if(t.sticky && s.progress().taskDone(owner,quest,task)) return 0;
        return Math.max(0,t.count-s.progress().value(owner,quest,task));
    }
    private long inputRemaining(String type){long available=remaining(type);QuestCore.Task definition=definition(type);return definition!=null && definition.maxInput>0?Math.min(available,definition.maxInput):available;}
    private void credit(long amount) {
        QuestService service=QuestsMod.service; EntityPlayerMP actor=player();
        if(actor!=null) service.creditTask(actor,quest,task,amount);
        else if(owner!=null) { service.creditTask(owner,quest,task,amount); if(bindingProfile.equals(service.progress().profile())) service.finishStored(owner,service.quests.get(quest),worldObj.getTotalWorldTime()); }
    }
    public int percent() {
        QuestService service=QuestsMod.service; if(service==null || owner==null || !bindingProfile.equals(service.progress().profile())) return 0;
        return service.objectPercent(owner,quest);
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
        if(mode==0 && q!=null && owner!=null){QuestCore.Task selected=null;for(QuestCore.Task candidate:q.tasks)if(candidate.id.equals(task)){selected=candidate;break;}if(selected!=null){long value=s.progress().value(owner,q.id,selected.id);if(s.progress().taskDone(owner,q.id,selected.id) || s.progress().completed(owner).contains(q.id))value=selected.count;if(screenTaskProgress!=value){screenTaskProgress=value;markDirty();worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);}}}
        if(!screenTitle.equals(title) || screenPercent!=percent) { screenTitle=title; screenPercent=percent; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
        if(owner!=null && bindingProfile.equals(s.progress().profile())) next=s.objectComplete(owner,quest);
        if(mode==9) {
            int output=analog?(int)Math.round(percent*15/100D):next?15:0;
            if(redstoneOutput!=output) { redstoneOutput=output; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType()); }
        }
        if(mode==5) next=detectPlayers(worldObj.getEntitiesWithinAABB(EntityPlayerMP.class,detectorBounds()));
        if(mode==4) next=worldObj.getStrongestIndirectPower(xCoord,yCoord,zCoord)>=detectorPower;
        try {
            if(mode==6 && owner!=null && bindingProfile.equals(s.progress().profile())) {
                flushLoot();
                if(pendingLoot.isEmpty() && input!=null && input.getItem() instanceof QuestBlocks.LootCrateItem) {
                    try { if(!input.hasTagCompound()) throw new IllegalArgumentException("Loot crate needs a table"); s.openLootAutomated(owner,player,QuestLoot.table(input.getTagCompound()),input,worldObj.rand,this::queueLoot); }
                    finally { if(input!=null && input.stackSize<=0) input=null; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
                }
                flushLoot();
            }
            if(mode==0 && q!=null && owner!=null) {
                if(input!=null) { s.acceptItems(owner,quest,task,input,false); if(input.stackSize<=0) input=null; markDirty(); }
                if(player!=null) { if(s.active(player,q)) s.finish(player,q); } else s.finishStored(owner,q,worldObj.getTotalWorldTime());
            }
            if(mode==4)checkRedstone();
            if(mode==3 && q!=null && owner!=null && s.progress().completed(owner).contains(quest) && !s.claimed(owner,q)) {
                java.util.function.Predicate<java.util.List<net.minecraft.item.ItemStack>> capacity=stacks->QuestRewardBuffer.fits(collectorItems,stacks);
                java.util.function.Consumer<net.minecraft.item.ItemStack> sink=stack->{ if(!QuestRewardBuffer.add(collectorItems,stack)) throw new IllegalStateException("Collector capacity changed during reward transaction"); markDirty(); };
                if(player!=null) s.collect(player,q,task,capacity,sink); else s.collectStored(owner,q,task,worldObj.rand,capacity,sink);
            }
            if(mode==3 && q!=null && owner!=null) s.repeatAutomated(owner,q,worldObj.getTotalWorldTime());
        } catch(RuntimeException ex) { if(ticks%1200==0) cpw.mods.fml.common.FMLLog.warning("Quest device at %s,%s,%s: %s",xCoord,yCoord,zCoord,ex.toString()); }
        if(signal!=next) { signal=next; markDirty(); worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType()); }
    }
    private void queueLoot(net.minecraft.item.ItemStack stack) {
        if(pendingLoot.size()>=QuestService.MAX_AUTOMATED_STACKS) throw new IllegalArgumentException("Pending loot exceeds limit");
        pendingLoot.add(stack.copy()); queuedLoot+=stack.stackSize; markDirty();
    }
    private void flushLoot() {
        int previous=queuedLoot;
        while(!pendingLoot.isEmpty()) {
            net.minecraft.item.ItemStack first=pendingLoot.get(0); int accepted=QuestRewardBuffer.offer(collectorItems,first); first.stackSize-=accepted; queuedLoot-=accepted;
            if(first.stackSize>0) break; pendingLoot.remove(0);
        }
        if(previous!=queuedLoot) { markDirty(); if(worldObj!=null) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
    }
    public java.util.List<net.minecraft.item.ItemStack> drainDeviceItems() {
        java.util.List<net.minecraft.item.ItemStack> result=new java.util.ArrayList<net.minecraft.item.ItemStack>();
        if(input!=null && input.stackSize>0) result.add(input); input=null;
        for(int i=0;i<collectorItems.length;i++) { if(collectorItems[i]!=null && collectorItems[i].stackSize>0) result.add(collectorItems[i]); collectorItems[i]=null; }
        result.addAll(drainPendingLoot()); markDirty(); return result;
    }
    public java.util.List<net.minecraft.item.ItemStack> drainPendingLoot() {
        java.util.List<net.minecraft.item.ItemStack> result=new java.util.ArrayList<net.minecraft.item.ItemStack>(pendingLoot); pendingLoot.clear(); queuedLoot=0; markDirty(); return result;
    }
    public AxisAlignedBB detectorBounds() {
        return AxisAlignedBB.getBoundingBox(xCoord+.5-detectorRadius,yCoord+detectorOffsetY,zCoord+.5-detectorRadius,xCoord+.5+detectorRadius,yCoord+detectorOffsetY+detectorHeight,zCoord+.5+detectorRadius);
    }
    public void configureDetector(double radius,double offset,double height,int power) {
        if(!Double.isFinite(radius) || radius<.25 || radius>100 || !Double.isFinite(offset) || offset< -1000 || offset>1000 || !Double.isFinite(height) || height<.1 || height>1000 || power<1 || power>15) throw new IllegalArgumentException("Invalid detector settings");
        detectorRadius=radius; detectorOffsetY=offset; detectorHeight=height; detectorPower=power;
        markDirty(); if(worldObj!=null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
    }
    /** Like the native detector, a steady powered signal does not retrigger a reset task. */
    public void checkRedstone(){if(worldObj==null || worldObj.isRemote || QuestsMod.service==null)return;int power=worldObj.getStrongestIndirectPower(xCoord,yCoord,zCoord);int previous=lastRedstoneInput;lastRedstoneInput=power;if(power!=previous)markDirty();if(power!=previous && power>=detectorPower && owner!=null && definition("custom")!=null)creditDetected(owner,player());}
    private void creditDetected(UUID recipient,EntityPlayerMP online) {
        QuestService service=QuestsMod.service; QuestCore.Task target=definition("custom"); if(target==null) return;
        QuestCore.Quest q=service.quests.get(quest);
        QuestEvents.withNotifications(detectorNotifications,() -> {
            boolean completed=service.progress().completed(recipient).contains(quest);
            service.creditTask(recipient,quest,task,target.count);
            service.finishStored(recipient,q,worldObj.getTotalWorldTime());
        });
    }
    /** A public detector credits the detected player's team, never its builder's team. */
    public boolean detectPlayers(Iterable<?> players) {
        QuestService service=QuestsMod.service; QuestCore.Task target=definition("custom"); boolean present=false;
        for(Object raw:players) if(raw instanceof EntityPlayerMP) {
            present=true; EntityPlayerMP detected=(EntityPlayerMP)raw;
            if(target!=null) {
                creditDetected(detected.getUniqueID(),detected);
            }
        }
        return present;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);lastRedstoneInput=Math.max(0,Math.min(15,nbt.getInteger("lastRedstoneInput")));
        try { owner=UUID.fromString(nbt.getString("owner")); } catch(IllegalArgumentException e) { owner=null; }
        euFraction=nbt.getDouble("euFraction"); if(!Double.isFinite(euFraction) || euFraction<0 || euFraction>=1) euFraction=0; euBinding=nbt.getString("euBinding");
        pendingLoot.clear(); queuedLoot=Math.max(0,Math.min(QuestService.MAX_AUTOMATED_STACKS*64,nbt.getInteger("queuedLoot")));
        if(nbt.hasKey("pendingLoot")) { queuedLoot=0; net.minecraft.nbt.NBTTagList queued=nbt.getTagList("pendingLoot",10); for(int i=0;i<Math.min(queued.tagCount(),QuestService.MAX_AUTOMATED_STACKS);i++) { net.minecraft.item.ItemStack stack=net.minecraft.item.ItemStack.loadItemStackFromNBT(queued.getCompoundTagAt(i)); if(stack!=null && stack.stackSize>0) { stack.stackSize=Math.min(64,stack.stackSize); pendingLoot.add(stack); queuedLoot+=stack.stackSize; } } }
        java.util.Arrays.fill(collectorItems,null); net.minecraft.nbt.NBTTagList storedRewards=nbt.getTagList("collectorItems",10);
        for(int i=0;i<storedRewards.tagCount();i++) { NBTTagCompound entry=storedRewards.getCompoundTagAt(i); int slot=entry.getByte("Slot"); if(slot>=0 && slot<9) collectorItems[slot]=net.minecraft.item.ItemStack.loadItemStackFromNBT(entry); }
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
    @Override public void writeToNBT(NBTTagCompound nbt) {
        net.minecraft.nbt.NBTTagList queued=new net.minecraft.nbt.NBTTagList(); for(net.minecraft.item.ItemStack stack:pendingLoot) { NBTTagCompound entry=new NBTTagCompound(); stack.writeToNBT(entry); queued.appendTag(entry); } nbt.setTag("pendingLoot",queued); nbt.setInteger("queuedLoot",queuedLoot);
        net.minecraft.nbt.NBTTagList storedRewards=new net.minecraft.nbt.NBTTagList();
        for(int i=0;i<9;i++) if(collectorItems[i]!=null) { NBTTagCompound entry=new NBTTagCompound(); collectorItems[i].writeToNBT(entry); entry.setByte("Slot",(byte)i); storedRewards.appendTag(entry); } nbt.setTag("collectorItems",storedRewards);
        nbt.setBoolean("detectorNotifications",detectorNotifications); nbt.setDouble("detectorRadius",detectorRadius); nbt.setDouble("detectorOffsetY",detectorOffsetY); nbt.setDouble("detectorHeight",detectorHeight); nbt.setInteger("detectorPower",detectorPower);
        nbt.setBoolean("indestructible",indestructible); nbt.setString("screenSkin",screenSkin); nbt.setInteger("screenSkinMeta",screenSkinMeta);
        nbt.setBoolean("screenFullscreen",screenFullscreen); nbt.setBoolean("screenHideIcons",screenHideIcons); nbt.setBoolean("screenInputOnly",screenInputOnly); if(screenInputIcon!=null) { NBTTagCompound icon=new NBTTagCompound(); screenInputIcon.writeToNBT(icon); nbt.setTag("screenInputIcon",icon); }
        super.writeToNBT(nbt);nbt.setInteger("lastRedstoneInput",lastRedstoneInput); nbt.setBoolean("analog",analog); nbt.setInteger("redstoneOutput",redstoneOutput); nbt.setInteger("screenWidth",screenWidth); nbt.setInteger("screenHeight",screenHeight); nbt.setBoolean("screenLinked",screenLinked); nbt.setInteger("coreX",coreX); nbt.setInteger("coreY",coreY); nbt.setInteger("coreZ",coreZ); nbt.setInteger("facing",facing); nbt.setInteger("screenPercent",screenPercent);nbt.setLong("screenTaskProgress",screenTaskProgress); nbt.setString("screenTitle",screenTitle); nbt.setString("bindingProfile",bindingProfile); nbt.setDouble("euFraction",euFraction); nbt.setString("euBinding",euBinding); if(input!=null) { NBTTagCompound stack=new NBTTagCompound(); input.writeToNBT(stack); nbt.setTag("input",stack); } if(owner!=null) nbt.setString("owner",owner.toString()); nbt.setString("quest",quest); nbt.setString("task",task); nbt.setBoolean("signal",signal);
    }
    @Override public AxisAlignedBB getRenderBoundingBox() { return AxisAlignedBB.getBoundingBox(xCoord-8,yCoord,zCoord-8,xCoord+9,yCoord+9,zCoord+9); }
    @Override public Packet getDescriptionPacket() { NBTTagCompound nbt=new NBTTagCompound(); writeToNBT(nbt); nbt.removeTag("collectorItems"); nbt.removeTag("input"); nbt.removeTag("pendingLoot"); return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,nbt); }
    @Override public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet) { String oldSkin=screenSkin; int oldMeta=screenSkinMeta; net.minecraft.item.ItemStack held=input; net.minecraft.item.ItemStack[] rewards=collectorItems.clone(); java.util.List<net.minecraft.item.ItemStack> queued=new java.util.ArrayList<net.minecraft.item.ItemStack>(pendingLoot); readFromNBT(packet.func_148857_g()); input=held; System.arraycopy(rewards,0,collectorItems,0,9); pendingLoot.clear(); pendingLoot.addAll(queued); if(worldObj!=null && worldObj.isRemote && (!oldSkin.equals(screenSkin) || oldMeta!=screenSkinMeta)) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
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
        return Math.max(0,inputRemaining("ic2_energy")-(euBinding.equals(euKey())?euFraction:0));
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
        long accepted=Math.min(mana,inputRemaining("botania_mana"));
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
    @Override public boolean canConnectEnergy(ForgeDirection from) { return taskTerminal() && (definition("energy")!=null || definition("buildcraft_mj")!=null); }
    @Override public int receiveEnergy(ForgeDirection from,int amount,boolean simulate) {
        if(worldObj==null || worldObj.isRemote || amount<=0) return 0;
        boolean mj=definition("buildcraft_mj")!=null; long remaining=inputRemaining(mj?"buildcraft_mj":"energy");
        long units=mj?remaining/100000+(remaining%100000==0?0:1):remaining;
        int accepted=(int)Math.min(amount,units);
        if(!simulate && accepted>0) credit(mj?Math.min(remaining,accepted*100000L):accepted);
        return accepted;
    }
    @Override public int getEnergyStored(ForgeDirection from) { return 0; }
    @Override public int getMaxEnergyStored(ForgeDirection from) { long remaining=remaining(definition("buildcraft_mj")!=null?"buildcraft_mj":"energy"); if(definition("buildcraft_mj")!=null) remaining=remaining/100000+(remaining%100000==0?0:1); return (int)Math.min(Integer.MAX_VALUE,remaining); }
    @Override public int getSizeInventory() { return opener()?10:collector()?9:1; }
    @Override public net.minecraft.item.ItemStack getStackInSlot(int slot) { int index=outputIndex(slot); return index>=0?collectorItems[index]:slot==0 && !collector()?input:null; }
    @Override public net.minecraft.item.ItemStack decrStackSize(int slot,int amount) {
        net.minecraft.item.ItemStack existing=getStackInSlot(slot); if(existing==null || amount<=0) return null;
        net.minecraft.item.ItemStack taken=existing.splitStack(Math.min(amount,existing.stackSize)); if(existing.stackSize<=0) setInventorySlotContents(slot,null); markDirty(); return taken;
    }
    @Override public net.minecraft.item.ItemStack getStackInSlotOnClosing(int slot) { net.minecraft.item.ItemStack result=getStackInSlot(slot); setInventorySlotContents(slot,null); return result; }
    @Override public void setInventorySlotContents(int slot,net.minecraft.item.ItemStack stack) {
        int index=outputIndex(slot); if(index>=0) collectorItems[index]=stack; else if(slot==0 && !collector()) input=stack; else return;
        if(stack!=null) stack.stackSize=Math.min(stack.stackSize,Math.min(64,stack.getMaxStackSize())); markDirty();
    }
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
    @Override public int[] getAccessibleSlotsFromSide(int side) { return opener()?new int[]{0,1,2,3,4,5,6,7,8,9}:collector()?new int[]{0,1,2,3,4,5,6,7,8}:new int[]{0}; }
    @Override public boolean canInsertItem(int slot,net.minecraft.item.ItemStack stack,int side) { return isItemValidForSlot(slot,stack); }
    @Override public boolean canExtractItem(int slot,net.minecraft.item.ItemStack stack,int side) { return outputIndex(slot)>=0; }
}
