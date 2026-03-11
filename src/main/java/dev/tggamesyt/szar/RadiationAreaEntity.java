package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

public class RadiationAreaEntity extends Entity {

    private int lifetime = 2400;
    private float radius = 5.0F;
    private int age = 0;
    private int wave = 0; // 0 = full gravity, 1 = gravity then float, 2 = no gravity

    public RadiationAreaEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(false); // default on, wave logic overrides
    }

    public void setLifetime(int ticks) { this.lifetime = ticks; }
    public void setRadius(float r) { this.radius = r; }
    public void setWave(int wave) {
        this.wave = wave;
        // wave 2 starts with no gravity immediately
        if (wave == 2) this.setNoGravity(true);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        // ── Gravity behaviour per wave ──────────────────────────────────
        if (wave == 0) {
            // full gravity always — entity falls and stays on ground
            this.setNoGravity(false);
            if (!getWorld().isClient) {
                Vec3d vel = this.getVelocity().add(0, -0.04, 0).multiply(0.98);
                this.setVelocity(vel);
                this.move(MovementType.SELF, vel);
            }
        } else if (wave == 1) {
            if (age <= 45) {
                // first 45 ticks: fall with gravity
                this.setNoGravity(false);
                if (!getWorld().isClient) {
                    Vec3d vel = this.getVelocity().add(0, -0.04, 0).multiply(0.98);
                    this.setVelocity(vel);
                    this.move(MovementType.SELF, vel);
                }
            } else {
                // after 45 ticks: float in place
                this.setNoGravity(true);
                this.setVelocity(this.getVelocity().multiply(0.1)); // bleed off remaining velocity
            }
        }
        // wave 2: noGravity already set to true, no movement needed

        if (!getWorld().isClient) {
            if (age >= lifetime) {
                this.discard();
                return;
            }

            // apply radiation every second
            if (age % 20 == 0) {
                List<LivingEntity> nearby = getWorld().getEntitiesByClass(
                        LivingEntity.class,
                        new Box(getPos().subtract(radius, radius, radius),
                                getPos().add(radius, radius, radius)),
                        e -> e.squaredDistanceTo(this) < radius * radius
                );

                for (LivingEntity entity : nearby) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            Szar.RADIATION, 200, 1, false, true, true));
                }
            }

            // particles
            ServerWorld serverWorld = (ServerWorld) getWorld();
            for (int i = 0; i < 5; i++) {
                double px = getX() + (getWorld().random.nextDouble() - 0.5) * radius * 2;
                double py = getY() + getWorld().random.nextDouble() * 2;
                double pz = getZ() + (getWorld().random.nextDouble() - 0.5) * radius * 2;

                if (getPos().squaredDistanceTo(px, py, pz) > radius * radius) continue;

                // green glowing dust particles
                serverWorld.spawnParticles(
                        new DustParticleEffect(
                                new Vector3f(0.224f, 1.0f, 0.078f),
                                1.5F
                        ),
                        px, py, pz, 1, 0, 0.02, 0, 0.0
                );

                // warped spore for extra green floaty effect
                serverWorld.spawnParticles(ParticleTypes.WARPED_SPORE,
                        px, py, pz, 1, 0, 0.05, 0, 0.01);

                // occasional smoke
                if (getWorld().random.nextInt(10) == 0) {
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            px, py + 1, pz, 1, 0, 0.05, 0, 0.01);
                }
            }
        }
    }

    @Override
    protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.lifetime = nbt.getInt("Lifetime");
        this.radius = nbt.getFloat("Radius");
        this.age = nbt.getInt("Age");
        this.wave = nbt.getInt("Wave");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Lifetime", lifetime);
        nbt.putFloat("Radius", radius);
        nbt.putInt("Age", age);
        nbt.putInt("Wave", wave);
    }
}