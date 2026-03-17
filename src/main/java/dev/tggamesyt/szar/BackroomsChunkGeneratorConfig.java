package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class BackroomsChunkGeneratorConfig {

    public static final Codec<BackroomsChunkGeneratorConfig> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("floor_y").forGetter(c -> c.floorY)
            ).apply(instance, BackroomsChunkGeneratorConfig::new));

    public static final BackroomsChunkGeneratorConfig DEFAULT =
            new BackroomsChunkGeneratorConfig(4);

    public final int floorY;

    public BackroomsChunkGeneratorConfig(int floorY) {
        this.floorY = floorY;
    }
}