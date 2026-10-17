package dev.sigmarix.quests;
import java.io.File;
import java.net.URL;
import java.util.*;
import net.minecraft.launchwrapper.*;
/** Boots only Forge registries in an isolated LaunchClassLoader; starts no game/server. */
public final class ForgeTestLauncher {
    public static void main(String[] args) throws Exception {
        List<URL> urls=new ArrayList<URL>();
        for(String entry:System.getProperty("java.class.path").split(File.pathSeparator)) urls.add(new File(entry).toURI().toURL());
        Launch.classLoader=new LaunchClassLoader(urls.toArray(new URL[0]));
        Launch.blackboard=new HashMap<String,Object>(); Launch.blackboard.put("fml.deobfuscatedEnvironment",true);
        Thread.currentThread().setContextClassLoader(Launch.classLoader);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        Class<?> bootstrap=Class.forName("dev.sigmarix.quests.ForgeTestBootstrap",true,Launch.classLoader);
        int failures=(Integer)bootstrap.getMethod("run").invoke(null);
        if(failures!=0) throw new AssertionError(failures+" Forge integration tests failed");
    }
}
