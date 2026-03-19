package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tggamesyt.szar.Szar;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BackroomsChunkGenerator extends ChunkGenerator {

    // Y layout constants
    private static final int FLOOR_Y     = 4;  // y=4 is the floor block
    private static final int CEILING_Y   = 9;  // y=9 is the ceiling block (4 air blocks between: 5,6,7,8)
    private static final int WALL_BASE_Y = 5;  // y=5 is WALL_BOTTOM_BLOCK
    private static final int WALL_TOP_Y  = 8;  // y=8 is top wall block (below ceiling)

    // Glowstone grid spacing
    private static final int GLOW_SPACING = 5;

    public static final Codec<BackroomsChunkGenerator> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Biome.REGISTRY_CODEC
                            .fieldOf("biome")
                            .forGetter(g -> g.biomeEntry)
            ).apply(instance, BackroomsChunkGenerator::new));

    private final RegistryEntry<Biome> biomeEntry;

    public BackroomsChunkGenerator(RegistryEntry<Biome> biomeEntry) {
        super(new FixedBiomeSource(biomeEntry));
        this.biomeEntry = biomeEntry;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Executor executor, Blender blender, NoiseConfig noiseConfig,
            StructureAccessor structureAccessor, Chunk chunk) {

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int worldX = chunkX * 16 + lx;
                int worldZ = chunkZ * 16 + lz;

                // Deterministic per-column open/wall decision using world seed-like hash
                boolean isOpen = isOpenSpace(worldX, worldZ);

                // Floor — always placed
                chunk.setBlockState(new BlockPos(lx, FLOOR_Y, lz),
                        Szar.PLASTIC.getDefaultState(), false);

                // Below floor — fill with plastic so there's no void underneath
                for (int y = 0; y < FLOOR_Y; y++) {
                    chunk.setBlockState(new BlockPos(lx, y, lz),
                            Szar.PLASTIC.getDefaultState(), false);
                }

                boolean isGlowstone = isGlowstonePos(worldX, worldZ);
                BlockState ceilingBlock;
                if (isGlowstone) {
                    long lightRoll = hash(worldX * 53 + 7, worldZ * 47 + 13);
                    int roll = (int)(Math.abs(lightRoll) % 100);
                    if (roll < 95) {
                        // 95% ON
                        ceilingBlock = Szar.BACKROOMS_LIGHT.getDefaultState()
                                .with(BackroomsLightBlock.LIGHT_STATE,
                                        BackroomsLightBlock.LightState.ON);
                    } else if (roll < 98) {
                        // 3% FLICKERING_ON
                        ceilingBlock = Szar.BACKROOMS_LIGHT.getDefaultState()
                                .with(BackroomsLightBlock.LIGHT_STATE,
                                        BackroomsLightBlock.LightState.FLICKERING_ON);
                    } else {
                        // 2% missing — ceiling block, no light
                        ceilingBlock = Szar.CEILING.getDefaultState();
                    }
                } else {
                    ceilingBlock = Szar.CEILING.getDefaultState();
                }
                chunk.setBlockState(new BlockPos(lx, CEILING_Y, lz), ceilingBlock, false);

                // Above ceiling — solid wall block fill so there's no void above
                for (int y = CEILING_Y + 1; y < 64; y++) {
                    chunk.setBlockState(new BlockPos(lx, y, lz),
                            Szar.CEILING.getDefaultState(), false);
                }

                if (isOpen) {
                    for (int y = WALL_BASE_Y; y <= WALL_TOP_Y; y++) {
                        chunk.setBlockState(new BlockPos(lx, y, lz),
                                Blocks.AIR.getDefaultState(), false);
                    }

                    int cellX = Math.floorDiv(worldX, 40);
                    int cellZ = Math.floorDiv(worldZ, 40);
                    long cellHash = hash(cellX * 31 + 17, cellZ * 29 + 11);

                    int barrelLocalX = (int)(cellHash & 0x1F) % 40; // 0-39
                    int barrelLocalZ = (int)((cellHash >> 8) & 0x1F) % 40; // 0-39
                    int cellStartX = cellX * 40;
                    int cellStartZ = cellZ * 40;

                    if (worldX == cellStartX + barrelLocalX && worldZ == cellStartZ + barrelLocalZ
                            && isOpenSpace(worldX, worldZ)) {
                        chunk.setBlockState(new BlockPos(lx, FLOOR_Y + 1, lz),
                                Blocks.BARREL.getDefaultState(), false);
                    }
                } else {
                    // Wall column
                    chunk.setBlockState(new BlockPos(lx, WALL_BASE_Y, lz),
                            Szar.WALL_BOTTOM_BLOCK.getDefaultState(), false);
                    for (int y = WALL_BASE_Y + 1; y <= WALL_TOP_Y; y++) {
                        chunk.setBlockState(new BlockPos(lx, y, lz),
                                Szar.WALL_BLOCK.getDefaultState(), false);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Determines if a world column is open space or a wall.
     * Uses a combination of large-scale and small-scale noise simulation
     * via a seeded hash to create wide rooms with occasional walls.
     */
    private boolean isOpenSpace(int x, int z) {
        // Smaller cells = more frequent walls
        int cellSize = 8; // was 16
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        int localX = Math.floorMod(x, cellSize);
        int localZ = Math.floorMod(z, cellSize);

        long cellHash = hash(cellX, cellZ);

        // ~50% chance of vertical wall, ~50% chance of horizontal wall
        boolean hasVerticalWall   = (cellHash & 0x1) == 0;
        boolean hasHorizontalWall = ((cellHash >> 1) & 0x1) == 0;

        if (hasVerticalWall) {
            int wallPos = 2 + (int) ((cellHash >> 4) & 0x3); // 2-5, keeps wall away from cell edge
            int doorPos = (int) ((cellHash >> 8) & 0x7);     // 0-7
            int doorWidth = 2 + (int) ((cellHash >> 12) & 0x1); // 2-3 wide door
            if (localX == wallPos) {
                boolean inDoor = localZ >= doorPos && localZ < doorPos + doorWidth;
                if (!inDoor) return false;
            }
        }

        if (hasHorizontalWall) {
            int wallPos = 2 + (int) ((cellHash >> 16) & 0x3);
            int doorPos = (int) ((cellHash >> 20) & 0x7);
            int doorWidth = 2 + (int) ((cellHash >> 24) & 0x1);
            if (localZ == wallPos) {
                boolean inDoor = localX >= doorPos && localX < doorPos + doorWidth;
                if (!inDoor) return false;
            }
        }

        return true;
    }

    private boolean isGlowstonePos(int x, int z) {
        // Grid every GLOW_SPACING blocks, offset slightly so it's not always on chunk borders
        return (Math.floorMod(x + 2, GLOW_SPACING) == 0)
                && (Math.floorMod(z + 2, GLOW_SPACING) == 0);
    }

    /**
     * Simple integer hash for deterministic world generation.
     * Not seeded — same world always generates the same backrooms.
     * If you want seed-dependent generation, pass in world seed too.
     */
    private long hash(int x, int z) {
        long h = 374761393L;
        h += x * 2654435761L;
        h ^= h >> 17;
        h += z * 2246822519L;
        h ^= h >> 13;
        h *= 3266489917L;
        h ^= h >> 16;
        return h & 0xFFFFFFFFL;
    }

    // --- Required overrides ---

    @Override
    public void carve(ChunkRegion chunk, long seed, NoiseConfig noiseConfig,
                      BiomeAccess access, StructureAccessor structureAccessor,
                      Chunk chunk2, GenerationStep.Carver carver) {}

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {}

    @Override
    public void populateEntities(ChunkRegion region) {}

    @Override
    public int getWorldHeight() { return 64; }

    @Override
    public int getSeaLevel() { return -1; }

    @Override
    public int getMinimumY() { return 0; }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap,
                         HeightLimitView world, NoiseConfig noiseConfig) {
        return CEILING_Y;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z,
                                               HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[64];
        for (int y = 0; y < 64; y++) {
            if (y < FLOOR_Y) states[y] = Szar.PLASTIC.getDefaultState();
            else if (y == FLOOR_Y) states[y] = Szar.PLASTIC.getDefaultState();
            else if (y <= WALL_TOP_Y) states[y] = isOpenSpace(x, z)
                    ? Blocks.AIR.getDefaultState()
                    : Szar.WALL_BLOCK.getDefaultState();
            else if (y == CEILING_Y) states[y] = Szar.CEILING.getDefaultState();
            else states[y] = Szar.CEILING.getDefaultState();
        }
        return new VerticalBlockSample(0, states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig,
                                BlockPos pos) {}

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk,
                                 StructureAccessor structureAccessor) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int chunkX = chunk.getPos().getStartX();
        int chunkZ = chunk.getPos().getStartZ();

        // Initialize wall block entities
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 64; y++) {
                    mutable.set(chunkX + lx, y, chunkZ + lz);
                    if (world.getBlockState(mutable).getBlock() instanceof WallBlock) {
                        world.setBlockState(mutable, Szar.WALL_BLOCK.getDefaultState(),
                                Block.NOTIFY_ALL);
                        if (world.getBlockEntity(mutable) instanceof WallBlockEntity wall) {
                            wall.initializeIfNeeded();
                        }
                    }
                }
                // Inside the lx/lz loop in generateFeatures, after the wall block section:
                mutable.set(chunkX + lx, 9, chunkZ + lz); // CEILING_Y = 9
                BlockState lightState = world.getBlockState(mutable);
                if (lightState.getBlock() instanceof BackroomsLightBlock) {
                    if (world.getBlockEntity(mutable) == null) {
                        world.setBlockState(mutable, lightState, Block.NOTIFY_ALL);
                    }
                    if (world.getBlockEntity(mutable) instanceof BackroomsLightBlockEntity light) {
                        light.flickerOffset = (int)(Math.abs(hash(chunkX + lx, chunkZ + lz)) % 100);
                        light.flickerTimer = light.flickerOffset; // stagger initial timers
                        light.markDirty();
                    }
                }
            }
        }

        // Place tracker portals
        long chunkHash = hash((chunkX / 16) * 7 + 3, (chunkZ / 16) * 13 + 7);
        if ((chunkHash % 20) == 0) {
            int lx = 4 + (int)(chunkHash >> 8 & 0x7);
            int lz = 4 + (int)(chunkHash >> 12 & 0x7);

            if (isOpenSpace(chunkX + lx, chunkZ + lz)) {
                BlockPos trackerPos = new BlockPos(chunkX + lx, FLOOR_Y + 1, chunkZ + lz);
                BlockPos portalPos = trackerPos.down(4);

                world.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState(),
                        Block.NOTIFY_ALL);
                world.setBlockState(trackerPos, Szar.TRACKER_BLOCK.getDefaultState(),
                        Block.NOTIFY_ALL);

                if (world.getBlockEntity(trackerPos) instanceof TrackerBlockEntity te) {
                    te.placedByPlayer = false;
                    te.originalPortalBlock = Szar.PLASTIC.getDefaultState();
                    te.markDirty();
                }
            }
        }
    }
}