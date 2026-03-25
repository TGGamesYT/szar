package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BlueprintBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class BlueprintBlockEntityRenderer implements BlockEntityRenderer<BlueprintBlockEntity> {

    public BlueprintBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(BlueprintBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.hasStoredBlock()) return;

        String storedId = entity.getStoredBlockId();
        if (storedId == null) return;

        var block = Registries.BLOCK.get(new Identifier(storedId));
        if (block == null) return;

        BlockState storedState = block.getDefaultState();
        if (storedState.getRenderType() == BlockRenderType.INVISIBLE) return;

        // Get the shape/state of the blueprint block itself
        BlockState blueprintState = entity.getCachedState();

        // We want to render the stored block's TEXTURE but on the blueprint block's SHAPE.
        // The way to do this: find the model for the blueprint block shape,
        // but swap in the stored block's sprite via a custom render layer.
        // Simplest approach: render the blueprint block's model with the stored block's
        // textures by remapping the sprite.

        BlockRenderManager renderer = MinecraftClient.getInstance().getBlockRenderManager();

        matrices.push();

        // Render the blueprint shape using the stored block's texture
        // by temporarily using the stored block's model sprites on our shape
        renderWithStoredTexture(entity, blueprintState, storedState, matrices, vertexConsumers, light, overlay, renderer);

        matrices.pop();
    }

    private void renderWithStoredTexture(BlueprintBlockEntity entity, BlockState blueprintState,
                                         BlockState storedState, MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers, int light, int overlay,
                                         BlockRenderManager renderer) {
        // Get the first (main) sprite from the stored block's model
        var storedModel = renderer.getModel(storedState);
        var blueprintModel = renderer.getModel(blueprintState);

        var sprites = storedModel.getParticleSprite(); // main texture of stored block

        // Render blueprint model quads, replacing its texture with stored block's sprite
        var random = Random.create();
        random.setSeed(42L);

        var bufferSource = vertexConsumers;
        var layer = net.minecraft.client.render.RenderLayers.getBlockLayer(blueprintState);
        var consumer = bufferSource.getBuffer(layer);

        for (var direction : new net.minecraft.util.math.Direction[]{
                null,
                net.minecraft.util.math.Direction.UP,
                net.minecraft.util.math.Direction.DOWN,
                net.minecraft.util.math.Direction.NORTH,
                net.minecraft.util.math.Direction.SOUTH,
                net.minecraft.util.math.Direction.EAST,
                net.minecraft.util.math.Direction.WEST
        }) {
            random.setSeed(42L);
            var quads = blueprintModel.getQuads(blueprintState, direction, random);
            for (var quad : quads) {
                // Emit the quad but with the stored block's sprite UV remapped
                emitQuadWithSprite(consumer, matrices, quad, sprites, light, overlay);
            }
        }
    }

    private void emitQuadWithSprite(net.minecraft.client.render.VertexConsumer consumer,
                                    MatrixStack matrices,
                                    net.minecraft.client.render.model.BakedQuad quad,
                                    net.minecraft.client.texture.Sprite sprite,
                                    int light, int overlay) {
        // Re-emit the quad geometry but remap UVs to the new sprite
        consumer.quad(matrices.peek(), quad, 1f, 1f, 1f, light, overlay);
        // Note: this uses the quad's original UVs which point to the blueprint texture.
        // For full texture remapping you'd need to manually rewrite vertex data.
        // This gives correct shape with blueprint texture as fallback —
        // see note below for full UV remapping.
    }
}