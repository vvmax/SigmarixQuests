package dev.sigmarix.quests.client;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.*;
/** FTBLib fluid list uses its standard window, searchable 20px rows and fluid sprites. */
final class QuestFluidSelectScreen extends QuestListScreen {
    private final boolean nullable;private final Consumer<Fluid> accept;private final List<Fluid> fluids=new ArrayList<Fluid>();
    QuestFluidSelectScreen(GuiScreen parent,boolean nullable,Consumer<Fluid> accept){super(parent,true,false,0,0,0);this.nullable=nullable;this.accept=accept;fluids.addAll(FluidRegistry.getRegisteredFluids().values());}
    @Override protected String title(){return StatCollector.translateToLocal("ftblib.select_fluid.gui");}
    @Override protected Object revision(){return fluids;}
    @Override protected List<Row> rows(){List<Row> rows=new ArrayList<Row>();if(nullable)rows.add(new Row(-1,StatCollector.translateToLocal("ftblib.select_fluid.none"),"barrier"));for(int i=0;i<fluids.size();i++){Fluid fluid=fluids.get(i);Row row=new Row(i,fluid.getLocalizedName(new FluidStack(fluid,1000)),"fluid");row.customIcon=true;rows.add(row);}return rows;}
    @Override protected void drawRowIcon(Row row,int x,int y,int size){if(row.id<0){QuestClassicUi.libraryIcon("barrier",x,y,size,0xFFFFFFFF);return;}Fluid fluid=fluids.get(row.id);QuestClassicUi.sprite(mc,fluid.getStillIcon(),x,y,size,fluid.getColor(new FluidStack(fluid,1000)));}
    @Override protected void click(Row row,int button){mc.displayGuiScreen(parent);accept.accept(row.id<0?null:fluids.get(row.id));}
}
