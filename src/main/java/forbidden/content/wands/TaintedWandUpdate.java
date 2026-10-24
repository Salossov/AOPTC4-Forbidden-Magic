package forbidden.content.wands;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;

/**
 * Tainted wand update (B18):
 * Slowly recovers vis in tainted biomes: interval 50 ticks, 1 centivis for each primal aspect up to 15000 (150 vis).
 */
public final class TaintedWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final TaintedWandUpdate INSTANCE = new TaintedWandUpdate();
    private static final ResourceLocation TAINTED_LAND = ResourceLocation.parse("thaumcraft:tainted_land");

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.tickCount % 50 != 0) return;
        var biome = player.level().getBiome(player.blockPosition());
        boolean isTaintBiome = biome.is(TAINTED_LAND)
                || biome.unwrapKey().map(k -> k.location().getPath().contains("taint")).orElse(false);
        if (!isTaintBiome) return;

        for (var aspect : Aspects.PRIMALS) {
            int current = WandApi.stored(wand, aspect);
            int cap = Math.min(15000, WandApi.capacity(wand));
            if (current < cap) {
                WandApi.set(wand, aspect, current + 1);
            }
        }
    }
}
