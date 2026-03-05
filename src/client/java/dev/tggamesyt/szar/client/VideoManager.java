package dev.tggamesyt.szar.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.*;

import static dev.tggamesyt.szar.Szar.MOD_ID;

public class VideoManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final int TOTAL_FRAMES = 193;
    private static final int TICKS_PER_FRAME = 1;

    private static final Map<UUID, VideoInstance> activeVideos = new HashMap<>();

    private static final List<Identifier> FRAMES = new ArrayList<>();


    /*
     * Load frames (call once during client init)
     */
    public static void init() {

        if (!FRAMES.isEmpty()) return;

        for (int i = 0; i < TOTAL_FRAMES; i++) {
            String frame = String.format("textures/video/frame_%03d.png", i);
            FRAMES.add(new Identifier(MOD_ID, frame));
        }
    }


    /*
     * Start playing the video on a player
     */
    public static void startVideo(String playerUuid) {

        if (client.world == null) return;

        UUID uuid = UUID.fromString(playerUuid);

        activeVideos.put(uuid, new VideoInstance(uuid));

        playSound(uuid);
    }


    /*
     * Tick method (call every client tick)
     */
    public static void tick() {

        if (activeVideos.isEmpty()) return;

        Iterator<Map.Entry<UUID, VideoInstance>> iterator = activeVideos.entrySet().iterator();

        while (iterator.hasNext()) {

            VideoInstance instance = iterator.next().getValue();

            instance.tick();

            if (instance.finished()) {
                iterator.remove();
            }

        }

    }


    /*
     * Check if a player currently has a video playing
     */
    public static boolean isPlaying(UUID player) {
        return activeVideos.containsKey(player);
    }


    /*
     * Get current frame texture for player
     */
    public static Identifier getCurrentFrame(UUID player) {

        VideoInstance instance = activeVideos.get(player);

        if (instance == null) return null;

        int frameIndex = instance.frame;

        if (frameIndex >= FRAMES.size()) return null;

        return FRAMES.get(frameIndex);
    }


    /*
     * Play sound from the player's location
     */
    private static void playSound(UUID playerUuid) {

        ClientWorld world = client.world;

        if (world == null) return;

        var player = world.getPlayerByUuid(playerUuid);

        if (player == null) return;

        Identifier soundId = new Identifier(MOD_ID, "firtana");
        SoundEvent soundEvent = SoundEvent.of(soundId);

        client.getSoundManager().play(
                new PositionedSoundInstance(
                        soundEvent,
                        SoundCategory.PLAYERS,
                        1.0f, // volume
                        1.0f, // pitch
                        player.getRandom(),
                        player.getX(),
                        player.getY(),
                        player.getZ()
                )
        );
    }


    /*
     * Video instance for each player
     */
    private static class VideoInstance {

        UUID player;
        int frame = 0;
        int tickCounter = 0;

        VideoInstance(UUID player) {
            this.player = player;
        }

        void tick() {

            tickCounter++;

            if (tickCounter >= TICKS_PER_FRAME) {

                frame++;
                tickCounter = 0;

            }

        }

        boolean finished() {
            return frame >= TOTAL_FRAMES;
        }

    }

}