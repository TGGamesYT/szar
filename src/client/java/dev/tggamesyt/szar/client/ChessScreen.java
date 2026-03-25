package dev.tggamesyt.szar.client;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import dev.tggamesyt.szar.ChessBlockEntity;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class ChessScreen extends Screen {

    // Piece textures — one per piece type
    // Naming: chess_wp.png (white pawn), chess_bn.png (black knight) etc.
    private static final Map<Piece, Identifier> PIECE_TEXTURES = new HashMap<>();

    static {
        String[] colors = {"w", "b"};
        String[] types = {"p", "n", "b", "r", "q", "k"};
        Piece[] whitePieces = {Piece.WHITE_PAWN, Piece.WHITE_KNIGHT, Piece.WHITE_BISHOP,
                Piece.WHITE_ROOK, Piece.WHITE_QUEEN, Piece.WHITE_KING};
        Piece[] blackPieces = {Piece.BLACK_PAWN, Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP,
                Piece.BLACK_ROOK, Piece.BLACK_QUEEN, Piece.BLACK_KING};

        for (int i = 0; i < 6; i++) {
            PIECE_TEXTURES.put(whitePieces[i],
                    new Identifier("szar", "textures/gui/chess_w" + types[i] + ".png"));
            PIECE_TEXTURES.put(blackPieces[i],
                    new Identifier("szar", "textures/gui/chess_b" + types[i] + ".png"));
        }
    }

    private static final int CELL = 24;       // pixels per square
    private static final int BOARD_PIXELS = CELL * 8; // 192
    private static final int GUI_WIDTH = BOARD_PIXELS + 16;
    private static final int GUI_HEIGHT = BOARD_PIXELS + 32;
    private static final int BOARD_OFFSET_X = 8;
    private static final int BOARD_OFFSET_Y = 8;

    // Colors
    private static final int LIGHT_SQUARE = 0xFFF0D9B5;
    private static final int DARK_SQUARE  = 0xFFB58863;
    private static final int SELECTED     = 0xAA7FC97F;
    private static final int VALID_MOVE   = 0xAA7FC97F;
    private static final int LAST_MOVE    = 0xAA99CC66;

    private ChessBlockEntity.State state;
    private final BlockPos blockPos;
    private final UUID localPlayer;
    private boolean isSpectator;

    private Square selectedSquare = null;
    private Set<Square> validMoveTargets = new HashSet<>();

    public ChessScreen(BlockPos pos, ChessBlockEntity.State state) {
        super(Text.literal("Chess"));
        this.blockPos = pos;
        this.state = state;
        this.localPlayer = MinecraftClient.getInstance().player.getUuid();
        this.isSpectator = state.isSpectator;
    }

    public void updateState(ChessBlockEntity.State newState) {
        this.state = newState;
        this.isSpectator = newState.isSpectator;
        // Clear selection on state update
        selectedSquare = null;
        validMoveTargets.clear();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int bx = (this.width - GUI_WIDTH) / 2 + BOARD_OFFSET_X;
        int by = (this.height - GUI_HEIGHT) / 2 + BOARD_OFFSET_Y;

        Board board = new Board();
        board.loadFromFen(state.fen);

        // Draw squares
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                int drawFile = state.isWhite ? file : (7 - file);
                int drawRank = state.isWhite ? (7 - rank) : rank;

                int sx = bx + drawFile * CELL;
                int sy = by + drawRank * CELL;

                boolean light = (file + rank) % 2 == 0;
                int squareColor = light ? LIGHT_SQUARE : DARK_SQUARE;

                // Check if selected or valid move target
                Square sq = getSquare(file, rank);
                if (sq != null && sq.equals(selectedSquare)) {
                    squareColor = SELECTED;
                } else if (sq != null && validMoveTargets.contains(sq)) {
                    squareColor = VALID_MOVE;
                }

                context.fill(sx, sy, sx + CELL, sy + CELL, squareColor);
            }
        }

        // Draw pieces
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Square sq = getSquare(file, rank);
                if (sq == null) continue;

                Piece piece = board.getPiece(sq);
                if (piece == null || piece == Piece.NONE) continue;

                Identifier tex = PIECE_TEXTURES.get(piece);
                if (tex == null) continue;

                int drawFile = state.isWhite ? file : (7 - file);
                int drawRank = state.isWhite ? (7 - rank) : rank;

                int sx = bx + drawFile * CELL + 1;
                int sy = by + drawRank * CELL + 1;

                context.drawTexture(tex, sx, sy, 0, 0,
                        CELL - 2, CELL - 2, CELL - 2, CELL - 2);
            }
        }

        // Draw rank/file labels
        for (int i = 0; i < 8; i++) {
            String fileLabel = String.valueOf((char)('a' + (state.isWhite ? i : 7 - i)));
            String rankLabel = String.valueOf(state.isWhite ? 8 - i : i + 1);
            context.drawText(this.textRenderer, fileLabel,
                    bx + i * CELL + CELL / 2 - 3,
                    by + BOARD_PIXELS + 2, 0xFFFFFF, true);
            context.drawText(this.textRenderer, rankLabel,
                    bx - 7, by + i * CELL + CELL / 2 - 4,
                    0xFFFFFF, true);
        }

        // Status text
        String status;
        if (state.winner == 1) status = "§fWhite wins!";
        else if (state.winner == 2) status = "§8Black wins!";
        else if (state.winner == 3) status = "§7Draw! " + state.statusMessage;
        else if (!state.statusMessage.isEmpty()) status = "§e" + state.statusMessage;
        else if (isSpectator) status = "§7Spectating...";
        else {
            Side sideToMove = board.getSideToMove();
            boolean myTurn = (sideToMove == Side.WHITE && localPlayer.equals(state.player1))
                    || (sideToMove == Side.BLACK && localPlayer.equals(state.player2));
            status = myTurn ? "§aYour turn!" : "§7Opponent's turn...";
        }

        int statusX = (this.width - GUI_WIDTH) / 2 + GUI_WIDTH / 2
                - this.textRenderer.getWidth(status) / 2;
        int statusY = (this.height - GUI_HEIGHT) / 2 + BOARD_OFFSET_Y
                + BOARD_PIXELS + 16;
        context.drawTextWithShadow(this.textRenderer, Text.literal(status),
                statusX, statusY, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSpectator || button != 0 || state.winner != 0)
            return super.mouseClicked(mouseX, mouseY, button);

        Board board = new Board();
        board.loadFromFen(state.fen);

        Side sideToMove = board.getSideToMove();
        boolean myTurn = (sideToMove == Side.WHITE && localPlayer.equals(state.player1))
                || (sideToMove == Side.BLACK && localPlayer.equals(state.player2));
        if (!myTurn) return super.mouseClicked(mouseX, mouseY, button);

        int bx = (this.width - GUI_WIDTH) / 2 + BOARD_OFFSET_X;
        int by = (this.height - GUI_HEIGHT) / 2 + BOARD_OFFSET_Y;

        // Check if click is on the board
        if (mouseX < bx || mouseX >= bx + BOARD_PIXELS
                || mouseY < by || mouseY >= by + BOARD_PIXELS)
            return super.mouseClicked(mouseX, mouseY, button);

        int drawFile = (int)((mouseX - bx) / CELL);
        int drawRank = (int)((mouseY - by) / CELL);

        // Convert draw coords back to board coords
        int file = state.isWhite ? drawFile : (7 - drawFile);
        int rank = state.isWhite ? (7 - drawRank) : drawRank;

        Square clicked = getSquare(file, rank);
        if (clicked == null) return super.mouseClicked(mouseX, mouseY, button);

        if (selectedSquare == null) {
            // Select a piece
            Piece piece = board.getPiece(clicked);
            if (piece != Piece.NONE) {
                boolean ownPiece = (sideToMove == Side.WHITE && piece.getPieceSide() == Side.WHITE)
                        || (sideToMove == Side.BLACK && piece.getPieceSide() == Side.BLACK);
                if (ownPiece) {
                    selectedSquare = clicked;
                    // Calculate valid moves for this piece
                    validMoveTargets.clear();
                    for (Move move : board.legalMoves()) {
                        if (move.getFrom().equals(clicked)) {
                            validMoveTargets.add(move.getTo());
                        }
                    }
                }
            }
        } else {
            if (clicked.equals(selectedSquare)) {
                // Deselect
                selectedSquare = null;
                validMoveTargets.clear();
            } else if (validMoveTargets.contains(clicked)) {
                // Make the move
                String uci = selectedSquare.value().toLowerCase()
                        + clicked.value().toLowerCase();

                // Auto-promote to queen if pawn reaches last rank
                Piece moving = board.getPiece(selectedSquare);
                if (moving == Piece.WHITE_PAWN && clicked.getRank().ordinal() == 7) {
                    uci += "q";
                } else if (moving == Piece.BLACK_PAWN && clicked.getRank().ordinal() == 0) {
                    uci += "q";
                }

                sendMove(uci);
                selectedSquare = null;
                validMoveTargets.clear();
            } else {
                // Try selecting a different piece
                Piece piece = board.getPiece(clicked);
                if (piece != Piece.NONE) {
                    boolean ownPiece = (sideToMove == Side.WHITE && piece.getPieceSide() == Side.WHITE)
                            || (sideToMove == Side.BLACK && piece.getPieceSide() == Side.BLACK);
                    if (ownPiece) {
                        selectedSquare = clicked;
                        validMoveTargets.clear();
                        for (Move move : board.legalMoves()) {
                            if (move.getFrom().equals(clicked)) {
                                validMoveTargets.add(move.getTo());
                            }
                        }
                    } else {
                        selectedSquare = null;
                        validMoveTargets.clear();
                    }
                } else {
                    selectedSquare = null;
                    validMoveTargets.clear();
                }
            }
        }

        return true;
    }

    private Square getSquare(int file, int rank) {
        try {
            String name = String.valueOf((char)('A' + file)) + (rank + 1);
            return Square.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendMove(String uci) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockPos);
        buf.writeString(uci);
        ClientPlayNetworking.send(Szar.CHESS_MAKE_MOVE, buf);
    }

    @Override
    public boolean shouldPause() { return false; }
}