package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class DrunkEffect extends StatusEffect {

    public DrunkEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF9900);
    }

    static void tick(MinecraftServer server) {
        for (var world : server.getWorlds()) {
            // Every 20 ticks = 1 second
            if (world.getTime() % 20 != 0) continue;

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.hasStatusEffect(Szar.DRUNK_EFFECT)) continue;

                // 1 in 5 chance
                if (world.random.nextInt(5) != 0) continue;

                // Raycast to find what the player is looking at
                HitResult hit = player.raycast(4.5, 0, false);
                if (hit.getType() != HitResult.Type.ENTITY) continue;
                if (!(hit instanceof EntityHitResult entityHit)) continue;
                if (!(entityHit.getEntity() instanceof LivingEntity target)) continue;

                // Hit the entity as if the player attacked it
                player.attack(target);
            }
        }
    }
}