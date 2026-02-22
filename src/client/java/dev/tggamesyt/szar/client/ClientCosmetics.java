package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientCosmetics {

    public enum NameType {
        STATIC,
        GRADIENT
    }

    public static class CosmeticProfile {
        public final NameType nameType;
        public final Formatting staticColor;   // for STATIC names
        public final Identifier capeTexture;   // optional cape
        public final int gradientStart;        // RGB int, for GRADIENT
        public final int gradientEnd;          // RGB int, for GRADIENT

        public CosmeticProfile(NameType nameType, Formatting staticColor, Identifier capeTexture,
                               int gradientStart, int gradientEnd) {
            this.nameType = nameType;
            this.staticColor = staticColor;
            this.capeTexture = capeTexture;
            this.gradientStart = gradientStart;
            this.gradientEnd = gradientEnd;
        }
    }

    // Registry
    private static final Map<UUID, CosmeticProfile> PROFILES = new HashMap<>();

    static {
        // ===== TGdoesCode ===== animated gradient
        PROFILES.put(
                UUID.fromString("20bbb23e-2f22-46ba-b201-c6bd435b445b"),
                new CosmeticProfile(
                        NameType.GRADIENT,
                        null,
                        new Identifier(Szar.MOD_ID, "textures/etc/tg_cape.png"),
                        0x8CD6FF,   // light blue
                        0x00FFFF    // cyan
                )
        );

        // ===== Berci08ur_mom =====
        PROFILES.put(
                UUID.fromString("dda61748-15a4-45ff-9eea-29efc99c1711"),
                new CosmeticProfile(
                        NameType.STATIC,
                        Formatting.GREEN,
                        new Identifier(Szar.MOD_ID, "textures/etc/gold_cape.png"),
                        0, 0
                )
        );

        // ===== gabri =====
        PROFILES.put(
                UUID.fromString("52af5540-dd18-4ad9-9acb-50eb11531180"),
                new CosmeticProfile(
                        NameType.STATIC,
                        Formatting.RED,
                        new Identifier(Szar.MOD_ID, "textures/etc/gbr_cape.png"),
                        0, 0
                )
        );
    }

    public static CosmeticProfile getProfile(UUID uuid) {
        return PROFILES.get(uuid);
    }

    public static Text buildName(UUID uuid, String name) {
        CosmeticProfile profile = PROFILES.get(uuid);
        if (profile == null) return null;

        if (profile.nameType == NameType.STATIC) {
            return Text.literal(name).formatted(profile.staticColor, Formatting.BOLD);
        }

        // GRADIENT animation
        long time = Util.getMeasuringTimeMs();
        MutableText animated = Text.empty();

        for (int i = 0; i < name.length(); i++) {
            // Animate wave
            float progress = (time * 0.08f) + (i * 1.2f);
            float wave = (float) Math.sin(progress);
            float t = (wave + 1f) / 2f; // 0..1

            // Interpolate RGB
            int r = (int) MathHelper.lerp(t, (profile.gradientStart >> 16) & 0xFF, (profile.gradientEnd >> 16) & 0xFF);
            int g = (int) MathHelper.lerp(t, (profile.gradientStart >> 8) & 0xFF, (profile.gradientEnd >> 8) & 0xFF);
            int b = (int) MathHelper.lerp(t, profile.gradientStart & 0xFF, profile.gradientEnd & 0xFF);

            int color = (r << 16) | (g << 8) | b;

            animated.append(
                    Text.literal(String.valueOf(name.charAt(i)))
                            .styled(style -> style.withColor(color).withBold(true))
            );
        }

        return animated;
    }
}