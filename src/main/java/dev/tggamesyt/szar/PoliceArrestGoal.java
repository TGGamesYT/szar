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
        target = null;

        // 1️⃣ Find nearest arrestable mob (NOT police)
        List<? extends MobEntity> arrestables = police.getWorld().getEntitiesByClass(
                MobEntity.class,
                police.getBoundingBox().expand(16),
                mob -> mob != police && isArrestable(mob)
        );

        if (!arrestables.isEmpty()) {
            target = police.getWorld().getClosestEntity(
                    arrestables,
                    TargetPredicate.DEFAULT,
                    police,
                    police.getX(),
                    police.getY(),
                    police.getZ()
            );
            return target != null;
        }

        // 2️⃣ Find actual criminals (players or mobs attacking protected entities)
        List<LivingEntity> criminals = police.getWorld().getEntitiesByClass(
                LivingEntity.class,
                police.getBoundingBox().expand(16),
                entity -> entity != police && isAttackingProtected(entity)
        );

        if (!criminals.isEmpty()) {
            target = police.getWorld().getClosestEntity(
                    criminals,
                    TargetPredicate.DEFAULT,
                    police,
                    police.getX(),
                    police.getY(),
                    police.getZ()
            );
            return target != null;
        }

        return false;
    }


    @Override
    public void start() {
        if (target == null || !target.isAlive()) {
            return;
        }
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
        if (mob == police) return false;
        return mob instanceof Arrestable arrestable && arrestable.isArrestable();
    }


    private boolean isAttackingProtected(LivingEntity entity) {
        if (entity == police) return false;
        if (entity instanceof PlayerEntity player) {
            long lastCrime = player.getDataTracker().get(Szar.LAST_CRIME_TICK);
            return police.getWorld().getTime() - lastCrime < 200; // 10 sec window
        }

        if (entity instanceof MobEntity mob) {
            LivingEntity target = mob.getTarget();
            return target instanceof PoliceEntity
                    || target != null && target.getType().getSpawnGroup().isPeaceful();
        }

        return false;
    }

}
