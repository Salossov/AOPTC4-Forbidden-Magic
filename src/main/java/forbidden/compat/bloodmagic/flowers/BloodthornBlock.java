package forbidden.compat.bloodmagic.flowers;

import forbidden.compat.bloodmagic.BloodMagicItems;
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
 * Bloodthorn: Hybrid flower (C08).
 * Inflicts weakness and thorns damage to surrounding non-player mobs and converts their blood to LP and Mana.
 */
public class BloodthornBlock extends BushBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<BloodthornBlock> CODEC = simpleCodec(BloodthornBlock::new);
    @Override public com.mojang.serialization.MapCodec<BloodthornBlock> codec() { return CODEC; }

    protected static final VoxelShape SHAPE = box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);

    public BloodthornBlock() {
        this(BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).noCollission().instabreak().sound(SoundType.GRASS));
    }

    public BloodthornBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodthornBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, p, st, be) -> {
            if (be instanceof BloodthornBlockEntity flower) {
                flower.tick(lvl, p, st);
            }
        };
    }
}
