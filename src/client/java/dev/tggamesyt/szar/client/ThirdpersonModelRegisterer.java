package dev.tggamesyt.szar.client;

import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThirdpersonModelRegisterer {
    // Maps base item ID -> custom in-hand model ID
    private static final Map<Identifier, Identifier> CUSTOM_MODELS = new HashMap<>();

    public static void register(Identifier itemId, Identifier inHandModelId) {
        CUSTOM_MODELS.put(itemId, inHandModelId);
    }

    public static Identifier get(Item item) {
        return CUSTOM_MODELS.get(Registries.ITEM.getId(item));
    }

    public static Map<Identifier, Identifier> getAll() {
        return Collections.unmodifiableMap(CUSTOM_MODELS);
    }
}