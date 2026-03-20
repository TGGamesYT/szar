package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BackroomsLightManager {

    public enum GlobalEvent { NONE, FLICKER, BLACKOUT }

    public static GlobalEvent currentEvent = GlobalEvent.NONE;
    public static int eventTimer = 0;
    public static int cooldownTimer = 3600;

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

        if (currentEvent != GlobalEvent.NONE) {
            eventTimer--;
            if (eventTimer <= 0) {
                endEvent(backrooms);
            }
        } else {
            cooldownTimer--;
            if (cooldownTimer <= 0) {
                int roll = backrooms.random.nextInt(100);
                if (roll < 30) {
                    startBlackout(backrooms);
                } else if (roll < 63) {
                    startFlicker(backrooms);
                } else {
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
        // Set all lights to brightness 0
        forEachLightEntity(world, entity -> {
            entity.brightness = 0.0f;
            entity.markDirty();
        });
    }

    private static void endEvent(ServerWorld world) {
        // Restore all lights to full brightness
        forEachLightEntity(world, entity -> {
            entity.brightness = 1.0f;
            entity.markDirty();
        });
        currentEvent = GlobalEvent.NONE;
        eventTimer = 0;
        cooldownTimer = EVENT_COOLDOWN;
    }

    // Called per-light from BackroomsLightBlockEntity.tick
    public static void tickLight(World world, BlockPos pos, BlockState state,
                                 BackroomsLightBlockEntity entity) {
        if (currentEvent == GlobalEvent.BLACKOUT) return;

        BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);
        if (ls == BackroomsLightBlock.LightState.OFF) return;

        boolean inFlickerEvent = currentEvent == GlobalEvent.FLICKER;

        // Always-flickering lights tick regardless of event
        // During flicker event, all ON lights also flicker
        if (!entity.isFlickering && !inFlickerEvent) {
            // Normal ON light, not in event — ensure full brightness
            if (entity.brightness != 1.0f) {
                entity.brightness = 1.0f;
                entity.markDirty();
            }
            return;
        }

        entity.flickerTimer--;
        if (entity.flickerTimer > 0) return;

        // Random new brightness and timer
        float newBrightness;
        if (world.random.nextFloat() < 0.3f) {
            // 30% chance of a dim flicker
            newBrightness = 0.1f + world.random.nextFloat() * 0.4f;
        } else {
            // 70% chance of full or near-full
            newBrightness = 0.7f + world.random.nextFloat() * 0.3f;
        }

        entity.brightness = newBrightness;
        entity.flickerTimer = 2 + world.random.nextInt(8 + (entity.flickerOffset % 5));
        entity.markDirty();
    }

    private static void forEachLightEntity(ServerWorld world,
                                           Consumer<BackroomsLightBlockEntity> consumer) {
        for (WorldChunk chunk : getLoadedChunks(world)) {
            int cx = chunk.getPos().getStartX();
            int cz = chunk.getPos().getStartZ();
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    mutable.set(cx + lx, 9, cz + lz);
                    if (world.getBlockEntity(mutable.toImmutable())
                            instanceof BackroomsLightBlockEntity entity) {
                        consumer.accept(entity);
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
        forEachLightEntity(world, entity -> {
            entity.brightness = 1.0f;
            entity.markDirty();
        });
        currentEvent = GlobalEvent.NONE;
        eventTimer = 0;
        cooldownTimer = EVENT_COOLDOWN;
    }

    public static void forceBlackout(ServerWorld world) {
        forEachLightEntity(world, entity -> {
            entity.brightness = 0.0f;
            entity.markDirty();
        });
        currentEvent = GlobalEvent.BLACKOUT;
        eventTimer = 3600;
    }
}