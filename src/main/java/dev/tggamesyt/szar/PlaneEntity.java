package dev.tggamesyt.szar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlaneEntity extends Entity {

    private static final TrackedData<Float> ENGINE_TARGET =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> DAMAGE_WOBBLE_TICKS =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> DAMAGE_WOBBLE_STRENGTH =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> DAMAGE_WOBBLE_SIDE =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private PlaneAnimation currentServerAnimation = null;
    private float enginePower = 0f;
    private double  lastY;
    double stallSpeed = 1.0;
    double explodeSpeed = 1.0;
    private int brakeHoldTicks = 0;
    @Environment(EnvType.CLIENT)
    private PlaneAnimation currentAnimation;

    @Environment(EnvType.CLIENT)
    private long animationStartTick;

    @Environment(EnvType.CLIENT)
    private boolean looping;

    public PlaneEntity(EntityType<? extends PlaneEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setStepHeight(2.0f);
        this.setNoGravity(false); // FORCE gravity ON
    }


    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(DAMAGE_WOBBLE_TICKS, 0);
        this.dataTracker.startTracking(DAMAGE_WOBBLE_STRENGTH, 0f);
        this.dataTracker.startTracking(DAMAGE_WOBBLE_SIDE, 1);
        this.dataTracker.startTracking(ENGINE_TARGET, 0f);
    }
    @Override
    public boolean isCollidable() {
        return false;
    }
    @Override
    public boolean canHit() {
        return true;
    }

    public float getEngineTarget() {
        return dataTracker.get(ENGINE_TARGET);
    }

    public void setEngineTarget(float value) {
        dataTracker.set(ENGINE_TARGET, MathHelper.clamp(value, 0f, 1f));
    }

    public float getEnginePower() {
        return enginePower;
    }

    @Override
    public void tick() {
        super.tick();
        PlayerEntity player = getControllingPassenger();

        // -----------------------------
        // No pilot: just apply basic gravity
        // -----------------------------
        if (player == null) {
            Vec3d velocity = getVelocity().add(0, -0.04, 0).multiply(0.95);
            setVelocity(velocity);
            move(MovementType.SELF, velocity);
            return;
        }

        // -----------------------------
        // Yaw & pitch control
        // -----------------------------
        setYaw(getYaw() - player.sidewaysSpeed * 4.0f);
        if (!isOnGround() || getVelocity().length() > 0.9) setPitch(getPitch() - player.forwardSpeed * 1.5f);
        setPitch(getPitch() * 0.98f); // auto leveling
        player.setInvisible(true);

        // -----------------------------
        // Engine target adjustments (server authoritative)
        // -----------------------------
        boolean forward = !getWorld().isClient && PlayerMovementManager.isForwardPressed((ServerPlayerEntity) player);
        boolean braking = !getWorld().isClient && PlayerMovementManager.isBackwardPressed((ServerPlayerEntity) player);

        if (forward) setEngineTarget(getEngineTarget() + 0.02f);

        if (braking) {
            brakeHoldTicks++;
            float baseBrake = isOnGround() ? 0.04f : 0.015f;
            float progressive = Math.min(brakeHoldTicks * 0.0035f, 0.15f);
            float brakeStrength = baseBrake + progressive;
            setEngineTarget(getEngineTarget() - brakeStrength);

            // Apply actual braking locally
            Vec3d vel = getVelocity();
            if (vel.lengthSquared() > 0.0001) {
                Vec3d brakeDir = vel.normalize().multiply(-brakeStrength * 0.6);
                setVelocity(vel.add(brakeDir));
            }
        } else {
            brakeHoldTicks = 0;
        }

        // -----------------------------
        // Engine power smoothing
        // -----------------------------
        float lerpSpeed = braking ? 0.25f : 0.05f;
        enginePower += (getEngineTarget() - enginePower) * lerpSpeed;

        // -----------------------------
        // PHYSICS (runs on both client & server)
        // -----------------------------
        Vec3d forwardVec = getRotationVector().normalize();
        Vec3d velocity = getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Stall gravity in air
        if (!isOnGround() && horizontalSpeed < stallSpeed) {
            velocity = velocity.add(0, -0.2, 0);
        }

        // Forward locking (only if not braking)
        double speed = velocity.length();
        if (speed > 0.001 && enginePower > 0.01f && !braking) {
            Vec3d newDir = velocity.normalize().lerp(forwardVec, 0.25).normalize();
            velocity = newDir.multiply(speed);
        }

        // Apply thrust
        double thrust = Math.pow(enginePower, 2.0) * 0.08;
        velocity = velocity.add(forwardVec.multiply(thrust));

        // Glide (air only)
        double diffY = lastY - getY();
        if (!isOnGround() && lastY != 0.0 && diffY != 0.0) {
            velocity = velocity.add(forwardVec.multiply(diffY * 0.04 * (1.0 - Math.abs(forwardVec.y))));
        }
        lastY = getY();

        // Dynamic gravity
        horizontalSpeed = velocity.length() * (1.0 - Math.abs(forwardVec.y));
        double gravityFactor = Math.max(0.0, 1.0 - horizontalSpeed * 1.5);
        velocity = velocity.add(0, -0.04 * gravityFactor, 0);

        // Drag / friction
        velocity = isOnGround() ? velocity.multiply(0.94) : velocity.multiply(0.98);

        // Max speed clamp
        double maxSpeed = 2;
        if (velocity.length() > maxSpeed) velocity = velocity.normalize().multiply(maxSpeed);

        // Save vertical velocity before move for impact check
        Vec3d preMoveVelocity = velocity;

        // Move
        setVelocity(velocity);
        move(MovementType.SELF, velocity);

        // -----------------------------
        // Crash detection (server only)
        // Explodes if hitting block with high horizontal or vertical velocity
        // -----------------------------
        if (!getWorld().isClient) {
            double horizontalImpact = Math.sqrt(preMoveVelocity.x * preMoveVelocity.x + preMoveVelocity.z * preMoveVelocity.z);
            double verticalImpact = Math.abs(preMoveVelocity.y);

            boolean crash = (horizontalImpact > 1.5 && horizontalCollision) || (verticalImpact > explodeSpeed && verticalCollision);
            if (crash) {
                getWorld().createExplosion(this, getX(), getY(), getZ(), 9.0f, World.ExplosionSourceType.TNT);
                remove(RemovalReason.KILLED);
                return;
            }
        }

        // Stop tiny movements
        if (velocity.lengthSquared() < 0.005) velocity = Vec3d.ZERO;
        setVelocity(velocity);

        // -----------------------------
        // Animation sync
        // -----------------------------
        boolean hasPassenger = getControllingPassenger() != null;
        if (!hasPassenger) playServerAnimation(null);
        else if (enginePower < 0.05f) playServerAnimation(PlaneAnimation.START_ENGINE);
        else if (isOnGround()) playServerAnimation(PlaneAnimation.LAND_STARTED);
        else playServerAnimation(PlaneAnimation.FLYING);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof PlayerEntity player) {
            player.setInvisible(false);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("EngineTarget")) {
            setEngineTarget(nbt.getFloat("EngineTarget"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("EngineTarget", getEngineTarget());
    }


    @Nullable
    public PlayerEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof PlayerEntity p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof PlayerEntity && getPassengerList().isEmpty();
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater updater) {
        passenger.setPosition(getX(), getY() + 0.8, getZ());
    }
    private void playServerAnimation(PlaneAnimation anim) {
        if (this.currentServerAnimation == anim) return;

        this.currentServerAnimation = anim;
        Szar.playPlaneAnimation(anim, this.getId());
    }

    @Environment(EnvType.CLIENT)
    public void playAnimation(PlaneAnimation anim, boolean looping) {
        this.currentAnimation = anim;
        this.looping = looping;
        this.animationStartTick = this.age;
    }

    @Environment(EnvType.CLIENT)
    public void stopAnimation() {
        this.currentAnimation = null;
        this.looping = false;
    }

    @Environment(EnvType.CLIENT)
    @Nullable
    public PlaneAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient) {
            player.startRiding(this);
        }
        return ActionResult.SUCCESS;
    }
    public void setDamageWobbleTicks(int ticks) {
        this.dataTracker.set(DAMAGE_WOBBLE_TICKS, ticks);
    }

    public int getDamageWobbleTicks() {
        return this.dataTracker.get(DAMAGE_WOBBLE_TICKS);
    }

    public void setDamageWobbleStrength(float strength) {
        this.dataTracker.set(DAMAGE_WOBBLE_STRENGTH, strength);
    }

    public float getDamageWobbleStrength() {
        return this.dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
    }

    public void setDamageWobbleSide(int side) {
        this.dataTracker.set(DAMAGE_WOBBLE_SIDE, side);
    }

    public int getDamageWobbleSide() {
        return this.dataTracker.get(DAMAGE_WOBBLE_SIDE);
    }

    private void dropItemAsItem() {
        this.dropItem(Szar.PLANE);
    }
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (this.getWorld().isClient) return true;

        // wobble effect
        this.setDamageWobbleSide(-this.getDamageWobbleSide());
        this.setDamageWobbleTicks(10);
        this.setDamageWobbleStrength(this.getDamageWobbleStrength() + amount * 10f);

        boolean isCreative = source.getAttacker() instanceof PlayerEntity player && player.getAbilities().creativeMode;

        if (isCreative || this.getDamageWobbleStrength() > 40f) {
            if (!isCreative && this.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                this.dropItemAsItem(); // drop plane item
            }
            this.remove(RemovalReason.KILLED);
        }
        return true;
    }
}
