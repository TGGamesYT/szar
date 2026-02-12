package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AtomItem extends Item {

    public AtomItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;

        ServerWorld serverWorld = (ServerWorld) world;
        BlockPos pos = context.getBlockPos(); // The block the player clicked
        Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 1, 0); // Spawn 1 block above clicked block

        // Create entity
        AtomEntity atom = new AtomEntity(Szar.AtomEntityType, serverWorld);
        atom.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);

        // Spawn it
        serverWorld.spawnEntity(atom);

        // Damage the item or set cooldown if desired
        ItemStack stack = context.getStack();
        stack.damage(1, context.getPlayer(), p -> p.sendToolBreakStatus(context.getHand()));

        return ActionResult.CONSUME;
    }
}
