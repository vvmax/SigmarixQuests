package dev.sigmarix.quests;
import cpw.mods.fml.common.Loader;
import java.io.File;
import java.util.ArrayList;
import org.junit.runner.*;
import org.junit.internal.TextListener;
public final class ForgeTestBootstrap {
    public static int run() throws Exception {
        java.lang.reflect.Field side=cpw.mods.fml.relauncher.FMLRelaunchLog.class.getDeclaredField("side");
        side.setAccessible(true); side.set(null,cpw.mods.fml.relauncher.Side.SERVER);
        Loader.injectData("7","99","40","1614","1.7.10","9.05",new File("."),new ArrayList<String>());
        JUnitCore runner=new JUnitCore(); runner.addListener(new TextListener(System.out));
        Result result=runner.run(QuestSystemsTest.class);
        return result.getFailureCount();
    }
}
