package forbidden.content.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.world.IBlockStartBreakTool;

/** Chameleon Pickaxe: 3 phase cycles, separate enchantments/names, 3x3 Impact, Voidtouched repair. */
public class MorphPickaxeItem extends PickaxeItem implements IRepairable, IWarpingGear, IBlockStartBreakTool {
    private Direction lastFace;

    public MorphPickaxeItem() {
        super(ForbiddenToolTiers.MORPH, new Properties().rarity(Rarity.EPIC).attributes(
            PickaxeItem.createAttributes(ForbiddenToolTiers.MORPH, 1.0F, -2.8F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return MorphToolHelper.isValidRepair(repair);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return MorphToolHelper.cyclePhase(player.getItemInHand(hand), player, level, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        MorphToolHelper.inventoryTick(stack, level, entity);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return MorphToolHelper.getWarp(stack, player);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, ServerPlayer player) {
        HitResult hit = player.pick(20.0D, 0.0F, false);
        if (hit instanceof BlockHitResult bhr) {
            lastFace = bhr.getDirection();
        }
        return false;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        boolean result = super.mineBlock(stack, level, state, pos, miningEntity);
        MorphToolHelper.mineImpact3x3(stack, level, state, pos, miningEntity, lastFace, BlockTags.MINEABLE_WITH_PICKAXE);
        return result;
    }
}
