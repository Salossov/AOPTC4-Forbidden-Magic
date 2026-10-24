package forbidden.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** WorldGenTaintedTree release loop order, heights and corner random calls. */
public final class TaintTree {
    public static boolean grow(ServerLevel level, BlockPos origin, RandomSource random) {
        int height = 4 + random.nextInt(3);
        if (origin.getY() < level.getMinBuildHeight() + 1 || origin.getY() + height + 1 > level.getMaxBuildHeight()) return false;
        for (int y = 0; y <= height + 1; y++) {
            int radius = y == 0 ? 0 : 1;
            if (y >= height - 1) radius = 2;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                var pos = origin.offset(x, y, z);
                if (!level.hasChunkAt(pos) || !replaceable(level, pos)) return false;
            }
        }
        var ground = level.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.FARMLAND)) return false;
        if (origin.getY() >= level.getMaxBuildHeight() - height - 1) return false;
        level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), 2);
        for (int y = height - 3; y <= height; y++) {
            int delta = y - height, radius = 1 - delta / 2;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) != radius || Math.abs(z) != radius || random.nextInt(2) != 0 && delta != 0) {
                    var pos = origin.offset(x, y, z);
                    if (airOrLeaves(level, pos)) level.setBlock(pos, ForbiddenContent.TAINT_LEAVES.get().defaultBlockState(), 3);
                }
            }
        }
        for (int y = 0; y < height; y++) { var pos = origin.above(y); if (airOrLeaves(level, pos)) level.setBlock(pos, ForbiddenContent.TAINT_LOG.get().defaultBlockState(), 3); }
        return true;
    }
    private static boolean airOrLeaves(ServerLevel level, BlockPos pos) { var state=level.getBlockState(pos); return state.isAir() || state.is(BlockTags.LEAVES) && !state.is(ForbiddenContent.TAINT_LEAVES.get()); }
    private static boolean replaceable(ServerLevel level, BlockPos pos) { var state=level.getBlockState(pos); return airOrLeaves(level,pos) || state.is(BlockTags.DIRT) || state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG) || state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.ACACIA_LOG) || state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.OAK_SAPLING) || state.is(Blocks.VINE); }
    private TaintTree() { }
}
