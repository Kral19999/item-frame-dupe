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
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int doubleWidth = 310;
        int singleWidth = 150;
        int y = 40;

        addDrawableChild(new SliderWidget(leftX, y, doubleWidth, 20,
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

        boolean isNormalMode = Config.INSTANCE.mode == Config.Mode.Normal;
        int currentMaxFrames = isNormalMode ? 3 : Math.min(9, Config.INSTANCE.maxFrames);
        SliderWidget maxFramesSlider = new SliderWidget(leftX, y, doubleWidth, 20,
                Text.translatable("dupe.debug.max_frames", currentMaxFrames),
                (currentMaxFrames - 1) / 8.0) {
            @Override
            protected void updateMessage() {
                int displayVal = Config.INSTANCE.mode == Config.Mode.Normal ? 3 : Math.min(9, Config.INSTANCE.maxFrames);
                this.setMessage(Text.translatable("dupe.debug.max_frames", displayVal));
            }

            @Override
            protected void applyValue() {
                if (Config.INSTANCE.mode != Config.Mode.Normal) {
                    Config.INSTANCE.maxFrames = 1 + (int) Math.round(this.value * 8);
                    Config.save();
                }
            }
        };
        maxFramesSlider.active = !isNormalMode;
        addDrawableChild(maxFramesSlider);
        y += 24;

        if (Config.INSTANCE.mode == Config.Mode.Speed) {
            addDrawableChild(CyclingButtonWidget
                    .<Boolean>builder(
                            value -> Text.translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                    .values(true, false)
                    .initially(Config.INSTANCE.replaceItemFrames)
                    .build(leftX, y, singleWidth, 20, Text.translatable("dupe.controls.replace"), (button, value) -> {
                        Config.INSTANCE.replaceItemFrames = value;
                        Config.save();
                    }));
            keybindButton = ButtonWidget.builder(getKeybindText(), button -> {
                listeningForKey = !listeningForKey;
                button.setMessage(getKeybindText());
            }).dimensions(rightX, y, singleWidth, 20).build();
        } else {
            keybindButton = ButtonWidget.builder(getKeybindText(), button -> {
                listeningForKey = !listeningForKey;
                button.setMessage(getKeybindText());
            }).dimensions(leftX, y, doubleWidth, 20).build();
        }
        addDrawableChild(keybindButton);
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.controls.select_items"), button -> {
            MinecraftClient.getInstance().setScreen(new ItemSelectScreen(this));
        }).dimensions(leftX, y, doubleWidth, 20).build());
        y += 24;

        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(
                        (Boolean) value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.checkStatus)
                .build(leftX, y, singleWidth, 20, Text.translatable("dupe.settings.status_check"), (button, value) -> {
                    Config.INSTANCE.checkStatus = (Boolean) value;
                    Config.save();
                }));

        addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(
                        value -> Text
                                .translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.multitask)
                .build(rightX, y, singleWidth, 20, Text.translatable("dupe.settings.multitask"), (button, value) -> {
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
                .build(leftX, y, singleWidth, 20, Text.translatable("dupe.settings.anti_double_click"), (button, value) -> {
                    Config.INSTANCE.antiDoubleClick = (Boolean) value;
                    Config.save();
                    updateSliderVisibility();
                });
        addDrawableChild(antiDoubleClickButton);

        delaySlider = new SliderWidget(rightX, y, singleWidth, 20,
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

        addDrawableChild(CyclingButtonWidget
                .<Config.RotationMode>builder(value -> Text.translatable("dupe.controls.rotation_mode." + value.name().toLowerCase()))
                .values(Config.RotationMode.values())
                .initially(Config.INSTANCE.rotationMode)
                .build(leftX, y, singleWidth, 20, Text.translatable("dupe.controls.rotation_mode"), (button, value) -> {
                    Config.INSTANCE.rotationMode = value;
                    Config.save();
                }));

        addDrawableChild(new SliderWidget(rightX, y, singleWidth, 20,
                Text.translatable("dupe.settings.tick_delay", Config.INSTANCE.tickDelay),
                Config.INSTANCE.tickDelay / 3.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.settings.tick_delay", Config.INSTANCE.tickDelay));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.tickDelay = (int) Math.round(this.value * 3.0);
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(value -> Text.translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.invPick)
                .build(leftX, y, doubleWidth, 20, Text.translatable("dupe.settings.inv_pick"), (button, value) -> {
                    Config.INSTANCE.invPick = value;
                    Config.save();
                }));
        y += 24;

        addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(value -> Text.translatable(value ? "dupe.controls.enabled" : "dupe.controls.disabled"))
                .values(true, false)
                .initially(Config.INSTANCE.showHud)
                .build(leftX, y, doubleWidth, 20, Text.translatable("dupe.settings.show_hud"), (button, value) -> {
                    Config.INSTANCE.showHud = value;
                    Config.save();
                }));
        y += 24;

        addDrawableChild(new SliderWidget(leftX, y, doubleWidth, 20,
                Text.translatable("dupe.settings.hud_x", String.format("%d", (int) (Config.INSTANCE.hudX * 100))),
                Config.INSTANCE.hudX) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.settings.hud_x", String.format("%d", (int) (Config.INSTANCE.hudX * 100))));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.hudX = this.value;
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(new SliderWidget(leftX, y, doubleWidth, 20,
                Text.translatable("dupe.settings.hud_y", String.format("%d", (int) (Config.INSTANCE.hudY * 100))),
                Config.INSTANCE.hudY) {
            @Override
            protected void updateMessage() {
                this.setMessage(Text.translatable("dupe.settings.hud_y", String.format("%d", (int) (Config.INSTANCE.hudY * 100))));
            }

            @Override
            protected void applyValue() {
                Config.INSTANCE.hudY = this.value;
                Config.save();
            }
        });
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.done"), button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    private void updateSliderVisibility() {
        boolean isSpeedMode = Config.INSTANCE.mode == Config.Mode.Speed
                || Config.INSTANCE.mode == Config.Mode.Multi;

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

        if (Config.INSTANCE.showHud) {
            String previewText = "DPS: 0 (" + Text.translatable("dupe.settings.hud_preview").getString() + ")";
            int textWidth = this.textRenderer.getWidth(previewText);
            int textHeight = this.textRenderer.fontHeight;
            int drawX = 4 + (int) (Config.INSTANCE.hudX * (this.width - textWidth - 8));
            int drawY = 4 + (int) (Config.INSTANCE.hudY * (this.height - textHeight - 8));

            context.fill(drawX - 2, drawY - 2, drawX + textWidth + 2, drawY + textHeight + 2, 0xAA000000);
            context.drawTextWithShadow(this.textRenderer, previewText, drawX, drawY, 0x00FF00);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
