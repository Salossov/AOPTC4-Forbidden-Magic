package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.botania.BotaniaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;

/**
 * Euclidaisy BlockEntity (C03).
 * Consumes 9000 Mana every 400 ticks to synthesize celestial Wisp Essence with authentic aspect NBT.
 */
public class EuclidaisyBlockEntity extends BlockEntity {
    public static final int COST = 9000;
    public static final int INTERVAL = 400;

    private int ticksExisted = 0;
    private int mana = 0;
    private BlockPos boundPos = null;

    public EuclidaisyBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.EUCLIDAISY_BE.get(), pos, state);
    }

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        return COST;
    }

    public void addMana(int amount) {
        this.mana = Math.clamp(this.mana + amount, 0, COST);
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

        if (level.hasNeighborSignal(pos)) {
            return;
        }

        if (mana < COST) {
            int drawn = BotaniaCompat.drawMana(level, pos, boundPos, COST - mana);
            if (drawn > 0) {
                mana += drawn;
                setChanged();
            }
        }

        if (ticksExisted % INTERVAL == 0) {
            if (mana >= COST) {
                mana -= COST;
                spawnEssence(level, pos);
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

    public static ItemStack createWispEssence(Aspect aspect) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:wisp_essence"));
        ItemStack stack = (item != null && item != Items.AIR) ? new ItemStack(item) : new ItemStack(Items.GLOWSTONE_DUST);
        if (aspect != null) {
            CompoundTag data = new CompoundTag();
            data.putString("EssenceAspect", aspect.getTag());
            data.putInt("EssenceAmount", 2);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }

    private void spawnEssence(Level level, BlockPos pos) {
        List<Aspect> allAspects = new ArrayList<>(Aspect.all().values());
        Aspect aspect;
        if (level.random.nextInt(10) < 4 || allAspects.isEmpty()) {
            aspect = Aspect.getAspect("auram");
            if (aspect == null && !allAspects.isEmpty()) aspect = allAspects.get(0);
        } else {
            aspect = allAspects.get(level.random.nextInt(allAspects.size()));
        }

        ItemStack stack = createWispEssence(aspect);

        double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.4D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.4D;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
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
        this.mana = Math.clamp(tag.getInt("mana"), 0, COST);
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
