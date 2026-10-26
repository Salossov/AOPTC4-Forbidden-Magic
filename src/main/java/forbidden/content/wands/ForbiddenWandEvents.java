package forbidden.content.wands;

import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;

/**
 * Event subscribers for wand triggers, protections, and tooltips (B18, B19, B20, B22).
 */
public final class ForbiddenWandEvents {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        HellfireWandTrigger.trigger(event);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack wand = player.getMainHandItem();
        if (WandApi.kind(wand) == WandApi.Kind.NONE) {
            wand = player.getOffhandItem();
            if (WandApi.kind(wand) == WandApi.Kind.NONE) return;
        }

        var parts = WandApi.parts(wand);
        String rod = parts.rod();

        // Infernal wand (B18): cancels fire and wither damage
        if (rod.equals("infernal") || rod.equals("forbidden:infernal")) {
            var source = event.getSource();
            if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.WITHER)) {
                event.setCanceled(true);
            }
        }

        // TC4 addVis(..., 1, true): one whole vis to each of three random primals.
        if (rod.equals("tainted") || rod.equals("forbidden:tainted")) {
            var source = event.getSource();
            boolean isTaintDamage = source.typeHolder().unwrapKey()
                    .map(k -> k.location().getPath().contains("taint")).orElse(false);
            if (isTaintDamage) {
                var primals = Aspects.PRIMALS;
                for (int i = 0; i < 3; i++) {
                    var aspect = primals.get(RANDOM.nextInt(primals.size()));
                    int cur = WandApi.stored(wand, aspect);
                    if (cur < WandApi.capacity(wand)) {
                        WandApi.set(wand, aspect, Math.min(WandApi.capacity(wand), cur + WandApi.centivis(1)));
                    }
                }
            }
        }
    }

    private ForbiddenWandEvents() {}
}
