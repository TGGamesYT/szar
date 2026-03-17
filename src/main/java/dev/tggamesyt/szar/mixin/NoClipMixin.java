package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.TrackerBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.world.WorldView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class NoClipMixin {

    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
        at = @At("HEAD"), cancellable = true
    )
    private void szar_noClipBelowTracker(BlockView world, BlockPos pos,
                                          ShapeContext ctx,
                                          CallbackInfoReturnable<VoxelShape> cir) {
        if (!(ctx instanceof EntityShapeContext esc)) return;
        if (!(esc.getEntity() instanceof PlayerEntity)) return;

        for (int i = 1; i <= 5; i++) {
            BlockPos above = pos.up(i);
            if (world.getBlockState(above).getBlock() instanceof TrackerBlock) {
                cir.setReturnValue(VoxelShapes.empty());
                return;
            }
        }
    }
}
