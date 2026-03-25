package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BlueprintBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedQuad;
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

        var renderer = MinecraftClient.getInstance().getBlockRenderManager();

        var storedModel = renderer.getModel(storedState);
        var blueprintModel = renderer.getModel(blueprintState);

        var wrappedModel = new BlueprintWrappedModel(
                blueprintModel,
                storedModel,
                blueprintState,
                storedState, entity
        );

        var layer = net.minecraft.client.render.RenderLayers.getBlockLayer(storedState);
        var consumer = vertexConsumers.getBuffer(layer);

        matrices.push();

        renderer.getModelRenderer().render(
                entity.getWorld(),
                wrappedModel,
                storedState,
                entity.getPos(),
                matrices,
                consumer,
                false,
                net.minecraft.util.math.random.Random.create(),
                42L,
                overlay
        );

        matrices.pop();
    }
}