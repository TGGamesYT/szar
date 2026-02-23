package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.ClientCosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class TGcapeMixin {

    @Inject(method = "getCapeTexture", at = @At("HEAD"), cancellable = true)
    private void injectCapeTexture(CallbackInfoReturnable<Identifier> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)(Object) this;

        ClientCosmetics.CosmeticProfile profile =
                ClientCosmetics.get(player.getUuid());

        // Only override if we actually have a custom cape
        if (profile != null && profile.capeTexture != null) {
            cir.setReturnValue(profile.capeTexture);
        }
        // Otherwise vanilla continues → Mojang cape works normally
    }
}