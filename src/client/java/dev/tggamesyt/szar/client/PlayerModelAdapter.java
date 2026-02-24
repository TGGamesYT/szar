package dev.tggamesyt.szar.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.Optional;

public class PlayerModelAdapter<T extends LivingEntity>
        extends SinglePartEntityModel<T> {
    public ModelPart root;
    private final PlayerEntityModel<T> playerModel;

    public PlayerModelAdapter(PlayerEntityModel<T> playerModel) {
        this.playerModel = playerModel;
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    public void setRoot(ModelPart root) {
        this.root = root;
    }

    @Override
    public Optional<ModelPart> getChild(String name) {
        return switch (name) {
            case "head" -> Optional.of(playerModel.head);
            case "body" -> Optional.of(playerModel.body);
            case "left_arm" -> Optional.of(playerModel.leftArm);
            case "right_arm" -> Optional.of(playerModel.rightArm);
            case "left_leg" -> Optional.of(playerModel.leftLeg);
            case "right_leg" -> Optional.of(playerModel.rightLeg);
            default -> Optional.empty();
        };
    }

    @Override
    public void setAngles(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        playerModel.setAngles(entity, limbSwing, limbSwingAmount,
                ageInTicks, netHeadYaw, headPitch);
    }
}