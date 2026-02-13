package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class RadiatedItem extends Item {

    private final double radiationPerItem; // e.g. 0.1 per item
    private final double heldMultiplier;   // extra multiplier when selected

    public RadiatedItem(Settings settings, double radiationPerItem, double heldMultiplier) {
        super(settings);
        this.radiationPerItem = radiationPerItem;
        this.heldMultiplier = heldMultiplier;
    }

    public double getRadiationPerItem() {
        return radiationPerItem;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player) {

            double totalRadiation = 0.0;

            // Loop entire inventory
            for (ItemStack invStack : player.getInventory().main) {
                if (invStack.getItem() instanceof RadiatedItem radiatedItem) {

                    double perItem = radiatedItem.getRadiationPerItem();
                    double stackContribution = invStack.getCount() * perItem;

                    // If this stack is selected, increase its contribution
                    if (player.getInventory().getStack(slot) == invStack && selected) {
                        stackContribution *= radiatedItem.heldMultiplier;
                    }

                    totalRadiation += stackContribution;
                }
            }

            // Round final radiation to amplifier level
            int amplifier = (int) Math.round(totalRadiation);

            if (amplifier > 0) {
                player.addStatusEffect(new StatusEffectInstance(
                        Szar.RADIATION,
                        100,
                        amplifier - 1,
                        true,
                        false,
                        true
                ));
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }
}
