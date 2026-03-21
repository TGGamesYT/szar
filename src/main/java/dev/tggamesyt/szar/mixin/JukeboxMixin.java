package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public class JukeboxMixin {

    @Inject(method = "startPlaying", at = @At("HEAD"))
    private void szar_onDiscPlayed(CallbackInfo ci) {
        JukeboxBlockEntity self =
                (JukeboxBlockEntity)(Object)this;

        if (self.getWorld() == null || self.getWorld().isClient) return;

        ItemStack stack = self.getStack(0);
        if (stack.isEmpty()) return;

        // Check if the disc's identifier contains "szar"
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (!id.getNamespace().equals("szar")) return;

        // Grant advancement to all nearby players
        self.getWorld().getEntitiesByClass(
                net.minecraft.entity.player.PlayerEntity.class,
                new net.minecraft.util.math.Box(self.getPos()).expand(64),
                p -> true
        ).forEach(p -> Szar.grantAdvancement(p, "crazy"));
    }
}