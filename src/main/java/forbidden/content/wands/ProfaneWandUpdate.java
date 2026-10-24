package forbidden.content.wands;

import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;

/**
 * Profane wand contract update (B19):
 * Charges wand up to 25000 centivis contract deficit, granting ROD_profane research,
 * applying warp via ResearchApi.addWarp (K01), and transforming rod into profaned when fulfilled.
 */
public final class ProfaneWandUpdate implements BiConsumer<ItemStack, Player> {
    public static final ProfaneWandUpdate INSTANCE = new ProfaneWandUpdate();
    public static final int CONTRACT_TOTAL = 25000;

    @Override
    public void accept(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        var customData = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.contains("contract")) {
            if (ResearchApi.exists(serverPlayer, "ROD_profane")) {
                ResearchApi.grant(serverPlayer, "ROD_profane");
            }
            customData.putInt("contract", CONTRACT_TOTAL);
            CustomData.update(DataComponents.CUSTOM_DATA, wand, tag -> tag.putInt("contract", CONTRACT_TOTAL));
        }

        int contract = customData.getInt("contract");
        if (contract <= 0) return;

        for (var aspect : Aspects.PRIMALS) {
            if (contract <= 0) break;
            int max = WandApi.capacity(wand);
            int cur = WandApi.stored(wand, aspect);
            int deficit = max - cur;
            if (deficit > 0) {
                int restore = Math.min(contract, deficit);
                WandApi.set(wand, aspect, cur + restore);
                contract -= restore;
                if (serverPlayer.serverLevel().random.nextInt(2501) < restore) {
                    ResearchApi.addWarp(serverPlayer, ResearchApi.WarpKind.STICKY, 1);
                }
            }
        }

        if (contract <= 0) {
            var parts = WandApi.parts(wand);
            WandApi.parts(wand, new WandParts("forbidden:profaned", parts.cap()));
            ResearchApi.addWarp(serverPlayer, ResearchApi.WarpKind.STICKY, 1);
            serverPlayer.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    serverPlayer.getX(), serverPlayer.getY() + serverPlayer.getBbHeight() / 2.0, serverPlayer.getZ(),
                    1, 0, 0, 0, 0);
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        final int finalContract = contract;
        CustomData.update(DataComponents.CUSTOM_DATA, wand, tag -> tag.putInt("contract", finalContract));
    }
}
