package dev.tggamesyt.szar;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class NaziEntity extends PathAwareEntity implements Arrestable{

    public static boolean arrestable = false;
    @Nullable
    private HitterEntity leader;
    public NaziEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Szar.AK47));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new FollowLeaderWanderGoal(this, 1.0D, 6.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(1, new AK47AttackGoal(this, 16.0F, 2));
    }


    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        var rand = this.getRandom();
        if (rand.nextFloat() < 0.01F) {
            this.dropItem(Szar.AK47);
        }
        if (rand.nextFloat() < 0.01F) {
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

            NbtCompound nbt = book.getOrCreateNbt();
            nbt.putString("title", "Nazi's message");
            nbt.putString("author", "Nazi");

            // Pages need to be JSON text components
            NbtList pages = new NbtList();
            pages.add(NbtString.of("{\"text\":\"Hail Hitler\"}"));

            nbt.put("pages", pages);

            this.dropStack(book);
        }

        int count = rand.nextInt(17);
        if (count > 0) {
            this.dropStack(new ItemStack(Szar.AK_AMMO, count));
        }
    }

    @Override
    public boolean isArrestable() {
        return arrestable;
    }

    public void setLeader(HitterEntity leader) {
        this.leader = leader;
    }

    @Nullable
    public HitterEntity getLeader() {
        return this.leader;
    }

}
