package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrackerBlockEntity extends BlockEntity {

    // The overworld entry coords (used by nether-side tracker to know where to send players back)
    public double returnX, returnY, returnZ;
    // Whether this tracker is in the nether or overworld
    public boolean isNetherSide = false;
    // BlockPos of the paired tracker in the other dimension
    public BlockPos pairedTrackerPos = null;

    // UUIDs of players currently inside the dimension via this portal
    private final Set<UUID> playersInside = new HashSet<>();

    public TrackerBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.TRACKER_BLOCK_ENTITY, pos, state);
    }

    public void addPlayer(UUID uuid) {
        playersInside.add(uuid);
        markDirty();
    }

    public void removePlayer(UUID uuid) {
        playersInside.remove(uuid);
        markDirty();
    }

    public boolean hasPlayers() {
        return !playersInside.isEmpty();
    }

    public Set<UUID> getPlayersInside() {
        return playersInside;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putDouble("ReturnX", returnX);
        nbt.putDouble("ReturnY", returnY);
        nbt.putDouble("ReturnZ", returnZ);
        nbt.putBoolean("IsNetherSide", isNetherSide);

        if (pairedTrackerPos != null) {
            nbt.putInt("PairedX", pairedTrackerPos.getX());
            nbt.putInt("PairedY", pairedTrackerPos.getY());
            nbt.putInt("PairedZ", pairedTrackerPos.getZ());
        }

        NbtList list = new NbtList();
        for (UUID uuid : playersInside) {
            list.add(NbtString.of(uuid.toString()));
        }
        nbt.put("PlayersInside", list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        returnX = nbt.getDouble("ReturnX");
        returnY = nbt.getDouble("ReturnY");
        returnZ = nbt.getDouble("ReturnZ");
        isNetherSide = nbt.getBoolean("IsNetherSide");

        if (nbt.contains("PairedX")) {
            pairedTrackerPos = new BlockPos(
                    nbt.getInt("PairedX"),
                    nbt.getInt("PairedY"),
                    nbt.getInt("PairedZ")
            );
        }

        NbtList list = nbt.getList("PlayersInside", 8);
        playersInside.clear();
        for (int i = 0; i < list.size(); i++) {
            playersInside.add(UUID.fromString(list.getString(i)));
        }
    }
}