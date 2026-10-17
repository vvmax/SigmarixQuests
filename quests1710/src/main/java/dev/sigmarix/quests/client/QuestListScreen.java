package dev.sigmarix.quests.client;
import java.util.*;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
/** Common 1.12 list geometry: a 140px panel, 16px scrollbar, optional 12px search box. */
abstract class QuestListScreen extends QuestLibraryScreen {
    static final class Row {
        int id;String title,icon="",playerId="",playerName="";int border=0xFF808080,height;boolean spacer,customIcon;boolean enabled=true,teamHead;List<String> tooltip=new ArrayList<String>();
        Row(int id,String title,String icon){this.id=id;this.title=title;this.icon=icon;}
    }
    final GuiScreen parent;final boolean hasSearch,questTheme;final int borderH,borderV,gap;
    protected GuiTextField input;protected int left,top,panelWidth,boxWidth,boxHeight,scroll,contentHeight,panelY;
    private boolean dragging;private int dragOffset;private final List<Row> visibleRows=new ArrayList<Row>();
    private Object seen;private int refreshTicks;
    QuestListScreen(GuiScreen parent,boolean search,boolean questTheme,int h,int v,int gap){this.parent=parent;hasSearch=search;this.questTheme=questTheme;borderH=h;borderV=v;this.gap=gap;}
    protected int rowHeight(){return 20;}
    private int rowHeight(Row row){return row.height>0?row.height:rowHeight();}
    private int rowOffset(int index){int y=0;for(int i=0;i<index;i++)y+=rowHeight(visibleRows.get(i))+gap;return y;}
    protected int minimumPanelWidth(){return 100;}
    protected void drawRowIcon(Row row,int x,int y,int size){QuestClassicUi.icon(row.icon,x,y,size,row.enabled?0xFFFFFFFF:0xFF999999);}
    protected abstract List<Row> rows();protected abstract String title();protected abstract void click(Row row,int button);
    protected Object revision(){return ClientProxy.snapshot;}
    @Override public void initGui(){rebuild();}
    protected final void rebuild(){
        String filter=input==null?"":input.getText();boolean focus=input!=null && input.isFocused();int previousWidth=panelWidth;
        List<Row> source=rows();visibleRows.clear();panelWidth=minimumPanelWidth();
        for(Row row:source)if(!hasSearch || filter.isEmpty() || net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(row.title).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))){visibleRows.add(row);panelWidth=Math.max(panelWidth,fontRendererObj.getStringWidth(row.title)+(row.icon.isEmpty() && row.playerId.isEmpty()?8:28));}
        if(hasSearch)panelWidth=Math.max(panelWidth,previousWidth);boxWidth=panelWidth+39;boxHeight=158+(hasSearch?14:0);left=(width-boxWidth)/2;top=(height-boxHeight)/2;panelY=hasSearch?23:9;
        contentHeight=borderV*2+rowOffset(visibleRows.size())-(visibleRows.isEmpty()?0:gap);scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-140)));
        input=new QuestLibraryTextField(fontRendererObj,left+12,top+8,boxWidth-24,8).clip(left+8,top+6,boxWidth-16,12);input.setMaxStringLength(128);input.setText(filter);input.setFocused(focus);
        buttonList.clear();for(int i=0;i<visibleRows.size();i++){Row row=visibleRows.get(i);GuiButton button=questTheme?new QuestFlatButton(row.id,left+9+borderH,top+panelY+borderV+rowOffset(i)-scroll,panelWidth-borderH*2,rowHeight(row),row.title).left(row.icon.isEmpty() && row.playerId.isEmpty()?4:(rowHeight(row)<16?(rowHeight(row)-8)/2+12:22)):new QuestLibraryUi.Button(row.id,left+9+borderH,top+panelY+borderV+rowOffset(i)-scroll,panelWidth-borderH*2,rowHeight(row),row.title,row.playerId.isEmpty() && !row.customIcon?row.icon:"blank").left();button.enabled=row.enabled;buttonList.add(button);}
        seen=revision();
    }
    private void drawScrollbar(){
        int x=left+panelWidth+15,y=top+panelY-1,h=142,max=Math.max(0,contentHeight-140);
        if(questTheme)drawRect(x,y,x+16,y+h,0x44000000);else{QuestLibraryUi.patch("container_beacon",35,136,18,18,3,x,y,16,h);}
        if(max>0){int size=Math.max(10,(int)(140D/(max+140)*h)),sy=y+(int)(scroll/(double)max*(h-size));if(questTheme){QuestClassicUi.frame(x,sy,16,size,QuestClassicUi.BORDER);drawRect(x+1,sy+1,x+15,sy+size-1,0x44000000);}else QuestLibraryUi.patch("container_beacon",0,219,22,22,6,x+1,sy+1,14,size-2);}
    }
    @Override public void drawScreen(int mx,int my,float partial){
        drawDefaultBackground();if(questTheme)QuestClassicUi.background(mc,left,top,boxWidth,boxHeight);else QuestLibraryUi.window(left,top,boxWidth,boxHeight);
        drawCenteredString(fontRendererObj,title(),width/2,top-11,0xFFFFFF);
        if(hasSearch){QuestLibraryUi.textBox(left+8,top+6,boxWidth-16,12);input.drawTextBox();if(input.getText().isEmpty() && !input.isFocused())QuestLibraryUi.ghost(net.minecraft.util.StatCollector.translateToLocal("gui.search_box"),left+12,top+8);}
        if(questTheme)QuestClassicUi.frame(left+8,top+panelY-1,panelWidth+2,142,QuestClassicUi.BORDER);else QuestLibraryUi.slot(left+9,top+panelY,panelWidth,140);
        drawScrollbar();ScaledResolution res=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);int f=res.getScaleFactor();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((left+9)*f,(res.getScaledHeight()-top-panelY-140)*f,panelWidth*f,140*f);
        Row hover=null;
        try{for(int i=0;i<buttonList.size();i++){GuiButton b=(GuiButton)buttonList.get(i);Row row=visibleRows.get(i);b.visible=b.yPosition+b.height>top+panelY && b.yPosition<top+panelY+140;if(!row.spacer)b.drawButton(mc,mx,my);if(!b.visible || row.spacer)continue;
            if(!row.playerId.isEmpty())QuestPlayerHeads.draw(row.playerId,row.playerName,b.xPosition+2,b.yPosition+2,16,row.border,row.teamHead);
            else if((questTheme || row.customIcon) && !row.icon.isEmpty())drawRowIcon(row,b.xPosition+(b.height-(b.height<16?8:16))/2,b.yPosition+(b.height-(b.height<16?8:16))/2,b.height<16?8:16);
            if(mx>=b.xPosition && mx<b.xPosition+b.width && my>=Math.max(top+panelY,b.yPosition) && my<Math.min(top+panelY+140,b.yPosition+b.height))hover=row;
        }}finally{GL11.glPopAttrib();}
        if(hover!=null && !hover.tooltip.isEmpty())drawHoveringText(hover.tooltip,mx,my,fontRendererObj);
    }
    @Override protected void actionPerformed(GuiButton button){for(Row row:visibleRows)if(row.id==button.id && row.enabled){click(row,0);return;}}
    @Override protected void mouseClicked(int x,int y,int button){
        if(hasSearch)input.mouseClicked(x,y,button);int sbx=left+panelWidth+15,sby=top+panelY-1,max=Math.max(0,contentHeight-140);
        if(button==0 && x>=sbx && x<sbx+16 && y>=sby && y<sby+142 && max>0){int size=Math.max(10,(int)(140D/(max+140)*142)),sy=sby+(int)(scroll/(double)max*(142-size));dragOffset=y>=sy && y<sy+size?y-sy:size/2;dragging=true;drag(x,y);return;}
        if(x<left+9 || x>=left+9+panelWidth || y<top+panelY || y>=top+panelY+140)return;
        int offset=y-top-panelY+scroll-borderV;if(offset<0)return;
        for(int index=0;index<visibleRows.size();index++){Row row=visibleRows.get(index);int rowY=rowOffset(index);if(offset>=rowY && offset<rowY+rowHeight(row) && row.enabled && !row.spacer){mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.func_147674_a(new net.minecraft.util.ResourceLocation("gui.button.press"),1));click(row,button);return;}}

    }
    private void drag(int x,int y){int max=Math.max(0,contentHeight-140);if(max==0)return;int size=Math.max(10,(int)(140D/(max+140)*142));scroll=(int)Math.round((y-top-panelY+1-dragOffset)/(double)Math.max(1,142-size)*max);rebuild();}
    @Override protected void mouseClickMove(int x,int y,int button,long time){if(dragging)drag(x,y);}
    @Override protected void mouseMovedOrUp(int x,int y,int button){super.mouseMovedOrUp(x,y,button);if(button==0)dragging=false;}
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll+=wheel>0?-20:20;rebuild();}}
    @Override protected void keyTyped(char ch,int key){if(key==1){mc.displayGuiScreen(parent);return;}if(hasSearch && input.textboxKeyTyped(ch,key)){rebuild();return;}super.keyTyped(ch,key);}
    @Override public void updateScreen(){if(input!=null)input.updateCursorCounter();if(seen!=revision())rebuild();if(ClientProxy.snapshot!=null && ++refreshTicks%40==0)ClientProxy.send(new dev.sigmarix.quests.QuestNetwork.Action());}
    @Override public boolean doesGuiPauseGame(){return false;}
}
