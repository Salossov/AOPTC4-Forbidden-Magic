package forbidden.compat.botania.wands;

import forbidden.compat.botania.BotaniaCompat;
import forbidden.config.ForbiddenConfig;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;

/**
 * Livingwood / Dreamwood rod update handler (C02).
 * Converts player Mana into primal Vis every 40 ticks for held wands.
 */
public enum YandereWandUpdate implements BiConsumer<ItemStack, Player> {
    INSTANCE;

    @Override
    public void accept(ItemStack stack, Player player) {
        onUpdate(stack, player);
    }

    public void onUpdate(ItemStack stack, Player player) {
        if (!BotaniaCompat.isAvailable() || !ForbiddenConfig.CROSS_WAND.get()) {
            return;
        }
        if (player.tickCount % 40 != 0 || !checkHotbar(stack, player)) {
            return;
        }

        try {
            int baseCost = ForbiddenConfig.MANA_VIS.get();
            var parts = WandApi.parts(stack);
            String capTag = parts != null && parts.cap() != null ? parts.cap() : "";
            if (capTag.contains("manasteel") || capTag.contains("elementium")) {
                baseCost = Math.max(0, baseCost - 2);
            }

            for (var aspect : Aspects.PRIMALS) {
                int maxVis = WandApi.capacity(stack);
                int currentVis = WandApi.stored(stack, aspect);
                int deficit = maxVis - currentVis;
                if (deficit > 0) {
                    int step = Math.min(deficit, 100);
                    int manaNeeded = step * baseCost;
                    if (player.isCreative() || manaNeeded <= 0 || BotaniaCompat.requestManaExact(stack, player, manaNeeded)) {
                        WandApi.set(stack, aspect, currentVis + step);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private boolean checkHotbar(ItemStack stack, Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i) == stack) {
                return true;
            }
        }
        return false;
    }
}
