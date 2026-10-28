package forbidden.compat.botania;

import forbidden.component.ForbiddenComponents;
import forbidden.content.baubles.RingNutritionItem;
import forbidden.content.baubles.SubCollarItem;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.item.CosmeticAttachable;

/** Optional Botania interfaces are resolved only when Botania is installed. */
public final class BotaniaBaubleBridge {
    public static RingNutritionItem ring() { return new Ring(); }
    public static SubCollarItem collar() { return new Collar(); }

    private static void setCosmetic(ItemStack stack, ItemStack cosmetic) {
        if (cosmetic.isEmpty()) stack.remove(ForbiddenComponents.COSMETIC.get());
        else stack.set(ForbiddenComponents.COSMETIC.get(), cosmetic.copyWithCount(1));
    }

    private static final class Ring extends RingNutritionItem implements CosmeticAttachable {
        @Override public ItemStack getCosmeticItem(ItemStack stack) { return ForbiddenComponents.cosmetic(stack); }
        @Override public void setCosmeticItem(ItemStack stack, ItemStack cosmetic) { setCosmetic(stack, cosmetic); }
    }

    private static final class Collar extends SubCollarItem implements CosmeticAttachable {
        @Override public ItemStack getCosmeticItem(ItemStack stack) { return ForbiddenComponents.cosmetic(stack); }
        @Override public void setCosmeticItem(ItemStack stack, ItemStack cosmetic) { setCosmetic(stack, cosmetic); }
    }

    private BotaniaBaubleBridge() { }
}
