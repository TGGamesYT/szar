package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class IslamTerrorist extends PathAwareEntity {

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
}
