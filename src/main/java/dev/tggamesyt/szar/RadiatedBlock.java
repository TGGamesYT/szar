package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.joml.Vector3f;

public class RadiatedBlock extends Block {

    private static final DustParticleEffect GREEN_DUST =
            new DustParticleEffect(new Vector3f(0.2f, 1.0f, 0.2f), 1.0f);

    public RadiatedBlock(Settings settings) {
        super(settings);
    }

    // Standing on it
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity living) {
            applyRadiation(living, 60, 0);
        }

        spawnParticles(world, pos);
        super.onSteppedOn(world, pos, state, entity);
    }

    // When mined
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            applyRadiation(player, 200, 1);
        }

        spawnParticles(world, pos);
        super.onBreak(world, pos, state, player);
    }

    // Random tick glow like redstone
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextFloat() < 0.3f) {
            spawnParticles(world, pos);
        }
    }

    private void applyRadiation(LivingEntity entity, int duration, int amplifier) {
        entity.addStatusEffect(new StatusEffectInstance(
                Szar.RADIATION,
                duration,
                amplifier,
                true,
                true,
                true
        ));
    }

    private void spawnParticles(World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 5; i++) {
                serverWorld.spawnParticles(
                        GREEN_DUST,
                        pos.getX() + world.random.nextDouble(),
                        pos.getY() + 1.0,
                        pos.getZ() + world.random.nextDouble(),
                        1,
                        0, 0, 0,
                        0
                );
            }
        }
    }
}
