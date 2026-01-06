package dev.tggamesyt.szar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancement.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.*;
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
            new Identifier(MOD_ID, "nwordpacket");
    public static final ItemGroup SZAR_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier("modid", "szar_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.szar_group"))
                    .icon(() -> new ItemStack(Szar.CIGANYBLOCK)) // icon item
                    .entries((displayContext, entries) -> {
                        entries.add(Szar.CIGANYBLOCK);
                        entries.add(Szar.FASZITEM);
                        entries.add(Szar.NWORD_PASS);
                        entries.add(Szar.NIGGER_SPAWNEGG);
                        entries.add(Szar.CANNABIS_ITEM);
                        entries.add(Szar.WEED_ITEM);
                    })
                    .build()
    );
    @Override
    public void onInitialize() {

        // register block
        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "cigany"),
                SZAR_BLOCK
        );


        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "fasz"),
                FASZ_BLOCK
        );

        ServerMessageDecoratorEvent.EVENT.register((player, message) -> CompletableFuture.completedFuture(
                filterMessage(player, message)
        ));

        FabricDefaultAttributeRegistry.register(
                NiggerEntityType,
                NiggerEntity.createAttributes()
        );
    }
    public static final StatusEffect DROG_EFFECT = Registry.register(
            Registries.STATUS_EFFECT,
            new Identifier(MOD_ID, "drog"),
            new DrogEffect()
    );
    public static final Block CANNABIS_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(MOD_ID, "cannabis"),
            new TallPlantBlock(
                    FabricBlockSettings.copyOf(Blocks.LARGE_FERN)
            )
    );
    public static final Item CANNABIS_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "cannabis"),
            new BlockItem(
                    CANNABIS_BLOCK,
                    new Item.Settings()
            )
    );
    public static final Item WEED_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "weed"),
            new Item(new Item.Settings())
    );
    public static final Item CIGANYBLOCK = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "cigany"),
            new BlockItem(SZAR_BLOCK, new Item.Settings())
    );
    public static final Item FASZITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "fasz"),
            new FaszItem(FASZ_BLOCK, new Item.Settings())
    );
    public static final Item NWORD_PASS = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nwordpass"),
            new NwordPassItem(new Item.Settings())
    );
    public static final EntityType<NiggerEntity> NiggerEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "nigger"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, NiggerEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final Item NIGGER_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nigger_spawn_egg"),
            new SpawnEggItem(
                    NiggerEntityType,
                    0x964B00,
                    0x654321,
                    new Item.Settings()
            )
    );

    private static final List<String> FORBIDDEN_WORDS = List.of(
            "nigger",
            "niger",
            "niga",
            "nigga",
            "neger",
            "néger"
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
                .get(new Identifier(MOD_ID, "nwordpass"));

        if (advancement == null) return false;

        return player
                .getAdvancementTracker()
                .getProgress(advancement)
                .isDone();
    }


}
