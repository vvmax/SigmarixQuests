package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;import java.util.*;import net.minecraft.client.Minecraft;import org.lwjgl.opengl.GL11;
/** Original progress screens use a fitted rectangle for each quest, colored by progress. */
public final class QuestWorldMapRenderer {
    public static boolean draw(QuestDevice tile,int width,int height){
        if(!tile.quest.startsWith("chapter:") || ClientProxy.snapshot==null || !tile.bindingProfile.equals(ClientProxy.snapshot.profile))return false;
        QuestWorldMap progress=QuestWorldMaps.get(tile);if(progress==null)return true;Minecraft mc=Minecraft.getMinecraft();double range=3*Math.min(tile.screenWidth,tile.screenHeight);boolean nearby=mc.thePlayer!=null && mc.thePlayer.getDistanceSq(tile.xCoord+.5,tile.yCoord+tile.screenHeight/2D,tile.zCoord+.5)<=range*range;return draw(tile,width,height,progress,nearby);
    }
    static boolean draw(QuestDevice tile,int width,int height,QuestWorldMap progress,boolean nearby){QuestCore.Book book=ClientProxy.snapshot.book;QuestCore.Chapter chapter=QuestPresentation.chapter(book,tile.quest.substring(8));if(chapter==null)return true;
        List<QuestCore.Quest> quests=new ArrayList<QuestCore.Quest>();double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        for(QuestCore.Quest quest:book.quests)if(progress.values.containsKey(quest.id)){quests.add(quest);minX=Math.min(minX,quest.x-quest.size/2);maxX=Math.max(maxX,quest.x+quest.size/2);minY=Math.min(minY,quest.y-quest.size/2);maxY=Math.max(maxY,quest.y+quest.size/2);}
        Minecraft mc=Minecraft.getMinecraft();boolean unicode=mc.fontRenderer.getUnicodeFlag();mc.fontRenderer.setUnicodeFlag(true);GL11.glPushMatrix();
        try{GL11.glTranslated((width-Math.min(width,height))/2D,(height-Math.min(width,height))/2D,0);double canvas=Math.min(width,height);GL11.glScaled(canvas,canvas,1);
            if(!tile.screenFullscreen){String team=tile.owner==null?"":mc.thePlayer==null?tile.owner.toString():QuestTeamScreen.ownerName(tile.owner.toString());for(QuestNetwork.TeamEntry candidate:ClientProxy.snapshot.teams)if(candidate.owner.equals(tile.owner==null?"":tile.owner.toString())){team=QuestTeamScreen.teamTitle(candidate);break;}QuestScreenRenderer.worldString("\u00a7e"+chapter.title+"\u00a7r | "+team,.02,.12);QuestScreenRenderer.worldString(tile.screenPercent+"%",.86,.12);}
            if(quests.isEmpty())return true;double sizeX=maxX-minX,sizeY=maxY-minY;
            if(!tile.screenFullscreen){GL11.glTranslated(.15,.15,0);GL11.glScaled(.7,.7,1);}
            GL11.glDisable(GL11.GL_TEXTURE_2D);net.minecraft.client.renderer.Tessellator t=net.minecraft.client.renderer.Tessellator.instance;t.startDrawingQuads();
            for(QuestCore.Quest quest:quests){int percent=progress.values.get(quest.id)%101,color=percent==0?0xFF4747:percent==100?0x68D341:0xFFDF42;double rw=(1/sizeX-.005)*quest.size,rh=(1/sizeY-.005)*quest.size,rx=(quest.x-minX)/sizeX+.0025-rw/2,ry=(quest.y-minY)/sizeY+.0025-rh/2;t.setColorOpaque_I(color);t.addVertex(rx,ry,0);t.addVertex(rx,ry+rh,0);t.addVertex(rx+rw,ry+rh,0);t.addVertex(rx+rw,ry,0);}t.draw();GL11.glEnable(GL11.GL_TEXTURE_2D);
            if(!tile.screenHideIcons && nearby)for(QuestCore.Quest quest:quests){double scale=.75/Math.max(sizeX,sizeY)*quest.size;GL11.glPushMatrix();try{GL11.glTranslated((quest.x-minX)/sizeX,(quest.y-minY)/sizeY,.009);GL11.glScaled(scale,scale,1);QuestScreenRenderer.worldQuest(quest);}finally{GL11.glPopMatrix();}}
            return true;
        }finally{GL11.glPopMatrix();mc.fontRenderer.setUnicodeFlag(unicode);}
    }
    private QuestWorldMapRenderer(){}
}
