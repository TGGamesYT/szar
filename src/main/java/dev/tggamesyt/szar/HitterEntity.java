package dev.tggamesyt.szar;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.tggamesyt.szar.Szar.NaziEntityType;

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
        nbt.putString("title", "MeinCrampft");
        nbt.putString("author", "Hitler");

        // Pages need to be JSON text components
        NbtList pages = new NbtList();
        pages.add(NbtString.of("{\"text\":\"MeinCrampft\\n - Kill all jews\\n - Kill all players\"}"));
        pages.add(NbtString.of("{\"text\":\"die\"}"));

        nbt.put("pages", pages);

        this.dropStack(book);
    }



    @Override
    public boolean isArrestable() {
        return arrestable;
    }
    @Override
    @Nullable
    public EntityData initialize(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            @Nullable EntityData entityData,
            @Nullable NbtCompound entityNbt
    ) {
        // Always call super
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);

        Random random = world.getRandom();

        this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE)
                .addPersistentModifier(
                        new EntityAttributeModifier(
                                "Random spawn bonus",
                                random.nextTriangular(0.0D, 0.11485D),
                                EntityAttributeModifier.Operation.MULTIPLY_BASE
                        )
                );

        this.setLeftHanded(random.nextFloat() < 0.05F);

        // 🔥 SPAWN GROUP HERE
        if (spawnReason == SpawnReason.NATURAL && world instanceof ServerWorld serverWorld) {

            int groupSize = 4 + serverWorld.random.nextInt(7); // 4–10 Bs

            for (int i = 0; i < groupSize; i++) {
                Entity entityB = NaziEntityType.create(serverWorld);
                if (entityB != null) {
                    double offsetX = (serverWorld.random.nextDouble() - 0.5) * 6;
                    double offsetZ = (serverWorld.random.nextDouble() - 0.5) * 6;

                    entityB.refreshPositionAndAngles(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ,
                            serverWorld.random.nextFloat() * 360F,
                            0F
                    );

                    serverWorld.spawnEntity(entityB);
                    if (entityB instanceof NaziEntity nazi) {
                        nazi.setLeader(this);
                    }
                }
            }
        }

        return data;
    }

    @Override
    public void setAttacker(@Nullable LivingEntity attacker) {
        super.setAttacker(attacker);

        if (attacker == null || this.getWorld().isClient) return;

        List<NaziEntity> allies = this.getWorld().getEntitiesByClass(
                NaziEntity.class,
                this.getBoundingBox().expand(16),
                nazi -> nazi.getLeader() == this && nazi.isAlive()
        );

        for (NaziEntity nazi : allies) {
            nazi.setTarget(attacker);
        }
    }


}
