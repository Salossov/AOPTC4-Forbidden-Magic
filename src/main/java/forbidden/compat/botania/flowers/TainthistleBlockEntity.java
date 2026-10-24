package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tainthistle BlockEntity (C03).
 * Generating flower: scans 4 blocks around (9x9x9), consumes flux/taint blocks up to 4000 burn time,
 * produces 3 Mana every 2 ticks while burning.
 */
public class TainthistleBlockEntity extends BlockEntity {
    public static final int RANGE = 4;
    public static final int BURN_CAP = 4000;
    public static final int SCAN_INTERVAL = 80;

    private int burnTime = 0;
    private int ticksExisted = 0;
    private int generatedMana = 0;

    public TainthistleBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.TAINTHISTLE_BE.get(), pos, state);
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = Math.min(BURN_CAP, burnTime);
        setChanged();
    }

    public int getGeneratedMana() {
        return generatedMana;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        ticksExisted++;

        if (ticksExisted % SCAN_INTERVAL == 0) {
            scanAndConsume(level, pos);
        }

        if (burnTime > 0) {
            burnTime--;
            if (ticksExisted % 2 == 0) {
                generatedMana += 3;
            }
            setChanged();
        }
    }

    private void scanAndConsume(Level level, BlockPos pos) {
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    if (burnTime >= BURN_CAP) return;
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState targetState = level.getBlockState(target);
                    if (isTaintOrFlux(targetState)) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                        burnTime = Math.min(BURN_CAP, burnTime + 20);
                        setChanged();
                    }
                }
            }
        }
    }

    private boolean isTaintOrFlux(BlockState state) {
        if (state.isAir()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return id.getNamespace().equals("thaumcraft") && (path.contains("taint") || path.contains("flux"));
    }
}
