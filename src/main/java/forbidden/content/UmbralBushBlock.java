package forbidden.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class UmbralBushBlock extends DoublePlantBlock implements BonemealableBlock {
    public UmbralBushBlock(Properties properties) { super(properties.randomTicks()); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) <= 2) spread(level, pos, random);
    }
    @Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) { return true; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { spread(level, pos, random); }
    private void spread(ServerLevel level, BlockPos origin, RandomSource random) {
        BlockPos base = origin, target = candidate(base, random);
        for (int i = 0; i < 8; i++) {
            if (canPlant(level, target)) base = target;
            target = candidate(base, random);
        }
        if (canPlant(level, target)) level.setBlock(target, ForbiddenContent.INK_FLOWER.get().defaultBlockState(), 2);
    }
    private static BlockPos candidate(BlockPos pos, RandomSource random) {
        int x = random.nextInt(3) - 1, y = random.nextInt(2) - random.nextInt(2), z = random.nextInt(3) - 1;
        return pos.offset(x, y, z);
    }
    private boolean canPlant(ServerLevel level, BlockPos pos) { return level.isEmptyBlock(pos) && defaultBlockState().canSurvive(level, pos); }
}
