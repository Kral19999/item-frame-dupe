package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class DebugScreen extends Screen {

    private final Screen parent;

    public DebugScreen(Screen parent) {
        super(Text.translatable("dupe.debug.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 155;
        int width = 310;
        int y = 40;

        addDrawableChild(new SliderWidget(x, y, width, 20,
                Text.translatable("dupe.debug.max_frames", Config.INSTANCE.maxFrames),
                (Config.INSTANCE.maxFrames - 1) / 31.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.debug.max_frames", Config.INSTANCE.maxFrames));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxFrames = 1 + (int) Math.round(this.value * 31);
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(new SliderWidget(x, y, width, 20,
                Text.translatable("dupe.debug.max_placements", Config.INSTANCE.maxPlacements),
                (Config.INSTANCE.maxPlacements - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.debug.max_placements", Config.INSTANCE.maxPlacements));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxPlacements = 1 + (int) Math.round(this.value * 19);
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(new SliderWidget(x, y, width, 20,
                Text.translatable("dupe.debug.max_swaps", Config.INSTANCE.maxSwaps),
                (Config.INSTANCE.maxSwaps - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.debug.max_swaps", Config.INSTANCE.maxSwaps));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxSwaps = 1 + (int) Math.round(this.value * 19);
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(new SliderWidget(x, y, width, 20,
                Text.translatable("dupe.debug.max_moves", Config.INSTANCE.maxInventoryMoves),
                (Config.INSTANCE.maxInventoryMoves - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.debug.max_moves", Config.INSTANCE.maxInventoryMoves));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxInventoryMoves = 1 + (int) Math.round(this.value * 19);
                Config.save();
            }
        });

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.done"), button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
