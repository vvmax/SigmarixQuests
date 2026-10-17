package dev.sigmarix.quests.client;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.texture.DynamicTexture;import net.minecraft.client.resources.*;import net.minecraft.util.ResourceLocation;
/** Stable reference icon packing avoids subpixel sampling changes when optional mods alter the block atlas. */
final class QuestIconAtlas implements IResourceManagerReloadListener {
    private static final ResourceLocation TEXTURE=new ResourceLocation("sigmarixquests:gui/reference_icons");
    private static Layout layout;private static DynamicTexture texture;
    private static final class Layout {int atlas_width,atlas_height;Map<String,int[]> icons=new LinkedHashMap<String,int[]>();}
    @Override public void onResourceManagerReload(IResourceManager resources){if(texture!=null)Minecraft.getMinecraft().getTextureManager().deleteTexture(TEXTURE);texture=null;layout=null;}
    static double[] bind(String name){
        Minecraft mc=Minecraft.getMinecraft();
        if(texture==null){try{
            IResourceManager resources=mc.getResourceManager();
            try(Reader input=new InputStreamReader(resources.getResource(new ResourceLocation("sigmarixquests:gui_icon_layout.json")).getInputStream(),StandardCharsets.UTF_8)){layout=new Gson().fromJson(input,Layout.class);}
            if(layout.atlas_width<1 || layout.atlas_height<1 || layout.atlas_width>4096 || layout.atlas_height>4096)throw new IOException("Invalid icon atlas dimensions");
            BufferedImage image=new BufferedImage(layout.atlas_width,layout.atlas_height,BufferedImage.TYPE_INT_ARGB);
            for(Map.Entry<String,int[]> entry:layout.icons.entrySet()){
                int[] rect=entry.getValue();if(rect.length!=4 || rect[0]<0 || rect[1]<0 || rect[2]<1 || rect[3]<1 || rect[0]+rect[2]>image.getWidth() || rect[1]+rect[3]>image.getHeight())throw new IOException("Invalid icon rectangle");
                try(InputStream input=resources.getResource(new ResourceLocation("ftblib:textures/icons/"+entry.getKey()+".png")).getInputStream()){
                    BufferedImage icon=ImageIO.read(input);if(icon==null)throw new IOException("Invalid icon PNG");
                    for(int y=0;y<rect[3];y++)for(int x=0;x<rect[2];x++)image.setRGB(rect[0]+x,rect[1]+y,icon.getRGB(x*icon.getWidth()/rect[2],y*icon.getHeight()/rect[3]));
                }
            }
            texture=new DynamicTexture(image);mc.getTextureManager().loadTexture(TEXTURE,texture);
        }catch(IOException error){throw new IllegalStateException("Unable to create quest icon atlas",error);}}
        int[] rect=layout.icons.get(name);if(rect==null)return null;mc.getTextureManager().bindTexture(TEXTURE);
        return new double[]{rect[0]/(float)layout.atlas_width,rect[1]/(float)layout.atlas_height,(rect[0]+rect[2])/(float)layout.atlas_width,(rect[1]+rect[3])/(float)layout.atlas_height};
    }
}
