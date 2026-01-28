package dev.tggamesyt.szar;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
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

import java.util.*;

public class GypsyEntity extends PathAwareEntity {

    public static boolean arrestable = false;

    private final DefaultedList<ItemStack> stolenItems = DefaultedList.of();
    private final Set<UUID> stolenFromPlayers = new HashSet<>();

    private int stealCooldown = 0;
    private int panicTicks = 0;
    private UUID fleeingFrom = null;
    private int heldItemSwapTicks = 0;

    public GypsyEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(true);
    }

    // ================= ATTRIBUTES =================
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0);
    }

    // ================= GOALS =================
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new PanicRandomlyGoal(this));
        this.goalSelector.add(1, new FleeSpecificPlayerGoal(this));
        this.goalSelector.add(2, new DefensiveAttackGoal(this));
        this.goalSelector.add(3, new SneakBehindPlayerGoal(this));
        this.goalSelector.add(4, new BiasedWanderGoal(this, 0.6));
        this.goalSelector.add(5, new LookAroundGoal(this));
    }

    // ================= TICK =================
    @Override
    public void tick() {
        super.tick();

        if (stealCooldown > 0) stealCooldown--;

        if (!this.getWorld().isClient && !stolenItems.isEmpty()) {
            heldItemSwapTicks++;
            if (heldItemSwapTicks > 60) {
                heldItemSwapTicks = 0;
                stolenItems.add(stolenItems.remove(0));
            }
            equipStack(EquipmentSlot.MAINHAND, stolenItems.get(0));
        }
    }

    // ================= VISIBILITY =================
    private boolean isOnPlayerScreen(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d toEntity = this.getPos().subtract(player.getEyePos()).normalize();
        return look.dotProduct(toEntity) > 0.55;
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
        equipStack(EquipmentSlot.MAINHAND, stolen);

        stolenFromPlayers.add(player.getUuid());
        fleeingFrom = player.getUuid();

        stealCooldown = 20 * 20;
        this.getNavigation().stop();
    }

    // ================= ITEM PICKUP (WORLD ITEMS, NOT CRIME) =================
    @Override
    protected void loot(ItemEntity item) {
        if (this.getWorld().isClient) return;
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return;

        ItemStack taken = stack.split(1);
        stolenItems.add(taken);
        equipStack(EquipmentSlot.MAINHAND, taken);

        if (stack.isEmpty()) item.discard();
    }

    // ================= DAMAGE =================
    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);

        if (!this.getWorld().isClient) {
            panicTicks = 60 + random.nextInt(40);
            if (!stolenItems.isEmpty()) this.dropStack(stolenItems.remove(0));
        }

        return result;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        for (ItemStack stack : stolenItems) this.dropStack(stack);
        stolenItems.clear();
    }

    // ================= GOALS =================

    private static class PanicRandomlyGoal extends Goal {
        private final GypsyEntity mob;
        PanicRandomlyGoal(GypsyEntity mob) { this.mob = mob; this.setControls(EnumSet.of(Control.MOVE)); }
        @Override public boolean canStart() { return mob.panicTicks > 0; }
        @Override
        public void tick() {
            mob.panicTicks--;
            if (mob.getNavigation().isIdle()) {
                Vec3d dest = mob.getPos().add(
                        mob.random.nextGaussian() * 8,
                        0,
                        mob.random.nextGaussian() * 8
                );
                mob.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.5);
            }
        }
    }

    // 🔴 Flee from only specific victim, hide behind others
    private static class FleeSpecificPlayerGoal extends Goal {
        private final GypsyEntity mob;
        private PlayerEntity threat;

        FleeSpecificPlayerGoal(GypsyEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (mob.fleeingFrom == null) return false;
            PlayerEntity p = mob.getWorld().getPlayerByUuid(mob.fleeingFrom);
            if (p == null || !mob.canSee(p) || !mob.isOnPlayerScreen(p)) return false;
            threat = p;
            return true;
        }

        @Override
        public void tick() {
            TargetPredicate predicate = TargetPredicate.createNonAttackable()
                    .setBaseMaxDistance(16)
                    .setPredicate(player -> !player.getUuid().equals(mob.fleeingFrom));

            PlayerEntity shield = mob.getWorld().getClosestPlayer(predicate, mob);

            Vec3d dest;
            if (shield != null && !shield.isCreative()) {
                dest = shield.getPos(); // hide behind other players
            } else {
                dest = mob.getPos().subtract(threat.getPos()).normalize().multiply(10).add(mob.getPos());
            }

            mob.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.3);
        }
    }

    // ⚔ Attack only the player it stole from
    private static class DefensiveAttackGoal extends Goal {
        private final GypsyEntity mob;
        private PlayerEntity target;
        private int cooldown = 0;

        DefensiveAttackGoal(GypsyEntity mob) { this.mob = mob; }

        @Override
        public boolean canStart() {
            if (mob.fleeingFrom == null) return false;
            PlayerEntity p = mob.getWorld().getPlayerByUuid(mob.fleeingFrom);
            if (p == null || mob.distanceTo(p) > 1.3) return false;
            target = p;
            return true;
        }

        @Override
        public void tick() {
            if (cooldown-- > 0) return;
            cooldown = 20;
            mob.getLookControl().lookAt(target);
            mob.tryAttack(target);
        }
    }

    // 🟡 Sneak steal
    private static class SneakBehindPlayerGoal extends Goal {
        private final GypsyEntity mob;
        private PlayerEntity target;
        private int cooldown = 0;

        SneakBehindPlayerGoal(GypsyEntity mob) {
            this.mob = mob;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            target = mob.getWorld().getClosestPlayer(mob, 10);
            return target != null
                    && !mob.stolenFromPlayers.contains(target.getUuid())
                    && mob.stealCooldown == 0
                    && !mob.isOnPlayerScreen(target)
                    && !target.isCreative();
        }

        @Override
        public void tick() {
            if (cooldown-- > 0) return;
            cooldown = 5;

            Vec3d behind = target.getPos().subtract(target.getRotationVec(1.0F).normalize());
            mob.getNavigation().startMovingTo(behind.x, behind.y, behind.z, 1.15);

            if (mob.distanceTo(target) < 1.3) {
                mob.trySteal(target);
                arrestable = true;
            }
        }
    }

    // 🟢 Biased wander toward players when not guilty
    private static class BiasedWanderGoal extends WanderAroundFarGoal {
        private final GypsyEntity mob;

        BiasedWanderGoal(GypsyEntity mob, double speed) {
            super(mob, speed);
            this.mob = mob;
        }

        @Override
        protected Vec3d getWanderTarget() {
            Vec3d base = super.getWanderTarget();
            PlayerEntity player = mob.getWorld().getClosestPlayer(mob, 10);

            if (player == null || base == null || mob.fleeingFrom != null) return base;

            Vec3d best = base;
            double bestDist = base.squaredDistanceTo(player.getPos());

            for (int i = 0; i < 4; i++) {
                Vec3d c = super.getWanderTarget();
                if (c == null) continue;
                double d = c.squaredDistanceTo(player.getPos());
                if (d < bestDist) { // bias toward player
                    best = c;
                    bestDist = d;
                }
            }
            return best;
        }
    }
}
