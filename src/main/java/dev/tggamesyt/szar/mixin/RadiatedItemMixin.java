package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.tggamesyt.szar.Szar.RADIATION_DAMAGE;
import static dev.tggamesyt.szar.Szar.SERVER;

@Mixin(Item.class)
public abstract class RadiatedItemMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = cir.getReturnValue().getValue();
        if (!world.isClient && stack.hasNbt() && stack.getNbt().getBoolean("Radiated")) {
            RegistryEntry<DamageType> radiationEntry = Szar.SERVER.getRegistryManager()
                    .get(RegistryKeys.DAMAGE_TYPE)
                    .getEntry(Szar.RADIATION_DAMAGE)
                    .orElseThrow();
            DamageSource radiation = new DamageSource(radiationEntry);
            user.damage(radiation, Float.MAX_VALUE);
        }
    }
}
