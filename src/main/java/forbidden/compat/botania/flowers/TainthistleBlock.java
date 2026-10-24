package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Tainthistle: Generating Flower (C03).
 * Absorbs taint and flux blocks in a 4-block radius, converting them into burn time (cap 4000)
 * and generating Mana at 3 Mana per 2 ticks.
 */
public class TainthistleBlock extends BushBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<TainthistleBlock> CODEC = simpleCodec(TainthistleBlock::new);
    @Override public com.mojang.serialization.MapCodec<TainthistleBlock> codec() { return CODEC; }

    protected static final VoxelShape SHAPE = box(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D);

    public TainthistleBlock() {
        this(BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).noCollission().instabreak().sound(SoundType.GRASS));
    }

    public TainthistleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TainthistleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, p, st, be) -> {
            if (be instanceof TainthistleBlockEntity flower) {
                flower.tick(lvl, p, st);
            }
        };
    }
}
