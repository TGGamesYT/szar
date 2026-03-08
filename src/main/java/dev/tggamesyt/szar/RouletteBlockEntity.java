package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RouletteBlockEntity extends BlockEntity {
    public int wheelWaitSeconds    = 10;
    public int wheelRollingSeconds = 5;
    public int intermissionSeconds = 20;

    private static final int[] WHEEL_ORDER = {
            0, 26, 3, 35, 12, 28, 7, 29, 18, 22, 9,
            31, 14, 20, 1, 33, 16, 24, 5, 10, 23, 8,
            30, 11, 36, 13, 27, 6, 34, 17, 25, 2, 21,
            4, 19, 15, 32
    };

    // 0=green, then alternating red/black in standard European roulette order
    // Index = the number itself (0-36)
    public static final String[] NUMBER_COLORS = {
            "green",                                    // 0
            "red",   "black", "red",   "black", "red",  // 1-5
            "black", "red",   "black", "red",   "black",// 6-10
            "black", "red",   "black", "red",   "black",// 11-15
            "red",   "black", "red",   "black",   "black",// 16-20
            "red",   "black", "red",   "black", "red",  // 21-25
            "black", "red",   "red", "black", "red",  // 26-30
            "black", "red",   "black", "red",   "black",// 31-35
            "red"                                       // 36
    };

    public static float getWheelDegrees(int winnerNum) {
        for (int i = 0; i < WHEEL_ORDER.length; i++) {
            if (WHEEL_ORDER[i] == winnerNum) return i * 9.73f;
        }
        return 0.0f;
    }

    private int winnernum = 0;
    public boolean isIntermission = true;
    public int nextspinTime = intermissionSeconds * 20;
    final PropertyDelegate propertyDelegate;

    private final Map<UUID, PlayerBetInventories> playerInventories = new HashMap<>();

    public RouletteBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.ROULETTE_BLOCKENTITY, pos, state);
        this.propertyDelegate = new ArrayPropertyDelegate(3);
    }

    public static class PlayerBetInventories {
        public final SimpleInventory fullbet  = new SimpleInventory(37);
        public final SimpleInventory twelves  = new SimpleInventory(3);
        public final SimpleInventory halves   = new SimpleInventory(2);
        public final SimpleInventory evenodd  = new SimpleInventory(2);
        public final SimpleInventory blackred = new SimpleInventory(2);
        public final SimpleInventory thirds   = new SimpleInventory(3);

        public void clear() {
            fullbet.clear();
            twelves.clear();
            halves.clear();
            evenodd.clear();
            blackred.clear();
            thirds.clear();
        }

        public void writeNbt(NbtCompound nbt) {
            nbt.put("fullbet",  inventoryToNbt(fullbet));
            nbt.put("twelves",  inventoryToNbt(twelves));
            nbt.put("halves",   inventoryToNbt(halves));
            nbt.put("evenodd",  inventoryToNbt(evenodd));
            nbt.put("blackred", inventoryToNbt(blackred));
            nbt.put("thirds",   inventoryToNbt(thirds));
        }

        public void readNbt(NbtCompound nbt) {
            nbtToInventory(nbt.getList("fullbet",  10), fullbet);
            nbtToInventory(nbt.getList("twelves",  10), twelves);
            nbtToInventory(nbt.getList("halves",   10), halves);
            nbtToInventory(nbt.getList("evenodd",  10), evenodd);
            nbtToInventory(nbt.getList("blackred", 10), blackred);
            nbtToInventory(nbt.getList("thirds",   10), thirds);
        }

        private static NbtList inventoryToNbt(SimpleInventory inv) {
            NbtList list = new NbtList();
            for (int i = 0; i < inv.size(); i++) {
                NbtCompound slot = new NbtCompound();
                slot.putByte("Slot", (byte) i);
                inv.getStack(i).writeNbt(slot);
                list.add(slot);
            }
            return list;
        }

        private static void nbtToInventory(NbtList list, SimpleInventory inv) {
            for (int i = 0; i < list.size(); i++) {
                NbtCompound slot = list.getCompound(i);
                int index = slot.getByte("Slot") & 0xFF;
                if (index < inv.size()) {
                    inv.setStack(index, ItemStack.fromNbt(slot));
                }
            }
        }
    }

    public PlayerBetInventories getInventoriesFor(UUID uuid) {
        return playerInventories.computeIfAbsent(uuid, id -> new PlayerBetInventories());
    }

    public PlayerBetInventories getInventoriesFor(net.minecraft.entity.player.PlayerEntity player) {
        return getInventoriesFor(player.getUuid());
    }

    // ─── Payout logic ────────────────────────────────────────────────────────

    private static void giveItems(ServerPlayerEntity player, ItemStack stack, int multiplier) {
        if (stack.isEmpty()) return;
        int total = stack.getCount() * multiplier;
        while (total > 0) {
            int batchSize = Math.min(total, stack.getMaxCount());
            ItemStack give = new ItemStack(stack.getItem(), batchSize);
            player.getInventory().offerOrDrop(give);
            total -= batchSize;
        }
    }

    private void resolvePayouts(World world, int winner) {
        String winnerColor = NUMBER_COLORS[winner];

        for (Map.Entry<UUID, PlayerBetInventories> entry : playerInventories.entrySet()) {
            ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(entry.getKey());
            if (player == null) {
                continue;
            }
            PlayerBetInventories inv = entry.getValue();

            // fullbet
            ItemStack fullbetBet = inv.fullbet.getStack(winner).copy();
            inv.fullbet.setStack(winner, ItemStack.EMPTY);
            if (!fullbetBet.isEmpty()) {
                giveItems(player, fullbetBet, 36);
            }

            // twelves
            if (winner >= 1 && winner <= 12) {
                ItemStack bet = inv.twelves.getStack(0).copy();
                inv.twelves.setStack(0, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 3); }
            } else if (winner >= 13 && winner <= 24) {
                ItemStack bet = inv.twelves.getStack(1).copy();
                inv.twelves.setStack(1, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 3); }
            } else if (winner >= 25 && winner <= 36) {
                ItemStack bet = inv.twelves.getStack(2).copy();
                inv.twelves.setStack(2, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 3); }
            }

            // halves
            if (winner >= 1 && winner <= 18) {
                ItemStack bet = inv.halves.getStack(0).copy();
                inv.halves.setStack(0, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 2); }
            } else if (winner >= 19 && winner <= 36) {
                ItemStack bet = inv.halves.getStack(1).copy();
                inv.halves.setStack(1, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 2); }
            }

            // evenodd
            if (winner != 0) {
                if (winner % 2 == 0) {
                    ItemStack bet = inv.evenodd.getStack(0).copy();
                    inv.evenodd.setStack(0, ItemStack.EMPTY);
                    if (!bet.isEmpty()) { giveItems(player, bet, 2); }
                } else {
                    ItemStack bet = inv.evenodd.getStack(1).copy();
                    inv.evenodd.setStack(1, ItemStack.EMPTY);
                    if (!bet.isEmpty()) { giveItems(player, bet, 2); }
                }
            }

            // blackred
            if (winnerColor.equals("red")) {
                ItemStack bet = inv.blackred.getStack(0).copy();
                inv.blackred.setStack(0, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 2); }
            } else if (winnerColor.equals("black")) {
                ItemStack bet = inv.blackred.getStack(1).copy();
                inv.blackred.setStack(1, ItemStack.EMPTY);
                if (!bet.isEmpty()) { giveItems(player, bet, 2); }
            }

            // thirds
            if (winner != 0) {
                if (winner % 3 == 0) {
                    ItemStack bet = inv.thirds.getStack(0).copy();
                    inv.thirds.setStack(0, ItemStack.EMPTY);
                    if (!bet.isEmpty()) { giveItems(player, bet, 2); }
                } else if ((winner + 1) % 3 == 0) {
                    ItemStack bet = inv.thirds.getStack(1).copy();
                    inv.thirds.setStack(1, ItemStack.EMPTY);
                    if (!bet.isEmpty()) { giveItems(player, bet, 2); }
                } else if ((winner + 2) % 3 == 0) {
                    ItemStack bet = inv.thirds.getStack(2).copy();
                    inv.thirds.setStack(2, ItemStack.EMPTY);
                    if (!bet.isEmpty()) { giveItems(player, bet, 2); }
                }
            }
            inv.clear();
        }
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state, RouletteBlockEntity be) {
        if (world.isClient) return;

        be.nextspinTime--;

        if (be.isIntermission) {
            if (be.nextspinTime > 0) {
                // still counting down to spin
            } else if (be.nextspinTime == 0) {
                // Spin is starting — pick winner and clear all bets
                be.winnernum = new Random().nextInt(37);
                be.isIntermission = false;
            }
        } else {
            // Spinning phase — nextspinTime is negative here
            int spinTicks = (be.wheelWaitSeconds + be.wheelRollingSeconds) * 20;
            if (be.nextspinTime <= -spinTicks) {
                // Spin ended — resolve payouts then go back to intermission
                be.resolvePayouts(world, be.winnernum);
                be.isIntermission = true;
                be.nextspinTime   = be.intermissionSeconds * 20;
            }
        }

        be.propertyDelegate.set(0, be.isIntermission ? 1 : 0);
        be.propertyDelegate.set(1, be.nextspinTime);
        be.propertyDelegate.set(2, be.winnernum);
        be.markDirty();
    }

    private void clearAllBets() {
        for (PlayerBetInventories inv : playerInventories.values()) {
            inv.clear();
        }
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("isIntermission", isIntermission);
        nbt.putInt("nextspinTime", nextspinTime);
        nbt.putInt("winnernum", winnernum);

        NbtCompound playersNbt = new NbtCompound();
        for (Map.Entry<UUID, PlayerBetInventories> entry : playerInventories.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            entry.getValue().writeNbt(playerNbt);
            playersNbt.put(entry.getKey().toString(), playerNbt);
        }
        nbt.put("playerInventories", playersNbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        isIntermission = nbt.getBoolean("isIntermission");
        nextspinTime   = nbt.getInt("nextspinTime");
        winnernum      = nbt.getInt("winnernum");

        playerInventories.clear();
        NbtCompound playersNbt = nbt.getCompound("playerInventories");
        for (String key : playersNbt.getKeys()) {
            UUID uuid = UUID.fromString(key);
            PlayerBetInventories invs = new PlayerBetInventories();
            invs.readNbt(playersNbt.getCompound(key));
            playerInventories.put(uuid, invs);
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt);
        return nbt;
    }
}