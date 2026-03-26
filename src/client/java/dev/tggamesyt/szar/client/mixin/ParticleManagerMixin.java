package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.*;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Shadow
    public ClientWorld world;

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void redirectBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (world == null) return;

        if (!(state.getBlock() instanceof BlueprintStairsBlock ||
                state.getBlock() instanceof BlueprintSlabBlock ||
                state.getBlock() instanceof BlueprintDoorBlock ||
                state.getBlock() instanceof BlueprintTrapDoorBlock ||
                state.getBlock() instanceof BlueprintWallBlock ||
                state.getBlock() instanceof BlueprintFenceBlock)) {
            return;
        }

        var be = world.getBlockEntity(pos);
        if (!(be instanceof BlueprintBlockEntity blueprint) || !blueprint.hasStoredBlock()) return;

        BlockState storedState = Registries.BLOCK
                .get(new Identifier(blueprint.getStoredBlockId()))
                .getDefaultState();

        ci.cancel();

        // ✅ USE BLUEPRINT SHAPE, NOT STORED
        var shape = state.getOutlineShape(world, pos);

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double dx = Math.min(1.0, maxX - minX);
            double dy = Math.min(1.0, maxY - minY);
            double dz = Math.min(1.0, maxZ - minZ);

            int ix = Math.max(2, net.minecraft.util.math.MathHelper.ceil(dx / 0.25));
            int iy = Math.max(2, net.minecraft.util.math.MathHelper.ceil(dy / 0.25));
            int iz = Math.max(2, net.minecraft.util.math.MathHelper.ceil(dz / 0.25));

            for (int x = 0; x < ix; x++) {
                for (int y = 0; y < iy; y++) {
                    for (int z = 0; z < iz; z++) {

                        double fx = (x + 0.5) / ix;
                        double fy = (y + 0.5) / iy;
                        double fz = (z + 0.5) / iz;

                        double px = fx * dx + minX;
                        double py = fy * dy + minY;
                        double pz = fz * dz + minZ;

                        ((ParticleManager)(Object)this).addParticle(
                                new BlockDustParticle(
                                        world,
                                        pos.getX() + px,
                                        pos.getY() + py,
                                        pos.getZ() + pz,
                                        fx - 0.5,
                                        fy - 0.5,
                                        fz - 0.5,
                                        storedState, // ✅ texture comes from stored
                                        pos
                                )
                        );
                    }
                }
            }
        });
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void redirectBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlueprintStairsBlock ||
                state.getBlock() instanceof BlueprintSlabBlock ||
                state.getBlock() instanceof BlueprintDoorBlock ||
                state.getBlock() instanceof BlueprintTrapDoorBlock ||
                state.getBlock() instanceof BlueprintWallBlock ||
                state.getBlock() instanceof BlueprintFenceBlock)) {return;}

        var be = world.getBlockEntity(pos);
        if (!(be instanceof BlueprintBlockEntity blueprint) || !blueprint.hasStoredBlock()) return;

        BlockState storedState = Registries.BLOCK
                .get(new Identifier(blueprint.getStoredBlockId()))
                .getDefaultState();
        ci.cancel();

        Box box = storedState.getOutlineShape(world, pos).getBoundingBox();
        int i = pos.getX(), j = pos.getY(), k = pos.getZ();
        Random random = Random.create();
        double d = i + random.nextDouble() * (box.maxX - box.minX - 0.2) + 0.1 + box.minX;
        double e = j + random.nextDouble() * (box.maxY - box.minY - 0.2) + 0.1 + box.minY;
        double g = k + random.nextDouble() * (box.maxZ - box.minZ - 0.2) + 0.1 + box.minZ;
        if (direction == Direction.DOWN)  e = j + box.minY - 0.1;
        if (direction == Direction.UP)    e = j + box.maxY + 0.1;
        if (direction == Direction.NORTH) g = k + box.minZ - 0.1;
        if (direction == Direction.SOUTH) g = k + box.maxZ + 0.1;
        if (direction == Direction.WEST)  d = i + box.minX - 0.1;
        if (direction == Direction.EAST)  d = i + box.maxX + 0.1;

        ((ParticleManager)(Object)this).addParticle(
                new BlockDustParticle(world, d, e, g, 0, 0, 0, storedState, pos).move(0.2f).scale(0.6f)
        );
    }
}