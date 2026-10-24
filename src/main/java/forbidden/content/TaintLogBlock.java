package forbidden.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TaintLogBlock extends RotatedPillarBlock {
    public TaintLogBlock(Properties properties){super(properties);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!replacement.is(this))TaintLeavesBlock.markDecay(level,pos,4);
        super.onRemove(state,level,pos,replacement,moving);
    }
}
