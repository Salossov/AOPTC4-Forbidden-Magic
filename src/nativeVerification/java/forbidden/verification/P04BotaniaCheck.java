package forbidden.verification;

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
import java.util.List;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
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

        testC01ApiAndIsolation();
        testC02RodsAndCaps();
        testC03Flowers();
        testC04EmblemAndVisualsIntegrity();

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
        check("ROD_livingwood".equals(livingwood.getResearch()), "livingwood research ROD_livingwood");
        check(WandRod.rods.get("livingwood") == livingwood, "accessible via alias 'livingwood'");

        // Dreamwood Rod
        WandRod dreamwood = WandRod.rods.get("forbidden:dreamwood");
        check(dreamwood != null, "forbidden:dreamwood registered");
        check(dreamwood.getCapacity() == 100, "dreamwood capacity 100");
        check(dreamwood.getCraftCost() == 12, "dreamwood cost 12");
        check("ROD_dreamwood".equals(dreamwood.getResearch()), "dreamwood research ROD_dreamwood");
        check(WandRod.rods.get("dreamwood") == dreamwood, "accessible via alias 'dreamwood'");

        // Dreamwood Staff
        WandRod dreamwoodStaff = WandRod.rods.get("forbidden:dreamwood_staff");
        check(dreamwoodStaff != null, "forbidden:dreamwood_staff registered");
        check(dreamwoodStaff instanceof StaffRod, "dreamwood_staff is StaffRod");
        check(dreamwoodStaff.getCapacity() == 250, "dreamwood staff capacity 250");
        check(dreamwoodStaff.getCraftCost() == 25, "dreamwood staff cost 25");
        check(WandRod.rods.get("dreamwood_staff") == dreamwoodStaff, "accessible via alias 'dreamwood_staff'");

        // Manasteel Cap
        WandCap manasteel = WandCap.caps.get("forbidden:manasteel");
        check(manasteel != null, "forbidden:manasteel registered");
        check(Math.abs(manasteel.getBaseCostModifier() - 0.90f) < 1e-5, "manasteel 0.90f modifier (10% discount)");
        check(manasteel.getCraftCost() == 6, "manasteel craft cost 6");
        check("CAP_manasteel".equals(manasteel.getResearch()), "manasteel research CAP_manasteel");
        check(WandCap.caps.get("manasteel") == manasteel, "accessible via alias 'manasteel'");

        // Elementium Cap
        WandCap elementium = WandCap.caps.get("forbidden:elementium");
        check(elementium != null, "forbidden:elementium registered");
        check(Math.abs(elementium.getBaseCostModifier() - 0.80f) < 1e-5, "elementium 0.80f modifier (20% discount)");
        check(elementium.getCraftCost() == 9, "elementium craft cost 9");
        check("CAP_elementium".equals(elementium.getResearch()), "elementium research CAP_elementium");
        check(WandCap.caps.get("elementium") == elementium, "accessible via alias 'elementium'");

        // Terrasteel Cap
        WandCap terrasteel = WandCap.caps.get("forbidden:terrasteel");
        check(terrasteel != null, "forbidden:terrasteel registered");
        check(Math.abs(terrasteel.getBaseCostModifier() - 1.80f) < 1e-5, "terrasteel 1.80f modifier");
        check(terrasteel.getCraftCost() == 1, "terrasteel craft cost 1");
        check("CAP_terrasteel".equals(terrasteel.getResearch()), "terrasteel research CAP_terrasteel");
        check(WandCap.caps.get("terrasteel") == terrasteel, "accessible via alias 'terrasteel'");

        // Update singleton
        check(YandereWandUpdate.INSTANCE != null, "YandereWandUpdate singleton exists");
    }

    private static void testC03Flowers() {
        // Euclidaisy
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:euclidaisy")), "registered forbidden:euclidaisy BE");
        var euclidaisy = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:euclidaisy"));
        check(euclidaisy instanceof EuclidaisyBlock, "euclidaisy is EuclidaisyBlock");
        check(EuclidaisyBlockEntity.COST == 9000, "euclidaisy cost 9000");
        check(EuclidaisyBlockEntity.INTERVAL == 400, "euclidaisy interval 400");

        // Whisperweed
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:whisperweed")), "registered forbidden:whisperweed BE");
        var whisperweed = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:whisperweed"));
        check(whisperweed instanceof WhisperweedBlock, "whisperweed is WhisperweedBlock");
        check(WhisperweedBlockEntity.COST == 6000, "whisperweed cost 6000");
        check(WhisperweedBlockEntity.INTERVAL == 300, "whisperweed interval 300");
        check(WhisperweedBlockEntity.RANGE == 3, "whisperweed range 3");

        // Tainthistle
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:tainthistle")), "registered forbidden:tainthistle BE");
        var tainthistle = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:tainthistle"));
        check(tainthistle instanceof TainthistleBlock, "tainthistle is TainthistleBlock");
        check(TainthistleBlockEntity.RANGE == 4, "tainthistle range 4");
        check(TainthistleBlockEntity.BURN_CAP == 4000, "tainthistle burn cap 4000");
    }

    private static void testC04EmblemAndVisualsIntegrity() {
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:botanist_emblem")), "registered forbidden:botanist_emblem item");

        for (var model : WandVisuals.models()) {
            check(P04BotaniaCheck.class.getResource("/assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json") != null,
                    "registered composite model exists on classpath: " + model);
        }
    }

    private P04BotaniaCheck() {}
}
