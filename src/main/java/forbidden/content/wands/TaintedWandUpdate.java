package forbidden.content.wands;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;

/**
 * Tainted wand update (B18):
 * Recovers one whole vis per primal every 100 ticks in the tainted biome,
 * while that aspect is below one tenth of the wand capacity (TC4 addVis units).
 */
public final class TaintedWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final TaintedWandUpdate INSTANCE = new TaintedWandUpdate();
    private static final ResourceLocation TAINTED_LAND = ResourceLocation.parse("thaumcraft:tainted_land");

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.tickCount % 100 != 0) return;
        var biome = player.level().getBiome(player.blockPosition());
        boolean isTaintBiome = biome.is(TAINTED_LAND);
        if (!isTaintBiome) return;

        for (var aspect : Aspects.PRIMALS) {
            int current = WandApi.stored(wand, aspect);
            int capacity = WandApi.capacity(wand);
            if (current < capacity / 10) {
                WandApi.set(wand, aspect, Math.min(capacity, current + WandApi.centivis(1)));
            }
        }
    }
}
