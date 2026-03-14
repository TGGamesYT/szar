package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import dev.tggamesyt.szar.client.VideoHeadFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addVideoFeature(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {

        PlayerEntityRenderer renderer = (PlayerEntityRenderer)(Object)this;
        renderer.addFeature(new VideoHeadFeature(renderer));
    }

    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true)
    private static void injectJointArmPose(
            AbstractClientPlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<BipedEntityModel.ArmPose> cir
    ) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isEmpty()
                && player.getActiveHand() == hand
                && player.getItemUseTimeLeft() > 0
                && stack.getItem() instanceof Joint) {
            // SPYGLASS pose raises the arm, then BipedEntityModelMixin overrides the exact angles
            cir.setReturnValue(BipedEntityModel.ArmPose.SPYGLASS);
        }
    }
}