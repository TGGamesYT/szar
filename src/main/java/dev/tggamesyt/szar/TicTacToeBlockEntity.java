package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class TicTacToeBlockEntity extends BlockEntity {

    // 0 = empty, 1 = O (player1), 2 = X (player2)
    public int[] board = new int[9];
    public UUID player1 = null; // O
    public UUID player2 = null; // X
    public int currentTurn = 1; // 1 = O's turn, 2 = X's turn
    public int winner = 0; // 0 = ongoing, 1 = O wins, 2 = X wins, 3 = draw
    public final java.util.Set<UUID> spectators = new java.util.HashSet<>();
    public int resetTimer = -1; // -1 = no reset pending
    public TicTacToeBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.TIC_TAC_TOE_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
                            TicTacToeBlockEntity entity) {
        if (!world.isClient && entity.resetTimer > 0) {
            entity.resetTimer--;
            if (entity.resetTimer == 0) {
                entity.resetTimer = -1;
                entity.resetGame(((net.minecraft.server.world.ServerWorld) world).getServer());
                entity.syncToPlayers(((net.minecraft.server.world.ServerWorld) world).getServer());
            }
        }
    }

    public void handlePlayerJoin(ServerPlayerEntity player, BlockPos pos) {
        UUID uuid = player.getUuid();

        // Check if already in a different game
        BlockPos activePos = Szar.tttActivePlayers.get(uuid);
        if (activePos != null && !activePos.equals(pos)) {
            player.sendMessage(Text.literal("§cYou are already in a game at another board!"), true);
            return;
        }

        // Rejoin existing game
        if (uuid.equals(player1) || uuid.equals(player2)) {
            openScreen(player);
            return;
        }

        if (player1 == null) {
            player1 = uuid;
            Szar.tttActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §bO§a! Waiting for second player..."), true);
            markDirty();
            return;
        }

        if (player2 == null && !uuid.equals(player1)) {
            player2 = uuid;
            Szar.tttActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §cX§a! Game starting!"), true);

            ServerPlayerEntity p1 = getServer(player).getPlayerManager().getPlayer(player1);
            if (p1 != null) {
                p1.sendMessage(Text.literal("§aSecond player joined! Your turn!"), true);
            }

            openScreenForBoth(player);
            markDirty();
            return;
        }

        // At the bottom where it says "game is full", replace with:
        if (player1 != null && player2 != null) {
            spectators.add(uuid);
            player.sendMessage(Text.literal("§7Spectating the match..."), true);
            openScreen(player);
            markDirty();
        }
    }

    private net.minecraft.server.MinecraftServer getServer(ServerPlayerEntity player) {
        return player.getServer();
    }

    public void openScreenForBoth(ServerPlayerEntity joiner) {
        openScreen(joiner);
        ServerPlayerEntity p1 = joiner.getServer().getPlayerManager().getPlayer(player1);
        if (p1 != null) openScreen(p1);
    }

    public void openScreen(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.pos);
        writeStateToBuf(buf, player.getUuid());
        ServerPlayNetworking.send(player, Szar.TTT_OPEN_SCREEN, buf);
    }

    public void handleMove(ServerPlayerEntity player, int cell) {
        if (winner != 0) return;
        if (cell < 0 || cell > 8) return;
        if (board[cell] != 0) return;

        UUID uuid = player.getUuid();
        int playerNum = uuid.equals(player1) ? 1 : uuid.equals(player2) ? 2 : 0;
        if (playerNum == 0) return;
        if (playerNum != currentTurn) {
            player.sendMessage(Text.literal("§cNot your turn!"), true);
            return;
        }

        board[cell] = playerNum;
        currentTurn = (currentTurn == 1) ? 2 : 1;
        checkWinner();
        markDirty();
        syncToPlayers(player.getServer());
    }

    private void checkWinner() {
        int[][] lines = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };

        for (int[] line : lines) {
            int a = board[line[0]], b = board[line[1]], c = board[line[2]];
            if (a != 0 && a == b && b == c) {
                winner = a;
                scheduleReset();
                return;
            }
        }

        boolean full = true;
        for (int cell : board) {
            if (cell == 0) { full = false; break; }
        }
        if (full) {
            winner = 3;
            scheduleReset();
        }
    }

    private void scheduleReset() {
        resetTimer = 60;
        markDirty();
    }

    public void syncToPlayers(net.minecraft.server.MinecraftServer server) {
        sendToPlayer(server, player1);
        sendToPlayer(server, player2);
        for (UUID uuid : spectators) {
            sendToPlayer(server, uuid);
        }
    }

    private void sendToPlayer(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.pos);
        writeStateToBuf(buf, uuid);
        ServerPlayNetworking.send(p, Szar.TTT_STATE_SYNC, buf);
    }

    public void writeStateToBuf(PacketByteBuf buf, UUID viewerUuid) {
        for (int cell : board) buf.writeInt(cell);
        buf.writeBoolean(player1 != null);
        if (player1 != null) buf.writeUuid(player1);
        buf.writeBoolean(player2 != null);
        if (player2 != null) buf.writeUuid(player2);
        buf.writeInt(currentTurn);
        buf.writeInt(winner);
        // Is the viewer a spectator?
        boolean isSpectator = viewerUuid != null
                && !viewerUuid.equals(player1)
                && !viewerUuid.equals(player2);
        buf.writeBoolean(isSpectator);
    }

    public static State readStateFromBuf(PacketByteBuf buf) {
        State s = new State();
        s.board = new int[9];
        for (int i = 0; i < 9; i++) s.board[i] = buf.readInt();
        if (buf.readBoolean()) s.player1 = buf.readUuid();
        if (buf.readBoolean()) s.player2 = buf.readUuid();
        s.currentTurn = buf.readInt();
        s.winner = buf.readInt();
        s.isSpectator = buf.readBoolean();
        return s;
    }

    public static class State {
        public int[] board;
        public UUID player1, player2;
        public int currentTurn, winner;
        public boolean isSpectator;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putIntArray("Board", board);
        if (player1 != null) nbt.putUuid("Player1", player1);
        if (player2 != null) nbt.putUuid("Player2", player2);
        nbt.putInt("Turn", currentTurn);
        nbt.putInt("Winner", winner);
        nbt.putInt("ResetTimer", resetTimer);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        int[] saved = nbt.getIntArray("Board");
        if (saved.length == 9) board = saved;
        if (nbt.containsUuid("Player1")) player1 = nbt.getUuid("Player1");
        if (nbt.containsUuid("Player2")) player2 = nbt.getUuid("Player2");
        currentTurn = nbt.getInt("Turn");
        winner = nbt.getInt("Winner");
        resetTimer = nbt.getInt("ResetTimer");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() { return createNbt(); }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public void resetGame(net.minecraft.server.MinecraftServer server) {
        closeScreenForAll(server); // kick everyone from screen first
        if (player1 != null) Szar.tttActivePlayers.remove(player1);
        if (player2 != null) Szar.tttActivePlayers.remove(player2);
        spectators.clear();
        board = new int[9];
        player1 = null;
        player2 = null;
        currentTurn = 1;
        winner = 0;
        resetTimer = -1;
        markDirty();
    }

    public void closeScreenForAll(net.minecraft.server.MinecraftServer server) {
        closeScreenForPlayer(server, player1);
        closeScreenForPlayer(server, player2);
        for (UUID uuid : spectators) {
            closeScreenForPlayer(server, uuid);
        }
    }

    private void closeScreenForPlayer(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p == null) return;
        ServerPlayNetworking.send(p, Szar.TTT_CLOSE_SCREEN, PacketByteBufs.empty());
    }
}