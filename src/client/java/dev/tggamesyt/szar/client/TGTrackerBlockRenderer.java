package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.TrackerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.WorldRenderer;

public class TGTrackerBlockRenderer implements BlockEntityRenderer<TrackerBlockEntity> {

    private final ItemRenderer itemRenderer;

    public TGTrackerBlockRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(TrackerBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        boolean holding = player.getMainHandStack().isOf(Szar.TRACKER_BLOCK_ITEM.asItem())
                || player.getOffHandStack().isOf(Szar.TRACKER_BLOCK_ITEM.asItem());
        if (!holding) return;

        BlockPos pos = entity.getPos();
        int lightLevel = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos);

        // Use the actual camera yaw and pitch — this is what barrier does
        // Camera yaw/pitch are already available from the camera object
        float cameraYaw   = client.gameRenderer.getCamera().getYaw();
        float cameraPitch = client.gameRenderer.getCamera().getPitch();

        matrices.push();
        matrices.translate(0.5, 0.25, 0.5);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

        matrices.scale(2f, 2f, 2f);

        ItemStack stack = new ItemStack(Szar.TRACKER_BLOCK_ITEM.asItem());
        BakedModel model = itemRenderer.getModel(stack, entity.getWorld(), null, 0);

        itemRenderer.renderItem(
                stack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                lightLevel,
                overlay,
                model
        );

        matrices.pop();
    }
}