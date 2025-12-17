package dev.tggamesyt.szar;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Szar implements ModInitializer {

    public static final String MOD_ID = "szar";

    public static final Block SZAR_BLOCK =
            new SzarBlock();
    public static final Block FASZ_BLOCK =
            new FaszBlock();

    @Override
    public void onInitialize() {

        // register block
        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "cigany"),
                SZAR_BLOCK
        );

        // register item so you can hold it
        Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "cigany"),
                new BlockItem(SZAR_BLOCK, new Item.Settings())
        );

        Registry.register(
                Registries.BLOCK,
                new Identifier(MOD_ID, "fasz"),
                FASZ_BLOCK
        );

        // register item so you can hold it
        Registry.register(
                Registries.ITEM,
                new Identifier(MOD_ID, "fasz"),
                new FaszItem(FASZ_BLOCK, new Item.Settings())
        );
    }
}
