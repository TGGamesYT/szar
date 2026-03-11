package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class RouletteBlock extends Block implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public RouletteBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    VoxelShape shape23 = VoxelShapes.cuboid(0.25f, 0f, 0f, 0.75f, 0.25f, 0.125f);
    VoxelShape shape24 = VoxelShapes.cuboid(0.125f, 0f, 0.125f, 0.875f, 0.125f, 0.25f);
    VoxelShape shape25 = VoxelShapes.cuboid(0f, 0f, 0.25f, 1f, 0.125f, 0.75f);
    VoxelShape shape26 = VoxelShapes.cuboid(0.125f, 0f, 0.75f, 0.875f, 0.125f, 0.875f);
    VoxelShape shape27 = VoxelShapes.cuboid(0.25f, 0f, 0.875f, 0.75f, 0.25f, 1f);
    VoxelShape shape28 = VoxelShapes.cuboid(0f, 0.125f, 0.25f, 0.125f, 0.25f, 0.75f);
    VoxelShape shape29 = VoxelShapes.cuboid(0.875f, 0.125f, 0.25f, 1f, 0.25f, 0.75f);
    VoxelShape shape30 = VoxelShapes.cuboid(0.75f, 0.125f, 0.125f, 0.875f, 0.25f, 0.25f);
    VoxelShape shape31 = VoxelShapes.cuboid(0.75f, 0.125f, 0.75f, 0.875f, 0.25f, 0.875f);
    VoxelShape shape32 = VoxelShapes.cuboid(0.125f, 0.125f, 0.125f, 0.25f, 0.25f, 0.25f);
    VoxelShape shape33 = VoxelShapes.cuboid(0.125f, 0.125f, 0.75f, 0.25f, 0.25f, 0.875f);
    VoxelShape shape34 = VoxelShapes.cuboid(0.3125f, 0.313125f, 0.3125f, 0.6875f, 0.313125f, 0.6875f);
    VoxelShape shape35 = VoxelShapes.cuboid(0.4375f, 0.125f, 0.4375f, 0.5625f, 0.3125f, 0.5625f);
    VoxelShape BASE_SHAPE = VoxelShapes.union(shape23, shape24, shape25, shape26, shape27, shape28, shape29, shape30, shape31, shape32, shape33, shape34, shape35);

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};

        int times = (to.getHorizontal() - from.getHorizontal() + 4) % 4;

        for (int i = 0; i < times; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = VoxelShapes.union(buffer[1],
                            VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX))
            );
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        return buffer[0];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return rotateShape(Direction.NORTH, state.get(FACING), BASE_SHAPE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getCollisionShape(state, world, pos, context);
    }

    // ===== ROTATION =====

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RouletteBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (PlayerConfigStore.get(player, "gambling")) {return ActionResult.FAIL;}
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof RouletteBlockEntity be)) {
            return ActionResult.PASS;
        }

        Vec3d hitVec = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = state.get(FACING);

        double x = hitVec.x;
        double y = hitVec.y;
        double z = hitVec.z;

        // Rotate based on facing (proper Minecraft rotation logic)
        switch (facing) {
            case NORTH -> {
                // no change
            }
            case SOUTH -> {
                x = 1 - x;
                z = 1 - z;
            }
            case WEST -> {
                double temp = x;
                x = z;
                z = 1 - temp;
            }
            case EAST -> {
                double temp = x;
                x = 1 - z;
                z = temp;
            }
        }

        boolean isHandle =
                x >= 0.0625 && x <= 0.25 &&
                        y >= 0.5 && y <= 0.6875 &&
                        z >= 0.4375 && z <= 1.1875;
        if (!world.isClient) {
            // Open the GUI (client will receive block position)
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        }

        return ActionResult.SUCCESS;
    }
    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof RouletteBlockEntity slotBe)) return null;

        // Return an ExtendedScreenHandlerFactory that sends the BlockPos to the client
        return new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeBlockPos(pos); // send the block pos to client for the constructor
            }

            @Override
            public Text getDisplayName() {
                return Text.literal("Roulette");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                return new RouletteScreenHandler(syncId, inv, slotBe);
            }
        };
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        if (state.contains(FACING)) {
            return state.with(FACING, rotation.rotate(state.get(FACING)));
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        if (state.contains(FACING)) {
            return state.rotate(mirror.getRotation(state.get(FACING)));
        }
        return state;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type) {

        return type == Szar.ROULETTE_BLOCKENTITY
                ? (world1, pos, state1, blockEntity) ->
                RouletteBlockEntity.tick(world1, pos, state1, (RouletteBlockEntity) blockEntity)
                : null;
    }
}