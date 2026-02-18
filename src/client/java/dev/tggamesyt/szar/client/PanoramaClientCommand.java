package dev.tggamesyt.szar.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import dev.tggamesyt.szar.Szar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PanoramaClientCommand {

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("takepanorama")
                .executes(PanoramaClientCommand::execute));
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null) {
            context.getSource().sendError(Text.literal("Not in world."));
            return 0;
        }

        client.execute(() -> {
            try {
                createPanorama(client, context);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Failed: " + e.getMessage()));
                e.printStackTrace();
            }
        });

        return 1;
    }

    private static void createPanorama(MinecraftClient client,
                                       CommandContext<FabricClientCommandSource> context) throws Exception {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        File screenshotDir = new File(client.runDirectory, "screenshots");
        File panoramaDir = new File(screenshotDir, "panorama-" + timestamp);

        Files.createDirectories(panoramaDir.toPath());

        // This creates: panoramaDir/screenshots/
        Text result = client.takePanorama(panoramaDir, 1024, 1024);

        // Move files up one folder
        File innerScreenshotDir = new File(panoramaDir, "screenshots");

        if (innerScreenshotDir.exists()) {
            for (int i = 0; i < 6; i++) {
                File src = new File(innerScreenshotDir, "panorama_" + i + ".png");
                File dst = new File(panoramaDir, "panorama_" + i + ".png");

                if (src.exists()) {
                    Files.move(src.toPath(), dst.toPath());
                }
            }

            // Delete the now-empty inner screenshots folder
            innerScreenshotDir.delete();
        }

        context.getSource().sendFeedback(result);
    }

}
