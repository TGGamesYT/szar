package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class MerlQuestionScreen extends Screen {

    private TextFieldWidget textField;
    private final int entityId;

    protected MerlQuestionScreen(int entityId) {
        super(Text.literal("Ask Merl"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        textField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - 100,
                this.height / 2,
                200,
                20,
                Text.literal("Question")
        );

        this.addSelectableChild(textField);
        this.setInitialFocus(textField);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            sendAnswerPacket();
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void sendAnswerPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        buf.writeString(textField.getText());

        ClientPlayNetworking.send(Szar.MERL_QUESTION, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        textField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}

