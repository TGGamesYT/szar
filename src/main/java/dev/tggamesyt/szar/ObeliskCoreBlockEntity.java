package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class ObeliskCoreBlockEntity extends BlockEntity {

    private boolean hasPlaneMob = false;

    public ObeliskCoreBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.OBELISK_CORE_ENTITY, pos, state);
    }

    // NBT serialization
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("hasPlaneMob", hasPlaneMob);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasPlaneMob = nbt.getBoolean("hasPlaneMob");
    }

    public boolean hasPlaneMob() { return hasPlaneMob; }
    public void setHasPlaneMob(boolean value) { hasPlaneMob = value; }
}
