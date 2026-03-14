package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import dev.tggamesyt.szar.RevolverItem;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {

    @Final
    @Shadow public ModelPart rightArm;
    @Final
    @Shadow public ModelPart leftArm;
    @Final
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
        boolean mainHand = entity.getActiveHand() == Hand.MAIN_HAND;
        boolean rightHanded = entity.getMainArm() == Arm.RIGHT;
        boolean useRight = (mainHand && rightHanded) || (!mainHand && !rightHanded);
        if (entity.getActiveItem().getItem() instanceof Joint) {
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
        if (entity.getActiveItem().getItem() instanceof RevolverItem && entity.isSneaking()) {
            if (useRight) {
                float extraOut = MathHelper.clamp(-this.head.yaw, 0F, 1F) * 0.4F;
                float pitchDrop = MathHelper.clamp(-(this.head.pitch + 1.0F), 0F, 1F) * 0.4F;
                this.rightArm.pitch = -(float)Math.PI / 2F + pitchDrop;
                this.rightArm.yaw = this.head.yaw * (this.head.yaw > 0 ? 1.4F : 2.0F) + (float)Math.PI / 2F + extraOut;
                this.rightArm.roll = 0F;
                // Reset left arm to idle (undo vanilla two-hand pose)
                this.leftArm.pitch = 0F;
                this.leftArm.yaw = 0F;
                this.leftArm.roll = 0F;
            } else {
                float extraOut = MathHelper.clamp(this.head.yaw, 0F, 1F) * 0.4F;
                float pitchDrop = MathHelper.clamp(-(this.head.pitch + 1.0F), 0F, 1F) * 0.4F;
                this.leftArm.pitch = -(float)Math.PI / 2F + pitchDrop;
                this.leftArm.yaw = this.head.yaw * (this.head.yaw < 0 ? 1.4F : 2.0F) - (float)Math.PI / 2F - extraOut;
                this.leftArm.roll = 0F;
                // Reset right arm to idle (undo vanilla two-hand pose)
                this.rightArm.pitch = 0F;
                this.rightArm.yaw = 0F;
                this.rightArm.roll = 0F;
            }
        }
    }
}