package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SmilerEffectManager {

    public static final Identifier FLASHBANG_PACKET = new Identifier("szar", "flashbang");
    public static final Identifier JUMPSCARE_PACKET = new Identifier("szar", "jumpscare");

    public static void triggerFlashbang(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, FLASHBANG_PACKET, buf);
    }

    public static void triggerJumpscare(ServerPlayerEntity player, SmilerType type) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(type.name());
        ServerPlayNetworking.send(player, JUMPSCARE_PACKET, buf);
    }
}