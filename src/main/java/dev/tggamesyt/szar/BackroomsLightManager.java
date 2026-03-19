package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BackroomsLightManager {

    // Global event state
    public enum GlobalEvent { NONE, FLICKER, BLACKOUT }

    public static GlobalEvent currentEvent = GlobalEvent.NONE;
    public static int eventTimer = 0;       // ticks remaining in current event
    public static int cooldownTimer = 0;    // ticks until next event check

    // Flicker event duration: 3-8 seconds
    private static final int FLICKER_DURATION_MIN = 60;
    private static final int FLICKER_DURATION_MAX = 160;
    // Blackout duration: 50-100 seconds
    private static final int BLACKOUT_MIN = 1000;
    private static final int BLACKOUT_MAX = 2000;
    // Check for new event every ~3 minutes
    private static final int EVENT_COOLDOWN = 3600;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BackroomsLightManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
        if (backrooms == null) return;

        // Handle event timers
        if (currentEvent != GlobalEvent.NONE) {
            eventTimer--;
            if (eventTimer <= 0) {
                endEvent(backrooms);
            }
        } else {
            cooldownTimer--;
            if (cooldownTimer <= 0) {
                // Roll for new event
                int roll = backrooms.random.nextInt(100);
                if (roll < 30) {
                    startBlackout(backrooms);
                } else if (roll < 63) { // 30% blackout + 33% flicker
                    startFlicker(backrooms);
                } else {
                    // No event — reset cooldown
                    cooldownTimer = EVENT_COOLDOWN;
                }
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

        // Immediately turn off all ON lights in loaded chunks
        setAllLightsOff(world);
    }

    private static void endEvent(ServerWorld world) {
        if (currentEvent == GlobalEvent.BLACKOUT) {
            // Restore all lights
            setAllLightsOn(world);
        }
        currentEvent = GlobalEvent.NONE;
        eventTimer = 0;
        cooldownTimer = EVENT_COOLDOWN;
    }

    private static void setAllLightsOff(ServerWorld world) {
        forEachLight(world, (pos, state) -> {
            BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);
            if (ls == BackroomsLightBlock.LightState.ON) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.OFF));
            } else if (ls == BackroomsLightBlock.LightState.FLICKERING_ON) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.FLICKERING_OFF));
            }
        });
    }

    private static void setAllLightsOn(ServerWorld world) {
        forEachLight(world, (pos, state) -> {
            BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);
            if (ls == BackroomsLightBlock.LightState.OFF) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.ON));
            } else if (ls == BackroomsLightBlock.LightState.FLICKERING_OFF) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.FLICKERING_ON));
            }
        });
    }

    private static void forEachLight(ServerWorld world, java.util.function.BiConsumer<BlockPos, BlockState> consumer) {
        for (net.minecraft.world.chunk.WorldChunk chunk : getLoadedChunks(world)) {
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            int cx = chunk.getPos().getStartX();
            int cz = chunk.getPos().getStartZ();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    // Ceiling Y in backrooms is 9
                    mutable.set(cx + lx, 9, cz + lz);
                    BlockState state = world.getBlockState(mutable);
                    if (state.getBlock() instanceof BackroomsLightBlock) {
                        consumer.accept(mutable.toImmutable(), state);
                    }
                }
            }
        }
    }

    private static java.util.List<net.minecraft.world.chunk.WorldChunk> getLoadedChunks(ServerWorld world) {
        java.util.List<net.minecraft.world.chunk.WorldChunk> chunks = new java.util.ArrayList<>();
        // Iterate over all players and collect chunks around them
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            int playerChunkX = (int) player.getX() >> 4;
            int playerChunkZ = (int) player.getZ() >> 4;
            int viewDistance = world.getServer().getPlayerManager().getViewDistance();
            for (int cx = playerChunkX - viewDistance; cx <= playerChunkX + viewDistance; cx++) {
                for (int cz = playerChunkZ - viewDistance; cz <= playerChunkZ + viewDistance; cz++) {
                    if (world.getChunkManager().isChunkLoaded(cx, cz)) {
                        net.minecraft.world.chunk.WorldChunk chunk = world.getChunk(cx, cz);
                        if (!chunks.contains(chunk)) {
                            chunks.add(chunk);
                        }
                    }
                }
            }
        }
        return chunks;
    }

    // Called per-light from BackroomsLightBlockEntity.tick
    public static void tickLight(World world, BlockPos pos, BlockState state,
                                 BackroomsLightBlockEntity entity) {
        BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);

        // During blackout, lights are already set off — don't touch them
        if (currentEvent == GlobalEvent.BLACKOUT) return;

        // Only flickering lights and lights during flicker events need ticking
        boolean isFlickering = ls == BackroomsLightBlock.LightState.FLICKERING_ON
                || ls == BackroomsLightBlock.LightState.FLICKERING_OFF;
        boolean inFlickerEvent = currentEvent == GlobalEvent.FLICKER;

        if (!isFlickering && !inFlickerEvent) return;

        // Decrement timer
        entity.flickerTimer--;
        if (entity.flickerTimer > 0) return;

        // Toggle state and set new random timer
        // Flickering lights: 2-8 ticks per toggle
        // Event flicker: same but offset by entity's flickerOffset
        int baseTime = 2 + world.random.nextInt(7);

        if (inFlickerEvent && !isFlickering) {
            // Normal ON light during flicker event — toggle it
            if (ls == BackroomsLightBlock.LightState.ON) {
                // Apply offset so not all lights flicker simultaneously
                if (entity.flickerTimer == 0 && world.getTime() % 3 == entity.flickerOffset % 3) {
                    world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                            BackroomsLightBlock.LightState.OFF));
                    entity.flickerTimer = baseTime;
                    entity.markDirty();
                }
                return;
            } else if (ls == BackroomsLightBlock.LightState.OFF
                    && currentEvent == GlobalEvent.FLICKER) {
                world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE,
                        BackroomsLightBlock.LightState.ON));
                entity.flickerTimer = baseTime;
                entity.markDirty();
                return;
            }
        }

        if (isFlickering) {
            BackroomsLightBlock.LightState next =
                    ls == BackroomsLightBlock.LightState.FLICKERING_ON
                            ? BackroomsLightBlock.LightState.FLICKERING_OFF
                            : BackroomsLightBlock.LightState.FLICKERING_ON;
            world.setBlockState(pos, state.with(BackroomsLightBlock.LIGHT_STATE, next));
            entity.flickerTimer = baseTime;
            entity.markDirty();
        }
    }

    public static void forceRestoreAllLights(ServerWorld world) {
        setAllLightsOn(world);
    }

    public static void forceBlackout(ServerWorld world) {
        setAllLightsOff(world);
    }
}