package dev.tggamesyt.szar;


import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum SlotSymbol {
    SEVEN(Items.ENCHANTED_GOLDEN_APPLE),
    BELL(Items.GOLDEN_APPLE),
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
    public static SlotSymbol roll(java.util.Random random) {
        float r = random.nextFloat();
        if (r < 0.0255f) return SEVEN;          // 2.55%
        else if (r < 0.0255f + 0.101f) return BELL; // 10.1%
        else {
            // 5 fruits, equally likely
            int fruitIndex = random.nextInt(5);
            switch (fruitIndex) {
                case 0: return APPLE;
                case 1: return SWEET_BERRIES;
                case 2: return GLOW_BERRIES;
                case 3: return MELON_SLICE;
                default: return CHORUS_FRUIT;
            }
        }
    }
}