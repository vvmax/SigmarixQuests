package dev.sigmarix.quests.client;
import java.util.function.Consumer;import java.util.function.Predicate;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
/** 230x54 FTBLib scalar editor, shared by all property windows. */
final class QuestValueScreen extends QuestLibraryScreen {
    private final GuiScreen parent;private final String value;private final int color,limit;private final Consumer<String> apply;private final Predicate<String> valid;private GuiTextField input;private int left,top;private String error="";
    QuestValueScreen(GuiScreen parent,String value,int color,int limit,Consumer<String> apply){this(parent,value,color,limit,text->true,apply);}
    QuestValueScreen(GuiScreen parent,String value,int color,int limit,Predicate<String> valid,Consumer<String> apply){this.parent=parent;this.value=value;this.color=color;this.limit=limit;this.valid=valid;this.apply=apply;}
    static QuestValueScreen nbt(GuiScreen parent,String value,Consumer<String> apply){return new QuestValueScreen(parent,value.isEmpty()?"null":dev.sigmarix.quests.QuestSnbt.write(dev.sigmarix.quests.QuestService.tag(value)),0x999999,32768,text->{try{if(!text.equals("null")){if(text.isEmpty())return false;dev.sigmarix.quests.QuestService.tag(text);}return true;}catch(RuntimeException invalid){return false;}},text->apply.accept(text.equals("null")?"":text));}
    private boolean valid(){try{return valid.test(input.getText());}catch(RuntimeException error){return false;}}
    @Override public void initGui(){Keyboard.enableRepeatEvents(true);left=(width-230)/2;top=(height-54)/2;String text=input==null?value:input.getText();input=new QuestLibraryTextField(fontRendererObj,left+12,top+12,206,12);input.setMaxStringLength(limit);input.setText(text);input.setCursorPosition(0);input.setTextColor(color);input.setFocused(true);buttonList.clear();buttonList.add(new QuestLibraryUi.Button(1,left+8,top+30,105,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(0,left+117,top+30,105,16,QuestText.tr("Accept"),""));}
    @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();QuestLibraryUi.window(left,top,230,54);QuestLibraryUi.textBox(left+8,top+8,214,16);boolean allowed=valid();input.setTextColor(allowed?color:0xFF0000);((GuiButton)buttonList.get(1)).enabled=allowed;input.drawTextBox();if(!error.isEmpty())drawCenteredString(fontRendererObj,error,width/2,top-12,0xFF5555);super.drawScreen(x,y,partial);}
    @Override protected void actionPerformed(GuiButton button){if(button.id==1){mc.displayGuiScreen(parent);return;}if(!valid())return;try{apply.accept(input.getText());mc.displayGuiScreen(parent);}catch(RuntimeException ex){error=ex.getMessage()==null?"Invalid value":ex.getMessage();}}
    @Override protected void mouseClicked(int x,int y,int button){super.mouseClicked(x,y,button);input.mouseClicked(x,y,button);}
    @Override protected void keyTyped(char ch,int key){if(key==Keyboard.KEY_ESCAPE)mc.displayGuiScreen(parent);else if(key==Keyboard.KEY_RETURN)actionPerformed((GuiButton)buttonList.get(1));else input.textboxKeyTyped(ch,key);}
    @Override public void updateScreen(){input.updateCursorCounter();}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
