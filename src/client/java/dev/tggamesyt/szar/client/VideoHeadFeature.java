package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class VideoHeadFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private final PlayerEntityRenderer renderer;

    public VideoHeadFeature(PlayerEntityRenderer renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        // Only render if the player is playing a video
        if (!VideoManager.isPlaying(player.getUuid())) return;

        Identifier frame = VideoManager.getCurrentFrame(player.getUuid());
        if (frame == null) return;

        matrices.push();

        // Rotate to match the head
        this.getContextModel().head.rotate(matrices);

        // Position quad slightly in front of the face
        float size = 0.5f;
        matrices.translate(-size / 2f, -size / 2f - 0.24f, -0.30f);

        // Render the video frame
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(frame));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        vc.vertex(matrix, 0, 0, 0)
                .color(255,255,255,255)
                .texture(0,0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0,0,1)
                .next();

        vc.vertex(matrix, size, 0, 0)
                .color(255,255,255,255)
                .texture(1,0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0,0,1)
                .next();

        vc.vertex(matrix, size, size, 0)
                .color(255,255,255,255)
                .texture(1,1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0,0,1)
                .next();

        vc.vertex(matrix, 0, size, 0)
                .color(255,255,255,255)
                .texture(0,1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0,0,1)
                .next();

        matrices.pop();
    }
}