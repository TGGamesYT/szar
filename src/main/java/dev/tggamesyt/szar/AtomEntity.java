package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class AtomEntity extends Entity {

    private static final int NUKE_RADIUS = 100;
    // vanilla clamps explosion power at 5 internally for block destruction,
    // but we can still call createExplosion with higher values for damage/visuals
    // the absolute max that does anything meaningful is around 200F
    private static final float MAX_EXPLOSION_POWER = 200.0F;

    private boolean armed = false;
    private boolean wasFallingFast = false;

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

        if (this.getVelocity().y < -0.5) {
            wasFallingFast = true;
        }

        this.move(MovementType.SELF, this.getVelocity());

        if (!getWorld().isClient) {
            if (this.isOnFire()) armed = true;

            if (this.isOnGround() && (wasFallingFast || armed)) {
                explode();
                this.discard();
            }
        }
    }
    private void explode() {
        ServerWorld world = (ServerWorld) this.getWorld();
        Vec3d center = this.getPos();

        world.createExplosion(this, center.x, center.y, center.z,
                MAX_EXPLOSION_POWER, World.ExplosionSourceType.TNT);

        int rings = 6;
        for (int ring = 1; ring <= rings; ring++) {
            double ringRadius = (NUKE_RADIUS / (double) rings) * ring;
            float ringPower = MAX_EXPLOSION_POWER * (1.0F - (ring / (float) rings));
            ringPower = Math.max(ringPower, 4.0F);

            int pointsOnRing = 6 + ring * 4;
            for (int i = 0; i < pointsOnRing; i++) {
                double angle = (2 * Math.PI / pointsOnRing) * i;
                double ex = center.x + Math.cos(angle) * ringRadius;
                double ey = center.y;
                double ez = center.z + Math.sin(angle) * ringRadius;
                world.createExplosion(this, ex, ey, ez, ringPower, World.ExplosionSourceType.TNT);
            }
        }

        // no clearSphere call anymore
        spawnRadiationZones(world, center);
    }

    private void spawnRadiationZones(ServerWorld world, Vec3d center) {
        int zoneCount = 40;

        for (int wave = 0; wave < 3; wave++) {
            for (int i = 0; i < zoneCount; i++) {
                double angle = world.random.nextDouble() * 2 * Math.PI;
                double dist = world.random.nextDouble() * NUKE_RADIUS;
                double rx = center.x + Math.cos(angle) * dist;
                double rz = center.z + Math.sin(angle) * dist;

                double ry;
                if (wave == 0) {
                    // wave 1: starts on ground, falls with gravity — spawn at ground level
                    ry = center.y + world.random.nextDouble() * 5;
                } else if (wave == 1) {
                    // wave 2: mid air, will fall then float
                    ry = center.y + 10 + world.random.nextDouble() * 20;
                } else {
                    // wave 3: high up, no gravity at all
                    ry = center.y + 30 + world.random.nextDouble() * 40;
                }

                float proximity = (float)(1.0 - (dist / NUKE_RADIUS));

                RadiationAreaEntity rad = new RadiationAreaEntity(Szar.RADIATION_AREA, world);
                rad.setPosition(rx, ry, rz);
                rad.setLifetime((int)(1200 + proximity * 4800));
                rad.setRadius(3.0F + proximity * 8.0F);
                rad.setWave(wave);
                world.spawnEntity(rad);
            }
        }
    }

    private void clearSphere(ServerWorld world, BlockPos center, int radius) {
        int rSquared = radius * radius;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > rSquared) continue;
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
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