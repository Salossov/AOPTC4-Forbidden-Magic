package forbidden.content.scribes;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IScribeTools;

/** Primal Scribing Tools: infinite ink, unbreakable scribing tools forged with a primordial pearl. */
public class PrimewellItem extends Item implements IScribeTools {

    public PrimewellItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).durability(100));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 100;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        super.setDamage(stack, 0);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
