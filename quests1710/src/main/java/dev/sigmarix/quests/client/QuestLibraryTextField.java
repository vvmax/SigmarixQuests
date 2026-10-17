package dev.sigmarix.quests.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.lwjgl.opengl.GL11;
/** Native text editing with FTBLib's unshadowed text, cursor and selection appearance. */
final class QuestLibraryTextField extends GuiTextField {
    private final FontRenderer font;private final int left,top,contentWidth;private int tint=0xFFFFFF,offset,clipX,clipY,clipW,clipH;
    QuestLibraryTextField(FontRenderer font,int x,int y,int w,int h){super(font,x,y,w,h);this.font=font;left=x;top=y;contentWidth=w;setEnableBackgroundDrawing(false);clipX=x-4;clipY=y-4;clipW=w+8;clipH=h+4;}
    QuestLibraryTextField clip(int x,int y,int w,int h){clipX=x;clipY=y;clipW=w;clipH=h;return this;}
    @Override public void setTextColor(int color){super.setTextColor(color);tint=color;}
    @Override public void drawTextBox(){
        String text=getText();int cursor=getCursorPosition();offset=Math.max(0,Math.min(offset,text.length()));
        if(cursor<offset)offset=cursor;
        while(offset<cursor && font.getStringWidth(text.substring(offset,cursor))>Math.max(1,contentWidth-2))offset++;
        String visible=font.trimStringToWidth(text.substring(offset),contentWidth+8);int j=Math.max(0,Math.min(visible.length(),cursor-offset)),k=Math.max(0,Math.min(visible.length(),getSelectionEnd()-offset));
        Minecraft mc=Minecraft.getMinecraft();ScaledResolution res=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);int scale=res.getScaleFactor();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try{
            GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(clipX*scale,(res.getScaledHeight()-clipY-clipH)*scale,clipW*scale,clipH*scale);
            font.drawString(visible,left,top,tint);int cx=left+font.getStringWidth(visible.substring(0,j));
            if(isFocused() && Minecraft.getSystemTime()%1000>500){if(cursor<text.length())Gui.drawRect(cx-1,top-1,cx,top+10,0xFF000000|tint);else Gui.drawRect(cx,top+7,cx+5,top+8,0xFF000000|tint);}
            if(k!=j){int end=left+font.getStringWidth(visible.substring(0,k))-1;GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);GL11.glLogicOp(GL11.GL_OR_REVERSE);Gui.drawRect(Math.min(cx,end),top-1,Math.max(cx,end),top+10,0xFF0000FF);}
        }finally{GL11.glPopAttrib();}
    }
}
