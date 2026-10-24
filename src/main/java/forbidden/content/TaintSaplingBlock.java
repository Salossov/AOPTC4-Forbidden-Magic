package forbidden.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public final class TaintSaplingBlock extends SaplingBlock {
    public TaintSaplingBlock(Properties properties) { super(new TreeGrower("forbidden:tainted", Optional.empty(), Optional.empty(), Optional.empty()), properties); }
    @Override public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if(state.getValue(STAGE)==0){level.setBlock(pos,state.cycle(STAGE),4);return;}
        level.removeBlock(pos, false);
        if (!TaintTree.grow(level, pos, random)) level.setBlock(pos, state, 2);
    }
}
