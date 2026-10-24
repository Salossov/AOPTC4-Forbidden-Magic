package forbidden.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Twelve slices; regenerates one on every random tick without an extra RNG draw. */
public final class ArcaneCakeBlock extends Block {
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 11);
    public ArcaneCakeBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(BITES, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BITES); }
    // Approved Q03: divide the 14-pixel cake width into twelve equal slices.
    private static double left(BlockState state) { return 1 + state.getValue(BITES) * 14.0 / 12; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(left(state), 0, 1, 15, 8, 15); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(left(state), 0, 1, 15, 7, 15); }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return level.getBlockState(pos.below()).isSolid(); }
    @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { eat(state, level, pos, player); return InteractionResult.sidedSuccess(level.isClientSide); }
    @Override protected void attack(BlockState state, Level level, BlockPos pos, Player player) { eat(state, level, pos, player); }
    private void eat(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player.canEat(false)) {
            player.getFoodData().eat(2, 1F);
            if (state.getValue(BITES) == 11) level.removeBlock(pos, false);
            else level.setBlock(pos, state.cycle(BITES), 2);
        }
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { if (state.getValue(BITES) > 0) level.setBlock(pos, state.setValue(BITES, state.getValue(BITES) - 1), 2); }
}
