package dev.kral1999.dupe.gui;

import dev.kral1999.dupe.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
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
    private boolean confirmReset = false;
    private boolean confirmClear = false;
    private ButtonWidget clearAllButton;
    private ButtonWidget resetButton;

    private final java.util.Map<Category, List<Item>> categoryCache = new java.util.HashMap<>();
    private Category selectedCategory = Category.BUILDING_BLOCKS;
    private ButtonWidget categoryButton;
    private boolean categoryDropdownOpen = false;

    private Text getCategoryButtonText(Category category) {
        return Text.translatable(category.getTranslationKey()).copy().append(" ▼");
    }

    public enum Category {
        BUILDING_BLOCKS("dupe.category.building_blocks", net.minecraft.item.ItemGroups.BUILDING_BLOCKS),
        COLORED_BLOCKS("dupe.category.colored_blocks", net.minecraft.item.ItemGroups.COLORED_BLOCKS),
        NATURAL("dupe.category.natural", net.minecraft.item.ItemGroups.NATURAL),
        FUNCTIONAL("dupe.category.functional", net.minecraft.item.ItemGroups.FUNCTIONAL),
        REDSTONE("dupe.category.redstone", net.minecraft.item.ItemGroups.REDSTONE),
        TOOLS("dupe.category.tools", net.minecraft.item.ItemGroups.TOOLS),
        COMBAT("dupe.category.combat", net.minecraft.item.ItemGroups.COMBAT),
        FOOD_AND_DRINK("dupe.category.food_and_drink", net.minecraft.item.ItemGroups.FOOD_AND_DRINK),
        INGREDIENTS("dupe.category.ingredients", net.minecraft.item.ItemGroups.INGREDIENTS),
        CREATIVE("dupe.category.creative", net.minecraft.item.ItemGroups.OPERATOR);

        private final String translationKey;
        private final net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup> groupKey;

        Category(String translationKey, net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup> groupKey) {
            this.translationKey = translationKey;
            this.groupKey = groupKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public ItemSelectScreen(Screen parent) {
        super(Text.translatable("dupe.screen.select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        confirmClear = false;
        confirmReset = false;
        categoryDropdownOpen = false;
        if (allItems.isEmpty()) {
            Registries.ITEM.stream()
                    .filter(item -> item != net.minecraft.item.Items.AIR)
                    .sorted(Comparator.comparing(item -> item.getName().getString()))
                    .forEach(allItems::add);
        }

        int searchWidth = 180;
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 20, searchWidth, 20,
                Text.translatable("dupe.screen.select.search"));
        this.searchBox.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(searchBox);
        this.setFocused(searchBox);

        this.addDrawableChild(CyclingButtonWidget
                .<Boolean>builder(value -> Text.translatable(value ? "dupe.screen.select.blacklist" : "dupe.screen.select.whitelist"))
                .values(true, false)
                .initially(Config.INSTANCE.blacklistMode)
                .build(this.width / 2 + 40, 20, 110, 20, Text.translatable("dupe.screen.select.list_mode"), (button, value) -> {
                    Config.INSTANCE.blacklistMode = value;
                    Config.save();
                }));

        categoryButton = ButtonWidget.builder(getCategoryButtonText(selectedCategory), button -> {
            categoryDropdownOpen = !categoryDropdownOpen;
        }).dimensions(this.width / 2 - 100, 44, 200, 20).build();
        this.addDrawableChild(categoryButton);

        this.clearAllButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.clear_all"), button -> {
            if (!confirmClear) {
                confirmClear = true;
                button.setMessage(Text.translatable("dupe.screen.select.clear_all.confirm"));
                if (confirmReset && resetButton != null) {
                    confirmReset = false;
                    resetButton.setMessage(Text.translatable("dupe.screen.select.reset"));
                }
            } else {
                Config.clearDupeItems();
                confirmClear = false;
                button.setMessage(Text.translatable("dupe.screen.select.clear_all"));
            }
        }).dimensions(this.width / 2 - 155, this.height - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.done"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        this.resetButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("dupe.screen.select.reset"), button -> {
            if (!confirmReset) {
                confirmReset = true;
                button.setMessage(Text.translatable("dupe.screen.select.reset.confirm"));
                if (confirmClear && clearAllButton != null) {
                    confirmClear = false;
                    clearAllButton.setMessage(Text.translatable("dupe.screen.select.clear_all"));
                }
            } else {
                Config.resetDupeItems();
                confirmReset = false;
                button.setMessage(Text.translatable("dupe.screen.select.reset"));
            }
        }).dimensions(this.width / 2 + 55, this.height - 30, 100, 20).build());

        if (categoryButton != null) {
            categoryButton.visible = searchBox.getText().isEmpty();
        }

        updateFilteredItems();
    }

    private void onSearchChanged(String query) {
        if (categoryButton != null) {
            categoryButton.visible = query.isEmpty();
        }
        if (!query.isEmpty()) {
            categoryDropdownOpen = false;
        }
        updateFilteredItems();
        scrollOffset = 0;
    }

    private List<Item> getItemsForCategory(Category category) {
        if (categoryCache.containsKey(category)) {
            return categoryCache.get(category);
        }
        List<Item> items = new ArrayList<>();
        if (client == null || client.world == null || client.player == null) return items;

        List<net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup>> groupsToQuery = new ArrayList<>();
        groupsToQuery.add(category.groupKey);
        if (category == Category.CREATIVE) {
            groupsToQuery.add(net.minecraft.item.ItemGroups.SPAWN_EGGS);
        }

        var enabledFeatures = client.player.networkHandler.getEnabledFeatures();
        boolean showOp = true;
        var displayContext = new net.minecraft.item.ItemGroup.DisplayContext(enabledFeatures, showOp, client.world.getRegistryManager());

        for (var key : groupsToQuery) {
            net.minecraft.item.ItemGroup group = Registries.ITEM_GROUP.get(key);
            if (group == null) continue;
            group.updateEntries(displayContext);

            for (ItemStack stack : group.getDisplayStacks()) {
                Item item = stack.getItem();
                if (item != net.minecraft.item.Items.AIR && !items.contains(item)) {
                    items.add(item);
                }
            }
        }
        categoryCache.put(category, items);
        return items;
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        String query = searchBox.getText().toLowerCase();
        if (query.isEmpty()) {
            filteredItems.addAll(getItemsForCategory(selectedCategory));
        } else {
            for (Item item : allItems) {
                if (item.getName().getString().toLowerCase().contains(query) ||
                        Registries.ITEM.getId(item).toString().contains(query)) {
                    filteredItems.add(item);
                }
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
        int startY = 70;
        int maxRows = (this.height - 125) / ITEM_SIZE;
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
                this.height - 45, 0xAAAAAA);

        if (tooltipStack != null) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 500.0F);
            context.drawTooltip(this.textRenderer, tooltipStack.getName(), mouseX, mouseY);
            context.getMatrices().pop();
        }

        if (categoryDropdownOpen && searchBox.getText().isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 600.0F);
            int dropX = this.width / 2 - 100;
            int dropY = 64;
            int dropWidth = 200;
            int rowHeight = 16;
            int dropHeight = Category.values().length * rowHeight;

            context.fill(dropX - 1, dropY - 1, dropX + dropWidth + 1, dropY + dropHeight + 1, 0xFF404040);
            context.fill(dropX, dropY, dropX + dropWidth, dropY + dropHeight, 0xF20F0F0F);

            for (int i = 0; i < Category.values().length; i++) {
                Category cat = Category.values()[i];
                int rowY = dropY + (i * rowHeight);
                boolean isHovered = mouseX >= dropX && mouseX < dropX + dropWidth && mouseY >= rowY && mouseY < rowY + rowHeight;
                boolean isCurrent = cat == selectedCategory;

                if (isHovered) {
                    context.fill(dropX, rowY, dropX + dropWidth, rowY + rowHeight, 0xFF3A3A3A);
                } else if (isCurrent) {
                    context.fill(dropX, rowY, dropX + dropWidth, rowY + rowHeight, 0xFF252525);
                }

                int textColor = isCurrent ? 0xFFFFFF00 : (isHovered ? 0xFFFFFFFF : 0xCCCCCC);
                context.drawText(this.textRenderer, Text.translatable(cat.getTranslationKey()), dropX + 8, rowY + 4, textColor, false);
            }
            context.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (categoryDropdownOpen && searchBox.getText().isEmpty()) {
            int dropX = this.width / 2 - 100;
            int dropY = 64;
            int dropWidth = 200;
            int rowHeight = 16;
            int dropHeight = Category.values().length * rowHeight;

            if (mouseX >= dropX && mouseX < dropX + dropWidth && mouseY >= dropY && mouseY < dropY + dropHeight) {
                int index = (int) ((mouseY - dropY) / rowHeight);
                if (index >= 0 && index < Category.values().length) {
                    selectedCategory = Category.values()[index];
                    categoryButton.setMessage(getCategoryButtonText(selectedCategory));
                    updateFilteredItems();
                    scrollOffset = 0;
                }
            }
            categoryDropdownOpen = false;
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        int startX = (this.width - (ITEMS_PER_ROW * ITEM_SIZE)) / 2;
        int startY = 70;
        int maxRows = (this.height - 125) / ITEM_SIZE;
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
            int visibleRows = (this.height - 125) / ITEM_SIZE;

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
