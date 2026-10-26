package forbidden.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.IWarpingGear;

/** Release addWarpToItem values exposed through the public core equipment contract. */
final class WarpingBlockItem extends BlockItem implements IWarpingGear {
    private final int warp;

    WarpingBlockItem(Block block, Properties properties, int warp) {
        super(block, properties);
        this.warp = warp;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return warp;
    }
}
