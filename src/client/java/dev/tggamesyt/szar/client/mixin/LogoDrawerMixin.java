package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.time.LocalDate;

@Mixin(LogoDrawer.class)
public class LogoDrawerMixin {

    @Unique
    private static final Identifier VANILLA_EDITION =
            new Identifier("textures/gui/title/edition.png");

    @Unique
    private static final Identifier SZAR_EDITION =
            new Identifier("szar", "textures/aprilfools/edition.png");

    @Unique
    private static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == SzarClient.april && today.getDayOfMonth() == SzarClient.fools;
    }

    @ModifyArg(
            method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIFFIIII)V",
                    ordinal = 1
            ),
            index = 0
    )
    private Identifier replaceEditionTexture(Identifier texture) {
        return isAprilFools() ? SZAR_EDITION : texture;
    }
}