package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FaszItem extends BlockItem {

    public FaszItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && stack.isOf(this)) {
            ServerWorld serverWorld = (ServerWorld) world;

            // Get the direction the player's torso is looking
            var lookVec = user.getRotationVec(1.0F); // normalized direction vector

            // Calculate the particle spawn position 2 blocks ahead
            double px = user.getX() + lookVec.x * 2;
            double py = user.getBodyY(0.5); // torso height
            double pz = user.getZ() + lookVec.z * 2;

            // Spawn block particles
            serverWorld.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, Szar.FASZ_BLOCK.getDefaultState()),
                    px, py, pz, // position
                    20, // particle count
                    0.3, 0.3, 0.3, // spread in x/y/z
                    0.05 // velocity
            );
        }

        return TypedActionResult.pass(stack);
    }
}
