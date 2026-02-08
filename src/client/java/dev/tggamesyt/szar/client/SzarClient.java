package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.NyanEntity;
import dev.tggamesyt.szar.PlaneEntity;
import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.PlaneAnimation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.*;

import static dev.tggamesyt.szar.Szar.HitterEntityType;
import static dev.tggamesyt.szar.Szar.PLANE_ANIM_PACKET;
import static javax.swing.text.html.HTML.Attribute.ID;

public class SzarClient implements ClientModInitializer {
    private static final Map<KeyBinding, KeyBinding> activeScramble = new HashMap<>();
    public static final EntityModelLayer PLANE =
            new EntityModelLayer(
                    new Identifier(Szar.MOD_ID, "plane"),
                    "main"
            );
    public static final EntityModelLayer NYAN =
            new EntityModelLayer(
                new Identifier(Szar.MOD_ID, "nyan_cat"),
                    "main"
            );
    // Outside of your tick handler
    private final Map<NyanEntity, EntityTrackingSoundInstance> activeSounds = new HashMap<>();
    private static final SoundEvent NYAN_LOOP = SoundEvent.of(new Identifier("szar", "nyan_cat_loop"));
    private static final SoundEvent NYAN_START = SoundEvent.of(new Identifier("szar", "nyan_cat_first_loop"));
    int startOffset = 10;
    int startLength = 596;
    int loopLength = 541;
    int loopStart = startOffset + startLength;
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;

            Box box = new Box(
                    client.player.getX() - 128, client.player.getY() - 128, client.player.getZ() - 128,
                    client.player.getX() + 128, client.player.getY() + 128, client.player.getZ() + 128
            );

            for (NyanEntity nyan : client.world.getEntitiesByClass(NyanEntity.class, box, e -> true)) {
                if (!nyan.isAlive()) continue;

                int age = nyan.age;

                // ---- PLAY START ONCE ----
                if (age >= startOffset && !activeSounds.containsKey(nyan)) {
                    EntityTrackingSoundInstance startSound = new EntityTrackingSoundInstance(
                            NYAN_START,
                            SoundCategory.NEUTRAL,
                            1.0f,
                            1.0f,
                            nyan,
                            nyan.getId() // seed can be entity ID for stable pitch
                    );
                    client.getSoundManager().play(startSound);
                    activeSounds.put(nyan, startSound);
                }

                // ---- LOOP AFTER START FINISHES ----
                if (age >= loopStart && (age - loopStart) % loopLength == 0) {
                    EntityTrackingSoundInstance loopSound = new EntityTrackingSoundInstance(
                            NYAN_LOOP,
                            SoundCategory.NEUTRAL,
                            1.0f,
                            1.0f,
                            nyan,
                            nyan.getId() // seed
                    );
                    client.getSoundManager().play(loopSound);
                    activeSounds.put(nyan, loopSound);
                }
            }

            // Stop sounds for dead entities
            Iterator<Map.Entry<NyanEntity, EntityTrackingSoundInstance>> it = activeSounds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<NyanEntity, EntityTrackingSoundInstance> entry = it.next();
                if (!entry.getKey().isAlive()) {
                    client.getSoundManager().stop(entry.getValue());
                    it.remove();
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                PLANE_ANIM_PACKET,
                (client, handler, buf, sender) -> {

                    int entityId = buf.readInt();
                    PlaneAnimation anim = buf.readEnumConstant(PlaneAnimation.class);

                    client.execute(() -> {
                        if (client.world == null) return;

                        Entity e = client.world.getEntityById(entityId);
                        if (!(e instanceof PlaneEntity plane)) return;

                        if (anim == null) {
                            plane.stopAnimation();
                            return;
                        }

                        plane.playAnimation(anim, anim.looping);
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                Szar.TOTEMPACKET,
                (client, handler, buf, responseSender) -> {

                    ItemStack stack = buf.readItemStack();

                    client.execute(() -> {
                        MinecraftClient.getInstance()
                                .gameRenderer.showFloatingItem(stack);
                    });
                }
        );
        EntityRendererRegistry.register(
                Szar.NiggerEntityType,
                NiggerEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.HitterEntityType,
                HitterEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.NaziEntityType,
                NaziEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.BULLET,
                ctx -> new FlyingItemEntityRenderer<>(ctx)
        );
        EntityRendererRegistry.register(
                Szar.EpsteinEntityType,
                EpsteinEntityRenderer::new
        );

        EntityRendererRegistry.register(
                Szar.PoliceEntityType,
                PoliceEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.TERRORIST_ENTITY_TYPE,
                TerroristEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.PLANE_ENTITY_TYPE,
                PlaneEntityRenderer::new
        );
        EntityModelLayerRegistry.registerModelLayer(
                PLANE,
                PlaneEntityModel::getTexturedModelData
        );
        EntityRendererRegistry.register(
                Szar.NyanEntityType,
                NyanEntityRenderer::new
        );
        EntityModelLayerRegistry.registerModelLayer(
                NYAN,
                NyanCatEntityModel::getTexturedModelData
        );
        EntityRendererRegistry.register(
                Szar.GYPSY_ENTITY_TYPE,
                GypsyEntityRenderer::new
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                Szar.TALL_CANNABIS_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                Szar.CANNABIS_BLOCK,
                RenderLayer.getCutout()
        );
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;
            if (!client.player.hasStatusEffect(Szar.DROG_EFFECT)) return;

            var effect = client.player.getStatusEffect(Szar.DROG_EFFECT);
            int amplifier = effect.getAmplifier(); // 0 = level I
            if (amplifier > 2) {amplifier = 2;}

            float level = amplifier + 1f;
            float time = client.player.age + tickDelta;

            /* ───── Color speed (gentle ramp) ───── */
            float speed = 0.015f + amplifier * 0.012f;
            float hue = (time * speed) % 1.0f;

            int rgb = MathHelper.hsvToRgb(hue, 0.95f, 1f);

            /* ───── Alpha (mostly stable) ───── */
            float pulse =
                    (MathHelper.sin(time * (0.04f + amplifier * 0.015f)) + 1f) * 0.5f;

            float alpha = MathHelper.clamp(
                    0.20f + amplifier * 0.10f + pulse * 0.10f,
                    0.20f,
                    0.70f
            );

            /* ───── Very subtle jitter ───── */
            float jitter = 0.15f * amplifier;
            float jitterX = (client.world.random.nextFloat() - 0.5f) * jitter;
            float jitterY = (client.world.random.nextFloat() - 0.5f) * jitter;

            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            int color =
                    ((int)(alpha * 255) << 24)
                            | (rgb & 0x00FFFFFF);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(jitterX, jitterY, 0);

            drawContext.fill(0, 0, width, height, color);

            drawContext.getMatrices().pop();

            RenderSystem.disableBlend();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!client.player.hasStatusEffect(Szar.DROG_EFFECT)) return;

            var effect = client.player.getStatusEffect(Szar.DROG_EFFECT);
            int amplifier = effect.getAmplifier();
            float level = amplifier + 1f;
            float chance = 0;
            if (level > 6) {chance = 0.20f * (level-6);}
            scrambleMovement(client, chance);
        });


        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            float scale = SmokeZoomHandler.getSmokeScale();
            if (scale > 0.51f) { // only when smoking
                client.inGameHud.spyglassScale = scale;
            }
        });

        SmokeZoomHandler.register();
        // In your mod initialization code
        FabricModelPredicateProviderRegistry.register(Szar.WEED_JOINT_ITEM, new Identifier("held"),
                (stack, world, entity, seed) -> {
                    return entity != null && entity.getMainHandStack() == stack ? 1.0f : 0.0f;
                });

    }
    private static void scrambleMovement(MinecraftClient client, float chance) {
        var options = client.options;
        long window = client.getWindow().getHandle();
        Random random = client.player.getRandom();

        KeyBinding[] movementKeys = {
                options.forwardKey,
                options.backKey,
                options.leftKey,
                options.rightKey
        };

        /* ───── Clear logical movement every tick ───── */
        for (KeyBinding key : movementKeys) {
            KeyBinding.setKeyPressed(key.getDefaultKey(), false);
        }

        /* ───── Handle each movement key ───── */
        for (KeyBinding key : movementKeys) {
            InputUtil.Key bound = key.getDefaultKey();

            if (bound.getCategory() != InputUtil.Type.KEYSYM) continue;

            boolean physicallyDown =
                    InputUtil.isKeyPressed(window, bound.getCode());

            if (physicallyDown) {
                /* ───── Key is held ───── */

                // If first tick of press → decide direction
                if (!activeScramble.containsKey(key)) {
                    KeyBinding chosen = key;

                    if (random.nextFloat() < chance) {
                        do {
                            chosen = movementKeys[random.nextInt(movementKeys.length)];
                        } while (chosen == key);
                    }

                    activeScramble.put(key, chosen);
                }

                // Apply stored direction
                KeyBinding result = activeScramble.get(key);
                KeyBinding.setKeyPressed(result.getDefaultKey(), true);

            } else {
                /* ───── Key released ───── */
                activeScramble.remove(key);
            }
        }
    }
    public enum MouseScrambleMode {
        NONE,
        INVERT_X,
        INVERT_Y,
        INVERT_BOTH
    }

}
