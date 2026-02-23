package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class PregnantEffect extends StatusEffect {

    public PregnantEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF66CC); // pink color
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false; // no ticking needed, only care about end
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        if (!entity.getWorld().isClient && entity instanceof ServerPlayerEntity player) {
            ServerWorld world = (ServerWorld) player.getWorld();

            UUID partnerUuid = Szar.pregnantPartners.remove(player.getUuid());
            ServerPlayerEntity partner;
            if (partnerUuid != null) {
                partner = (ServerPlayerEntity) world.getPlayerByUuid(partnerUuid);
            } else {
                partner = world.getPlayers().stream()
                        .filter(p -> p != player)
                        .min((a, b) -> Double.compare(player.squaredDistanceTo(a), player.squaredDistanceTo(b)))
                        .orElse(player);
            }

            KidEntity kid = Szar.Kid.create(world);
            if (kid != null) {
                kid.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(),
                        player.getYaw(), player.getPitch());
                kid.setParents(player.getUuid(), partner.getUuid());
                world.spawnEntity(kid);
            }
        }

        super.onRemoved(entity, attributes, amplifier);
    }
}