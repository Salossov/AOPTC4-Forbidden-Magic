package forbidden.content.wands;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandApi;

/**
 * Infernal wand update (B18):
 * Recharges Ignis up to 20% max capacity every 100 ticks.
 * In the Nether: also charges other primals up to 10% max capacity every 100 ticks.
 * When player is on fire: extinguishes player.
 * When player has Wither: removes effect.
 */
public final class InfernalWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final InfernalWandUpdate INSTANCE = new InfernalWandUpdate();
    private static final List<Aspect> NON_FIRE_PRIMALS = List.of(
            Aspect.getAspect("ordo"),
            Aspect.getAspect("perditio"),
            Aspect.getAspect("aer"),
            Aspect.getAspect("terra"),
            Aspect.getAspect("aqua")
    );

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player.level().isClientSide()) return;

        // Extinguish player
        if (player.isOnFire()) {
            player.clearFire();
        }

        // Remove wither effect
        if (player.hasEffect(MobEffects.WITHER)) {
            player.removeEffect(MobEffects.WITHER);
        }

        // 100-tick interval charging
        if (player.tickCount % 100 == 0) {
            if (player.level().dimension() == Level.NETHER) {
                for (var aspect : NON_FIRE_PRIMALS) {
                    int current = WandApi.stored(wand, aspect);
                    if (current < WandApi.capacity(wand) / 10) {
                        WandApi.set(wand, aspect, current + 1);
                    }
                }
            }
            var ignis = Aspect.getAspect("ignis");
            int current = WandApi.stored(wand, ignis);
            if (current < WandApi.capacity(wand) / 5) {
                WandApi.set(wand, ignis, current + 1);
            }
        }
    }
}
