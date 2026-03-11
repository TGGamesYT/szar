package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.ResourcePackHelper;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourcePackOrganizer.Pack.class)
public interface PackMixin {

    @Shadow
    String getName();

    @Inject(method = "canBeEnabled()Z", at = @At("RETURN"), cancellable = true)
    private void onCanBeEnabled(CallbackInfoReturnable<Boolean> cir) {
        if (ResourcePackHelper.isManaged(this.getName())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeDisabled()Z", at = @At("RETURN"), cancellable = true)
    private void onCanBeDisabled(CallbackInfoReturnable<Boolean> cir) {
        if (ResourcePackHelper.isManaged(this.getName())) {
            cir.setReturnValue(false);
        }
    }
}