package forbidden.compat.bloodmagic.wands;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Recharges staff primal vis by draining LP from the owner's soul network at 4x rate (every 25 ticks) (C06).
 */
public final class BloodStaffUpdate implements BiConsumer<ItemStack, Player> {
    public static final BloodStaffUpdate INSTANCE = new BloodStaffUpdate();

    @Override
    public void accept(ItemStack staff, Player player) {
        if (player == null || staff == null || staff.isEmpty()) return;
        if (BloodMagicCompat.isLoaded() && forbidden.config.ForbiddenConfig.CROSS_WAND.get()) {
            BloodMagicBridge.updateBloodStaff(staff, player);
        }
    }

    private BloodStaffUpdate() {}
}
