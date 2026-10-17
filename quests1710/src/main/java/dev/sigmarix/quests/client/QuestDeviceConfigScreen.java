package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.util.StatCollector;
/** Original device properties, backed by one server-validated transaction. */
final class QuestDeviceConfigScreen extends QuestConfigScreen {
    private final QuestDeviceScreen advanced;private final QuestDevice tile;private final QuestDeviceConfig draft;private final int mode;private boolean ready;
    QuestDeviceConfigScreen(QuestDeviceScreen advanced,QuestDevice tile){super(null);this.advanced=advanced;this.tile=tile;draft=QuestDeviceConfig.read(tile);mode=QuestScreenSettings.mode(tile);}
    private String type(){return mode==0?"screen":mode==1?"progress_screen":mode==2?"barrier":mode==3?"reward_collector":mode==4?"detector.redstone":mode==5?"detector.player":"progress_detector";}
    private String tr(String key){return StatCollector.translateToLocal(key);}
    @Override protected String title(){return tr("tile.ftbquests."+(mode==1?"progress_screen":type())+".name");}
    private Entry bool(String label,java.util.function.BooleanSupplier get,java.util.function.Consumer<Boolean> set){return new Entry("FTB Quests",label,()->Boolean.toString(get.getAsBoolean()),()->get.getAsBoolean()?0x33AA33:0xD52834,(button,gui)->set.accept(!get.getAsBoolean()));}
    private Entry number(String key,java.util.function.DoubleSupplier get,java.util.function.DoubleConsumer set){return new Entry("FTB Quests",tr("tile.ftbquests."+type()+"."+key),()->Double.toString(get.getAsDouble()),()->0xAA5AE8,(button,gui)->mc.displayGuiScreen(new QuestValueScreen(this,Double.toString(get.getAsDouble()),0xAA5AE8,32,text->set.accept(Double.parseDouble(text)))));}
    private String objectTitle(){if(draft.quest.isEmpty())return tr("ftbquests.null");for(QuestCore.Quest quest:ClientProxy.snapshot.book.quests)if(quest.id.equals(draft.quest)){if(mode==3)for(QuestCore.Reward r:quest.rewards)if(r.id.equals(draft.task))return "\u00a7d"+QuestTaskLabels.reward(r);for(QuestCore.Task task:quest.tasks)if(task.id.equals(draft.task))return "\u00a79"+QuestTaskLabels.title(task);return "\u00a7a"+quest.title;}for(QuestCore.Chapter chapter:ClientProxy.snapshot.book.chapters)if(("chapter:"+chapter.id).equals(draft.quest))return "\u00a76"+chapter.title;return draft.quest;}
    @Override protected List<Entry> entries(){
        List<Entry> entries=new ArrayList<Entry>();boolean editor=ClientProxy.snapshot!=null && ClientProxy.snapshot.canEdit;
        if(editor && (mode<=1 || mode==4 || mode==9))entries.add(new Entry("FTB Quests",tr("ftbquests.team"),()->{for(QuestNetwork.TeamEntry team:ClientProxy.snapshot.teams)if(team.owner.equals(draft.owner))return team.name;return ClientProxy.snapshot.team.isEmpty()?mc.thePlayer.getCommandSenderName():ClientProxy.snapshot.team;},()->0x0094FF,(button,gui)->teams()));
        int types=mode==1?QuestSelectObjectScreen.CHAPTER:mode==0 || mode==4 || mode==5?QuestSelectObjectScreen.TASK:mode==3?QuestSelectObjectScreen.REWARD:QuestSelectObjectScreen.FILE|QuestSelectObjectScreen.CHAPTER|QuestSelectObjectScreen.QUEST|QuestSelectObjectScreen.TASK;
        String label=tr(mode==0?"ftbquests.task":mode==3?"ftbquests.reward":mode==1?"ftbquests.chapter":mode==4 || mode==5?"tile.ftbquests.detector.task":"tile.ftbquests."+type()+".object");
        entries.add(new Entry("FTB Quests",label,this::objectTitle,()->0x0094FF,(button,gui)->mc.displayGuiScreen(new QuestSelectObjectScreen(this,types,true,s->mode==0?s.task!=null && (s.task.consume || Arrays.asList("fluid","energy","ic2_energy","botania_mana","buildcraft_mj").contains(s.task.type)):mode==4 || mode==5?s.task!=null && s.task.type.equals("custom"):mode!=3 || s.reward!=null && s.quest.repeatable && s.reward.type.equals("item"),s->{draft.quest=s.quest!=null?s.quest.id:s.id;draft.task=s.task!=null?s.task.id:s.reward!=null?s.reward.id:"";}))));
        if(mode<=1){
            entries.add(new Entry("FTB Quests",tr("tile.ftbquests.screen.skin"),()->draft.skin.isEmpty()?tr("ftbquests.null"):draft.skin,()->0x33AA33,(button,gui)->{Item item=draft.skin.isEmpty()?null:Item.getItemFromBlock((Block)Block.blockRegistry.getObject(draft.skin));ItemStack current=item==null?null:new ItemStack(item,1,draft.skinMeta);mc.displayGuiScreen(new QuestItemSelectScreen(this,current,true,stack->stack.getItem() instanceof ItemBlock && !Block.getBlockFromItem(stack.getItem()).hasTileEntity(stack.getItemDamage()),stack->{draft.skin=stack==null?"":(String)Block.blockRegistry.getNameForObject(Block.getBlockFromItem(stack.getItem()));draft.skinMeta=stack==null?0:stack.getItemDamage();}));}));
            if(mode==1){entries.add(bool(tr("tile.ftbquests.screen.fullscreen"),()->draft.fullscreen,v->draft.fullscreen=v));entries.add(bool(tr("tile.ftbquests.screen.hide_icons"),()->draft.hideIcons,v->draft.hideIcons=v));}
            if(editor)entries.add(bool(tr("tile.ftbquests.screen.indestructible"),()->draft.indestructible,v->draft.indestructible=v));
            if(mode==0){entries.add(bool(tr("tile.ftbquests.screen.input_only"),()->draft.inputOnly,v->draft.inputOnly=v));entries.add(new Entry("FTB Quests",tr("tile.ftbquests.screen.input_mode_icon"),()->draft.inputItem.isEmpty()?tr("ftbquests.null"):draft.inputItem,()->0x33AA33,(button,gui)->{Item item=(Item)Item.itemRegistry.getObject(draft.inputItem);ItemStack stack=item==null?null:new ItemStack(item,1,draft.inputMeta);if(stack!=null)stack.setTagCompound(QuestService.tag(draft.inputNbt));mc.displayGuiScreen(new QuestItemSelectScreen(this,stack,true,s->true,s->{draft.inputItem=s==null?"":(String)Item.itemRegistry.getNameForObject(s.getItem());draft.inputMeta=s==null?0:s.getItemDamage();draft.inputNbt=s!=null && s.hasTagCompound()?s.getTagCompound().toString():"";}));}));}
        }
        if(mode==4 || mode==5){entries.add(bool(tr("tile.ftbquests.detector.notifications"),()->draft.notifications,v->draft.notifications=v));if(mode==5){entries.add(number("radius",()->draft.radius,v->draft.radius=v));entries.add(number("offset_y",()->draft.offset,v->draft.offset=v));entries.add(number("height",()->draft.height,v->draft.height=v));}}
        if(mode==9)entries.add(bool(tr("tile.ftbquests.progress_detector.level"),()->draft.analog,v->draft.analog=v));
        return entries;
    }
    private void teams(){mc.displayGuiScreen(new QuestListScreen(this,true,false,2,2,0){protected String title(){return tr("ftbquests.team");}protected List<Row> rows(){List<Row> result=new ArrayList<Row>();int i=0;for(QuestNetwork.TeamEntry team:ClientProxy.snapshot.teams){Row row=new Row(i++,team.title.isEmpty()?team.name:team.title,"");row.playerId=team.owner;row.teamHead=true;row.border=0xFF000000|team.color;result.add(row);}return result;}protected void click(Row row,int button){draft.owner=ClientProxy.snapshot.teams.get(row.id).owner;mc.displayGuiScreen(QuestDeviceConfigScreen.this);}});}
    @Override public void drawScreen(int x,int y,float partial){if(ClientProxy.snapshot==null){drawDefaultBackground();drawCenteredString(fontRendererObj,tr("menu.loadingLevel"),width/2,height/2,0xFFFFFF);return;}super.drawScreen(x,y,partial);}
    @Override public void updateScreen(){if(!ready && ClientProxy.snapshot!=null){ready=true;initGui();}super.updateScreen();}
    @Override protected void save(){QuestNetwork.Action action=new QuestNetwork.Action();action.action="device_config";action.x=tile.xCoord;action.y=tile.yCoord;action.z=tile.zCoord;action.deviceSettings=draft;ClientProxy.send(action);}
    @Override protected void closed(boolean applied){mc.thePlayer.closeScreen();}
    @Override protected void keyTyped(char ch,int key){if(isCtrlKeyDown() && key==org.lwjgl.input.Keyboard.KEY_I){advanced.showAdvanced();mc.displayGuiScreen(advanced);return;}super.keyTyped(ch,key);}
}
