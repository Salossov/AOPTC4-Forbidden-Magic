package forbidden.content.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.IRepairable;
import forbidden.config.ForbiddenConfig;

/** Diabolist Fork: mob capture into empty mob crystal, Cries Havoc, Spork of Doom easter egg. */
public class DiabolistForkItem extends SwordItem implements IRepairable {
    public DiabolistForkItem() {
        super(ForbiddenToolTiers.FORK, new Properties().attributes(
            SwordItem.createAttributes(ForbiddenToolTiers.FORK, 3, -2.4F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.NETHER_BRICK);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (ForbiddenConfig.SPORK.get()) {
            return Component.translatable("item.DiabolistSpork.name");
        }
        return super.getName(stack);
    }
}
