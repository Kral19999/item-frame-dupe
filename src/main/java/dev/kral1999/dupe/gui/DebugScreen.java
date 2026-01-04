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
        super(Text.of("Debug Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = 50;

        addDrawableChild(new SliderWidget(x, y, 200, 20,
                Text.literal("Max Placements: " + Config.INSTANCE.maxPlacements),
                (Config.INSTANCE.maxPlacements - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Max Placements: " + Config.INSTANCE.maxPlacements));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxPlacements = 1 + (int) (this.value * 19);
                Config.save();
            }
        });

        addDrawableChild(new SliderWidget(x, y + 24, 200, 20,
                Text.literal("Max Swaps: " + Config.INSTANCE.maxSwaps),
                (Config.INSTANCE.maxSwaps - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Max Swaps: " + Config.INSTANCE.maxSwaps));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxSwaps = 1 + (int) (this.value * 19);
                Config.save();
            }
        });

        addDrawableChild(new SliderWidget(x, y + 48, 200, 20,
                Text.literal("Max Moves: " + Config.INSTANCE.maxInventoryMoves),
                (Config.INSTANCE.maxInventoryMoves - 1) / 19.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Max Moves: " + Config.INSTANCE.maxInventoryMoves));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxInventoryMoves = 1 + (int) (this.value * 19);
                Config.save();
            }
        });

        addDrawableChild(new SliderWidget(x, y + 72, 200, 20,
                Text.literal("Max Frames: " + Config.INSTANCE.maxFrames),
                (Config.INSTANCE.maxFrames - 1) / 7.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.literal("Max Frames: " + Config.INSTANCE.maxFrames));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.maxFrames = 1 + (int) (this.value * 7);
                Config.save();
            }
        });

        addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> this.close())
                .dimensions(x, this.height - 50, 200, 20)
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
