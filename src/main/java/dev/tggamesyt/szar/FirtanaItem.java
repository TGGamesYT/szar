package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FirtanaItem extends Item {

    public FirtanaItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!world.isClient) {
            Szar.grantAdvancement(user, "oi");
            for (ServerPlayerEntity player : ((ServerWorld) world).getPlayers()) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(user.getUuidAsString());
                ServerPlayNetworking.send(player, Szar.PLAY_VIDEO, buf);
            }

        }

        return TypedActionResult.success(user.getStackInHand(hand));
    }
}