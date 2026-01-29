package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class HandcuffItem extends Item {

    public HandcuffItem(Item.Settings settings) {
        super(settings);
    }

    /**
     * Called when the player right-clicks an entity with this item.
     */
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        entity.addStatusEffect(new StatusEffectInstance(Szar.ARRESTED, 2400, 1));
        stack.decrement(1);
        return ActionResult.SUCCESS;
    }
}
