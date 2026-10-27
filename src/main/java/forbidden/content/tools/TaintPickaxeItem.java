package forbidden.content.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;

/** Taint Pickaxe: 4 mining speed ranges based on hardness, level 5 harvest, repaired with thaumium. */
public class TaintPickaxeItem extends PickaxeItem implements IRepairable {
    public TaintPickaxeItem() {
        super(ForbiddenToolTiers.TAINT_PICKAXE, new Properties().rarity(Rarity.RARE).attributes(
            PickaxeItem.createAttributes(ForbiddenToolTiers.TAINT_PICKAXE, 1.0F, -2.8F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")))
            || BuiltInRegistries.ITEM.getKey(repair.getItem()).toString().equals("thaumcraft:thaumium_ingot");
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float hardness = state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
        return calculateTaintPickSpeed(hardness);
    }

    public static float calculateTaintPickSpeed(float hardness) {
        if (hardness == 0.0F) {
            return 0.0F;
        } else if (hardness < 5.0F) {
            return 0.1F;
        } else if (hardness < 20.0F) {
            return hardness / 2.0F;
        } else {
            return 5.0F + hardness;
        }
    }
}
