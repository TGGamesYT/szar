package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.SlotMachineBlockEntity;
import dev.tggamesyt.szar.SlotMachineScreenHandler;
import dev.tggamesyt.szar.Szar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.tggamesyt.szar.SlotSymbol;

public class SlotMachineScreen extends HandledScreen<SlotMachineScreenHandler> {

    private static final Identifier BG_TEXTURE =
            new Identifier(Szar.MOD_ID, "textures/gui/slot_machine.png");

    private static final Identifier HANDLE_1 =
            new Identifier(Szar.MOD_ID, "textures/gui/handle1.png");
    private static final Identifier HANDLE_2 =
            new Identifier(Szar.MOD_ID, "textures/gui/handle2.png");
    private static final Identifier HANDLE_3 =
            new Identifier(Szar.MOD_ID, "textures/gui/handle3.png");

    private final int handleX = 120;
    private final int handleY = 20;

    private boolean handleClicked = false;
    private int handleAnimTicks = 0;
    private Identifier currentHandleTexture = HANDLE_1;

    public SlotMachineScreen(SlotMachineScreenHandler handler,
                             PlayerInventory inventory,
                             Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    // ----------------------------
    // BACKGROUND
    // ----------------------------

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        context.drawTexture(BG_TEXTURE, guiLeft, guiTop,
                0, 0, backgroundWidth, backgroundHeight);

        drawReels(context, guiLeft, guiTop);
    }

    private void drawReels(DrawContext context, int guiLeft, int guiTop) {

        SlotMachineBlockEntity be = handler.blockEntity;
        if (be == null) return;

        int reelX = guiLeft + 70;
        int reelY = guiTop + 35;

        for (int i = 0; i < 3; i++) {

            int idx = be.getSymbol(i);
            if (idx < 0 || idx >= SlotSymbol.values().length)
                idx = 0;

            SlotSymbol symbol = SlotSymbol.values()[idx];
            ItemStack stack = new ItemStack(symbol.item);

            context.drawItem(stack, reelX + i * 18, reelY);
        }
    }

    // ----------------------------
    // RENDER
    // ----------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        if (client != null && client.player != null) {
            handler.tick(client.player);
        }
        drawMouseoverTooltip(context, mouseX, mouseY);

        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        // Handle animation
        if (handleClicked) {
            handleAnimTicks++;

            if (handleAnimTicks < 5) {
                currentHandleTexture = HANDLE_2;
            } else if (handleAnimTicks < 10) {
                currentHandleTexture = HANDLE_3;
            } else {
                currentHandleTexture = HANDLE_1;
                handleClicked = false;
                handleAnimTicks = 0;

                // CALL SCREEN HANDLER LOGIC HERE
                if (client != null && client.player != null && client.interactionManager != null) {
                        client.interactionManager.clickButton(handler.syncId, 0);
                }
            }
        }

        // Draw handle
        context.drawTexture(currentHandleTexture,
                guiLeft + handleX,
                guiTop + handleY,
                0, 0,
                32, 32,
                32, 32);
    }

    // ----------------------------
    // MOUSE
    // ----------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        double relX = mouseX - (guiLeft + handleX);
        double relY = mouseY - (guiTop + handleY);

        if (relX >= 0 && relX <= 32 && relY >= 0 && relY <= 32) {
            handleClicked = true;
            handleAnimTicks = 0;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}