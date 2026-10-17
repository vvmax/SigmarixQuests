package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.item.*;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.*;
/** The classic four-column valid-items view. Submission still uses only a server task ID. */
public final class QuestItemTaskScreen extends QuestLibraryScreen {
    private final GuiScreen parent; private final String questId,taskId; private final long revision;
    private final List<ItemStack> examples=new ArrayList<ItemStack>();
    private QuestCompactGrid grid; private final QuestCore.Task task; private int x,y,w,h,scroll,gridX,gridHeight,ticks;
    public QuestItemTaskScreen(GuiScreen parent,QuestCore.Quest quest,QuestCore.Task task) {
        this.parent=parent; questId=quest.id; taskId=task.id; this.task=task; revision=ClientProxy.snapshot.revision;
        add(task.target,task.meta,task.nbt); for(String target:task.targets) add(target,task.meta,task.nbt); filter(task.filter,0);
    }
    private void filter(QuestItemFilter filter,int depth) { if(filter==null || depth>16 || examples.size()>=256) return; if(filter.type.equals("item")) add(filter.value,filter.meta,filter.nbt); else if(filter.type.equals("ore")) add("ore:"+filter.value,0,""); for(QuestItemFilter child:filter.children) filter(child,depth+1); }
    private void add(String id,int meta,String nbt) {
        if(id==null || id.isEmpty() || examples.size()>=256) return;
        if(id.startsWith("ore:")) { for(ItemStack stack:OreDictionary.getOres(id.substring(4))) if(examples.size()<256) append(stack.copy()); return; }
        Item item=(Item)Item.itemRegistry.getObject(id); if(item==null) return;
        ItemStack stack=new ItemStack(item,1,meta<0 || meta==OreDictionary.WILDCARD_VALUE?0:meta); try { stack.setTagCompound(QuestService.tag(nbt)); } catch(RuntimeException invalid) {} append(stack);
    }
    private void append(ItemStack stack) { stack.stackSize=1; for(ItemStack existing:examples) if(existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing,stack)) return; examples.add(stack); }
    @Override public void initGui() {
        grid=new QuestCompactGrid(examples.size(),36); String title=net.minecraft.util.StatCollector.translateToLocalFormatted("ftbquests.task.ftbquests.item.valid_for",QuestTaskLabels.title(task)); w=Math.max(156,Math.min(width-8,fontRendererObj.getStringWidth(title)+12)); gridHeight=Math.min(160,Math.max(1,grid.height)); h=gridHeight+53; x=(width-w)/2; y=(height-h)/2; gridX=x+(w-144)/2;
        buttonList.clear(); buttonList.add(new QuestFlatButton(0,gridX-1,y+gridHeight+28,70,20,QuestText.tr("Back")));
        GuiButton submit=new QuestFlatButton(1,gridX+75,y+gridHeight+28,70,20,QuestText.tr("Submit"));
        submit.enabled=task.consume && !task.craftingOnly && !Boolean.TRUE.equals(ClientProxy.snapshot.taskStates.get(questId+":"+taskId)) && "available".equals(ClientProxy.snapshot.states.get(questId)); buttonList.add(submit);
    }
    @Override protected void actionPerformed(GuiButton button) { if(button.id==1 && ClientProxy.snapshot.revision==revision) { QuestNetwork.Action action=new QuestNetwork.Action(); action.action="click_task"; action.id=questId; action.text=taskId; ClientProxy.send(action); } mc.displayGuiScreen(parent); }
    @Override public void drawScreen(int mx,int my,float partial) {
        drawDefaultBackground();QuestClassicUi.background(mc,x,y,w,h); String title=fontRendererObj.trimStringToWidth(net.minecraft.util.StatCollector.translateToLocalFormatted("ftbquests.task.ftbquests.item.valid_for",QuestTaskLabels.title(task)),w-12); fontRendererObj.drawString(title,x+w/2-fontRendererObj.getStringWidth(title)/2,y+6,0xFFFFFFFF);
        QuestClassicUi.frame(gridX-1,y+21,146,gridHeight+2,QuestClassicUi.BORDER);
        net.minecraft.client.gui.ScaledResolution resolution=new net.minecraft.client.gui.ScaledResolution(mc,mc.displayWidth,mc.displayHeight); int f=resolution.getScaleFactor(); org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST); org.lwjgl.opengl.GL11.glScissor(gridX*f,(resolution.getScaledHeight()-y-22-gridHeight)*f,144*f,gridHeight*f);
        ItemStack hover=null;
        for(int i=0;i<examples.size();i++) { int ix=gridX+(144-grid.width)/2+grid.x(i),iy=y+22+grid.y(i)-scroll*36; if(iy+36<y+22 || iy>y+22+gridHeight) continue; ItemStack stack=examples.get(i); if(mx>=ix && mx<ix+36 && my>=iy && my<iy+36) { drawRect(ix,iy,ix+36,iy+36,0x21FFFFFF); hover=stack; } QuestClassicUi.item(mc,Item.itemRegistry.getNameForObject(stack.getItem()),stack.getItemDamage(),stack.hasTagCompound()?stack.getTagCompound().toString():"",ix+2,iy+2,32); }
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST); super.drawScreen(mx,my,partial);
        if(hover!=null && my>=y+22 && my<y+22+gridHeight) drawHoveringText(hover.getTooltip(mc.thePlayer,mc.gameSettings.advancedItemTooltips),mx,my,fontRendererObj);
    }
    @Override public void handleMouseInput() { super.handleMouseInput(); int wheel=Mouse.getEventDWheel(); if(wheel!=0) scroll=Math.max(0,Math.min(Math.max(0,grid.height/36-gridHeight/36),scroll+(wheel>0?-1:1))); }
    @Override protected void keyTyped(char c,int key) { if(key==Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent); else super.keyTyped(c,key); }
    @Override public void updateScreen() { if(ClientProxy.snapshot.revision!=revision) { mc.displayGuiScreen(parent); return; } if(!task.consume && ++ticks%40==0 && "available".equals(ClientProxy.snapshot.states.get(questId))) { QuestNetwork.Action action=new QuestNetwork.Action(); action.action="click_task"; action.id=questId; action.text=taskId; ClientProxy.send(action); } }
    @Override public boolean doesGuiPauseGame() { return false; }
}
