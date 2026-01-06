package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemSelectScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchBox;
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> filteredItems = new ArrayList<>();

    private static final int ITEM_SIZE = 18;
    private static final int ITEMS_PER_ROW = 14;
    private int scrollOffset = 0;

    public ItemSelectScreen(Screen parent) {
        super(Text.translatable("dupe.screen.select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (allItems.isEmpty()) {
            Registries.ITEM.stream()
                    .filter(item -> item != net.minecraft.item.Items.AIR)
                    .sorted(Comparator.comparing(item -> item.getName().getString()))
                    .forEach(allItems::add);
        }

        int searchWidth = 200;
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - searchWidth / 2, 20, searchWidth, 20,
                Text.translatable("dupe.screen.select.search"));
        this.searchBox.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(searchBox);
        this.setFocused(searchBox);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.done"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        updateFilteredItems();
    }

    private void onSearchChanged(String query) {
        updateFilteredItems();
        scrollOffset = 0;
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        String query = searchBox.getText().toLowerCase();
        for (Item item : allItems) {
            if (item.getName().getString().toLowerCase().contains(query) ||
                    Registries.ITEM.getId(item).toString().contains(query)) {
                filteredItems.add(item);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 400.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        int startX = (this.width - (ITEMS_PER_ROW * ITEM_SIZE)) / 2;
        int startY = 60;
        int maxRows = (this.height - 100) / ITEM_SIZE;
        int endY = startY + (maxRows * ITEM_SIZE);

        int x = startX;
        int y = startY;

        List<Item> dupeItems = Config.getDupeItems();
        int startIndex = scrollOffset * ITEMS_PER_ROW;

        ItemStack tooltipStack = null;

        for (int i = startIndex; i < filteredItems.size(); i++) {
            if (y + ITEM_SIZE > endY)
                break;

            Item item = filteredItems.get(i);
            boolean isSelected = dupeItems.contains(item);

            if (isSelected) {
                context.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0xFF00FF00);
            } else if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                context.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, 0x80FFFFFF);
            }

            ItemStack stack = new ItemStack(item);
            context.drawItem(stack, x + 1, y + 1);

            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                tooltipStack = stack;
            }

            x += ITEM_SIZE;
            if (x >= startX + (ITEMS_PER_ROW * ITEM_SIZE)) {
                x = startX;
                y += ITEM_SIZE;
            }
        }

        context.getMatrices().pop();

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("dupe.screen.select.count", dupeItems.size()), this.width / 2,
                45, 0xAAAAAA);

        if (tooltipStack != null) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 500.0F);
            context.drawTooltip(this.textRenderer, tooltipStack.getName(), mouseX, mouseY);
            context.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        int startX = (this.width - (ITEMS_PER_ROW * ITEM_SIZE)) / 2;
        int startY = 60;
        int maxRows = (this.height - 100) / ITEM_SIZE;
        int endY = startY + (maxRows * ITEM_SIZE);

        if (mouseY >= startY && mouseY < endY) {
            int relX = (int) (mouseX - startX);
            int relY = (int) (mouseY - startY);

            if (relX >= 0 && relX < ITEMS_PER_ROW * ITEM_SIZE) {
                int col = relX / ITEM_SIZE;
                int row = relY / ITEM_SIZE;
                int index = (scrollOffset * ITEMS_PER_ROW) + (row * ITEMS_PER_ROW) + col;

                if (index >= 0 && index < filteredItems.size()) {
                    Item item = filteredItems.get(index);
                    toggleItem(item);
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleItem(Item item) {
        if (Config.getDupeItems().contains(item)) {
            Config.removeDupeItem(item);
        } else {
            Config.addDupeItem(item);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            int rows = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_ROW);
            int visibleRows = (this.height - 100) / ITEM_SIZE;

            if (rows > visibleRows) {
                scrollOffset -= (int) verticalAmount;
                if (scrollOffset < 0)
                    scrollOffset = 0;
                if (scrollOffset > rows - visibleRows)
                    scrollOffset = rows - visibleRows;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchBox.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox.isFocused() && this.searchBox.isVisible()
                && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
