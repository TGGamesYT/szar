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
import net.minecraft.util.math.Direction;
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

        BlockState blueprintState = entity.getCachedState();
        BlockRenderManager renderer = MinecraftClient.getInstance().getBlockRenderManager();

        var storedModel = renderer.getModel(storedState);
        var blueprintModel = renderer.getModel(blueprintState);
        var particleSprite = storedModel.getParticleSprite();

        var random = Random.create();
        var layer = net.minecraft.client.render.RenderLayers.getBlockLayer(storedState);
        var consumer = vertexConsumers.getBuffer(layer);

        matrices.push();
// No scaling here anymore

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
                int[] vertexData = quad.getVertexData().clone();
                remapUVs(vertexData, quad.getSprite(), particleSprite);
                offsetVertsAlongNormal(vertexData, quad.getFace(), 0.001f);

                consumer.quad(
                        matrices.peek(),
                        new net.minecraft.client.render.model.BakedQuad(
                                vertexData,
                                quad.getColorIndex(),
                                quad.getFace(),
                                particleSprite,
                                quad.hasShade()
                        ),
                        1f, 1f, 1f, light, overlay
                );
            }
        }

        matrices.pop();

    }

    private void remapUVs(int[] vertexData,
                          net.minecraft.client.texture.Sprite fromSprite,
                          net.minecraft.client.texture.Sprite toSprite) {
        // Vertex format: X, Y, Z, COLOR, U, V, UV2, NORMAL — each vertex is 8 ints
        int vertexSize = 8;
        for (int i = 0; i < 4; i++) {
            int uvOffset = i * vertexSize + 4;
            // Unpack UV floats from int bits
            float u = Float.intBitsToFloat(vertexData[uvOffset]);
            float v = Float.intBitsToFloat(vertexData[uvOffset + 1]);

            // Normalize UV from the source sprite's atlas space to 0-1
            float normalizedU = (u - fromSprite.getMinU()) / (fromSprite.getMaxU() - fromSprite.getMinU());
            float normalizedV = (v - fromSprite.getMinV()) / (fromSprite.getMaxV() - fromSprite.getMinV());

            // Remap to target sprite's atlas space
            float newU = toSprite.getMinU() + normalizedU * (toSprite.getMaxU() - toSprite.getMinU());
            float newV = toSprite.getMinV() + normalizedV * (toSprite.getMaxV() - toSprite.getMinV());

            vertexData[uvOffset]     = Float.floatToRawIntBits(newU);
            vertexData[uvOffset + 1] = Float.floatToRawIntBits(newV);
        }
    }

    private void offsetVertsAlongNormal(int[] vertexData, net.minecraft.util.math.Direction face, float amount) {
        float dx = face.getOffsetX() * amount;
        float dy = face.getOffsetY() * amount;
        float dz = face.getOffsetZ() * amount;

        int vertexSize = 8;
        for (int i = 0; i < 4; i++) {
            int base = i * vertexSize;
            float x = Float.intBitsToFloat(vertexData[base]);
            float y = Float.intBitsToFloat(vertexData[base + 1]);
            float z = Float.intBitsToFloat(vertexData[base + 2]);

            vertexData[base] = Float.floatToRawIntBits(x + dx);
            vertexData[base + 1] = Float.floatToRawIntBits(y + dy);
            vertexData[base + 2] = Float.floatToRawIntBits(z + dz);
        }
    }

}