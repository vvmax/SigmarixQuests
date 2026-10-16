package cofh.api.energy;
import net.minecraftforge.common.util.ForgeDirection;
/** Compile-only RF interoperability signatures; use an installed CoFH API implementation at runtime. */
public interface IEnergyReceiver extends IEnergyConnection {
    int receiveEnergy(ForgeDirection side,int amount,boolean simulate);
    int getEnergyStored(ForgeDirection side);
    int getMaxEnergyStored(ForgeDirection side);
}
