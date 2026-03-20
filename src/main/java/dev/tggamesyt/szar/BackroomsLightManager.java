package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class BackroomsLightManager {

    public enum GlobalEvent { NONE, FLICKER, BLACKOUT }

    public static GlobalEvent currentEvent = GlobalEvent.NONE;
    public static int eventTimer = 0;
    public static int cooldownTimer = 3600;
    public static int globalFlickerTimer = 0;

    private static final int FLICKER_DURATION_MIN = 60;
    private static final int FLICKER_DURATION_MAX = 160;
    private static final int BLACKOUT_MIN = 1000;
    private static final int BLACKOUT_MAX = 2000;
    private static final int EVENT_COOLDOWN = 3600;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BackroomsLightManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
        if (backrooms == null) return;

        globalFlickerTimer++;

        // Every 20 ticks, fix any lights in wrong state for current event
        if (globalFlickerTimer % 20 == 0) {
            if (currentEvent == GlobalEvent.BLACKOUT) {
                forEachLightEntity(backrooms, (entity, state, pos) -> {
                    if (state.get(BackroomsLightBlock.LIGHT_STATE)
                            != BackroomsLightBlock.LightState.OFF) {
                        backrooms.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                                BackroomsLightBlock.LightState.OFF), Block.NOTIFY_ALL);
                    }
                });
            } else if (currentEvent == GlobalEvent.NONE) {
                forEachLightEntity(backrooms, (entity, state, pos) -> {
                    if (state.get(BackroomsLightBlock.LIGHT_STATE) == BackroomsLightBlock.LightState.OFF) {
                        BackroomsLightBlock.LightState restore = entity.isFlickering
                                ? BackroomsLightBlock.LightState.FLICKERING
                                : BackroomsLightBlock.LightState.ON;
                        backrooms.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                                restore), Block.NOTIFY_ALL);
                    }
                });
            }
        }

        if (currentEvent != GlobalEvent.NONE) {
            eventTimer--;
            if (eventTimer <= 0) endEvent(backrooms);
        } else {
            cooldownTimer--;
            if (cooldownTimer <= 0) {
                int roll = backrooms.random.nextInt(100);
                if (roll < 30) startBlackout(backrooms);
                else if (roll < 63) startFlicker(backrooms);
                else cooldownTimer = EVENT_COOLDOWN;
            }
        }
    }


    private static void startFlicker(ServerWorld world) {
        currentEvent = GlobalEvent.FLICKER;
        eventTimer = FLICKER_DURATION_MIN + world.random.nextInt(
                FLICKER_DURATION_MAX - FLICKER_DURATION_MIN);
        cooldownTimer = EVENT_COOLDOWN;
    }

    private static void startBlackout(ServerWorld world) {
        currentEvent = GlobalEvent.BLACKOUT;
        eventTimer = BLACKOUT_MIN + world.random.nextInt(BLACKOUT_MAX - BLACKOUT_MIN);
        cooldownTimer = EVENT_COOLDOWN;
        forEachLightEntity(world, (entity, state, pos) -> {
            if (state.get(BackroomsLightBlock.LIGHT_STATE) != BackroomsLightBlock.LightState.OFF) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.OFF), Block.NOTIFY_ALL);
            }
        });
    }

    private static void endEvent(ServerWorld world) {
        currentEvent = GlobalEvent.NONE;
        eventTimer = 0;
        cooldownTimer = EVENT_COOLDOWN;
        forEachLightEntity(world, (entity, state, pos) -> {
            BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);
            // Restore any light that was turned off by an event
            if (ls == BackroomsLightBlock.LightState.OFF) {
                BackroomsLightBlock.LightState restore = entity.isFlickering
                        ? BackroomsLightBlock.LightState.FLICKERING
                        : BackroomsLightBlock.LightState.ON;
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        restore), Block.NOTIFY_ALL);
            }
        });
    }

    // Called from generateFeatures when a new chunk loads — apply current event state
    public static void applyCurrentEventToChunk(ServerWorld world, WorldChunk chunk) {
        if (currentEvent == GlobalEvent.NONE) return;

        int cx = chunk.getPos().getStartX();
        int cz = chunk.getPos().getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                mutable.set(cx + lx, 9, cz + lz);
                BlockPos immutable = mutable.toImmutable();
                BlockState state = world.getBlockState(immutable);
                if (!(state.getBlock() instanceof BackroomsLightBlock)) continue;
                if (!(world.getBlockEntity(immutable) instanceof BackroomsLightBlockEntity entity)) continue;

                if (currentEvent == GlobalEvent.BLACKOUT) {
                    if (state.get(BackroomsLightBlock.LIGHT_STATE)
                            != BackroomsLightBlock.LightState.OFF) {
                        world.setBlockState(immutable, state.with(
                                BackroomsLightBlock.LIGHT_STATE,
                                BackroomsLightBlock.LightState.OFF), Block.NOTIFY_ALL);
                    }
                }
                // FLICKER is handled per-tick so no chunk-load action needed
            }
        }
    }

    @FunctionalInterface
    interface LightConsumer {
        void accept(BackroomsLightBlockEntity entity, BlockState state, BlockPos pos);
    }

    private static void forEachLightEntity(ServerWorld world, LightConsumer consumer) {
        for (WorldChunk chunk : getLoadedChunks(world)) {
            int cx = chunk.getPos().getStartX();
            int cz = chunk.getPos().getStartZ();
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    mutable.set(cx + lx, 9, cz + lz);
                    BlockPos immutable = mutable.toImmutable();
                    BlockState state = world.getBlockState(immutable);
                    if (state.getBlock() instanceof BackroomsLightBlock
                            && world.getBlockEntity(immutable)
                            instanceof BackroomsLightBlockEntity entity) {
                        consumer.accept(entity, state, immutable);
                    }
                }
            }
        }
    }

    private static List<WorldChunk> getLoadedChunks(ServerWorld world) {
        List<WorldChunk> chunks = new ArrayList<>();
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            int pcx = (int) player.getX() >> 4;
            int pcz = (int) player.getZ() >> 4;
            int viewDistance = world.getServer().getPlayerManager().getViewDistance();
            for (int cx = pcx - viewDistance; cx <= pcx + viewDistance; cx++) {
                for (int cz = pcz - viewDistance; cz <= pcz + viewDistance; cz++) {
                    if (world.getChunkManager().isChunkLoaded(cx, cz)) {
                        WorldChunk chunk = world.getChunk(cx, cz);
                        if (!chunks.contains(chunk)) chunks.add(chunk);
                    }
                }
            }
        }
        return chunks;
    }

    public static void forceRestoreAllLights(ServerWorld world) {
        currentEvent = GlobalEvent.NONE;
        eventTimer = 0;
        cooldownTimer = EVENT_COOLDOWN;
        forEachLightEntity(world, (entity, state, pos) -> {
            BackroomsLightBlock.LightState restore = entity.isFlickering
                    ? BackroomsLightBlock.LightState.FLICKERING
                    : BackroomsLightBlock.LightState.ON;
            world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                    restore), Block.NOTIFY_ALL);
        });
    }

    public static void forceBlackout(ServerWorld world) {
        currentEvent = GlobalEvent.BLACKOUT;
        eventTimer = 3600;
        forEachLightEntity(world, (entity, state, pos) ->
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.OFF), Block.NOTIFY_ALL));
    }
}