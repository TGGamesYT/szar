package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.ClientCosmetics;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class TGnameMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void overrideName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity)(Object)this;

        Text custom = ClientCosmetics.buildName(
                self.getUuid(),
                self.getGameProfile().getName()
        );

        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}