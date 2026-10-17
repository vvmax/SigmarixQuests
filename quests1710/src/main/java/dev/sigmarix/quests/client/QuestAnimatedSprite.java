package dev.sigmarix.quests.client;
import java.awt.image.BufferedImage;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.data.AnimationMetadataSection;
/** Adds the reference barrier's interpolated animation to the 1.7 texture atlas. */
public final class QuestAnimatedSprite extends TextureAtlasSprite {
    private AnimationMetadataSection metadata;
    private int[][] interpolated;
    public QuestAnimatedSprite(String name){super(name);}
    @Override public void loadSprite(BufferedImage[] images,AnimationMetadataSection animation,boolean anisotropic){super.loadSprite(images,animation,anisotropic);metadata=animation;interpolated=null;}
    @Override public void updateAnimation(){
        super.updateAnimation();if(metadata==null || tickCounter==0 || framesTextureData.size()<2)return;
        boolean sequential=metadata.getFrameCount()==0;int count=sequential?framesTextureData.size():metadata.getFrameCount(),current=sequential?frameCounter:metadata.getFrameIndex(frameCounter),next=sequential?(frameCounter+1)%count:metadata.getFrameIndex((frameCounter+1)%count);
        if(current==next || current<0 || next<0 || current>=framesTextureData.size() || next>=framesTextureData.size())return;
        int[][] a=getFrameTextureData(current),b=getFrameTextureData(next);double weight=1D-tickCounter/(double)(sequential?metadata.getFrameTime():metadata.getFrameTimeSingle(frameCounter));
        if(interpolated==null || interpolated.length!=a.length)interpolated=new int[a.length][];
        for(int level=0;level<a.length;level++)if(a[level]!=null && b[level]!=null && a[level].length==b[level].length){
            if(interpolated[level]==null || interpolated[level].length!=a[level].length)interpolated[level]=new int[a[level].length];
            for(int i=0;i<a[level].length;i++){int first=a[level][i],second=b[level][i];int red=(int)(weight*(first>>16&255)+(1-weight)*(second>>16&255)),green=(int)(weight*(first>>8&255)+(1-weight)*(second>>8&255)),blue=(int)(weight*(first&255)+(1-weight)*(second&255));interpolated[level][i]=(first&0xFF000000)|(red<<16)|(green<<8)|blue;}
        }
        TextureUtil.uploadTextureMipmap(interpolated,width,height,originX,originY,false,false);
    }
}
