package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.botania.BotaniaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;

/**
 * Euclidaisy BlockEntity (C03).
 * Consumes 9000 Mana every 400 ticks to synthesize celestial Wisp Essence.
 */
public class EuclidaisyBlockEntity extends BlockEntity {
    public static final int COST = 9000;
    public static final int INTERVAL = 400;

    private int ticksExisted = 0;
    private int mana = COST;

    public EuclidaisyBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.EUCLIDAISY_BE.get(), pos, state);
    }

    public int getMana() {
        return mana;
    }

    public void addMana(int amount) {
        this.mana = Math.min(COST, this.mana + amount);
        setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        ticksExisted++;

        if (level.hasNeighborSignal(pos)) {
            return;
        }

        if (ticksExisted % INTERVAL == 0) {
            if (mana >= COST) {
                mana -= COST;
                spawnEssence(level, pos);
                setChanged();
            }
        }
    }

    private void spawnEssence(Level level, BlockPos pos) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:wisp_essence"));
        ItemStack stack = (item != null && item != Items.AIR) ? new ItemStack(item) : new ItemStack(Items.GLOWSTONE_DUST);

        List<Aspect> allAspects = new ArrayList<>(Aspect.all().values());
        Aspect aspect;
        if (level.random.nextInt(10) < 4 || allAspects.isEmpty()) {
            aspect = Aspect.all().get("aura");
            if (aspect == null && !allAspects.isEmpty()) aspect = allAspects.get(0);
        } else {
            aspect = allAspects.get(level.random.nextInt(allAspects.size()));
        }

        double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.4D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.4D;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
