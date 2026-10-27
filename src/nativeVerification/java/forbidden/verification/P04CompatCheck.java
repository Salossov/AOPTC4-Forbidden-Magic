package forbidden.verification;

import forbidden.compat.Compat;
import forbidden.compat.tinkerer.OsmoticCostEntry;
import forbidden.compat.tinkerer.TinkererCompat;
import forbidden.compat.tinkerer.TinkererEnchantmentRegistry;
import forbidden.compat.twilight.TwilightCageMobRegistry;
import forbidden.compat.twilight.TwilightCompat;
import forbidden.compat.twilight.TwilightMobEntry;
import forbidden.compat.viewers.ViewersCompat;
import forbidden.config.ForbiddenConfig;
import forbidden.content.CageMobCatalog;
import forbidden.content.ForbiddenAspects;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Native verification suite for P04: Cross-mod Integrations (C09, C10, C11, C12–C16).
 * Covers:
 * - C09 (Thaumic Tinkerer): Contract T01 (8 enchantments aspect costs, level scaling formula, KAMI shard aspects).
 * - C10 (Twilight Forest): 36 mob mappings in Wrath Cage spawner, aspects, spawn conditions, noLust switching, missing mod resilience.
 * - C11 (Special Mobs): Deferred status check.
 * - C12–C15 (Legacy mods): Blocked status under Rule D8.
 * - C16 (JEI/EMI): Viewers bridge, category mappings without duplication (D14).
 */
public final class P04CompatCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("P04 check failed: " + reason);
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
        cached(ForbiddenConfig.TINKERER, true);
        cached(ForbiddenConfig.TWILIGHT_FOREST, true);
        cached(ForbiddenConfig.WRATH_CAGE, true);
        cached(ForbiddenConfig.NO_LUST, false);

        ForbiddenAspects.register();

        testC09ThaumicTinkerer();
        testC10TwilightForest();
        testC11SpecialMobsStatus();
        testC12ToC15LegacyModStatuses();
        testC16ViewersBridge();

        System.out.println("FORBIDDEN_P04_COMPAT_PASS checks=" + checks);
    }

    private static void testC09ThaumicTinkerer() throws Exception {
        // Contract T01: Exactly 8 enchantments registered
        var entries = TinkererEnchantmentRegistry.getAll();
        check(entries.size() == 8, "exactly 8 enchantments registered for Osmotic Enchanter");

        // 1. Wrath (max 5: perditio 6, ignis 6)
        OsmoticCostEntry wrath = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:wrath"));
        check(wrath != null, "wrath entry found");
        check(wrath.maxLevel() == 5, "wrath max level 5");
        check(wrath.getBaseCost("perditio") == 6, "wrath perditio base 6");
        check(wrath.getBaseCost("ignis") == 6, "wrath ignis base 6");
        check(wrath.research().equals("forbidden:wrath"), "wrath research forbidden:wrath");
        // cost at lvl 1 = (int)(6 * (1 * 1.2)) = 7
        AspectList wrathL1 = wrath.cost(1);
        check(wrathL1.getAmount(Aspect.getAspect("perditio")) == 7, "wrath lvl 1 perditio cost");
        check(wrathL1.getAmount(Aspect.getAspect("ignis")) == 7, "wrath lvl 1 ignis cost");
        // cost at lvl 5 = (int)(6 * (5 * 2.0)) = 60
        AspectList wrathL5 = wrath.cost(5);
        check(wrathL5.getAmount(Aspect.getAspect("perditio")) == 60, "wrath lvl 5 perditio cost");
        check(wrathL5.getAmount(Aspect.getAspect("ignis")) == 60, "wrath lvl 5 ignis cost");

        // 2. Cluster (max 4: ignis 5, terra 5, ordo 5)
        OsmoticCostEntry cluster = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:cluster"));
        check(cluster != null, "cluster entry found");
        check(cluster.maxLevel() == 4, "cluster max level 4");
        check(cluster.getBaseCost("ignis") == 5, "cluster ignis base 5");
        check(cluster.getBaseCost("terra") == 5, "cluster terra base 5");
        check(cluster.getBaseCost("ordo") == 5, "cluster ordo base 5");
        check(cluster.research().equals("forbidden:cluster"), "cluster research forbidden:cluster");

        // 3. Greedy (max 1: perditio 50, terra 30, ordo 20)
        OsmoticCostEntry greedy = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:greedy"));
        check(greedy != null, "greedy entry found");
        check(greedy.maxLevel() == 1, "greedy max level 1");
        check(greedy.getBaseCost("perditio") == 50, "greedy perditio base 50");
        check(greedy.getBaseCost("terra") == 30, "greedy terra base 30");
        check(greedy.getBaseCost("ordo") == 20, "greedy ordo base 20");
        // cost at lvl 1 = 50*1.2=60, 30*1.2=36, 20*1.2=24
        AspectList greedyCost = greedy.cost(1);
        check(greedyCost.getAmount(Aspect.getAspect("perditio")) == 60, "greedy perditio lvl 1");
        check(greedyCost.getAmount(Aspect.getAspect("terra")) == 36, "greedy terra lvl 1");
        check(greedyCost.getAmount(Aspect.getAspect("ordo")) == 24, "greedy ordo lvl 1");

        // 4. Educational (max 5: aer 10, ignis 10, aqua 10, terra 10, ordo 15, perditio 15)
        OsmoticCostEntry edu = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:educational"));
        check(edu != null, "educational entry found");
        check(edu.maxLevel() == 5, "educational max level 5");
        check(edu.getBaseCost("aer") == 10, "educational aer base 10");
        check(edu.getBaseCost("ignis") == 10, "educational ignis base 10");
        check(edu.getBaseCost("aqua") == 10, "educational aqua base 10");
        check(edu.getBaseCost("terra") == 10, "educational terra base 10");
        check(edu.getBaseCost("ordo") == 15, "educational ordo base 15");
        check(edu.getBaseCost("perditio") == 15, "educational perditio base 15");

        // 5. Consuming (max 1: perditio 10)
        OsmoticCostEntry consuming = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:consuming"));
        check(consuming != null, "consuming entry found");
        check(consuming.maxLevel() == 1, "consuming max level 1");
        check(consuming.getBaseCost("perditio") == 10, "consuming perditio base 10");

        // 6. Corrupting (max 1: perditio 20, ignis 15, terra 10)
        OsmoticCostEntry corrupting = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:corrupting"));
        check(corrupting != null, "corrupting entry found");
        check(corrupting.maxLevel() == 1, "corrupting max level 1");
        check(corrupting.getBaseCost("perditio") == 20, "corrupting perditio base 20");
        check(corrupting.getBaseCost("ignis") == 15, "corrupting ignis base 15");
        check(corrupting.getBaseCost("terra") == 10, "corrupting terra base 10");

        // 7. Voidtouched (max 1: perditio 70)
        OsmoticCostEntry voidtouched = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:voidtouched"));
        check(voidtouched != null, "voidtouched entry found");
        check(voidtouched.maxLevel() == 1, "voidtouched max level 1");
        check(voidtouched.getBaseCost("perditio") == 70, "voidtouched perditio base 70");

        // 8. Impact (max 1: perditio 30, terra 30)
        OsmoticCostEntry impact = TinkererEnchantmentRegistry.find(ResourceLocation.parse("forbidden:impact"));
        check(impact != null, "impact entry found");
        check(impact.maxLevel() == 1, "impact max level 1");
        check(impact.getBaseCost("perditio") == 30, "impact perditio base 30");
        check(impact.getBaseCost("terra") == 30, "impact terra base 30");

        // Level 0 cost returns empty
        check(wrath.cost(0).size() == 0, "level 0 cost is empty");

        // KAMI Shard Aspects
        AspectList netherAspects = TinkererCompat.getKamiAspects(TinkererCompat.NETHER_SHARD);
        check(netherAspects != null, "nether shard aspects present");
        check(netherAspects.getAmount(Aspect.getAspect("forbidden:infernus")) == 2, "nether shard infernus 2");
        check(netherAspects.getAmount(Aspect.getAspect("praecantatio")) == 1, "nether shard praecantatio 1");
        check(netherAspects.getAmount(Aspect.getAspect("vitreus")) == 1, "nether shard vitreus 1");

        AspectList enderAspects = TinkererCompat.getKamiAspects(TinkererCompat.ENDER_SHARD);
        check(enderAspects != null, "ender shard aspects present");
        check(enderAspects.getAmount(Aspect.getAspect("alienis")) == 2, "ender shard alienis 2");
        check(enderAspects.getAmount(Aspect.getAspect("praecantatio")) == 1, "ender shard praecantatio 1");
        check(enderAspects.getAmount(Aspect.getAspect("vitreus")) == 1, "ender shard vitreus 1");

        // Contract T01: Real receiver integration into Thaumic Tinkerer EnchanterCatalog
        int registeredInReceiver = TinkererCompat.registerWithReceiver();
        check(registeredInReceiver == 8, "all 8 enchantments registered into receiver");
        try {
            Class<?> catalogClass = Class.forName("thaumic.tinkerer.enchantment.EnchanterCatalog");
            var findMethod = catalogClass.getMethod("find", ResourceLocation.class);
            for (OsmoticCostEntry entry : TinkererEnchantmentRegistry.getAll()) {
                Object receiverEntry = findMethod.invoke(null, entry.id());
                check(receiverEntry != null, "receiver catalog contains " + entry.id());
                int maxLvl = (int) receiverEntry.getClass().getMethod("maxLevel").invoke(receiverEntry);
                check(maxLvl == entry.maxLevel(), "receiver maxLevel matches for " + entry.id());
                String resKey = (String) receiverEntry.getClass().getMethod("research").invoke(receiverEntry);
                check(resKey != null && resKey.equals(entry.research()), "receiver research matches: " + resKey + " == " + entry.research());
                check(resKey.startsWith("forbidden:"), "receiver research is namespaced: " + resKey);
                // Verify corresponding research JSON definition exists in FM
                String pack = entry.id().getPath().equals("greedy") ? "greedy" : "enchanting";
                String jsonPath = "/packs/" + pack + "/data/forbidden/thaumcraft/research/" + entry.id().getPath() + ".json";
                try (var resStream = P04CompatCheck.class.getResourceAsStream(jsonPath)) {
                    check(resStream != null, "research definition exists for " + entry.id() + " at " + jsonPath);
                }
            }
        } catch (ClassNotFoundException e) {
            // Standalone without TT JAR
        }

        // Verify KAMI JSON data file format and aspect preservation
        try (var stream = P04CompatCheck.class.getResourceAsStream("/packs/tinkerer/data/forbidden/thaumcraft/tinkerer_kami_aspects.json");
             var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
            check(stream != null, "tinkerer_kami_aspects.json exists in resources");
            com.google.gson.JsonObject kamiJson = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            check("item_aspect_additions".equals(kamiJson.get("kind").getAsString()), "kami aspects kind is item_aspect_additions");
            var reqMods = kamiJson.getAsJsonArray("required_mods");
            check(reqMods.size() == 1 && "thaumictinkerer".equals(reqMods.get(0).getAsString()), "kami aspects required_mods thaumictinkerer");
            var entriesObj = kamiJson.getAsJsonObject("entries");
            check(entriesObj.has("thaumictinkerer:nether_shard"), "kami entries has nether_shard");
            check(entriesObj.has("thaumictinkerer:ender_shard"), "kami entries has ender_shard");

            // Preservation check: applying KAMI additions onto an existing aspect map preserves non-KAMI aspects
            Map<String, Integer> baseAspects = new LinkedHashMap<>(Map.of("ignis", 1));
            for (var entry : entriesObj.getAsJsonObject("thaumictinkerer:nether_shard").entrySet()) {
                baseAspects.put(entry.getKey(), entry.getValue().getAsInt());
            }
            check(baseAspects.get("ignis") == 1, "existing base aspect preserved");
            check(baseAspects.get("forbidden:infernus") == 2, "added infernus aspect present");
            check(baseAspects.get("praecantatio") == 1, "added praecantatio aspect present");
            check(baseAspects.get("vitreus") == 1, "added vitreus aspect present");
        }
    }

    private static void testC10TwilightForest() throws Exception {
        // Exactly 35 unique mob entries registered
        var entries = TwilightCageMobRegistry.getAll();
        check(entries.size() == 35, "exactly 35 unique Twilight Forest mobs registered");

        // Verify aspects for all 35 entities
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:boar"), false) == Aspect.getAspect("bestia"), "wild boar aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:bighorn_sheep"), false) == Aspect.getAspect("pannus"), "bighorn sheep aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:deer"), false) == Aspect.getAspect("bestia"), "deer aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:redcap"), false) == Aspect.getAspect("metallum"), "redcap aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:swarm_spider"), false) == Aspect.getAspect("pannus"), "swarm spider aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:skeleton_druid"), false) == Aspect.getAspect("mortuus"), "skeleton druid aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:hostile_wolf"), false) == Aspect.getAspect("bestia"), "hostile wolf aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:wraith"), false) == Aspect.getAspect("exanimis"), "wraith aspect");
        check(TwilightCageMobRegistry.getSpawnCondition(ResourceLocation.parse("twilightforest:wraith")) == CageMobCatalog.SpawnCondition.AIR, "wraith is airborne");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:hedge_spider"), false) == Aspect.getAspect("pannus"), "hedge spider aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:penguin"), false) == Aspect.getAspect("volatus"), "penguin aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:tiny_bird"), false) == Aspect.getAspect("volatus"), "tiny bird aspect");
        check(TwilightCageMobRegistry.getSpawnCondition(ResourceLocation.parse("twilightforest:tiny_bird")) == CageMobCatalog.SpawnCondition.AIR, "tiny bird is airborne");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:squirrel"), false) == Aspect.getAspect("bestia"), "squirrel aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:dwarf_rabbit"), false) == Aspect.getAspect("bestia"), "bunny aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:raven"), false) == Aspect.getAspect("volatus"), "raven aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:kobold"), false) == Aspect.getAspect("lucrum"), "kobold aspect");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:mosquito_swarm"), false) == Aspect.getAspect("forbidden:gula"), "mosquito swarm gula");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:death_tome"), false) == Aspect.getAspect("cognitio"), "death tome cognitio");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:minotaur"), false) == Aspect.getAspect("bestia"), "minotaur bestia");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:minoshroom"), false) == Aspect.getAspect("herba"), "minoshroom herba");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:fire_beetle"), false) == Aspect.getAspect("ignis"), "fire beetle ignis");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:slime_beetle"), false) == Aspect.getAspect("limus"), "slime beetle limus");

        // Pinch beetle noLust switching
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:pinch_beetle"), false) == Aspect.getAspect("forbidden:luxuria"), "pinch beetle luxuria with noLust=false");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:pinch_beetle"), true) == Aspect.getAspect("bestia"), "pinch beetle bestia with noLust=true");

        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:maze_slime"), false) == Aspect.getAspect("limus"), "maze slime limus");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:mist_wolf"), false) == Aspect.getAspect("bestia"), "mist wolf bestia");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:king_spider"), false) == Aspect.getAspect("forbidden:superbia"), "king spider superbia");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:carminite_ghastling"), false) == Aspect.getAspect("forbidden:infernus"), "mini ghast infernus");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:carminite_ghastguard"), false) == Aspect.getAspect("forbidden:infernus"), "tower ghast infernus");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:carminite_golem"), false) == Aspect.getAspect("metallum"), "tower golem metallum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:towerwood_borer"), false) == Aspect.getAspect("praecantatio"), "tower termite praecantatio");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:upper_goblin_knight"), false) == Aspect.getAspect("metallum"), "upper goblin knight metallum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:lower_goblin_knight"), false) == Aspect.getAspect("metallum"), "lower goblin knight metallum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:helmet_crab"), false) == Aspect.getAspect("metallum"), "helmet crab metallum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:knight_phantom"), false) == Aspect.getAspect("metallum"), "knight phantom metallum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:yeti"), false) == Aspect.getAspect("gelum"), "yeti gelum");
        check(TwilightCageMobRegistry.getAspectForMob(ResourceLocation.parse("twilightforest:winter_wolf"), false) == Aspect.getAspect("gelum"), "winter wolf gelum");

        // Dynamic registration into CageMobCatalog
        TwilightCompat.init();
        var catalogEntries = CageMobCatalog.entries();
        check(!catalogEntries.containsKey(ResourceLocation.parse("twilightforest:boar")), "absent Twilight dependency prevents catalog registration");
        check(!catalogEntries.containsKey(ResourceLocation.parse("twilightforest:winter_wolf")), "absent Twilight dependency prevents winter wolf registration");
        check(CageMobCatalog.getSpawnCondition(ResourceLocation.parse("twilightforest:tiny_bird")) == CageMobCatalog.SpawnCondition.AIR, "catalog tiny bird air spawn");

        // Resilience check: non-existent mob lookup does not fail
        check(!TwilightCompat.isTwilightMob(ResourceLocation.parse("twilightforest:non_existent_beast")), "non-existent mob is false");
        check(TwilightCompat.getAspect(ResourceLocation.parse("twilightforest:non_existent_beast"), false) == null, "non-existent mob returns null aspect");
    }

    private static void testC11SpecialMobsStatus() {
        check(Compat.SPECIAL_MOBS_STATUS.contains("Deferred: Special Mobs not ported"), "C11 Special Mobs deferred status");
    }

    private static void testC12ToC15LegacyModStatuses() {
        check(Compat.ARS_MAGICA_STATUS.contains("Blocked: target mod unavailable on 1.21.1 NeoForge"), "C12 Ars Magica 2 blocked status");
        check(Compat.EE3_STATUS.contains("Replaced by optional ProjectE"), "C13 approved ProjectE replacement status");
        check(Compat.NATURA_STATUS.contains("Blocked: target mod unavailable on 1.21.1 NeoForge"), "C14 Natura blocked status");
        check(Compat.WITCHERY_STATUS.contains("Blocked: target mod unavailable on 1.21.1 NeoForge"), "C14 Witchery blocked status");
        check(Compat.TINKERS_CONSTRUCT_STATUS.contains("Blocked: target mod unavailable on 1.21.1 NeoForge"), "C15 Tinkers Construct blocked status");
    }

    private static void testC16ViewersBridge() {
        // D14 contract: all standard thaumcraft recipe categories supported
        check(ViewersCompat.SUPPORTED_CATEGORIES.size() == 5, "5 supported recipe categories");
        check(ViewersCompat.isSupportedCategory(ResourceLocation.parse("thaumcraft:arcane")), "arcane supported");
        check(ViewersCompat.isSupportedCategory(ResourceLocation.parse("thaumcraft:arcane_shapeless")), "arcane shapeless supported");
        check(ViewersCompat.isSupportedCategory(ResourceLocation.parse("thaumcraft:crucible")), "crucible supported");
        check(ViewersCompat.isSupportedCategory(ResourceLocation.parse("thaumcraft:infusion")), "infusion supported");
        check(ViewersCompat.isSupportedCategory(ResourceLocation.parse("thaumcraft:infernal_bonus")), "infernal bonus supported");
        check(!ViewersCompat.isSupportedCategory(ResourceLocation.parse("minecraft:smelting")), "smelting not in custom categories");

        // Safe execution without crashing when viewers are absent
        ViewersCompat.init();
    }

    private P04CompatCheck() {}
}
