package dev.tggamesyt.szar.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.tggamesyt.szar.client.ThirdpersonModelRegisterer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;" +
                    "Lnet/minecraft/client/render/model/json/ModelTransformationMode;" +
                    "ZLnet/minecraft/client/util/math/MatrixStack;" +
                    "Lnet/minecraft/client/render/VertexConsumerProvider;" +
                    "IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private BakedModel swapThirdPersonModel(
            BakedModel originalModel,
            ItemStack stack,
            ModelTransformationMode renderMode
    ) {
        if (renderMode != ModelTransformationMode.GUI && renderMode != ModelTransformationMode.GROUND) {

            Identifier customId = ThirdpersonModelRegisterer.get(stack.getItem());
            if (customId != null) {
                ModelIdentifier modelId = new ModelIdentifier(customId, "inventory");
                ItemRenderer self = (ItemRenderer)(Object)this;
                return self.getModels().getModelManager().getModel(modelId);
            }
        }

        return originalModel;
    }
}