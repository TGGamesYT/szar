package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.AtomEntity;
import dev.tggamesyt.szar.Szar;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class AtomEntityRenderer extends EntityRenderer<AtomEntity> {

    public static final EntityModelLayer ATOM_LAYER =
            new EntityModelLayer(new Identifier(Szar.MOD_ID, "atom"), "main");

    private static final Identifier TEXTURE =
            new Identifier(Szar.MOD_ID, "textures/entity/nuke.png");

    private final Atom model;

    public AtomEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new Atom(context.getPart(ATOM_LAYER));
        this.shadowRadius = 1.5F;
    }

    @Override
    public void render(
            AtomEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        matrices.push();

        model.render(
                matrices,
                vertexConsumers.getBuffer(model.getLayer(TEXTURE)),
                light,
                OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(AtomEntity entity) {
        return TEXTURE;
    }
}
