package forbidden.compat.botania;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.botania.api.mana.ManaCollector;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;

/**
 * Isolated bridge to Botania API (C01–C04). Loaded ONLY when Botania is active.
 */
public final class BotaniaBridge {
    public static boolean requestManaExactForTool(ItemStack stack, Player player, int mana) {
        return ManaItemHandler.instance().requestManaExactForTool(stack, player, mana, true);
    }

    public static BlockPos findClosestPool(Level level, BlockPos pos, int radius) {
        if (level == null) return null;
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
            var receiver = ManaReceiver.LOOKUP.find(level, target, null);
            if (receiver instanceof ManaPool) {
                double dist = pos.distSqr(target);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = target.immutable();
                }
            }
        }
        return closest;
    }

    public static BlockPos findClosestCollector(Level level, BlockPos pos, int radius) {
        if (level == null) return null;
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
            var receiver = ManaReceiver.LOOKUP.find(level, target, null);
            if (receiver instanceof ManaCollector) {
                double dist = pos.distSqr(target);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = target.immutable();
                }
            }
        }
        return closest;
    }

    public static int drawManaFromPool(Level level, BlockPos pos, BlockPos boundPos, int needed) {
        if (level == null || needed <= 0) return 0;
        BlockPos target = boundPos;
        if (target == null) {
            target = findClosestPool(level, pos, 10);
        }
        if (target != null) {
            var receiver = ManaReceiver.LOOKUP.find(level, target, null);
            if (receiver instanceof ManaPool pool) {
                int available = pool.getCurrentMana();
                int toDraw = Math.min(needed, available);
                if (toDraw > 0) {
                    pool.receiveMana(-toDraw);
                    return toDraw;
                }
            }
        }
        return 0;
    }

    public static int emptyManaIntoCollector(Level level, BlockPos pos, BlockPos boundPos, int available) {
        if (level == null || available <= 0) return 0;
        BlockPos target = boundPos;
        if (target == null) {
            target = findClosestCollector(level, pos, 6);
        }
        if (target != null) {
            var receiver = ManaReceiver.LOOKUP.find(level, target, null);
            if (receiver instanceof ManaCollector collector && !collector.isFull()) {
                int free = collector.getMaxMana() - collector.getCurrentMana();
                int toSend = Math.min(available, free);
                if (toSend > 0) {
                    collector.receiveMana(toSend);
                    return toSend;
                }
            }
        }
        return 0;
    }

    private BotaniaBridge() {}
}
