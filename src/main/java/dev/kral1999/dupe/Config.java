package dev.kral1999.dupe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Config {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("itemframeduper.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Item> cachedDupeItems = null;

    public static ConfigValues INSTANCE = new ConfigValues();

    public static class ConfigValues {
        public boolean enabled = false;
        public Mode mode = Mode.Normal;
        public double range = 4.0;
        public boolean replaceItemFrames = true;
        public int maxFrames = 3;
        public int dupeKey = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

        public int maxPlacements = 1;
        public int maxSwaps = 3;
        public int maxInventoryMoves = 1;

        public List<String> dupeItems = new ArrayList<>(Arrays.asList(
                "minecraft:shulker_box", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
                "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box",
                "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
                "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box",
                "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
                "minecraft:red_shulker_box", "minecraft:black_shulker_box"));
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ConfigValues.class);
                cachedDupeItems = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Item> getDupeItems() {
        if (cachedDupeItems == null) {
            cachedDupeItems = INSTANCE.dupeItems.stream()
                    .map(id -> Registries.ITEM.get(Identifier.of(id)))
                    .collect(Collectors.toList());
        }
        return cachedDupeItems;
    }

    public static void addDupeItem(Item item) {
        String id = Registries.ITEM.getId(item).toString();
        if (!INSTANCE.dupeItems.contains(id)) {
            INSTANCE.dupeItems.add(id);
            cachedDupeItems = null;
            save();
        }
    }

    public static void removeDupeItem(Item item) {
        String id = Registries.ITEM.getId(item).toString();
        if (INSTANCE.dupeItems.contains(id)) {
            INSTANCE.dupeItems.remove(id);
            cachedDupeItems = null;
            save();
        }
    }

    public enum Mode {
        Normal, Speed
    }
}
