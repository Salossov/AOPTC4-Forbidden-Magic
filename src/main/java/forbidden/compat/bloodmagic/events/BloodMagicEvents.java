package forbidden.compat.bloodmagic.events;

import forbidden.compat.bloodmagic.BloodMagicCompat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import thaumcraft.api.wands.WandApi;

/**
 * Event listeners for Blood Magic wand mechanics (C06).
 * Alchemical cap applies Weakness III to target on melee hit.
 */
public final class BloodMagicEvents {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!BloodMagicCompat.isLoaded()) return;
        if (event.getEntity().level().isClientSide()) return;

        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (WandApi.kind(held) == WandApi.Kind.NONE) {
                held = player.getOffhandItem();
                if (WandApi.kind(held) == WandApi.Kind.NONE) {
                    return;
                }
            }

            var parts = WandApi.parts(held);
            String cap = parts.cap();
            if ("alchemical".equals(cap) || "forbidden:alchemical".equals(cap)) {
                LivingEntity target = event.getEntity();
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
            }
        }
    }

    private BloodMagicEvents() {}
}
