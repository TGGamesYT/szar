package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;

import java.util.UUID;

public class ArrestedEffect extends StatusEffect {

    private static final UUID SPEED_MODIFIER_ID =
            UUID.fromString("b2c7c2b8-0e55-4c9f-9e8d-8c5fdd7b9c11");

    public ArrestedEffect() {
        super(StatusEffectCategory.HARMFUL, 0xA0A0A0);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!entity.getWorld().isClient) {
            // Freeze horizontal movement
            entity.setVelocity(0, entity.getVelocity().y, 0);
            entity.velocityModified = true;

            // Stop mob AI
            if (entity instanceof MobEntity mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        var speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;

        // Check by UUID manually (1.20.1-safe)
        if (speed.getModifier(SPEED_MODIFIER_ID) == null) {
            speed.addPersistentModifier(new EntityAttributeModifier(
                    SPEED_MODIFIER_ID,
                    "Arrested speed reduction",
                    -1.0D,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }


    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        var speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
