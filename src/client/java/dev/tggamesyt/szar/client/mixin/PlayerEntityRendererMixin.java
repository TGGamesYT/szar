package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.VideoHeadFeature;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addVideoFeature(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {

        PlayerEntityRenderer renderer = (PlayerEntityRenderer)(Object)this;

        renderer.addFeature(new VideoHeadFeature(renderer));
    }
}