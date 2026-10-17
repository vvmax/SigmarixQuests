package dev.sigmarix.quests.client;

import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import org.lwjgl.opengl.GL11;

/** A chapter graph fitted to a physical display without stretching its nodes. */
public final class QuestWorldMapRenderer extends Gui {
    private static final RenderItem items=new RenderItem();
    public static boolean draw(QuestDevice tile,int width,int height) {
        if(!tile.quest.startsWith("chapter:")) return false;
        if(ClientProxy.snapshot!=null && !tile.bindingProfile.equals(ClientProxy.snapshot.profile)) return false;
        QuestWorldMap progress=QuestWorldMaps.get(tile); Minecraft mc=Minecraft.getMinecraft();
        if(progress==null || ClientProxy.snapshot==null || ClientProxy.snapshot.book==null) return false;
        QuestCore.Book book=ClientProxy.snapshot.book; double unit=QuestPresentation.mapUnit(book);
        drawRect(0,0,width,height,QuestPresentation.color(book.theme.background)); if(!tile.screenFullscreen) drawRect(0,0,width,3,QuestPresentation.color(book.theme.edge)); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        QuestCore.Chapter chapter=QuestPresentation.chapter(book,tile.quest.substring(8));
        java.util.List<QuestPresentation.MapImage> illustrations=new java.util.ArrayList<QuestPresentation.MapImage>();
        if(chapter!=null) for(QuestPresentation.MapImage image:chapter.images) if(!image.dev || ClientProxy.snapshot.canEdit) illustrations.add(image);
        Map<String,QuestCore.Quest> all=new LinkedHashMap<String,QuestCore.Quest>(),visible=new LinkedHashMap<String,QuestCore.Quest>();
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        for(QuestCore.Quest quest:book.quests) {
            all.put(quest.id,quest); if(!progress.values.containsKey(quest.id)) continue;
            visible.put(quest.id,quest); double x=quest.x*unit,y=quest.y*unit;
            minX=Math.min(minX,x-42*quest.size); maxX=Math.max(maxX,x+42*quest.size); minY=Math.min(minY,y-15*quest.size); maxY=Math.max(maxY,y+30*quest.size);
        }
        for(QuestPresentation.MapImage image:illustrations) { double radius=unit/2*Math.hypot(image.width,image.height),x=image.x*unit,y=image.y*unit; minX=Math.min(minX,x-radius); maxX=Math.max(maxX,x+radius); minY=Math.min(minY,y-radius); maxY=Math.max(maxY,y+radius); }
        FontRenderer font=mc.fontRenderer;
        if(!tile.screenFullscreen) { font.drawString(font.trimStringToWidth(tile.screenTitle,width-16),8,10,0xFFFFFF); font.drawString(tile.screenPercent+"%",8,height-16,0xB8CDD9); }
        if(visible.isEmpty() && illustrations.isEmpty()) { font.drawString(QuestText.tr("No visible quests"),8,Math.max(26,height/2),0xB8CDD9); return true; }
        double top=tile.screenFullscreen?6:28,bottom=height-(tile.screenFullscreen?6:24);
        double scale=Math.min((width-12)/Math.max(1,maxX-minX),(bottom-top)/Math.max(1,maxY-minY));
        double originX=(width-(maxX-minX)*scale)/2-minX*scale,originY=top+(bottom-top-(maxY-minY)*scale)/2-minY*scale;
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(originX,originY,0); GL11.glScaled(scale,scale,1);

            if(chapter!=null && !chapter.image.isEmpty()) {
                GL11.glColor4f(1,1,1,.25f); mc.getTextureManager().bindTexture(QuestImages.texture(chapter.image));
                func_152125_a((int)minX,(int)minY,0,0,256,256,Math.max(1,(int)(maxX-minX)),Math.max(1,(int)(maxY-minY)),256,256); GL11.glColor4f(1,1,1,1);
            }
            for(QuestPresentation.MapImage image:illustrations) {
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
                try { GL11.glTranslated(image.x*unit,image.y*unit,0); GL11.glRotated(image.rotation,0,0,1); GL11.glColor4f(1,1,1,(float)image.opacity); mc.getTextureManager().bindTexture(QuestImages.texture(image.resource)); int iw=Math.max(1,(int)Math.round(image.width*unit)),ih=Math.max(1,(int)Math.round(image.height*unit)); func_152125_a(-iw/2,-ih/2,0,0,256,256,iw,ih,256,256); }
                finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
            }
            float oldLineWidth=GL11.glGetFloat(GL11.GL_LINE_WIDTH); GL11.glLineWidth((float)Math.max(.1,book.theme.lineWidth));
            GL11.glDisable(GL11.GL_TEXTURE_2D); int edge=QuestPresentation.color(book.theme.edge);
            GL11.glColor4f((edge>>16&255)/255f,(edge>>8&255)/255f,(edge&255)/255f,(edge>>>24)/255f); GL11.glBegin(GL11.GL_LINES);
            int edges=0;
            for(QuestCore.Quest quest:visible.values()) if(book.theme.lineWidth>0 && !quest.hideDependencyLines) for(String reference:quest.dependencies) {
                try { for(String id:QuestDependencies.owners(book,all,reference,quest.dependencyState.equals("started"))) {
                    QuestCore.Quest from=visible.get(id); if(from!=null && edges++<8192) { GL11.glVertex2d(from.x*unit,from.y*unit); GL11.glVertex2d(quest.x*unit,quest.y*unit); }
                } } catch(IllegalArgumentException hidden) { /* Redacted definitions can omit dependency owners. */ }
            }
            GL11.glEnd(); GL11.glLineWidth(oldLineWidth); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glColor4f(1,1,1,1);
            EntityPlayer viewer=mc.thePlayer; boolean icons=!tile.screenHideIcons && viewer!=null && viewer.getDistanceSq(tile.xCoord+.5,tile.yCoord+.5,tile.zCoord+.5)<=256;
            for(QuestCore.Quest quest:visible.values()) {
                int encoded=progress.values.get(quest.id); String state=encoded/101==3?"claimed":encoded/101==2?"complete":encoded/101==1?"available":"locked";
                String[] style=QuestPresentation.resolve(book,quest,state); int x=(int)Math.round(quest.x*unit),y=(int)Math.round(quest.y*unit);
                QuestNodeRenderer.draw(mc,x,y,Math.max(1,(int)Math.round(13*quest.size)),style[2],QuestPresentation.color(style[0]));
                if(icons && 13*quest.size*scale*Math.min(tile.screenWidth,tile.screenHeight)>=4) try {
                    Item item=(Item)Item.itemRegistry.getObject(quest.icon); if(item!=null) {
                        ItemStack stack=new ItemStack(item,1,quest.iconMeta); stack.setTagCompound(QuestService.tag(quest.iconNbt));
                        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushMatrix();
                        try { GL11.glTranslated(x,y,.01); GL11.glScaled(quest.size,quest.size,.001); RenderHelper.enableGUIStandardItemLighting(); items.renderItemAndEffectIntoGUI(font,mc.getTextureManager(),stack,-8,-8); }
                        finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
                    }
                } catch(RuntimeException ignored) { RenderHelper.disableStandardItemLighting(); }
                if(scale*quest.size*Math.min(tile.screenWidth,tile.screenHeight)>=.35) {
                    String title=font.trimStringToWidth(quest.title,80); GL11.glPushMatrix(); GL11.glTranslated(x,y+16*quest.size,0); GL11.glScaled(quest.size,quest.size,1);
                    font.drawString(title,-font.getStringWidth(title)/2,0,QuestPresentation.color(style[1])); GL11.glPopMatrix();
                }
            }
        } finally { GL11.glPopMatrix(); }
        return true;
    }
    private QuestWorldMapRenderer() {}
}
