package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.TicTacToeBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class TicTacToeScreen extends Screen {

    private static final Identifier BG = new Identifier("szar", "textures/gui/tictactoe.png");
    private static final Identifier X_TEX = new Identifier("szar", "textures/gui/x.png");
    private static final Identifier O_TEX = new Identifier("szar", "textures/gui/o.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int BOARD_X = 16; // offset inside GUI
    private static final int BOARD_Y = 16;
    private static final int CELL_SIZE = 44;

    private TicTacToeBlockEntity.State state;
    private final BlockPos blockPos;
    private final UUID localPlayer;

    // Add field
    private boolean isSpectator;

    // Update constructor
    public TicTacToeScreen(BlockPos pos, TicTacToeBlockEntity.State state) {
        super(Text.literal("Tic Tac Toe"));
        this.blockPos = pos;
        this.state = state;
        this.localPlayer = MinecraftClient.getInstance().player.getUuid();
        this.isSpectator = state.isSpectator;
    }

    // Update updateState
    public void updateState(TicTacToeBlockEntity.State newState) {
        this.state = newState;
        this.isSpectator = newState.isSpectator;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // Draw background
        context.drawTexture(BG, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        // Draw board cells
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int cx = x + BOARD_X + col * CELL_SIZE;
            int cy = y + BOARD_Y + row * CELL_SIZE;

            if (state.board[i] == 1) {
                // O
                context.drawTexture(O_TEX, cx + 4, cy + 4, 0, 0,
                        CELL_SIZE - 8, CELL_SIZE - 8,
                        CELL_SIZE - 8, CELL_SIZE - 8);
            } else if (state.board[i] == 2) {
                // X
                context.drawTexture(X_TEX, cx + 4, cy + 4, 0, 0,
                        CELL_SIZE - 8, CELL_SIZE - 8,
                        CELL_SIZE - 8, CELL_SIZE - 8);
            }
        }

        // Status text
        String status;
        if (state.winner == 1) status = "§bO wins!";
        else if (state.winner == 2) status = "§cX wins!";
        else if (state.winner == 3) status = "§eDraw!";
        else if (isSpectator) status = "§7Spectating...";
        else {
            boolean myTurn = (state.currentTurn == 1 && localPlayer.equals(state.player1))
                    || (state.currentTurn == 2 && localPlayer.equals(state.player2));
            status = myTurn ? "§aYour turn!" : "§7Opponent's turn...";
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(status),
                x + GUI_WIDTH / 2 - this.textRenderer.getWidth(status) / 2,
                y + BOARD_Y + 3 * CELL_SIZE + 8, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSpectator) return super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (state.winner != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Check if it's our turn
        boolean myTurn = (state.currentTurn == 1 && localPlayer.equals(state.player1))
                || (state.currentTurn == 2 && localPlayer.equals(state.player2));
        if (!myTurn) return super.mouseClicked(mouseX, mouseY, button);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int cx = x + BOARD_X + col * CELL_SIZE;
            int cy = y + BOARD_Y + row * CELL_SIZE;

            if (mouseX >= cx && mouseX < cx + CELL_SIZE
                    && mouseY >= cy && mouseY < cy + CELL_SIZE) {
                if (state.board[i] == 0) {
                    sendMove(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendMove(int cell) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockPos);
        buf.writeInt(cell);
        ClientPlayNetworking.send(Szar.TTT_MAKE_MOVE, buf);
    }

    @Override
    public boolean shouldPause() { return false; }
}
