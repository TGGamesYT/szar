package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;

public class ArrestedEffect extends StatusEffect {

    private static double originalspeed;
    public ArrestedEffect() {
        // BENEFICIAL = false because it's like a debuff
        super(StatusEffectCategory.HARMFUL, 0xA0A0A0); // gray color
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        // This method is called every tick while the effect is active
        if (!entity.getWorld().isClient) {
            // Stop movement completely
            entity.setVelocity(0, entity.getVelocity().y, 0); // keep Y velocity (falling) if you want
            entity.velocityModified = true;

            // Stop AI for mobs
            if (entity instanceof MobEntity mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }

            // Optional: reset movement speed to 0
            originalspeed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getValue();
            entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(0.0D);
        }
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Run every tick
        return true;
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        // Reset movement speed when effect ends
        entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(originalspeed); // default walking speed for mobs
    }
}
