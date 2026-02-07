package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NyanEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class NyanEntityRenderer extends MobEntityRenderer<NyanEntity, NyanCatEntityModel> {

    private static final Identifier[] TEXTURES = new Identifier[12];

    static {
        for (int i = 0; i < 12; i++) {
            TEXTURES[i] = new Identifier("szar", "textures/entity/nyan_cat_textures/nyan_" + (i + 1) + ".png");
        }
    }

    public NyanEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NyanCatEntityModel(context.getPart(SzarClient.NYAN)), 0.5f);
    }

    @Override
    public Identifier getTexture(NyanEntity entity) {
        // Use age to cycle textures every 2 ticks
        int index = (entity.age/ 1) % TEXTURES.length; // age is in ticks
        return TEXTURES[index];
    }
}
