package forbidden.compat.bloodmagic.flowers;

import forbidden.compat.bloodmagic.BloodMagicItems;
import forbidden.compat.botania.BotaniaCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Bloodthorn flower BlockEntity (C08).
 * Matches 0.575 reference SubTileBloodthorn:
 * - 360 Mana buffer
 * - 40 Mana cost per entity afflicted
 * - 6-block radius
 * - Inflicts Weakness III for 60 ticks if not already afflicted
 * - Pauses on redstone signal
 * - No direct damage or LP generation
 */
public class BloodthornBlockEntity extends BlockEntity {
    public static final int MAX_MANA = 360;
    public static final int COST = 40;
    public static final int RANGE = 6;

    private int mana = 0;
    private int ticksExisted = 0;
    private BlockPos boundPos = null;

    public BloodthornBlockEntity(BlockPos pos, BlockState state) {
        super(BloodMagicItems.BLOODTHORN_BE.get(), pos, state);
    }

    public int getMana() {
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
        if (level.isClientSide()) {
            return;
        }
        ticksExisted++;

        if (level.hasNeighborSignal(pos)) {
            return;
        }

        if (mana < MAX_MANA) {
            int drawn = BotaniaCompat.drawMana(level, pos, boundPos, MAX_MANA - mana);
            if (drawn > 0) {
                mana += drawn;
                setChanged();
            }
        }

        AABB box = new AABB(pos.getX() - RANGE, pos.getY() - RANGE, pos.getZ() - RANGE,
                pos.getX() + RANGE, pos.getY() + RANGE, pos.getZ() + RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && !(e instanceof Player)
        );

        for (LivingEntity target : targets) {
            if (target.getEffect(MobEffects.WEAKNESS) == null && mana >= COST) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
                mana -= COST;
                setChanged();
            }
        }
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public void setTicksExisted(int ticks) {
        this.ticksExisted = ticks;
    }

    public void writeCustomData(CompoundTag tag) {
        tag.putInt("mana", mana);
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
