package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.DrunkEffect;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.player.PlayerEntity.class)
public class PlayerDropMixin {
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD"))
    private void szar_trackDrop(ItemStack stack, boolean throwRandomly,
                                boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        net.minecraft.entity.player.PlayerEntity self =
                (net.minecraft.entity.player.PlayerEntity)(Object)this;
        if (self.getWorld().isClient) return;
        DrunkEffect.recentDropCount.merge(self.getUuid(), 1, Integer::sum);
    }
}
