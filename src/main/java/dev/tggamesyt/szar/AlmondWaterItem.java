package dev.tggamesyt.szar;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AlmondWaterItem extends Item {

    public AlmondWaterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        PlayerEntity player = user instanceof PlayerEntity ? (PlayerEntity) user : null;

        // Advancement trigger
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!world.isClient) {
            // Collect harmful effects first
            List<StatusEffectInstance> toRemove = new ArrayList<>();

            for (StatusEffectInstance effect : user.getStatusEffects()) {
                if (effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                    toRemove.add(effect);
                }
            }

            // Then remove them
            for (StatusEffectInstance effect : toRemove) {
                user.removeStatusEffect(effect.getEffectType());
            }

            // Hunger + saturation
            if (player != null) {
                player.getHungerManager().add(4, 0.6F);
            }
        }

        // Stats + stack handling
        if (player != null) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        // Return glass bottle
        if (player == null || !player.getAbilities().creativeMode) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (player != null) {
                player.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        user.emitGameEvent(net.minecraft.world.event.GameEvent.DRINK);
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public SoundEvent getDrinkSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatSound() {
        return SoundEvents.ENTITY_GENERIC_DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
}