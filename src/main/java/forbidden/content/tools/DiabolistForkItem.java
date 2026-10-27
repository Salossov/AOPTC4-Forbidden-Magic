package forbidden.content.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;
import forbidden.config.ForbiddenConfig;

/** Diabolist Fork: mob capture into empty mob crystal, Cries Havoc, Spork of Doom easter egg. */
public class DiabolistForkItem extends SwordItem implements IRepairable {
    public DiabolistForkItem() {
        super(ForbiddenToolTiers.FORK, new Properties().attributes(
            SwordItem.createAttributes(ForbiddenToolTiers.FORK, 3, -2.4F)
        ));
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        return 72000;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return net.minecraft.world.InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, net.minecraft.world.level.Level level,
            net.minecraft.world.entity.LivingEntity user, int remaining) {
        if (!(user instanceof net.minecraft.world.entity.player.Player player)
                || getUseDuration(stack, user) - remaining < 10
                || stack.getDamageValue() >= stack.getMaxDamage() - 1) return;
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(user.getUsedItemHand()));
            var fork = new ThrownDiabolistFork(level, player, stack.copyWithCount(1));
            fork.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.5F, 1);
            if (player.hasInfiniteMaterials()) {
                fork.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            if (level.addFreshEntity(fork)) {
                level.playSound(null, fork, net.minecraft.sounds.SoundEvents.TRIDENT_THROW.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
                if (!player.hasInfiniteMaterials()) player.getInventory().removeItem(stack);
            }
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.NETHER_BRICK);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (ForbiddenConfig.SPORK.get()) {
            return Component.translatable("item.DiabolistSpork.name");
        }
        return super.getName(stack);
    }
}
