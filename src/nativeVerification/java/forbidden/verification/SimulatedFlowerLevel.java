package forbidden.verification;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTicks;
import sun.misc.Unsafe;

public class SimulatedFlowerLevel extends Level {
    public final Map<BlockPos, BlockState> blocks = new HashMap<>();
    public final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    public final Map<BlockPos, Object> capabilities = new HashMap<>();
    public final List<Entity> entities = new ArrayList<>();
    public final List<Entity> spawnedEntities = new ArrayList<>();
    public boolean neighborSignal = false;

    @SuppressWarnings("null")
    protected SimulatedFlowerLevel() {
        super(null, null, null, null, null, false, false, 0L, 0);
    }

    public static SimulatedFlowerLevel create() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            SimulatedFlowerLevel level = (SimulatedFlowerLevel) unsafe.allocateInstance(SimulatedFlowerLevel.class);

            Field blocksField = SimulatedFlowerLevel.class.getDeclaredField("blocks");
            blocksField.setAccessible(true);
            blocksField.set(level, new HashMap<BlockPos, BlockState>());

            Field beField = SimulatedFlowerLevel.class.getDeclaredField("blockEntities");
            beField.setAccessible(true);
            beField.set(level, new HashMap<BlockPos, BlockEntity>());

            Field capField = SimulatedFlowerLevel.class.getDeclaredField("capabilities");
            capField.setAccessible(true);
            capField.set(level, new HashMap<BlockPos, Object>());

            Field entitiesField = SimulatedFlowerLevel.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
            entitiesField.set(level, new ArrayList<Entity>());

            Field spawnedField = SimulatedFlowerLevel.class.getDeclaredField("spawnedEntities");
            spawnedField.setAccessible(true);
            spawnedField.set(level, new ArrayList<Entity>());

            Field randomField = Level.class.getDeclaredField("random");
            randomField.setAccessible(true);
            randomField.set(level, RandomSource.create());

            Field isClientSideField = Level.class.getDeclaredField("isClientSide");
            isClientSideField.setAccessible(true);
            isClientSideField.setBoolean(level, false);

            return level;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate SimulatedFlowerLevel", e);
        }
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState state = blocks.get(pos);
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        blocks.put(pos.immutable(), state);
        return true;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, C> capability, BlockPos pos, BlockState state, BlockEntity blockEntity, C context) {
        Object val = capabilities.get(pos);
        if (val != null) {
            return (T) val;
        }
        return super.getCapability(capability, pos, state, blockEntity, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, C> capability, BlockPos pos, C context) {
        Object val = capabilities.get(pos);
        if (val != null) {
            return (T) val;
        }
        return super.getCapability(capability, pos, context);
    }

    @Override
    public boolean hasNeighborSignal(BlockPos pos) {
        return neighborSignal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB box, Predicate<? super T> filter) {
        List<T> result = new ArrayList<>();
        for (Entity e : entities) {
            if (entityClass.isInstance(e) && box.intersects(e.getBoundingBox())) {
                T cast = (T) e;
                if (filter == null || filter.test(cast)) {
                    result.add(cast);
                }
            }
        }
        return result;
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        spawnedEntities.add(entity);
        return true;
    }

    // Abstract methods required by net.minecraft.world.level.Level
    @Override protected net.minecraft.world.level.entity.LevelEntityGetter<Entity> getEntities() { return null; }
    @Override public Entity getEntity(int id) { return null; }
    @Override public MapItemSavedData getMapData(MapId id) { return null; }
    @Override public void setMapData(MapId id, MapItemSavedData data) {}
    @Override public MapId getFreeMapId() { return null; }
    @Override public void destroyBlockProgress(int id, BlockPos pos, int progress) {}
    @Override public Scoreboard getScoreboard() { return null; }
    @Override public RecipeManager getRecipeManager() { return null; }
    @Override public void gameEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context) {}
    @Override public LevelTicks<Fluid> getFluidTicks() { return null; }
    @Override public LevelTicks<Block> getBlockTicks() { return null; }
    @Override public ChunkSource getChunkSource() { return null; }
    @Override public void levelEvent(Player player, int type, BlockPos pos, int data) {}
    @Override public List<? extends Player> players() { return List.of(); }
    @Override public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) { return null; }
    @Override public FeatureFlagSet enabledFeatures() { return FeatureFlagSet.of(); }
    @Override public float getShade(Direction direction, boolean shade) { return 0.0f; }
    @Override public void sendBlockUpdated(BlockPos pos, BlockState old, BlockState newState, int flags) {}
    @Override public void playSeededSound(Player player, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}
    @Override public void playSeededSound(Player player, Entity entity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}
    @Override public String gatherChunkSourceStats() { return ""; }
    @Override public TickRateManager tickRateManager() { return null; }
    @Override public PotionBrewing potionBrewing() { return null; }
    @Override public float getDayTimePerTick() { return 0.0f; }
    @Override public void setDayTimePerTick(float f) {}
    @Override public float getDayTimeFraction() { return 0.0f; }
    @Override public void setDayTimeFraction(float f) {}
}
