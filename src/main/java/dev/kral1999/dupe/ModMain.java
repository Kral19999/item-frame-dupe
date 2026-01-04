package dev.kral1999.dupe;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class ModMain implements ClientModInitializer {

    private boolean wasKeyPresed = false;

    @Override
    public void onInitializeClient() {
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Config.INSTANCE.dupeKey != GLFW.GLFW_KEY_UNKNOWN) {
                boolean isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getHandle(),
                        Config.INSTANCE.dupeKey) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                if (isPressed && !wasKeyPresed) {
                    if (client.player != null) {
                        DuperManager.onDupeKeyPressed();
                    }
                }
                wasKeyPresed = isPressed;
            }

            DuperManager.tick();
        });

        System.out.println("Item Frame Duper Initialized!");
    }
}
