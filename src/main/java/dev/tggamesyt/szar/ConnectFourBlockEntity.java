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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ConnectFourBlockEntity extends BlockEntity {

    public static final int COLS = 7;
    public static final int ROWS = 6;

    // board[row][col], 0=empty, 1=player1(red), 2=player2(yellow)
    // row 0 = bottom
    public int[][] board = new int[ROWS][COLS];
    public UUID player1 = null; // red
    public UUID player2 = null; // yellow
    public int currentTurn = 1;
    public int winner = 0; // 0=ongoing, 1=p1, 2=p2, 3=draw
    public Set<UUID> spectators = new HashSet<>();
    public int resetTimer = -1;

    public ConnectFourBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.CONNECT_FOUR_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
                            ConnectFourBlockEntity entity) {
        if (!world.isClient && entity.resetTimer > 0) {
            entity.resetTimer--;
            if (entity.resetTimer == 0) {
                entity.resetTimer = -1;
                var server = ((net.minecraft.server.world.ServerWorld) world).getServer();
                entity.resetGame(server);
                entity.syncToPlayers(server);
            }
        }
    }

    public void handlePlayerJoin(ServerPlayerEntity player, BlockPos pos) {
        UUID uuid = player.getUuid();

        BlockPos activePos = Szar.c4ActivePlayers.get(uuid);
        if (activePos != null && !activePos.equals(pos)) {
            player.sendMessage(Text.literal("§cYou are already in a game at another board!"), true);
            return;
        }

        // Rejoin existing game
        if (uuid.equals(player1) || uuid.equals(player2)) {
            if (player1 != null && player2 != null) {
                // Game in progress — reopen screen
                openScreen(player);
            } else {
                // Still waiting for second player — leave
                if (uuid.equals(player1)) {
                    Szar.c4ActivePlayers.remove(player1);
                    player1 = null;
                } else {
                    Szar.c4ActivePlayers.remove(player2);
                    player2 = null;
                }
                player.sendMessage(Text.literal("§7Left the game."), true);
                markDirty();
            }
            return;
        }

        if (player1 == null) {
            player1 = uuid;
            Szar.c4ActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §cRed§a! Waiting for second player..."), true);
            markDirty();
            return;
        }

        if (player2 == null && !uuid.equals(player1)) {
            player2 = uuid;
            Szar.c4ActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §eYellow§a! Game starting!"), true);
            ServerPlayerEntity p1 = player.getServer().getPlayerManager().getPlayer(player1);
            if (p1 != null) p1.sendMessage(Text.literal("§aSecond player joined! Your turn!"), true);
            openScreenForBoth(player);
            markDirty();
            return;
        }

        if (player1 != null && player2 != null) {
            spectators.add(uuid);
            player.sendMessage(Text.literal("§7Spectating the match..."), true);
            openScreen(player);
            markDirty();
        }
    }

    public void handleMove(ServerPlayerEntity player, int col) {
        if (winner != 0) return;
        if (col < 0 || col >= COLS) return;

        UUID uuid = player.getUuid();
        int playerNum = uuid.equals(player1) ? 1 : uuid.equals(player2) ? 2 : 0;
        if (playerNum == 0) return;
        if (playerNum != currentTurn) {
            player.sendMessage(Text.literal("§cNot your turn!"), true);
            return;
        }

        // Find lowest empty row in this column (gravity)
        int targetRow = -1;
        for (int row = 0; row < ROWS; row++) {
            if (board[row][col] == 0) {
                targetRow = row;
                break;
            }
        }
        if (targetRow == -1) {
            player.sendMessage(Text.literal("§cColumn is full!"), true);
            return;
        }

        board[targetRow][col] = playerNum;
        currentTurn = (currentTurn == 1) ? 2 : 1;
        checkWinner();
        markDirty();
        syncToPlayers(player.getServer());
    }

    private void checkWinner() {
        // Check all 4-in-a-row directions
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int val = board[row][col];
                if (val == 0) continue;
                for (int[] dir : directions) {
                    int count = 1;
                    for (int i = 1; i < 4; i++) {
                        int r = row + dir[0] * i;
                        int c = col + dir[1] * i;
                        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) break;
                        if (board[r][c] != val) break;
                        count++;
                    }
                    if (count == 4) {
                        winner = val;
                        resetTimer = 100; // 5 seconds
                        markDirty();
                        return;
                    }
                }
            }
        }

        // Check draw — top row full
        boolean full = true;
        for (int col = 0; col < COLS; col++) {
            if (board[ROWS - 1][col] == 0) { full = false; break; }
        }
        if (full) {
            winner = 3;
            resetTimer = 100;
            markDirty();
        }
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
        ServerPlayNetworking.send(player, Szar.C4_OPEN_SCREEN, buf);
    }

    public void syncToPlayers(net.minecraft.server.MinecraftServer server) {
        sendToPlayer(server, player1);
        sendToPlayer(server, player2);
        for (UUID uuid : spectators) sendToPlayer(server, uuid);
    }

    private void sendToPlayer(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.pos);
        writeStateToBuf(buf, uuid);
        ServerPlayNetworking.send(p, Szar.C4_STATE_SYNC, buf);
    }

    public void writeStateToBuf(PacketByteBuf buf, UUID viewerUuid) {
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                buf.writeInt(board[row][col]);
        buf.writeBoolean(player1 != null);
        if (player1 != null) buf.writeUuid(player1);
        buf.writeBoolean(player2 != null);
        if (player2 != null) buf.writeUuid(player2);
        buf.writeInt(currentTurn);
        buf.writeInt(winner);
        boolean isSpectator = viewerUuid != null
                && !viewerUuid.equals(player1)
                && !viewerUuid.equals(player2);
        buf.writeBoolean(isSpectator);
    }

    public static State readStateFromBuf(PacketByteBuf buf) {
        State s = new State();
        s.board = new int[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                s.board[row][col] = buf.readInt();
        if (buf.readBoolean()) s.player1 = buf.readUuid();
        if (buf.readBoolean()) s.player2 = buf.readUuid();
        s.currentTurn = buf.readInt();
        s.winner = buf.readInt();
        s.isSpectator = buf.readBoolean();
        return s;
    }

    public static class State {
        public int[][] board;
        public UUID player1, player2;
        public int currentTurn, winner;
        public boolean isSpectator;
    }

    public void resetGame(net.minecraft.server.MinecraftServer server) {
        closeScreenForAll(server);
        if (player1 != null) Szar.c4ActivePlayers.remove(player1);
        if (player2 != null) Szar.c4ActivePlayers.remove(player2);
        spectators.clear();
        board = new int[ROWS][COLS];
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
        for (UUID uuid : spectators) closeScreenForPlayer(server, uuid);
    }

    private void closeScreenForPlayer(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (uuid == null) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p == null) return;
        ServerPlayNetworking.send(p, Szar.C4_CLOSE_SCREEN, PacketByteBufs.empty());
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        int[] flat = new int[ROWS * COLS];
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                flat[r * COLS + c] = board[r][c];
        nbt.putIntArray("Board", flat);
        if (player1 != null) nbt.putUuid("Player1", player1);
        if (player2 != null) nbt.putUuid("Player2", player2);
        nbt.putInt("Turn", currentTurn);
        nbt.putInt("Winner", winner);
        nbt.putInt("ResetTimer", resetTimer);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        int[] flat = nbt.getIntArray("Board");
        if (flat.length == ROWS * COLS) {
            for (int r = 0; r < ROWS; r++)
                for (int c = 0; c < COLS; c++)
                    board[r][c] = flat[r * COLS + c];
        }
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
}