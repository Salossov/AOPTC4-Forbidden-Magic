package forbidden.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Heretic's Study job site block (D15, B35).
 * Crafted from taint planks, taint stone, and a book.
 * Serves as the POI block for the Heretic villager profession.
 */
public class HereticStudyBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<HereticStudyBlock> CODEC = simpleCodec(HereticStudyBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape BASE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    private static final VoxelShape POST = Block.box(4.0D, 2.0D, 4.0D, 12.0D, 12.0D, 12.0D);
    private static final VoxelShape TOP = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 14.0D, 16.0D);
    private static final VoxelShape DESK = Block.box(2.0D, 14.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE = Shapes.or(BASE, POST, TOP, DESK);

    public HereticStudyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
