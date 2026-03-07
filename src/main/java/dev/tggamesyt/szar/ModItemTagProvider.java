package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output,
                              CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup lookup) {
        getOrCreateTagBuilder(ItemTags.MUSIC_DISCS).add(Szar.POPTART);
        getOrCreateTagBuilder(ItemTags.MUSIC_DISCS).add(Szar.BAITER_DISC);
        getOrCreateTagBuilder(ItemTags.MUSIC_DISCS).add(Szar.EFN_DISK);
        getOrCreateTagBuilder(ItemTags.MUSIC_DISCS).add(Szar.HELLO_DISC);
    }
}