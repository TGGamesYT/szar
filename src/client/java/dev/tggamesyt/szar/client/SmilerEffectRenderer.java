package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.SmilerEffectManager;
import dev.tggamesyt.szar.SmilerType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SmilerEffectRenderer {

    private static float flashbangAlpha = 0f;
    private static float jumpscareAlpha = 0f;
    private static SmilerType jumpscareType = null;
    private static int jumpscareTimer = 0;

    private static final Identifier EYES_FACE =
            new Identifier("szar", "textures/entity/eyes.png");
    private static final Identifier SCARY_FACE =
            new Identifier("szar", "textures/entity/scary.png");
    private static final Identifier REAL_FACE =
            new Identifier("szar", "textures/entity/real.png");

    public static void register() {

        // Flashbang packet (kept as-is, but mainly used by EYES now)
        ClientPlayNetworking.registerGlobalReceiver(
                SmilerEffectManager.FLASHBANG_PACKET, (client, handler, buf, responseSender) -> {
                    client.execute(() -> flashbangAlpha = 1.0f);
                });

        // Jumpscare packet
        ClientPlayNetworking.registerGlobalReceiver(
                SmilerEffectManager.JUMPSCARE_PACKET, (client, handler, buf, responseSender) -> {
                    String typeName = buf.readString();
                    SmilerType type = SmilerType.valueOf(typeName);

                    client.execute(() -> {
                        jumpscareType = type;
                        jumpscareTimer = 40;

                        // 🔊 Play correct sound per type
                        Identifier soundId = switch (type) {
                            case EYES -> new Identifier("szar", "flashbang");
                            case SCARY -> new Identifier("szar", "scary");
                            case REAL -> new Identifier("szar", "real");
                        };

                        client.getSoundManager().play(
                                PositionedSoundInstance.master(
                                        SoundEvent.of(soundId), 1.0f));

                        // 💥 Special handling for EYES
                        if (type == SmilerType.EYES) {
                            flashbangAlpha = 1.0f;
                            jumpscareAlpha = 0f; // no face rendering
                        } else {
                            jumpscareAlpha = 1.0f;
                        }
                    });
                });

        // HUD renderer
        HudRenderCallback.EVENT.register(SmilerEffectRenderer::renderHud);
    }

    private static void renderHud(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // 💥 Flashbang
        if (flashbangAlpha > 0) {
            int alpha = (int)(flashbangAlpha * 255);
            context.fill(0, 0, screenW, screenH,
                    (alpha << 24) | 0x00FFFFFF);
            flashbangAlpha = Math.max(0, flashbangAlpha - 0.02f);
        }

        // 👁️ Jumpscare (ONLY if not EYES)
        if (jumpscareAlpha > 0 && jumpscareType != null && jumpscareType != SmilerType.EYES) {
            jumpscareTimer--;

            Identifier faceTexture = switch (jumpscareType) {
                case SCARY -> SCARY_FACE;
                case REAL -> REAL_FACE;
                default -> SCARY_FACE;
            };

            int alpha = (int)(jumpscareAlpha * 255);

            context.drawTexture(faceTexture,
                    0, 0, screenW, screenH,
                    0, 0, 768, 768,
                    1536, 1536);

            context.fill(0, 0, screenW, screenH,
                    (alpha << 24) & 0xFF000000);

            if (jumpscareTimer <= 0) {
                jumpscareAlpha = Math.max(0, jumpscareAlpha - 0.05f);
                if (jumpscareAlpha <= 0) jumpscareType = null;
            }
        }
    }
}