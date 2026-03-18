package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CanOfBeansItem extends Item {

    public CanOfBeansItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(6));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.success(stack);

        // Give one bean
        ItemStack bean = new ItemStack(Szar.BEAN);
        if (!player.getInventory().insertStack(bean)) {
            player.dropItem(bean, false);
        }

        // Play a little sound
        world.playSound(null, player.getBlockPos(),
                SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Damage the item by 1 — if it breaks, replace with empty hand
        stack.damage(1, player, p -> p.sendToolBreakStatus(hand));

        return TypedActionResult.success(stack);
    }
}