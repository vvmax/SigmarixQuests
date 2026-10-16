package dev.sigmarix.quests.client;
import java.util.Locale;
import net.minecraft.util.StatCollector;
public final class QuestText {
    public static String tr(String text) {
        String key="quests.ui."+text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
        return StatCollector.canTranslate(key)?StatCollector.translateToLocal(key):text;
    }
}
