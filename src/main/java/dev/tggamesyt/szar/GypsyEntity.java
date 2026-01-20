package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class GypsyEntity extends PathAwareEntity {

    private final DefaultedList<ItemStack> stolenItems = DefaultedList.of();
    private int stealCooldown = 0;
    private boolean fleeing = false;

    private static final double FLEE_DISTANCE = 15.0;

    public GypsyEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    // ================= ATTRIBUTES =================

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    // ================= GOALS =================

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new FleeWhenSeenGoal(this));
        this.goalSelector.add(1, new SneakBehindPlayerGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    // ================= TICK =================

    @Override
    public void tick() {
        super.tick();

        if (stealCooldown > 0) {
            stealCooldown--;
        }
    }

    // ================= VISIBILITY CHECK =================

    /**
     * True if the entity is anywhere on the player's screen (FOV-based)
     */
    private boolean isOnPlayerScreen(PlayerEntity player) {
        if (player.isCreative()) return false;

        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEntity = this.getPos().subtract(player.getEyePos()).normalize();

        // Rough FOV check (~120° total)
        return look.dotProduct(toEntity) > 0.3;
    }

    // ================= STEALING =================

    private void trySteal(PlayerEntity player) {
        if (stealCooldown > 0 || player.isCreative() || this.getWorld().isClient) return;

        List<ItemStack> nonEmpty = player.getInventory().main.stream()
                .filter(stack -> !stack.isEmpty())
                .toList();

        if (nonEmpty.isEmpty()) return;

        ItemStack chosen = nonEmpty.get(this.random.nextInt(nonEmpty.size()));
        ItemStack stolen = chosen.split(1);

        stolenItems.add(stolen);
        stealCooldown = 20 * 20; // 20 seconds
        fleeing = true;

        this.getNavigation().stop();
    }

    // ================= DAMAGE & LOOT =================

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);

        if (!this.getWorld().isClient && !stolenItems.isEmpty()) {
            this.dropStack(stolenItems.remove(0));
        }

        return result;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        for (ItemStack stack : stolenItems) {
            this.dropStack(stack);
        }
        stolenItems.clear();
    }

    // ================= GOALS =================

    /**
     * Runs away when visible OR after stealing,
     * stops once far enough away.
     */
    private static class FleeWhenSeenGoal extends Goal {

        private final GypsyEntity GypsyEntity;
        private PlayerEntity target;

        public FleeWhenSeenGoal(GypsyEntity GypsyEntity) {
            this.GypsyEntity = GypsyEntity;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            this.target = GypsyEntity.getWorld().getClosestPlayer(GypsyEntity, 20);

            if (target == null || target.isCreative()) return false;

            if (GypsyEntity.fleeing) return true;

            return GypsyEntity.isOnPlayerScreen(target);
        }

        @Override
        public boolean shouldContinue() {
            return GypsyEntity.fleeing
                    && target != null
                    && GypsyEntity.squaredDistanceTo(target) < FLEE_DISTANCE * FLEE_DISTANCE;
        }

        @Override
        public void start() {
            GypsyEntity.fleeing = true;
            moveAway();
        }

        @Override
        public void tick() {
            moveAway();

            if (GypsyEntity.squaredDistanceTo(target) >= FLEE_DISTANCE * FLEE_DISTANCE) {
                GypsyEntity.fleeing = false;
            }
        }

        private void moveAway() {
            Vec3d away = GypsyEntity.getPos()
                    .subtract(target.getPos())
                    .normalize()
                    .multiply(10);

            Vec3d dest = GypsyEntity.getPos().add(away);
            GypsyEntity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.35);
        }
    }

    /**
     * Sneaks behind players ONLY when unseen and not fleeing
     */
    private static class SneakBehindPlayerGoal extends Goal {

        private final GypsyEntity GypsyEntity;
        private PlayerEntity target;

        public SneakBehindPlayerGoal(GypsyEntity GypsyEntity) {
            this.GypsyEntity = GypsyEntity;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            this.target = GypsyEntity.getWorld().getClosestPlayer(GypsyEntity, 12);

            return target != null
                    && !target.isCreative()
                    && !GypsyEntity.isOnPlayerScreen(target)
                    && GypsyEntity.stealCooldown == 0
                    && !GypsyEntity.fleeing;
        }

        @Override
        public void tick() {
            Vec3d behind = target.getPos()
                    .subtract(target.getRotationVec(1.0F).normalize().multiply(2));

            GypsyEntity.getNavigation().startMovingTo(
                    behind.x, behind.y, behind.z, 1.0
            );

            if (GypsyEntity.distanceTo(target) < 1.5) {
                GypsyEntity.trySteal(target);
            }
        }
    }
}
