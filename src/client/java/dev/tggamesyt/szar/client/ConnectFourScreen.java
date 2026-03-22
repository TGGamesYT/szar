package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.ConnectFourBlockEntity;
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

import java.util.UUID;

public class ConnectFourScreen extends Screen {

    private static final Identifier BG =
            new Identifier("szar", "textures/gui/connectfour.png");
    private static final Identifier RED_TEX =
            new Identifier("szar", "textures/gui/c4_red.png");
    private static final Identifier YELLOW_TEX =
            new Identifier("szar", "textures/gui/c4_yellow.png");
    private static final Identifier HOVER_TEX =
            new Identifier("szar", "textures/gui/c4_hover.png");

    // GUI dimensions — adjust to match your texture
    private static final int GUI_WIDTH = 204;  // 7 cells * ~28px + padding
    private static final int GUI_HEIGHT = 196; // 6 cells * ~28px + padding + status
    private static final int BOARD_X = 18;
    private static final int BOARD_Y = 18;
    private static final int CELL_SIZE = 24;

    private ConnectFourBlockEntity.State state;
    private final BlockPos blockPos;
    private final UUID localPlayer;
    private boolean isSpectator;
    private int hoveredCol = -1;

    public ConnectFourScreen(BlockPos pos, ConnectFourBlockEntity.State state) {
        super(Text.literal("Connect Four"));
        this.blockPos = pos;
        this.state = state;
        this.localPlayer = MinecraftClient.getInstance().player.getUuid();
        this.isSpectator = state.isSpectator;
    }

    public void updateState(ConnectFourBlockEntity.State newState) {
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

        // Update hovered column
        hoveredCol = -1;
        for (int col = 0; col < ConnectFourBlockEntity.COLS; col++) {
            int cx = x + BOARD_X + col * CELL_SIZE;
            if (mouseX >= cx && mouseX < cx + CELL_SIZE) {
                hoveredCol = col;
                break;
            }
        }

        // Draw hover indicator on top row if it's our turn
        boolean myTurn = !isSpectator && state.winner == 0
                && ((state.currentTurn == 1 && localPlayer.equals(state.player1))
                || (state.currentTurn == 2 && localPlayer.equals(state.player2)));

        if (myTurn && hoveredCol >= 0) {
            int cx = x + BOARD_X + hoveredCol * CELL_SIZE;
            context.drawTexture(HOVER_TEX, cx + 2, y + BOARD_Y - CELL_SIZE + 2, 0, 0,
                    CELL_SIZE - 4, CELL_SIZE - 4, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        // Draw board — row 0 is bottom, so render rows in reverse
        for (int row = 0; row < ConnectFourBlockEntity.ROWS; row++) {
            for (int col = 0; col < ConnectFourBlockEntity.COLS; col++) {
                int cx = x + BOARD_X + col * CELL_SIZE;
                // Flip row so row 0 (bottom) renders at the bottom of the GUI
                int displayRow = ConnectFourBlockEntity.ROWS - 1 - row;
                int cy = y + BOARD_Y + displayRow * CELL_SIZE;

                int val = state.board[row][col];
                if (val == 1) {
                    context.drawTexture(RED_TEX, cx + 2, cy + 2, 0, 0,
                            CELL_SIZE - 4, CELL_SIZE - 4,
                            CELL_SIZE - 4, CELL_SIZE - 4);
                } else if (val == 2) {
                    context.drawTexture(YELLOW_TEX, cx + 2, cy + 2, 0, 0,
                            CELL_SIZE - 4, CELL_SIZE - 4,
                            CELL_SIZE - 4, CELL_SIZE - 4);
                }
            }
        }

        // Status text
        String status;
        if (state.winner == 1) status = "§cRed wins!";
        else if (state.winner == 2) status = "§eYellow wins!";
        else if (state.winner == 3) status = "§7Draw!";
        else if (isSpectator) status = "§7Spectating...";
        else status = myTurn ? "§aYour turn!" : "§7Opponent's turn...";

        context.drawTextWithShadow(this.textRenderer, Text.literal(status),
                x + GUI_WIDTH / 2 - this.textRenderer.getWidth(status) / 2,
                y + BOARD_Y + ConnectFourBlockEntity.ROWS * CELL_SIZE + 6,
                0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSpectator) return super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (state.winner != 0) return super.mouseClicked(mouseX, mouseY, button);

        boolean myTurn = (state.currentTurn == 1 && localPlayer.equals(state.player1))
                || (state.currentTurn == 2 && localPlayer.equals(state.player2));
        if (!myTurn) return super.mouseClicked(mouseX, mouseY, button);

        int x = (this.width - GUI_WIDTH) / 2;

        for (int col = 0; col < ConnectFourBlockEntity.COLS; col++) {
            int cx = x + BOARD_X + col * CELL_SIZE;
            if (mouseX >= cx && mouseX < cx + CELL_SIZE) {
                sendMove(col);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendMove(int col) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockPos);
        buf.writeInt(col);
        ClientPlayNetworking.send(Szar.C4_MAKE_MOVE, buf);
    }

    @Override
    public boolean shouldPause() { return false; }
}