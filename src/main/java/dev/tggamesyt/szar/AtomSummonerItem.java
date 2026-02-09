package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class AtomSummonerItem extends Item {

    private static final int COOLDOWN_TICKS = 20 * 60; // 10 minutes
    private static final double RAY_DISTANCE = 500.0D;

    public AtomSummonerItem(Settings settings) {
        super(settings.maxDamage(2));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient()) {
            return TypedActionResult.success(player.getStackInHand(hand));
        }

        ServerWorld serverWorld = (ServerWorld) world;
        ItemStack stack = player.getStackInHand(hand);

        // Raycast from eyes
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(RAY_DISTANCE));

        BlockHitResult hit = serverWorld.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        // If player is looking into nothing, abort
        if (hit.getType() == HitResult.Type.MISS) {
            return TypedActionResult.fail(stack);
        }

        BlockPos hitPos = hit.getBlockPos();
        Vec3d spawnPos = Vec3d.ofCenter(hitPos).add(0, 100, 0);

        AtomEntity atom = new AtomEntity(Szar.AtomEntityType, serverWorld);
        atom.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);

        serverWorld.spawnEntity(atom);

        // Cooldown + durability
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        stack.damage(1, player, p -> p.sendToolBreakStatus(hand));

        return TypedActionResult.success(stack);
    }
}
