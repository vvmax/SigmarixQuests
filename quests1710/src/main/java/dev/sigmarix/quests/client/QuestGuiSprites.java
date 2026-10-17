package dev.sigmarix.quests.client;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
/** Original FTBLib icons are atlas sprites; their texel inset affects small icons. */
public final class QuestGuiSprites {
    private static final String[] NAMES={"accept","accept_gray","add","add_gray","art","back","beacon","bed","bell","bin","blank","blank_blue","blue_button","blue_grid","bug","camera","cancel","chat","check","close","color_blank","color_hsb","color_rgb","compass","controller","diamond","dice","down","feather","friends","globe","heart","info","info_gray","inv_in","inv_io","inv_none","inv_out","jacket","left","lock","lock_open","map","marker","money","money_bag","notes","offline","online","online_red","player","player_gray","refresh","remove","remove_gray","right","rs_high","rs_low","rs_none","rs_pulse","security_friends","security_private","security_public","security_team","settings","settings_red","shield","sort_az","star","support","time","toggle_day","toggle_gamemode","toggle_night","toggle_rain","up"};
    @SubscribeEvent public void stitch(TextureStitchEvent.Pre event){if(event.map.getTextureType()==0)for(String name:NAMES){String id="ftblib:icons/"+name;event.map.setTextureEntry(id,new Sprite(id,new ResourceLocation("ftblib:textures/icons/"+name+".png")));}}
    private static final class Sprite extends TextureAtlasSprite {
        private final ResourceLocation source;
        Sprite(String name,ResourceLocation source){super(name);this.source=source;}
        @Override public boolean hasCustomLoader(IResourceManager manager,ResourceLocation ignored){return true;}
        @Override public boolean load(IResourceManager manager,ResourceLocation ignored){try(java.io.InputStream input=manager.getResource(source).getInputStream()){BufferedImage image=ImageIO.read(input);BufferedImage[] levels=new BufferedImage[Math.max(1,net.minecraft.client.Minecraft.getMinecraft().gameSettings.mipmapLevels+1)];levels[0]=image;loadSprite(levels,null,false);return false;}catch(java.io.IOException error){throw new IllegalStateException("Unable to load quest icon "+source,error);}}
    }
}
