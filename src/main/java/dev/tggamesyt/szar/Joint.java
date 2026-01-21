package dev.tggamesyt.szar;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpyglassItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class Joint extends SpyglassItem {

    public Joint(Settings settings) {
        super(settings.maxDamage(20)); // max durability
    }
    private static final int COOLDOWN_TICKS = 20 * 5;
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPYGLASS; // keeps spyglass hold animation
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 40; // shorter “smoking” duration
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // play custom smoke sound
        user.playSound(SoundEvents.ITEM_HONEY_BOTTLE_DRINK, 1.0F, 1.0F);
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        user.setCurrentHand(hand); // start using
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        // Only do server/client side durability and effect
        //if (!world.isClient) return;

        // Consume 1 durability
        stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
        if (user instanceof PlayerEntity player && !world.isClient) {
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
        // Increase drug effect
        int amplifier = 0;
        if (user.hasStatusEffect(Szar.DROG_EFFECT)) {
            amplifier = Math.min(user.getStatusEffect(Szar.DROG_EFFECT).getAmplifier() + 1, 9); // max 10 levels
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.LUCK,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.MINING_FATIGUE,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HEALTH_BOOST,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SATURATION,
                    1200,
                    amplifier,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
        }
        if (amplifier > 3) {
            int nausealevel = amplifier - 3;
            user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA,
                    1200,
                    nausealevel,
                    false, // ambient
                    true, // show particles
                    true   // show icon
            ));
        }

        // Apply the effect (10 seconds, invisible particles)
        user.addStatusEffect(new StatusEffectInstance(
                Szar.DROG_EFFECT,
                1200,
                amplifier,
                false, // ambient
                true, // show particles
                true   // show icon
        ));

        // Optional: play inhale / stop sound
        user.playSound(SoundEvents.ITEM_HONEY_BOTTLE_DRINK, 1.0F, 1.0F);
    }

}
