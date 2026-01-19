package dev.tggamesyt.szar;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class NiggeriteMaterial implements ToolMaterial {

    public static final NiggeriteMaterial INSTANCE = new NiggeriteMaterial();

    @Override
    public int getDurability() {
        return 2500; // Netherite is 2031
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 10.0F; // Netherite is 9
    }

    @Override
    public float getAttackDamage() {
        return 4.5F;
    }

    @Override
    public int getMiningLevel() {
        return 4; // Netherite level
    }

    @Override
    public int getEnchantability() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Szar.NIGGERITE_INGOT);
    }
}

