package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;

import java.util.EnumSet;

public class AK47AttackGoal extends Goal {

    private final NaziEntity mob;
    private final float range;
    private final int cooldownTicks;
    private int cooldown;

    public AK47AttackGoal(NaziEntity mob, float range, int cooldownTicks) {
        this.mob = mob;
        this.range = range;
        this.cooldownTicks = cooldownTicks;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return mob.getTarget() != null && mob.getTarget().isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        mob.getLookControl().lookAt(target, 30.0F, 30.0F);

        double distanceSq = mob.squaredDistanceTo(target);
        if (distanceSq > range * range) {
            mob.getNavigation().startMovingTo(target, 1.0);
            return;
        }

        mob.getNavigation().stop();

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        shoot(target);
        cooldown = cooldownTicks;
    }

    private void shoot(LivingEntity target) {
        BulletEntity bullet = new BulletEntity(mob.getWorld(), mob);

        double dx = target.getX() - mob.getX();
        double dy = target.getBodyY(0.5) - bullet.getY();
        double dz = target.getZ() - mob.getZ();

        float velocity = 4.5F;
        float inaccuracy = 1.0F;

        bullet.setVelocity(
                dx,
                dy,
                dz,
                velocity,
                inaccuracy
        );

        mob.getWorld().spawnEntity(bullet);
    }
}
