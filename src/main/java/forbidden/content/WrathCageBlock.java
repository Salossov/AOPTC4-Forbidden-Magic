package forbidden.content;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WrathCageBlock extends Block implements EntityBlock {
    public WrathCageBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WrathCageEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ForbiddenContent.CAGE_ENTITY.get()) return null;
        return level.isClientSide
            ? (l, p, s, be) -> ((WrathCageEntity) be).clientTick()
            : (l, p, s, be) -> ((WrathCageEntity) be).serverTick();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WrathCageEntity cage)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        var id = stack.get(ForbiddenComponents.MOB.get());
        if (ForbiddenContent.MOB_CRYSTAL != null && stack.is(ForbiddenContent.MOB_CRYSTAL.get()) && id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            if (!level.isClientSide) player.setItemInHand(hand, cage.exchange(id));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse("forbidden:diabolist_fork")) && ForbiddenConfig.WRATH_COST.get() > 0) {
            if (!level.isClientSide) cage.cycleMode();
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof WrathCageEntity cage) {
            var crystal = cage.crystal();
            if (!crystal.isEmpty()) {
                var random = level.random;
                float x = random.nextFloat() * .8F + .1F, y = random.nextFloat() * .8F + .1F, z = random.nextFloat() * .8F + .1F;
                var entity = new ItemEntity(level, pos.getX() + x, pos.getY() + y, pos.getZ() + z, crystal);
                entity.setDeltaMovement((float) random.nextGaussian() * .05F, (float) random.nextGaussian() * .05F + .2F, (float) random.nextGaussian() * .05F);
                level.addFreshEntity(entity);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
