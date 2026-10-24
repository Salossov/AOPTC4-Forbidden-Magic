package forbidden.content.wands;

import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;

/**
 * Creative wand/staff update (B20):
 * Refills all primal vis to maximum capacity every tick.
 */
public final class CreativeWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final CreativeWandUpdate INSTANCE = new CreativeWandUpdate();

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player.level().isClientSide()) return;
        int max = WandApi.capacity(wand);
        for (var aspect : Aspects.PRIMALS) {
            if (WandApi.stored(wand, aspect) < max) {
                WandApi.set(wand, aspect, max);
            }
        }
    }
}
