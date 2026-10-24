package forbidden.compat.bloodmagic.items;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wayoftime.bloodmagic.common.item.BloodOrb;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.common.item.IBloodOrb;
import wayoftime.bloodmagic.core.data.Binding;

/**
 * Eldritch Blood Orb with Blood Magic IBloodOrb and IBindable capability.
 */
public class BmEldritchOrbItem extends BaseEldritchOrbItem implements IBloodOrb, IBindable {

    @Override
    public BloodOrb getOrb(ItemStack stack) {
        return BloodMagicBridge.ELDRITCH_BLOOD_ORB;
    }

    @Override
    public Binding getBinding(ItemStack stack) {
        return Binding.fromStack(stack);
    }

    @Override
    public boolean onBind(Player player, ItemStack stack) {
        return true;
    }
}
