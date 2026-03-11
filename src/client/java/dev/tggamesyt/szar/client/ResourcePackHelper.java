package dev.tggamesyt.szar.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.*;

public class ResourcePackHelper {

    public static void applyAll(MinecraftClient client) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();

        Set<String> original = new HashSet<>(
                manager.getEnabledProfiles().stream()
                        .map(p -> p.getName())
                        .toList()
        );

        Set<String> enabledNames = new HashSet<>(
                manager.getEnabledProfiles().stream()
                        .map(p -> p.getName())
                        .toList()
        );

        for (ConfigEntry entry : ModConfig.allSettings()) {
            if (!entry.hasResourcePack()) continue;
            if (entry.get()) {
                enabledNames.add(entry.linkedResourcePack);
            } else {
                enabledNames.remove(entry.linkedResourcePack);
            }
        }

        if (enabledNames.equals(original)) {
            return;
        }


        // Use the manager to set enabled packs properly — this is the key fix
        manager.setEnabledProfiles(enabledNames);

        // Sync back to options and save
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(
                manager.getEnabledProfiles().stream()
                        .map(p -> p.getName())
                        .toList()
        );
        client.options.write();
        client.reloadResources();
    }

    public static Set<String> getManagedPacks() {
        Set<String> managed = new HashSet<>();
        for (ConfigEntry entry : ModConfig.allSettings()) {
            if (entry.hasResourcePack()) managed.add(entry.linkedResourcePack);
        }
        return managed;
    }

    public static boolean isManaged(String packName) {
        return getManagedPacks().contains(packName);
    }
}