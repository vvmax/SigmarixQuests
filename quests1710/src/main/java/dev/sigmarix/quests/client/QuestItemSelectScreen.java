package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;
/** Native FTBLib item picker geometry, with searchable variants and editable stack data. */
final class QuestItemSelectScreen extends GuiScreen {
    private static boolean allItems=true;
    private final GuiScreen parent;private final boolean single;private final Predicate<ItemStack> filter;private final Consumer<ItemStack> accept;
    private final List<ItemStack> all=new ArrayList<ItemStack>(),visible=new ArrayList<ItemStack>();private ItemStack selected;private GuiTextField search;private int left,top,scroll;private boolean dragging;
    QuestItemSelectScreen(GuiScreen parent,ItemStack selected,boolean single,Predicate<ItemStack> filter,Consumer<ItemStack> accept){this.parent=parent;this.selected=selected==null?null:selected.copy();this.single=single;this.filter=filter;this.accept=accept;}
    @Override public void initGui(){
        String text=search==null?"":search.getText();left=(width-211)/2;top=(height-150)/2;search=new QuestLibraryTextField(fontRendererObj,left+12,top+9,187,8).clip(left+8,top+7,195,12);search.setMaxStringLength(128);search.setText(text);search.setFocused(true);Keyboard.enableRepeatEvents(true);
        buttonList.clear();buttonList.add(new QuestLibraryUi.Button(0,left+8,top+126,95,16,QuestText.tr("Cancel"),""));buttonList.add(new QuestLibraryUi.Button(1,left+108,top+126,95,16,QuestText.tr("Accept"),""));collect();
    }
    private void collect(){
        all.clear();if(allItems){for(Object raw:Item.itemRegistry){Item item=(Item)raw;List<ItemStack> variants=new ArrayList<ItemStack>();try{item.getSubItems(item,item.getCreativeTab(),variants);}catch(RuntimeException ignored){}for(ItemStack stack:variants)if(stack!=null && filter.test(stack))all.add(stack.copy());}}
        else if(mc.thePlayer!=null)for(ItemStack stack:mc.thePlayer.inventory.mainInventory)if(stack!=null && filter.test(stack))all.add(stack.copy());
        refresh();
    }
    private void refresh(){String text=search.getText().toLowerCase(Locale.ROOT);visible.clear();visible.add(null);for(ItemStack stack:all){String id=(String)Item.itemRegistry.getNameForObject(stack.getItem());if(text.isEmpty() || text.startsWith("@") && id.split(":",2)[0].contains(text.substring(1)) || !text.startsWith("@") && stack.getDisplayName().toLowerCase(Locale.ROOT).contains(text))visible.add(stack);}scroll=Math.max(0,Math.min(scroll,Math.max(0,(visible.size()+8)/9*19+1-96)));}
    private void stack(ItemStack stack,int x,int y,int size){if(stack!=null)QuestClassicUi.inventoryItem(mc,(String)Item.itemRegistry.getNameForObject(stack.getItem()),stack.getItemDamage(),stack.hasTagCompound()?stack.getTagCompound().toString():"",x,y,size);}
    @Override public void drawScreen(int mx,int my,float partial){
        drawDefaultBackground();QuestLibraryUi.window(left,top,211,150);QuestLibraryUi.textBox(left+8,top+7,195,12);search.drawTextBox();if(search.getText().isEmpty() && !search.isFocused())QuestLibraryUi.ghost(net.minecraft.util.StatCollector.translateToLocal("gui.search_box"),left+12,top+9);
        QuestLibraryUi.slot(left+9,top+24,172,96);QuestLibraryUi.patch("container_beacon",35,136,18,18,3,left+187,top+23,16,98);int max=Math.max(0,(visible.size()+8)/9*19+1-96);if(max>0){int h=Math.max(10,96*98/(max+96)),y=top+23+scroll*(98-h)/max;QuestLibraryUi.patch("container_beacon",0,219,22,22,6,left+188,y+1,14,h-2);}
        ScaledResolution resolution=new ScaledResolution(mc,mc.displayWidth,mc.displayHeight);int scale=resolution.getScaleFactor();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((left+9)*scale,(resolution.getScaledHeight()-top-120)*scale,172*scale,96*scale);ItemStack hover=null;
        try{for(int i=0;i<visible.size();i++){int x=left+10+i%9*19,y=top+25+i/9*19-scroll;if(y+18<=top+24 || y>=top+120)continue;ItemStack item=visible.get(i);boolean over=mx>=x && mx<x+18 && my>=y && my<y+18,same=item==null?selected==null:selected!=null && item.isItemEqual(selected) && ItemStack.areItemStackTagsEqual(item,selected);drawRect(x,y,x+18,y+18,over || same?0x4656FF56:0x32000000);stack(item,x+1,y+1,16);if(over)hover=item;}}
        finally{GL11.glPopAttrib();}
        String[] icons={allItems?"minecraft:compass":"minecraft:chest","","minecraft:paper","minecraft:stonebrick","minecraft:name_tag","minecraft:anvil","minecraft:sign"};
        for(int i=0;i<7;i++){int y=top+8+i*20;boolean enabled=i<2 || selected!=null;if(i==2 && single)enabled=false;QuestLibraryUi.patch("widgets",0,!enabled?46:mx>=left-19 && mx<left+1 && my>=y && my<y+20?86:66,200,20,4,left-19,y,20,20);if(i==1)stack(selected,left-17,y+2,16);else QuestClassicUi.item(mc,icons[i],i==3?2:0,"",left-17,y+2,16);}
        super.drawScreen(mx,my,partial);if(hover!=null && my>=top+24 && my<top+120)drawHoveringText(hover.getTooltip(mc.thePlayer,mc.gameSettings.advancedItemTooltips),mx,my,fontRendererObj);
    }
    @Override protected void actionPerformed(GuiButton button){mc.displayGuiScreen(parent);if(button.id==1){ItemStack value=selected==null?null:selected.copy();if(value!=null && single)value.stackSize=1;accept.accept(value);}}
    private void edit(int tab){
        if(tab==0){allItems=!allItems;collect();return;}if(tab>1 && selected==null || tab==2 && single)return;
        final ItemStack previous=selected==null?null:selected.copy();String value="";int color=0xFFAA49;
        if(tab==1 && selected!=null){NBTTagCompound tag=new NBTTagCompound();selected.writeToNBT(tag);value=tag.toString();}else if(tab==2){value=Integer.toString(selected.stackSize);color=0xAA5AE8;}else if(tab==3){value=Integer.toString(selected.getItemDamage());color=0xAA5AE8;}else if(tab==4)value=selected.hasTagCompound()?selected.getTagCompound().toString():"{}";else if(tab==5)value="{}";else if(tab==6)value=selected.hasDisplayName()?selected.getDisplayName():"";
        mc.displayGuiScreen(new QuestValueScreen(this,value,color,32768,text->{
            if(tab==1){ItemStack next=text.isEmpty()?null:ItemStack.loadItemStackFromNBT(QuestService.tag(text));if(next!=null && !filter.test(next))throw new IllegalArgumentException("Item does not match this field");selected=next;}
            else if(tab==2){int count=Integer.parseInt(text);if(count<1 || count>previous.getMaxStackSize())throw new IllegalArgumentException("Count exceeds stack limit");previous.stackSize=count;selected=previous;}
            else if(tab==3){int meta=Integer.parseInt(text);if(meta<0 || meta>32767)throw new IllegalArgumentException("Invalid item metadata");previous.setItemDamage(meta);if(!filter.test(previous))throw new IllegalArgumentException("Item does not match this field");selected=previous;}
            else if(tab==4){previous.setTagCompound(QuestService.tag(text));selected=previous;}
            else if(tab==5){NBTTagCompound caps=QuestService.tag(text);if(caps!=null && !caps.hasNoTags())throw new IllegalArgumentException("Forge 1.7.10 has no item capability system");}
            else if(tab==6){if(text.isEmpty())previous.func_135074_t();else previous.setStackDisplayName(text);selected=previous;}
        }));
    }
    @Override protected void mouseClicked(int x,int y,int button){search.mouseClicked(x,y,button);if(x>=left-19 && x<left+1 && y>=top+8 && y<top+148){edit((y-top-8)/20);return;}if(x>=left+187 && x<left+203 && y>=top+23 && y<top+121){dragging=button==0;drag(y);return;}
        if(x>=left+9 && x<left+181 && y>=top+24 && y<top+120){int px=x-left-10,py=y-top-25+scroll;if(px>=0 && py>=0 && px%19<18 && py%19<18){int index=py/19*9+px/19;if(index<visible.size())selected=visible.get(index)==null?null:visible.get(index).copy();}return;}super.mouseClicked(x,y,button);
    }
    private void drag(int y){int max=Math.max(0,(visible.size()+8)/9*19+1-96);scroll=Math.max(0,Math.min(max,(y-top-23)*max/98));}
    @Override protected void mouseClickMove(int x,int y,int button,long time){if(dragging)drag(y);}
    @Override protected void mouseMovedOrUp(int x,int y,int button){super.mouseMovedOrUp(x,y,button);if(button==0)dragging=false;}
    @Override public void handleMouseInput(){super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0){scroll+=wheel>0?-20:20;refresh();}}
    @Override protected void keyTyped(char ch,int key){if(key==1){mc.displayGuiScreen(parent);return;}if(search.textboxKeyTyped(ch,key)){scroll=0;refresh();return;}super.keyTyped(ch,key);}
    @Override public void updateScreen(){search.updateCursorCounter();}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
