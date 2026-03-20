package dev.tggamesyt.szar;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SmilerEntity extends PathAwareEntity {

    public SmilerType smilerType = SmilerType.EYES;

    // Targeting
    @Nullable private UUID targetPlayerUUID = null;
    @Nullable private PlayerEntity cachedTarget = null;

    // State tracking
    private int stateTimer = 0;
    private boolean hasActed = false; // has this entity done its main action?

    // SCARY specific
    private boolean scaryTeleported = false;
    private boolean playerWasLooking = false;

    public SmilerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setInvisible(false);
        this.setNoGravity(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void initGoals() {
        // No standard goals — all behavior handled in tick
    }

    public void setTargetPlayer(PlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) return;

        this.targetPlayerUUID = player.getUuid();
        this.cachedTarget = player;
    }

    @Nullable
    public PlayerEntity getTargetPlayer() {
        if (cachedTarget != null && cachedTarget.isAlive()) return cachedTarget;
        if (targetPlayerUUID != null && getWorld() instanceof ServerWorld sw) {
            cachedTarget = sw.getPlayerByUuid(targetPlayerUUID);
            return cachedTarget;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        PlayerEntity player = getTargetPlayer();

        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            this.discard();
            return;
        }

        // Despawn if blackout ended
        if (BackroomsLightManager.currentEvent != BackroomsLightManager.GlobalEvent.BLACKOUT) {
            this.discard();
            return;
        }

        stateTimer++;

        switch (smilerType) {
            case EYES -> tickEyes(player);
            case SCARY -> tickScary(player);
            case REAL -> tickReal(player);
        }
    }

    private void tickEyes(PlayerEntity player) {
        if (hasActed) return;

        // Slowly approach player
        Vec3d toPlayer = player.getPos().subtract(this.getPos()).normalize();
        this.setVelocity(toPlayer.multiply(0.05));
        this.velocityModified = true;

        // Look at player
        this.getLookControl().lookAt(player, 30f, 30f);

        // When close enough, flashbang
        double dist = this.squaredDistanceTo(player);
        if (dist < 4.0) {
            hasActed = true;
            if (player instanceof ServerPlayerEntity sp) {
                SmilerEffectManager.triggerFlashbang(sp);
            }
            // Notify spawn manager
            SmilerSpawnManager.onSmilerActed(player.getUuid(), SmilerType.EYES);
            this.discard();
        }
    }

    private void tickScary(PlayerEntity player) {
        if (hasActed) return;

        boolean playerLooking = isPlayerLookingAt(player);

        if (!scaryTeleported) {
            // Stay still while player is looking
            if (playerLooking) {
                playerWasLooking = true;
                // Don't move
                this.setVelocity(Vec3d.ZERO);
            } else if (playerWasLooking) {
                // Player looked away — teleport close
                Vec3d behindPlayer = player.getPos()
                        .add(player.getRotationVector().multiply(-1.5));
                this.teleport(behindPlayer.x, behindPlayer.y, behindPlayer.z);
                scaryTeleported = true;
            }
        } else {
            // Teleported — wait for player to look at us again
            if (playerLooking) {
                hasActed = true;
                if (player instanceof ServerPlayerEntity sp) {
                    SmilerEffectManager.triggerJumpscare(sp, SmilerType.SCARY);
                    float damage = 2.0f + getWorld().random.nextFloat() * 4.0f; // 2-6 hearts = 4-12 hp
                    player.damage(getWorld().getDamageSources().mobAttack(this), damage * 2);
                }
                SmilerSpawnManager.onSmilerActed(player.getUuid(), SmilerType.SCARY);
                this.discard();
            }
        }
    }

    private void tickReal(PlayerEntity player) {
        if (hasActed) return;

        // Run directly at player
        Vec3d toPlayer = player.getPos().subtract(this.getPos()).normalize();
        this.setVelocity(toPlayer.multiply(0.4));
        this.velocityModified = true;
        this.getLookControl().lookAt(player, 30f, 30f);

        double dist = this.squaredDistanceTo(player);
        if (dist < 4.0) {
            hasActed = true;
            if (player instanceof ServerPlayerEntity sp) {
                SmilerEffectManager.triggerJumpscare(sp, SmilerType.REAL);
                float damage = 4.0f + getWorld().random.nextFloat() * 4.0f; // 4-8 hearts = 8-16 hp
                player.damage(getWorld().getDamageSources().mobAttack(this), damage * 2);
            }
            SmilerSpawnManager.onSmilerActed(player.getUuid(), SmilerType.REAL);
            this.discard();
        }
    }

    private boolean isPlayerLookingAt(PlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVector();
        Vec3d toEntity = this.getPos().subtract(eyePos).normalize();
        double dot = lookVec.dotProduct(toEntity);
        return dot > 0.97; // ~14 degree cone
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SmilerType", smilerType.name());
        if (targetPlayerUUID != null) {
            nbt.putUuid("TargetPlayer", targetPlayerUUID);
        }
        nbt.putBoolean("HasActed", hasActed);
        nbt.putBoolean("ScaryTeleported", scaryTeleported);
        nbt.putBoolean("PlayerWasLooking", playerWasLooking);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SmilerType")) {
            smilerType = SmilerType.valueOf(nbt.getString("SmilerType"));
        }
        if (nbt.containsUuid("TargetPlayer")) {
            targetPlayerUUID = nbt.getUuid("TargetPlayer");
        }
        hasActed = nbt.getBoolean("HasActed");
        scaryTeleported = nbt.getBoolean("ScaryTeleported");
        playerWasLooking = nbt.getBoolean("PlayerWasLooking");
    }

    @Override
    public boolean isInvisible() { return false; }

    @Override
    public boolean canTarget(EntityType<?> type) { return false; }

    @Override
    protected boolean canStartRiding(Entity entity) { return false; }

    @Override
    public boolean isPushable() { return false; }
}