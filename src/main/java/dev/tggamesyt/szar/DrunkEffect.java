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
            if (world.getTime() % 20 != 0) continue;

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.hasStatusEffect(Szar.DRUNK_EFFECT)) continue;
                if (world.random.nextInt(5) >= 2) continue; // 2 in 5 chance

                double reach = 4.5;
                net.minecraft.util.math.Vec3d eyePos = player.getEyePos();
                net.minecraft.util.math.Vec3d lookVec = player.getRotationVector();
                net.minecraft.util.math.Vec3d endPos = eyePos.add(lookVec.multiply(reach));

                EntityHitResult entityHit = net.minecraft.entity.projectile.ProjectileUtil
                        .getEntityCollision(
                                world,
                                player,
                                eyePos,
                                endPos,
                                player.getBoundingBox().stretch(lookVec.multiply(reach)).expand(1.0),
                                e -> e instanceof LivingEntity && e != player && !e.isSpectator()
                        );

                if (entityHit == null) continue;
                if (!(entityHit.getEntity() instanceof LivingEntity target)) continue;

                player.attack(target);
                // Swing main hand
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }
}