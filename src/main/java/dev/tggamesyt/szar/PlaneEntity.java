package dev.tggamesyt.szar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlaneEntity extends Entity {

    private static final TrackedData<Float> ENGINE_TARGET =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private PlaneAnimation currentServerAnimation = null;
    private float enginePower = 0f;
    private double  lastY;
    @Environment(EnvType.CLIENT)
    private PlaneAnimation currentAnimation;

    @Environment(EnvType.CLIENT)
    private long animationStartTick;

    @Environment(EnvType.CLIENT)
    private boolean looping;

    public PlaneEntity(EntityType<? extends PlaneEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setNoGravity(false); // FORCE gravity ON
    }


    @Override
    protected void initDataTracker() {
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
        if (player == null) {
            Vec3d velocity = getVelocity();

            // Apply gravity even without pilot
            velocity = velocity.add(0, -0.04, 0);

            velocity = velocity.multiply(0.98);

            setVelocity(velocity);
            move(MovementType.SELF, velocity);
            return;
        }


        /* --------------------------------
           CONTROLLER (AircraftEntity logic)
        -------------------------------- */

        // YAW
        setYaw(getYaw() - player.sidewaysSpeed * 4.0f);

        // PITCH (only in air)
        if (!isOnGround()) {
            setPitch(getPitch() - player.forwardSpeed * 2.5f);
        }

        // Stabilizer (small auto leveling)
        setPitch(getPitch() * 0.98f);

        /* --------------------------------
           THROTTLE (AirplaneEntity logic)
        -------------------------------- */

        if (player.jumping) {
            setEngineTarget(getEngineTarget() + 0.02f);
        }

        if (player.isSneaking()) {
            setEngineTarget(getEngineTarget() - 0.02f);
        }

        // Smooth engine reaction
        enginePower += (getEngineTarget() - enginePower) * 0.05f;

/* --------------------------------
   PHYSICS (STABLE VERSION)
-------------------------------- */

        Vec3d forward = getRotationVector().normalize();
        Vec3d velocity = getVelocity();

        /* ---------- REALIGN VELOCITY ---------- */
        /* Prevents internal momentum stacking */

        double speed = velocity.length();

        if (speed > 0.001) {
            // Stronger forward locking
            double alignment = 0.25; // was 0.08
            Vec3d newDir = velocity.normalize().lerp(forward, alignment).normalize();
            velocity = newDir.multiply(speed);
        }


        /* ---------- THRUST ---------- */

        double thrust = Math.pow(enginePower, 2.0) * 0.08;
        velocity = velocity.add(forward.multiply(thrust));

        /* ---------- GLIDE ---------- */

        double diffY = lastY - getY();
        if (lastY != 0.0 && diffY != 0.0) {
            velocity = velocity.add(
                    forward.multiply(diffY * 0.04 * (1.0 - Math.abs(forward.y)))
            );
        }
        lastY = getY();

        /* ---------- DYNAMIC GRAVITY ---------- */

        double horizontalSpeed = velocity.length() * (1.0 - Math.abs(forward.y));
        double gravityFactor = Math.max(0.0, 1.0 - horizontalSpeed * 1.5);

// ALWAYS apply — do not check hasNoGravity()
        velocity = velocity.add(0, -0.04 * gravityFactor, 0);

        /* ---------- DRAG ---------- */

        velocity = velocity.multiply(0.98);

        /* ---------- MAX SPEED CLAMP ---------- */

        double maxSpeed = 1.5;
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }

        setVelocity(velocity);
        move(MovementType.SELF, velocity);

        /* --------------------------------
   ANIMATION STATE SYNC
-------------------------------- */

            boolean hasPassenger = getControllingPassenger() != null;

            if (!hasPassenger) {
                playServerAnimation(null);
            }
            else if (enginePower < 0.05f) {
                playServerAnimation(PlaneAnimation.START_ENGINE);
            }
            else if (isOnGround()) {
                playServerAnimation(PlaneAnimation.LAND_STARTED);
            }
            else {
                playServerAnimation(PlaneAnimation.FLYING);
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

}
