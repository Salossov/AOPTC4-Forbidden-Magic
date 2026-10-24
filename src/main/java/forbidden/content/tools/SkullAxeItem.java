package forbidden.content.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;

/** Skull Axe: drops 5 types of heads (skeleton, wither skeleton, zombie, creeper, player), sword mechanics. */
public class SkullAxeItem extends SwordItem implements IRepairable {
    public SkullAxeItem() {
        super(ForbiddenToolTiers.SKULL_AXE, new Properties().rarity(Rarity.UNCOMMON).attributes(
            SwordItem.createAttributes(ForbiddenToolTiers.SKULL_AXE, 3, -2.4F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")))
            || BuiltInRegistries.ITEM.getKey(repair.getItem()).toString().equals("thaumcraft:thaumium_ingot");
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }
}
