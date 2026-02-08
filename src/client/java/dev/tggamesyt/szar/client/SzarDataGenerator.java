package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.ModItemTagProvider;
import dev.tggamesyt.szar.ModPoiTagProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryWrapper;

public class SzarDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(ModPoiTagProvider::new);
        pack.addProvider(ModItemTagProvider::new);
    }
}
