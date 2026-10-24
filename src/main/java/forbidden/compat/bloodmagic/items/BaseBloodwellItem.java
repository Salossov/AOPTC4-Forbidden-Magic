package forbidden.compat.bloodmagic.items;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.component.ForbiddenComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IScribeTools;

/**
 * Bloodwell: Bloody Scrivener's Tools (C07).
 * Restores ink via Soul Network LP or direct sacrifice of player health.
 * Base class safe for ClassLoader when Blood Magic is not installed.
 */
public class BaseBloodwellItem extends Item implements IScribeTools {

    public BaseBloodwellItem() {
        super(new Item.Properties().stacksTo(1).durability(5));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        bind(stack, player, level);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    protected void bind(ItemStack stack, Player player, Level level) {
        stack.set(ForbiddenComponents.BLOOD_OWNER.get(), player.getGameProfile().getName());
        if (BloodMagicCompat.isLoaded()) {
            BloodMagicBridge.checkAndSetOwner(stack, player);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (stack.getDamageValue() > 0) {
            if (player.isCreative()) {
                stack.setDamageValue(0);
            } else if (BloodMagicCompat.isLoaded() && BloodMagicBridge.syphonFromPlayerNetwork(player, 25)) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            } else if (player.getHealth() > 6.0f) {
                player.setHealth(player.getHealth() - 2.0f);
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (damage > 0 && BloodMagicCompat.isLoaded()) {
            if (BloodMagicBridge.syphonFromStackOrNetwork(stack, null, 25 * damage)) {
                super.setDamage(stack, 0);
                return;
            }
        }
        super.setDamage(stack, damage);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String owner = stack.get(ForbiddenComponents.BLOOD_OWNER.get());
        if (owner == null && BloodMagicCompat.isLoaded()) {
            owner = BloodMagicBridge.getBindingOwner(stack);
        }
        if (owner != null && !owner.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.currentowner").append(" ").append(owner).withStyle(ChatFormatting.GRAY));
        }
    }
}
