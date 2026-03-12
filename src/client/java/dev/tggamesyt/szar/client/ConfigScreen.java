package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.*;

public class ConfigScreen extends Screen {

    private final Screen parent;
    private final List<ButtonWidget> presetButtons  = new ArrayList<>();
    private final List<ButtonWidget> toggleButtons  = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.literal("Szar Mod Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y  = 40;

        // ── Preset buttons ────────────────────────────────────────────────────
        List<ConfigPreset> presets = new ArrayList<>(ModConfig.allPresets());
        int btnW  = Math.min(70, (width - 40) / presets.size() - 4);
        int total = presets.size() * (btnW + 4);
        int px    = cx - total / 2;

        presetButtons.clear();
        for (ConfigPreset preset : presets) {
            int bx = px;
            ButtonWidget btn = ButtonWidget.builder(
                            presetButtonLabel(preset),
                            b -> {
                                ModConfig.applyPreset(preset.id);
                                refreshPresetButtons();
                                refreshToggles();
                            })
                    .dimensions(bx, y, btnW, 20)
                    .build();
            addDrawableChild(btn);
            presetButtons.add(btn);
            px += btnW + 4;
        }
        y += 34;

        // ── Toggle buttons — generated from registered settings ───────────────
        toggleButtons.clear();
        for (ConfigEntry entry : ModConfig.allSettings()) {
            ButtonWidget btn = ButtonWidget.builder(
                            toggleLabel(entry),
                            b -> {
                                ModConfig.setAndMarkCustom(entry.id, !entry.get());
                                b.setMessage(toggleLabel(entry));
                                refreshPresetButtons(); // update custom highlight
                            })
                    .dimensions(cx - 100, y, 200, 20)
                    .build();
            addDrawableChild(btn);
            toggleButtons.add(btn);
            y += 26;
        }

        refreshPresetButtons();
        refreshToggles();

        // ── Done ──────────────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
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
            client.setScreen(parent);
        }).dimensions(cx - 75, height - 30, 150, 20).build());
    }

    private void refreshPresetButtons() {
        List<ConfigPreset> presets = new ArrayList<>(ModConfig.allPresets());
        for (int i = 0; i < presetButtons.size(); i++) {
            presetButtons.get(i).setMessage(presetButtonLabel(presets.get(i)));
        }
    }

    private void refreshToggles() {
        boolean isCustom = PRESETS.values().stream()
                .filter(ConfigPreset::isCustom)
                .anyMatch(p -> p.id.equals(ModConfig.getActivePresetId()));

        List<ConfigEntry> entries = new ArrayList<>(ModConfig.allSettings());
        for (int i = 0; i < toggleButtons.size(); i++) {
            ConfigEntry entry = entries.get(i);
            toggleButtons.get(i).active = isCustom;
            toggleButtons.get(i).setMessage(toggleLabel(entry));
        }
    }

    // Grab presets map for isCustom check
    private static final java.util.LinkedHashMap<String, ConfigPreset> PRESETS;
    static {
        try {
            var field = ModConfig.class.getDeclaredField("PRESETS");
            field.setAccessible(true);
            //noinspection unchecked
            PRESETS = (java.util.LinkedHashMap<String, ConfigPreset>) field.get(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Text presetButtonLabel(ConfigPreset preset) {
        boolean active = preset.id.equals(ModConfig.getActivePresetId());
        return active
                ? Text.literal("§e[" + preset.displayName + "]§r")
                : Text.literal(preset.displayName);
    }

    private Text toggleLabel(ConfigEntry entry) {
        return Text.literal(entry.displayName + ": " + (entry.get() ? "§aON§r" : "§cOFF§r"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Preset:"),  width / 2 - 130, 43, 0xAAAAAA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
