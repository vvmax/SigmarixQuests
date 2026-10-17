package dev.sigmarix.quests;
import java.lang.reflect.*;
import net.minecraft.entity.player.*;
/** Optional 1.7.10 ProjectE wallet API. No ProjectE classes are linked into the production mod. */
public final class QuestProjectE {
    public static final long MAX_EXACT=9007199254740992L;
    private static volatile Method read;
    private static Method write,sync;
    public static synchronized void validateAPI() {
        if(read!=null) return;
        if(!cpw.mods.fml.common.Loader.isModLoaded("ProjectE")) throw new IllegalArgumentException("EMC tasks require ProjectE");
        try {
            Class<?> api=Class.forName("moze_intel.projecte.playerData.Transmutation");
            Method getter=api.getMethod("getEmc",EntityPlayer.class),setter=api.getMethod("setEmc",EntityPlayer.class,double.class),synchronizer=api.getMethod("sync",EntityPlayer.class);
            write=setter; sync=synchronizer; read=getter;
        } catch(ReflectiveOperationException e) { throw new IllegalArgumentException("Unsupported ProjectE wallet API; expected 1.7.10 PE1.10.1",e); }
    }
    public static double balance(EntityPlayer player) {
        validateAPI();
        try { double value=((Number)read.invoke(null,player)).doubleValue(); checkBalance(value); return value; }
        catch(ReflectiveOperationException e) { throw new IllegalArgumentException("ProjectE wallet read failed",e); }
    }
    public static void set(EntityPlayerMP player,double value) {
        validateAPI(); checkBalance(value);
        try { write.invoke(null,player,value); sync.invoke(null,player); }
        catch(ReflectiveOperationException e) { throw new IllegalArgumentException("ProjectE wallet update failed",e); }
    }
    public static void checkBalance(double value) { if(!Double.isFinite(value) || value<0) throw new IllegalArgumentException("Invalid ProjectE EMC balance"); }
    public static double subtract(double balance,long amount) {
        checkBalance(balance);
        if(amount<0 || amount>MAX_EXACT || balance<amount) throw new IllegalArgumentException("Invalid EMC transfer amount");
        double result=balance-amount;
        if(balance-result!=(double)amount) throw new IllegalArgumentException("EMC balance exceeds precise transfer range of ProjectE 1.7 double storage");
        return result;
    }
    private QuestProjectE() {}
}
