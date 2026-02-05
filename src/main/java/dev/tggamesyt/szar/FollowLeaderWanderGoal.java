package dev.tggamesyt.szar;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.random.Random;

public class FollowLeaderWanderGoal extends Goal {
    private final PathAwareEntity mob;
    private final double speed;
    private final float radius;

    public FollowLeaderWanderGoal(PathAwareEntity mob, double speed, float radius) {
        this.mob = mob;
        this.speed = speed;
        this.radius = radius;
    }

    @Override
    public boolean canStart() {
        if (!(mob instanceof NaziEntity nazi)) return false;
        HitterEntity leader = nazi.getLeader();
        return leader != null && leader.isAlive();
    }

    @Override
    public void start() {
        NaziEntity nazi = (NaziEntity) mob;
        HitterEntity leader = nazi.getLeader();
        if (leader == null) return;

        Random random = mob.getRandom();

        double offsetX = (random.nextDouble() - 0.5) * radius * 2;
        double offsetZ = (random.nextDouble() - 0.5) * radius * 2;

        mob.getNavigation().startMovingTo(
                leader.getX() + offsetX,
                leader.getY(),
                leader.getZ() + offsetZ,
                speed
        );
    }
}
