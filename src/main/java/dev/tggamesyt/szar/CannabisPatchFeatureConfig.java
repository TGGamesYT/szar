package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import net.minecraft.world.gen.feature.FeatureConfig;

public class CannabisPatchFeatureConfig implements FeatureConfig {

    public static final Codec<CannabisPatchFeatureConfig> CODEC =
            Codec.unit(new CannabisPatchFeatureConfig());

}