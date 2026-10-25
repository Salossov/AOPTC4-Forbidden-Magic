package forbidden.content.wands;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.WandApi;

/**
 * Hellfire wand-block trigger (B22):
 * Focus-less wand right-click on Direction.UP of Obsidian or Netherrack.
 * Requires "forbidden:hellfire" research, costs 2 Ignis (200 centivis), checks block permissions,
 * and atomically creates fire above the block with ignite sound and player swing.
 */
public final class HellfireWandTrigger {
    public static boolean trigger(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return false;
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        if (event.getFace() != Direction.UP) return false;

        ItemStack wand = event.getItemStack();
        if (WandApi.kind(wand) == WandApi.Kind.NONE) return false;
        if (!WandApi.focus(wand).isEmpty()) return false;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.NETHERRACK)) return false;

        if (!ResearchApi.knows(player, "forbidden:hellfire")) return false;
        if (!player.level().mayInteract(player, event.getPos())) return false;

        BlockPos above = event.getPos().above();
        if (!player.level().isEmptyBlock(above)) return false;

        Aspect ignis = Aspect.getAspect("ignis");
        if (!WandApi.consume(wand, player, Map.of(ignis, 200), true)) return false;

        BlockState fireState = BaseFireBlock.getState(player.level(), above);
        player.level().setBlock(above, fireState, 11);
        player.level().playSound(null, above, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                1.0f, player.level().random.nextFloat() * 0.4f + 0.8f);
        player.swing(event.getHand(), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    private HellfireWandTrigger() {}
}
