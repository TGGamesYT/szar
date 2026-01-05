package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NiggerEntity;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.item.ItemStack;

public class SzarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                Szar.NWORDPACKET,
                (client, handler, buf, responseSender) -> {

                    ItemStack stack = buf.readItemStack();

                    client.execute(() -> {
                        MinecraftClient.getInstance()
                                .gameRenderer.showFloatingItem(stack);
                    });
                }
        );
        EntityRendererRegistry.register(
                Szar.NiggerEntityType,
                NiggerEntityRenderer::new
        );
    }
}
