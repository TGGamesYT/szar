package dev.tggamesyt.szar;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.Square;
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

public class ChessBlockEntity extends BlockEntity {
    public String DEFAULT_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    // FEN string stores full board state — chesslib handles all logic
    public String fen = DEFAULT_POSITION;
    public UUID player1 = null; // white
    public UUID player2 = null; // black
    public int winner = 0; // 0=ongoing, 1=white, 2=black, 3=draw
    public String statusMessage = "";
    public Set<UUID> spectators = new HashSet<>();
    public int resetTimer = -1;

    public ChessBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.CHESS_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
                            ChessBlockEntity entity) {
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

        BlockPos activePos = Szar.chessActivePlayers.get(uuid);
        if (activePos != null && !activePos.equals(pos)) {
            player.sendMessage(Text.literal("§cYou are already in a chess game elsewhere!"), true);
            return;
        }

        if (uuid.equals(player1) || uuid.equals(player2)) {
            if (player1 != null && player2 != null) {
                openScreen(player);
            } else {
                // Leave if waiting
                if (uuid.equals(player1)) {
                    Szar.chessActivePlayers.remove(player1);
                    player1 = null;
                } else {
                    Szar.chessActivePlayers.remove(player2);
                    player2 = null;
                }
                player.sendMessage(Text.literal("§7Left the game."), true);
                markDirty();
            }
            return;
        }

        if (player1 == null) {
            player1 = uuid;
            Szar.chessActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §fWhite§a! Waiting for second player..."), true);
            markDirty();
            return;
        }

        if (player2 == null && !uuid.equals(player1)) {
            player2 = uuid;
            Szar.chessActivePlayers.put(uuid, pos);
            player.sendMessage(Text.literal("§aYou are §8Black§a! Game starting!"), true);
            ServerPlayerEntity p1 = player.getServer().getPlayerManager().getPlayer(player1);
            if (p1 != null) p1.sendMessage(Text.literal("§aSecond player joined! Your turn (White)!"), true);
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

    public void handleMove(ServerPlayerEntity player, String uciMove) {
        if (winner != 0) return;

        UUID uuid = player.getUuid();
        Board board = new Board();
        board.loadFromFen(fen);

        // Check it's the right player's turn
        Side sideToMove = board.getSideToMove();
        boolean isWhite = uuid.equals(player1);
        boolean isBlack = uuid.equals(player2);
        if (!isWhite && !isBlack) return;
        if (sideToMove == Side.WHITE && !isWhite) {
            player.sendMessage(Text.literal("§cNot your turn!"), true);
            return;
        }
        if (sideToMove == Side.BLACK && !isBlack) {
            player.sendMessage(Text.literal("§cNot your turn!"), true);
            return;
        }

        // Validate and apply move
        try {
            Square from = Square.valueOf(uciMove.substring(0, 2).toUpperCase());
            Square to = Square.valueOf(uciMove.substring(2, 4).toUpperCase());

            // Handle promotion — default to queen
            String promotion = uciMove.length() > 4 ? uciMove.substring(4) : "";
            Move move;
            if (!promotion.isEmpty()) {
                com.github.bhlangonijr.chesslib.Piece promoPiece =
                        sideToMove == Side.WHITE
                                ? com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN
                                : com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN;
                move = new Move(from, to, promoPiece);
            } else {
                move = new Move(from, to);
            }

            // Check move is legal
            if (!board.legalMoves().contains(move)) {
                player.sendMessage(Text.literal("§cIllegal move!"), true);
                return;
            }

            board.doMove(move);
            fen = board.getFen();

            // Check game end conditions
            if (board.isMated()) {
                winner = sideToMove == Side.WHITE ? 1 : 2;
                statusMessage = (sideToMove == Side.WHITE ? "White" : "Black") + " wins by checkmate!";
                resetTimer = 100;
            } else if (board.isStaleMate()) {
                winner = 3;
                statusMessage = "Draw by stalemate!";
                resetTimer = 100;
            } else if (board.isInsufficientMaterial()) {
                winner = 3;
                statusMessage = "Draw by insufficient material!";
                resetTimer = 100;
            } else if (board.isRepetition()) {
                winner = 3;
                statusMessage = "Draw by repetition!";
                resetTimer = 100;
            } else if (board.isKingAttacked()) {
                statusMessage = "Check!";
            } else {
                statusMessage = "";
            }

            markDirty();
            syncToPlayers(player.getServer());

        } catch (Exception e) {
            player.sendMessage(Text.literal("§cInvalid move format!"), true);
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
        ServerPlayNetworking.send(player, Szar.CHESS_OPEN_SCREEN, buf);
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
        ServerPlayNetworking.send(p, Szar.CHESS_STATE_SYNC, buf);
    }

    public void writeStateToBuf(PacketByteBuf buf, UUID viewerUuid) {
        buf.writeString(fen);
        buf.writeBoolean(player1 != null);
        if (player1 != null) buf.writeUuid(player1);
        buf.writeBoolean(player2 != null);
        if (player2 != null) buf.writeUuid(player2);
        buf.writeInt(winner);
        buf.writeString(statusMessage);
        boolean isSpectator = viewerUuid != null
                && !viewerUuid.equals(player1)
                && !viewerUuid.equals(player2);
        buf.writeBoolean(isSpectator);
        // Is this viewer playing white or black
        boolean isWhite = viewerUuid != null && viewerUuid.equals(player1);
        buf.writeBoolean(isWhite);
    }

    public static State readStateFromBuf(PacketByteBuf buf) {
        State s = new State();
        s.fen = buf.readString();
        if (buf.readBoolean()) s.player1 = buf.readUuid();
        if (buf.readBoolean()) s.player2 = buf.readUuid();
        s.winner = buf.readInt();
        s.statusMessage = buf.readString();
        s.isSpectator = buf.readBoolean();
        s.isWhite = buf.readBoolean();
        return s;
    }

    public static class State {
        public String fen;
        public UUID player1, player2;
        public int winner;
        public String statusMessage;
        public boolean isSpectator;
        public boolean isWhite; // true = viewing from white's perspective
    }

    public void resetGame(net.minecraft.server.MinecraftServer server) {
        closeScreenForAll(server);
        if (player1 != null) Szar.chessActivePlayers.remove(player1);
        if (player2 != null) Szar.chessActivePlayers.remove(player2);
        spectators.clear();
        fen = DEFAULT_POSITION;
        player1 = null;
        player2 = null;
        winner = 0;
        statusMessage = "";
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
        ServerPlayNetworking.send(p, Szar.CHESS_CLOSE_SCREEN, PacketByteBufs.empty());
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Fen", fen);
        if (player1 != null) nbt.putUuid("Player1", player1);
        if (player2 != null) nbt.putUuid("Player2", player2);
        nbt.putInt("Winner", winner);
        nbt.putString("Status", statusMessage);
        nbt.putInt("ResetTimer", resetTimer);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        fen = nbt.getString("Fen");
        if (fen.isEmpty()) fen = DEFAULT_POSITION;
        if (nbt.containsUuid("Player1")) player1 = nbt.getUuid("Player1");
        if (nbt.containsUuid("Player2")) player2 = nbt.getUuid("Player2");
        winner = nbt.getInt("Winner");
        statusMessage = nbt.getString("Status");
        resetTimer = nbt.getInt("ResetTimer");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() { return createNbt(); }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}