package dev.tggamesyt.szar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancement.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class Szar implements ModInitializer {

    public static final String MOD_ID = "szar";

    public static final Block SZAR_BLOCK =
            new SzarBlock();
    public static final Block FASZ_BLOCK =
            new FaszBlock();
    public static final Identifier NWORDPACKET =
            new Identifier("szar", "nwordpacket");

    @Override
    public void onInitialize() {

        // register block
        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "cigany"),
                SZAR_BLOCK
        );

        // register item so you can hold it
        Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "cigany"),
                new BlockItem(SZAR_BLOCK, new Item.Settings())
        );

        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "fasz"),
                FASZ_BLOCK
        );

        // register item so you can hold it
        Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "fasz"),
                new FaszItem(FASZ_BLOCK, new Item.Settings())
        );

        ServerMessageDecoratorEvent.EVENT.register((player, message) -> CompletableFuture.completedFuture(
                filterMessage(player, message)
        ));

        FabricDefaultAttributeRegistry.register(
                NiggerEntityType,
                NiggerEntity.createAttributes()
        );
    }
public static final Item NWORD_PASS = Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "nwordpass"),
                new NwordPassItem(new Item.Settings())
        );
    public static final EntityType<NiggerEntity> NiggerEntityType =
                Registry.register(
                        Registries.ENTITY_TYPE,
                        new Identifier("szar", "nigger"),
                        FabricEntityTypeBuilder
                                .create(SpawnGroup.CREATURE, NiggerEntity::new)
                                .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                                .build()
                );
    private static final List<String> FORBIDDEN_WORDS = List.of(
            "nigger",
            "niger",
            "niga"
    );
    private static Text filterMessage(ServerPlayerEntity player, Text original) {

        // If player has the advancement, do nothing
        if (hasAdvancement(player)) {
            return original;
        }

        String filtered = original.getString();

        boolean censoredAnything = false;

        for (String word : FORBIDDEN_WORDS) {

            Pattern pattern = Pattern.compile(
                    "(?i)\\b" + Pattern.quote(word) + "\\b"
            );

            if (pattern.matcher(filtered).find()) {
                filtered = pattern.matcher(filtered)
                        .replaceAll("******");
                censoredAnything = true;
            }
        }

        // Send warning once per message
        if (censoredAnything) {
            player.sendMessage(
                    Text.literal("Nincs N-Word Pass-ed!")
                            .formatted(Formatting.RED),
                    false
            );
        }

        return Text.literal(filtered);
    }


    private static boolean hasAdvancement(ServerPlayerEntity player) {

        Advancement advancement = player
                .getServer()
                .getAdvancementLoader()
                .get(new Identifier("szar", "nwordpass"));

        if (advancement == null) return false;

        return player
                .getAdvancementTracker()
                .getProgress(advancement)
                .isDone();
    }


}
