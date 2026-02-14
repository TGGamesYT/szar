package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class MerlEntity extends PathAwareEntity {

    public MerlEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        // Panic when recently damaged
        this.goalSelector.add(0, new EscapeDangerGoal(this, 1.4D));

        // Wander normally
        this.goalSelector.add(1, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(2, new LookAroundGoal(this));
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
        nbt.putString("title", "My answer");
        nbt.putString("author", "Merl");

        // Pages need to be JSON text components
        NbtList pages = new NbtList();
        pages.add(NbtString.of("{\"text\":\"I don't know.\"}"));
        pages.add(NbtString.of("{\"text\":\"-Merl\"}"));

        nbt.put("pages", pages);

        this.dropStack(book);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(this.getId());

            ServerPlayNetworking.send(serverPlayer, Szar.OPEN_MERL_SCREEN, buf);
        }

        return ActionResult.SUCCESS;
    }
}
