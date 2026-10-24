package forbidden.content.baubles;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IRunicArmor;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Ring of Nutrition: adds bonus nutrition and saturation when eating food. */
public class RingNutritionItem extends Item implements ICurioItem, IRunicArmor {
    public RingNutritionItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }
}
