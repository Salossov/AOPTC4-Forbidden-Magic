package forbidden.content;

import forbidden.config.ForbiddenConfig;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;

/**
 * Spawner logic for the Wrath Cage.
 * Matches original 1.7.10 WrathSpawnerLogic:
 * - Fueling priorities: Special aspect -> Wrath (ira) -> Sloth (desidia)
 * - Delay: base 200..299 ticks + 200 ticks if slothful
 * - Limits: maxNearbyEntities = 6, spawnCount = 3, spawnRange = 4
 * - Redstone disables spawning
 * - No player proximity requirement (spawns regardless of player presence)
 */
public class WrathSpawnerLogic {
    final WrathCageEntity mobSpawnerEntity;
    public boolean mobSet = false;
    public int spawnDelay = 20;
    private ResourceLocation mobID = ResourceLocation.parse("minecraft:pig");
    Aspect aspect = Aspect.getAspect("lucrum");
    private boolean slothful = false;
    private int fuel = 0;
    public double spin;
    public double prevSpin;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 300;
    private int spawnCount = 3;
    private int maxNearbyEntities = 6;
    private int activatingRangeFromPlayer = 16;
    private int spawnRange = 4;
    private boolean customMaxNearby = false;
    private boolean customSpawnRange = false;

    public WrathSpawnerLogic(WrathCageEntity par1TileEntityMobSpawner) {
        this.mobSpawnerEntity = par1TileEntityMobSpawner;
        if (this.aspect == null) {
            this.aspect = Aspect.getAspect("lucrum");
        }
    }

    public ResourceLocation getMobID() {
        return this.mobID;
    }

    public String getEntityNameToSpawn() {
        return this.mobID == null ? "minecraft:pig" : this.mobID.toString();
    }

    public Aspect getAspect() {
        if (this.aspect == null) {
            this.aspect = Aspect.getAspect("lucrum");
        }
        return this.aspect;
    }

    public void setMobID(ResourceLocation id) {
        this.mobID = id;
        this.aspect = CageMobCatalog.getAspect(this.mobID);
        if (this.aspect == null) {
            this.aspect = Aspect.getAspect("lucrum");
        }
    }

    public void setMobID(String par1Str) {
        setMobID(par1Str == null || par1Str.isEmpty() ? null : ResourceLocation.tryParse(par1Str));
    }

    public void mobIsSet(boolean inp) {
        this.mobSet = inp;
    }

    public boolean isMobSet() {
        return this.mobSet;
    }

    public int getFuel() {
        return this.fuel;
    }

    public void setFuel(int fuel) {
        this.fuel = fuel;
    }

    public boolean isSlothful() {
        return this.slothful;
    }

    public void setSlothful(boolean slothful) {
        this.slothful = slothful;
    }

    public int getSpawnDelay() {
        return this.spawnDelay;
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public int getMinSpawnDelay() {
        return this.minSpawnDelay;
    }

    public void setMinSpawnDelay(int minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
    }

    public int getMaxSpawnDelay() {
        return this.maxSpawnDelay;
    }

    public void setMaxSpawnDelay(int maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
    }

    public int getSpawnCount() {
        return this.spawnCount;
    }

    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    public int getMaxNearbyEntities() {
        return this.maxNearbyEntities;
    }

    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
        this.customMaxNearby = true;
    }

    public int getSpawnRange() {
        return this.spawnRange;
    }

    public void setSpawnRange(int spawnRange) {
        this.spawnRange = spawnRange;
        this.customSpawnRange = true;
    }

    public int getActivatingRangeFromPlayer() {
        return this.activatingRangeFromPlayer;
    }

    public void updateSpawnerBlock(int par1) {
        this.mobSpawnerEntity.sync();
    }

    public Level getSpawnerWorld() {
        return this.mobSpawnerEntity.getLevel();
    }

    public BlockPos getSpawnerPos() {
        return this.mobSpawnerEntity.getBlockPos();
    }

    public int getSpawnerX() {
        return this.mobSpawnerEntity.getBlockPos().getX();
    }

    public int getSpawnerY() {
        return this.mobSpawnerEntity.getBlockPos().getY();
    }

    public int getSpawnerZ() {
        return this.mobSpawnerEntity.getBlockPos().getZ();
    }

    public void updateSpawner() {
        if (!this.mobSet) return;
        Level world = getSpawnerWorld();
        if (world == null) return;

        int wrathCost = ForbiddenConfig.WRATH_COST.get();
        int wrathEff = ForbiddenConfig.WRATH_EFFICIENCY.get();

        if (world.isClientSide) {
            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            }
            this.prevSpin = this.spin;
            this.spin = (this.spin + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0;
            return;
        }

        // Server-side
        if (!(world instanceof ServerLevel serverLevel)) return;
        if (!ForbiddenConfig.WRATH_CAGE.get()) return;

        // Redstone check: stops working if powered
        if (world.hasNeighborSignal(getSpawnerPos())) return;

        // Fueling priorities: Special aspect -> Wrath (ira) -> Sloth (desidia)
        if (wrathCost > 0 && this.fuel <= 0) {
            if (this.mobSpawnerEntity.getSpecial() >= wrathCost) {
                this.mobSpawnerEntity.setSpecial(this.mobSpawnerEntity.getSpecial() - wrathCost);
                this.slothful = false;
                this.fuel = wrathEff;
                this.mobSpawnerEntity.sync();
            } else if (this.mobSpawnerEntity.getWrath() >= wrathCost) {
                this.mobSpawnerEntity.setWrath(this.mobSpawnerEntity.getWrath() - wrathCost);
                this.slothful = false;
                this.fuel = wrathEff;
                this.mobSpawnerEntity.sync();
            } else if (this.mobSpawnerEntity.getSloth() >= wrathCost) {
                this.mobSpawnerEntity.setSloth(this.mobSpawnerEntity.getSloth() - wrathCost);
                this.slothful = true;
                this.fuel = wrathEff;
                this.mobSpawnerEntity.sync();
            }
        }

        if (this.spawnDelay == -1) {
            this.updateDelay();
        }

        if (this.fuel <= 0 && wrathCost > 0) {
            return;
        }

        if (this.spawnDelay > 0) {
            this.spawnDelay--;
            return;
        }

        if (this.mobID == null) return;
        var optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(this.mobID);
        if (optionalType.isEmpty()) {
            return; // Safe unknown handling
        }
        EntityType<?> entityType = optionalType.get();

        for (int i = 0; i < this.spawnCount && (this.fuel > 0 || wrathCost <= 0); i++) {
            AABB aabb = new AABB(
                getSpawnerX(), getSpawnerY(), getSpawnerZ(),
                getSpawnerX() + 1, getSpawnerY() + 1, getSpawnerZ() + 1
            ).inflate(this.spawnRange * 2, 4.0, this.spawnRange * 2);

            int nearby = serverLevel.getEntities(entityType, aabb, Entity::isAlive).size();
            if (nearby >= this.maxNearbyEntities) {
                this.updateDelay();
                return;
            }

            double d0 = getSpawnerX() + (serverLevel.random.nextDouble() - serverLevel.random.nextDouble()) * this.spawnRange;
            double d3 = getSpawnerY() + serverLevel.random.nextInt(3) - 1;
            double d4 = getSpawnerZ() + (serverLevel.random.nextDouble() - serverLevel.random.nextDouble()) * this.spawnRange;

            Entity entity = entityType.create(serverLevel);
            if (entity == null) return;

            entity.moveTo(d0, d3, d4, serverLevel.random.nextFloat() * 360.0F, 0.0F);
            if (this.entityCanSpawn(entity)) {
                this.spawnMob(entity);
                serverLevel.levelEvent(2004, getSpawnerPos(), 0);
                if (entity instanceof Mob mob) {
                    mob.spawnAnim();
                    this.fuel--;
                }
                this.updateDelay();
            } else {
                entity.discard();
            }
        }
    }

    public boolean entityCanSpawn(Entity entity) {
        if (entity == null || getSpawnerWorld() == null) return false;
        Level world = getSpawnerWorld();
        if (!world.noCollision(entity)) return false;

        var condition = CageMobCatalog.getSpawnCondition(this.mobID);
        boolean inLiquid = world.containsAnyLiquid(entity.getBoundingBox());
        if (condition == CageMobCatalog.SpawnCondition.WATER || entity instanceof net.minecraft.world.entity.animal.WaterAnimal) {
            if (!inLiquid) return false;
        } else {
            if (inLiquid) return false;
        }
        if (entity instanceof Mob mob) {
            return mob.checkSpawnObstruction(world);
        }
        return true;
    }

    public void updateDelay() {
        Level world = getSpawnerWorld();
        net.minecraft.util.RandomSource random = world != null ? world.random : net.minecraft.util.RandomSource.create();
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            int i = this.maxSpawnDelay - this.minSpawnDelay;
            this.spawnDelay = this.minSpawnDelay + random.nextInt(i);
        }

        if (this.slothful) {
            this.spawnDelay += 200;
        }

        this.updateSpawnerBlock(1);
    }

    public Entity spawnMob(Entity entity) {
        Level world = getSpawnerWorld();
        if (entity instanceof Mob mob && world instanceof ServerLevel serverLevel) {
            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWNER, null);
            serverLevel.addFreshEntity(entity);
        } else if (entity != null && world instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(entity);
        }
        return entity;
    }

    public boolean setDelayToMin(int par1) {
        Level world = getSpawnerWorld();
        if (par1 == 1 && world != null && world.isClientSide) {
            this.spawnDelay = this.minSpawnDelay;
            return true;
        } else {
            return false;
        }
    }

    public void readFromNBT(CompoundTag tag) {
        String idStr = tag.getString("EntityId");
        this.mobID = idStr.isEmpty() ? ResourceLocation.parse("minecraft:pig") : ResourceLocation.tryParse(idStr);
        if (this.mobID == null) {
            this.mobID = ResourceLocation.parse("minecraft:pig");
        }
        this.aspect = CageMobCatalog.getAspect(this.mobID);
        if (this.aspect == null) {
            this.aspect = Aspect.getAspect("lucrum");
        }

        this.mobSet = tag.getBoolean("MobSet");
        this.slothful = tag.getBoolean("Slothful");
        this.fuel = tag.getShort("Fuel");
        this.spawnDelay = tag.contains("Delay") ? tag.getShort("Delay") : 20;

        if (tag.contains("MaxNearbyEntities")) {
            this.maxNearbyEntities = tag.getShort("MaxNearbyEntities");
            this.customMaxNearby = true;
        }
        if (tag.contains("SpawnRange")) {
            this.spawnRange = tag.getShort("SpawnRange");
            this.customSpawnRange = true;
        }
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putString("EntityId", this.getEntityNameToSpawn());
        tag.putBoolean("MobSet", this.mobSet);
        tag.putBoolean("Slothful", this.slothful);
        tag.putShort("Fuel", (short) this.fuel);
        tag.putShort("Delay", (short) this.spawnDelay);
        if (this.customMaxNearby || tag.contains("MaxNearbyEntities")) {
            tag.putShort("MaxNearbyEntities", (short) this.maxNearbyEntities);
        }
        if (this.customSpawnRange || tag.contains("SpawnRange")) {
            tag.putShort("SpawnRange", (short) this.spawnRange);
        }
    }
}
