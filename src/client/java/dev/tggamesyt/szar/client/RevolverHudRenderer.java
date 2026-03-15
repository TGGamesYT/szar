package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.RevolverItem;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class RevolverHudRenderer {

    private static final int RADIUS = 30;
    private static final int CX_FROM_RIGHT = 80;
    private static final int CY_FROM_BOTTOM = 80;
    private static final float SNAP_SPEED = 0.15F;
    private static final float SLIDE_SPEED = 0.18F;
    // How far off screen to the right the barrel slides when closed
    private static final int SLIDE_DISTANCE = 150;

    private static float displayAngle = 0F;
    private static int lastKnownChamber = 0;

    // 0 = fully hidden (off screen right), 1 = fully visible
    private static float slideProgress = 0F;

    public static void register() {
        HudRenderCallback.EVENT.register(RevolverHudRenderer::render);
    }

    public static void tick(int currentChamber, float tickDelta) {
        float degreesPerChamber = 360F / RevolverItem.CHAMBERS;
        float targetAngle = -currentChamber * degreesPerChamber;

        if (currentChamber != lastKnownChamber) {
            lastKnownChamber = currentChamber;
        }

        float delta = targetAngle - displayAngle;
        while (delta > 180F)  delta -= 360F;
        while (delta < -180F) delta += 360F;
        displayAngle += delta * SNAP_SPEED * tickDelta;

        // Slide in/out
        float targetSlide = RevolverHudState.isOpen ? 1F : 0F;
        slideProgress += (targetSlide - slideProgress) * SLIDE_SPEED * tickDelta;
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isOf(Szar.REVOLVER)) return;

        int current = RevolverItem.getCurrentChamber(stack);
        tick(current, tickDelta);

        // Don't render at all if fully hidden
        if (slideProgress < 0.01F) return;

        boolean[] chambers = RevolverItem.getChambers(stack);

        int baseCx = context.getScaledWindowWidth() - CX_FROM_RIGHT;
        int cy = context.getScaledWindowHeight() - CY_FROM_BOTTOM;

        // Slide offset: 0 = off screen right, 1 = in place
        int slideOffset = (int)((1F - slideProgress) * SLIDE_DISTANCE);
        int cx = baseCx + slideOffset;

        // Draw translucent background circle
        int circleAlpha = (int)(slideProgress * 120); // max alpha 120 (~47% opacity)
        drawCircle(context, cx, cy, RADIUS + 8, circleAlpha);

        // Draw chamber dots
        for (int i = 0; i < RevolverItem.CHAMBERS; i++) {
            float baseAngle = (float)(-Math.PI / 2.0)
                    + i * (float)(2.0 * Math.PI / RevolverItem.CHAMBERS)
                    + (float)Math.toRadians(displayAngle);

            int x = (int)(cx + Math.cos(baseAngle) * RADIUS);
            int y = (int)(cy + Math.sin(baseAngle) * RADIUS);

            boolean isCurrent = i == current;
            int size = isCurrent ? 6 : 4;
            int color;
            if (isCurrent) {
                color = (int)(slideProgress * 255) << 24 | (chambers[i] ? 0x0000FF00 : 0x00FF4444);
            } else {
                color = (int)(slideProgress * 255) << 24 | (chambers[i] ? 0x00FFFFFF : 0x00555555);
            }

            context.fill(x - size / 2, y - size / 2, x + size / 2, y + size / 2, color);
        }

        // Center dot
        int dotAlpha = (int)(slideProgress * 255);
        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, dotAlpha << 24 | 0x00AAAAAA);
    }

    private static void drawCircle(DrawContext context, int cx, int cy, int radius, int alpha) {
        // Approximate circle with filled quads at each degree
        int steps = 64;
        int color = alpha << 24 | 0x00222222;
        for (int i = 0; i < steps; i++) {
            float a1 = (float)(i * 2 * Math.PI / steps);
            float a2 = (float)((i + 1) * 2 * Math.PI / steps);

            // Draw thin triangle slice as a quad
            int x1 = (int)(cx + Math.cos(a1) * radius);
            int y1 = (int)(cy + Math.sin(a1) * radius);
            int x2 = (int)(cx + Math.cos(a2) * radius);
            int y2 = (int)(cy + Math.sin(a2) * radius);

            // Fill triangle from center to edge
            context.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1,
                    Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
        // Fill center solid
        context.fill(cx - radius, cy - radius, cx + radius, cy + radius, color);
    }
}