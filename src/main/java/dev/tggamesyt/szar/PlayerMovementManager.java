package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class PlayerMovementManager {

    // Packet ID
    public static final Identifier PACKET_ID = new Identifier("szar", "player_movement");

    // Stores current key state per player
    private static final Map<ServerPlayerEntity, MovementState> playerStates = new HashMap<>();

    // Represents pressed keys
    public static class MovementState {
        public boolean forwardPressed = false;
        public boolean backwardPressed = false;
    }

    // Server-side init (register packet receiver)
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, responseSender) -> {
            boolean forward = buf.readBoolean();
            boolean backward = buf.readBoolean();

            server.execute(() -> {
                MovementState state = playerStates.computeIfAbsent(player, k -> new MovementState());
                state.forwardPressed = forward;
                state.backwardPressed = backward;
            });
        });
    }

    // Helper to get player state
    public static boolean isForwardPressed(ServerPlayerEntity player) {
        return playerStates.getOrDefault(player, new MovementState()).forwardPressed;
    }

    public static boolean isBackwardPressed(ServerPlayerEntity player) {
        return playerStates.getOrDefault(player, new MovementState()).backwardPressed;
    }

    // Optional: clear state when player disconnects
    public static void removePlayer(ServerPlayerEntity player) {
        playerStates.remove(player);
    }
}
