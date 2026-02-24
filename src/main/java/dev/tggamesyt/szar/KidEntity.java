package dev.tggamesyt.szar;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class KidEntity extends PathAwareEntity {

    private static final TrackedData<Integer> AGE =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> PARENT_A =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> PARENT_B =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> STAYING =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final int MAX_AGE = 360000; // 30 days * 10h
    private BlockPos stayPos;
    private BlockPos lastSeenParentPos;
    public KidEntity(EntityType<KidEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(AGE, 0);
        this.dataTracker.startTracking(PARENT_A, Optional.empty());
        this.dataTracker.startTracking(PARENT_B, Optional.empty());
        this.dataTracker.startTracking(STAYING, false);
    }

    public float getAgeFraction() {
        return Math.min(dataTracker.get(AGE) / (float) MAX_AGE, 1.0f);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new KidParentBrainGoal(this, 1.3));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClient) {
            int age = dataTracker.get(AGE);
            age++;
            dataTracker.set(AGE, age);
            EntityPose current = this.getPose();
            this.setPose(EntityPose.SITTING);
            this.setPose(current);

            // Remove brutal killing; just let the kid “grow up” visually
            if (age > MAX_AGE) {
                // Optionally, you can clamp AGE to MAX_AGE to avoid overflow
                dataTracker.set(AGE, MAX_AGE);
            }

            // Forget anger quickly
            if (this.getAttacker() != null && age % 60 == 0) {
                this.setTarget(null);
                this.setAttacker(null);
            }
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
    }

    public float getGrowthScale() {
        return 0.3f + getAgeFraction() * 0.7f;
    }

    public UUID getParentA() {
        return this.dataTracker.get(PARENT_A).orElse(null);
    }

    public UUID getParentB() {
        return this.dataTracker.get(PARENT_B).orElse(null);
    }

    // Used for consistent skin generation
    public long getHybridSeed() {
        UUID a = getParentA();
        UUID b = getParentB();

        if (a == null || b == null) return 0L;

        return a.getMostSignificantBits() ^ b.getLeastSignificantBits();
    }

    public void setParents(UUID a, UUID b) {
        this.dataTracker.set(PARENT_A, Optional.ofNullable(a));
        this.dataTracker.set(PARENT_B, Optional.ofNullable(b));
    }

    public PlayerEntity getNearbyParent() {
        if (this.getWorld().isClient) return null;

        ServerWorld world = (ServerWorld) this.getWorld();

        UUID a = getParentA();
        UUID b = getParentB();

        PlayerEntity parentA = a != null ? world.getPlayerByUuid(a) : null;
        PlayerEntity parentB = b != null ? world.getPlayerByUuid(b) : null;

        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (PlayerEntity p : new PlayerEntity[]{parentA, parentB}) {
            if (p != null && p.isAlive() && !p.isSpectator()) {
                double dist = this.squaredDistanceTo(p);
                if (dist < closestDist) {
                    closest = p;
                    closestDist = dist;
                }
            }
        }

        // Render distance check (32 blocks = 1024 sq)
        if (closest != null && closestDist <= 1024) {
            return closest;
        }

        return null;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        UUID a = getParentA();
        UUID b = getParentB();

        if (a != null) nbt.putUuid("ParentA", a);
        if (b != null) nbt.putUuid("ParentB", b);

        nbt.putInt("Age", dataTracker.get(AGE));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        if (nbt.containsUuid("ParentA"))
            dataTracker.set(PARENT_A, Optional.of(nbt.getUuid("ParentA")));

        if (nbt.containsUuid("ParentB"))
            dataTracker.set(PARENT_B, Optional.of(nbt.getUuid("ParentB")));

        dataTracker.set(AGE, nbt.getInt("Age"));
    }
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && hand == Hand.MAIN_HAND) {
            boolean staying = this.dataTracker.get(STAYING);

            if (!staying) {
                player.sendMessage(Text.literal("Kid will now say here."));
                this.dataTracker.set(STAYING, true);
                this.stayPos = this.getBlockPos();
            } else {
                player.sendMessage(Text.literal("Kid will now follow parents."));
                this.dataTracker.set(STAYING, false);
            }
        }

        return ActionResult.success(this.getWorld().isClient);
    }
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        float ageFraction = getAgeFraction();
        float bodyScale = 0.3f + (1.0f - 0.3f) * ageFraction;

        EntityDimensions base = super.getDimensions(pose);

        return EntityDimensions.changing(
                base.width * bodyScale,
                base.height * bodyScale
        );
    }
    static class KidParentBrainGoal extends Goal {

        private final KidEntity kid;
        private final double speed;

        public KidParentBrainGoal(KidEntity kid, double speed) {
            this.kid = kid;
            this.speed = speed;
        }

        @Override
        public boolean canStart() {
            return true; // Always active brain
        }

        @Override
        public void tick() {

            if (kid.dataTracker.get(KidEntity.STAYING)) {
                handleStayMode();
                return;
            }

            PlayerEntity parent = kid.getNearbyParent();

            if (parent != null) {
                kid.lastSeenParentPos = parent.getBlockPos();

                double dist = kid.squaredDistanceTo(parent);

                if (dist > 120) {
                    // RUN to parent
                    kid.getNavigation().startMovingTo(parent, speed * 1.5);
                } else if (dist < 36) { // 6 blocks
                    kid.getNavigation().stop();
                } else {
                    // Wander casually near parent
                    if (kid.getNavigation().isIdle() && kid.getRandom().nextInt(80) == 0) {
                        wanderNear(parent.getBlockPos(), 6);
                    }
                }

            } else {
                // No visible parent → panic mode
                handlePanic();
            }
        }

        private void handleStayMode() {
            if (kid.stayPos == null) return;

            if (kid.getNavigation().isIdle() && kid.getRandom().nextInt(60) == 0) {
                wanderNear(kid.stayPos, 5);
            }
        }

        private void handlePanic() {

            if (kid.lastSeenParentPos == null) return;

            ServerWorld world = (ServerWorld) kid.getWorld();

            double dist = kid.squaredDistanceTo(
                    kid.lastSeenParentPos.getX(),
                    kid.lastSeenParentPos.getY(),
                    kid.lastSeenParentPos.getZ()
            );

            // ===== MOVEMENT =====

            // If far from last seen location, run there fast
            if (dist > 16) { // 4 blocks
                kid.getNavigation().startMovingTo(
                        kid.lastSeenParentPos.getX(),
                        kid.lastSeenParentPos.getY(),
                        kid.lastSeenParentPos.getZ(),
                        speed * 1.2
                );
            }
            // Once there → panic wander in 5 block radius
            else if (kid.getNavigation().isIdle() && kid.getRandom().nextInt(20) == 0) {

                double panicX = kid.lastSeenParentPos.getX() + kid.getRandom().nextInt(11) - 5;
                double panicZ = kid.lastSeenParentPos.getZ() + kid.getRandom().nextInt(11) - 5;

                kid.getNavigation().startMovingTo(panicX,
                        kid.lastSeenParentPos.getY(),
                        panicZ,
                        speed);
            }

            // ===== CRYING PARTICLES =====

            if (kid.getRandom().nextInt(2) == 0) {

                // Proper head height (accounts for body scaling)
                double headY = kid.getY() + kid.getStandingEyeHeight();

                // Face direction
                float yawRad = (float) Math.toRadians(kid.getYaw());

                // Offset to simulate left/right eyes
                double eyeOffsetX = Math.cos(yawRad) * 0.15;
                double eyeOffsetZ = Math.sin(yawRad) * 0.15;

                // Left eye
                spawnTear(world,
                        kid.getX() - eyeOffsetZ,
                        headY,
                        kid.getZ() + eyeOffsetX);

                // Right eye
                spawnTear(world,
                        kid.getX() + eyeOffsetZ,
                        headY,
                        kid.getZ() - eyeOffsetX);
            }
        }
        private void spawnTear(ServerWorld world, double x, double y, double z) {

            // Downward falling motion
            double velX = (kid.getRandom().nextDouble() - 0.5) * 0.02;
            double velY = -0.12 - kid.getRandom().nextDouble() * 0.05;
            double velZ = (kid.getRandom().nextDouble() - 0.5) * 0.02;

            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.FALLING_WATER,
                    x,
                    y,
                    z,
                    1,
                    velX,
                    velY,
                    velZ,
                    0.0
            );
        }

        private void wanderNear(BlockPos center, int radius) {
            double x = center.getX() + kid.getRandom().nextInt(radius * 2) - radius;
            double z = center.getZ() + kid.getRandom().nextInt(radius * 2) - radius;
            double y = center.getY();

            kid.getNavigation().startMovingTo(x, y, z, 1.0);
        }
    }
}