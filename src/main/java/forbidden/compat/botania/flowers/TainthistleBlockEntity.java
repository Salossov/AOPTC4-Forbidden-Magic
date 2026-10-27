package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.botania.BotaniaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Tainthistle BlockEntity (C03).
 * Generating flower: scans 4 blocks around (9x9x9), consumes flux blocks (gas/goo) up to 4000 burn time,
 * produces 3 Mana every 2 ticks while burning into its 150 buffer, and emits to mana spreaders/pools.
 * Strictly ignores own position and non-flux construction/taint blocks.
 */
public class TainthistleBlockEntity extends BlockEntity {
    public static final int RANGE = 4;
    public static final int BURN_CAP = 4000;
    public static final int SCAN_INTERVAL = 80;
    public static final int MAX_MANA = 150;

    private int burnTime = 0;
    private int ticksExisted = 0;
    private int mana = 0;
    private BlockPos boundPos = null;

    public TainthistleBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.TAINTHISTLE_BE.get(), pos, state);
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = Math.clamp(burnTime, 0, BURN_CAP);
        setChanged();
    }

    public int getMana() {
        return mana;
    }

    public int getGeneratedMana() {
        return mana;
    }

    public int getMaxMana() {
        return MAX_MANA;
    }

    public void addMana(int amount) {
        this.mana = Math.clamp(this.mana + amount, 0, MAX_MANA);
        setChanged();
    }

    public BlockPos getBindingPos() {
        return boundPos;
    }

    public void setBindingPos(BlockPos pos) {
        this.boundPos = pos;
        setChanged();
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
                this.mana = Math.min(MAX_MANA, this.mana + 3);
            }
            setChanged();
        }

        if (this.mana > 0) {
            int sent = BotaniaCompat.emitMana(level, pos, boundPos, this.mana);
            if (sent > 0) {
                this.mana -= sent;
                setChanged();
            }
        }
    }

    private void scanAndConsume(Level level, BlockPos pos) {
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dy = -RANGE; dy <= RANGE; dy++) {
                for (int dz = -RANGE; dz <= RANGE; dz++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    if (target.equals(pos)) continue; // STRICT: never consume self!
                    BlockState targetState = level.getBlockState(target);
                    if (isFlux(targetState)) {
                        int depth = getFluxDepth(targetState);
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                        burnTime = Math.min(BURN_CAP, burnTime + depth * 20);
                        setChanged();
                    }
                }
            }
        }
    }

    public static boolean isFlux(BlockState state) {
        if (state == null || state.isAir()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        return "thaumcraft".equals(id.getNamespace())
                && ("flux_gas".equals(id.getPath()) || "flux_goo".equals(id.getPath()));
    }

    public static int getFluxDepth(BlockState state) {
        if (state == null) return 1;
        for (var prop : state.getProperties()) {
            if ("quanta".equals(prop.getName()) && prop instanceof IntegerProperty intProp) {
                return state.getValue(intProp);
            }
        }
        return 1;
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public void setTicksExisted(int ticks) {
        this.ticksExisted = ticks;
    }

    public void writeCustomData(CompoundTag tag) {
        tag.putInt("mana", mana);
        tag.putInt("burnTime", burnTime);
        tag.putInt("ticksExisted", ticksExisted);
        if (boundPos != null) {
            tag.putInt("boundX", boundPos.getX());
            tag.putInt("boundY", boundPos.getY());
            tag.putInt("boundZ", boundPos.getZ());
            tag.putBoolean("hasBoundPos", true);
        }
    }

    public void readCustomData(CompoundTag tag) {
        this.mana = Math.clamp(tag.getInt("mana"), 0, MAX_MANA);
        this.burnTime = Math.clamp(tag.getInt("burnTime"), 0, BURN_CAP);
        this.ticksExisted = tag.getInt("ticksExisted");
        if (tag.getBoolean("hasBoundPos")) {
            this.boundPos = new BlockPos(tag.getInt("boundX"), tag.getInt("boundY"), tag.getInt("boundZ"));
        } else {
            this.boundPos = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeCustomData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readCustomData(tag);
    }
}
