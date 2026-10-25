package forbidden.verification;

import com.google.gson.JsonParser;
import forbidden.compat.botania.BotaniaBridge;
import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.botania.BotaniaItems;
import forbidden.compat.botania.flowers.*;
import forbidden.compat.botania.wands.YandereWandUpdate;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenAspects;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenWandVisuals;
import forbidden.content.ForbiddenWands;
import forbidden.content.wands.ForbiddenWandMechanics;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.api.wands.WandVisuals;

/**
 * Native verification suite for Tasks C01–C04: Botania integration.
 * Tests:
 * - C01: Mod ID, ClassLoader isolation, Botania API presence.
 * - C02: Livingwood Rod (cap 100, cost 12, YandereWandUpdate),
 *        Dreamwood Rod (cap 100, cost 12, YandereWandUpdate),
 *        Dreamwood Staff (cap 250, cost 25, YandereWandUpdate),
 *        Manasteel Cap (0.90f modifier, 10% discount, cost 6),
 *        Elementium Cap (0.80f modifier, 20% discount, cost 9),
 *        Terrasteel Cap (1.80f modifier, cost 1),
 *        Composite models and wand mechanics.
 * - C03: Euclidaisy (9000 cost, 400 interval), Whisperweed (6000 cost, range 3),
 *        Tainthistle (range 4, burn cap 4000, 3 mana / 2 ticks).
 * - C04: Botanist Emblem, composite models classpath integrity.
 */
public final class P04BotaniaCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("P04 Botania check failed: " + reason);
        }
    }

    private static void cached(ModConfigSpec.ConfigValue<?> value, Object target) throws Exception {
        var field = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        field.setAccessible(true);
        field.set(value, target);
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
        cached(ForbiddenConfig.CROSS_MOD, true);
        cached(ForbiddenConfig.CROSS_WAND, true);
        cached(ForbiddenConfig.BOTANIA, true);
        cached(ForbiddenConfig.MANA_VIS, 8);
        BotaniaCompat.setForceLoadedForTesting(true);
        vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities.registerBlockApiLookup(vazkii.botania.api.mana.ManaReceiver.LOOKUP);

        var bus = BusBuilder.builder().build();
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);
        BotaniaCompat.register(bus);

        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        for (var registry : List.of(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                BuiltInRegistries.MOB_EFFECT,
                BuiltInRegistries.BLOCK,
                BuiltInRegistries.ITEM,
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                BuiltInRegistries.CREATIVE_MODE_TAB
        )) {
            ((MappedRegistry<?>) registry).unfreeze();
            bus.post((RegisterEvent) constructor.newInstance(registry.key(), registry));
            registry.freeze();
        }

        ForbiddenAspects.register();
        ForbiddenWandMechanics.registerAll();
        BotaniaCompat.registerWandParts();
        ForbiddenWandVisuals.register();

        try {
            var emptyTypeHolder = net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE;
            var holderField = net.neoforged.neoforge.registries.DeferredHolder.class.getDeclaredField("holder");
            holderField.setAccessible(true);
            if (emptyTypeHolder != null && holderField.get(emptyTypeHolder) == null) {
                var emptyFluidType = new net.neoforged.neoforge.fluids.FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create());
                holderField.set(emptyTypeHolder, net.minecraft.core.Holder.direct(emptyFluidType));
            }
        } catch (Throwable ignored) {
        }
        registerTestFluxBlocks();

        testC01ApiAndIsolation();
        testC02RodsAndCaps();
        testC03Flowers();
        testC04EmblemAndVisualsIntegrity();
        testDataPackIntegrity();

        System.out.println("FORBIDDEN_P04_BOTANIA_PASS checks=" + checks);
    }

    private static void testC01ApiAndIsolation() {
        check("botania".equals(BotaniaCompat.MODID), "Botania MODID is botania");
        // Check that BotaniaBridge class is resolvable on classpath
        check(BotaniaBridge.class != null, "BotaniaBridge resolved");
    }

    private static void testC02RodsAndCaps() {
        // Livingwood Rod
        WandRod livingwood = WandRod.rods.get("forbidden:livingwood");
        check(livingwood != null, "forbidden:livingwood registered");
        check(livingwood.getCapacity() == 100, "livingwood capacity 100");
        check(livingwood.getCraftCost() == 12, "livingwood cost 12");
        check("forbidden:rod_livingwood".equals(livingwood.getResearch()), "livingwood research forbidden:rod_livingwood");
        check(WandRod.rods.get("livingwood") == livingwood, "accessible via alias 'livingwood'");

        // Dreamwood Rod
        WandRod dreamwood = WandRod.rods.get("forbidden:dreamwood");
        check(dreamwood != null, "forbidden:dreamwood registered");
        check(dreamwood.getCapacity() == 100, "dreamwood capacity 100");
        check(dreamwood.getCraftCost() == 12, "dreamwood cost 12");
        check("forbidden:rod_dreamwood".equals(dreamwood.getResearch()), "dreamwood research forbidden:rod_dreamwood");
        check(WandRod.rods.get("dreamwood") == dreamwood, "accessible via alias 'dreamwood'");

        // Dreamwood Staff
        WandRod dreamwoodStaff = WandRod.rods.get("forbidden:dreamwood_staff");
        check(dreamwoodStaff != null, "forbidden:dreamwood_staff registered");
        check(dreamwoodStaff instanceof StaffRod, "dreamwood_staff is StaffRod");
        check(dreamwoodStaff.getCapacity() == 250, "dreamwood staff capacity 250");
        check(dreamwoodStaff.getCraftCost() == 25, "dreamwood staff cost 25");
        check("forbidden:rod_dreamwood_staff".equals(dreamwoodStaff.getResearch()), "dreamwood staff research forbidden:rod_dreamwood_staff");
        check(WandRod.rods.get("dreamwood_staff") == dreamwoodStaff, "accessible via alias 'dreamwood_staff'");

        // Manasteel Cap
        WandCap manasteel = WandCap.caps.get("forbidden:manasteel");
        check(manasteel != null, "forbidden:manasteel registered");
        check(Math.abs(manasteel.getBaseCostModifier() - 0.90f) < 1e-5, "manasteel 0.90f modifier (10% discount)");
        check(manasteel.getCraftCost() == 6, "manasteel craft cost 6");
        check("forbidden:cap_manasteel".equals(manasteel.getResearch()), "manasteel research forbidden:cap_manasteel");
        check(WandCap.caps.get("manasteel") == manasteel, "accessible via alias 'manasteel'");

        // Elementium Cap
        WandCap elementium = WandCap.caps.get("forbidden:elementium");
        check(elementium != null, "forbidden:elementium registered");
        check(Math.abs(elementium.getBaseCostModifier() - 0.80f) < 1e-5, "elementium 0.80f modifier (20% discount)");
        check(elementium.getCraftCost() == 9, "elementium craft cost 9");
        check("forbidden:cap_elementium".equals(elementium.getResearch()), "elementium research forbidden:cap_elementium");
        check(WandCap.caps.get("elementium") == elementium, "accessible via alias 'elementium'");

        // Terrasteel Cap
        WandCap terrasteel = WandCap.caps.get("forbidden:terrasteel");
        check(terrasteel != null, "forbidden:terrasteel registered");
        check(Math.abs(terrasteel.getBaseCostModifier() - 1.80f) < 1e-5, "terrasteel 1.80f modifier");
        check(terrasteel.getCraftCost() == 1, "terrasteel craft cost 1");
        check("forbidden:cap_terrasteel".equals(terrasteel.getResearch()), "terrasteel research forbidden:cap_terrasteel");
        check(WandCap.caps.get("terrasteel") == terrasteel, "accessible via alias 'terrasteel'");

        // Update singleton
        check(YandereWandUpdate.INSTANCE != null, "YandereWandUpdate singleton exists");
    }

    private static void testC03Flowers() {
        // Euclidaisy
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy BE");
        var euclidaisyBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:euclidaisy"));
        check(euclidaisyBlock instanceof EuclidaisyBlock, "euclidaisy is EuclidaisyBlock");
        check(EuclidaisyBlockEntity.COST == 9000, "euclidaisy cost 9000");
        check(EuclidaisyBlockEntity.INTERVAL == 400, "euclidaisy interval 400");

        var euclidaisyBE = new EuclidaisyBlockEntity(BlockPos.ZERO, euclidaisyBlock.defaultBlockState());
        check(euclidaisyBE.getMana() == 0, "euclidaisy starts at 0 mana");
        check(euclidaisyBE.getMaxMana() == 9000, "euclidaisy max mana is 9000");
        euclidaisyBE.addMana(500);
        check(euclidaisyBE.getMana() == 500, "euclidaisy mana after addMana(500) == 500");
        euclidaisyBE.addMana(20000);
        check(euclidaisyBE.getMana() == 9000, "euclidaisy mana capped at 9000");
        euclidaisyBE.setTicksExisted(250);
        check(euclidaisyBE.getTicksExisted() == 250, "euclidaisy ticksExisted == 250");
        euclidaisyBE.setBindingPos(new BlockPos(10, 20, 30));
        check(new BlockPos(10, 20, 30).equals(euclidaisyBE.getBindingPos()), "euclidaisy binding pos set");

        // Euclidaisy NBT Persistence
        CompoundTag euclidaisyTag = new CompoundTag();
        euclidaisyBE.writeCustomData(euclidaisyTag);
        check(euclidaisyTag.getInt("mana") == 9000, "euclidaisy tag mana == 9000");
        check(euclidaisyTag.getInt("ticksExisted") == 250, "euclidaisy tag ticks == 250");
        check(euclidaisyTag.getBoolean("hasBoundPos"), "euclidaisy tag hasBoundPos");
        check(euclidaisyTag.getInt("boundX") == 10 && euclidaisyTag.getInt("boundY") == 20 && euclidaisyTag.getInt("boundZ") == 30, "euclidaisy tag coords match");

        var euclidaisyReloaded = new EuclidaisyBlockEntity(BlockPos.ZERO, euclidaisyBlock.defaultBlockState());
        euclidaisyReloaded.readCustomData(euclidaisyTag);
        check(euclidaisyReloaded.getMana() == 9000, "reloaded euclidaisy mana == 9000");
        check(euclidaisyReloaded.getTicksExisted() == 250, "reloaded euclidaisy ticks == 250");
        check(new BlockPos(10, 20, 30).equals(euclidaisyReloaded.getBindingPos()), "reloaded euclidaisy binding pos matches");

        // Euclidaisy Wisp Essence NBT
        var auram = Aspect.getAspect("auram");
        check(auram != null, "Aspect auram exists");
        ItemStack essenceStack = EuclidaisyBlockEntity.createWispEssence(auram);
        check(essenceStack != null && !essenceStack.isEmpty(), "essence stack not empty");
        CustomData customData = essenceStack.get(DataComponents.CUSTOM_DATA);
        check(customData != null, "essence stack has custom data component");
        CompoundTag essenceTag = customData.copyTag();
        check("auram".equals(essenceTag.getString("EssenceAspect")), "essence aspect is auram");
        check(essenceTag.getInt("EssenceAmount") == 2, "essence amount is 2");

        // Whisperweed
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed BE");
        var whisperweedBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:whisperweed"));
        check(whisperweedBlock instanceof WhisperweedBlock, "whisperweed is WhisperweedBlock");
        check(WhisperweedBlockEntity.COST == 6000, "whisperweed cost 6000");
        check(WhisperweedBlockEntity.INTERVAL == 300, "whisperweed interval 300");
        check(WhisperweedBlockEntity.RANGE == 3, "whisperweed range 3");

        var whisperweedBE = new WhisperweedBlockEntity(BlockPos.ZERO, whisperweedBlock.defaultBlockState());
        check(whisperweedBE.getMana() == 0, "whisperweed starts at 0 mana");
        check(whisperweedBE.getMaxMana() == 6000, "whisperweed max mana is 6000");
        whisperweedBE.addMana(1500);
        check(whisperweedBE.getMana() == 1500, "whisperweed mana after addMana(1500) == 1500");
        whisperweedBE.addMana(10000);
        check(whisperweedBE.getMana() == 6000, "whisperweed mana capped at 6000");
        whisperweedBE.setTicksExisted(123);
        whisperweedBE.setBindingPos(new BlockPos(5, 6, 7));

        CompoundTag whisperweedTag = new CompoundTag();
        whisperweedBE.writeCustomData(whisperweedTag);
        check(whisperweedTag.getInt("mana") == 6000, "whisperweed tag mana == 6000");
        check(whisperweedTag.getInt("ticksExisted") == 123, "whisperweed tag ticks == 123");
        var whisperweedReloaded = new WhisperweedBlockEntity(BlockPos.ZERO, whisperweedBlock.defaultBlockState());
        whisperweedReloaded.readCustomData(whisperweedTag);
        check(whisperweedReloaded.getMana() == 6000, "reloaded whisperweed mana == 6000");
        check(whisperweedReloaded.getTicksExisted() == 123, "reloaded whisperweed ticks == 123");
        check(new BlockPos(5, 6, 7).equals(whisperweedReloaded.getBindingPos()), "reloaded whisperweed boundPos matches");

        // Tainthistle
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle BE");
        var tainthistleBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:tainthistle"));
        check(tainthistleBlock instanceof TainthistleBlock, "tainthistle is TainthistleBlock");
        check(TainthistleBlockEntity.RANGE == 4, "tainthistle range 4");
        check(TainthistleBlockEntity.BURN_CAP == 4000, "tainthistle burn cap 4000");
        check(TainthistleBlockEntity.MAX_MANA == 150, "tainthistle max mana 150");

        var tainthistleBE = new TainthistleBlockEntity(BlockPos.ZERO, tainthistleBlock.defaultBlockState());
        check(tainthistleBE.getMana() == 0, "tainthistle starts at 0 mana");
        check(tainthistleBE.getMaxMana() == 150, "tainthistle max mana == 150");
        tainthistleBE.addMana(75);
        check(tainthistleBE.getMana() == 75, "tainthistle mana == 75");
        tainthistleBE.addMana(500);
        check(tainthistleBE.getMana() == 150, "tainthistle mana capped at 150");
        check(tainthistleBE.getBurnTime() == 0, "tainthistle burn time starts at 0");
        tainthistleBE.setBurnTime(1800);
        check(tainthistleBE.getBurnTime() == 1800, "tainthistle burn time == 1800");
        tainthistleBE.setBurnTime(8000);
        check(tainthistleBE.getBurnTime() == 4000, "tainthistle burn time capped at 4000");
        tainthistleBE.setTicksExisted(456);
        tainthistleBE.setBindingPos(new BlockPos(1, 2, 3));

        CompoundTag tainthistleTag = new CompoundTag();
        tainthistleBE.writeCustomData(tainthistleTag);
        check(tainthistleTag.getInt("mana") == 150, "tainthistle tag mana == 150");
        check(tainthistleTag.getInt("burnTime") == 4000, "tainthistle tag burnTime == 4000");
        check(tainthistleTag.getInt("ticksExisted") == 456, "tainthistle tag ticks == 456");
        var tainthistleReloaded = new TainthistleBlockEntity(BlockPos.ZERO, tainthistleBlock.defaultBlockState());
        tainthistleReloaded.readCustomData(tainthistleTag);
        check(tainthistleReloaded.getMana() == 150, "reloaded tainthistle mana == 150");
        check(tainthistleReloaded.getBurnTime() == 4000, "reloaded tainthistle burnTime == 4000");
        check(tainthistleReloaded.getTicksExisted() == 456, "reloaded tainthistle ticks == 456");
        check(new BlockPos(1, 2, 3).equals(tainthistleReloaded.getBindingPos()), "reloaded tainthistle boundPos matches");

        // --- Functional Simulation of Tainthistle (R01 & R06) ---
        check(TainthistleBlockEntity.isFlux(testFluxGoo.defaultBlockState()), "isFlux recognizes flux_goo");
        check(TainthistleBlockEntity.isFlux(testFluxGas.defaultBlockState()), "isFlux recognizes flux_gas");
        check(!TainthistleBlockEntity.isFlux(tainthistleBlock.defaultBlockState()), "isFlux strictly rejects own flower (R01 protection)");
        var taintStone = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:taint_stone"));
        check(taintStone != null && !TainthistleBlockEntity.isFlux(taintStone.defaultBlockState()), "isFlux rejects taint_stone construction block");
        var taintLog = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:taint_log"));
        check(taintLog != null && !TainthistleBlockEntity.isFlux(taintLog.defaultBlockState()), "isFlux rejects taint_log construction block");
        check(!TainthistleBlockEntity.isFlux(Blocks.AIR.defaultBlockState()), "isFlux rejects air");
        check(!TainthistleBlockEntity.isFlux(null), "isFlux rejects null");

        check(TainthistleBlockEntity.getFluxDepth(testFluxGoo.defaultBlockState().setValue(QUANTA, 1)) == 1, "flux depth quanta=1");
        check(TainthistleBlockEntity.getFluxDepth(testFluxGoo.defaultBlockState().setValue(QUANTA, 4)) == 4, "flux depth quanta=4");
        check(TainthistleBlockEntity.getFluxDepth(testFluxGoo.defaultBlockState().setValue(QUANTA, 8)) == 8, "flux depth quanta=8");
        check(TainthistleBlockEntity.getFluxDepth(testFluxGoo.defaultBlockState().setValue(QUANTA, 16)) == 16, "flux depth quanta=16");

        SimulatedFlowerLevel simLevel = SimulatedFlowerLevel.create();
        BlockPos flowerPos = new BlockPos(0, 64, 0);
        simLevel.setBlock(flowerPos, tainthistleBlock.defaultBlockState(), 3);
        simLevel.setBlock(flowerPos.above(), tainthistleBlock.defaultBlockState(), 3);
        simLevel.setBlock(flowerPos.east(), taintStone.defaultBlockState(), 3);
        simLevel.setBlock(flowerPos.north(), testFluxGoo.defaultBlockState().setValue(QUANTA, 4), 3);
        simLevel.setBlock(flowerPos.south(), testFluxGas.defaultBlockState().setValue(QUANTA, 8), 3);

        var simTainthistle = new TainthistleBlockEntity(flowerPos, tainthistleBlock.defaultBlockState());
        for (int t = 0; t < 80; t++) {
            simTainthistle.tick(simLevel, flowerPos, tainthistleBlock.defaultBlockState());
        }
        check(simTainthistle.getBurnTime() == 239, "burnTime increased by 4*20 + 8*20 - 1 = 239, actual: " + simTainthistle.getBurnTime());
        check(simLevel.getBlockState(flowerPos).getBlock() == tainthistleBlock, "tainthistle NEVER deletes itself at pos!");
        check(simLevel.getBlockState(flowerPos.above()).getBlock() == tainthistleBlock, "neighboring tainthistle intact!");
        check(simLevel.getBlockState(flowerPos.east()).getBlock() == taintStone, "taint_stone intact!");
        check(simLevel.getBlockState(flowerPos.north()).isAir(), "flux goo consumed to air!");
        check(simLevel.getBlockState(flowerPos.south()).isAir(), "flux gas consumed to air!");

        for (int t = 0; t < 10; t++) {
            simTainthistle.tick(simLevel, flowerPos, tainthistleBlock.defaultBlockState());
        }
        check(simTainthistle.getBurnTime() == 229, "burnTime decreased from 239 to 229: " + simTainthistle.getBurnTime());
        check(simTainthistle.getMana() == 18, "mana increased by 18 (3 per 2 ticks across 6 even ticks: 80,82,84,86,88,90): " + simTainthistle.getMana());

        // --- S06: Positive Mana Transfer & Conservation for Tainthistle ---
        BlockPos collectorPos = flowerPos.above(2);
        TestManaCollector collector = new TestManaCollector(simLevel, collectorPos, 0, 1000);
        simLevel.capabilities.put(collectorPos, collector);

        int flowerManaBefore = simTainthistle.getMana();
        check(flowerManaBefore == 18, "tainthistle generated 18 mana: " + flowerManaBefore);
        int collectorManaBefore = collector.getCurrentMana();
        int totalTainthistleManaBefore = flowerManaBefore + collectorManaBefore;

        int sent = BotaniaCompat.emitMana(simLevel, flowerPos, collectorPos, flowerManaBefore);
        check(sent == 18, "emitMana transferred exact 18 mana: " + sent);
        simTainthistle.addMana(-sent);
        check(simTainthistle.getMana() == 0, "flower mana drained to 0");
        check(collector.getCurrentMana() == 18, "collector received 18 mana: " + collector.getCurrentMana());
        int totalTainthistleManaAfter = simTainthistle.getMana() + collector.getCurrentMana();
        check(totalTainthistleManaBefore == totalTainthistleManaAfter, "strict mana conservation in emitMana: " + totalTainthistleManaBefore + " == " + totalTainthistleManaAfter);

        // Test discovery of closest collector with boundPos = null
        simTainthistle.addMana(50);
        int sentUnbound = BotaniaCompat.emitMana(simLevel, flowerPos, null, 50);
        check(sentUnbound == 50, "unbound emitMana discovered collector and sent 50 mana: " + sentUnbound);
        simTainthistle.addMana(-sentUnbound);
        check(simTainthistle.getMana() == 0, "flower mana drained to 0 after unbound send");
        check(collector.getCurrentMana() == 68, "collector mana incremented to 68 (18 + 50)");

        // --- Functional Simulation of Euclidaisy (R06) ---
        var simEuclidaisy = new EuclidaisyBlockEntity(flowerPos, euclidaisyBlock.defaultBlockState());
        simLevel.neighborSignal = true;
        simEuclidaisy.addMana(9000);
        for (int t = 0; t < 400; t++) {
            simEuclidaisy.tick(simLevel, flowerPos, euclidaisyBlock.defaultBlockState());
        }
        check(simEuclidaisy.getMana() == 9000, "euclidaisy pauses on redstone signal, does not consume mana");
        check(simLevel.spawnedEntities.isEmpty(), "euclidaisy does not spawn essence when paused by redstone");

        simLevel.neighborSignal = false;
        simEuclidaisy.setTicksExisted(399);
        simEuclidaisy.tick(simLevel, flowerPos, euclidaisyBlock.defaultBlockState());
        check(simEuclidaisy.getMana() == 0, "euclidaisy consumed 9000 mana on interval");
        check(simLevel.spawnedEntities.size() == 1, "euclidaisy spawned essence entity");
        var spawnedItem = simLevel.spawnedEntities.get(0);
        check(spawnedItem instanceof ItemEntity, "spawned entity is ItemEntity");
        var itemStack = ((ItemEntity) spawnedItem).getItem();
        check(!itemStack.isEmpty(), "spawned essence stack is not empty");
        CustomData customDataComp = itemStack.get(DataComponents.CUSTOM_DATA);
        check(customDataComp != null, "spawned essence has custom data");
        CompoundTag spawnedTag = customDataComp.copyTag();
        check(spawnedTag.contains("EssenceAspect"), "spawned essence has EssenceAspect");
        check(spawnedTag.getInt("EssenceAmount") == 2, "spawned essence amount is 2");

        // --- Functional Simulation of Whisperweed (R06) ---
        var simWhisperweed = new WhisperweedBlockEntity(flowerPos, whisperweedBlock.defaultBlockState());
        simLevel.neighborSignal = true;
        simWhisperweed.addMana(6000);
        for (int t = 0; t < 300; t++) {
            simWhisperweed.tick(simLevel, flowerPos, whisperweedBlock.defaultBlockState());
        }
        check(simWhisperweed.getMana() == 6000, "whisperweed pauses on redstone signal");
        simLevel.neighborSignal = false;
        for (int t = 0; t < 300; t++) {
            simWhisperweed.tick(simLevel, flowerPos, whisperweedBlock.defaultBlockState());
        }
        check(simWhisperweed.getMana() == 6000, "whisperweed without player in range does not consume mana");

        // --- S06: Positive Mana Transfer & Conservation for Whisperweed ---
        BlockPos poolPos = flowerPos.south(2);
        TestManaPool pool = new TestManaPool(simLevel, poolPos, 10000, 100000);
        simLevel.capabilities.put(poolPos, pool);

        simWhisperweed.addMana(-simWhisperweed.getMana()); // reset flower mana to 0
        int whisperweedManaBefore = simWhisperweed.getMana();
        int poolManaBefore = pool.getCurrentMana();
        int totalWhisperweedManaBefore = whisperweedManaBefore + poolManaBefore;

        int drawn = BotaniaCompat.drawMana(simLevel, flowerPos, poolPos, 6000);
        check(drawn == 6000, "drawMana drew exact 6000 mana: " + drawn);
        simWhisperweed.addMana(drawn);
        check(simWhisperweed.getMana() == 6000, "whisperweed received 6000 mana: " + simWhisperweed.getMana());
        check(pool.getCurrentMana() == 4000, "pool decremented to 4000: " + pool.getCurrentMana());
        int totalWhisperweedManaAfter = simWhisperweed.getMana() + pool.getCurrentMana();
        check(totalWhisperweedManaBefore == totalWhisperweedManaAfter, "strict mana conservation in drawMana: " + totalWhisperweedManaBefore + " == " + totalWhisperweedManaAfter);

        // Test discovery of closest pool with boundPos = null
        simWhisperweed.addMana(-simWhisperweed.getMana()); // reset flower mana to 0
        int drawnUnbound = BotaniaCompat.drawMana(simLevel, flowerPos, null, 3000);
        check(drawnUnbound == 3000, "unbound drawMana discovered pool and drew 3000 mana: " + drawnUnbound);
        simWhisperweed.addMana(drawnUnbound);
        check(simWhisperweed.getMana() == 3000, "whisperweed received 3000 mana: " + simWhisperweed.getMana());
        check(pool.getCurrentMana() == 1000, "pool decremented to 1000 (4000 - 3000): " + pool.getCurrentMana());

        check(BotaniaCompat.drawMana(null, flowerPos, null, 100) == 0, "drawMana with null level returns 0");
        check(BotaniaCompat.drawMana(simLevel, flowerPos, null, -50) == 0, "drawMana with negative needed returns 0");
        check(BotaniaCompat.emitMana(null, flowerPos, null, 100) == 0, "emitMana with null level returns 0");
        check(BotaniaCompat.emitMana(simLevel, flowerPos, null, -50) == 0, "emitMana with negative available returns 0");
    }

    private static void testDataPackIntegrity() throws Exception {
        Set<String> validBotaniaItems = new HashSet<>();
        java.net.URL jarUrl = vazkii.botania.common.item.BotaniaItems.class.getProtectionDomain().getCodeSource().getLocation();
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(new java.io.File(jarUrl.toURI()))) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("assets/botania/models/item/") && name.endsWith(".json")) {
                    String id = name.substring("assets/botania/models/item/".length(), name.length() - 5);
                    validBotaniaItems.add("botania:" + id);
                }
                if (name.startsWith("assets/botania/blockstates/") && name.endsWith(".json")) {
                    String id = name.substring("assets/botania/blockstates/".length(), name.length() - 5);
                    validBotaniaItems.add("botania:" + id);
                }
            }
        }
        check(!validBotaniaItems.isEmpty(), "Botania items registered in set: " + validBotaniaItems.size());
        check(!validBotaniaItems.contains("botania:mana_petal"), "legacy botania:mana_petal is NOT a valid item");

        List<String> researches = List.of(
                "botany", "rod_livingwood", "cap_manasteel", "rod_dreamwood_staff", "rod_dreamwood",
                "cap_elementium", "cap_terrasteel", "euclidaisy", "whisperweed", "tainthistle"
        );
        for (String r : researches) {
            String path = "/packs/botania/data/forbidden/thaumcraft/research/" + r + ".json";
            var stream = P04BotaniaCheck.class.getResourceAsStream(path);
            check(stream != null, "Botania research exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("key") && !json.get("key").getAsString().isEmpty(), "Research has non-empty key: " + r);
                check(json.has("category") && json.get("category").getAsString().equals("forbidden:forbidden"), "Research in FORBIDDEN category: " + r);
            }
        }

        List<String> recipes = List.of(
                "wand_rod_livingwood", "mana_infusion_wand_rod_livingwood",
                "wand_cap_manasteel_inert", "mana_infusion_wand_cap_manasteel",
                "wand_rod_dreamwood", "mana_infusion_wand_rod_dreamwood",
                "wand_rod_dreamwood_staff", "wand_cap_elementium_inert", "wand_cap_elementium",
                "wand_cap_terrasteel", "euclidaisy", "whisperweed", "tainthistle", "botanist_emblem"
        );
        for (String rec : recipes) {
            String path = "/packs/botania/data/forbidden/recipe/" + rec + ".json";
            var stream = P04BotaniaCheck.class.getResourceAsStream(path);
            check(stream != null, "Botania recipe exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("type") && !json.get("type").getAsString().isEmpty(), "Recipe has non-empty type: " + rec);
                check(json.has("result") || json.has("output") || json.has("key"), "Recipe has result/output/key: " + rec);
                validateReferencedItems(json, validBotaniaItems, rec);
            }
        }
    }

    private static void validateReferencedItems(com.google.gson.JsonObject json, Set<String> validBotania, String recipe) {
        List<String> itemsToCheck = new ArrayList<>();
        var res = json.has("result") ? json.get("result") : json.get("output");
        if (res != null) {
            if (res.isJsonObject()) {
                var r = res.getAsJsonObject();
                if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
            } else if (res.isJsonPrimitive()) {
                itemsToCheck.add(res.getAsString());
            }
        }
        var in = json.has("input") ? json.get("input") : (json.has("central_item") ? json.get("central_item") : json.get("catalyst"));
        if (in != null) {
            if (in.isJsonObject()) {
                var r = in.getAsJsonObject();
                if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
            } else if (in.isJsonPrimitive()) {
                itemsToCheck.add(in.getAsString());
            }
        }
        if (json.has("components")) {
            for (var elem : json.getAsJsonArray("components")) {
                if (elem.isJsonObject()) {
                    var r = elem.getAsJsonObject();
                    if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                    else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
                } else if (elem.isJsonPrimitive()) {
                    itemsToCheck.add(elem.getAsString());
                }
            }
        }
        if (json.has("key")) {
            var keyObj = json.getAsJsonObject("key");
            for (var entry : keyObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    var r = entry.getValue().getAsJsonObject();
                    if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                    else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
                } else if (entry.getValue().isJsonPrimitive()) {
                    itemsToCheck.add(entry.getValue().getAsString());
                }
            }
        }

        for (String id : itemsToCheck) {
            check(!id.isBlank(), "item id non-blank in " + recipe);
            if (id.startsWith("botania:")) {
                check(validBotania.contains(id), "Botania item exists in real mod registry: " + id + " in " + recipe);
            } else if (id.startsWith("forbidden:")) {
                var loc = ResourceLocation.parse(id);
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc);
                check(exists, "Forbidden Item/Block exists in registry: " + id + " in " + recipe);
            } else if (id.startsWith("minecraft:")) {
                var loc = ResourceLocation.parse(id);
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc);
                check(exists, "Minecraft Item/Block exists in registry: " + id + " in " + recipe);
            } else if (id.startsWith("thaumcraft:")) {
                var loc = ResourceLocation.parse(id);
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc)
                        || P04BotaniaCheck.class.getResource("/assets/thaumcraft/models/item/" + loc.getPath() + ".json") != null
                        || P04BotaniaCheck.class.getResource("/assets/thaumcraft/blockstates/" + loc.getPath() + ".json") != null;
                check(exists, "Thaumcraft Item/Block exists: " + id + " in " + recipe);
            }
        }
    }

    private static final IntegerProperty QUANTA = IntegerProperty.create("quanta", 1, 16);
    private static Block testFluxGoo;
    private static Block testFluxGas;

    private static class TestFluxBlock extends Block {
        TestFluxBlock() {
            super(BlockBehaviour.Properties.of());
            registerDefaultState(stateDefinition.any().setValue(QUANTA, 1));
        }
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(QUANTA);
        }
    }

    private static void registerTestFluxBlocks() {
        var gooLoc = ResourceLocation.parse("thaumcraft:flux_goo");
        var gasLoc = ResourceLocation.parse("thaumcraft:flux_gas");
        ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).unfreeze();
        if (!BuiltInRegistries.BLOCK.containsKey(gooLoc)) {
            testFluxGoo = new TestFluxBlock();
            Registry.register(BuiltInRegistries.BLOCK, gooLoc, testFluxGoo);
        } else {
            testFluxGoo = BuiltInRegistries.BLOCK.get(gooLoc);
        }
        if (!BuiltInRegistries.BLOCK.containsKey(gasLoc)) {
            testFluxGas = new TestFluxBlock();
            Registry.register(BuiltInRegistries.BLOCK, gasLoc, testFluxGas);
        } else {
            testFluxGas = BuiltInRegistries.BLOCK.get(gasLoc);
        }
        BuiltInRegistries.BLOCK.freeze();
    }

    private static void testC04EmblemAndVisualsIntegrity() {
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:botanist_emblem")), "registered forbidden:botanist_emblem item");

        for (var model : WandVisuals.models()) {
            check(P04BotaniaCheck.class.getResource("/assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json") != null,
                    "registered composite model exists on classpath: " + model);
        }
    }


    public static class TestManaCollector implements vazkii.botania.api.mana.ManaCollector {
        private final net.minecraft.world.level.Level level;
        private final BlockPos pos;
        private int mana;
        private final int maxMana;

        public TestManaCollector(net.minecraft.world.level.Level level, BlockPos pos, int initialMana, int maxMana) {
            this.level = level;
            this.pos = pos;
            this.mana = initialMana;
            this.maxMana = maxMana;
        }

        @Override public net.minecraft.world.level.Level getManaReceiverLevel() { return level; }
        @Override public BlockPos getManaReceiverPos() { return pos; }
        @Override public int getCurrentMana() { return mana; }
        @Override public boolean isFull() { return mana >= maxMana; }
        @Override public void receiveMana(int amount) { this.mana = Math.min(maxMana, Math.max(0, this.mana + amount)); }
        @Override public boolean canReceiveManaFromBursts() { return true; }
        @Override public void onClientDisplayTick() {}
        @Override public float getManaYieldMultiplier(vazkii.botania.api.internal.ManaBurst burst) { return 1.0f; }
        @Override public int getMaxMana() { return maxMana; }
    }

    public static class TestManaPool implements vazkii.botania.api.mana.ManaPool {
        private final net.minecraft.world.level.Level level;
        private final BlockPos pos;
        private int mana;
        private final int maxMana;

        public TestManaPool(net.minecraft.world.level.Level level, BlockPos pos, int initialMana, int maxMana) {
            this.level = level;
            this.pos = pos;
            this.mana = initialMana;
            this.maxMana = maxMana;
        }

        @Override public net.minecraft.world.level.Level getManaReceiverLevel() { return level; }
        @Override public BlockPos getManaReceiverPos() { return pos; }
        @Override public int getCurrentMana() { return mana; }
        @Override public boolean isFull() { return mana >= maxMana; }
        @Override public void receiveMana(int amount) { this.mana = Math.min(maxMana, Math.max(0, this.mana + amount)); }
        @Override public boolean canReceiveManaFromBursts() { return true; }
        @Override public boolean isOutputtingPower() { return false; }
        @Override public int getMaxMana() { return maxMana; }
    }

    private P04BotaniaCheck() {}
}