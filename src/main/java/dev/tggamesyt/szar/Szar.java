package dev.tggamesyt.szar;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.advancement.Advancement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.poi.PointOfInterestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static dev.tggamesyt.szar.ServerCosmetics.USERS;
import static dev.tggamesyt.szar.ServerCosmetics.sync;

public class Szar implements ModInitializer {
    public static final String MOD_ID = "szar";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static MinecraftServer SERVER;
    public static final SoundEvent MERL_SOUND =
            SoundEvent.of(new Identifier("szar", "merl"));
    public static final Identifier PLANE_ANIM_PACKET =
            new Identifier(MOD_ID, "plane_anim");
    public static final Identifier NAZI_HAND_GESTURE = new Identifier("szar", "hit_hand");
    public static final Identifier OPEN_URL = new Identifier(MOD_ID, "epsteinfiles");

    public static final Block SZAR_BLOCK =
            new SzarBlock();
    public static final Block URANIUM_BLOCK =
            new RadiatedBlock(
                    FabricBlockSettings.create()
                            .strength(20.0f, 1200.0f).requiresTool()
            );
    // ConfiguredFeature Key
    public static final RegistryKey<ConfiguredFeature<?, ?>> URANIUM_ORE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MOD_ID, "uranium_ore"));

    // PlacedFeature Key
    public static final RegistryKey<PlacedFeature> URANIUM_ORE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "uranium_ore_placed"));

    public static final TrackedData<Long> LAST_CRIME_TICK =
            DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.LONG);
    public static final Block NIGGERITEBLOCK =
            new Block(AbstractBlock.Settings.copy(Blocks.NETHERITE_BLOCK));
    public static final Block FASZ_BLOCK =
            new FaszBlock();
    public static final Identifier TOTEMPACKET =
            new Identifier(MOD_ID, "nwordpacket");
    public static final Identifier OPEN_MERL_SCREEN =
            new Identifier(MOD_ID, "open_merl_screen");
    public static final Identifier MERL_QUESTION =
            new Identifier("szar", "merl_question");
    public static final Block CHEMICAL_WORKBENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS));
    public static final RegistryKey<PointOfInterestType> CHEMICAL_WORKBENCH_POI_KEY =
            RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, new Identifier(MOD_ID, "chemical_workbench_poi"));
    public static PointOfInterestType CHEMICAL_WORKBENCH_POI =
            PointOfInterestHelper.register(new Identifier(MOD_ID, "chemical_workbench_poi"), 1, 1, CHEMICAL_WORKBENCH);

    public static final RegistryKey<DamageType> BULLET_DAMAGE =
            RegistryKey.of(
                    RegistryKeys.DAMAGE_TYPE,
                    new Identifier(MOD_ID, "bullet")
            );
    public static VillagerProfession DROG_DEALER = Registry.register(
            Registries.VILLAGER_PROFESSION,
            new Identifier(MOD_ID, "drog_dealer"),
            new VillagerProfession(
                    "drog_dealer",
                    entry -> entry.matchesKey(CHEMICAL_WORKBENCH_POI_KEY),
                    entry -> entry.matchesKey(CHEMICAL_WORKBENCH_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.ENTITY_VILLAGER_WORK_CLERIC
            )
    );
    public static final EntityType<NyanEntity> NyanEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "nyan_cat"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, NyanEntity::new)
                            .dimensions(EntityDimensions.fixed(1.0F, 1.4F))
                            .build()
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
    public static final EntityType<KidEntity> Kid =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "kid"),
                    FabricEntityTypeBuilder.create(SpawnGroup.CREATURE,
                                    KidEntity::new) // ✅ matches EntityType<KidEntity>
                            .dimensions(EntityDimensions.changing(0.6F, 1.8F))
                            .build()
            );
    public static final EntityType<EpsteinEntity> EpsteinEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "epstein"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, EpsteinEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final EntityType<HitterEntity> HitterEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "hitler"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, HitterEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final EntityType<MerlEntity> MerlEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "merl"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, MerlEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final EntityType<NaziEntity> NaziEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "nazi"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, NaziEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final EntityType<PoliceEntity> PoliceEntityType =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "police"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, PoliceEntity::new)
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
    public static final EntityType<PlaneEntity> PLANE_ENTITY_TYPE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "plane"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, PlaneEntity::new)
                            .dimensions(EntityDimensions.fixed(5.0F, 2.0F))
                            .build()
            );
    public static final EntityType<IslamTerrorist> TERRORIST_ENTITY_TYPE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "islam_terrorist"),
                    FabricEntityTypeBuilder
                            .create(SpawnGroup.CREATURE, IslamTerrorist::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // player-sized
                            .build()
            );
    public static final ItemGroup SZAR_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "szar_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.szar_group"))
                    .icon(() -> new ItemStack(Szar.CIGANYBLOCK)) // icon item
                    .entries((displayContext, entries) -> {
                        // drugs
                        entries.add(Szar.CANNABIS_ITEM);
                        entries.add(Szar.WEED_ITEM);
                        entries.add(Szar.WEED_JOINT_ITEM);
                        entries.add(Szar.CHEMICAL_WORKBENCH_ITEM);
                        // racism
                        entries.add(Szar.CIGANYBLOCK);
                        entries.add(Szar.NWORD_PASS);
                        entries.add(Szar.HITTER_SPAWNEGG);
                        entries.add(Szar.NAZI_SPAWNEGG);
                        entries.add(Szar.NIGGER_SPAWNEGG);
                        entries.add(Szar.GYPSY_SPAWNEGG);
                        entries.add(Szar.TERRORIST_SPAWNEGG);
                        entries.add(Szar.POLICE_SPAWNEGG);
                        entries.add(Szar.KEY_ITEM);
                        entries.add(Szar.HANDCUFF_ITEM);
                        // crazy weponary
                        entries.add(Szar.AK_AMMO);
                        entries.add(Szar.AK47);
                        entries.add(Szar.ATOM_DETONATOR);
                        entries.add(Szar.URANIUM_ORE);
                        entries.add(Szar.URANIUM);
                        entries.add(Szar.URANIUM_ROD);
                        entries.add(Szar.ATOM_CORE);
                        entries.add(Szar.ATOM);
                        entries.add(Szar.WHEEL);
                        entries.add(Szar.PLANE);
                        // random ahh silly stuff
                        entries.add(Szar.POPTART);
                        entries.add(Szar.NYAN_SPAWNEGG);
                        entries.add(Szar.EPSTEIN_FILES);
                        entries.add(Szar.EPSTEIN_SPAWNEGG);
                        entries.add(Szar.BAITER_DISK);
                        entries.add(Szar.MERL_SPAWNEGG);
                        entries.add(Szar.EFN_DISK);
                        // nsfw
                        entries.add(Szar.FASZITEM);
                        entries.add(Szar.CNDM);
                        entries.add(Szar.LATEX);
                        entries.add(Szar.WHITE_LIQUID);
                        // niggerite shits at the end
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
    private final Map<UUID, BlockPos> sleepingPlayers = new HashMap<>();
    @Override
    public void onInitialize() {
        PlayerMovementManager.init();
        ServerCosmetics.init();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            ServerCosmetics.UserCosmetics user = USERS.get(player.getUuid());
            if (user != null) {

                // AUTO SELECT FIRST CAPE IF NONE SELECTED
                if (user.selectedCape == null && !user.ownedCapes.isEmpty()) {
                    user.selectedCape = user.ownedCapes.get(0);
                } else {
                    user.selectedCape = null;
                }

                sync(player, user);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER = null;
        });
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
                new Identifier(MOD_ID, "uranium_ore"),
                URANIUM_BLOCK
        );

        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "fasz"),
                FASZ_BLOCK
        );
        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "chemical_workbench"),
                CHEMICAL_WORKBENCH
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                1, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.EMERALD, 3),
                                    new ItemStack(Items.PAPER, 6),
                                    12,   // max uses
                                    2,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                1, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.SUGAR_CANE, 6),
                                    new ItemStack(Items.EMERALD, 1),
                                    10,   // max uses
                                    2,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                2, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.EMERALD, 10),
                                    new ItemStack(CANNABIS_ITEM, 1),
                                    20,   // max uses
                                    4,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                3, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.EMERALD, 15),
                                    new ItemStack(WEED_ITEM, 1),
                                    16,   // max uses
                                    8,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                3, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.EMERALD, 64),
                                    new ItemStack(WEED_JOINT_ITEM, 1),
                                    5,   // max uses
                                    12,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                4, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(Items.EMERALD, 4),
                                    new ItemStack(Items.CAMPFIRE, 1),
                                    16,   // max uses
                                    10,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );
        TradeOfferHelper.registerVillagerOffers(
                DROG_DEALER,
                5, // villager level
                factories -> {
                    factories.add((entity, random) ->
                            new TradeOffer(
                                    new ItemStack(NIGGERITE_INGOT, 10),
                                    new ItemStack(WEED_JOINT_ITEM, 1),
                                    12,   // max uses
                                    2,    // villager XP
                                    0.05f // price multiplier
                            )
                    );
                }
        );

        ServerMessageDecoratorEvent.EVENT.register((player, message) -> CompletableFuture.completedFuture(
                filterMessage(player, message)
        ));

        FabricDefaultAttributeRegistry.register(
                NiggerEntityType,
                NiggerEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                Kid,
                KidEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                EpsteinEntityType,
                NiggerEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                NyanEntityType,
                NyanEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                NaziEntityType,
                NaziEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                HitterEntityType,
                HitterEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                MerlEntityType,
                MerlEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                PoliceEntityType,
                PoliceEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                GYPSY_ENTITY_TYPE,
                GypsyEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                TERRORIST_ENTITY_TYPE,
                IslamTerrorist.createAttributes()
        );
        SpawnRestriction.register(
                Szar.PoliceEntityType,
                SpawnRestriction.Location.ON_GROUND,      // spawn on solid blocks
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, // avoids leaves
                PoliceEntity::canSpawnHere                  // your custom condition
        );
        ServerTickEvents.END_SERVER_TICK.register(PlayerValueTimer::onServerTick);
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.DESERT,
                        BiomeKeys.BADLANDS,
                        BiomeKeys.ERODED_BADLANDS,
                        BiomeKeys.WOODED_BADLANDS
                ),
                SpawnGroup.MONSTER,
                TERRORIST_ENTITY_TYPE,
                10, // weight (lower = rarer)
                1,  // min group size
                2   // max group size
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.JUNGLE,
                        BiomeKeys.BAMBOO_JUNGLE,
                        BiomeKeys.SPARSE_JUNGLE
                ),
                SpawnGroup.MONSTER,
                NiggerEntityType,
                5, // weight (lower = rarer)
                1,  // min group size
                2   // max group size
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.WINDSWEPT_GRAVELLY_HILLS, BiomeKeys.STONY_PEAKS),
                SpawnGroup.MONSTER,
                HitterEntityType,
                1, 1, 1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.FOREST, BiomeKeys.FLOWER_FOREST),
                SpawnGroup.MONSTER,
                MerlEntityType,
                1, 1, 1
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.JUNGLE, BiomeKeys.BAMBOO_JUNGLE, BiomeKeys.SPARSE_JUNGLE),
                SpawnGroup.MONSTER,
                GYPSY_ENTITY_TYPE,
                5, 1, 5
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.FOREST, BiomeKeys.FLOWER_FOREST),
                SpawnGroup.AMBIENT,
                NyanEntityType,
                1, 1, 1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(BiomeKeys.FOREST, BiomeKeys.FLOWER_FOREST),
                SpawnGroup.MONSTER,
                EpsteinEntityType,
                1, 1, 1
        );
        BiomeModifications.addFeature(
                BiomeSelectors.tag(BiomeTags.IS_JUNGLE),
                GenerationStep.Feature.VEGETAL_DECORATION,
                RegistryKey.of(
                        RegistryKeys.PLACED_FEATURE,
                        new Identifier(MOD_ID, "cannabis_patch")
                )
        );
        // Hook generation (RegistryKey only)
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                URANIUM_ORE_PLACED_KEY
        );
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof LivingEntity victim) {

                // Villagers or police are protected
                if (victim instanceof PoliceEntity || victim instanceof VillagerEntity) {

                    player.getDataTracker().set(
                            Szar.LAST_CRIME_TICK,
                            world.getTime()
                    );
                }
            }
            return ActionResult.PASS;
        });
        ServerPlayNetworking.registerGlobalReceiver(MERL_QUESTION,
                (server, player, handler, buf, responseSender) -> {

                    int entityId = buf.readInt();
                    String question = buf.readString();

                    server.execute(() -> {
                        Entity entity = player.getWorld().getEntityById(entityId);

                        if (entity instanceof MerlEntity merl) {
                            player.sendMessage(
                                    Text.literal("Merl whispers to you: I don't know.")
                                            .formatted(Formatting.GRAY, Formatting.ITALIC),
                                    false
                            );
                            merl.getWorld().playSound(
                                    null,
                                    merl.getX(),
                                    merl.getY(),
                                    merl.getZ(),
                                    MERL_SOUND,
                                    SoundCategory.NEUTRAL,
                                    1.0F,
                                    1.0F
                            );
                        }
                    });
                });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isSleeping()) {
                    BlockPos bedPos = player.getSleepingPosition().orElse(null);
                    if (bedPos != null && !sleepingPlayers.containsKey(player.getUuid())) {
                        sleepingPlayers.put(player.getUuid(), bedPos);
                        checkSleepPairs(server, player, bedPos);
                    }
                } else {
                    // remove on wakeup
                    sleepingPlayers.remove(player.getUuid());
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("ny")
                            .requires(context -> context.hasPermissionLevel(2))
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                ServerWorld world = source.getWorld();

                                // Kill all KidEntity instances
                                int count = world.getEntitiesByType(NyanEntityType, e -> true).size();
                                world.getEntitiesByType(NyanEntityType, e -> true).forEach(e -> e.kill());

                                source.sendMessage(Text.literal("Killed " + count + " nyan cats."));
                                return count;
                            })
            );
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("getnearestobeliskcore")
                            .requires(context -> context.hasPermissionLevel(2))
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                ServerWorld world = source.getWorld();

                                assert source.getEntity() != null;
                                ObeliskCoreBlockEntity nearest = findNearestObelisk(world, source.getEntity().getBlockPos(), 100);
                                if (nearest != null) {
                                    boolean hasPlane = nearest.hasPlaneMob();

                                    source.sendMessage(Text.literal(
                                            "HasPlane: " + hasPlane
                                    ));
                                    return 1;
                                }
                                return 0;
                            })
            );
        });
        Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "towers"),
                new BlockItem(OBELISK_CORE, new Item.Settings())
        );
    }
    public static ObeliskCoreBlockEntity findNearestObelisk(ServerWorld world, BlockPos center, int radius) {
        ObeliskCoreBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    mutable.set(center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z);

                    BlockEntity be = world.getBlockEntity(mutable);

                    if (be instanceof ObeliskCoreBlockEntity obelisk) {
                        double distance = center.getSquaredDistance(mutable);

                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closest = obelisk;
                        }
                    }
                }
            }
        }

        return closest;
    }
    public static final Item CNDM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "cndm"),
            new Item(new Item.Settings())
    );
    public static final Item WHITE_LIQUID = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "white_liquid"),
            new Item(new Item.Settings().food(new FoodComponent.Builder().alwaysEdible().hunger(1).build()))
    );
    public static final Item LATEX = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "latex"),
            new Item(new Item.Settings())
    );
    public static final Item WHEEL = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "wheel"),
            new Item(new Item.Settings().food(new FoodComponent.Builder().alwaysEdible().hunger(1).build()))
    );
    public static final Item PLANE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "plane"),
            new SzarSpawnEgg(
                    PLANE_ENTITY_TYPE,
                    new Item.Settings()
            )
    );
    public static final StructurePieceType TNT_OBELISK_PIECE =
            Registry.register(
                    Registries.STRUCTURE_PIECE,
                    new Identifier(MOD_ID, "tower"),
                    TntObeliskPiece::new
            );
    public static final StructureType<TntObeliskStructure> TNT_OBELISK_TYPE =
            Registry.register(
                    Registries.STRUCTURE_TYPE,
                    new Identifier(MOD_ID, "two_towers"),
                    () -> TntObeliskStructure.CODEC
            );
    public static final Block OBELISK_CORE = Registry.register(
            Registries.BLOCK,
            new Identifier(MOD_ID, "obelisk_core"),
            new ObeliskCoreBlock(
                    AbstractBlock.Settings
                            .copy(Blocks.DIRT) // soft block
                            .strength(0.5f, 1.0f)    // very easy to break, low blast resistance
            )
    );
    public static final BlockEntityType<ObeliskCoreBlockEntity> OBELISK_CORE_ENTITY = Registry.register(
    Registries.BLOCK_ENTITY_TYPE,
            new Identifier(MOD_ID, "obelisk_core"),
            FabricBlockEntityTypeBuilder.create(
                    ObeliskCoreBlockEntity::new,
                    OBELISK_CORE // block(s) this BE is linked to
            ).build(null)
        );




    public static final Feature<CannabisPatchFeatureConfig> CANNABIS_PATCH =
            Registry.register(
                    Registries.FEATURE,
                    new Identifier(MOD_ID, "cannabis_patch"),
                    new CannabisPatchFeature(CannabisPatchFeatureConfig.CODEC)
            );
    public static final Map<UUID, Integer> PLAYER_JOINT_LEVEL = new HashMap<>();
    public static final Map<UUID, Boolean> PLAYER_ADDICTION_LEVEL = new HashMap<>();
    public static final StatusEffect DROG_EFFECT = Registry.register(
            Registries.STATUS_EFFECT,
            new Identifier(MOD_ID, "drog"),
            new DrogEffect()
    );
    public static final  StatusEffect ARRESTED = Registry.register(Registries.STATUS_EFFECT, new Identifier(MOD_ID, "arrested"), new ArrestedEffect());
    public static final StatusEffect RADIATION = Registry.register(
            Registries.STATUS_EFFECT,
            new Identifier(MOD_ID, "radiation"),
            new RadiationStatusEffect()
    );
    public static final StatusEffect PREGNANT =
            Registry.register(Registries.STATUS_EFFECT,
                    new Identifier(Szar.MOD_ID, "pregnant"),
                    new PregnantEffect());
    public static final RegistryKey<DamageType> RADIATION_DAMAGE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(MOD_ID, "radiation"));
    public static final RegistryKey<DamageType> FCK_DAMAGE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(MOD_ID, "fck"));
    public static final Item AK_AMMO = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "bullet"),
            new Item(new Item.Settings())
    );
    public static final EntityType<BulletEntity> BULLET =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "bullet"),
                    FabricEntityTypeBuilder.<BulletEntity>create(SpawnGroup.MISC, BulletEntity::new)
                            .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                            .trackRangeBlocks(64)
                            .trackedUpdateRate(20)
                            .build()
            );
    public static final EntityType<AtomEntity> AtomEntityType  =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(MOD_ID, "nuke"),
                    FabricEntityTypeBuilder.create()
                            .entityFactory(AtomEntity::new)
                            .dimensions(EntityDimensions.fixed(1.0F, 1.6F))
                            .trackRangeBlocks(256)
                            .trackedUpdateRate(1)
                            .build()
            );
    public static final Item AK47 = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "ak47"),
            new AK47Item(new Item.Settings().maxCount(1))
    );

    public static final Item CHEMICAL_WORKBENCH_ITEM = Registry.register(
    Registries.ITEM,
            new Identifier(MOD_ID, "chemical_workbench"),
                new BlockItem(CHEMICAL_WORKBENCH, new FabricItemSettings())
            );
    public static final Block TALL_CANNABIS_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(MOD_ID, "tall_cannabis"),
            new TallPlantBlock(
                    FabricBlockSettings.copyOf(Blocks.LARGE_FERN)
            )
    );
    public static final Block CANNABIS_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(MOD_ID, "cannabis"),
            new CannabisBlock(
                    FabricBlockSettings.copyOf(Blocks.FERN)
                            .ticksRandomly()
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
    public static final Item URANIUM_ORE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "uranium_ore"),
            new BlockItem(
                    URANIUM_BLOCK,
                    new Item.Settings()
            )
    );
    public static final Item URANIUM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "uranium"),
            new RadiatedItem(new Item.Settings(), 0.1, 1.1)
    );
    public static final Item URANIUM_ROD = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "uranium_rod"),
            new RadiatedItem(new Item.Settings(), 0.2, 1.2)
    );
    public static final Item ATOM_CORE = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nuke_core"),
            new RadiatedItem(new Item.Settings(), 1, 2)
    );
    public static final Item KEY_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "police_key"),
            new KeyItem(new Item.Settings())
    );
    public static final Item EPSTEIN_FILES = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "epstein_files"),
            new EpsteinFile(new Item.Settings())
    );
    public static final Item HANDCUFF_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "police_handcuff"),
            new HandcuffItem(new Item.Settings())
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
    public static final SoundEvent NYAN_MUSIC =
            SoundEvent.of(new Identifier(MOD_ID, "nyan_music"));
    public static final Item POPTART = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "pop_tart"),
            new MusicDiscItem(13, NYAN_MUSIC,  new Item.Settings()
                    .food(new FoodComponent.Builder()
                            .saturationModifier(0.6f).
                            hunger((Math.random() < 0.5) ? 6 : 7) // SIX OR SEVEN
                            .build()), 217)
    );
    public static final SoundEvent BAITER =
            SoundEvent.of(new Identifier(MOD_ID, "baiter"));
    public static final Item BAITER_DISK = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "baiter"),
            new MusicDiscItem(12, BAITER,  new Item.Settings().maxCount(1).rarity(Rarity.RARE), 172)
    );
    public static final SoundEvent EFN =
            SoundEvent.of(new Identifier(MOD_ID, "efn"));
    public static final Item EFN_DISK = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "efn"),
            new MusicDiscItem(11, EFN,  new Item.Settings().maxCount(1).rarity(Rarity.RARE), 133)
    );
    public static final Item ATOM_DETONATOR = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "detonator"),
            new AtomSummonerItem(new Item.Settings())
    );
    public static final Item NWORD_PASS = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nwordpass"),
            new NwordPassItem(new Item.Settings())
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
    public static final Item NYAN_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nyan_cat_spawn_egg"),
            new SpawnEggItem(
                    NyanEntityType,
                    0xFF99FF,
                    0xFF3399,
                    new Item.Settings()
            )
    );
    public static final Item HITTER_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "hitler_spawn_egg"),
            new SpawnEggItem(
                    HitterEntityType,
                    0xC4A484,
                    0xFF0000,
                    new Item.Settings()
            )
    );
    public static final Item MERL_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "merl_spawn_egg"),
            new SpawnEggItem(
                    MerlEntityType,
                    0xD08B4F,
                    0xCD75A8,
                    new Item.Settings()
            )
    );
    public static final Item ATOM = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nuke"),
            new AtomItem(
                    new Item.Settings()
            )
    );
    public static final Item NAZI_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "nazi_spawn_egg"),
            new SpawnEggItem(
                    NaziEntityType,
                    0x654321,
                    0xFF0000,
                    new Item.Settings()
            )
    );
    public static final Item POLICE_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "police_spawn_egg"),
            new SpawnEggItem(
                    PoliceEntityType,
                    0x0000FF,
                    0xFF0000,
                    new Item.Settings()
            )
    );
    public static final Item EPSTEIN_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "epstein_spawn_egg"),
            new SpawnEggItem(
                    EpsteinEntityType,
                    0xB47459,
                    0x151D2D,
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
    public static final Item TERRORIST_SPAWNEGG = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "terrorist_spawn_egg"),
            new SpawnEggItem(
                    TERRORIST_ENTITY_TYPE,
                    0xFF0000,
                    0x8B0000,
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
    public static void playPlaneAnimation(PlaneAnimation animation, int entityId) {
        for (ServerWorld world : SERVER.getWorlds()) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(entityId); // <-- important change
            buf.writeEnumConstant(animation);  // PlaneAnimation
            ServerPlayNetworking.send(player, PLANE_ANIM_PACKET, buf);
        }
        }
    }
    public static final Map<PlaneAnimation, Integer> ANIMATION_TIMINGS = new HashMap<>();

    static {
        ANIMATION_TIMINGS.put(PlaneAnimation.START_ENGINE, 46);    // 2.2917s * 20 ticks
        ANIMATION_TIMINGS.put(PlaneAnimation.STOP_ENGINE, 40);     // 2.0s * 20 ticks
        ANIMATION_TIMINGS.put(PlaneAnimation.FLYING, -1);          // looping
        ANIMATION_TIMINGS.put(PlaneAnimation.LANDING, 40);         // 2.0s * 20 ticks
        ANIMATION_TIMINGS.put(PlaneAnimation.LAND_STARTED, -1);    // looping
        ANIMATION_TIMINGS.put(PlaneAnimation.LIFT_UP, 30);         // 1.5s * 20 ticks
    }
    public static final Map<PlaneAnimation, Float> ANIMATION_TIMINGS_SECONDS = new HashMap<>();

    static {
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.START_ENGINE, 2.2917f);    // 2.2917s * 20 ticks
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.STOP_ENGINE, 2f);     // 2.0s * 20 ticks
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.FLYING, -1f);          // looping
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.LANDING, 2f);         // 2.0s * 20 ticks
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.LAND_STARTED, -1f);    // looping
        ANIMATION_TIMINGS_SECONDS.put(PlaneAnimation.LIFT_UP, 1.5f);         // 1.5s * 20 ticks
    }
    // Kaupenjoe-style ConfiguredFeature bootstrap
    public static class ModConfiguredFeatures {
        public static void boostrap(Registerable<ConfiguredFeature<?, ?>> context) {
            var stoneTag = new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES);
            var deepslateTag = new TagMatchRuleTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

            List<OreFeatureConfig.Target> targets = List.of(
                    OreFeatureConfig.createTarget(stoneTag, URANIUM_BLOCK.getDefaultState()),
                    OreFeatureConfig.createTarget(deepslateTag, URANIUM_BLOCK.getDefaultState())
            );

            context.register(URANIUM_ORE_KEY, new ConfiguredFeature<>(Feature.ORE, new OreFeatureConfig(targets, 4)));
        }
    }

    // Kaupenjoe-style PlacedFeature bootstrap
    public static class ModPlacedFeatures {
        public static void boostrap(Registerable<PlacedFeature> context) {
            RegistryEntry<ConfiguredFeature<?, ?>> configuredEntry =
                    context.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE)
                            .getOrThrow(URANIUM_ORE_KEY);

            context.register(URANIUM_ORE_PLACED_KEY, new PlacedFeature(
                    configuredEntry,
                    List.of(
                            CountPlacementModifier.of(2),
                            SquarePlacementModifier.of(),
                            HeightRangePlacementModifier.uniform(YOffset.fixed(-63), YOffset.fixed(-58)),
                            BiomePlacementModifier.of()
                    )
            ));
        }
    }
    public static final Map<UUID, UUID> pregnantPartners = new HashMap<>();
    private void checkSleepPairs(MinecraftServer server, ServerPlayerEntity sleeper, BlockPos bedPos) {
        double maxDist = 2.0;

        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == sleeper) continue;

            if (other.isSleeping()) {
                BlockPos otherPos = other.getSleepingPosition().orElse(null);
                if (otherPos != null && otherPos.isWithinDistance(bedPos, maxDist)) {

                    // Determine who is holding the special item
                    if (isHoldingSpecial(sleeper)) {
                        // The OTHER player gets the effect
                        givePregnantEffect(other, sleeper, sleeper.getOffHandStack().getItem() == CNDM ? 100 : 5);
                    } else if (isHoldingSpecial(other)) {
                        givePregnantEffect(sleeper, other, other.getOffHandStack().getItem() == CNDM ? 100 : 5);
                    }
                }
            }
        }
    }

    private boolean isHoldingSpecial(ServerPlayerEntity p) {
        return p.getMainHandStack().getItem() == FASZITEM;
    }

    private void givePregnantEffect(ServerPlayerEntity player, ServerPlayerEntity partner, int chance) {
        if (partner.getOffHandStack().getItem() == Szar.CNDM) {
            partner.getOffHandStack().decrement(1);
            partner.dropStack(new ItemStack(WHITE_LIQUID));
        }
        Random r = new Random();
        System.out.println(r.nextInt());
        if (r.nextInt(chance) == 0) {
            player.addStatusEffect(new StatusEffectInstance(PREGNANT, 20 * 60 * 20, 0, false, false, true));
            pregnantPartners.put(player.getUuid(), partner.getUuid());
        }
    }
}

