package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.items.Joint;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;

public class SmokeZoomHandler {
    private static float smokeScale = 0.5f; // start scale
    private static final float TARGET_SCALE = 1.05f; // max zoom
    private static final float LERP_SPEED = 0.5f; // lerp speed

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ItemStack stack = client.player.getActiveItem();
            boolean usingSmoke = stack.getItem() instanceof Joint;

            float target = usingSmoke ? TARGET_SCALE : 0.5f;

            // lerp smokeScale toward target like spyglass
            smokeScale = lerp(LERP_SPEED * client.getTickDelta(), smokeScale, target);
        });
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static float getSmokeScale() {
        return smokeScale;
    }
}
