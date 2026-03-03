package dev.tggamesyt.szar;


import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Random;

public enum SlotSymbol {
    SEVEN(Items.DEEPSLATE_EMERALD_ORE),
    BELL(Items.BELL),
    APPLE(Items.APPLE),
    SWEET_BERRIES(Items.SWEET_BERRIES),
    GLOW_BERRIES(Items.GLOW_BERRIES),
    MELON_SLICE(Items.MELON_SLICE),
    CHORUS_FRUIT(Items.CHORUS_FRUIT);

    public final Item item;

    SlotSymbol(Item item) {
        this.item = item;
    }

    // Roll a random symbol according to the specified probabilities
    public static SlotSymbol roll(Random random) {
        float r = random.nextFloat();
        if (r < 0.0255f) return SEVEN;          // 2.55%
        else if (r < 0.0255f + 0.101f) return BELL; // 10.1%
        else {
            return rollFruit(random);
        }
    }

    public static SlotSymbol rollFruit(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> APPLE;
            case 1 -> SWEET_BERRIES;
            case 2 -> GLOW_BERRIES;
            case 3 -> MELON_SLICE;
            default -> CHORUS_FRUIT;
        };
    }

    public static SlotSymbol intToSymbol(int num) {
        return SlotSymbol.values()[num];
    };

    public static int symbolToInt(SlotSymbol num) {
        return num.ordinal();
    };
}