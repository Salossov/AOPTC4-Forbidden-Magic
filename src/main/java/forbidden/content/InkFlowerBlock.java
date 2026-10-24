package forbidden.content;

import net.minecraft.world.level.block.BushBlock;

/** No random ticking or spontaneous spread: BlockBlackFlower never called its helper. */
public final class InkFlowerBlock extends BushBlock {
    public static final com.mojang.serialization.MapCodec<InkFlowerBlock> CODEC = simpleCodec(InkFlowerBlock::new);
    @Override public com.mojang.serialization.MapCodec<InkFlowerBlock> codec() { return CODEC; }
    public InkFlowerBlock(Properties properties) { super(properties); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return box(4.8,0,4.8,11.2,6.4,11.2);}
}
