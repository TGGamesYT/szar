package dev.tggamesyt.szar;

import dev.tggamesyt.szar.Szar;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.EnumSet;
import java.util.List;

public class PoliceArrestGoal extends Goal {
    private final PathAwareEntity police;
    private LivingEntity target;
    private static final int ARREST_DURATION = 2400; // 2 minutes in ticks

    public PoliceArrestGoal(PathAwareEntity police) {
        this.police = police;
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
    }

    @Override
    public boolean canStart() {
        // 1️⃣ Find nearest arrestable mob
        List<? extends MobEntity> nearby = police.getWorld().getEntitiesByClass(
                MobEntity.class,
                police.getBoundingBox().expand(16),
                mob -> isArrestable(mob)
        );

        if (!nearby.isEmpty()) {
            target = nearby.get(0);
            return true;
        }

        // 2️⃣ Find entities attacking villagers or police
        List<LivingEntity> possibleTargets = police.getWorld().getEntitiesByClass(
                LivingEntity.class,
                police.getBoundingBox().expand(16),
                entity -> isAttackingProtected(entity)
        );

        if (!possibleTargets.isEmpty()) {
            target = possibleTargets.get(0);
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        police.getNavigation().startMovingTo(target, 1.2D);
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            police.setTarget(null);
            return;
        }

        police.setTarget(target);
        police.getNavigation().startMovingTo(target, 1.2D);

        double distanceSq = police.squaredDistanceTo(target);
        if (distanceSq < 4.0D) { // ~2 blocks
            // Swing the hand
            police.swingHand(Hand.MAIN_HAND);

            // Deal half a heart using vanilla attack system
            police.tryAttack(target); // this uses vanilla attack logic and triggers death messages

            // Apply arrested effect
            target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    Szar.ARRESTED, ARREST_DURATION, 0
            ));

            // Reset target so Police doesn’t spam the effect
            target = null;
        }
    }



    @Override
    public boolean shouldContinue() {
        return target != null && target.isAlive();
    }

    private boolean isArrestable(MobEntity mob) {
        try {
            return mob.getClass().getField("arrestable").getBoolean(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

    private boolean isAttackingProtected(LivingEntity entity) {
        // Check if entity is currently attacking a villager or police
        if (entity instanceof MobEntity mob) {
            LivingEntity targetEntity = mob.getTarget();
            if (targetEntity instanceof PlayerEntity player) {
                return false; // optional: ignore if player attacking non-protected
            }
            return targetEntity instanceof MobEntity protectedEntity &&
                    (protectedEntity instanceof PathAwareEntity p && p.getClass() == police.getClass()
                            || protectedEntity.getType().getSpawnGroup().isPeaceful());
        } else if (entity instanceof PlayerEntity) {
            // Check if player recently attacked villager or police
            // You may need to track recent attacks in an event listener
            return false; // placeholder, can be implemented via attack events
        }
        return false;
    }
}
