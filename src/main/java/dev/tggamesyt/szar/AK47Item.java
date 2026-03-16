package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class AK47Item extends Item {

    public AK47Item(Settings settings) {
        super(settings.maxDamage(512));
    }

    public boolean consumeAmmo(PlayerEntity player) {
        if (player.getAbilities().creativeMode) return true;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Szar.BULLET_ITEM)) {
                stack.decrement(1);
                ItemStack gun = player.getMainHandStack();
                if (gun.isOf(Szar.AK47)) {
                    gun.damage(1, player, p -> p.sendToolBreakStatus(p.getActiveHand()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW; // raises arm
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }
}
