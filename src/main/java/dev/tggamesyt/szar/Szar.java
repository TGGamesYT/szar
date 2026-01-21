package dev.tggamesyt.szar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancement.Advancement;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class Szar implements ModInitializer {

    public static final String MOD_ID = "szar";

    public static final Block SZAR_BLOCK =
            new SzarBlock();
    public static final Block NIGGERITEBLOCK =
            new Block(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK));
    public static final Block FASZ_BLOCK =
            new FaszBlock();
    public static final Identifier TOTEMPACKET =
            new Identifier(MOD_ID, "nwordpacket");
    public static final ItemGroup SZAR_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "szar_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.szar_group"))
                    .icon(() -> new ItemStack(Szar.CIGANYBLOCK)) // icon item
                    .entries((displayContext, entries) -> {
                        entries.add(Szar.CIGANYBLOCK);
                        entries.add(Szar.FASZITEM);
                        entries.add(Szar.NWORD_PASS);
                        entries.add(Szar.NIGGER_SPAWNEGG);
                        entries.add(Szar.GYPSY_SPAWNEGG);
                        entries.add(Szar.CANNABIS_ITEM);
                        entries.add(Szar.WEED_ITEM);
                        entries.add(Szar.WEED_JOINT_ITEM);
                        entries.add(Szar.NIGGERITE_INGOT);
                        entries.add(Szar.NIGGERITE_SWORD);
                        entries.add(Szar.NIGGERITE_AXE);
                        entries.add(Szar.NIGGERITE_PICKAXE);
                        entries.add(Szar.NIGGERITE_SHOVEL);
                        entries.add(Szar.NIGGERITE_HOE);
                        entries.add(Szar.NIGGERITE_HELMET);
                        entries.add(Szar.NIGGERITE_CHESTPLATE);
                        entries.add(Szar.NIGGERITE_LEGGINGS);
                        entries.add(Szar.NIGGERITE_BOOTS);
                        entries.add(Szar.NIGGERITE_BLOCK);
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
                new Identifier(MOD_ID, "niggerite_block"),
                NIGGERITEBLOCK
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
        FabricDefaultAttributeRegistry.register(
                GYPSY_ENTITY_TYPE,
                GypsyEntity.createAttributes()
        );
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (var entity : world.getPlayers()) {
                if (entity.getHealth() <= 0f) {
                    tryDrugTotem(entity);
                }
            }
        });
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
    public static final Item NIGGERITE_INGOT = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_ingot"),
            new Item(new FabricItemSettings().fireproof())
    );
    public static final Item NIGGERITE_SWORD = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_sword"),
            new SwordItem(
                    NiggeriteMaterial.INSTANCE,
                    4,
                    -2.4F,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_AXE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_axe"),
            new AxeItem(
                    NiggeriteMaterial.INSTANCE,
                    4,
                    -2.4F,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_PICKAXE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_pickaxe"),
            new PickaxeItem(
                    NiggeriteMaterial.INSTANCE,
                    4,
                    -2.4F,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_HOE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_hoe"),
            new HoeItem(
                    NiggeriteMaterial.INSTANCE,
                    4,
                    -2.4F,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_SHOVEL = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_shovel"),
            new ShovelItem(
                    NiggeriteMaterial.INSTANCE,
                    4,
                    -2.4F,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_HELMET = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_helmet"),
            new ArmorItem(
                    NiggeriteArmorMaterial.INSTANCE,
                    ArmorItem.Type.HELMET,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_CHESTPLATE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_chestplate"),
            new ArmorItem(
                    NiggeriteArmorMaterial.INSTANCE,
                    ArmorItem.Type.CHESTPLATE,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_LEGGINGS = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_leggings"),
            new ArmorItem(
                    NiggeriteArmorMaterial.INSTANCE,
                    ArmorItem.Type.LEGGINGS,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item NIGGERITE_BOOTS = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_boots"),
            new ArmorItem(
                    NiggeriteArmorMaterial.INSTANCE,
                    ArmorItem.Type.BOOTS,
                    new FabricItemSettings().fireproof()
            )
    );
    public static final Item WEED_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "weed"),
            new Item(new Item.Settings())
    );
    public static final Item WEED_JOINT_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "weed_joint"),
            new Joint(new Item.Settings())
    );
    public static final Item CIGANYBLOCK = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "cigany"),
            new BlockItem(SZAR_BLOCK, new Item.Settings())
    );
    public static final Item NIGGERITE_BLOCK = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "niggerite_block"),
            new BlockItem(NIGGERITEBLOCK, new Item.Settings())
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
    public static final EntityType<GypsyEntity> GYPSY_ENTITY_TYPE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "gypsy"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, GypsyEntity::new)
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
    public static final Item GYPSY_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "gypsy_spawn_egg"),
            new SpawnEggItem(
                    GYPSY_ENTITY_TYPE,
                    0x964B00,
                    0xF1C27D,
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
    public static boolean tryDrugTotem(PlayerEntity player) {
        StatusEffectInstance effect = player.getStatusEffect(Szar.DROG_EFFECT);
        if (effect == null || effect.getAmplifier() < 5) return false;

        // Only trigger if holding Joint
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof Joint)) return false;

        World world = player.getWorld();

        // Prevent death
        player.setHealth(1f);

        // Clear negative effects
        player.clearStatusEffects();

        // Vanilla totem effects
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));

        // Sound
        player.playSound(SoundEvents.ITEM_TOTEM_USE, 1f, 1f);

        // Animation via packet
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeItemStack(stack);
            ServerPlayNetworking.send(serverPlayer, Szar.TOTEMPACKET, buf);
        }

        // Reduce drug level safely
        int duration = effect.getDuration();
        int amplifier = effect.getAmplifier();
        player.addStatusEffect(new StatusEffectInstance(
                Szar.DROG_EFFECT,
                duration,
                Math.max(0, amplifier - 2),
                false,
                true,
                true
        ));

        return true;
    }

}
