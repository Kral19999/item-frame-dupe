package dev.kral1999.dupe.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger("Item Frame Dupe");
    public static volatile boolean isUpdateAvailable = false;
    public static volatile String latestVersion = "";
    private static final String CURRENT_VERSION = FabricLoader.getInstance()
            .getModContainer("itemframeduper").get().getMetadata().getVersion().getFriendlyString();

    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = URI
                        .create("https://api.github.com/repos/Kral19999/item-frame-dupe/releases")
                        .toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                        var jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

                        if (jsonArray.size() > 0) {
                            JsonObject json = jsonArray.get(0).getAsJsonObject();
                            String tagName = json.get("tag_name").getAsString();

                            String cleanTag = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                            if (isNewerVersion(CURRENT_VERSION, cleanTag)) {
                                latestVersion = tagName;
                                isUpdateAvailable = true;
                                LOGGER.info("Update available: " + tagName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to check for updates: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int v1 = 0;
            int v2 = 0;
            try {
                v1 = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            } catch (NumberFormatException ignored) {
            }
            try {
                v2 = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            } catch (NumberFormatException ignored) {
            }

            if (v2 > v1)
                return true;
            if (v2 < v1)
                return false;
        }
        return false;
    }
}
