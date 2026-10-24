package forbidden.content;

import java.util.Random;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class TaintCoalItem extends Item {
    private final Random random = new Random();
    public TaintCoalItem(Properties properties) { super(properties); }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) { return 1 + random.nextInt(2400); }
}
