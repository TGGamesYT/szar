package dev.tggamesyt.szar.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class RadiatedItemRendererMixin {

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("TAIL")
    )
    private void renderRadiationPixel(ItemStack stack,
                                      ModelTransformationMode mode,
                                      boolean leftHanded,
                                      MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers,
                                      int light,
                                      int overlay,
                                      BakedModel model,
                                      CallbackInfo ci) {

        if (!stack.hasNbt() || !stack.getNbt().getBoolean("Radiated")) return;

        int x = Math.min(stack.getNbt().getInt("RadPixelX"), 15) / 2;
        int y = Math.min(stack.getNbt().getInt("RadPixelY"), 15) / 2;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getSolid()); // no atlas

        float px = x / 16f;
        float py = y / 16f;
        float size = 1f / 16f;
        float z = 0.01f;

        // draw 1px green quad on top of item
        vc.vertex(matrix, px, py, z).color(0f,1f,0f,1f).texture(0f,0f).overlay(overlay).light(light).normal(0f,0f,1f).next();
        vc.vertex(matrix, px + size, py, z).color(0f,1f,0f,1f).texture(0f,0f).overlay(overlay).light(light).normal(0f,0f,1f).next();
        vc.vertex(matrix, px + size, py + size, z).color(0f,1f,0f,1f).texture(0f,0f).overlay(overlay).light(light).normal(0f,0f,1f).next();
        vc.vertex(matrix, px, py + size, z).color(0f,1f,0f,1f).texture(0f,0f).overlay(overlay).light(light).normal(0f,0f,1f).next();

        matrices.pop();
    }
}
