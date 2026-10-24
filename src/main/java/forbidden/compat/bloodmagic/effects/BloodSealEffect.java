package forbidden.compat.bloodmagic.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Blood Seal effect:
 * Prevents the victim from regenerating or acquiring Life Essence (LP) while active.
 */
public class BloodSealEffect extends MobEffect {
    public BloodSealEffect() {
        super(MobEffectCategory.HARMFUL, 0x7D0600);
    }
}
