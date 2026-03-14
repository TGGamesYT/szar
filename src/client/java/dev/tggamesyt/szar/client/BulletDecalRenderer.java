package dev.tggamesyt.szar.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import static dev.tggamesyt.szar.Szar.MOD_ID;

public class BulletDecalRenderer {

    private static final Identifier DECAL_TEXTURE =
            new Identifier(MOD_ID, "textures/entity/bullet_impact.png");
    private static final float SIZE = 0.15F;
    private static final long FADE_START_MS = BulletDecalStore.FADE_START_MS;
    private static final long LIFETIME_MS = BulletDecalStore.LIFETIME_MS;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BulletDecalRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cam = ctx.camera().getPos();
        long now = System.currentTimeMillis();

        VertexConsumer consumer = consumers.getBuffer(
                RenderLayer.getEntityTranslucentCull(DECAL_TEXTURE)
        );

        for (BulletDecalStore.Decal decal : BulletDecalStore.getDecals()) {
            long age = now - decal.spawnTime();
            float alpha = 1.0F;
            if (age > FADE_START_MS) {
                alpha = 1.0F - (float)(age - FADE_START_MS) / (LIFETIME_MS - FADE_START_MS);
            }
            int a = (int)(alpha * 255);
            if (a <= 0) continue;

            matrices.push();
            Direction face = decal.face();
            matrices.translate(
                    decal.pos().x - cam.x + face.getOffsetX() * 0.002,
                    decal.pos().y - cam.y + face.getOffsetY() * 0.002,
                    decal.pos().z - cam.z + face.getOffsetZ() * 0.002
            );

            applyFaceRotation(matrices, face);

            Matrix4f mat = matrices.peek().getPositionMatrix();

            consumer.vertex(mat, -SIZE,  SIZE, 0).color(255,255,255,a).texture(0,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,0,1).next();
            consumer.vertex(mat,  SIZE,  SIZE, 0).color(255,255,255,a).texture(1,1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,0,1).next();
            consumer.vertex(mat,  SIZE, -SIZE, 0).color(255,255,255,a).texture(1,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,0,1).next();
            consumer.vertex(mat, -SIZE, -SIZE, 0).color(255,255,255,a).texture(0,0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0,0,1).next();
            matrices.pop();
        }
    }

    private static void applyFaceRotation(MatrixStack matrices, Direction face) {
        switch (face) {
            case NORTH -> {}
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            case EAST  -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
            case WEST  -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            case UP    -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            case DOWN  -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
        }
    }
}