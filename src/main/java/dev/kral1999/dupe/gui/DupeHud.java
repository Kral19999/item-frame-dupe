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
        if (!Config.INSTANCE.enabled)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        int dps = DuperManager.getDps();

        int color = 0x00FF00;

        int y = 4;
        context.drawTextWithShadow(mc.textRenderer, "DPS: " + dps, 4, y, color);
    }
}
