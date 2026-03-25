package dev.tggamesyt.szar;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BlueprintBlocks {

    private static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.TERRACOTTA_LIGHT_BLUE)
                .strength(1.0f);
    }

    public static final BlueprintStairsBlock BLUEPRINT_STAIRS =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_stairs"),
                    new BlueprintStairsBlock(settings()));

    public static final BlueprintSlabBlock BLUEPRINT_SLAB =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_slab"),
                    new BlueprintSlabBlock(settings()));

    public static final BlueprintDoorBlock BLUEPRINT_DOOR =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_door"),
                    new BlueprintDoorBlock(settings()));

    public static final BlueprintTrapDoorBlock BLUEPRINT_TRAPDOOR =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_trapdoor"),
                    new BlueprintTrapDoorBlock(settings()));

    public static final BlueprintWallBlock BLUEPRINT_WALL =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_wall"),
                    new BlueprintWallBlock(AbstractBlock.Settings.copy(Blocks.STONE_BRICK_WALL)));

    public static final BlueprintFenceBlock BLUEPRINT_FENCE =
            Registry.register(Registries.BLOCK, new Identifier(Szar.MOD_ID, "blueprint_fence"),
                    new BlueprintFenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE)));

    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_STAIRS_BE_TYPE;
    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_SLAB_BE_TYPE;
    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_DOOR_BE_TYPE;
    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_TRAPDOOR_BE_TYPE;
    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_WALL_BE_TYPE;
    public static final BlockEntityType<BlueprintBlockEntity> BLUEPRINT_FENCE_BE_TYPE;

    static {
        BLUEPRINT_STAIRS_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_stairs_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_STAIRS).build());

        BLUEPRINT_SLAB_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_slab_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_SLAB).build());

        BLUEPRINT_DOOR_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_door_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_DOOR).build());

        BLUEPRINT_TRAPDOOR_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_trapdoor_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_TRAPDOOR).build());

        BLUEPRINT_WALL_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_wall_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_WALL).build());

        BLUEPRINT_FENCE_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Szar.MOD_ID, "blueprint_fence_be"),
                FabricBlockEntityTypeBuilder.create(BlueprintBlockEntity::new, BLUEPRINT_FENCE).build());
    }
    public static final BlockItem BLUEPRINT_STAIRS_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_stairs"),
            new BlockItem(BLUEPRINT_STAIRS, new Item.Settings()));

    public static final BlockItem BLUEPRINT_SLAB_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_slab"),
            new BlockItem(BLUEPRINT_SLAB, new Item.Settings()));

    public static final BlockItem BLUEPRINT_DOOR_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_door"),
            new BlockItem(BLUEPRINT_DOOR, new Item.Settings()));

    public static final BlockItem BLUEPRINT_TRAPDOOR_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_trapdoor"),
            new BlockItem(BLUEPRINT_TRAPDOOR, new Item.Settings()));

    public static final BlockItem BLUEPRINT_WALL_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_wall"),
            new BlockItem(BLUEPRINT_WALL, new Item.Settings()));

    public static final BlockItem BLUEPRINT_FENCE_ITEM = Registry.register(Registries.ITEM,
            new Identifier(Szar.MOD_ID, "blueprint_fence"),
            new BlockItem(BLUEPRINT_FENCE, new Item.Settings()));
    public static void init() {} // just call this to trigger class loading
}
