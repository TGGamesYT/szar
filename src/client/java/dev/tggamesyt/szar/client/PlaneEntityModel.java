package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.PlaneEntity;
import dev.tggamesyt.szar.PlaneAnimation;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import org.joml.Vector3f;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class PlaneEntityModel extends SinglePartEntityModel<Entity> {
	private final Vector3f tempVec = new Vector3f();
	private final ModelPart root;
	private final ModelPart right_wheel;
	private final ModelPart base;
	private final ModelPart left_wheel;
	private final ModelPart left_wing;
	private final ModelPart right_wing;
	private final ModelPart back_wing;
	private final ModelPart rotor;
	public PlaneEntityModel(ModelPart root) {
		this.root = root;
		this.right_wheel = root.getChild("right_wheel");
		this.base = root.getChild("base");
		this.left_wheel = root.getChild("left_wheel");
		this.left_wing = root.getChild("left_wing");
		this.right_wing = root.getChild("right_wing");
		this.back_wing = root.getChild("back_wing");
		this.rotor = root.getChild("rotor");
	}
	public final AnimationState engineStart = new AnimationState();
	public final AnimationState flying = new AnimationState();
	public final AnimationState engineStop = new AnimationState();

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData right_wheel = modelPartData.addChild("right_wheel", ModelPartBuilder.create().uv(24, 58).cuboid(-2.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(-3.0F, 19.0F, 0.0F));

		ModelPartData cube_r1 = right_wheel.addChild("cube_r1", ModelPartBuilder.create().uv(32, 58).cuboid(0.0F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create().uv(24, 55).cuboid(-3.0F, 0.0F, -1.0F, 6.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(0, 24).cuboid(-2.0F, -1.0F, -11.0F, 4.0F, 1.0F, 18.0F, new Dilation(0.0F))
		.uv(44, 24).cuboid(2.0F, -3.0F, -8.0F, 1.0F, 2.0F, 15.0F, new Dilation(0.0F))
		.uv(44, 24).cuboid(-3.0F, -3.0F, -8.0F, 1.0F, 2.0F, 15.0F, new Dilation(0.0F))
		.uv(0, 0).cuboid(-2.0F, -3.0F, -12.0F, 4.0F, 2.0F, 22.0F, new Dilation(0.0F))
		.uv(44, 41).cuboid(-1.0F, -5.0F, -8.0F, 2.0F, 2.0F, 12.0F, new Dilation(0.0F))
		.uv(52, 12).cuboid(-1.0F, -10.0F, 13.0F, 2.0F, 7.0F, 4.0F, new Dilation(0.0F))
		.uv(24, 62).cuboid(-1.0F, -3.0F, -13.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
		.uv(0, 55).cuboid(1.0F, -5.0F, -7.0F, 1.0F, 2.0F, 11.0F, new Dilation(0.0F))
		.uv(0, 55).cuboid(-2.0F, -5.0F, -7.0F, 1.0F, 2.0F, 11.0F, new Dilation(0.0F))
		.uv(34, 51).cuboid(-1.0F, -4.0F, 4.0F, 2.0F, 1.0F, 3.0F, new Dilation(0.0F))
		.uv(60, 61).cuboid(1.0F, -4.0F, 4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(60, 61).cuboid(-2.0F, -4.0F, 4.0F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(52, 0).cuboid(-1.0F, -6.0F, -7.0F, 2.0F, 1.0F, 11.0F, new Dilation(0.0F))
		.uv(52, 61).cuboid(-1.0F, -5.0F, 4.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
		.uv(38, 62).cuboid(-1.0F, -4.0F, -9.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 19.0F, 0.0F));

		ModelPartData cube_r2 = base.addChild("cube_r2", ModelPartBuilder.create().uv(40, 55).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -2.0F, 11.0F, 0.3927F, 0.0F, 0.0F));

		ModelPartData left_wheel = modelPartData.addChild("left_wheel", ModelPartBuilder.create().uv(24, 58).cuboid(0.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(3.0F, 19.0F, 0.0F));

		ModelPartData cube_r3 = left_wheel.addChild("cube_r3", ModelPartBuilder.create().uv(32, 58).cuboid(-1.0F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		ModelPartData left_wing = modelPartData.addChild("left_wing", ModelPartBuilder.create(), ModelTransform.pivot(2.0F, 18.0F, -5.0F));

		ModelPartData cube_r4 = left_wing.addChild("cube_r4", ModelPartBuilder.create().uv(0, 43).cuboid(0.0F, 0.0F, 0.0F, 15.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));

		ModelPartData right_wing = modelPartData.addChild("right_wing", ModelPartBuilder.create(), ModelTransform.pivot(-2.0F, 18.0F, -5.0F));

		ModelPartData cube_r5 = right_wing.addChild("cube_r5", ModelPartBuilder.create().uv(0, 43).cuboid(-15.0F, 0.0F, 0.0F, 15.0F, 1.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.3927F, 0.0F));

		ModelPartData back_wing = modelPartData.addChild("back_wing", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 16.0F, 14.0F));

		ModelPartData cube_r6 = back_wing.addChild("cube_r6", ModelPartBuilder.create().uv(0, 51).cuboid(-7.0F, -1.0F, 0.0F, 14.0F, 1.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

		ModelPartData rotor = modelPartData.addChild("rotor", ModelPartBuilder.create().uv(52, 55).cuboid(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 16.0F, -13.0F));
		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		right_wheel.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		base.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		left_wheel.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		left_wing.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		right_wing.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		back_wing.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		rotor.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}

	@Override
	public void setAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		// Reset transforms
		this.root.traverse().forEach(ModelPart::resetTransform);
		if (entity instanceof PlaneEntity planeEntity) {
			PlaneAnimation anim = planeEntity.getCurrentAnimation();

			if (anim != null) {
				AnimationHelper.animate(
						this,
						PlaneAnimationResolver.resolve(anim),
						planeEntity.age,
						1.0F,
						tempVec
				);
			} else {
				this.root.traverse().forEach(ModelPart::resetTransform);
			}
		}
	}
}