package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

public class UpdateScreen extends Screen {

    private final Screen parent;
    private final String version;

    public UpdateScreen(Screen parent, String version) {
        super(Text.translatable("dupe.update.title"));
        this.parent = parent;
        this.version = version;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.update.yes"), button -> {
            Util.getOperatingSystem()
                    .open(URI.create("https://github.com/Kral19999/item-frame-dupe/releases/tag/" + version));
        }).dimensions(centerX - buttonWidth / 2, centerY - 20, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.update.no"), button -> {
            this.client.setScreen(this.parent);
        }).dimensions(centerX - buttonWidth / 2, centerY + 5, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.update.ignore"), button -> {
            Config.INSTANCE.ignoreVersion = version;
            Config.save();
            this.client.setScreen(this.parent);
        }).dimensions(centerX - buttonWidth / 2, centerY + 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("dupe.update.available", version),
                this.width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
