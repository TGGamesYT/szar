package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import dev.tggamesyt.szar.RevolverItem;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public abstract class PlayerHeldItemFeatureRendererMixin<T extends net.minecraft.entity.player.PlayerEntity, M extends net.minecraft.client.render.entity.model.EntityModel<T> & net.minecraft.client.render.entity.model.ModelWithArms & ModelWithHead> extends net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer<T, M> {

    @Shadow @Final private HeldItemRenderer playerHeldItemRenderer;

    public PlayerHeldItemFeatureRendererMixin(net.minecraft.client.render.entity.feature.FeatureRendererContext<T, M> context, HeldItemRenderer heldItemRenderer) {
        super(context, heldItemRenderer);
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void injectJointRender(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode transformationMode,
            Arm arm,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (stack.getItem() instanceof Joint
                && entity.getActiveItem() == stack
                && entity.handSwingTicks == 0) {

            matrices.push();

            ModelPart head = ((ModelWithHead) this.getContextModel()).getHead();
            float savedPitch = head.pitch;

            // clamp head pitch so the joint doesn't clip into the head when looking up/down
            head.pitch = MathHelper.clamp(head.pitch, -(float)(Math.PI / 6F), (float)(Math.PI / 2F));
            head.rotate(matrices);
            head.pitch = savedPitch;

            HeadFeatureRenderer.translate(matrices, false);

            boolean isLeft = arm == Arm.LEFT;

            matrices.translate(
                    0F,
                    -0.3F,
                    0.1F
            );

            this.playerHeldItemRenderer.renderItem(entity, stack, ModelTransformationMode.HEAD, false, matrices, vertexConsumers, light);

            matrices.pop();
            ci.cancel();
        }
        if (stack.getItem() instanceof RevolverItem
                && entity.getActiveItem() == stack
                && entity.handSwingTicks == 0 && entity.isSneaking()) {

            matrices.push();

            ModelPart head = (this.getContextModel()).getHead();
            head.rotate(matrices);

            HeadFeatureRenderer.translate(matrices, false);

            boolean isLeft = arm == Arm.LEFT;

            matrices.translate(
                    isLeft ? -1F : 1F,
                    -0.4F,
                    0F
            );
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft ? -90 : 90));
            matrices.scale(0.6F, 0.6F, 0.6F);
            this.playerHeldItemRenderer.renderItem(entity, stack, ModelTransformationMode.HEAD, false, matrices, vertexConsumers, light);

            matrices.pop();
            ci.cancel();
        }
    }
}