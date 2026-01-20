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

public class SettingsScreen extends Screen {

    private final Screen parent;
    private ButtonWidget keybindButton;
    private SliderWidget delaySlider;
    private boolean listeningForKey = false;
    private CyclingButtonWidget<Boolean> antiDoubleClickButton;

    public SettingsScreen(Screen parent) {
        super(Text.translatable("dupe.screen.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = 40;

        addDrawableChild(new SliderWidget(x, y, 200, 20,
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

        if (Config.INSTANCE.mode == Config.Mode.Speed) {
            addDrawableChild(CyclingButtonWidget
                    .<Boolean>builder(
                            value -> Text.translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                    .values(true, false)
                    .initially(Config.INSTANCE.replaceItemFrames)
                    .build(x, y, 200, 20, Text.translatable("dupe.controls.replace"), (button, value) -> {
                        Config.INSTANCE.replaceItemFrames = value;
                        Config.save();
                    }));
            y += 24;
        }

        keybindButton = ButtonWidget.builder(getKeybindText(), button -> {
            listeningForKey = !listeningForKey;
            button.setMessage(getKeybindText());
        }).dimensions(x, y, 200, 20).build();
        addDrawableChild(keybindButton);
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.controls.select_items"), button -> {
            MinecraftClient.getInstance().setScreen(new ItemSelectScreen(this));
        }).dimensions(x, y, 200, 20).build());

        y += 36;

        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(
                        (Boolean) value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.checkStatus)
                .build(x, y, 200, 20, Text.translatable("dupe.settings.status_check"), (button, value) -> {
                    Config.INSTANCE.checkStatus = (Boolean) value;
                    Config.save();
                }));
        y += 24;

        addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(
                        value -> Text
                                .translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.multitask)
                .build(x, y, 200, 20, Text.translatable("dupe.settings.multitask"), (button, value) -> {
                    Config.INSTANCE.multitask = value;
                    Config.save();
                }));
        y += 24;

        antiDoubleClickButton = CyclingButtonWidget
                .<Boolean>builder(
                        value -> Text.translatable((Boolean) value ? "dupe.controls.enabled"
                                : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.antiDoubleClick)
                .build(x, y, 200, 20, Text.translatable("dupe.settings.anti_double_click"), (button, value) -> {
                    Config.INSTANCE.antiDoubleClick = (Boolean) value;
                    Config.save();
                    updateSliderVisibility();
                });
        addDrawableChild(antiDoubleClickButton);
        y += 24;

        delaySlider = new SliderWidget(x, y, 200, 20,
                Text.translatable("dupe.settings.limit_ms", Config.INSTANCE.doubleClickDelay),
                Config.INSTANCE.doubleClickDelay / 200.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.settings.limit_ms", Config.INSTANCE.doubleClickDelay));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.doubleClickDelay = (int) (this.value * 200);
                Config.save();
            }
        };
        addDrawableChild(delaySlider);
        updateSliderVisibility();
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.done"), button -> this.close())
                .dimensions(x, this.height - 40, 200, 20)
                .build());
    }

    private void updateSliderVisibility() {
        boolean isSpeedMode = Config.INSTANCE.mode == Config.Mode.Speed;

        if (antiDoubleClickButton != null) {
            antiDoubleClickButton.visible = !isSpeedMode;
        }

        if (delaySlider != null) {
            delaySlider.visible = !isSpeedMode && Config.INSTANCE.antiDoubleClick;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (listeningForKey) {
            listeningForKey = false;
            keybindButton.setMessage(getKeybindText());
            return true;
        }
        return false;
    }

    @Override
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
        return super.keyPressed(keyCode, scanCode, modifiers);
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
