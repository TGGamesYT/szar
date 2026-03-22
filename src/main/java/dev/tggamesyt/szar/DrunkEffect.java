package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DrunkEffect extends StatusEffect {

    public enum DrunkType { AGGRESSIVE, STUMBLING, SLEEPY, GENEROUS, PARANOID }

    private static final Map<UUID, DrunkType> playerTypes = new HashMap<>();
    private static final Map<UUID, Integer> sleepyTimer = new HashMap<>();
    static String currentDisplayType = "None";

    // Behavior tracking maps — updated via mixins or event hooks
    public static final Map<UUID, Long> lastAttackTime = new HashMap<>();       // AGGRESSIVE
    public static final Map<UUID, Long> lastBlockCollisionTime = new HashMap<>(); // STUMBLING
    public static final Map<UUID, Long> lastSleepTime = new HashMap<>();         // SLEEPY
    public static final Map<UUID, Integer> recentDropCount = new HashMap<>();    // GENEROUS
    public static final Map<UUID, Long> lastDropResetTime = new HashMap<>();
    public static final Map<UUID, Float> lastYaw = new HashMap<>();              // PARANOID
    public static final Map<UUID, Float> totalYawChange = new HashMap<>();
    public static final Map<UUID, Long> yawTrackStart = new HashMap<>();

    // Window in ticks for behavior tracking
    private static final long BEHAVIOR_WINDOW = 6000L; // 5 minutes

    public DrunkEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF9900);
    }

    // Called every tick from server tick event to track yaw changes
    public static void trackBehavior(MinecraftServer server) {
        for (var world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID uuid = player.getUuid();
                long now = world.getTime();

                // Track yaw change for PARANOID — only count large sudden movements
                float yaw = player.getYaw();
                Float prev = lastYaw.get(uuid);
                if (prev != null) {
                    float delta = Math.abs(yaw - prev);
                    if (delta > 180) delta = 360 - delta;
                    // Only count if moved more than 45 degrees in a single tick — sharp spin
                    if (delta > 45f) {
                        totalYawChange.merge(uuid, delta, Float::sum);
                    }
                }
                lastYaw.put(uuid, yaw);

                // Reset yaw accumulator every 5 minutes
                long trackStart = yawTrackStart.getOrDefault(uuid, now);
                if (now - trackStart > BEHAVIOR_WINDOW) {
                    totalYawChange.put(uuid, 0f);
                    yawTrackStart.put(uuid, now);
                }

                // Reset drop counter every 5 minutes
                long dropReset = lastDropResetTime.getOrDefault(uuid, now);
                if (now - dropReset > BEHAVIOR_WINDOW) {
                    recentDropCount.put(uuid, 0);
                    lastDropResetTime.put(uuid, now);
                }
            }
        }
    }

    private static DrunkType selectWeightedType(ServerPlayerEntity player, ServerWorld world) {
        UUID uuid = player.getUuid();
        long now = world.getTime();

        // Base weight for each type
        Map<DrunkType, Float> weights = new HashMap<>();
        for (DrunkType t : DrunkType.values()) weights.put(t, 1.0f);

        // AGGRESSIVE: attacked recently
        Long lastAttack = lastAttackTime.get(uuid);
        if (lastAttack != null) {
            long ticksAgo = now - lastAttack;
            if (ticksAgo < BEHAVIOR_WINDOW) {
                // More recent = higher weight, max 5x boost
                float boost = 1.0f + (4.0f * (1.0f - (float) ticksAgo / BEHAVIOR_WINDOW));
                weights.merge(DrunkType.AGGRESSIVE, boost, Float::sum);
            }
        }

        // STUMBLING: bumped into a block recently
        Long lastCollision = lastBlockCollisionTime.get(uuid);
        if (lastCollision != null) {
            long ticksAgo = now - lastCollision;
            if (ticksAgo < BEHAVIOR_WINDOW) {
                float boost = 1.0f + (4.0f * (1.0f - (float) ticksAgo / BEHAVIOR_WINDOW));
                weights.merge(DrunkType.STUMBLING, boost, Float::sum);
            }
        }

        // SLEEPY: slept recently
        Long lastSleep = lastSleepTime.get(uuid);
        if (lastSleep != null) {
            long ticksAgo = now - lastSleep;
            if (ticksAgo < BEHAVIOR_WINDOW) {
                float boost = 1.0f + (4.0f * (1.0f - (float) ticksAgo / BEHAVIOR_WINDOW));
                weights.merge(DrunkType.SLEEPY, boost, Float::sum);
            }
        }

        // GENEROUS: dropped many items recently
        int drops = recentDropCount.getOrDefault(uuid, 0);
        if (drops > 0) {
            // Every 5 drops = +1 weight, max 5x boost
            float boost = Math.min(5.0f, drops / 5.0f);
            weights.merge(DrunkType.GENEROUS, boost, Float::sum);
        }

        // PARANOID: spun camera around a lot recently
        float yawChange = totalYawChange.getOrDefault(uuid, 0f);
        if (yawChange > 360f) {
            // Every full rotation = +0.5 weight, max 5x boost
            float boost = Math.min(5.0f, yawChange / 720f);
            weights.merge(DrunkType.PARANOID, boost, Float::sum);
        }

        // Weighted random selection
        // Weighted random selection
        float totalWeight = weights.values().stream().reduce(0f, Float::sum);
        float roll = world.random.nextFloat() * totalWeight;
        float cumulative = 0f;

        for (DrunkType type : DrunkType.values()) {
            cumulative += weights.get(type);
            if (roll < cumulative) {
                return type;
            }
        }

        return DrunkType.values()[world.random.nextInt(DrunkType.values().length)];
    }

    static void tick(MinecraftServer server) {
        trackBehavior(server);

        for (var world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.hasStatusEffect(Szar.DRUNK_EFFECT)) {
                    playerTypes.remove(player.getUuid());
                    sleepyTimer.remove(player.getUuid());
                    continue;
                }

                DrunkType type = playerTypes.computeIfAbsent(player.getUuid(), k -> {
                    DrunkType assigned = selectWeightedType(player, world);

                    String typeName = assigned.name().charAt(0)
                            + assigned.name().substring(1).toLowerCase();
                    net.minecraft.network.PacketByteBuf buf =
                            net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                    buf.writeString(typeName);
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, Szar.DRUNK_TYPE_PACKET, buf);

                    return assigned;
                });

                switch (type) {
                    case AGGRESSIVE -> tickAggressive(world, player);
                    case STUMBLING -> tickStumbling(world, player);
                    case SLEEPY -> tickSleepy(world, player);
                    case GENEROUS -> tickGenerous(world, player);
                    case PARANOID -> tickParanoid(world, player);
                }
            }
        }
    }

    // --- AGGRESSIVE ---
    private static void tickAggressive(net.minecraft.server.world.ServerWorld world,
                                       ServerPlayerEntity player) {
        if (world.getTime() % 20 != 0) return;
        if (world.random.nextInt(5) >= 2) return;

        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVector();
        Vec3d endPos = eyePos.add(lookVec.multiply(4.5));

        EntityHitResult hit = ProjectileUtil.getEntityCollision(
                world, player, eyePos, endPos,
                player.getBoundingBox().stretch(lookVec.multiply(4.5)).expand(1.0),
                e -> e instanceof LivingEntity && e != player && !e.isSpectator()
        );

        if (hit == null) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;

        player.attack(target);
        player.swingHand(Hand.MAIN_HAND);
        player.networkHandler.sendPacket(new EntityAnimationS2CPacket(player, 0));
    }

    // --- STUMBLING ---
    private static void tickStumbling(net.minecraft.server.world.ServerWorld world,
                                      ServerPlayerEntity player) {
        if (world.getTime() % 10 != 0) return;
        if (world.random.nextInt(4) != 0) return;

        Vec3d current = player.getVelocity();
        double pushX = (world.random.nextDouble() - 0.5) * 0.4;
        double pushZ = (world.random.nextDouble() - 0.5) * 0.4;
        player.setVelocity(current.x + pushX, current.y, current.z + pushZ);
        player.velocityModified = true;
    }

    // --- SLEEPY ---
    private static void tickSleepy(net.minecraft.server.world.ServerWorld world,
                                   ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int timer = sleepyTimer.getOrDefault(uuid, 0);

        if (timer > 0) {
            player.setVelocity(0, player.getVelocity().y, 0);
            player.velocityModified = true;
            player.setSneaking(true);
            sleepyTimer.put(uuid, timer - 1);
        } else {
            player.setSneaking(false);
            if (world.getTime() % 60 == 0 && world.random.nextInt(4) == 0) {
                int sleepDuration = 20 + world.random.nextInt(40);
                sleepyTimer.put(uuid, sleepDuration);
            }
        }
    }

    // --- GENEROUS ---
    private static void tickGenerous(net.minecraft.server.world.ServerWorld world,
                                     ServerPlayerEntity player) {
        if (world.getTime() % 60 != 0) return;
        if (world.random.nextInt(4) != 0) return;

        var inv = player.getInventory();
        java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) nonEmpty.add(i);
        }
        if (nonEmpty.isEmpty()) return;

        int slot = nonEmpty.get(world.random.nextInt(nonEmpty.size()));
        ItemStack stack = inv.getStack(slot);

        ItemStack toDrop = stack.copy();
        toDrop.setCount(1);
        stack.decrement(1);
        if (stack.isEmpty()) inv.setStack(slot, ItemStack.EMPTY);

        player.dropItem(toDrop, false);
    }

    // --- PARANOID ---
    private static void tickParanoid(net.minecraft.server.world.ServerWorld world,
                                     ServerPlayerEntity player) {
        if (world.getTime() % 20 != 0) return;
        if (world.random.nextInt(3) != 0) return;

        double offsetX = (world.random.nextDouble() - 0.5) * 10;
        double offsetZ = (world.random.nextDouble() - 0.5) * 10;
        double x = player.getX() + offsetX;
        double y = player.getY();
        double z = player.getZ() + offsetZ;

        net.minecraft.sound.SoundEvent[] footsteps = {
                SoundEvents.BLOCK_STONE_STEP,
                SoundEvents.BLOCK_GRAVEL_STEP,
                SoundEvents.BLOCK_WOOD_STEP,
                SoundEvents.BLOCK_SAND_STEP
        };
        net.minecraft.sound.SoundEvent sound =
                footsteps[world.random.nextInt(footsteps.length)];

        player.networkHandler.sendPacket(
                new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                        world.getRegistryManager()
                                .get(net.minecraft.registry.RegistryKeys.SOUND_EVENT)
                                .getEntry(sound),
                        SoundCategory.PLAYERS,
                        x, y, z,
                        0.8f + world.random.nextFloat() * 0.4f,
                        0.8f + world.random.nextFloat() * 0.4f,
                        world.random.nextLong()
                )
        );
    }

    @Override
    public Text getName() {
        if (currentDisplayType.isEmpty()) return Text.literal("Drunk");
        return Text.literal("Drunk ")
                .append(Text.literal("(" + currentDisplayType + ")")
                        .formatted(net.minecraft.util.Formatting.GRAY));
    }

    public static void setDisplayType(String typeName) {
        currentDisplayType = typeName;
    }

    public static void preAssignType(UUID uuid, DrunkType type) {
        playerTypes.put(uuid, type);
    }
}