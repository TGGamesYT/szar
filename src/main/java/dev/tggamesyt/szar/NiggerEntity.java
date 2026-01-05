package dev.tggamesyt.szar;

import dev.tggamesyt.szar.Szar;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class NiggerEntity extends PathAwareEntity {

    public NiggerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        this.dropItem(Szar.NWORD_PASS);
    }
}
