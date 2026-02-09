package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.AtomEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class Atom extends EntityModel<AtomEntity> {
	private final ModelPart bb_main;
	public Atom(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData bb_main = modelPartData.addChild("bb_main", ModelPartBuilder.create().uv(32, 15).cuboid(-2.0F, -4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new Dilation(0.0F))
		.uv(0, 30).cuboid(-3.0F, -6.0F, -3.0F, 6.0F, 2.0F, 6.0F, new Dilation(0.0F))
		.uv(0, 15).cuboid(-4.0F, -13.0F, -4.0F, 8.0F, 7.0F, 8.0F, new Dilation(0.0F))
		.uv(36, 41).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(24, 30).cuboid(-3.0F, -15.0F, -3.0F, 6.0F, 2.0F, 6.0F, new Dilation(0.0F))
		.uv(32, 21).cuboid(-2.0F, -17.0F, -2.0F, 4.0F, 2.0F, 4.0F, new Dilation(0.0F))
		.uv(42, 27).cuboid(-1.0F, -18.0F, -1.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(0, 43).cuboid(2.0F, -19.0F, -3.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
		.uv(4, 43).cuboid(-3.0F, -19.0F, -3.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
		.uv(8, 43).cuboid(-3.0F, -19.0F, 2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
		.uv(12, 43).cuboid(2.0F, -19.0F, 2.0F, 1.0F, 4.0F, 1.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-6.0F, -22.0F, -6.0F, 12.0F, 3.0F, 12.0F, new Dilation(0.0F))
		.uv(0, 38).cuboid(-2.0F, -19.0F, -2.0F, 4.0F, 1.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData cube_r1 = bb_main.addChild("cube_r1", ModelPartBuilder.create().uv(26, 38).cuboid(-1.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, -15.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		ModelPartData cube_r2 = bb_main.addChild("cube_r2", ModelPartBuilder.create().uv(36, 38).cuboid(-2.0F, -2.0F, 0.0F, 4.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -15.0F, -3.0F, -0.3927F, 0.0F, 0.0F));

		ModelPartData cube_r3 = bb_main.addChild("cube_r3", ModelPartBuilder.create().uv(16, 38).cuboid(0.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -15.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		ModelPartData cube_r4 = bb_main.addChild("cube_r4", ModelPartBuilder.create().uv(32, 27).cuboid(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -15.0F, 3.0F, 0.3927F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 64, 64);
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		bb_main.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}

	@Override
	public void setAngles(AtomEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

	}
}