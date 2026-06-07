package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import dev.kral1999.dupe.DuperManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class DupeHud implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!Config.INSTANCE.enabled || !Config.INSTANCE.showHud)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null)
            return;

        int dps = DuperManager.getDps();
        String text = "DPS: " + dps;
        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int drawX = 4 + (int) (Config.INSTANCE.hudX * (width - textWidth - 8));
        int drawY = 4 + (int) (Config.INSTANCE.hudY * (height - textHeight - 8));

        int color = 0x00FF00;
        context.drawTextWithShadow(mc.textRenderer, text, drawX, drawY, color);
    }
}
