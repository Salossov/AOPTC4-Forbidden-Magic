package forbidden.compat.bloodmagic.items;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.component.ForbiddenComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.research.ResearchApi;

/**
 * Boundwell: Bound Scribing Tools (C07).
 * Infinite ink consuming Soul Network LP; right-click at 100 damage grants primal research points.
 * Base class safe for ClassLoader when Blood Magic is not installed.
 */
public class BaseBoundwellItem extends Item implements IScribeTools {

    public BaseBoundwellItem() {
        super(new Item.Properties().stacksTo(1).durability(100));
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
    public boolean isFoil(ItemStack stack) {
        return stack.getDamageValue() >= 100;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        bind(stack, player, level);

        if (stack.getDamageValue() >= 100) {
            player.swing(hand);
            if (!level.isClientSide()) {
                if (player.isCreative()) {
                    stack.setDamageValue(0);
                } else if (BloodMagicCompat.isLoaded()) {
                    boolean ok = BloodMagicBridge.syphonAndDamage(player, 10000);
                    if (ok && player.isAlive()) {
                        stack.setDamageValue(0);
                    } else {
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                } else {
                    stack.setDamageValue(0);
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    for (Aspect aspect : Aspects.PRIMALS) {
                        int amount = level.random.nextInt(4) + 4;
                        int current = ResearchApi.knowledge(serverPlayer).pool(aspect);
                        short nextAmount = (short) Math.min(Short.MAX_VALUE, current + amount);
                        ResearchApi.completeAspect(serverPlayer, aspect, nextAmount);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    protected void bind(ItemStack stack, Player player, Level level) {
        stack.set(ForbiddenComponents.BLOOD_OWNER.get(), player.getGameProfile().getName());
        if (BloodMagicCompat.isLoaded()) {
            BloodMagicBridge.checkAndSetOwner(stack, player);
        }
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (damage > 0 && BloodMagicCompat.isLoaded()) {
            if (BloodMagicBridge.syphonFromStackOrNetwork(stack, null, 10 * damage)) {
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
