package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {

    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart head;

    @Inject(
            method = "setAngles",
            at = @At(
                    value = "INVOKE",
                    // hat.copyTransform(head) is the absolute last call in setAngles
                    target = "Lnet/minecraft/client/model/ModelPart;copyTransform(Lnet/minecraft/client/model/ModelPart;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectJointPose(T entity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (!entity.isUsingItem()) return;
        if (!(entity.getActiveItem().getItem() instanceof Joint)) return;

        boolean mainHand = entity.getActiveHand() == Hand.MAIN_HAND;
        boolean rightHanded = entity.getMainArm() == Arm.RIGHT;
        boolean useRight = (mainHand && rightHanded) || (!mainHand && !rightHanded);

        if (useRight) {
            this.rightArm.pitch = MathHelper.clamp(
                    this.head.pitch - 1.7F - (entity.isInSneakingPose() ? 0.2617994F : 0.0F),
                    -2.4F, 3.3F
            );
            this.rightArm.yaw = this.head.yaw - 0.4F;
        } else {
            this.leftArm.pitch = MathHelper.clamp(
                    this.head.pitch - 1.7F - (entity.isInSneakingPose() ? 0.2617994F : 0.0F),
                    -2.4F, 3.3F
            );
            this.leftArm.yaw = this.head.yaw + 0.4F;
        }
    }
}