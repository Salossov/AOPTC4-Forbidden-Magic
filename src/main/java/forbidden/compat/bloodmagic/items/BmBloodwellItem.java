package forbidden.compat.bloodmagic.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.core.data.Binding;

/**
 * Bloodwell with Blood Magic IBindable capability.
 */
public class BmBloodwellItem extends BaseBloodwellItem implements IBindable {
    @Override
    public Binding getBinding(ItemStack stack) {
        return Binding.fromStack(stack);
    }

    @Override
    public boolean onBind(Player player, ItemStack stack) {
        return true;
    }
}
