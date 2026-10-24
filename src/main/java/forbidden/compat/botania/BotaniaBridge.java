package forbidden.compat.botania;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.mana.ManaItemHandler;

/**
 * Isolated bridge to Botania API (C01). Loaded ONLY when Botania is active.
 */
public final class BotaniaBridge {
    public static boolean requestManaExactForTool(ItemStack stack, Player player, int mana) {
        return ManaItemHandler.instance().requestManaExactForTool(stack, player, mana, true);
    }

    private BotaniaBridge() {}
}
