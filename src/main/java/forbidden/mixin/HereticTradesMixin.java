package forbidden.mixin;

import forbidden.villager.ForbiddenVillagers;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the release merchant inventory on the actual lazy vanilla creation path. */
@Mixin(Villager.class)
public abstract class HereticTradesMixin {
    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void forbidden$releaseOffers(CallbackInfo callback) {
        if (ForbiddenVillagers.populateReleaseOffers((Villager) (Object) this)) {
            callback.cancel();
        }
    }
}
