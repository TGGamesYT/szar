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

    // Per-player assigned drunk type — assigned when effect is first detected
    private static final Map<UUID, DrunkType> playerTypes = new HashMap<>();
    // For sleepy — track how long the player is forced to crouch
    private static final Map<UUID, Integer> sleepyTimer = new HashMap<>();
    static String currentDisplayType = "None";
    public DrunkEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF9900);
    }

    static void tick(MinecraftServer server) {
        for (var world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.hasStatusEffect(Szar.DRUNK_EFFECT)) {
                    // Clean up when effect ends
                    playerTypes.remove(player.getUuid());
                    sleepyTimer.remove(player.getUuid());
                    continue;
                }

                // Assign type if not yet assigned
                DrunkType type = playerTypes.computeIfAbsent(player.getUuid(), k -> {
                    DrunkType[] values = DrunkType.values();
                    DrunkType assigned = values[world.random.nextInt(values.length)];

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

        // Add a random sideways velocity push
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
            // Force crouch and stop movement
            player.setVelocity(0, player.getVelocity().y, 0);
            player.velocityModified = true;
            player.setSneaking(true);
            sleepyTimer.put(uuid, timer - 1);
        } else {
            player.setSneaking(false);
            // Every 3 seconds, 1 in 4 chance to fall asleep for 1-3 seconds
            if (world.getTime() % 60 == 0 && world.random.nextInt(4) == 0) {
                int sleepDuration = 20 + world.random.nextInt(40); // 1-3 seconds
                sleepyTimer.put(uuid, sleepDuration);
            }
        }
    }

    // --- GENEROUS ---
    private static void tickGenerous(net.minecraft.server.world.ServerWorld world,
                                     ServerPlayerEntity player) {
        if (world.getTime() % 60 != 0) return; // every 3 seconds
        if (world.random.nextInt(4) != 0) return;

        // Find a random non-empty slot
        var inv = player.getInventory();
        java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) nonEmpty.add(i);
        }
        if (nonEmpty.isEmpty()) return;

        int slot = nonEmpty.get(world.random.nextInt(nonEmpty.size()));
        ItemStack stack = inv.getStack(slot);

        // Drop one item from the stack
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

        // Play a footstep sound at a random position nearby
        double offsetX = (world.random.nextDouble() - 0.5) * 10;
        double offsetZ = (world.random.nextDouble() - 0.5) * 10;
        double x = player.getX() + offsetX;
        double y = player.getY();
        double z = player.getZ() + offsetZ;

        // Pick a random footstep sound
        net.minecraft.sound.SoundEvent[] footsteps = {
                SoundEvents.BLOCK_STONE_STEP,
                SoundEvents.BLOCK_GRAVEL_STEP,
                SoundEvents.BLOCK_WOOD_STEP,
                SoundEvents.BLOCK_SAND_STEP
        };
        net.minecraft.sound.SoundEvent sound =
                footsteps[world.random.nextInt(footsteps.length)];

        // Play only to this player
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
