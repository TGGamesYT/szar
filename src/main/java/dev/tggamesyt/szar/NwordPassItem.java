package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class NwordPassItem extends Item {

    public NwordPassItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // Play totem animation
            if (user instanceof ServerPlayerEntity serverPlayer) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeItemStack(stack);

                ServerPlayNetworking.send(serverPlayer, Szar.TOTEMPACKET, buf);
            }
            //world.sendEntityStatus(user, (byte) 35);

            // Grant advancement
            if (user instanceof ServerPlayerEntity serverPlayer) {
                Szar.grantAdvancement(serverPlayer, "nwordpass");
            }

            // Consume item
            stack.decrement(1);
        }

        return TypedActionResult.success(stack, world.isClient);
    }
}
