package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class NyanEntity extends PathAwareEntity {

    public NyanEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new WanderAroundFarGoal(this, 10.0D));
        this.goalSelector.add(0, new LookAroundGoal(this));
    }


    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
    }
    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        // Only above ground
        boolean aboveGround = this.getY() >= world.getBottomY() + 1 && this.getY() <= world.getTopY();

        // Only allow 5% of spawn attempts to succeed
        boolean rareChance = this.random.nextFloat() < 0.05f;

        // Standard mob spawn rules + above ground + rare chance
        return super.canSpawn(world, spawnReason) && aboveGround && rareChance;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);
        if (result) {
            // Trigger panic
            this.setPanic(200); // panic for 100 ticks (5 seconds)
        }
        return result;
    }

    private int panicTicks = 0;

    private void setPanic(int ticks) {
        this.panicTicks = ticks;
    }

    @Override
    public void tick() {
        super.tick();

        if (panicTicks > 0) {
            panicTicks--;

            // Move in a random direction away from attacker
            double speed = 1.5D;
            double dx = (this.random.nextDouble() - 0.5) * 2;
            double dz = (this.random.nextDouble() - 0.5) * 2;

            this.getNavigation().startMovingTo(this.getX() + dx * 5, this.getY(), this.getZ() + dz * 5, speed);
        }
    }


    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        this.dropItem(Szar.POPTART);
    }
}
