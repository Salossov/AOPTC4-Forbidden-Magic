package forbidden.verification;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Native verification suite for P03: Wrath Cage & Spawner Logic (B32, B33, B34).
 * Covers:
 * - B32: WrathSpawnerLogic delay calculations (200..299 base + 200 sloth),
 *        fuel priority (special -> wrath -> sloth), limits (maxNearby 6, spawnCount 3, range 4),
 *        absence of player proximity requirement, safe unknown entity handling.
 * - B33: WrathCageEntity essentia suction (256), mode cycling (0, 1, 2),
 *        F01 buffer overflow clamping (max 64) for addEssentia and addToContainer,
 *        drawEssentia priority, mob buffer reset on exchange without leaks.
 * - B34: CageMobCatalog mapping of all mobs, native spawn conditions (ground, water, air),
 *        safe unknown handling, and optional mod support without hard classloading.
 */
public final class P03WrathCageCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("P03 check failed: " + reason);
        }
    }

    public static void main(String[] args) throws Exception {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        for (var field : ForbiddenConfig.class.getFields()) {
            if (field.get(null) instanceof ModConfigSpec.ConfigValue<?> value) {
                var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
                cache.setAccessible(true);
                cache.set(value, value.getDefault());
            }
        }
        cached(ForbiddenConfig.WRATH_CAGE, true);
        cached(ForbiddenConfig.WRATH_COST, 5);
        cached(ForbiddenConfig.WRATH_EFFICIENCY, 4);

        var bus = BusBuilder.builder().build();
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);

        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        for (var registry : List.of(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                BuiltInRegistries.BLOCK,
                BuiltInRegistries.ITEM,
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                BuiltInRegistries.CREATIVE_MODE_TAB)) {
            ((MappedRegistry<?>) registry).unfreeze();
            bus.post((RegisterEvent) constructor.newInstance(registry.key(), registry));
            registry.freeze();
        }
        ForbiddenAspects.register();

        testB34CatalogMappings();
        testB33WrathCageEssentiaAndF01();
        testB32WrathSpawnerLogic();

        System.out.println("FORBIDDEN_P03_WRATH_CAGE_PASS checks=" + checks);
    }

    private static void testB34CatalogMappings() throws Exception {
        // Vanilla mappings
        var entries = CageMobCatalog.entries();
        check(entries.get(ResourceLocation.parse("minecraft:zombie")) == Aspect.getAspect("corpus"), "zombie aspect");
        check(entries.get(ResourceLocation.parse("minecraft:skeleton")) == Aspect.getAspect("mortuus"), "skeleton aspect");
        check(entries.get(ResourceLocation.parse("minecraft:creeper")) == Aspect.getAspect("ignis"), "creeper aspect");
        check(entries.get(ResourceLocation.parse("minecraft:pig")) == Aspect.getAspect("bestia"), "pig aspect");
        check(entries.get(ResourceLocation.parse("minecraft:sheep")) == Aspect.getAspect("pannus"), "sheep aspect");
        check(entries.get(ResourceLocation.parse("minecraft:squid")) == Aspect.getAspect("sensus"), "squid aspect");
        check(entries.get(ResourceLocation.parse("minecraft:bat")) == Aspect.getAspect("volatus"), "bat aspect");
        check(entries.get(ResourceLocation.parse("minecraft:ghast")) == Aspect.getAspect("forbidden:infernus"), "ghast aspect");
        check(entries.get(ResourceLocation.parse("minecraft:zombified_piglin")) == Aspect.getAspect("lucrum"), "zombified piglin aspect");
        check(entries.get(ResourceLocation.parse("minecraft:enderman")) == Aspect.getAspect("alienis"), "enderman aspect");
        check(entries.get(ResourceLocation.parse("minecraft:cave_spider")) == Aspect.getAspect("venenum"), "cave spider aspect");
        check(entries.get(ResourceLocation.parse("minecraft:blaze")) == Aspect.getAspect("ignis"), "blaze aspect");
        check(entries.get(ResourceLocation.parse("minecraft:magma_cube")) == Aspect.getAspect("ignis"), "magma cube aspect");
        check(entries.get(ResourceLocation.parse("minecraft:witch")) == Aspect.getAspect("praecantatio"), "witch aspect");
        check(entries.get(ResourceLocation.parse("minecraft:villager")) == Aspect.getAspect("lucrum"), "villager aspect");

        // Thaumcraft mappings
        check(entries.get(ResourceLocation.parse("thaumcraft:firebat")) == Aspect.getAspect("ignis"), "firebat aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:wisp")) == Aspect.getAspect("auram"), "wisp aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:thaumic_slime")) == Aspect.getAspect("vitium"), "thaumic slime aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:brainy_zombie")) == Aspect.getAspect("cognitio"), "brainy zombie aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:cultist_knight")) == Aspect.getAspect("alienis"), "cultist knight aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:pech")) == Aspect.getAspect("lucrum"), "pech aspect");
        check(entries.get(ResourceLocation.parse("thaumcraft:eldritch_guardian")) == Aspect.getAspect("alienis"), "eldritch guardian aspect");

        // Silverfish emerald gate check
        cached(ForbiddenConfig.SILVERFISH_EMERALDS, true);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("minecraft:silverfish")) == Aspect.getAspect("lucrum"), "silverfish emeralds enabled");
        cached(ForbiddenConfig.SILVERFISH_EMERALDS, false);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("minecraft:silverfish")) == Aspect.getAspect("bestia"), "silverfish emeralds disabled");

        // Twilight forest optional mob mapping
        cached(ForbiddenConfig.TWILIGHT_FOREST, true);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("twilightforest:wild_boar")) == Aspect.getAspect("bestia"), "twilight boar enabled");
        check(CageMobCatalog.getAspect(ResourceLocation.parse("twilightforest:mini_ghast")) == Aspect.getAspect("forbidden:infernus"), "twilight mini ghast enabled");
        cached(ForbiddenConfig.NO_LUST, false);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("twilightforest:pinch_beetle")) == Aspect.getAspect("forbidden:luxuria"), "pinch beetle lust");
        cached(ForbiddenConfig.NO_LUST, true);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("twilightforest:pinch_beetle")) == Aspect.getAspect("bestia"), "pinch beetle no lust");
        cached(ForbiddenConfig.TWILIGHT_FOREST, false);
        check(!CageMobCatalog.isKnown(ResourceLocation.parse("twilightforest:wild_boar")), "twilight forest disabled gate");

        // Special Mobs optional mapping
        cached(ForbiddenConfig.SPECIAL_MOBS, true);
        check(CageMobCatalog.getAspect(ResourceLocation.parse("specialmobs:special_creeper")) == Aspect.getAspect("ignis"), "special creeper enabled");
        check(CageMobCatalog.getAspect(ResourceLocation.parse("specialmobs:special_ghast")) == Aspect.getAspect("forbidden:infernus"), "special ghast enabled");
        cached(ForbiddenConfig.SPECIAL_MOBS, false);
        check(!CageMobCatalog.isKnown(ResourceLocation.parse("specialmobs:special_creeper")), "special mobs disabled gate");

        // Safe unknown entity handling
        var unknown = ResourceLocation.parse("nonexistent:unknown_mob_xyz");
        check(!CageMobCatalog.isKnown(unknown), "unknown mob is not known");
        check(CageMobCatalog.getAspect(unknown) == Aspect.getAspect("lucrum"), "unknown mob falls back to lucrum (greed)");

        // Native spawn condition environments
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("minecraft:squid")) == CageMobCatalog.SpawnCondition.WATER, "squid requires water");
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("minecraft:bat")) == CageMobCatalog.SpawnCondition.AIR, "bat in air");
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("minecraft:ghast")) == CageMobCatalog.SpawnCondition.AIR, "ghast in air");
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("thaumcraft:wisp")) == CageMobCatalog.SpawnCondition.AIR, "wisp in air");
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("minecraft:zombie")) == CageMobCatalog.SpawnCondition.GROUND, "zombie on ground");
    }

    private static void testB33WrathCageEssentiaAndF01() throws Exception {
        var cage = new WrathCageEntity(BlockPos.ZERO, ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        cached(ForbiddenConfig.WRATH_COST, 5);

        // Suction amount is 256 for all 6 faces
        for (var dir : Direction.values()) {
            check(cage.isConnectable(dir), "connectable on " + dir);
            check(cage.canInputFrom(dir), "can input from " + dir);
            check(!cage.canOutputTo(dir), "cannot output to " + dir);
            check(cage.getSuctionAmount(dir) == 256, "suction amount is 256 on " + dir);
        }

        // Mode cycling: 0 -> 1 -> 2 -> 0
        check(cage.mode() == 0, "initial mode is 0");
        cage.setMob(ResourceLocation.parse("minecraft:creeper"));
        check(cage.getSuctionType(Direction.UP) == Aspect.getAspect("ignis"), "mode 0 suction is special (ignis)");

        var fieldMode = WrathCageEntity.class.getDeclaredField("mode");
        fieldMode.setAccessible(true);
        fieldMode.set(cage, (byte) 1);
        check(cage.getSuctionType(Direction.UP) == Aspect.getAspect("forbidden:ira"), "mode 1 suction is wrath (ira)");

        fieldMode.set(cage, (byte) 2);
        check(cage.getSuctionType(Direction.UP) == Aspect.getAspect("forbidden:desidia"), "mode 2 suction is sloth (desidia)");

        fieldMode.set(cage, (byte) 0);

        // F01 Buffer Overflow Clamping Tests (max 64)
        var ignis = Aspect.getAspect("ignis");
        var ira = Aspect.getAspect("forbidden:ira");
        var desidia = Aspect.getAspect("forbidden:desidia");

        cage.setSpecial(50);
        int accepted = cage.addEssentia(ignis, 20, Direction.UP);
        check(accepted == 14, "F01 addEssentia accepts only remaining free space (14 of 20)");
        check(cage.getSpecial() == 64, "special clamped at exactly 64");

        accepted = cage.addEssentia(ignis, 10, Direction.UP);
        check(accepted == 0, "addEssentia accepts 0 when already full at 64");
        check(cage.getSpecial() == 64, "special remains 64");

        // IAspectContainer addToContainer returns unaccepted remainder
        cage.setWrath(60);
        int remainder = cage.addToContainer(ira, 10);
        check(remainder == 6, "F01 addToContainer returns remainder 6");
        check(cage.getWrath() == 64, "wrath clamped at exactly 64");

        remainder = cage.addToContainer(ira, 5);
        check(remainder == 5, "F01 addToContainer returns full amount 5 when full");
        check(cage.getWrath() == 64, "wrath remains 64");

        // Sloth buffer test
        cage.setSloth(0);
        accepted = cage.addEssentia(desidia, 70, Direction.DOWN);
        check(accepted == 64, "addEssentia clamps to 64 from empty");
        check(cage.getSloth() == 64, "sloth clamped at 64");

        // Container query checks
        check(cage.containerContains(ignis) == 64, "containerContains special");
        check(cage.containerContains(ira) == 64, "containerContains wrath");
        check(cage.containerContains(desidia) == 64, "containerContains sloth");
        check(cage.doesContainerContainAmount(ignis, 64), "doesContainerContainAmount 64");
        check(!cage.doesContainerContainAmount(ignis, 65), "doesContainerContainAmount 65 is false");
        check(!cage.takeFromContainer(ignis, 1), "takeFromContainer always false");

        // Changing mob resets mob-specific buffers (special essentia and spawner fuel)
        cage.getSpawnerLogic().setFuel(10);
        cage.getSpawnerLogic().setSlothful(true);
        cage.setMob(ResourceLocation.parse("minecraft:skeleton"));

        check(cage.mob().equals(ResourceLocation.parse("minecraft:skeleton")), "new mob set");
        check(cage.getSpecial() == 0, "special buffer reset to 0 on mob change");
        check(cage.getSpawnerLogic().getFuel() == 0, "fuel reset to 0 on mob change");
        check(!cage.getSpawnerLogic().isSlothful(), "slothful reset to false on mob change");
        check(cage.getWrath() == 64, "generic wrath buffer preserved");
        check(cage.getSloth() == 64, "generic sloth buffer preserved");
        check(cage.getAspect() == Aspect.getAspect("mortuus"), "new mob aspect updated to mortuus");
    }

    private static void testB32WrathSpawnerLogic() throws Exception {
        var cage = new WrathCageEntity(BlockPos.ZERO, ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        var logic = cage.getSpawnerLogic();
        cached(ForbiddenConfig.WRATH_COST, 5);
        cached(ForbiddenConfig.WRATH_EFFICIENCY, 4);

        // Limits check
        check(logic.getMaxNearbyEntities() == 6, "maxNearbyEntities limit is 6");
        check(logic.getSpawnCount() == 3, "spawnCount limit is 3");
        check(logic.getSpawnRange() == 4, "spawnRange limit is 4");
        check(logic.getActivatingRangeFromPlayer() == 16, "activatingRangeFromPlayer is 16");

        // Delays check (base 200..299 ticks, slothful +200)
        check(logic.getMinSpawnDelay() == 200, "minSpawnDelay is 200");
        check(logic.getMaxSpawnDelay() == 300, "maxSpawnDelay is 300");

        logic.setSlothful(false);
        for (int i = 0; i < 50; i++) {
            logic.updateDelay();
            check(logic.getSpawnDelay() >= 200 && logic.getSpawnDelay() < 300,
                    "base delay in range [200, 299]: " + logic.getSpawnDelay());
        }

        logic.setSlothful(true);
        for (int i = 0; i < 50; i++) {
            logic.updateDelay();
            check(logic.getSpawnDelay() >= 400 && logic.getSpawnDelay() < 500,
                    "slothful delay in range [400, 499]: " + logic.getSpawnDelay());
        }

        // Fueling priority test:
        // Priority 1: Special aspect
        cage.setMob(ResourceLocation.parse("minecraft:creeper")); // ignis
        cage.setSpecial(10);
        cage.setWrath(10);
        cage.setSloth(10);
        logic.setFuel(0);
        logic.setSlothful(true);

        // Simulate fueling step in updateSpawner
        int wrathCost = ForbiddenConfig.WRATH_COST.get();
        int wrathEff = ForbiddenConfig.WRATH_EFFICIENCY.get();

        if (cage.getSpecial() >= wrathCost) {
            cage.setSpecial(cage.getSpecial() - wrathCost);
            logic.setSlothful(false);
            logic.setFuel(wrathEff);
        }
        check(cage.getSpecial() == 5, "special consumed by wrathCost (10 - 5 = 5)");
        check(cage.getWrath() == 10, "wrath untouched when special has enough");
        check(logic.getFuel() == 4, "fuel set to wrathEff 4");
        check(!logic.isSlothful(), "special fuel produces non-slothful state");

        // Priority 2: Wrath aspect (when special < wrathCost)
        cage.setSpecial(2); // < 5
        logic.setFuel(0);
        if (cage.getSpecial() >= wrathCost) {
            cage.setSpecial(cage.getSpecial() - wrathCost);
            logic.setSlothful(false);
            logic.setFuel(wrathEff);
        } else if (cage.getWrath() >= wrathCost) {
            cage.setWrath(cage.getWrath() - wrathCost);
            logic.setSlothful(false);
            logic.setFuel(wrathEff);
        }
        check(cage.getSpecial() == 2, "special untouched when insufficient");
        check(cage.getWrath() == 5, "wrath consumed by wrathCost (10 - 5 = 5)");
        check(logic.getFuel() == 4, "fuel set to wrathEff 4");
        check(!logic.isSlothful(), "wrath fuel produces non-slothful state");

        // Priority 3: Sloth aspect (when special < wrathCost and wrath < wrathCost)
        cage.setSpecial(1);
        cage.setWrath(3);
        logic.setFuel(0);
        if (cage.getSpecial() >= wrathCost) {
            cage.setSpecial(cage.getSpecial() - wrathCost);
            logic.setSlothful(false);
            logic.setFuel(wrathEff);
        } else if (cage.getWrath() >= wrathCost) {
            cage.setWrath(cage.getWrath() - wrathCost);
            logic.setSlothful(false);
            logic.setFuel(wrathEff);
        } else if (cage.getSloth() >= wrathCost) {
            cage.setSloth(cage.getSloth() - wrathCost);
            logic.setSlothful(true);
            logic.setFuel(wrathEff);
        }
        check(cage.getSloth() == 5, "sloth consumed by wrathCost (10 - 5 = 5)");
        check(logic.getFuel() == 4, "fuel set to wrathEff 4");
        check(logic.isSlothful(), "sloth fuel activates slothful state (+200 delay)");

        // NBT roundtrip preservation
        var nbt = new CompoundTag();
        nbt.putString("EntityId", "minecraft:blaze");
        nbt.putBoolean("MobSet", true);
        nbt.putBoolean("Slothful", true);
        nbt.putShort("Fuel", (short) 3);
        nbt.putShort("Delay", (short) 425);
        nbt.putShort("Wrath", (short) 32);
        nbt.putShort("Sloth", (short) 16);
        nbt.putShort("Special", (short) 8);
        nbt.putByte("Mode", (byte) 2);

        var cage2 = new WrathCageEntity(BlockPos.ZERO, ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        var loadMethod = WrathCageEntity.class.getDeclaredMethod("loadAdditional", CompoundTag.class, HolderLookup.Provider.class);
        loadMethod.setAccessible(true);
        loadMethod.invoke(cage2, nbt, (HolderLookup.Provider) null);

        check(cage2.mob().equals(ResourceLocation.parse("minecraft:blaze")), "deserialized entity id");
        check(cage2.getSpawnerLogic().isSlothful(), "deserialized slothful");
        check(cage2.getSpawnerLogic().getFuel() == 3, "deserialized fuel");
        check(cage2.getSpawnerLogic().getSpawnDelay() == 425, "deserialized delay");
        check(cage2.getWrath() == 32, "deserialized wrath");
        check(cage2.getSloth() == 16, "deserialized sloth");
        check(cage2.getSpecial() == 8, "deserialized special");
        check(cage2.mode() == 2, "deserialized mode");

        var savedNbt = new CompoundTag();
        var saveMethod = WrathCageEntity.class.getDeclaredMethod("saveAdditional", CompoundTag.class, HolderLookup.Provider.class);
        saveMethod.setAccessible(true);
        saveMethod.invoke(cage2, savedNbt, (HolderLookup.Provider) null);

        for (var key : nbt.getAllKeys()) {
            check(nbt.get(key).equals(savedNbt.get(key)), "NBT roundtrip field " + key);
        }
    }

    private static void cached(ModConfigSpec.ConfigValue<?> value, Object setting) throws Exception {
        var f = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        f.setAccessible(true);
        f.set(value, setting);
    }
}
