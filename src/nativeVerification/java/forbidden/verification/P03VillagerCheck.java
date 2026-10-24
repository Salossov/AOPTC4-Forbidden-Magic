package forbidden.verification;

import com.mojang.serialization.Lifecycle;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import forbidden.villager.ForbiddenVillagers;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;

/**
 * Native verification for P03 Heretic villager profession, trades, and Heretic Study POI (D15, B35).
 */
public final class P03VillagerCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("P03 Villager check failed: " + reason);
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

        forbidden.content.ForbiddenAspects.register();
        forbidden.content.wands.ForbiddenWandMechanics.registerAll();

        try {
            WandApi.install(new WandApi.Access() {
                public int get(ItemStack stack, Aspect aspect) { return 0; }
                public int capacity(ItemStack stack) { return 5000; }
                public void set(ItemStack stack, Aspect aspect, int centivis) {}
                public Map<Aspect, Integer> quote(ItemStack stack, net.minecraft.world.entity.player.Player player, Map<Aspect, Integer> baseCentivis) { return baseCentivis; }
                public boolean consume(ItemStack stack, net.minecraft.world.entity.player.Player player, Map<Aspect, Integer> baseCentivis, boolean commit) { return true; }
                public ItemStack focus(ItemStack stack) { return ItemStack.EMPTY; }
                public void focus(ItemStack wand, ItemStack focus) {}
                public void parts(ItemStack stack, WandParts parts) {}
                public WandApi.Kind kind(ItemStack stack) { return WandApi.Kind.WAND; }
                public WandParts parts(ItemStack stack) { return new WandParts("forbidden:profane", "iron"); }
                public Map<Aspect, Integer> quoteCrafting(ItemStack stack, net.minecraft.world.entity.player.Player player, Map<Aspect, Integer> wholeVis) { return wholeVis; }
            });
        } catch (IllegalStateException ignored) {}

        // B35 & D15: Block and Item existence
        var studyBlockId = ResourceLocation.parse("forbidden:heretic_study");
        check(BuiltInRegistries.BLOCK.containsKey(studyBlockId), "forbidden:heretic_study block registered");
        check(BuiltInRegistries.ITEM.containsKey(studyBlockId), "forbidden:heretic_study item registered");

        var block = ForbiddenContent.HERETIC_STUDY.get();
        check(block != null, "heretic_study block instance");
        var states = block.getStateDefinition().getPossibleStates();
        check(states.size() == 4, "heretic_study has 4 directional states");
        for (var state : states) {
            check(state.hasProperty(HorizontalDirectionalBlock.FACING), "state has FACING");
            var shape = state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
            check(!shape.isEmpty(), "heretic_study shape is non-empty");
        }

        // POI Registration check
        var poiRegConstructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        poiRegConstructor.setAccessible(true);

        var poiRegistry = new net.minecraft.core.MappedRegistry<>(Registries.POINT_OF_INTEREST_TYPE, Lifecycle.stable());
        var poiEvent = (RegisterEvent) poiRegConstructor.newInstance(Registries.POINT_OF_INTEREST_TYPE, poiRegistry);
        ForbiddenVillagers.ModEvents.onRegister(poiEvent);
        check(poiRegistry.containsKey(ForbiddenVillagers.HERETIC_ID), "heretic POI registered");
        var hereticPoi = poiRegistry.get(ForbiddenVillagers.HERETIC_ID);
        check(hereticPoi != null, "heretic POI instance");
        for (var state : states) {
            check(hereticPoi.matchingStates().contains(state), "heretic POI matches heretic_study state " + state);
        }
        check(!hereticPoi.matchingStates().contains(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState()),
                "heretic POI does NOT match enchanting table");

        // Villager Profession Registration check
        var profRegistry = new net.minecraft.core.MappedRegistry<>(Registries.VILLAGER_PROFESSION, Lifecycle.stable());
        var profEvent = (RegisterEvent) poiRegConstructor.newInstance(Registries.VILLAGER_PROFESSION, profRegistry);
        ForbiddenVillagers.ModEvents.onRegister(profEvent);
        check(profRegistry.containsKey(ForbiddenVillagers.HERETIC_ID), "heretic profession registered");
        var profession = profRegistry.get(ForbiddenVillagers.HERETIC_ID);
        check(profession != null, "heretic profession instance");

        // Trades check
        testTrades(profession, false);
        testTrades(profession, true);

        // Recipe check
        try (InputStream stream = P03VillagerCheck.class.getResourceAsStream("/data/forbidden/recipe/heretic_study.json")) {
            check(stream != null, "heretic_study recipe exists in resources");
            var content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            check(content.contains("forbidden:heretic_study"), "recipe outputs heretic_study");
            check(content.contains("forbidden:taint_planks"), "recipe requires taint_planks");
            check(content.contains("forbidden:taint_stone"), "recipe requires taint_stone");
            check(content.contains("minecraft:book"), "recipe requires book");
        }

        // Models check
        check(P03VillagerCheck.class.getResource("/assets/forbidden/blockstates/heretic_study.json") != null, "heretic_study blockstate exists");
        check(P03VillagerCheck.class.getResource("/assets/forbidden/models/block/heretic_study.json") != null, "heretic_study block model exists");
        check(P03VillagerCheck.class.getResource("/assets/forbidden/models/item/heretic_study.json") != null, "heretic_study item model exists");

        System.out.println("FORBIDDEN_P03_VILLAGER_PASS checks=" + checks);
    }

    private static void testTrades(VillagerProfession profession, boolean noLust) throws Exception {
        var f = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        f.setAccessible(true);
        f.set(ForbiddenConfig.NO_LUST, noLust);

        var event = new VillagerTradesEvent(new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(), profession);
        ForbiddenVillagers.GameEvents.onVillagerTrades(event);

        var trades = event.getTrades().get(1);
        check(trades != null, "tier 1 trades registered");
        int expectedTradeCount = noLust ? 8 : 9;
        check(trades.size() == expectedTradeCount, "trade count is " + expectedTradeCount + " (noLust=" + noLust + ")");

        var rand = RandomSource.create(42);
        boolean foundGreed = false;
        boolean foundLust = false;
        boolean foundWrath = false;
        boolean foundGluttony = false;
        boolean foundSkull = false;
        boolean foundGhastTear = false;
        boolean foundGlowstone = false;
        boolean foundWand = false;
        boolean foundFocus = false;

        for (var trade : trades) {
            var offer = trade.getOffer(null, rand);
            var result = offer.getResult();
            var costA = offer.getBaseCostA();

            if (result.is(ForbiddenContent.item("greed_shard"))) {
                foundGreed = true;
                check(costA.is(Items.EMERALD), "greed cost is emerald");
            } else if (result.is(Items.EMERALD) && costA.is(ForbiddenContent.item("lust_shard"))) {
                foundLust = true;
            } else if (result.is(Items.EMERALD) && costA.is(ForbiddenContent.item("wrath_shard"))) {
                foundWrath = true;
            } else if (result.is(Items.EMERALD) && costA.is(ForbiddenContent.item("gluttony_shard"))) {
                foundGluttony = true;
            } else if (result.is(Items.SKELETON_SKULL)) {
                foundSkull = true;
                check(costA.is(Items.EMERALD), "skull cost is emerald");
            } else if (result.is(Items.GHAST_TEAR)) {
                foundGhastTear = true;
                check(costA.is(Items.EMERALD), "ghast tear cost is emerald");
            } else if (result.is(Items.GLOWSTONE_DUST)) {
                foundGlowstone = true;
                check(costA.is(Items.EMERALD), "glowstone cost is emerald");
            } else if (!offer.getCostB().isEmpty() && offer.getCostB().is(Items.STICK)) {
                foundWand = true;
                check(offer.getMaxUses() == 2, "profane wand maxUses is 2");
            } else if (!offer.getCostB().isEmpty() && offer.getCostB().is(Items.DIAMOND)) {
                foundFocus = true;
                check(offer.getMaxUses() == 2, "fire focus maxUses is 2");
                check(!result.is(Items.BLAZE_POWDER), "fire focus must NOT be blaze powder");
            }
        }

        check(foundGreed, "greed trade present");
        check(foundLust == !noLust, "lust trade present matches !noLust");
        check(foundWrath, "wrath trade present");
        check(foundGluttony, "gluttony trade present");
        check(foundSkull, "skull trade present");
        check(foundGhastTear, "ghast tear trade present");
        check(foundGlowstone, "glowstone trade present");
        check(foundWand, "profane wand trade present");
        check(foundFocus, "fire focus trade present without fallback");
    }
}
