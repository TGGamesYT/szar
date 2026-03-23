package dev.tggamesyt.szar.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.*;

import static dev.tggamesyt.szar.Szar.MOD_ID;

public class VideoManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final int TOTAL_FRAMES = 193;
    private static final int TICKS_PER_FRAME = 1;
    private static final int SOUND_DELAY_TICKS = 4;

    private static final Map<UUID, VideoInstance> activeVideos = new HashMap<>();
    private static final Map<UUID, SoundInstance> activeSounds = new HashMap<>();
    private static final List<Identifier> FRAMES = new ArrayList<>();

    /* Load frames (call once during client init) */
    public static void init() {
        if (!FRAMES.isEmpty()) return;

        for (int i = 1; i <= TOTAL_FRAMES; i++) {
            String frame = String.format("textures/video/frame_%03d.png", i);
            FRAMES.add(new Identifier(MOD_ID, frame));
        }
    }

    /* Start or restart video for a player */
    public static void startVideo(String playerUuid) {
        if (client.world == null) return;

        UUID uuid = UUID.fromString(playerUuid);

        // Stop existing video and sound if playing
        if (activeVideos.containsKey(uuid)) {
            stopVideo(uuid);
        }

        // Start new video with sound delay
        VideoInstance instance = new VideoInstance(uuid, SOUND_DELAY_TICKS);
        activeVideos.put(uuid, instance);
    }

    /* Tick method (call every client tick) */
    public static void tick() {
        if (activeVideos.isEmpty()) return;

        List<UUID> finishedVideos = new ArrayList<>();

        for (Map.Entry<UUID, VideoInstance> entry : activeVideos.entrySet()) {
            VideoInstance instance = entry.getValue();
            instance.tick();

            // Start sound after delay
            if (!activeSounds.containsKey(entry.getKey()) && instance.shouldPlaySound()) {
                playSound(entry.getKey());
            }

            if (instance.finished()) {
                finishedVideos.add(entry.getKey());
            }
        }

        // Stop finished videos and their sounds
        for (UUID uuid : finishedVideos) {
            stopVideo(uuid);
        }
    }

    public static boolean isPlaying(UUID player) {
        return activeVideos.containsKey(player);
    }

    public static Identifier getCurrentFrame(UUID player) {
        VideoInstance instance = activeVideos.get(player);
        if (instance == null) return null;

        int frameIndex = instance.frame;
        if (frameIndex >= FRAMES.size()) return null;

        return FRAMES.get(frameIndex);
    }

    private static void stopVideo(UUID playerUuid) {
        activeVideos.remove(playerUuid);

        SoundInstance sound = activeSounds.remove(playerUuid);
        if (sound != null) {
            client.getSoundManager().stop(sound);
        }
    }

    private static void playSound(UUID playerUuid) {
        if (activeSounds.containsKey(playerUuid)) return;

        ClientWorld world = client.world;
        if (world == null) return;

        PlayerEntity player = world.getPlayerByUuid(playerUuid);
        if (player == null) return;

        Identifier soundId = new Identifier(MOD_ID, "firtana");
        SoundEvent soundEvent = SoundEvent.of(soundId);

        PositionedSoundInstance soundInstance = new PositionedSoundInstance(
                soundEvent,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f,
                player.getRandom(),
                player.getX(),
                player.getY(),
                player.getZ()
        );

        client.getSoundManager().play(soundInstance);
        activeSounds.put(playerUuid, soundInstance);
    }

    /* Video instance with sound delay support */
    private static class VideoInstance {
        UUID player;
        int frame = 0;
        int tickCounter = 0;
        int soundDelay;

        VideoInstance(UUID player, int soundDelayTicks) {
            this.player = player;
            this.soundDelay = soundDelayTicks;
        }

        void tick() {
            tickCounter++;
            if (tickCounter >= TICKS_PER_FRAME) {
                frame++;
                tickCounter = 0;
            }

            if (soundDelay > 0) {
                soundDelay--;
            }
        }

        boolean shouldPlaySound() {
            return soundDelay <= 0;
        }

        boolean finished() {
            return frame >= TOTAL_FRAMES;
        }
    }
}