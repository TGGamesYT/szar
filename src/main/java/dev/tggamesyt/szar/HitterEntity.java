package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class HitterEntity extends PathAwareEntity implements Arrestable{

    public static boolean arrestable = true;

    public HitterEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(3, new LookAroundGoal(this));

        this.targetSelector.add(1, new AggroOnHitRevengeGoal(this));
    }


    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "Hitler's will");
        nbt.putString("author", "Hitler");

        // Pages need to be JSON text components
        NbtList pages = new NbtList();
        pages.add(NbtString.of("{\"text\":\"Hitler's will\\n - Kill all jews\\n - Kill all players\"}"));
        pages.add(NbtString.of("{\"text\":\"die\"}"));

        nbt.put("pages", pages);

        this.dropStack(book);
    }



    @Override
    public boolean isArrestable() {
        return arrestable;
    }
}
