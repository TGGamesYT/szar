package dev.tggamesyt.szar;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BackroomsLightBlock extends BlockWithEntity {

    public enum LightState implements StringIdentifiable {
        ON, OFF, FLICKERING_ON, FLICKERING_OFF;

        @Override
        public String asString() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<LightState> LIGHT_STATE =
            EnumProperty.of("light_state", LightState.class);

    public BackroomsLightBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(LIGHT_STATE, LightState.ON));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_STATE);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BackroomsLightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return type == Szar.BACKROOMS_LIGHT_ENTITY
                ? (w, pos, s, be) -> BackroomsLightBlockEntity.tick(
                w, pos, s, (BackroomsLightBlockEntity) be)
                : null;
    }

    // Light level based on state
    public static int getLightLevel(BlockState state) {
        return switch (state.get(LIGHT_STATE)) {
            case ON, FLICKERING_ON -> 15;
            case OFF, FLICKERING_OFF -> 0;
        };
    }
}