package dev.tggamesyt.szar.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class SzarTosHandler {

    private static final Gson GSON = new Gson();

    private static final File CONFIG_FILE =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "szar/tos.json");

    private static final Text MOD_TOS_TEXT = Text.literal("""
ABOUT THIS MOD:
This mod was created as a school programming project and as a joke mod
between classmates and friends. Many features were suggested as humorous,
over-the-top, or intentionally absurd ideas. The content is NOT meant to be
taken seriously. It is a fictional, parody-style Minecraft modification.

okay, AI slop aside, this mod includes various "unacceptable"
or age restricted topics, so please tell your age below.
please note this is only saved locally, and can be edited any time from Mod Menu's settings,
but for your own safety please only disable filters approppriate for your age.

also do NOT attempt to do any illegal activities that you see in this mod,
this is purely a fictional representation of thoose things.

thank you and enjoy my silly mod <3
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

    private static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();

            JsonObject obj = new JsonObject();
            obj.addProperty("tosAccepted", true);

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception ignored) {}
    }

    // ============================
    // AGE → PRESET LOGIC
    // ============================

    private static void applyPresetForAge(int age) {
        List<ConfigPreset> presets = new ArrayList<>(ModConfig.allPresets());
        MinecraftClient client = MinecraftClient.getInstance();

        String targetId;
        if (age >= 18) {
            targetId = "18+";
        } else if (age == 17) {
            targetId = "17+";
        } else {
            targetId = "minor";
        }

        for (ConfigPreset preset : presets) {
            if (preset.id.equals(targetId)) {
                ModConfig.applyPreset(preset.id);
                ModConfig.save();
                ResourcePackHelper.applyAll(client);
                if (client.world != null) {
                    PacketByteBuf buf = PacketByteBufs.create();

                    // Write each setting as: id (string), value (boolean)
                    var settings = ModConfig.allSettings();
                    buf.writeInt(settings.size());
                    for (ConfigEntry entry : settings) {
                        buf.writeString(entry.id);
                        buf.writeBoolean(entry.get());
                    }

                    ClientPlayNetworking.send(Szar.CONFIG_SYNC, buf);
                }
                break;
            }
        }
    }

    // ============================
    // CUSTOM SCREEN
    // ============================

    private static class TosScreen extends Screen {

        private TextFieldWidget ageField;
        private ButtonWidget agreeButton;
        private String errorMessage = "";

        private int scrollOffset = 0;
        private int maxScroll = 0;

        private static final int PADDING = 20;
        private static final int TITLE_HEIGHT = 30;
        private static final int FOOTER_HEIGHT = 80; // taller to fit age input

        private String[] lines;

        protected TosScreen() {
            super(Text.literal("Szar Mod - Information"));
        }

        @Override
        protected void init() {
            lines = MOD_TOS_TEXT.getString().split("\n");

            int textHeight = lines.length * 12;
            int visibleHeight = this.height - TITLE_HEIGHT - FOOTER_HEIGHT - PADDING;
            maxScroll = Math.max(0, (textHeight + 20) - visibleHeight);

            int centerX = this.width / 2;

            // Age input field
            ageField = new TextFieldWidget(
                    this.textRenderer,
                    centerX - 50,
                    this.height - 65,
                    100,
                    20,
                    Text.literal("Age")
            );
            ageField.setMaxLength(3);
            ageField.setPlaceholder(Text.literal("Your age..."));
            ageField.setChangedListener(text -> {
                errorMessage = "";
                updateAgreeButton();
            });
            this.addDrawableChild(ageField);

            // Agree button
            agreeButton = ButtonWidget.builder(
                    Text.literal("Agree"),
                    button -> {
                        String input = ageField.getText().trim();
                        try {
                            int age = Integer.parseInt(input);
                            if (age < 1 || age > 130) {
                                errorMessage = "Please enter a valid age.";
                                return;
                            }
                            save();
                            applyPresetForAge(age);
                            MinecraftClient.getInstance().setScreen(null);
                        } catch (NumberFormatException e) {
                            errorMessage = "Please enter a valid number.";
                        }
                    }
            ).dimensions(centerX - 155, this.height - 40, 150, 20).build();
            agreeButton.active = false;
            this.addDrawableChild(agreeButton);

            // Decline button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Decline"),
                    button -> MinecraftClient.getInstance().stop()
            ).dimensions(centerX + 5, this.height - 40, 150, 20).build());
        }

        private void updateAgreeButton() {
            if (agreeButton == null) return;
            String text = ageField.getText().trim();
            agreeButton.active = !text.isEmpty() && text.matches("\\d+");
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            scrollOffset -= (int)(amount * 15);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);

            // Title
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

            // Scroll area background
            context.fill(boxLeft, boxTop, boxRight, boxBottom, 0x88000000);
            context.enableScissor(boxLeft, boxTop, boxRight, boxBottom);

            int y = boxTop + 10 - scrollOffset;
            for (String line : lines) {
                context.drawTextWithShadow(this.textRenderer, line, boxLeft + 10, y, 0xDDDDDD);
                y += 12;
            }

            context.disableScissor();

            // Age label
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Enter your age:"),
                    this.width / 2,
                    this.height - 78,
                    0xAAAAAA
            );

            // Error message
            if (!errorMessage.isEmpty()) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal(errorMessage),
                        this.width / 2,
                        this.height - 55,
                        0xFF4444
                );
            }

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }
    }
}