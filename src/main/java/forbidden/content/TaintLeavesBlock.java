package forbidden.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class TaintLeavesBlock extends LeavesBlock {
    public static final BooleanProperty CHECK_DECAY = BooleanProperty.create("check_decay");
    public TaintLeavesBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(CHECK_DECAY, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(CHECK_DECAY); }
    @Override public boolean isRandomlyTicking(BlockState state) { return state.getValue(CHECK_DECAY) && !state.getValue(PERSISTENT); }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasChunksAt(pos.offset(-5,-5,-5),pos.offset(5,5,5))) return;
        // Release isLeaves() returns false, including for this block. Preserve that
        // unusual behavior: the center is not a traversable leaf in its decay search.
        Block.dropResources(state,level,pos); level.removeBlock(pos,false);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this)) markDecay(level,pos,1);
        super.onRemove(state,level,pos,replacement,moving);
    }
    static void markDecay(Level level, BlockPos pos, int radius) {
        if (level.isClientSide || !level.hasChunksAt(pos.offset(-radius-1,-radius-1,-radius-1),pos.offset(radius+1,radius+1,radius+1))) return;
        for (var target : BlockPos.betweenClosed(pos.offset(-radius,-radius,-radius),pos.offset(radius,radius,radius))) {
            var state=level.getBlockState(target);
            if(state.is(ForbiddenContent.TAINT_LEAVES.get())) level.setBlock(target,state.setValue(CHECK_DECAY,true),4);
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var tool=builder.getOptionalParameter(LootContextParams.TOOL);
        var registry=builder.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        if(tool!=null && (tool.getItem() instanceof ShearsItem || EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.SILK_TOUCH),tool)>0)) return List.of(new ItemStack(this));
        int fortune=tool==null?0:EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.FORTUNE),tool);
        int chance=60; if(fortune>0) chance=Math.max(10,chance-(2<<fortune));
        var result=new ArrayList<ItemStack>();var random=builder.getLevel().random;
        if(random.nextInt(chance)==0)result.add(new ItemStack(ForbiddenContent.TAINT_SAPLING.get()));
        if(random.nextInt(20)==0)result.add(new ItemStack(ForbiddenContent.TAINT_FRUIT.get()));
        return result;
    }
}
