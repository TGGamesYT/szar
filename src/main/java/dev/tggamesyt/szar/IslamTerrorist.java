package dev.tggamesyt.szar;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IslamTerrorist extends PathAwareEntity implements Arrestable{
    private BlockPos targetCoreBlock = null; // the core block this mob is attacking
    private Vec3d taxiDirection;
    private Vec3d currentDirection = null; // direction plane is moving
    private BlockPos taxiTarget;
    private int flyStraightTicks = 0;
    private int planeTaxiTicks = 0;
    public static boolean arrestable = false;
    private int BlowUpCooldown = 0;
    private int panicTicks = 0;
    private UUID fleeingFrom = null;

    public IslamTerrorist(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(true);
    }

    // ================= ATTRIBUTES =================
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0);
    }

    // ================= GOALS =================
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new PanicRandomlyGoal(this));
        this.goalSelector.add(1, new FleeSpecificPlayerGoal(this));
        this.goalSelector.add(3, new SneakBehindPlayerGoal(this));
        this.goalSelector.add(4, new BiasedWanderGoal(this, 0.6));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    // ================= TICK =================
    @Override
    public void tick() {
        super.tick();

        if (BlowUpCooldown > 0) BlowUpCooldown--;

        Entity vehicle = this.getVehicle();
        if (!(vehicle instanceof PlaneEntity plane) || targetCoreBlock == null) return;

        Vec3d vel = plane.getVelocity();

        // -------------------------
        // TAXI PHASE: ground, random direction
        // -------------------------
        if (planeTaxiTicks > 0) {
            planeTaxiTicks--;

            if (taxiTarget != null) {
                Vec3d toTarget = Vec3d.ofCenter(taxiTarget).subtract(plane.getPos());
                if (toTarget.length() > 0.5) {
                    Vec3d desired = toTarget.normalize().multiply(0.4);
                    plane.setVelocity(vel.lerp(desired, 0.1));
                }
            }

            // small lift near end
            if (planeTaxiTicks < 20) {
                plane.setVelocity(plane.getVelocity().x, 0.08, plane.getVelocity().z);
            }

            // Face movement
            Vec3d look = plane.getVelocity().normalize();
            if (look.length() > 0) {
                plane.setYaw((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
                plane.setPitch((float) -Math.toDegrees(Math.asin(look.y)));
            }

            return;
        }

        // -------------------------
        // FLY STRAIGHT PHASE
        // -------------------------
        if (flyStraightTicks > 0) {
            flyStraightTicks--;

            plane.setVelocity(currentDirection.x * 1.4, 0.25, currentDirection.z * 1.4);

            Vec3d look = plane.getVelocity().normalize();
            if (look.length() > 0) {
                plane.setYaw((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
                plane.setPitch((float) -Math.toDegrees(Math.asin(look.y)));
            }

            return;
        }

        // -------------------------
        // HOMING PHASE
        // -------------------------
        Vec3d target = Vec3d.ofCenter(targetCoreBlock);
        Vec3d toTarget = target.subtract(plane.getPos());
        Vec3d desired = toTarget.normalize().multiply(1.8);

        // Add small upward lift if below target
        if (plane.getY() < target.y - 10) {
            desired = desired.add(0, 0.2, 0);
        }

        // Smooth turning toward target
        currentDirection = currentDirection.lerp(desired.normalize(), 0.03).normalize();

        plane.setVelocity(currentDirection.multiply(1.8));

        // Face movement
        Vec3d look = plane.getVelocity().normalize();
        if (look.length() > 0) {
            plane.setYaw((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
            plane.setPitch((float) -Math.toDegrees(Math.asin(look.y)));
        }

        // -------------------------
        // IMPACT
        // -------------------------
        if (!getWorld().isClient &&
                (plane.horizontalCollision || plane.verticalCollision)) {

            getWorld().createExplosion(
                    plane,
                    plane.getX(),
                    plane.getY(),
                    plane.getZ(),
                    7.0f,
                    World.ExplosionSourceType.TNT
            );

            plane.discard();
            this.discard();

            BlockEntity be = getWorld().getBlockEntity(targetCoreBlock);
            if (be instanceof ObeliskCoreBlockEntity core) {
                core.setHasPlaneMob(false);
                core.markDirty();
            }

            targetCoreBlock = null;
        }
    }

    // ================= VISIBILITY =================
    private boolean isOnPlayerScreen(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEntity = this.getPos().subtract(player.getEyePos()).normalize();
        return look.dotProduct(toEntity) > 0.55;
    }

    // ================= STEALING =================
    private void triggerExposion(PlayerEntity player) {
        if (this.getWorld().isClient) return;

        // Spawn primed TNT
        TntEntity tnt = new TntEntity(
                this.getWorld(),
                this.getX(),
                this.getY(),
                this.getZ(),
                this
        );

        tnt.setFuse(40); // 2 seconds (80 = normal TNT)
        this.getWorld().spawnEntity(tnt);

        // Panic + flee
        this.fleeingFrom = player.getUuid();
        this.panicTicks = 100;
        this.BlowUpCooldown = 20*10;

        // Immediate movement impulse away from player
        Vec3d runDir = this.getPos()
                .subtract(player.getPos())
                .normalize()
                .multiply(1.2);

        this.addVelocity(runDir.x, 0.3, runDir.z);
        this.velocityDirty = true;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (targetCoreBlock == null) return;

        BlockEntity be = getWorld().getBlockEntity(targetCoreBlock);
        if (be instanceof ObeliskCoreBlockEntity core) {
            core.setHasPlaneMob(false);
            core.markDirty();
        }
    }


    // ================= DAMAGE =================

    // ================= GOALS =================

    private static class PanicRandomlyGoal extends Goal {
        private final IslamTerrorist mob;
        PanicRandomlyGoal(IslamTerrorist mob) { this.mob = mob; this.setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return mob.panicTicks > 0; }
        @Override
        public void tick() {
            mob.panicTicks--;
            if (mob.getNavigation().isIdle()) {
                Vec3d dest = mob.getPos().add(
                        mob.random.nextGaussian() * 8,
                        0,
                        mob.random.nextGaussian() * 8
                );
                mob.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.5);
            }
        }
    }

    @Override
    public EntityData initialize(
            ServerWorldAccess worldAccess,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            @Nullable EntityData entityData,
            @Nullable NbtCompound entityNbt
    ) {
        EntityData data = super.initialize(worldAccess, difficulty, spawnReason, entityData, entityNbt);

        if (!(worldAccess instanceof ServerWorld world)) return data;

        BlockPos corePos = findNearbyCoreBlock(this.getBlockPos(), 100);
        if (corePos == null) return data;

        BlockEntity be = world.getBlockEntity(corePos);
        if (!(be instanceof ObeliskCoreBlockEntity core)) return data;

        if (!core.hasPlaneMob() && world.random.nextInt(100) == 0) {

            PlaneEntity plane = new PlaneEntity(Szar.PLANE_ENTITY_TYPE, world);
            plane.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());

            world.spawnEntity(plane);
            this.startRiding(plane, true);

            core.setHasPlaneMob(true);
            core.markDirty();

            this.targetCoreBlock = corePos;

            // Taxi + straight flight setup
            this.planeTaxiTicks = 60;  // 3 seconds ground taxi
            this.flyStraightTicks = 40; // 2 seconds straight flight before homing

            // Random horizontal direction
            float randomYaw = world.random.nextFloat() * 360f;
            this.currentDirection = Vec3d.fromPolar(0, randomYaw).normalize();

            // Taxi target 20 blocks ahead in random direction
            this.taxiTarget = this.getBlockPos().add(
                    (int)(currentDirection.x * 20),
                    0,
                    (int)(currentDirection.z * 20)
            );
        }

        return data;
    }

    private BlockPos findNearbyCoreBlock(BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (getWorld().getBlockState(check).getBlock() == Szar.OBELISK_CORE) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    // 🔴 Flee from only specific victim, hide behind others
    private static class FleeSpecificPlayerGoal extends Goal {
        private final IslamTerrorist mob;
        private PlayerEntity threat;

        FleeSpecificPlayerGoal(IslamTerrorist mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (mob.fleeingFrom == null) return false;
            PlayerEntity p = mob.getWorld().getPlayerByUuid(mob.fleeingFrom);
            if (p == null || !mob.canSee(p) || !mob.isOnPlayerScreen(p)) return false;
            threat = p;
            return true;
        }

        @Override
        public void tick() {
            TargetPredicate predicate = TargetPredicate.createNonAttackable()
                    .setBaseMaxDistance(16)
                    .setPredicate(player -> !player.getUuid().equals(mob.fleeingFrom));

            PlayerEntity shield = mob.getWorld().getClosestPlayer(predicate, mob);

            Vec3d dest;
            if (shield != null && !shield.isCreative()) {
                dest = shield.getPos(); // hide behind other players
            } else {
                dest = mob.getPos().subtract(threat.getPos()).normalize().multiply(10).add(mob.getPos());
            }

            mob.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.3);
        }
    }

    // 🟡 Sneak steal
    private static class SneakBehindPlayerGoal extends Goal {
        private final IslamTerrorist mob;
        private PlayerEntity target;
        private int cooldown = 0;

        SneakBehindPlayerGoal(IslamTerrorist mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            target = mob.getWorld().getClosestPlayer(mob, 10);
            return target != null
                    && mob.BlowUpCooldown == 0
                    && !mob.isOnPlayerScreen(target)
                    && !target.isCreative();
        }

        @Override
        public void tick() {
            if (cooldown-- > 0) return;
            cooldown = 5;

            Vec3d behind = target.getPos().subtract(target.getRotationVec(1.0F).normalize());
            mob.getNavigation().startMovingTo(behind.x, behind.y, behind.z, 1.15);

            if (mob.distanceTo(target) < 1.3) {
                mob.triggerExposion(target);
                arrestable = true;
            }
        }
    }

    // 🟢 Biased wander toward players when not guilty
    private static class BiasedWanderGoal extends WanderAroundFarGoal {
        private final IslamTerrorist mob;

        BiasedWanderGoal(IslamTerrorist mob, double speed) {
            super(mob, speed);
            this.mob = mob;
        }

        @Override
        protected Vec3d getWanderTarget() {
            Vec3d base = super.getWanderTarget();
            PlayerEntity player = mob.getWorld().getClosestPlayer(mob, 10);

            if (player == null || base == null || mob.fleeingFrom != null) return base;

            Vec3d best = base;
            double bestDist = base.squaredDistanceTo(player.getPos());

            for (int i = 0; i < 4; i++) {
                Vec3d c = super.getWanderTarget();
                if (c == null) continue;
                double d = c.squaredDistanceTo(player.getPos());
                if (d < bestDist) { // bias toward player
                    best = c;
                    bestDist = d;
                }
            }
            return best;
        }
    }
    @Override
    public boolean isArrestable() {
        return arrestable;
    }
}
