package sigma.guide;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.*;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.init.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.ScreenShotHelper;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
@Mod(modid="sigmaguidecapture",name="Isolated guide screenshot fixture",version="1")
public class GuideCapture {
 int tick,scene=-1,readyAt; boolean launched,finished; volatile int requested=-1; volatile boolean ready;
 String[] names={"blood_altar_t2","blood_altar_t3","blood_altar_t4","blood_altar_t5","blood_altar_t6","blood_ritual_water","thaum_infusion","witchery_altar"};
 @Mod.EventHandler public void init(FMLInitializationEvent e){FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent public void server(TickEvent.ServerTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||requested<0)return;
  MinecraftServer s=MinecraftServer.getServer();if(s==null||s.getConfigurationManager().playerEntityList.isEmpty())return;
  EntityPlayerMP p=(EntityPlayerMP)s.getConfigurationManager().playerEntityList.get(0);World w=p.worldObj;int n=requested;requested=-1;
  w.getGameRules().setOrCreateGameRule("doDaylightCycle","false");w.getGameRules().setOrCreateGameRule("doMobSpawning","false");w.setWorldTime(6000);
  int x=n*64,y=70,z=0;
  int floor=n<5?68-n:63;
  for(int dx=-15;dx<=15;dx++)for(int dz=-15;dz<=15;dz++){w.setBlock(x+dx,floor,z+dz,Blocks.stonebrick);for(int dy=floor+1;dy<85;dy++)w.setBlockToAir(x+dx,dy,z+dz);}
  if(n<5){
   Class<?> u=Class.forName("WayofTime.alchemicalWizardry.common.bloodAltarUpgrade.UpgradedAltars");
   String[] fields={"secondTierAltar","thirdTierAltar","fourthTierAltar","fifthTierAltar","sixthTierAltar"};
   List<?> list=(List<?>)u.getField(fields[n]).get(null);
   for(Object a:list){Class<?> c=a.getClass();int dx=(Integer)c.getMethod("getX").invoke(a),dy=(Integer)c.getMethod("getY").invoke(a),dz=(Integer)c.getMethod("getZ").invoke(a);Block b=(Block)c.getMethod("getBlock").invoke(a);int m=(Integer)c.getMethod("getMetadata").invoke(a);if(b==Blocks.stonebrick)b=Blocks.stonebrick;w.setBlock(x+dx,y+dy,z+dz,b,m,3);}
   w.setBlock(x,y,z,GameRegistry.findBlock("AWWayofTime","Altar"));
   int tier=(Integer)u.getMethod("isAltarValid",World.class,int.class,int.class,int.class).invoke(null,w,x,y,z);
   if(tier!=n+2)throw new IllegalStateException("Altar tier mismatch "+tier+" scene "+n);
   System.out.println("GUIDE_VALIDATED altar="+tier+" components="+list.size());
  }else if(n==5){
   w.setBlock(x,64,z,GameRegistry.findBlock("AWWayofTime","masterStone"));
   for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1})w.setBlock(x+dx,64,z+dz,GameRegistry.findBlock("AWWayofTime","ritualStone"),1,3);
  }else if(n==6){
   Block b=GameRegistry.findBlock("Thaumcraft","blockStoneDevice");w.setBlock(x,64,z,b,1,3);w.setBlock(x,66,z,b,2,3);
   for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1}){w.setBlock(x+dx,64,z+dz,b,3,3);w.setBlock(x+dx,65,z+dz,b,4,3);}
   for(int[] a:new int[][]{{-3,0},{3,0},{0,-3},{0,3}})w.setBlock(x+a[0],64,z+a[1],b,1,3);
  }else{
   Block b=GameRegistry.findBlock("witchery","altar");for(int dx=0;dx<3;dx++)for(int dz=0;dz<2;dz++)w.setBlock(x+dx,64,z+dz,b,0,3);
  }
  if(n==0)verifyReactor();
  double distance=n<5?4+n*4:3.5;double cameraY=n<5?73+n*2:68;double targetY=n<5?69:65;
  p.addStat(net.minecraft.stats.AchievementList.openInventory,1);
  float pitch=(float)Math.toDegrees(Math.atan2(cameraY+1.62-targetY,Math.sqrt(2)*distance));
  p.capabilities.allowFlying=true;p.capabilities.isFlying=true;p.sendPlayerAbilities();p.playerNetServerHandler.setPlayerLocation(x+distance,cameraY,z+distance,135,pitch);
  ready=true;
 }
 void verifyReactor()throws Exception{
  Class<?> api=Class.forName("ic2.api.reactor.IReactor"),component=Class.forName("ic2.api.reactor.IReactorComponent"),items=Class.forName("ic2.core.Ic2Items");
  final net.minecraft.item.ItemStack[][] grid=new net.minecraft.item.ItemStack[3][6];
  grid[0][0]=((net.minecraft.item.ItemStack)items.getField("reactorUraniumSimple").get(null)).copy();
  grid[1][0]=((net.minecraft.item.ItemStack)items.getField("reactorVent").get(null)).copy();final int[] heat={0};
  Object reactor=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},new InvocationHandler(){public Object invoke(Object proxy,Method m,Object[] a){
   String k=m.getName();if(k.equals("getItemAt")){int x=(Integer)a[0],y=(Integer)a[1];return x>=0&&x<3&&y>=0&&y<6?grid[x][y]:null;}
   if(k.equals("setItemAt")){grid[(Integer)a[0]][(Integer)a[1]]=(net.minecraft.item.ItemStack)a[2];return null;}
   if(k.equals("produceEnergy"))return true;if(k.equals("isFluidCooled"))return false;if(k.equals("getHeat"))return heat[0];
   if(k.equals("addHeat")){heat[0]+=(Integer)a[0];return heat[0];}if(k.equals("setHeat")){heat[0]=(Integer)a[0];return null;}
   if(m.getReturnType()==float.class)return 0f;if(m.getReturnType()==double.class)return 0d;if(m.getReturnType()==int.class)return 0;return null;
  }});
  Method process=component.getMethod("processChamber",api,net.minecraft.item.ItemStack.class,int.class,int.class,boolean.class);
  for(int cycle=0;cycle<20000;cycle++)for(boolean heatRun:new boolean[]{false,true})for(int x=0;x<3;x++)for(int y=0;y<6;y++){
   net.minecraft.item.ItemStack stack=grid[x][y];if(stack!=null&&component.isInstance(stack.getItem()))process.invoke(stack.getItem(),reactor,stack,x,y,heatRun);
   if(heat[0]!=0)throw new IllegalStateException("Reactor hull heat "+heat[0]+" cycle "+cycle);
  }
  if(grid[1][0]==null)throw new IllegalStateException("Vent destroyed");
  System.out.println("GUIDE_VALIDATED IC2 native component simulation: 20000 cycles, hull heat=0, vent retained");
 }
 @SubscribeEvent public void client(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||finished)return;Minecraft m=Minecraft.getMinecraft();tick++;
  if(!launched&&m.currentScreen instanceof GuiMainMenu&&tick>40){launched=true;m.launchIntegratedServer("GuideIllustrations","GuideIllustrations",new WorldSettings(1234,WorldSettings.GameType.CREATIVE,false,false,WorldType.FLAT));return;}
  if(m.theWorld==null||m.thePlayer==null)return;
  if(scene<0){scene=0;requested=scene;return;}
  if(ready){ready=false;readyAt=tick+160;m.displayGuiScreen(null);m.gameSettings.hideGUI=true;m.gameSettings.thirdPersonView=0;m.gameSettings.renderDistanceChunks=5;m.gameSettings.pauseOnLostFocus=false;}
  if(readyAt>0&&tick>=readyAt){
   readyAt=0;File out=new File(m.mcDataDir,"captures");out.mkdirs();ScreenShotHelper.saveScreenshot(out,names[scene]+".png",m.displayWidth,m.displayHeight,m.getFramebuffer());
   System.out.println("GUIDE_CAPTURED "+names[scene]);scene++;if(scene==names.length){finished=true;m.shutdown();}else requested=scene;
  }
 }
}
