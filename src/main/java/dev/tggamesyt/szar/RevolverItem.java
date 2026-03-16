package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static dev.tggamesyt.szar.Szar.REVOLVER_STATE_SYNC;

public class RevolverItem extends Item {

    public static final int CHAMBERS = 6;

    public RevolverItem(Settings settings) {
        super(settings.maxDamage(384));
    }

    // ── NBT helpers ──────────────────────────────────────────────

    public static boolean[] getChambers(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        boolean[] chambers = new boolean[CHAMBERS];
        for (int i = 0; i < CHAMBERS; i++) {
            chambers[i] = nbt.getBoolean("chamber_" + i);
        }
        return chambers;
    }

    public static void setChambers(ItemStack stack, boolean[] chambers) {
        NbtCompound nbt = stack.getOrCreateNbt();
        for (int i = 0; i < CHAMBERS; i++) {
            nbt.putBoolean("chamber_" + i, chambers[i]);
        }
    }

    public static int getCurrentChamber(ItemStack stack) {
        return stack.getOrCreateNbt().getInt("current_chamber");
    }

    public static void setCurrentChamber(ItemStack stack, int index) {
        stack.getOrCreateNbt().putInt("current_chamber", index);
    }

    /** Rotate by a random 1-6 steps (called by keybind) */
    public static void spin(ItemStack stack, World world) {
        int steps = 1 + world.getRandom().nextInt(CHAMBERS);
        int current = getCurrentChamber(stack);
        setCurrentChamber(stack, (current + steps) % CHAMBERS);
        //play sound
    }

    // ── Use (right-click hold = aim) ─────────────────────────────

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        player.setCurrentHand(hand);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW; // raises arm
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // held indefinitely
    }

    public static void syncRevolverToClient(ServerPlayerEntity player, ItemStack stack) {
        boolean[] chambers = RevolverItem.getChambers(stack);
        int current = RevolverItem.getCurrentChamber(stack);

        PacketByteBuf buf = PacketByteBufs.create();
        for (int i = 0; i < RevolverItem.CHAMBERS; i++) {
            buf.writeBoolean(chambers[i]);
        }
        buf.writeInt(current);
        ServerPlayNetworking.send(player, REVOLVER_STATE_SYNC, buf);
    }

}