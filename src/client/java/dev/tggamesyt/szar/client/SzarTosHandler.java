package dev.tggamesyt.szar.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import static dev.tggamesyt.szar.client.UraniumUtils.updateUranium;

public class SzarTosHandler {

    private static final Gson GSON = new Gson();

    private static final File CONFIG_FILE =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "szar_tos.json");

    // ============================
    // FULL TOS TEXT
    // ============================
    private static final Text MOD_TOS_TEXT = Text.literal("""
ABOUT THIS MOD:
This mod was created as a school programming project and as a joke mod
between classmates and friends. Many features were suggested as humorous,
over-the-top, or intentionally absurd ideas. The content is NOT meant to be
taken seriously. It is a fictional, parody-style Minecraft modification.

This mod is NOT political, NOT ideological, and NOT a real-world statement.
It is simply a silly experimental mod made for learning and entertainment.

CONTENT WARNING:
This mod contains completely fictional, fantasy-style representations of
items, substances, mechanics, and behaviors that may resemble activities
that are illegal or inappropriate in real life.

All such content exists ONLY within Minecraft as game mechanics.
Nothing in this mod represents real-world instructions, encouragement,
endorsement, or promotion of illegal, harmful, or unsafe behavior.

The developer DOES NOT support, promote, encourage, or condone:
- Real-world illegal activities
- Substance abuse
- Criminal behavior
- Harmful or unsafe conduct
- Offensive or discriminatory beliefs

AGE CONFIRMATION:
- This mod is intended for users 18 years or older.
- By continuing, you confirm that you meet this age requirement.

USER RESPONSIBILITY:
- You are voluntarily choosing to use this mod.
- You accept full responsibility for its use.
- You agree not to redistribute this mod publicly without permission.

TECHNICAL DISCLAIMER:
- This mod is provided "AS IS" without warranties.
- The developer is not responsible for crashes, data loss, or issues.

LEGAL DISCLAIMER:
- All content is fictional.
- The developer shall not be held liable for interpretation or misuse.

ACCEPTANCE:
By clicking "Agree", you accept all terms listed above.
If you do not agree, click "Decline" and close the game.
""");

    // ============================
    // PUBLIC ENTRY METHOD
    // ============================

    public static void checkAndShow() {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (!isAccepted()) {
                client.setScreen(new TosScreen());
            }
        });
    }

    // ============================
    // CONFIG HANDLING
    // ============================

    private static boolean isAccepted() {
        if (!CONFIG_FILE.exists()) return false;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            return obj.has("tosAccepted") && obj.get("tosAccepted").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static void sendDataIfAllowed() {
        if (!CONFIG_FILE.exists()) return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if(obj.has("allowDiagnostics") && obj.get("allowDiagnostics").getAsBoolean()) {
                updateUranium();
            };
        } catch (Exception e) {
            System.out.println("Error occurred while trying to read TOS config json: " + e);
        }
    }

    private static void save(boolean diagnosticsEnabled) {
        try {
            CONFIG_FILE.getParentFile().mkdirs();

            JsonObject obj = new JsonObject();
            obj.addProperty("tosAccepted", true);
            obj.addProperty("allowDiagnostics", diagnosticsEnabled);

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception ignored) {}
    }

    // ============================
    // CUSTOM SCREEN
    // ============================

    private static class TosScreen extends Screen {

        private CheckboxWidget diagnosticsCheckbox;

        private int scrollOffset = 0;
        private int maxScroll = 0;

        private static final int PADDING = 20;
        private static final int TITLE_HEIGHT = 30;
        private static final int FOOTER_HEIGHT = 60;

        private String[] lines;

        protected TosScreen() {
            super(Text.literal("Szar Mod - Information and Terms of Service"));
        }

        @Override
        protected void init() {

            lines = MOD_TOS_TEXT.getString().split("\n");

            int textHeight = lines.length * 12;
            int checkboxHeight = 24;

            int visibleHeight = this.height - TITLE_HEIGHT - FOOTER_HEIGHT - PADDING;

            maxScroll = Math.max(0, (textHeight + checkboxHeight + 20) - visibleHeight);

            int centerX = this.width / 2;

            diagnosticsCheckbox = new CheckboxWidget(
                    centerX - 150,
                    0, // will be repositioned every frame
                    300,
                    20,
                    Text.literal("Allow anonymous diagnostic & statistic data"),
                    true
            );

            this.addDrawableChild(diagnosticsCheckbox);

            // Agree button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Agree"),
                    button -> {
                        save(diagnosticsCheckbox.isChecked());
                        sendDataIfAllowed();
                        MinecraftClient.getInstance().setScreen(null);
                    }
            ).dimensions(centerX - 155, this.height - 40, 150, 20).build());

            // Decline button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Decline"),
                    button -> MinecraftClient.getInstance().stop()
            ).dimensions(centerX + 5, this.height - 40, 150, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {

            scrollOffset -= amount * 15;

            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;

            return true;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {

            this.renderBackground(context);

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.title,
                    this.width / 2,
                    10,
                    0xFFFFFF
            );

            int boxTop = TITLE_HEIGHT;
            int boxBottom = this.height - FOOTER_HEIGHT;
            int boxLeft = PADDING;
            int boxRight = this.width - PADDING;

            context.fill(boxLeft, boxTop, boxRight, boxBottom, 0x88000000);

            context.enableScissor(boxLeft, boxTop, boxRight, boxBottom);

            int y = boxTop + 10 - scrollOffset;

            for (String line : lines) {
                context.drawTextWithShadow(
                        this.textRenderer,
                        line,
                        boxLeft + 10,
                        y,
                        0xDDDDDD
                );
                y += 12;
            }

            context.disableScissor();

            // Real checkbox position (true scroll position)
            int checkboxY = y + 10;
            int checkboxX = (this.width / 2) - 150;

            diagnosticsCheckbox.setPosition(checkboxX, checkboxY);

            // Determine if checkbox is inside visible scroll region
            boolean insideVisibleArea =
                    checkboxY >= boxTop &&
                            checkboxY + 20 <= boxBottom;

            diagnosticsCheckbox.visible = insideVisibleArea;
            diagnosticsCheckbox.active = insideVisibleArea;

            super.render(context, mouseX, mouseY, delta);
        }


        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }
    }

}
