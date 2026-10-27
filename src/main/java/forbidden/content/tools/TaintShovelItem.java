package forbidden.content.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;
import forbidden.content.ForbiddenContent;

/** Taint Shovel: cleans flux goo/gas in 11x9x11, mines taint blocks fast, repaired with thaumium. */
public class TaintShovelItem extends ShovelItem implements IRepairable {
    private static final TagKey<Block> TAINT_MATERIAL = TagKey.create(
        net.minecraft.core.registries.Registries.BLOCK,
        ResourceLocation.parse("forbidden:materials/taint")
    );

    public TaintShovelItem() {
        super(ForbiddenToolTiers.TAINT, new Properties().rarity(Rarity.RARE).attributes(
            ShovelItem.createAttributes(ForbiddenToolTiers.TAINT, 1.5F, -3.0F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")))
            || BuiltInRegistries.ITEM.getKey(repair.getItem()).toString().equals("thaumcraft:thaumium_ingot");
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return ForbiddenToolTiers.TAINT.getSpeed();
        }
        if (isTaintMaterial(state)
            && !state.is(ForbiddenContent.TAINT_LOG.get())
            && !state.is(ForbiddenContent.TAINT_PLANKS.get())
            && !state.is(ForbiddenContent.TAINT_STONE.get())) {
            return ForbiddenToolTiers.TAINT.getSpeed();
        }
        return super.getDestroySpeed(stack, state);
    }

    public static boolean isTaintMaterial(BlockState state) {
        if (state.is(TAINT_MATERIAL)) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("forbidden") && id.getPath().startsWith("taint_")
            || id.getNamespace().equals("thaumcraft") && id.getPath().startsWith("taint");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        int purified = 0;

        int x = clickedPos.getX();
        int y = clickedPos.getY();
        int z = clickedPos.getZ();

        // Clear flux goo/gas in 11x9x11 volume
        for (int ex = x - 5; ex <= x + 5; ex++) {
            for (int wy = y - 4; wy <= y + 4; wy++) {
                for (int zee = z - 5; zee <= z + 5; zee++) {
                    BlockPos target = new BlockPos(ex, wy, zee);
                    BlockState targetState = level.getBlockState(target);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());
                    if (id.getNamespace().equals("thaumcraft") && (id.getPath().equals("flux_goo") || id.getPath().equals("flux_gas"))) {
                        purified++;
                        // removeBlock restores the fluid's legacy block instead of clearing it.
                        // Match TC4 setBlockToAir, with world changes owned by the server.
                        if (!level.isClientSide) level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                        // The release consumes three coordinate samples for every block,
                        // even when the dedicated-server proxy renders nothing.
                        float endX = ex + level.random.nextFloat();
                        float endY = wy + level.random.nextFloat();
                        float endZ = zee + level.random.nextFloat();
                        if (player != null) thaumcraft.api.client.ParticleApi.nodeBolt(level,
                                (float) player.getX(), (float) player.getY(), (float) player.getZ(),
                                endX, endY, endZ);
                    }
                }
            }
        }

        if (purified > 0) {
            if (!level.isClientSide && player != null) {
                context.getItemInHand().hurtAndBreak(Math.min(purified, 15), player, LivingEntity.getSlotForHand(context.getHand()));
                player.swing(context.getHand());
            }
            if (!level.isClientSide) level.playSound(null, clickedPos,
                SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wandfail")),
                SoundSource.PLAYERS, 0.2F, 0.2F + level.random.nextFloat() * 0.2F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }
}
