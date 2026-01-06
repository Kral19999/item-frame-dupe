package dev.kral1999.dupe;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DupeCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("dupe")
                    .then(ClientCommandManager.literal("toggle")
                            .executes(DupeCommands::toggle))
                    .then(ClientCommandManager.literal("add")
                            .executes(DupeCommands::add))
                    .then(ClientCommandManager.literal("remove")
                            .executes(DupeCommands::remove))
                    .then(ClientCommandManager.literal("list")
                            .executes(DupeCommands::list)));
        });
    }

    private static int toggle(CommandContext<FabricClientCommandSource> context) {
        DuperManager.onDupeKeyPressed();
        boolean enabled = Config.INSTANCE.enabled;
        context.getSource()
                .sendFeedback(Text.translatable(enabled ? "dupe.command.toggle.on" : "dupe.command.toggle.off")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        return 1;
    }

    private static int add(CommandContext<FabricClientCommandSource> context) {
        ItemStack stack = context.getSource().getPlayer().getMainHandStack();
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("dupe.command.add.no_item"));
            return 0;
        }

        Item item = stack.getItem();
        if (Config.getDupeItems().contains(item)) {
            context.getSource().sendFeedback(Text.translatable("dupe.command.add.exists")
                    .formatted(Formatting.YELLOW));
        } else {
            Config.addDupeItem(item);
            context.getSource().sendFeedback(Text.translatable("dupe.command.add.success", Registries.ITEM.getId(item))
                    .formatted(Formatting.GREEN));
        }
        return 1;
    }

    private static int remove(CommandContext<FabricClientCommandSource> context) {
        ItemStack stack = context.getSource().getPlayer().getMainHandStack();
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("dupe.command.remove.no_item"));
            return 0;
        }

        Item item = stack.getItem();
        if (!Config.getDupeItems().contains(item)) {
            context.getSource().sendFeedback(Text.translatable("dupe.command.remove.not_in_list")
                    .formatted(Formatting.YELLOW));
        } else {
            Config.removeDupeItem(item);
            context.getSource()
                    .sendFeedback(Text.translatable("dupe.command.remove.success", Registries.ITEM.getId(item))
                            .formatted(Formatting.GREEN));
        }
        return 1;
    }

    private static int list(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.translatable("dupe.command.list.header").formatted(Formatting.GOLD));
        for (Item item : Config.getDupeItems()) {
            context.getSource().sendFeedback(Text.literal("- " + Registries.ITEM.getId(item))
                    .formatted(Formatting.WHITE));
        }
        return 1;
    }
}
