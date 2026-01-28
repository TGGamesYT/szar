package dev.tggamesyt.szar;

import dev.tggamesyt.szar.AggroOnHitRevengeGoal;
import dev.tggamesyt.szar.PoliceArrestGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

public class PoliceEntity extends PathAwareEntity {

    public PoliceEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        // PoliceArrestGoal replaces normal melee behavior
        this.goalSelector.add(0, new PoliceArrestGoal(this));

        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(3, new LookAroundGoal(this));

        this.targetSelector.add(1, new AggroOnHitRevengeGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0); // half heart
    }
    public static boolean canSpawnHere(EntityType<PoliceEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
        // Only spawn near players
        return world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 48, false) != null
                && world.getLightLevel(pos) > 8; // optional, spawn in light
    }


    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        this.dropItem(Items.DEBUG_STICK);
    }
}
