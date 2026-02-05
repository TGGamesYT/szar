package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class AK47Item extends Item {

    public AK47Item(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof PlayerEntity player)) return;
        if (!selected) return;
        if (!player.isUsingItem()) return;
        if (world.isClient) return;

        if (!consumeAmmo(player)) return;

        BulletEntity bullet = new BulletEntity(world, player);
        bullet.setVelocity(
                player,
                player.getPitch(),
                player.getYaw(),
                0.0F,
                4.5F, // speed
                1.0F  // spread
        );

        world.spawnEntity(bullet);
        player.getItemCooldownManager().set(this, 2); // fire rate
    }

    private boolean consumeAmmo(PlayerEntity player) {
        if (player.getAbilities().creativeMode) return true;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Szar.AK_AMMO)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
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
