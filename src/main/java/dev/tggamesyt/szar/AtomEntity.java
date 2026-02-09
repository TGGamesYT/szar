package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AtomEntity extends Entity {
    private static final int NUKE_RADIUS = 100;
    public AtomEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {}

    @Override
    public void tick() {
        super.tick();

        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0, -0.08, 0));
        }

        this.move(MovementType.SELF, this.getVelocity());

        if (!getWorld().isClient && this.isOnGround()) {
            explode();
            this.discard();
        }
    }

    private void explode() {
        ServerWorld world = (ServerWorld) this.getWorld();

        // Visual / sound explosion only
        world.createExplosion(
                this,
                getX(),
                getY(),
                getZ(),
                50.0F, // just visuals
                World.ExplosionSourceType.TNT
        );

        clearSphere(world, this.getBlockPos(), NUKE_RADIUS);
    }
    private void clearSphere(ServerWorld world, BlockPos center, int radius) {
        int rSquared = radius * radius;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    if (x * x + y * y + z * z > rSquared) continue;

                    mutable.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );

                    // Skip void / out of world
                    if (!world.isInBuildLimit(mutable)) continue;

                    world.setBlockState(mutable, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
                }
            }
        }
    }


    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
