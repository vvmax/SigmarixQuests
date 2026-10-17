package dev.sigmarix.quests.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;

/** FTBLib panels use the classic font independently of the current locale's Unicode flag. */
abstract class QuestLibraryScreen extends GuiScreen {
    private static FontRenderer libraryFont;
    static FontRenderer font(Minecraft mc) {
        if (libraryFont == null) {
            libraryFont = new PanelFont(mc);
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(libraryFont);
        }
        libraryFont.setBidiFlag(mc.getLanguageManager().isCurrentLanguageBidirectional());
        return libraryFont;
    }
    /** 1.12 offsets Unicode shadows by half a pixel even in a classic-font panel. */
    private static final class PanelFont extends FontRenderer {
        private boolean shadowPass;
        private int remaining;
        PanelFont(Minecraft mc) { super(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.getTextureManager(), false); }
        @Override public int drawString(String text, int x, int y, int color, boolean shadow) {
            remaining = 0;
            if (text != null) for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\u00a7' && i + 1 < text.length()) i++;
                else remaining++;
            }
            shadowPass = shadow && remaining > 0;
            try { return super.drawString(text, x, y, color, shadow); }
            finally { shadowPass = false; }
        }
        @Override protected float renderUnicodeChar(char character, boolean italic) {
            boolean offset = shadowPass && !getUnicodeFlag();
            if (offset) { posX += .5F; posY += .5F; }
            try { return super.renderUnicodeChar(character, italic); }
            finally { if (offset) { posX -= .5F; posY -= .5F; } }
        }
        @Override protected void doDraw(float advance) {
            super.doDraw(advance);
            if (shadowPass && --remaining == 0) shadowPass = false;
        }
    }
    @Override public void setWorldAndResolution(Minecraft minecraft, int width, int height) {
        this.mc = minecraft;
        this.fontRendererObj = font(minecraft);
        this.width = width;
        this.height = height;
        if (!MinecraftForge.EVENT_BUS.post(new InitGuiEvent.Pre(this, this.buttonList))) {
            this.buttonList.clear();
            this.initGui();
        }
        MinecraftForge.EVENT_BUS.post(new InitGuiEvent.Post(this, this.buttonList));
    }
    // Native GuiWrapper draws tooltips after restoring the locale font.
    @Override protected void drawHoveringText(List lines, int x, int y, FontRenderer font) {
        super.drawHoveringText(lines, x, y, font == fontRendererObj ? mc.fontRenderer : font);
    }
}
