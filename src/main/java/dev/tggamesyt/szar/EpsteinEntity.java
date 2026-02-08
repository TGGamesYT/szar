package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class EpsteinEntity extends PathAwareEntity implements Arrestable {

    public static boolean arrestable = true;

    public EpsteinEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(3, new LookAroundGoal(this));
        this.targetSelector.add(1, new AttackNearbyBabiesGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        this.dropItem(Szar.EPSTEIN_FILES);
    }

    @Override
    public boolean isArrestable() {
        return arrestable;
    }

    // Custom goal class
    static class AttackNearbyBabiesGoal extends Goal {
        private final PathAwareEntity mob;
        private LivingEntity target;

        public AttackNearbyBabiesGoal(PathAwareEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.TARGET));
        }

        @Override
        public boolean canStart() {
            List<LivingEntity> entities = mob.getWorld().getEntitiesByClass(
                    LivingEntity.class,
                    mob.getBoundingBox().expand(8.0D), // 8-block radius
                    e -> (e.isBaby()) && !e.isDead() && e.isAlive()
            );
            if (!entities.isEmpty()) {
                target = entities.get(0); // pick the first one
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            if (target != null) {
                mob.getNavigation().startMovingTo(target, 1.2D);
            }
        }

        @Override
        public void tick() {
            if (target != null && target.isAlive()) {
                mob.getLookControl().lookAt(target);
                mob.getNavigation().startMovingTo(target, 1.2D);

                // Attack if in range
                if (mob.squaredDistanceTo(target) <= 2.0D) {
                    mob.tryAttack(target);
                }
            }
        }

        @Override
        public boolean shouldContinue() {
            return target != null && target.isAlive();
        }
    }
}
