package dev.tggamesyt.szar;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TwoTowersUtil {

    public static void grantNearbyAdvancement(
            ServerWorld world,
            BlockPos center,
            int radius
    ) {
        double radiusSq = radius * radius;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getPos().squaredDistanceTo(
                    center.getX() + 0.5,
                    center.getY() + 0.5,
                    center.getZ() + 0.5
            ) <= radiusSq) {

                grant(player);
            }
        }
    }

    private static void grant(ServerPlayerEntity player) {
        Identifier id = new Identifier(Szar.MOD_ID, "two_towers_explosion");
        Advancement adv = player.server.getAdvancementLoader().get(id);
        if (adv == null) return;

        AdvancementProgress progress =
                player.getAdvancementTracker().getProgress(adv);

        if (progress.isDone()) return;

        for (String criterion : progress.getUnobtainedCriteria()) {
            player.getAdvancementTracker().grantCriterion(adv, criterion);
        }
    }
}

