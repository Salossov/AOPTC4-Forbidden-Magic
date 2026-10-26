package forbidden.compat.bloodmagic.wands;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Recharges wand primal vis by draining LP from the owner's soul network (C06).
 */
public final class BloodWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final BloodWandUpdate INSTANCE = new BloodWandUpdate();

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player == null || wand == null || wand.isEmpty()) return;
        if (BloodMagicCompat.isLoaded() && forbidden.config.ForbiddenConfig.CROSS_WAND.get()) {
            BloodMagicBridge.updateBloodWand(wand, player);
        }
    }

    private BloodWandUpdate() {}
}
