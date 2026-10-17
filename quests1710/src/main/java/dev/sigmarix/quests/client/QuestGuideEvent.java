package dev.sigmarix.quests.client;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.*;
import net.minecraft.client.gui.GuiScreen;
/** Independent client-side guide discovery/open events for optional 1.7.10 guide mods. */
@SideOnly(Side.CLIENT)
public abstract class QuestGuideEvent extends Event {
    public final String path;
    private QuestGuideEvent(String path) { this.path=path; }
    public static final class Check extends QuestGuideEvent {
        public boolean available;
        public Check() { this(""); }
        public Check(String path) { super(path); }
    }
    public static final class Open extends QuestGuideEvent {
        public final GuiScreen parent;
        public boolean opened;
        public Open() { this("",null); }
        public Open(String path,GuiScreen parent) { super(path); this.parent=parent; }
    }
}
