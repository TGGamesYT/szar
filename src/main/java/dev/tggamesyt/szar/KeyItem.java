package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class KeyItem extends Item {

    public KeyItem(Settings settings) {
        super(settings.maxDamage(3)); // max durability 3
    }

    // Right-click on entity → lose 1 durability
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!player.getWorld().isClient) {
            if (target.hasStatusEffect(Szar.ARRESTED)) {
                target.removeStatusEffect(Szar.ARRESTED);

                // Play sound to indicate effect removed
                player.getWorld().playSound(null, target.getBlockPos(),
                        SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Use 1 durability
                stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
            }
        }
        return ActionResult.SUCCESS;
    }

    // Hold right-click to use on self → lose 3 durability
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (user.hasStatusEffect(Szar.ARRESTED)) {
                user.removeStatusEffect(Szar.ARRESTED);

                world.playSound(null, user.getBlockPos(),
                        SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Use 3 durability
                stack.damage(3, user, p -> p.sendToolBreakStatus(hand));
            }
        }

        // Start hold animation
        return TypedActionResult.consume(stack);
    }

    // Make it holdable like a bow
    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    // Continuous usage tick
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            if (player.hasStatusEffect(Szar.ARRESTED)) {
                player.removeStatusEffect(Szar.ARRESTED);
                world.playSound(null, player.getBlockPos(),
                        SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

                // Remove full 3 durability if held
                stack.damage(3, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
            }
        }
    }
}
