package dev.tggamesyt.szar;

import dev.tggamesyt.szar.PlaneAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static dev.tggamesyt.szar.Szar.ANIMATION_TIMINGS;
import static dev.tggamesyt.szar.Szar.ANIMATION_TIMINGS_SECONDS;

public class PlaneEntity extends Entity {

    /* -------- DATA TRACKER -------- */

    private static final TrackedData<Boolean> ENGINE_ON =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final TrackedData<Boolean> IS_FLYING =
            DataTracker.registerData(PlaneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // PlaneEntity.java (CLIENT SIDE ONLY FIELDS)
    @Environment(EnvType.CLIENT)
    private PlaneAnimation currentAnimation;

    @Environment(EnvType.CLIENT)
    private long animationStartTick;

    @Environment(EnvType.CLIENT)
    private boolean looping;

    // --- SERVER STATE ---
    private PlaneAnimation currentServerAnimation = null;
    private int animationTick = 0;
    private boolean hadPassengerLastTick = false;
    private boolean wasFlyingLastTick = false;

    private void playServerAnimation(PlaneAnimation anim) {
        if (this.currentServerAnimation == anim) return;

        this.currentServerAnimation = anim;
        Szar.playPlaneAnimation(anim, this.getId());
    }
    private boolean isAboveGround(double distance) {
        return !this.getWorld().isSpaceEmpty(
                this,
                this.getBoundingBox().offset(0, -distance, 0)
        );
    }


    public PlaneEntity(EntityType<? extends PlaneEntity> type, World world) {
        super(type, world);
        this.noClip = false;
    }

    /* -------- DATA -------- */

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(ENGINE_ON, false);
        this.dataTracker.startTracking(IS_FLYING, false);
    }

    public boolean isEngineOn() {
        return dataTracker.get(ENGINE_ON);
    }

    public void setEngineOn(boolean value) {
        dataTracker.set(ENGINE_ON, value);
    }

    public boolean isFlying() {
        return dataTracker.get(IS_FLYING);
    }

    public void setFlying(boolean value) {
        dataTracker.set(IS_FLYING, value);
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

    @Environment(EnvType.CLIENT)
    public float getAnimationTime(float tickDelta) {
        if (currentAnimation == null) return 0;

        float time = (this.age + tickDelta) - animationStartTick;

        if (looping) {
            return time % ANIMATION_TIMINGS_SECONDS.getOrDefault(currentAnimation, -1f);
        }

        return Math.min(time, ANIMATION_TIMINGS_SECONDS.getOrDefault(currentAnimation, -1f));
    }


    /* -------- SAVE / LOAD -------- */

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setEngineOn(nbt.getBoolean("EngineOn"));
        setFlying(nbt.getBoolean("IsFlying"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("EngineOn", isEngineOn());
        nbt.putBoolean("IsFlying", isFlying());
    }

    /* -------- TICK -------- */

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) return;

        PlayerEntity player = getControllingPassenger();
        boolean hasPassenger = player != null;

        // Tick the current animation
        if (currentServerAnimation != null) {
            animationTick++;

            int duration = ANIMATION_TIMINGS.getOrDefault(currentServerAnimation, -1);

            // If it's a non-looping animation and finished, clear it
            if (duration != -1 && animationTick >= duration) {
                currentServerAnimation = null;
                animationTick = 0;
            }
        }

    /* --------------------------------
       LANDING / GRAVITY CHECK
    -------------------------------- */

        boolean nearGround = isAboveGround(1.5);

        if (!nearGround && isFlying()) {
            setFlying(false);
            setNoGravity(false);
            playServerAnimation(PlaneAnimation.LANDING);
        }

    /* --------------------------------
       PASSENGER STATE CHANGES
    -------------------------------- */

        if (!hadPassengerLastTick && hasPassenger) {
            setEngineOn(true);
            playServerAnimation(PlaneAnimation.START_ENGINE);
        }

        if (hadPassengerLastTick && !hasPassenger) {
            setEngineOn(false);
            setFlying(false);
            setNoGravity(false);
            playServerAnimation(PlaneAnimation.STOP_ENGINE);
            currentServerAnimation = null; // allow full stop
        }

        hadPassengerLastTick = hasPassenger;

    /* --------------------------------
       IDLE (NO PASSENGER)
    -------------------------------- */

        if (!hasPassenger) {
            setVelocity(getVelocity().multiply(0.9));
            move(MovementType.SELF, getVelocity());
            return;
        }

    /* --------------------------------
       ENGINE RUNNING (ON GROUND)
    -------------------------------- */

        if (isEngineOn() && !isFlying()) {
            playServerAnimation(PlaneAnimation.LAND_STARTED);
        }

    /* --------------------------------
   FLIGHT TAKEOFF
-------------------------------- */
        boolean nearGroundForTakeoff = isAboveGround(1.5);
        boolean wantsTakeoff = player.jumping;

        if (!isFlying()) {
            if (wantsTakeoff && nearGroundForTakeoff) {
                // Start engine if not on yet
                if (!isEngineOn()) {
                    setEngineOn(true);
                    playServerAnimation(PlaneAnimation.START_ENGINE);
                }
                // Play lift up if not already lifting
                if (currentServerAnimation != PlaneAnimation.LIFT_UP) {
                    playServerAnimation(PlaneAnimation.LIFT_UP);
                    animationTick = 0;
                }
            }

            // If lift-up animation finished, start flying
            if (currentServerAnimation == PlaneAnimation.LIFT_UP) {
                int liftUpDuration = ANIMATION_TIMINGS.getOrDefault(PlaneAnimation.LIFT_UP, 10);
                if (animationTick >= liftUpDuration) {
                    setFlying(true);
                    setNoGravity(true);
                    playServerAnimation(PlaneAnimation.FLYING);
                }
            }
        } else {
            // Already flying, handle in-air movement
            setNoGravity(true);
            playServerAnimation(PlaneAnimation.FLYING); // ensure looping
        }

    /* --------------------------------
       FLIGHT PHYSICS
    -------------------------------- */

        setYaw(player.getYaw());
        setPitch(player.getPitch());
        prevYaw = getYaw();

        float forward = player.forwardSpeed;
        float sideways = player.sidewaysSpeed;
        float speed = isFlying() ? 0.4f : 0.15f;

        Vec3d forwardDir = Vec3d.fromPolar(0, getYaw());
        Vec3d velocity = forwardDir.multiply(forward * speed);
        setYaw(getYaw() + sideways * 3.0f);

        if (isFlying() && player.isSneaking()) {
            velocity = velocity.add(0, -0.06, 0);
        }

        setVelocity(getVelocity().add(velocity).multiply(0.98));
        move(MovementType.SELF, getVelocity());

        wasFlyingLastTick = isFlying();
    }


    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        player.startRiding(this);
        return ActionResult.SUCCESS;
    }
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof PlayerEntity && this.getPassengerList().isEmpty();
    }
    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            passenger.setPosition(
                    this.getX(),
                    this.getY() + 0.8, // seat height
                    this.getZ() - 1.0
            );
        }
    }
    @Nullable
    public PlayerEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof PlayerEntity player ? player : null;
    }

    @Override
    public void updateTrackedPosition(double x, double y, double z) {
        super.updateTrackedPosition(x, y, z);
        this.prevYaw = this.getYaw();
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0);
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }
}
