package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ItemRenderer.class)
public abstract class RadiatedItemRendererMixin {

    private static final Identifier WHITE =
            new Identifier(Szar.MOD_ID, "textures/block/white.png");

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

        /*
         * Generate deterministic random pixel position.
         * Same stack type + same NBT → same pixel.
         * No stored coordinates required.
         */
        int seed = Item.getRawId(stack.getItem());

        if (stack.hasNbt()) {
            seed = 31 * seed + stack.getNbt().hashCode();
        }

        Random random = new Random(seed);

        int x = random.nextInt(16);
        int y = random.nextInt(16);

        matrices.push();

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer vc = vertexConsumers.getBuffer(
                RenderLayer.getBeaconBeam(WHITE, false)
        );

        // Convert 0–15 pixel coords into -0.5 → +0.5 item space
        float px = (x / 16f) - 0.5f;
        float py = (y / 16f) - 0.5f;
        float size = 1f / 16f;
        float z = 0.5f;

        // Bright radioactive green
        float r = 0.35f;
        float g = 1.0f;
        float b = 0.35f;

        vc.vertex(matrix, px, py, z)
                .color(r, g, b, 1f)
                .texture(0f, 0f)
                .overlay(overlay)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .next();

        vc.vertex(matrix, px + size, py, z)
                .color(r, g, b, 1f)
                .texture(0f, 0f)
                .overlay(overlay)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .next();

        vc.vertex(matrix, px + size, py + size, z)
                .color(r, g, b, 1f)
                .texture(0f, 0f)
                .overlay(overlay)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .next();

        vc.vertex(matrix, px, py + size, z)
                .color(r, g, b, 1f)
                .texture(0f, 0f)
                .overlay(overlay)
                .light(0xF000F0)
                .normal(0f, 0f, 1f)
                .next();

        matrices.pop();
    }
}
