package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import dev.tggamesyt.szar.RevolverItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void injectJointFirstPerson(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (item.getItem() instanceof Joint) {
            // only override position while actively using, otherwise let normal rendering handle equip/unequip
            if (!player.isUsingItem() || player.getActiveHand() != hand || player.getItemUseTimeLeft() <= 0) return;

            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRight = arm == Arm.RIGHT;

            matrices.push();
            // rotate 80 degrees toward player (around Y axis, so it faces them)
            matrices.translate(
                    0.0F,
                    -0.15F,  // was -0.35F, more negative = higher up
                    -0.5F
            );

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(95.0F));
            matrices.translate(0.0F, equipProgress * -0.6F, 0.0F);

            HeldItemRenderer self = (HeldItemRenderer) (Object) this;
            self.renderItem(
                    player,
                    item,
                    isRight ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                    !isRight,
                    matrices,
                    vertexConsumers,
                    light
            );

            matrices.pop();
            ci.cancel();
        }

        if (item.getItem() instanceof RevolverItem
                && player.isUsingItem()
                && player.getActiveHand() == hand) {

            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRight = arm == Arm.RIGHT;

            matrices.push();

            // Center in middle of screen regardless of hand
            matrices.translate(
                    isMainHand ? -0.18F : 0.18F,
                    -0.5F,
                    -0.5F
            );

            matrices.translate(0.0F, equipProgress * -0.6F, 0.0F);

            HeldItemRenderer self = (HeldItemRenderer) (Object) this;
            self.renderItem(player, item,
                    isRight ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                    !isRight, matrices, vertexConsumers, light);

            matrices.pop();
            ci.cancel();
        }
    }
}