package dev.tggamesyt.szar;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerValueTimer {

    // 20 ticks/sec * 60 sec * 3 min
    private static final int INTERVAL_TICKS = 20 * 60 * 3;
    private static final int ONE_MIN = 20 * 20;

    private static int tickCounter = 0;
    private static int tickCounterMin = 0;

    static void onServerTick(MinecraftServer server) {
        tickCounter++;
        tickCounterMin++;

        if (tickCounter >= INTERVAL_TICKS) {
            tickCounter = 0;
            runDecrease(server);
        }
        if (tickCounterMin >= ONE_MIN) {
            tickCounterMin = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                if (Szar.PLAYER_ADDICTION_LEVEL.getOrDefault(player.getUuid(), false) && Szar.PLAYER_JOINT_LEVEL.getOrDefault(player.getUuid(), 0) < 10) {
                    if (!player.hasStatusEffect(Szar.DROG_EFFECT)) {
                        player.addStatusEffect(
                                new StatusEffectInstance(StatusEffects.WITHER, 10, 0)
                        );
                    };
                }
            }
        }
    }

    private static void runDecrease(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            
            int current = Szar.PLAYER_JOINT_LEVEL.getOrDefault(uuid, 0);
            int newValue = Math.max(0, current - 2);
            Szar.LOGGER.info(player.getEntityName() + "'s joint level is now " + newValue);
            Szar.PLAYER_JOINT_LEVEL.put(uuid, newValue);
        }
    }
}
