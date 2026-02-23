package dev.tggamesyt.szar.client;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.tggamesyt.szar.ServerCosmetics.NameType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static dev.tggamesyt.szar.ServerCosmetics.MOJANG_CAPES_SYNC;

public class ClientCosmetics {


    public static class CosmeticProfile {
        public NameType nameType;
        public Integer staticColor;
        public Integer gradientStart;
        public Integer gradientEnd;
        public Identifier capeTexture;
    }

    private static final Map<UUID, CosmeticProfile> PROFILES = new HashMap<>();

    public static class MojangCape {
        public String id;
        public String name;
        public String url;
    }

    public static void apply(UUID uuid, NameType type,
                             Integer staticColor,
                             Integer gradientStart,
                             Integer gradientEnd,
                             Identifier capeTexture) {

        CosmeticProfile profile = new CosmeticProfile();
        profile.nameType = type;
        profile.staticColor = staticColor;
        profile.gradientStart = gradientStart;
        profile.gradientEnd = gradientEnd;
        profile.capeTexture = capeTexture;

        PROFILES.put(uuid, profile);
    }

    public static CosmeticProfile get(UUID uuid) {
        return PROFILES.get(uuid);
    }

    public static Text buildName(UUID uuid, String name) {
        CosmeticProfile profile = PROFILES.get(uuid);
        if (profile == null) return null;

        if (profile.nameType == NameType.STATIC && profile.staticColor != null) {
            return Text.literal(name)
                    .styled(s -> s.withColor(profile.staticColor).withBold(true));
        }

        long time = Util.getMeasuringTimeMs();
        MutableText animated = Text.empty();

        for (int i = 0; i < name.length(); i++) {
            float progress = (time * 0.008f) + (i * 0.5f);
            float wave = (float) Math.sin(progress);
            float t = (wave + 1f) / 2f;

            int r = (int) MathHelper.lerp(t,
                    (profile.gradientStart >> 16) & 0xFF,
                    (profile.gradientEnd >> 16) & 0xFF);

            int g = (int) MathHelper.lerp(t,
                    (profile.gradientStart >> 8) & 0xFF,
                    (profile.gradientEnd >> 8) & 0xFF);

            int b = (int) MathHelper.lerp(t,
                    profile.gradientStart & 0xFF,
                    profile.gradientEnd & 0xFF);

            int color = (r << 16) | (g << 8) | b;

            animated.append(
                    Text.literal(String.valueOf(name.charAt(i)))
                            .styled(s -> s.withColor(color).withBold(true))
            );
        }

        return animated;
    }

    /* ---------------- FETCH MOJANG CAPES ---------------- */

    public static void fetchMojangCapes(UUID uuid) {
        try {MinecraftClient client = MinecraftClient.getInstance();
            String accessToken = client.getSession().getAccessToken();
            if (accessToken == null) return;

            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("Authorization", "Bearer " + accessToken);
            con.setRequestMethod("GET");

            InputStream in = con.getInputStream();
            String json = new String(in.readAllBytes());
            in.close();

            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonArray capes = obj.getAsJsonArray("capes");
            if (capes == null) return;

            List<MojangCape> list = new ArrayList<>();
            for (JsonElement e : capes) {
                JsonObject c = e.getAsJsonObject();
                MojangCape cape = new MojangCape();
                cape.id = c.get("id").getAsString();
                cape.name = c.has("alias") ? c.get("alias").getAsString() : cape.id;
                cape.url = c.get("url").getAsString();
                list.add(cape);
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(uuid);
            buf.writeInt(list.size());

            for (MojangCape cape : list) {
                buf.writeString(cape.id);
                buf.writeString(cape.name);
                buf.writeString(cape.url);
            }

            ClientPlayNetworking.send(MOJANG_CAPES_SYNC, buf);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String getAccessTokenFromLaunchArgs() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getSession().getAccessToken();
    }
    public static Identifier loadTextureFromURL(String url, String playerId) {
        if (url == null || url.isEmpty() || playerId == null || playerId.isEmpty()) return null;
        try (InputStream in = new URL(url).openStream()) {
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(img);
            Identifier id = new Identifier("szar", "cape_" + playerId);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}