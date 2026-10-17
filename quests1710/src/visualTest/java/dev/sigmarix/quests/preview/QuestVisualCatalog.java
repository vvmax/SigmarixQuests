package dev.sigmarix.quests.preview;
import dev.sigmarix.quests.*;
import dev.sigmarix.quests.client.*;
import net.minecraft.client.gui.GuiScreen;
/** Render-only inventory catalog; never included in production. */
final class QuestVisualCatalog extends GuiScreen {
    @Override public void drawScreen(int mx,int my,float partial){
        QuestClassicUi.background(mc,width,height);drawCenteredString(fontRendererObj,"Reference item and block textures",width/2,10,0xFFFFFF);
        String[] ids={"quest_book","custom_icon","loot_crate","task_screen","progress_screen","quest_barrier","reward_collector","redstone_detector","player_detector","loot_crate_opener","quest_chest","loot_crate_storage","progress_detector"};
        for(int i=0;i<ids.length;i++){
            int x=14+(i%7)*Math.max(42,(width-28)/7),y=35+(i/7)*60;
            String nbt=ids[i].equals("loot_crate")?"{color:16755200}":"";
            QuestClassicUi.item(mc,"sigmarixquests:"+ids[i],0,nbt,x,y,32);
            String name=ids[i].replace("loot_crate","crate").replace("progress","prog.").replace("redstone","red.").replace("reward","rew.").replace("player","play.").replace("_"," ");
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(name,Math.max(38,(width-28)/7-2)),x,y+36,0xFFFFFF);
        }
        String[] shapes={"square","rsquare","circle","diamond","pentagon","hexagon","octagon","heart","gear"};
        for(int i=0;i<shapes.length;i++)QuestNodeRenderer.node(mc,20+i*(width-40)/shapes.length,height-28,12,shapes[i],0xC856FF56);
    }
    @Override public boolean doesGuiPauseGame(){return false;}
}
