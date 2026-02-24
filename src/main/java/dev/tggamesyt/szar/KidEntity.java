package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class KidEntity extends PathAwareEntity {

    private static final TrackedData<Integer> AGE =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> PARENT_A =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> PARENT_B =
            DataTracker.registerData(KidEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    BlockPos homePosition = null;
    int homeRadius = 20; // default max radius around parent
    public static final int MAX_AGE = 360000; // 30 days * 10h

    public KidEntity(EntityType<KidEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(AGE, 0);
        this.dataTracker.startTracking(PARENT_A, Optional.empty());
        this.dataTracker.startTracking(PARENT_B, Optional.empty());
    }

    public float getAgeFraction() {
        return Math.min(dataTracker.get(AGE) / (float) MAX_AGE, 1.0f);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 6F, 1.2, 1.5));
        this.goalSelector.add(2, new KidWanderGoal(this, 1.0)); // ← use our custom goal
        this.goalSelector.add(3, new LookAroundGoal(this));
    }
    public void setStayHere(BlockPos pos) {
        this.homePosition = pos;
        this.homeRadius = 5; // smaller radius for "stay here"
    }
    public void followParents() {
        this.homePosition = null;
        this.homeRadius = 20;
    }
    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClient) {
            int age = dataTracker.get(AGE);
            age++;
            dataTracker.set(AGE, age);

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

    BlockPos getNearestParentPos() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return null;

        UUID parentA = getParentA();
        UUID parentB = getParentB();

        AtomicReference<BlockPos> nearestPos = new AtomicReference<>();
        AtomicReference<Double> nearestDistance = new AtomicReference<>(Double.MAX_VALUE);

        for (UUID parentId : new UUID[]{parentA, parentB}) {
            if (parentId == null) continue;

            serverWorld.getPlayers().stream()
                    .filter(p -> p.getUuid().equals(parentId))
                    .findFirst()
                    .ifPresent(p -> {
                        double d = this.squaredDistanceTo(p);
                        if (d < nearestDistance.get()) {
                            nearestDistance.set(d);
                            nearestPos.set(p.getBlockPos());
                        }
                    });
        }

        return nearestPos.get();
    }

    public boolean onParentInteract(PlayerEntity parent) {
        UUID parentUuid = parent.getUuid();
        if (!parentUuid.equals(getParentA()) && !parentUuid.equals(getParentB())) {
            return false; // not a parent
        }

        // Set “stay here” at current kid position
        if (this.homePosition == null) {
            setStayHere(this.getBlockPos());
            parent.sendMessage(
                    Text.literal("Kid will stay here!"),
                    false
            );
        } else {
            followParents();
            parent.sendMessage(
                    Text.literal("Kid will follow parents!"),
                    false
            );
        }

        return true;
    }
}