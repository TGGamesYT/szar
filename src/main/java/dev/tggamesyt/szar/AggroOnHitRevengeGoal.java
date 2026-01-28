package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Box;
import net.minecraft.predicate.entity.EntityPredicates;

import java.util.EnumSet;
import java.util.List;

public class AggroOnHitRevengeGoal extends TrackTargetGoal {

    private static final TargetPredicate VALID_TARGET =
            TargetPredicate.createAttackable().ignoreVisibility().ignoreDistanceScalingFactor();

    private int lastHurtTime;
    private int lastAttackTime;

    public AggroOnHitRevengeGoal(PathAwareEntity mob) {
        super(mob, true);
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        LivingEntity attacker = this.mob.getAttacker();
        LivingEntity target = this.mob.getTarget();

        int hurtTime = this.mob.getLastAttackedTime();
        int attackTime = this.mob.getLastAttackTime();

        // Trigger if mob was hurt
        if (attacker != null && hurtTime != this.lastHurtTime) {
            return this.canTrack(attacker, VALID_TARGET);
        }

        // Trigger if mob attacked someone
        if (target != null && attackTime != this.lastAttackTime) {
            return this.canTrack(target, VALID_TARGET);
        }

        return false;
    }

    @Override
    public void start() {
        LivingEntity target =
                this.mob.getAttacker() != null
                        ? this.mob.getAttacker()
                        : this.mob.getTarget();

        this.mob.setTarget(target);

        this.lastHurtTime = this.mob.getLastAttackedTime();
        this.lastAttackTime = this.mob.getLastAttackTime();

        this.callForHelp(target);

        super.start();
    }

    protected void callForHelp(LivingEntity target) {
        double range = this.getFollowRange();
        Box box = this.mob.getBoundingBox().expand(range, 10.0D, range);

        List<? extends MobEntity> allies =
                this.mob.getWorld().getEntitiesByClass(
                        this.mob.getClass(),
                        box,
                        EntityPredicates.EXCEPT_SPECTATOR
                );

        for (MobEntity ally : allies) {
            if (ally != this.mob && ally.getTarget() == null) {
                ally.setTarget(target);
            }
        }
    }
}
