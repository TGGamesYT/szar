package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class CoordInputScreen extends Screen {

    private TextFieldWidget xField;
    private TextFieldWidget zField;

    public CoordInputScreen() {
        super(Text.literal("Nuclear Strike Coordinates"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        xField = new TextFieldWidget(textRenderer, centerX - 60, centerY - 30, 120, 20, Text.literal("X"));
        xField.setPlaceholder(Text.literal("X coordinate"));
        xField.setMaxLength(10);
        addDrawableChild(xField);

        zField = new TextFieldWidget(textRenderer, centerX - 60, centerY, 120, 20, Text.literal("Z"));
        zField.setPlaceholder(Text.literal("Z coordinate"));
        zField.setMaxLength(10);
        addDrawableChild(zField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Launch"), button -> submit())
                .dimensions(centerX - 40, centerY + 30, 80, 20)
                .build());
    }

    private void submit() {
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int z = Integer.parseInt(zField.getText().trim());

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(x);
            buf.writeInt(z);
            ClientPlayNetworking.send(Szar.DETONATOR_INPUT, buf);

            this.close();
        } catch (NumberFormatException e) {
            // flash the fields red
            xField.setEditableColor(0xFF5555);
            zField.setEditableColor(0xFF5555);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("X:"), this.width / 2 - 75, this.height / 2 - 27, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal("Z:"), this.width / 2 - 75, this.height / 2 + 3, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}