package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BlueprintBlockEntity extends BlockEntity {

    @Nullable
    private String storedBlockId = null;

    public BlueprintBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasStoredBlock() {
        return storedBlockId != null;
    }

    @Nullable
    public String getStoredBlockId() {
        return storedBlockId;
    }

    public void setStoredBlock(String blockId) {
        this.storedBlockId = blockId;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void clearStoredBlock() {
        this.storedBlockId = null;
        markDirty();
    }

    /** Gets the hardness of the stored block, or default if none. */
    public float getStoredHardness() {
        if (storedBlockId == null) return 1.0f;
        var block = Registries.BLOCK.get(new Identifier(storedBlockId));
        return block.getDefaultState().getHardness(null, null);
    }

    /** Gets the blast resistance of the stored block, or default if none. */
    public float getStoredResistance() {
        if (storedBlockId == null) return 1.0f;
        return Registries.BLOCK.get(new Identifier(storedBlockId)).getBlastResistance();
    }

    /** Returns an ItemStack of the stored block for dropping. */
    public ItemStack getStoredDrop() {
        if (storedBlockId == null) return ItemStack.EMPTY;
        var block = Registries.BLOCK.get(new Identifier(storedBlockId));
        return new ItemStack(block.asItem());
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (storedBlockId != null) nbt.putString("StoredBlock", storedBlockId);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        storedBlockId = nbt.contains("StoredBlock") ? nbt.getString("StoredBlock") : null;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}