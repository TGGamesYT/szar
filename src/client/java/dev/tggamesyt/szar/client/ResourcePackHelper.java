package dev.tggamesyt.szar.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.*;

public class ResourcePackHelper {

    public static void applyAll(MinecraftClient client) {
        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();

        List<String> orderedEnabled = manager.getEnabledProfiles().stream()
                .map(ResourcePackProfile::getName)
                .toList();

        Set<String> original = new LinkedHashSet<>(orderedEnabled);

        Set<String> toAdd = new LinkedHashSet<>();
        Set<String> toRemove = new HashSet<>();

        for (ConfigEntry entry : ModConfig.allSettings()) {
            if (!entry.hasResourcePack()) continue;
            if (entry.get()) {
                toAdd.add(entry.linkedResourcePack);
            } else {
                toRemove.add(entry.linkedResourcePack);
            }
        }

        // Build final list: keep originals in order, remove disabled, append new at end
        List<String> finalList = new ArrayList<>();
        for (String name : orderedEnabled) {
            if (!toRemove.contains(name)) {
                finalList.add(name);
            }
        }
        for (String name : toAdd) {
            if (!finalList.contains(name)) {
                finalList.add(name);
            }
        }

        if (new LinkedHashSet<>(finalList).equals(original)) {
            return;
        }

        System.out.println("original: " + original);
        System.out.println("now: " + finalList);

        manager.setEnabledProfiles(finalList);

        // Don't trust getEnabledProfiles() order after setEnabledProfiles — use our list
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(finalList);
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