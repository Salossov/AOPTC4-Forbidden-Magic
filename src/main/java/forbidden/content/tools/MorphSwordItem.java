package forbidden.content.tools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;

/** Chameleon Sword: 3 phase cycles, separate enchantments/names, Voidtouched repair. */
public class MorphSwordItem extends SwordItem implements IRepairable, IWarpingGear {
    public MorphSwordItem() {
        super(ForbiddenToolTiers.MORPH, new Properties().rarity(Rarity.EPIC).attributes(
            SwordItem.createAttributes(ForbiddenToolTiers.MORPH, 3, -2.4F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return MorphToolHelper.isValidRepair(repair);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return MorphToolHelper.cyclePhase(player.getItemInHand(hand), player, level, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        MorphToolHelper.inventoryTick(stack, level, entity);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return MorphToolHelper.getWarp(stack, player);
    }
}
