package dev.sigmarix.quests.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
/** FTBLib-style flat outline with a translucent hover, using native 1.7 widgets. */
public class QuestFlatButton extends GuiButton {
    public String glyph="",tooltip="";
    public boolean tab,active;
    public int textInset=-1;
    public QuestFlatButton left(int inset) { textInset=inset; return this; }
    public int tint=0xFFFFFFFF;
    public QuestFlatButton(int id,int x,int y,int width,int height,String text) { super(id,x,y,width,height,text); tooltip=text; }
    public QuestFlatButton(int id,int x,int y,String text) { this(id,x,y,200,20,text); }
    public QuestFlatButton glyph(String glyph,boolean tab) { this.glyph=glyph; this.tab=tab; return this; }
    @Override public void drawButton(Minecraft mc,int x,int y) {
        if(!visible) return; field_146123_n=x>=xPosition && y>=yPosition && x<xPosition+width && y<yPosition+height;
        if(!tab) QuestClassicUi.frame(xPosition,yPosition,width,height,QuestClassicUi.BORDER);
        if(enabled && field_146123_n || active) drawRect(xPosition+1,yPosition+1,xPosition+width-1,yPosition+height-1,0x44000000);
        int color=enabled?(field_146123_n && tint==0xFFFFFFFF?0xFFFFFFA0:tint):0xFF999999;
        if(!glyph.isEmpty()) {int size=height>=16?16:8;QuestClassicUi.icon(glyph,xPosition+(width-size)/2,yPosition+(height-size)/2,size,glyph.equals("left") || glyph.equals("right")?QuestClassicUi.BORDER:color);}
        else { int available=width-(textInset<0?8:textInset+4); String text=QuestLibraryScreen.font(mc).trimStringToWidth(displayString,Math.max(0,available)); QuestLibraryScreen.font(mc).drawStringWithShadow(text,xPosition+(textInset<0?(width-QuestLibraryScreen.font(mc).getStringWidth(text))/2:textInset),yPosition+(height-8)/2,color); }
    }
}
