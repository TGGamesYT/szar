package dev.tggamesyt.szar.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import dev.tggamesyt.szar.KidEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class HybridSkinManager {

    private static final Map<Long, Identifier> CACHE = new HashMap<>();
    private static final Map<UUID, NativeImage> SKIN_CACHE = new HashMap<>();

    public static Identifier getHybridSkin(KidEntity entity) {
        long seed = entity.getHybridSeed();

        if (CACHE.containsKey(seed)) return CACHE.get(seed);

        NativeImage skinA = getParentSkin(entity.getParentA());
        NativeImage skinB = getParentSkin(entity.getParentB());

        if (skinA == null || skinB == null) return DefaultSkinHelper.getTexture();

        NativeImage result = new NativeImage(64, 64, true);
        Random r = new Random(seed);

        copyHead(result, r.nextBoolean() ? skinA : skinB);
        copyTorso(result, r.nextBoolean() ? skinA : skinB);
        copyArms(result, r.nextBoolean() ? skinA : skinB);
        copyLegs(result, r.nextBoolean() ? skinA : skinB);

        saveToDisk(result, seed);

        Identifier id = MinecraftClient.getInstance()
                .getTextureManager()
                .registerDynamicTexture("jungle_" + entity.getUuid(),
                        new NativeImageBackedTexture(result));

        CACHE.put(seed, id);
        return id;
    }

    private static NativeImage getParentSkin(UUID uuid) {
        if (uuid == null) return null;
        if (SKIN_CACHE.containsKey(uuid)) return SKIN_CACHE.get(uuid);

        MinecraftClient client = MinecraftClient.getInstance();
        NativeImage img = null;

        // 1️⃣ Try mineskin.eu
        String name = null;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry != null) name = entry.getProfile().getName();

        if (name != null) {
            try (InputStream in = new URL("https://mineskin.eu/skin/" + name).openStream()) {
                img = NativeImage.read(in);
                if (img.getWidth() != 64 || img.getHeight() != 64) img = null;
            } catch (Exception ignored) {}
        }

        // 2️⃣ Fallback to Mojang skin provider
        if (img == null) {
            try {
                GameProfile profile = entry != null ? entry.getProfile() : new GameProfile(uuid, name);
                PlayerSkinProvider provider = client.getSkinProvider();
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                        provider.getTextures(profile);
                MinecraftProfileTexture skinTexture = textures.get(MinecraftProfileTexture.Type.SKIN);

                if (skinTexture != null) {
                    Identifier id = provider.loadSkin(skinTexture, MinecraftProfileTexture.Type.SKIN);
                    try (InputStream stream = client.getResourceManager().getResource(id).get().getInputStream()) {
                        img = NativeImage.read(stream);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3️⃣ Fallback to default skin
        if (img == null) img = loadDefaultSkin(new GameProfile(uuid, name));

        SKIN_CACHE.put(uuid, img);
        return img;
    }

    private static void copyRegion(NativeImage target, NativeImage source,
                                   int x, int y, int w, int h) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                target.setColor(x + dx, y + dy, source.getColor(x + dx, y + dy));
            }
        }
    }

    private static void copyPartWithOverlay(NativeImage target, NativeImage source,
                                            int innerX, int innerY, int w, int h,
                                            int outerX, int outerY) {
        // Copy inner layer
        copyRegion(target, source, innerX, innerY, w, h);
        // Copy outer layer
        copyRegion(target, source, outerX, outerY, w, h);
    }

    // Updated coordinates as per your UV mapping
    private static void copyHead(NativeImage t, NativeImage s) {
        copyPartWithOverlay(t, s, 0, 0, 64, 16, 0, 16);
    }

    private static void copyTorso(NativeImage t, NativeImage s) {
        copyPartWithOverlay(t, s, 16, 16, 24, 32, 16, 32);
    }

    private static void copyArms(NativeImage t, NativeImage s) {
        // Right arm
        copyPartWithOverlay(t, s, 40, 16, 16, 32, 40, 32);
        // Left arm
        copyPartWithOverlay(t, s, 32, 48, 32, 16, 32, 48);
    }

    private static void copyLegs(NativeImage t, NativeImage s) {
        // Right leg
        copyPartWithOverlay(t, s, 0, 16, 16, 32, 0, 32);
        // Left leg
        copyPartWithOverlay(t, s, 0, 48, 32, 16, 0, 48);
    }


    private static void saveToDisk(NativeImage img, long seed) {
        try {
            Path path = MinecraftClient.getInstance()
                    .runDirectory.toPath()
                    .resolve("hybrid_skins");
            Files.createDirectories(path);
            File file = path.resolve(seed + ".png").toFile();
            img.writeTo(file);
        } catch (Exception ignored) {}
    }

    private static NativeImage loadDefaultSkin(GameProfile profile) {
        try {
            Identifier fallback = DefaultSkinHelper.getTexture(profile.getId());
            try (InputStream stream = MinecraftClient.getInstance()
                    .getResourceManager().getResource(fallback).get().getInputStream()) {
                return NativeImage.read(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new NativeImage(64, 64, true);
    }
}