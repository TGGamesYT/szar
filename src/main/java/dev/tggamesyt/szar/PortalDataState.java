package dev.tggamesyt.szar;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.UUID;

public class PortalDataState extends PersistentState {

    private static final String KEY = "szar_portal_data";
    private final NbtCompound data = new NbtCompound();

    public NbtCompound getOrCreatePlayerData(UUID uuid) {
        String key = uuid.toString();
        if (!data.contains(key)) {
            data.put(key, new NbtCompound());
        }
        return data.getCompound(key);
    }

    public void removePlayerData(UUID uuid) {
        data.remove(uuid.toString());
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("PlayerData", data.copy());
        return nbt;
    }

    public static PortalDataState fromNbt(NbtCompound nbt) {
        PortalDataState state = new PortalDataState();
        NbtCompound saved = nbt.getCompound("PlayerData");
        for (String key : saved.getKeys()) {
            state.data.put(key, saved.getCompound(key));
        }
        return state;
    }

    public static PortalDataState getOrCreate(ServerWorld overworld) {
        return overworld.getPersistentStateManager()
                .getOrCreate(PortalDataState::fromNbt, PortalDataState::new, KEY);
    }

    public void saveEntityEntry(UUID uuid, double x, double y, double z) {
        NbtCompound entry = new NbtCompound();
        entry.putDouble("X", x);
        entry.putDouble("Y", y);
        entry.putDouble("Z", z);
        data.put("entity_" + uuid, entry);
        markDirty();
    }

    public double[] getAndRemoveEntityEntry(UUID uuid) {
        String key = "entity_" + uuid;
        if (!data.contains(key)) return null;
        NbtCompound entry = data.getCompound(key);
        double[] coords = new double[]{
                entry.getDouble("X"),
                entry.getDouble("Y"),
                entry.getDouble("Z")
        };
        data.remove(key);
        markDirty();
        return coords;
    }
}