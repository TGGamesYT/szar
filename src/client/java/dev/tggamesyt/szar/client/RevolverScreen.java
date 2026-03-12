package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.RevolverItem;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class RevolverScreen extends Screen {

    private final ItemStack revolverStack;

    private static final int[][] SLOT_OFFSETS = {
            { 0, -50}, {43, -25}, {43,  25},
            { 0,  50}, {-43, 25}, {-43,-25}
    };

    public RevolverScreen(ItemStack stack) {
        super(Text.literal("Revolver"));
        this.revolverStack = stack;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        boolean[] chambers = RevolverItem.getChambers(revolverStack);

        for (int i = 0; i < RevolverItem.CHAMBERS; i++) {
            final int index = i;
            int bx = cx + SLOT_OFFSETS[i][0] - 15;
            int by = cy + SLOT_OFFSETS[i][1] - 10;

            this.addDrawableChild(ButtonWidget.builder(
                    getChamberText(index, chambers),
                    btn -> {
                        PlayerEntity player = MinecraftClient.getInstance().player;
                        if (player == null) return;

                        boolean[] current = RevolverItem.getChambers(revolverStack);

                        if (current[index]) {
                            // Optimistically update client visual
                            current[index] = false;
                            RevolverItem.setChambers(revolverStack, current);
                        } else {
                            // Check if player has bullet before sending — purely for visual feedback
                            boolean hasBullet = false;
                            for (int o = 0; o < player.getInventory().size(); o++) {
                                ItemStack s = player.getInventory().getStack(o);
                                if (!s.isEmpty() && s.isOf(Szar.BULLET_ITEM)) {
                                    hasBullet = true;
                                    break;
                                }
                            }
                            if (!hasBullet) return; // don't even send packet

                            current[index] = true;
                            RevolverItem.setChambers(revolverStack, current);
                        }

                        // Send to server — server does the actual inventory changes
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeInt(index);
                        buf.writeBoolean(!current[index]); // wasLoaded = what it WAS before the flip
                        ClientPlayNetworking.send(Szar.REVOLVER_CHAMBER_CHANGE, buf);

                        clearChildren();
                        init();
                    }
            ).dimensions(bx, by, 30, 20).build());
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                btn -> this.close()
        ).dimensions(cx - 40, cy + 75, 80, 20).build());
    }

    private boolean takeBullet(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(Szar.BULLET_ITEM)) {
                s.decrement(1);
                return true;
            }
        }
        return false;
    }

    private Text getChamberText(int i, boolean[] chambers) {
        int current = RevolverItem.getCurrentChamber(revolverStack);
        String prefix = (i == current) ? "►" : " ";
        return Text.literal(prefix + (chambers[i] ? "●" : "○"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Load Revolver"), this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("► = current chamber"), this.width / 2, this.height / 2 - 58, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void syncToServer() {
        boolean[] chambers = RevolverItem.getChambers(revolverStack);
        int current = RevolverItem.getCurrentChamber(revolverStack);

        PacketByteBuf buf = PacketByteBufs.create();
        for (int i = 0; i < RevolverItem.CHAMBERS; i++) {
            buf.writeBoolean(chambers[i]);
        }
        buf.writeInt(current);

        ClientPlayNetworking.send(Szar.REVOLVER_SYNC, buf);
    }
}