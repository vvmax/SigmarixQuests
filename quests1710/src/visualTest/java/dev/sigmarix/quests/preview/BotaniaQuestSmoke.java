package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import vazkii.botania.common.entity.EntityManaBurst;
/** Real Botania entity flies into the terminal through the ordinary world collision path. */
public final class BotaniaQuestSmoke {
    private static EntityManaBurst burst;
    private static World world;
    private static EntityPlayerMP player;
    private static java.util.UUID recipient;
    private static int x,y,z,ticks;
    public static void start(EntityPlayerMP owner) {
        player=owner; recipient=java.util.UUID.randomUUID(); world=owner.worldObj; x=(int)Math.floor(owner.posX)+3; y=(int)Math.floor(owner.posY); z=(int)Math.floor(owner.posZ)+3; ticks=0;
        world.setBlock(x,y,z,QuestBlocks.devices[0]); QuestDevice sink=(QuestDevice)world.getTileEntity(x,y,z); sink.owner=recipient; sink.quest="smoke-mana"; sink.task="charge"; sink.markDirty();
        world.setBlockToAir(x-1,y,z); world.setBlockToAir(x-2,y,z);
        burst=new EntityManaBurst(world); burst.setFake(false); burst.setMana(16); burst.setStartingMana(16); burst.setMinManaLoss(200); burst.setManaLossPerTick(0); burst.setColor(0x20FF20); burst.setBurstSourceCoords(x-3,y,z);
        burst.setPosition(x-1.5,y+.5,z+.5); burst.setMotion(.4,0,0); world.spawnEntityInWorld(burst);
    }
    public static boolean tick() {
        if(burst==null) return false; ticks++;
        if(QuestsMod.service.progress().value(recipient,"smoke-mana","charge")>=16) {
            if(!burst.isDead) throw new IllegalStateException("Botania credited mana without consuming the burst");
            if(QuestsMod.service.progress().value(player.getUniqueID(),"smoke-mana","charge")!=0) throw new IllegalStateException("Resource progress went to the online bystander instead of the offline owner");
            System.out.println("Quest Botania PASS: real mana burst delivered 16 mana to offline owner and was consumed");
            burst=null; world.setBlockToAir(x,y,z); return true;
        }
        if(ticks>200) throw new IllegalStateException("Real Botania mana burst did not deliver mana; dead="+burst.isDead+", pos="+burst.posX+","+burst.posY+","+burst.posZ);
        return false;
    }
}
