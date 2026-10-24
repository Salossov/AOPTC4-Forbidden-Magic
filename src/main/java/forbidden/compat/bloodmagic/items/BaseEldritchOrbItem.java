package forbidden.compat.bloodmagic.items;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.component.ForbiddenComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IWarpingGear;

/**
 * Eldritch Orb: Tier 6 Blood Orb (C07).
 * Capacity: 80,000,000 LP.
 * Reusable crafting container item; provides 5 Warp.
 * Base class safe for ClassLoader when Blood Magic is not installed.
 */
public class BaseEldritchOrbItem extends Item implements IWarpingGear {

    public BaseEldritchOrbItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.set(ForbiddenComponents.BLOOD_OWNER.get(), player.getGameProfile().getName());
        if (BloodMagicCompat.isLoaded()) {
            return BloodMagicBridge.useBloodOrb(stack, level, player, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 5;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.divineorb").withStyle(ChatFormatting.GRAY));
        String owner = stack.get(ForbiddenComponents.BLOOD_OWNER.get());
        if (owner == null && BloodMagicCompat.isLoaded()) {
            owner = BloodMagicBridge.getBindingOwner(stack);
        }
        if (owner != null && !owner.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.currentowner").append(" ").append(owner).withStyle(ChatFormatting.GRAY));
        }
    }
}
