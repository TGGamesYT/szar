package dev.tggamesyt.szar;

import dev.tggamesyt.szar.KidEntity;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class KidGoToBedGoal extends Goal {

    private final KidEntity kid;
    private final double speed;
    private BlockPos targetBed;
    private BlockPos pathTarget;

    public KidGoToBedGoal(KidEntity kid, double speed) {
        this.kid = kid;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (kid.getWorld().isClient) return false;

        long time = kid.getWorld().getTimeOfDay() % 24000L;
        if (time < 13000L || time > 23000L) return false; // night only

        targetBed = findNearestBed();
        if (targetBed == null) return false;

        pathTarget = getAdjacentWalkable(targetBed);
        return pathTarget != null;
    }

    private BlockPos findNearestBed() {
        ServerWorld world = (ServerWorld) kid.getWorld();
        BlockPos pos = kid.getBlockPos();
        int radius = 16;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);
                    if (world.getBlockState(check).getBlock() instanceof BedBlock) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos getAdjacentWalkable(BlockPos bed) {
        // pick one of the 4 cardinal blocks adjacent to bed that is walkable
        ServerWorld world = (ServerWorld) kid.getWorld();
        BlockPos[] candidates = {
                bed.north(),
                bed.south(),
                bed.east(),
                bed.west()
        };
        for (BlockPos candidate : candidates) {
            if (world.isAir(candidate) && world.isAir(candidate.up())) {
                return candidate;
            }
        }
        return null; // no free adjacent block
    }

    @Override
    public boolean shouldContinue() {
        return pathTarget != null && !kid.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (pathTarget != null && targetBed != null) {
            kid.setSleepingPosition(targetBed);
            kid.getNavigation().startMovingTo(
                    pathTarget.getX() + 0.5,
                    pathTarget.getY(),
                    pathTarget.getZ() + 0.5,
                    speed
            );
        }
    }

    @Override
    public void tick() {
        if (pathTarget != null) {
            double dx = pathTarget.getX() + 0.5 - kid.getX();
            double dy = pathTarget.getY() - kid.getY();
            double dz = pathTarget.getZ() + 0.5 - kid.getZ();
            if (dx*dx + dy*dy + dz*dz > 1.0) { // ~1 block distance
                kid.getNavigation().startMovingTo(
                        pathTarget.getX() + 0.5,
                        pathTarget.getY(),
                        pathTarget.getZ() + 0.5,
                        speed
                );
            }
        }
    }

    @Override
    public void stop() {
        if (targetBed != null) {
            kid.clearSleepingPosition();
            targetBed = null;
            pathTarget = null;
        }
    }
}