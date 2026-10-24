package forbidden.content.scribes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.research.ResearchApi;

/** Crystal Scribing Tools: absorbs research points, granted upon right-click at 100 damage or slowly over time in easy mode. */
public class CrystalwellItem extends Item implements IScribeTools {

    public CrystalwellItem() {
        super(new Item.Properties().stacksTo(1).durability(100));
    }

    public static boolean isEasyMode(Level level) {
        if (level != null) {
            return level.isClientSide() ? ResearchApi.clientEasyMode() : ResearchApi.serverEasyMode();
        }
        try {
            if (ResearchApi.serverEasyMode()) {
                return true;
            }
        } catch (Throwable ignored) {}
        return ResearchApi.clientEasyMode();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return isEasyMode(null) ? 50 : 100;
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
        return !isEasyMode(null) && stack.getDamageValue() >= 100;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isEasyMode(level) && stack.getDamageValue() >= 100) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                for (Aspect aspect : Aspects.PRIMALS) {
                    int amount = level.random.nextInt(4) + 4;
                    int current = ResearchApi.knowledge(serverPlayer).pool(aspect);
                    short nextAmount = (short) Math.min(Short.MAX_VALUE, current + amount);
                    ResearchApi.completeAspect(serverPlayer, aspect, nextAmount);
                }
            }
            player.swing(hand);
            Item scribingTools = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "scribing_tools"));
            ItemStack exhausted = new ItemStack(scribingTools != Items.AIR ? scribingTools : this, 1);
            exhausted.setDamageValue(100);
            return InteractionResultHolder.sidedSuccess(exhausted, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (isEasyMode(level) && entity.tickCount % 200 == 0) {
            int index = level.random.nextInt(Aspects.PRIMALS.size());
            Aspect aspect = Aspects.PRIMALS.get(index);
            int amount = level.random.nextInt(4) + 1;
            int current = ResearchApi.knowledge(player).pool(aspect);
            short nextAmount = (short) Math.min(Short.MAX_VALUE, current + amount);
            ResearchApi.completeAspect(player, aspect, nextAmount);

            int newDamage = stack.getDamageValue() + 1;
            stack.setDamageValue(newDamage);
            if (newDamage >= stack.getMaxDamage()) {
                stack.shrink(1);
                if (slotId >= 0 && slotId < player.getInventory().getContainerSize() && player.getInventory().getItem(slotId) == stack) {
                    player.getInventory().setItem(slotId, ItemStack.EMPTY);
                }
            }
        }
    }
}
