package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.client.gui.GuiScreen;
/** Independent client extension point for custom:namespace/path and other click protocols. */
public abstract class QuestClickEvent extends Event {
    public final String quest,type,value;
    protected QuestClickEvent(String quest,String type,String value) { this.quest=quest; this.type=type; this.value=value; }
    public static final class Check extends QuestClickEvent {
        public boolean available;
        public Check() { this("","",""); }
        public Check(String quest,String type,String value) { super(quest,type,value); }
    }
    public static final class Open extends QuestClickEvent {
        public final GuiScreen parent; public boolean opened;
        public Open() { this("","","",null); }
        public Open(String quest,String type,String value,GuiScreen parent) { super(quest,type,value); this.parent=parent; }
    }
}
