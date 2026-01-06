package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import dev.kral1999.dupe.DuperManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.InputUtil;

import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DuperControls {

    private final Screen screen;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> children = new ArrayList<>();
    private final List<net.minecraft.client.gui.Drawable> drawables = new ArrayList<>();

    private ButtonWidget keybindButton;
    private CyclingButtonWidget<Boolean> replaceFramesButton;
    private boolean listeningForKey = false;

    public DuperControls(Screen screen) {
        this.screen = screen;
    }

    public void init(int inventoryX, int inventoryY) {
        children.clear();
        drawables.clear();

        int x = inventoryX - 105;
        int y = inventoryY + 5;
        int width = 100;
        int height = 20;

        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text
                        .translatable((Boolean) value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.enabled)
                .build(x, y, width, height, Text.translatable("dupe.controls.status"), (button, value) -> {
                    Config.INSTANCE.enabled = (Boolean) value;
                    Config.save();
                }));
        y += 24;

        addDrawableChild(new CyclingButtonWidget.Builder<Config.Mode>(value -> Text.of(((Config.Mode) value).name()))
                .values(Config.Mode.Normal, Config.Mode.Speed)
                .initially(Config.INSTANCE.mode)
                .build(x, y, width, height, Text.translatable("dupe.controls.mode"), (button, value) -> {
                    Config.INSTANCE.mode = (Config.Mode) value;
                    updateReplaceFramesVisibility();
                    Config.save();
                    if (Config.INSTANCE.enabled) {
                        DuperManager.reloadFrames();
                    }
                }));
        y += 24;

        replaceFramesButton = CyclingButtonWidget
                .<Boolean>builder(
                        value -> Text.translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.replaceItemFrames)
                .build(x, y, width, height, Text.translatable("dupe.controls.replace"), (button, value) -> {
                    Config.INSTANCE.replaceItemFrames = value;
                    Config.save();
                });
        addDrawableChild(replaceFramesButton);
        updateReplaceFramesVisibility();
        y += 24;

        addDrawableChild(new SliderWidget(x, y, width, height,
                Text.translatable("dupe.controls.range", String.format("%.1f", Config.INSTANCE.range)),
                (Config.INSTANCE.range - 2.0) / 3.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.controls.range", String.format("%.1f", Config.INSTANCE.range)));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.range = 2.0 + (this.value * 3.0);
                Config.save();
                if (Config.INSTANCE.enabled) {
                    DuperManager.reloadFrames();
                }
            }
        });
        y += 24;

        keybindButton = ButtonWidget.builder(getKeybindText(), button -> {
            listeningForKey = !listeningForKey;
            button.setMessage(getKeybindText());
        }).dimensions(x, y, width, height).build();
        addDrawableChild(keybindButton);
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.controls.select_items"), button -> {
            MinecraftClient.getInstance().setScreen(new ItemSelectScreen(this.screen));
        }).dimensions(x, y, width, height).build());
    }

    private void addDrawableChild(net.minecraft.client.gui.widget.ClickableWidget widget) {
        children.add(widget);
        drawables.add(widget);
    }

    private void updateReplaceFramesVisibility() {
        if (replaceFramesButton != null) {
            replaceFramesButton.visible = Config.INSTANCE.mode == Config.Mode.Speed;
        }
    }

    private Text getKeybindText() {
        if (listeningForKey) {
            return Text.translatable("dupe.controls.key.listening");
        }
        if (Config.INSTANCE.dupeKey == GLFW.GLFW_KEY_UNKNOWN || Config.INSTANCE.dupeKey == -1) {
            return Text.translatable("dupe.controls.key.none");
        }
        try {
            String keyName = InputUtil.fromKeyCode(Config.INSTANCE.dupeKey, 0).getLocalizedText().getString();
            return Text.translatable("dupe.controls.key.prefix", keyName);
        } catch (Exception e) {
            return Text.translatable("dupe.controls.key.prefix", "???");
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (net.minecraft.client.gui.Drawable drawable : drawables) {
            drawable.render(context, mouseX, mouseY, delta);
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (net.minecraft.client.gui.Element element : children) {
            if (element.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (listeningForKey) {
            listeningForKey = false;
            keybindButton.setMessage(getKeybindText());
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                Config.INSTANCE.dupeKey = GLFW.GLFW_KEY_UNKNOWN;
                Config.save();
                listeningForKey = false;
            } else {
                Config.INSTANCE.dupeKey = keyCode;
                Config.save();
                listeningForKey = false;
            }
            keybindButton.setMessage(getKeybindText());
            return true;
        }
        return false;
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getChildren() {
        return children;
    }

    public void setVisible(boolean visible) {
        for (net.minecraft.client.gui.widget.ClickableWidget widget : children) {
            widget.visible = visible;
        }
        if (replaceFramesButton != null && visible) {
            updateReplaceFramesVisibility();
        }
    }

    public void updatePositions(int inventoryX, int inventoryY) {
        int x = inventoryX - 105;
        int y = inventoryY + 5;

        for (net.minecraft.client.gui.widget.ClickableWidget widget : children) {
            widget.setX(x);
            widget.setY(y);
            y += 24;
        }
    }
}
