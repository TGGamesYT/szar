package dev.tggamesyt.szar;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import net.minecraft.entity.ai.goal.Goal;

public class KidWanderGoal extends Goal {
    private final KidEntity kid;
    private final double speed;

    public KidWanderGoal(KidEntity kid, double speed) {
        this.kid = kid;
        this.speed = speed;
    }

    @Override
    public boolean canStart() {
        return kid.getNavigation().isIdle();
    }

    @Override
    public void tick() {
        BlockPos target;

        if (kid.homePosition != null) {
            // stay in the small radius set by parent
            target = kid.homePosition.add(
                    kid.getRandom().nextInt(kid.homeRadius * 2 + 1) - kid.homeRadius,
                    0,
                    kid.getRandom().nextInt(kid.homeRadius * 2 + 1) - kid.homeRadius
            );
        } else {
            // wander near nearest parent player
            BlockPos parentPos = kid.getNearestParentPos();
            if (parentPos == null) return; // no parents online
            target = parentPos.add(
                    kid.getRandom().nextInt(41) - 20,
                    0,
                    kid.getRandom().nextInt(41) - 20
            );
        }

        kid.getNavigation().startMovingTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }
}