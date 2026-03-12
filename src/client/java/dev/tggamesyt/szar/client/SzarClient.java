package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.*;
import dev.tggamesyt.szar.ServerCosmetics.NameType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Unique;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static dev.tggamesyt.szar.ServerCosmetics.SYNC_PACKET;
import static dev.tggamesyt.szar.Szar.*;
import static dev.tggamesyt.szar.client.ClientCosmetics.loadTextureFromURL;
import static dev.tggamesyt.szar.client.UraniumUtils.updateUranium;

public class SzarClient implements ClientModInitializer {
    // add this field to your client init class
    public static final int april = 4;
    public static final int fools = 1;
    private float drogOverlayProgress = 0.0F;
    private long lastTime = 0;
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
    public static final EntityModelLayer ATOM =
            new EntityModelLayer(
                    new Identifier(Szar.MOD_ID, "atom"),
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

    public static final KeyBinding SPIN_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.szar.spin", InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R, "key.categories.szar")
    );
    @Override
    public void onInitializeClient() {

// Then in a ClientTickEvents.END_CLIENT_TICK:
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (SPIN_KEY.wasPressed() && client.player != null) {
                ItemStack stack = client.player.getMainHandStack();
                if (stack.isOf(Szar.REVOLVER)) {
                    // Send spin packet to server
                    ClientPlayNetworking.send(Szar.REVOLVER_SPIN,
                            PacketByteBufs.create());
                }
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PacketByteBuf buf = PacketByteBufs.create();

            // Write each setting as: id (string), value (boolean)
            var settings = ModConfig.allSettings();
            buf.writeInt(settings.size());
            for (ConfigEntry entry : settings) {
                buf.writeString(entry.id);
                buf.writeBoolean(entry.get());
            }

            ClientPlayNetworking.send(Szar.CONFIG_SYNC, buf);
        });
        ModSettings.init(); // register all settings & presets FIRST
        ModConfig.load();       // then load saved values

        ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(MOD_ID, "nsfw"),
                FabricLoader.getInstance().getModContainer(MOD_ID).get(),
                Text.literal("NSFW Censorship"),
                ResourcePackActivationType.NORMAL
        );
        ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(MOD_ID, "racist"),
                FabricLoader.getInstance().getModContainer(MOD_ID).get(),
                Text.literal("Racism Censorship"),
                ResourcePackActivationType.NORMAL
        );
        EntityRendererRegistry.register(Szar.RADIATION_AREA, EmptyEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(Szar.PLAY_VIDEO,
                (client, handler, buf, responseSender) -> {
                    String player = buf.readString();
                    client.execute(() -> {
                        VideoManager.startVideo(player);
                    });

                });
        VideoManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VideoManager.tick();
        });
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> {
            ThirdpersonModelRegisterer.getAll().forEach((itemId, modelId) -> {
                out.accept(new ModelIdentifier(modelId, "inventory"));
            });
        });
        ThirdpersonModelRegisterer.register(new Identifier(MOD_ID, "weed_joint"), new Identifier(MOD_ID, "weed_joint_in_hand"));
        ThirdpersonModelRegisterer.register(new Identifier(MOD_ID, "fasz"), new Identifier(MOD_ID, "fasz_in_hand"));
        ThirdpersonModelRegisterer.register(new Identifier(MOD_ID, "slot_machine"), new Identifier(MOD_ID, "slot_machine_3d"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean forward = client.options.attackKey.isPressed();
            boolean backward = client.options.useKey.isPressed();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(forward);
            buf.writeBoolean(backward);

            ClientPlayNetworking.send(PlayerMovementManager.PACKET_ID, buf);
        });
        ClientPlayNetworking.registerGlobalReceiver(SYNC_PACKET, (client, handler, buf, responseSender) -> {
            // First read the player UUID
            UUID playerUuid = buf.readUuid();

            // Read cosmetic data
            NameType nameType = buf.readEnumConstant(NameType.class);
            Integer staticColor = buf.readBoolean() ? buf.readInt() : null;
            Integer gradientStart = buf.readBoolean() ? buf.readInt() : null;
            Integer gradientEnd = gradientStart != null ? buf.readInt() : null;

            String textureUrl = buf.readString();
            Identifier capeTexture = loadTextureFromURL(textureUrl, playerUuid.toString());

            // Apply the cosmetic profile on the main thread
            client.execute(() -> {
                ClientCosmetics.fetchMojangCapes(playerUuid);
                ClientCosmetics.apply(playerUuid, nameType, staticColor, gradientStart, gradientEnd, capeTexture);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Szar.OPEN_MERL_SCREEN,
                (client, handler, buf, responseSender) -> {
                    int entityId = buf.readInt();

                    client.execute(() -> {
                        client.setScreen(new MerlQuestionScreen(entityId));
                    });
                });
        SzarTosHandler.checkAndShow();
        ClientPlayNetworking.registerGlobalReceiver(Szar.OPEN_URL,
                (client, handler, buf, responseSender) -> {
                    assert client.player != null;
                    if (PlayerConfigStore.get(client.player.getUuid(), "nsfw")) {return;}
                    String url = "https://files.tggamesyt.dev/f/1770574109164-655298600-2022.03.17-1%20Exhibit%201.pdf";
                    // maybe https://www.justice.gov/epstein/doj-disclosures

                    client.execute(() -> {
                        Util.getOperatingSystem().open(url);
                    });
                }
        );
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
        /*BlockEntityRendererRegistry.register(
                SLOT_MACHINE_BLOCKENTITY,
                SlotMachineRenderer::new
        );*/
        HandledScreens.register(Szar.SLOT_MACHINE_SCREEN_HANDLER_TYPE, SlotMachineScreen::new);
        HandledScreens.register(Szar.ROULETTE_SCREEN_HANDLER_TYPE, RouletteScreen::new);

        EntityRendererRegistry.register(
                Szar.NiggerEntityType,
                NiggerEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.Kid,
                KidRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.AtomEntityType,
                AtomEntityRenderer::new
        );
        EntityModelLayerRegistry.registerModelLayer(
                ATOM,
                Atom::getTexturedModelData
        );
        EntityRendererRegistry.register(
                Szar.HitterEntityType,
                HitterEntityRenderer::new
        );
        EntityRendererRegistry.register(
                Szar.MerlEntityType,
                MerlEntityRenderer::new
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
        BlockRenderLayerMap.INSTANCE.putBlock(
                ROULETTE_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                SLOT_MACHINE_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                C_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                A_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                S_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                I_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                N_BLOCK,
                RenderLayer.getCutout()
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                O_BLOCK,
                RenderLayer.getCutout()
        );
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            boolean hasEffect = client.player.hasStatusEffect(Szar.DROG_EFFECT);

            // ease in/out — 0.02F controls speed, lower = slower transition
            if (hasEffect) {
                drogOverlayProgress = Math.min(1.0F, drogOverlayProgress + tickDelta * 0.02F);
            } else {
                drogOverlayProgress = Math.max(0.0F, drogOverlayProgress - tickDelta * 0.02F);
            }

            if (drogOverlayProgress <= 0.0F) return;

            // S-curve easing so it accelerates then decelerates
            float eased = drogOverlayProgress * drogOverlayProgress * (3.0F - 2.0F * drogOverlayProgress);

            var effect = hasEffect ? client.player.getStatusEffect(Szar.DROG_EFFECT) : null;
            int amplifier = effect != null ? Math.min(effect.getAmplifier(), 2) : 0;

            float time = client.player.age + tickDelta;

            float speed = 0.015f + amplifier * 0.012f;
            float hue = (time * speed) % 1.0f;

            int rgb = MathHelper.hsvToRgb(hue, 0.95f, 1f);

            float pulse =
                    (MathHelper.sin(time * (0.04f + amplifier * 0.015f)) + 1f) * 0.5f;

            // multiply alpha by eased so it fades in/out smoothly
            float alpha = MathHelper.clamp(
                    (0.20f + amplifier * 0.10f + pulse * 0.10f) * eased,
                    0.0f,
                    0.70f
            );

            // jitter also scales with eased so it doesn't pop in suddenly
            float jitter = 0.15f * amplifier * eased;
            float jitterX = (client.world.random.nextFloat() - 0.5f) * jitter;
            float jitterY = (client.world.random.nextFloat() - 0.5f) * jitter;

            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            int color = ((int)(alpha * 255) << 24) | (rgb & 0x00FFFFFF);

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

        // In your mod initialization code
        FabricModelPredicateProviderRegistry.register(Szar.WEED_JOINT_ITEM, new Identifier("held"),
                (stack, world, entity, seed) -> entity != null && entity.getMainHandStack() == stack ? 1.0f : 0.0f);
        if (isDebugEnabled()) {
            ClientCommandRegistrationCallback.EVENT.register(
                    (dispatcher, registryAccess) -> PanoramaClientCommand.register(dispatcher)
            );
        }
    }
    private boolean isDebugEnabled() {

        MinecraftClient client = MinecraftClient.getInstance();

        File configDir = new File(client.runDirectory, "config");
        File debugFile = new File(configDir, "panodebugmode.txt");

        if (!debugFile.exists()) {
            return false;
        }

        try {
            String content = Files.readString(debugFile.toPath(), StandardCharsets.UTF_8)
                    .trim();

            return content.equals("true");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
