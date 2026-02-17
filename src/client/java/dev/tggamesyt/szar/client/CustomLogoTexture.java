package dev.tggamesyt.szar.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Environment(EnvType.CLIENT)
public class CustomLogoTexture extends ResourceTexture {

    public CustomLogoTexture(Identifier id) {
        super(id);
    }

    @Override
    protected TextureData loadTextureData(ResourceManager resourceManager) {
        try {
            InputStream inputStream = MinecraftClient.class
                    .getResourceAsStream("/assets/szar/textures/gui/szarmod.png");

            if (inputStream == null) {
                return new TextureData(
                        new FileNotFoundException(this.location.toString())
                );
            }

            return new TextureData(
                    new TextureResourceMetadata(true, true),
                    NativeImage.read(inputStream)
            );

        } catch (IOException e) {
            return new TextureData(e);
        }
    }
}

