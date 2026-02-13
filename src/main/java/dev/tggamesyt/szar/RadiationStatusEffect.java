package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;

public class RadiationStatusEffect extends StatusEffect {

    public RadiationStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x39FF14);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        int level = amplifier + 1;
        int interval = (int) getInterpolatedInterval(level);

        return duration % Math.max(interval, 1) == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        int level = amplifier + 1;

        float damage = (float) getInterpolatedDamage(level);

        entity.damage(
                entity.getDamageSources().magic(),
                damage
        );
    }

    /* ========================= */
    /*      INTERPOLATION        */
    /* ========================= */

    private double getInterpolatedDamage(int level) {
        if (level <= 1) return 1.0;

        if (level <= 10)
            return lerp(level, 1, 10, 1.0, 2.0);

        if (level <= 100)
            return lerp(level, 10, 100, 2.0, 4.0);

        return lerp(level, 100, 255, 4.0, 8.0);
    }

    private double getInterpolatedInterval(int level) {
        if (level <= 1) return 200;

        if (level <= 10)
            return lerp(level, 1, 10, 200, 100);

        if (level <= 100)
            return lerp(level, 10, 100, 100, 40);

        return lerp(level, 100, 255, 40, 20);
    }

    private double lerp(double value, double minLevel, double maxLevel,
                        double minValue, double maxValue) {

        double t = (value - minLevel) / (maxLevel - minLevel);
        return minValue + t * (maxValue - minValue);
    }
}
