package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.tggamesyt.szar.Szar.MOD_ID;

public class FartManager {
    public static final net.minecraft.sound.SoundEvent FART_SOUND = Registry.register(
            Registries.SOUND_EVENT,
            new Identifier(MOD_ID, "fart"),
            net.minecraft.sound.SoundEvent.of(new Identifier(MOD_ID, "fart"))
    );

    public static final net.minecraft.sound.SoundEvent FART1_SOUND = Registry.register(
            Registries.SOUND_EVENT,
            new Identifier(MOD_ID, "fart1"),
            net.minecraft.sound.SoundEvent.of(new Identifier(MOD_ID, "fart1"))
    );
    // Ticks since player joined
    private static final Map<UUID, Long> tickCounter = new HashMap<>();
    // How many damage increments have been applied (max 10)
    private static final Map<UUID, Integer> damageLevel = new HashMap<>();
    private static final java.util.Set<UUID> wasSneaking = new java.util.HashSet<>();
    public static final RegistryKey<DamageType> FART_DAMAGE_TYPE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
                    new Identifier(MOD_ID, "fart"));
    private static final long FIRST_FART_DELAY = 36000;  // first fart after 30 min
    private static final long FART_INTERVAL = 12000;      // every 10 min after
    private static final int MAX_DAMAGE_LEVEL = 10;       // max 10 half-hearts extra
    private static final float EXPLOSION_THRESHOLD = 8.0f; // 8 hearts = explosion
    private static final float MAX_RANGE = 5.0f;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FartManager::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUuid();
            tickCounter.put(uuid, 0L);
            damageLevel.put(uuid, 0);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            tickCounter.remove(uuid);
            damageLevel.remove(uuid);
            wasSneaking.remove(uuid);
        });
    }

    private static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();

                if (!tickCounter.containsKey(uuid)) {
                    tickCounter.put(uuid, 0L);
                    damageLevel.put(uuid, 0);
                }

                if (player.isSneaking()) {
                    if (!wasSneaking.contains(uuid)) {
                        // First tick of sneaking — trigger fart
                        wasSneaking.add(uuid);
                        int level = damageLevel.getOrDefault(uuid, 0);
                        if (level > 0 || world.random.nextInt(10) == 0) {
                            releaseFart(world, player, level);
                            tickCounter.put(uuid, 0L);
                            damageLevel.put(uuid, 0);
                        }
                    }
                    continue;
                } else {
                    // Player stopped sneaking — clear the flag
                    wasSneaking.remove(uuid);
                }

                long ticks = tickCounter.get(uuid) + 1;
                tickCounter.put(uuid, ticks);

                if (ticks % FART_INTERVAL == 0) {
                    // Damage only starts increasing after FIRST_FART_DELAY
                    if (ticks >= FIRST_FART_DELAY) {
                        int level = damageLevel.getOrDefault(uuid, 0);
                        if (level < MAX_DAMAGE_LEVEL) {
                            level++;
                            damageLevel.put(uuid, level);
                        }
                        damageLevel.put(uuid, Math.min(level, MAX_DAMAGE_LEVEL));
                    }
                    releaseFart(world, player, damageLevel.getOrDefault(uuid, 0));
                }
            }
        }
    }

    private static void releaseFart(ServerWorld world, ServerPlayerEntity farter, int level) {
        // Calculate damage: level * 0.5 hearts = level * 1.0 damage points
        float damage = level * 1.0f;

        // Play fart sound to everyone nearby
        playfartSound(world, farter);

        // Damage nearby players
        Vec3d fartPos = farter.getPos();
        for (ServerPlayerEntity victim : world.getPlayers()) {
            if (victim == farter) continue;
            double dist = victim.getPos().distanceTo(fartPos);
            if (dist > MAX_RANGE) continue;

            // Damage falls off linearly: 1 block = 100%, 5 blocks = 20%
            float multiplier = 1.0f - ((float)(dist - 1.0) / (MAX_RANGE - 1.0f));
            multiplier = Math.max(0.2f, Math.min(1.0f, multiplier));
            float victimDamage = damage * multiplier;

            victim.damage(new DamageSource(
                    world.getRegistryManager()
                            .get(RegistryKeys.DAMAGE_TYPE)
                            .entryOf(FART_DAMAGE_TYPE),
                    farter
            ), victimDamage);
        }

        // Farter takes 50% of full damage
        farter.damage(new DamageSource(
                world.getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .entryOf(FART_DAMAGE_TYPE),
                farter
        ), damage * 0.5f);

        // Explosion if damage exceeds 8 hearts (16 damage points)
        if (damage > EXPLOSION_THRESHOLD) {
            world.createExplosion(
                    farter,
                    fartPos.x, fartPos.y, fartPos.z,
                    2.5f,
                    false,
                    World.ExplosionSourceType.NONE
            );
        }
    }

    private static void playfartSound(ServerWorld world, ServerPlayerEntity farter) {
        // Pick random fart sound
        boolean useFart1 = world.random.nextBoolean();
        SoundEvent sound = useFart1 ? FART1_SOUND : FART_SOUND;

        // Send to all players in range
        for (ServerPlayerEntity listener : world.getPlayers()) {
            if (listener.getPos().distanceTo(farter.getPos()) > 16) continue;
            listener.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    world.getRegistryManager()
                            .get(net.minecraft.registry.RegistryKeys.SOUND_EVENT)
                            .getEntry(sound),
                    SoundCategory.PLAYERS,
                    farter.getX(), farter.getY(), farter.getZ(),
                    1.0f + world.random.nextFloat() * 0.2f,
                    0.8f + world.random.nextFloat() * 0.4f,
                    world.random.nextLong()
            ));
        }
    }

    // Called when crouching resets — also called externally if needed
    public static void resetPlayer(UUID uuid) {
        tickCounter.put(uuid, 0L);
        damageLevel.put(uuid, 0);
    }

    public static long getTicksForPlayer(UUID uuid) {
        return tickCounter.getOrDefault(uuid, 0L);
    }

    public static int getDamageLevelForPlayer(UUID uuid) {
        return damageLevel.getOrDefault(uuid, 0);
    }

    public static void setTicksForPlayer(UUID uuid, long ticks) {
        tickCounter.put(uuid, ticks);
        // Recalculate damage level based on new tick value
        int level = 0;
        if (ticks >= FIRST_FART_DELAY) {
            long ticksSinceFirst = ticks - FIRST_FART_DELAY;
            level = (int) Math.min(ticksSinceFirst / FART_INTERVAL, MAX_DAMAGE_LEVEL);
        }
        damageLevel.put(uuid, level);
    }
}