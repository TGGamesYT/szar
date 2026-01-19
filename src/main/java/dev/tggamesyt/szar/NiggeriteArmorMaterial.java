package dev.tggamesyt.szar;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class NiggeriteArmorMaterial implements ArmorMaterial {

    public static final NiggeriteArmorMaterial INSTANCE = new NiggeriteArmorMaterial();

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};
    private static final int[] PROTECTION = {4, 8, 10, 4};

    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getEquipmentSlot().getEntitySlotId()] * 40;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION[type.getEquipmentSlot().getEntitySlotId()];
    }

    @Override
    public int getEnchantability() {
        return 18;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Szar.NIGGERITE_INGOT);
    }

    @Override
    public String getName() {
        return "mythril";
    }

    @Override
    public float getToughness() {
        return 3.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.15F;
    }
}
