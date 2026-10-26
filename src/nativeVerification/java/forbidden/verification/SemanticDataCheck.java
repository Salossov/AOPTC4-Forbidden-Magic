package forbidden.verification;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dedicated semantic data verification suite for V02 (Recipes) and V03 (Research).
 * Validates 65 reviewed recipe snapshots and 49 research snapshots.
 * Snapshot equality is not an independent proof of parity with the legacy release.
 * across base and integration datapacks (Botania, Blood Magic, Enchanting).
 * Verifies exact types, results, counts, inputs, components, aspect costs, vis costs,
 * coordinates, complexity, warp, parents, hidden parents, recipe pages, text pages, page order,
 * pattern, key, ingredients, mana, altarSyphon, consumptionRate, drainRate, upgradeLevel,
 * and research flags (concealed, secondary, round, hidden, siblings).
 * Includes extensive negative mutation regressions for all key semantic properties.
 */
public final class SemanticDataCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("Semantic data check failed: " + reason);
        }
    }

    public record RecipeSpec(
            String type,
            String research,
            String resultId,
            int resultCount,
            int instability,
            String centralInput,
            List<String> components,
            Map<String, Integer> aspects,
            Map<String, Integer> vis,
            List<String> pattern,
            Map<String, String> key,
            List<String> ingredients,
            int mana,
            int altarSyphon,
            int consumptionRate,
            int drainRate,
            int upgradeLevel
    ) {}

    public record ResearchSpec(
            String key,
            int col,
            int row,
            int comp,
            int warp,
            List<String> parents,
            List<String> hiddenParents,
            List<String> recipePages,
            List<String> textPages,
            List<String> pageOrder,
            Map<String, Integer> aspects,
            boolean autoUnlock,
            boolean stub,
            boolean concealed,
            boolean secondary,
            boolean round,
            boolean hidden,
            List<String> siblings
    ) {}

    private static final List<String> RECIPE_FILES = List.of(
            "/data/forbidden/recipe/arcane_cake_hardcore.json",
            "/data/forbidden/recipe/arcane_cake_normal.json",
            "/data/forbidden/recipe/black_ink.json",
            "/data/forbidden/recipe/crystalwell.json",
            "/data/forbidden/recipe/emerald.json",
            "/data/forbidden/recipe/emerald_nuggets.json",
            "/data/forbidden/recipe/emerald_transmutation.json",
            "/data/forbidden/recipe/focus_blink.json",
            "/packs/cage/data/forbidden/recipe/fork.json",
            "/data/forbidden/recipe/heretic_study.json",
            "/data/forbidden/recipe/mob_crystal.json",
            "/data/forbidden/recipe/morph_axe.json",
            "/data/forbidden/recipe/morph_pickaxe.json",
            "/data/forbidden/recipe/morph_shovel.json",
            "/data/forbidden/recipe/morph_sword.json",
            "/data/forbidden/recipe/nether_stars.json",
            "/data/forbidden/recipe/primewell.json",
            "/data/forbidden/recipe/riding_crop.json",
            "/packs/gluttony/data/forbidden/recipe/ring_food.json",
            "/packs/gluttony_hard/data/forbidden/recipe/ring_food.json",
            "/data/forbidden/recipe/skull_axe.json",
            "/data/forbidden/recipe/star_block.json",
            "/packs/lust/data/forbidden/recipe/sub_collar.json",
            "/data/forbidden/recipe/taint_bricks.json",
            "/data/forbidden/recipe/taint_coal.json",
            "/data/forbidden/recipe/taint_pick.json",
            "/data/forbidden/recipe/taint_planks.json",
            "/data/forbidden/recipe/taint_sapling.json",
            "/data/forbidden/recipe/taint_shovel.json",
            "/data/forbidden/recipe/taint_stone.json",
            "/data/forbidden/recipe/umbral_bush.json",
            "/data/forbidden/recipe/wand_rod_infernal.json",
            "/data/forbidden/recipe/wand_rod_tainted.json",
            "/data/forbidden/recipe/wrath_cage.json",
            "/packs/bloodmagic_wands/data/forbidden/recipe/altar_wand_rod_blood.json",
            "/packs/bloodmagic/data/forbidden/recipe/blood_rapier.json",
            "/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json",
            "/packs/bloodmagic/data/forbidden/recipe/bloodwell.json",
            "/packs/bloodmagic/data/forbidden/recipe/boundwell.json",
            "/packs/bloodmagic/data/forbidden/recipe/eldritch_orb.json",
            "/packs/bloodmagic_wands/data/forbidden/recipe/wand_cap_alchemical.json",
            "/packs/bloodmagic_wands/data/forbidden/recipe/wand_rod_blood.json",
            "/packs/bloodmagic_wands/data/forbidden/recipe/wand_rod_blood_staff.json",
            "/packs/botania/data/forbidden/recipe/botanist_emblem.json",
            "/packs/botania/data/forbidden/recipe/euclidaisy.json",
            "/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_cap_manasteel.json",
            "/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_dreamwood.json",
            "/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json",
            "/packs/botania/data/forbidden/recipe/tainthistle.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_cap_elementium.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_cap_elementium_inert.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_cap_manasteel_inert.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_cap_terrasteel.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_rod_dreamwood.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_rod_dreamwood_staff.json",
            "/packs/botania_wands/data/forbidden/recipe/wand_rod_livingwood.json",
            "/packs/botania/data/forbidden/recipe/whisperweed.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_cluster.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_consuming.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_corrupting.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_educational.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_greedy.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_impact.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_voidtouched.json",
            "/packs/enchanting/data/forbidden/recipe/enchant_wrath.json"
    );

    private static final List<String> RESEARCH_FILES = List.of(
            "/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json",
            "/packs/gluttony_hard/data/forbidden/thaumcraft/research/arcanecake.json",
            "/data/forbidden/thaumcraft/research/blackflower.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/cluster.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/consuming.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/corrupting.json",
            "/data/forbidden/thaumcraft/research/crystalwell.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/educational.json",
            "/data/forbidden/thaumcraft/research/focusblink.json",
            "/packs/cage/data/forbidden/thaumcraft/research/fork.json",
            "/packs/greedy/data/forbidden/thaumcraft/research/greedy.json",
            "/data/forbidden/thaumcraft/research/hellfire.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/impact.json",
            "/data/forbidden/thaumcraft/research/morphtools.json",
            "/data/forbidden/thaumcraft/research/nethershards.json",
            "/data/forbidden/thaumcraft/research/primewell.json",
            "/data/forbidden/thaumcraft/research/ridingcrop.json",
            "/packs/gluttony/data/forbidden/thaumcraft/research/ringfood.json",
            "/data/forbidden/thaumcraft/research/rod_infernal.json",
            "/data/forbidden/thaumcraft/research/rod_profane.json",
            "/data/forbidden/thaumcraft/research/rod_tainted.json",
            "/packs/schools/data/forbidden/thaumcraft/research/schools.json",
            "/data/forbidden/thaumcraft/research/skullaxe.json",
            "/packs/lust/data/forbidden/thaumcraft/research/subcollar.json",
            "/data/forbidden/thaumcraft/research/taintpick.json",
            "/data/forbidden/thaumcraft/research/taintshovel.json",
            "/data/forbidden/thaumcraft/research/taintstone.json",
            "/data/forbidden/thaumcraft/research/tainttree.json",
            "/packs/emerald/data/forbidden/thaumcraft/research/transemerald.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/voidtouched.json",
            "/packs/enchanting/data/forbidden/thaumcraft/research/wrath.json",
            "/packs/cage/data/forbidden/thaumcraft/research/wrathcage.json",
            "/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodmagic.json",
            "/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodrapier.json",
            "/packs/bloodthorn/data/forbidden/thaumcraft/research/bloodthorn.json",
            "/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodwell.json",
            "/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/cap_alchemical.json",
            "/packs/bloodmagic/data/forbidden/thaumcraft/research/eldritch_orb.json",
            "/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/rod_blood.json",
            "/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/rod_blood_staff.json",
            "/packs/botania/data/forbidden/thaumcraft/research/botany.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/cap_elementium.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/cap_manasteel.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/cap_terrasteel.json",
            "/packs/botania/data/forbidden/thaumcraft/research/euclidaisy.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/rod_dreamwood.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/rod_dreamwood_staff.json",
            "/packs/botania_wands/data/forbidden/thaumcraft/research/rod_livingwood.json",
            "/packs/botania/data/forbidden/thaumcraft/research/tainthistle.json",
            "/packs/botania/data/forbidden/thaumcraft/research/whisperweed.json"
    );

    private static final Map<String, RecipeSpec> RECIPE_SPECS = Map.ofEntries(
            Map.entry("/data/forbidden/recipe/arcane_cake_hardcore.json", new RecipeSpec("thaumcraft:infusion", "forbidden:arcanecake", "forbidden:arcane_cake", 1, 3, "minecraft:cake", List.of("thaumcraft:salis_mundus", "forbidden:star_block", "forbidden:star_block", "minecraft:egg", "forbidden:gluttony_shard", "forbidden:gluttony_shard"), Map.ofEntries(Map.entry("forbidden:gula", 12), Map.entry("fames", 24), Map.entry("fabrico", 24)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/arcane_cake_normal.json", new RecipeSpec("thaumcraft:infusion", "forbidden:arcanecake", "forbidden:arcane_cake", 1, 3, "minecraft:cake", List.of("thaumcraft:salis_mundus", "minecraft:egg", "minecraft:milk_bucket", "minecraft:egg", "forbidden:gluttony_shard", "forbidden:gluttony_shard"), Map.ofEntries(Map.entry("forbidden:gula", 12), Map.entry("fames", 24), Map.entry("fabrico", 24)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/black_ink.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:black_dye", 2, 0, null, List.of(), Map.of(), Map.of(), List.of("#"), Map.ofEntries(Map.entry("#", "forbidden:ink_flower")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/crystalwell.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:crystalwell", "forbidden:crystalwell", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aqua", 1), Map.entry("ordo", 1)), List.of(), Map.of(), List.of("thaumcraft:scribing_tools", "#c:dyes/black", "#forbidden:research_shards", "#forbidden:research_shards"), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/emerald.json", new RecipeSpec("minecraft:crafting_shaped", null, "minecraft:emerald", 1, 0, null, List.of(), Map.of(), Map.of(), List.of("###", "###", "###"), Map.ofEntries(Map.entry("#", "forbidden:emerald_nugget")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/emerald_nuggets.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:emerald_nugget", 9, 0, null, List.of(), Map.of(), Map.of(), List.of("#"), Map.ofEntries(Map.entry("#", "minecraft:emerald")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/emerald_transmutation.json", new RecipeSpec("thaumcraft:crucible", "forbidden:transemerald", "forbidden:emerald_nugget", 4, 0, "#c:nuggets/emerald", List.of(), Map.ofEntries(Map.entry("vitreus", 2), Map.entry("lucrum", 2)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/focus_blink.json", new RecipeSpec("thaumcraft:infusion", "forbidden:focusblink", "forbidden:blink_focus", 1, 3, "thaumcraft:focus_warding", List.of("minecraft:ender_eye", "minecraft:ender_pearl", "minecraft:ender_pearl", "forbidden:sloth_shard"), Map.ofEntries(Map.entry("iter", 16), Map.entry("motus", 16), Map.entry("praecantatio", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/cage/data/forbidden/recipe/fork.json", new RecipeSpec("thaumcraft:infusion", "forbidden:fork", "forbidden:diabolist_fork", 1, 3, "minecraft:iron_bars", List.of("forbidden:wrath_shard", "forbidden:wrath_shard", "minecraft:quartz", "minecraft:quartz"), Map.ofEntries(Map.entry("forbidden:ira", 16), Map.entry("telum", 16), Map.entry("spiritus", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/heretic_study.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:heretic_study", 1, 0, null, List.of(), Map.of(), Map.of(), List.of("PPP", " B ", " S "), Map.ofEntries(Map.entry("P", "forbidden:taint_planks"), Map.entry("B", "minecraft:book"), Map.entry("S", "forbidden:taint_stone")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/mob_crystal.json", new RecipeSpec("thaumcraft:crucible", "forbidden:wrathcage", "forbidden:mob_crystal", 1, 0, "minecraft:diamond", List.of(), Map.ofEntries(Map.entry("cognitio", 10), Map.entry("potentia", 10)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/morph_axe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_axe", 1, 2, "thaumcraft:elemental_axe", List.of("forbidden:sloth_shard", "forbidden:sloth_shard", "thaumcraft:fire_shard", "thaumcraft:air_shard", "minecraft:quartz"), Map.ofEntries(Map.entry("permutatio", 16), Map.entry("instrumentum", 8), Map.entry("arbor", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/morph_pickaxe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_pickaxe", 1, 2, "thaumcraft:elemental_pickaxe", List.of("forbidden:sloth_shard", "forbidden:sloth_shard", "thaumcraft:order_shard", "thaumcraft:entropy_shard", "minecraft:quartz"), Map.ofEntries(Map.entry("permutatio", 16), Map.entry("instrumentum", 8), Map.entry("perfodio", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/morph_shovel.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_shovel", 1, 2, "thaumcraft:elemental_shovel", List.of("forbidden:sloth_shard", "forbidden:sloth_shard", "thaumcraft:earth_shard", "thaumcraft:water_shard", "minecraft:quartz"), Map.ofEntries(Map.entry("permutatio", 16), Map.entry("instrumentum", 8), Map.entry("terra", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/morph_sword.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_sword", 1, 2, "thaumcraft:elemental_sword", List.of("forbidden:sloth_shard", "forbidden:sloth_shard", "forbidden:wrath_shard", "thaumcraft:entropy_shard", "minecraft:quartz"), Map.ofEntries(Map.entry("permutatio", 16), Map.entry("instrumentum", 8), Map.entry("telum", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/nether_stars.json", new RecipeSpec("minecraft:crafting_shaped", null, "minecraft:nether_star", 9, 0, null, List.of(), Map.of(), Map.of(), List.of("#"), Map.ofEntries(Map.entry("#", "forbidden:star_block")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/primewell.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:primewell", "forbidden:primewell", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 50), Map.entry("ignis", 50), Map.entry("terra", 50), Map.entry("aqua", 50), Map.entry("ordo", 50), Map.entry("perditio", 50)), List.of(), Map.of(), List.of("minecraft:feather", "thaumcraft:primal_pearl", "minecraft:glass_bottle"), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/riding_crop.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:riding_crop", 1, 0, null, List.of(), Map.of(), Map.of(), List.of("  L", " S ", "S  "), Map.ofEntries(Map.entry("L", "minecraft:leather"), Map.entry("S", "minecraft:stick")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/gluttony/data/forbidden/recipe/ring_food.json", new RecipeSpec("thaumcraft:arcane", "forbidden:ringfood", "forbidden:ring_nutrition", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("perditio", 10), Map.entry("aqua", 20), Map.entry("terra", 20)), List.of(" D ", "SXS", " S "), Map.ofEntries(Map.entry("D", "minecraft:diamond"), Map.entry("S", "forbidden:gluttony_shard"), Map.entry("X", "thaumcraft:ring_blank")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/gluttony_hard/data/forbidden/recipe/ring_food.json", new RecipeSpec("thaumcraft:arcane", "forbidden:ringfood", "forbidden:ring_nutrition", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("perditio", 10), Map.entry("aqua", 20), Map.entry("terra", 20)), List.of(" D ", "SXS", " S "), Map.ofEntries(Map.entry("D", "forbidden:star_block"), Map.entry("S", "forbidden:gluttony_shard"), Map.entry("X", "thaumcraft:ring_blank")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/skull_axe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:skullaxe", "forbidden:skull_axe", 1, 1, "thaumcraft:thaumium_axe", List.of("forbidden:wrath_shard", "forbidden:wrath_shard", "minecraft:diamond", "minecraft:diamond"), Map.ofEntries(Map.entry("forbidden:ira", 8), Map.entry("telum", 8), Map.entry("forbidden:infernus", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/star_block.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:star_block", 1, 0, null, List.of(), Map.of(), Map.of(), List.of("###", "###", "###"), Map.ofEntries(Map.entry("#", "minecraft:nether_star")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/lust/data/forbidden/recipe/sub_collar.json", new RecipeSpec("thaumcraft:infusion", "forbidden:subcollar", "forbidden:sub_collar", 1, 2, "thaumcraft:vis_amulet", List.of("forbidden:lust_shard", "forbidden:lust_shard", "minecraft:lead", "minecraft:leather", "minecraft:iron_bars"), Map.ofEntries(Map.entry("forbidden:luxuria", 16), Map.entry("vinculum", 16), Map.entry("auram", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_bricks.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:taint_bricks", 4, 0, null, List.of(), Map.of(), Map.of(), List.of("##", "##"), Map.ofEntries(Map.entry("#", "forbidden:taint_stone")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_coal.json", new RecipeSpec("minecraft:smelting", null, "forbidden:taint_coal", 1, 0, null, List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_pick.json", new RecipeSpec("thaumcraft:infusion", "forbidden:taintpick", "forbidden:taint_pickaxe", 1, 1, "thaumcraft:thaumium_pickaxe", List.of("thaumcraft:entropy_shard", "forbidden:taint_shard", "minecraft:diamond", "thaumcraft:greatwood_log"), Map.ofEntries(Map.entry("perditio", 8), Map.entry("perfodio", 8), Map.entry("vitium", 12)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_planks.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:taint_planks", 4, 0, null, List.of(), Map.of(), Map.of(), List.of("#"), Map.ofEntries(Map.entry("#", "forbidden:taint_log")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_sapling.json", new RecipeSpec("thaumcraft:crucible", "forbidden:tainttree", "forbidden:taint_sapling", 1, 0, "minecraft:oak_sapling", List.of(), Map.ofEntries(Map.entry("vitium", 10), Map.entry("venenum", 4)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_shovel.json", new RecipeSpec("thaumcraft:infusion", "forbidden:taintshovel", "forbidden:taint_shovel", 1, 5, "thaumcraft:thaumium_shovel", List.of("thaumcraft:order_shard", "forbidden:taint_shard", "minecraft:diamond", "thaumcraft:silverwood_log"), Map.ofEntries(Map.entry("sano", 8), Map.entry("vitreus", 24), Map.entry("perfodio", 12)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/taint_stone.json", new RecipeSpec("thaumcraft:arcane", "forbidden:taintstone", "forbidden:taint_stone", 9, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("perditio", 2), Map.entry("ordo", 1)), List.of("SSS", "SXS", "SSS"), Map.ofEntries(Map.entry("S", "minecraft:stone"), Map.entry("X", "#forbidden:resource_taint")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/umbral_bush.json", new RecipeSpec("thaumcraft:crucible", "forbidden:blackflower", "forbidden:umbral_bush", 1, 0, "minecraft:rose_bush", List.of(), Map.ofEntries(Map.entry("tenebrae", 8), Map.entry("victus", 5)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/wand_rod_infernal.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_infernal", "forbidden:wand_rod_infernal", 1, 5, "minecraft:blaze_rod", List.of("thaumcraft:salis_mundus", "forbidden:pride_shard", "forbidden:pride_shard", "minecraft:soul_sand", "minecraft:wither_skeleton_skull", "minecraft:quartz", "minecraft:blaze_powder"), Map.ofEntries(Map.entry("forbidden:infernus", 32), Map.entry("praecantatio", 12), Map.entry("forbidden:superbia", 12)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/wand_rod_tainted.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_tainted", "forbidden:wand_rod_tainted", 1, 5, "thaumcraft:taint_tendril", List.of("thaumcraft:salis_mundus", "forbidden:taint_shard", "forbidden:taint_shard", "forbidden:taint_shard", "forbidden:taint_shard", "forbidden:taint_shard", "forbidden:taint_shard"), Map.ofEntries(Map.entry("vitium", 24), Map.entry("praecantatio", 12), Map.entry("perditio", 12)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/data/forbidden/recipe/wrath_cage.json", new RecipeSpec("thaumcraft:infusion", "forbidden:wrathcage", "forbidden:wrath_cage", 1, 10, "thaumcraft:thaumium_block", List.of("forbidden:wrath_shard", "forbidden:wrath_shard", "forbidden:wrath_shard", "forbidden:wrath_shard", "minecraft:diamond", "minecraft:diamond", "minecraft:diamond", "minecraft:diamond", "thaumcraft:warded_jar", "thaumcraft:warded_jar", "thaumcraft:warded_jar"), Map.ofEntries(Map.entry("forbidden:ira", 32), Map.entry("praecantatio", 32), Map.entry("bestia", 32), Map.entry("machina", 16)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/recipe/altar_wand_rod_blood.json", new RecipeSpec("bloodmagic:altar", null, "forbidden:wand_rod_blood", 1, 0, "forbidden:wand_rod_blood_inert", List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 0, 20000, 15, 20, 3)),
            Map.entry("/packs/bloodmagic/data/forbidden/recipe/blood_rapier.json", new RecipeSpec("thaumcraft:infusion", "forbidden:bloodrapier", "forbidden:blood_rapier", 1, 8, "thaumcraft:void_sword", List.of("thaumcraft:void_seed", "forbidden:wrath_shard", "forbidden:gluttony_shard", "minecraft:feather", "thaumcraft:quicksilver"), Map.ofEntries(Map.entry("victus", 18), Map.entry("fames", 32), Map.entry("telum", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json", new RecipeSpec("bloodmagic:altar", null, "forbidden:bloodthorn", 1, 0, "botania:red_mystical_flower", List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 0, 15000, 25, 50, 3)),
            Map.entry("/packs/bloodmagic/data/forbidden/recipe/bloodwell.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:bloodwell", "forbidden:bloodwell", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aqua", 10), Map.entry("terra", 10)), List.of(), Map.of(), List.of("minecraft:feather", "bloodmagic:life_essence_bucket", "minecraft:glass_bottle", "bloodmagic:weakbloodorb"), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic/data/forbidden/recipe/boundwell.json", new RecipeSpec("minecraft:crafting_shapeless", null, "forbidden:boundwell", 1, 0, null, List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of("forbidden:crystalwell", "bloodmagic:weakbloodorb"), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic/data/forbidden/recipe/eldritch_orb.json", new RecipeSpec("thaumcraft:infusion", "forbidden:eldritch_orb", "forbidden:eldritch_orb", 1, 2, "bloodmagic:archmagebloodorb", List.of("thaumcraft:primal_pearl", "thaumcraft:void_seed", "minecraft:nether_star", "forbidden:taint_coal"), Map.ofEntries(Map.entry("victus", 64), Map.entry("alienis", 48), Map.entry("tenebrae", 32), Map.entry("vacuos", 64)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/recipe/wand_cap_alchemical.json", new RecipeSpec("thaumcraft:infusion", "forbidden:cap_alchemical", "forbidden:wand_cap_alchemical", 1, 3, "thaumcraft:iron_wand_cap", List.of("bloodmagic:reagentbinding", "bloodmagic:reagentbinding", "bloodmagic:reagentbinding"), Map.ofEntries(Map.entry("victus", 12), Map.entry("aqua", 6)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/recipe/wand_rod_blood.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_blood", "forbidden:wand_rod_blood_inert", 1, 2, "bloodmagic:masterbloodorb", List.of("bloodmagic:reagentbinding", "bloodmagic:infusedslate", "bloodmagic:infusedslate", "bloodmagic:reagentvoid", "bloodmagic:reagentbloodlight", "bloodmagic:reagentwater", "bloodmagic:reagentlava", "bloodmagic:reagentgrowth", "bloodmagic:reagentair"), Map.ofEntries(Map.entry("victus", 32), Map.entry("praecantatio", 12), Map.entry("aqua", 16)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/recipe/wand_rod_blood_staff.json", new RecipeSpec("thaumcraft:arcane", "forbidden:rod_blood_staff", "forbidden:wand_rod_blood_staff", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 26), Map.entry("terra", 26), Map.entry("ignis", 26), Map.entry("aqua", 26), Map.entry("ordo", 26), Map.entry("perditio", 26)), List.of("  D", " B ", "B  "), Map.ofEntries(Map.entry("B", "forbidden:wand_rod_blood"), Map.entry("D", "bloodmagic:demonslate")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania/data/forbidden/recipe/botanist_emblem.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:botanist_emblem", 1, 0, null, List.of(), Map.of(), Map.of(), List.of(" P ", "PEP", " P "), Map.ofEntries(Map.entry("P", "#botania:petals"), Map.entry("E", "minecraft:emerald")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania/data/forbidden/recipe/euclidaisy.json", new RecipeSpec("thaumcraft:infusion", "forbidden:euclidaisy", "forbidden:euclidaisy", 1, 8, "botania:cyan_mystical_flower", List.of("thaumcraft:salis_mundus", "botania:mana_pearl", "botania:mana_diamond", "botania:cyan_mystical_petal", "botania:cyan_mystical_petal", "botania:rune_of_autumn", "botania:rune_of_winter"), Map.ofEntries(Map.entry("auram", 8), Map.entry("alienis", 10), Map.entry("praecantatio", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_cap_manasteel.json", new RecipeSpec("botania:mana_infusion", null, "forbidden:wand_cap_manasteel", 1, 0, "forbidden:wand_cap_manasteel_inert", List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 1000, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_dreamwood.json", new RecipeSpec("botania:mana_infusion", null, "forbidden:wand_rod_dreamwood", 1, 0, "forbidden:wand_rod_dreamwood_inert", List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 10000, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json", new RecipeSpec("botania:mana_infusion", null, "forbidden:wand_rod_livingwood", 1, 0, "forbidden:wand_rod_livingwood_inert", List.of(), Map.of(), Map.of(), List.of(), Map.of(), List.of(), 10000, 0, 0, 0, 0)),
            Map.entry("/packs/botania/data/forbidden/recipe/tainthistle.json", new RecipeSpec("thaumcraft:infusion", "forbidden:tainthistle", "forbidden:tainthistle", 1, 4, "forbidden:taint_stone", List.of("botania:purple_mystical_petal", "botania:purple_mystical_petal", "botania:purple_mystical_petal", "botania:rune_of_wrath", "botania:rune_of_spring"), Map.ofEntries(Map.entry("vitium", 8), Map.entry("fames", 4), Map.entry("aer", 4), Map.entry("aqua", 4)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_cap_elementium.json", new RecipeSpec("thaumcraft:infusion", "forbidden:cap_elementium", "forbidden:wand_cap_elementium", 1, 1, "forbidden:wand_cap_elementium_inert", List.of("botania:pixie_dust", "botania:rune_of_water", "botania:rune_of_fire", "botania:rune_of_earth", "botania:rune_of_air"), Map.ofEntries(Map.entry("terra", 18), Map.entry("ignis", 18), Map.entry("aqua", 18), Map.entry("aer", 18)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_cap_elementium_inert.json", new RecipeSpec("thaumcraft:arcane", "forbidden:cap_elementium", "forbidden:wand_cap_elementium_inert", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 27), Map.entry("terra", 27), Map.entry("ignis", 27), Map.entry("aqua", 27), Map.entry("ordo", 27), Map.entry("perditio", 27)), List.of("NNN", "N N"), Map.ofEntries(Map.entry("N", "forbidden:elementium_nugget")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_cap_manasteel_inert.json", new RecipeSpec("thaumcraft:arcane", "forbidden:cap_manasteel", "forbidden:wand_cap_manasteel_inert", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 6), Map.entry("terra", 6), Map.entry("ignis", 6), Map.entry("aqua", 6), Map.entry("ordo", 6), Map.entry("perditio", 6)), List.of("NNN", "N N"), Map.ofEntries(Map.entry("N", "forbidden:manasteel_nugget")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_cap_terrasteel.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:wand_cap_terrasteel", 1, 0, null, List.of(), Map.of(), Map.of(), List.of("NNN", "N N"), Map.ofEntries(Map.entry("N", "botania:terrasteel_nugget")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_rod_dreamwood.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_dreamwood_staff", "forbidden:wand_rod_dreamwood_inert", 1, 3, "botania:dreamwood_log", List.of("botania:mana_diamond", "botania:rune_of_mana", "botania:rune_of_water", "botania:rune_of_fire", "botania:rune_of_earth", "botania:rune_of_air", "botania:rune_of_spring", "botania:rune_of_summer", "botania:rune_of_autumn", "botania:rune_of_winter"), Map.ofEntries(Map.entry("praecantatio", 16), Map.entry("arbor", 32), Map.entry("victus", 16), Map.entry("sensus", 8), Map.entry("herba", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_rod_dreamwood_staff.json", new RecipeSpec("thaumcraft:arcane", "forbidden:rod_dreamwood_staff", "forbidden:wand_rod_dreamwood_staff", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 26), Map.entry("terra", 26), Map.entry("ignis", 26), Map.entry("aqua", 26), Map.entry("ordo", 26), Map.entry("perditio", 26)), List.of("  D", " B ", "B  "), Map.ofEntries(Map.entry("B", "forbidden:wand_rod_dreamwood"), Map.entry("D", "botania:dragonstone")), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania_wands/data/forbidden/recipe/wand_rod_livingwood.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_livingwood", "forbidden:wand_rod_livingwood_inert", 1, 3, "botania:livingwood_log", List.of("botania:mana_diamond", "botania:rune_of_mana", "botania:rune_of_water", "botania:rune_of_fire", "botania:rune_of_earth", "botania:rune_of_air", "botania:rune_of_spring", "botania:rune_of_summer", "botania:rune_of_autumn", "botania:rune_of_winter"), Map.ofEntries(Map.entry("praecantatio", 16), Map.entry("arbor", 32), Map.entry("victus", 16), Map.entry("sensus", 8), Map.entry("herba", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/botania/data/forbidden/recipe/whisperweed.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:whisperweed", "forbidden:whisperweed", 1, 0, null, List.of(), Map.of(), Map.ofEntries(Map.entry("aer", 10), Map.entry("terra", 10), Map.entry("ignis", 10), Map.entry("aqua", 10), Map.entry("ordo", 10), Map.entry("perditio", 10)), List.of(), Map.of(), List.of("minecraft:short_grass", "forbidden:manasteel_nugget", "botania:mana_diamond", "thaumcraft:amber", "botania:gray_mystical_petal", "botania:magenta_mystical_petal", "botania:rune_of_greed", "thaumcraft:tallow"), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_cluster.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:cluster", "", 1, 3, null, List.of("thaumcraft:elemental_pickaxe", "thaumcraft:salis_mundus"), Map.ofEntries(Map.entry("ignis", 4), Map.entry("metallum", 4), Map.entry("lucrum", 4)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_consuming.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:consuming", "", 1, 3, null, List.of("minecraft:iron_pickaxe", "minecraft:lava_bucket", "thaumcraft:salis_mundus"), Map.ofEntries(Map.entry("vacuos", 8), Map.entry("instrumentum", 8), Map.entry("fames", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_corrupting.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:corrupting", "", 1, 4, null, List.of("minecraft:nether_wart", "minecraft:soul_sand", "thaumcraft:salis_mundus"), Map.ofEntries(Map.entry("forbidden:infernus", 16), Map.entry("permutatio", 16), Map.entry("vitreus", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_educational.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:educational", "", 1, 3, null, List.of("thaumcraft:zombie_brain", "minecraft:book", "thaumcraft:salis_mundus"), Map.ofEntries(Map.entry("praecantatio", 4), Map.entry("telum", 4), Map.entry("cognitio", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_greedy.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:greedy", "", 1, 4, null, List.of("minecraft:golden_sword", "minecraft:diamond", "forbidden:greed_shard", "forbidden:greed_shard", "thaumcraft:salis_mundus"), Map.ofEntries(Map.entry("forbidden:infernus", 16), Map.entry("telum", 8), Map.entry("lucrum", 16)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_impact.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:impact", "", 1, 4, null, List.of("thaumcraft:salis_mundus", "forbidden:envy_shard", "forbidden:envy_shard", "thaumcraft:elemental_shovel"), Map.ofEntries(Map.entry("perditio", 16), Map.entry("perfodio", 16)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_voidtouched.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:voidtouched", "", 1, 8, null, List.of("thaumcraft:salis_mundus", "thaumcraft:void_ingot", "thaumcraft:void_ingot", "thaumcraft:void_ingot", "forbidden:envy_shard", "forbidden:envy_shard", "forbidden:envy_shard"), Map.ofEntries(Map.entry("vacuos", 16), Map.entry("tenebrae", 16), Map.entry("forbidden:invidia", 24), Map.entry("alienis", 16)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0)),
            Map.entry("/packs/enchanting/data/forbidden/recipe/enchant_wrath.json", new RecipeSpec("thaumcraft:infusion_enchantment", "forbidden:wrath", "", 1, 8, null, List.of("thaumcraft:salis_mundus", "minecraft:diamond_sword", "forbidden:wrath_shard", "forbidden:wrath_shard", "forbidden:wrath_shard"), Map.ofEntries(Map.entry("forbidden:ira", 16), Map.entry("telum", 16), Map.entry("forbidden:infernus", 8)), Map.of(), List.of(), Map.of(), List.of(), 0, 0, 0, 0, 0))
    );

    private static final Map<String, ResearchSpec> RESEARCH_SPECS = Map.ofEntries(
            Map.entry("/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json", new ResearchSpec("forbidden:arcanecake", -12, -6, 2, 0, List.of("forbidden:nethershards", "INFUSION"), List.of(), List.of("ArcaneCake"), List.of("forbidden.research_page.ARCANECAKE.1"), List.of("forbidden.research_page.ARCANECAKE.1", "ArcaneCake"), Map.ofEntries(Map.entry("forbidden:gula", 6), Map.entry("fames", 3), Map.entry("fabrico", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/blackflower.json", new ResearchSpec("forbidden:blackflower", -2, -6, 1, 0, List.of(), List.of(), List.of("BlackFlower", "BlackInk"), List.of("forbidden.research_page.BLACKFLOWER.1"), List.of("forbidden.research_page.BLACKFLOWER.1", "BlackFlower", "BlackInk"), Map.ofEntries(Map.entry("herba", 3), Map.entry("sensus", 2), Map.entry("tenebrae", 4)), false, false, false, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/cluster.json", new ResearchSpec("forbidden:cluster", -10, -1, 3, 0, List.of("forbidden:morphtools", "ELEMENTALPICK"), List.of(), List.of("Cluster"), List.of("forbidden.research_page.CLUSTER.1"), List.of("forbidden.research_page.CLUSTER.1", "Cluster"), Map.ofEntries(Map.entry("metallum", 1), Map.entry("ignis", 4), Map.entry("forbidden:invidia", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/consuming.json", new ResearchSpec("forbidden:consuming", -4, -8, 1, 0, List.of("INFUSIONENCHANTMENT"), List.of(), List.of("Consuming"), List.of("forbidden.research_page.CONSUMING.1"), List.of("forbidden.research_page.CONSUMING.1", "Consuming"), Map.ofEntries(Map.entry("vacuos", 4), Map.entry("perditio", 3), Map.entry("praecantatio", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/corrupting.json", new ResearchSpec("forbidden:corrupting", -6, -4, 1, 0, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of(), List.of("Corrupting"), List.of("forbidden.research_page.CORRUPTING.1"), List.of("forbidden.research_page.CORRUPTING.1", "Corrupting"), Map.ofEntries(Map.entry("forbidden:infernus", 5), Map.entry("vitreus", 2), Map.entry("permutatio", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/crystalwell.json", new ResearchSpec("forbidden:crystalwell", -2, -8, 1, 0, List.of("RESEARCH"), List.of(), List.of("Crystalwell"), List.of("forbidden.research_page.CRYSTALWELL.1"), List.of("forbidden.research_page.CRYSTALWELL.1", "Crystalwell"), Map.ofEntries(Map.entry("cognitio", 3), Map.entry("vitreus", 2), Map.entry("praecantatio", 1)), false, false, false, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/educational.json", new ResearchSpec("forbidden:educational", -4, -7, 2, 0, List.of("INFUSIONENCHANTMENT"), List.of(), List.of("Educational"), List.of("forbidden.research_page.EDUCATIONAL.1"), List.of("forbidden.research_page.EDUCATIONAL.1", "Educational"), Map.ofEntries(Map.entry("cognitio", 5), Map.entry("telum", 1), Map.entry("praecantatio", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/focusblink.json", new ResearchSpec("forbidden:focusblink", -11, -3, 2, 0, List.of("forbidden:nethershards", "INFUSION"), List.of(), List.of("FocusBlink"), List.of("forbidden.research_page.FOCUSBLINK.1"), List.of("forbidden.research_page.FOCUSBLINK.1", "FocusBlink"), Map.ofEntries(Map.entry("iter", 3), Map.entry("perditio", 3), Map.entry("forbidden:infernus", 3), Map.entry("forbidden:desidia", 6)), false, false, true, false, false, false, List.of())),
            // ForbiddenResearch.java:91: ResearchPage("WRATHCAGE", "forbidden.research_page.FORK.wc") is gated text after the recipe.
            Map.entry("/packs/cage/data/forbidden/thaumcraft/research/fork.json", new ResearchSpec("forbidden:fork", -6, -7, 0, 0, List.of("INFUSION", "forbidden:nethershards", "THAUMIUM"), List.of(), List.of("Fork"), List.of("forbidden.research_page.FORK.1", "forbidden.research_page.FORK.wc"), List.of("forbidden.research_page.FORK.1", "Fork", "forbidden.research_page.FORK.wc"), Map.ofEntries(Map.entry("forbidden:infernus", 3), Map.entry("machina", 1), Map.entry("instrumentum", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/greedy/data/forbidden/thaumcraft/research/greedy.json", new ResearchSpec("forbidden:greedy", -6, -5, 2, 0, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of(), List.of("Greedy"), List.of("forbidden.research_page.GREEDY.1"), List.of("forbidden.research_page.GREEDY.1", "Greedy"), Map.ofEntries(Map.entry("praecantatio", 2), Map.entry("telum", 1), Map.entry("lucrum", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/hellfire.json", new ResearchSpec("forbidden:hellfire", -8, -8, 1, 0, List.of("forbidden:nethershards"), List.of(), List.of(), List.of("forbidden.research_page.HELLFIRE.1"), List.of("forbidden.research_page.HELLFIRE.1"), Map.ofEntries(Map.entry("ignis", 3), Map.entry("forbidden:infernus", 4), Map.entry("iter", 2)), false, false, false, false, true, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/impact.json", new ResearchSpec("forbidden:impact", -9, -1, 3, 0, List.of("forbidden:morphtools", "ELEMENTALSHOVEL"), List.of(), List.of("Impact"), List.of("forbidden.research_page.IMPACT.1"), List.of("forbidden.research_page.IMPACT.1", "Impact"), Map.ofEntries(Map.entry("perditio", 8), Map.entry("instrumentum", 10), Map.entry("perfodio", 16), Map.entry("forbidden:invidia", 10)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/morphtools.json", new ResearchSpec("forbidden:morphtools", -9, -3, 3, 4, List.of("forbidden:nethershards"), List.of("THAUMIUM", "INFUSIONENCHANTMENT"), List.of("MorphPick", "MorphSword", "MorphShovel", "MorphAxe"), List.of("forbidden.research_page.MORPHTOOLS.1"), List.of("forbidden.research_page.MORPHTOOLS.1", "MorphPick", "MorphSword", "MorphShovel", "MorphAxe"), Map.ofEntries(Map.entry("instrumentum", 2), Map.entry("forbidden:invidia", 3), Map.entry("permutatio", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/nethershards.json", new ResearchSpec("forbidden:nethershards", -8, -5, 0, 0, List.of(), List.of(), List.of(), List.of("forbidden.research_page.NETHERSHARDS.1", "forbidden.research_page.NETHERSHARDS.2", "forbidden.research_page.NETHERSHARDS.3"), List.of("forbidden.research_page.NETHERSHARDS.1", "forbidden.research_page.NETHERSHARDS.2", "forbidden.research_page.NETHERSHARDS.3"), Map.of(), true, true, false, false, true, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/primewell.json", new ResearchSpec("forbidden:primewell", 2, -8, 1, 0, List.of("PRIMPEARL"), List.of(), List.of("Primewell"), List.of("forbidden.research_page.PRIMEWELL.1"), List.of("forbidden.research_page.PRIMEWELL.1", "Primewell"), Map.ofEntries(Map.entry("cognitio", 3), Map.entry("alienis", 6), Map.entry("fabrico", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/ridingcrop.json", new ResearchSpec("forbidden:ridingcrop", 0, -8, 0, 0, List.of(), List.of(), List.of("RidingCrop"), List.of("forbidden.research_page.RIDINGCROP.1"), List.of("forbidden.research_page.RIDINGCROP.1", "RidingCrop"), Map.of(), true, true, false, false, true, false, List.of())),
            Map.entry("/packs/gluttony/data/forbidden/thaumcraft/research/ringfood.json", new ResearchSpec("forbidden:ringfood", -11, -6, 2, 0, List.of("forbidden:nethershards"), List.of(), List.of("RingFood"), List.of("forbidden.research_page.RINGFOOD.1"), List.of("forbidden.research_page.RINGFOOD.1", "RingFood"), Map.ofEntries(Map.entry("forbidden:gula", 4), Map.entry("fames", 3), Map.entry("victus", 2)), false, false, false, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/rod_infernal.json", new ResearchSpec("forbidden:rod_infernal", -11, -4, 3, 2, List.of("ROD_silverwood", "INFUSION", "forbidden:nethershards"), List.of(), List.of("WandRodInfernal"), List.of("forbidden.research_page.ROD_infernal.1", "forbidden.research_page.ROD_infernal.2"), List.of("forbidden.research_page.ROD_infernal.1", "forbidden.research_page.ROD_infernal.2", "WandRodInfernal"), Map.ofEntries(Map.entry("forbidden:infernus", 4), Map.entry("ignis", 3), Map.entry("instrumentum", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/rod_profane.json", new ResearchSpec("forbidden:rod_profane", -9, -8, 0, 0, List.of("forbidden:nethershards"), List.of(), List.of(), List.of("forbidden.research_page.ROD_profane.1"), List.of("forbidden.research_page.ROD_profane.1"), Map.of(), false, true, false, false, true, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/rod_tainted.json", new ResearchSpec("forbidden:rod_tainted", -8, 3, 3, 2, List.of("ROD_silverwood", "forbidden:taintshovel", "INFUSION"), List.of(), List.of("WandRodTainted"), List.of("forbidden.research_page.ROD_tainted.1"), List.of("forbidden.research_page.ROD_tainted.1", "WandRodTainted"), Map.ofEntries(Map.entry("praecantatio", 4), Map.entry("vitium", 5), Map.entry("instrumentum", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/schools/data/forbidden/thaumcraft/research/schools.json", new ResearchSpec("forbidden:schools", -1, 1, 0, 0, List.of(), List.of(), List.of(), List.of("forbidden.research_page.SCHOOLS.1"), List.of("forbidden.research_page.SCHOOLS.1"), Map.of(), true, true, false, false, true, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/skullaxe.json", new ResearchSpec("forbidden:skullaxe", -11, -7, 2, 1, List.of("forbidden:nethershards", "THAUMIUM", "INFUSION"), List.of(), List.of("SkullAxe"), List.of("forbidden.research_page.SKULLAXE.1"), List.of("forbidden.research_page.SKULLAXE.1", "SkullAxe"), Map.ofEntries(Map.entry("telum", 3), Map.entry("forbidden:ira", 4), Map.entry("forbidden:infernus", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/lust/data/forbidden/thaumcraft/research/subcollar.json", new ResearchSpec("forbidden:subcollar", -11, -5, 2, 0, List.of("forbidden:nethershards", "VISAMULET", "INFUSION"), List.of(), List.of("SubCollar"), List.of("forbidden.research_page.SUBCOLLAR.1"), List.of("forbidden.research_page.SUBCOLLAR.1", "SubCollar"), Map.ofEntries(Map.entry("auram", 3), Map.entry("forbidden:luxuria", 8), Map.entry("praecantatio", 3), Map.entry("vinculum", 6), Map.entry("corpus", 4)), false, false, false, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/taintpick.json", new ResearchSpec("forbidden:taintpick", -10, 1, 2, 0, List.of("forbidden:taintshovel"), List.of(), List.of("TaintPick"), List.of("forbidden.research_page.TAINTPICK.1"), List.of("forbidden.research_page.TAINTPICK.1", "TaintPick"), Map.ofEntries(Map.entry("instrumentum", 2), Map.entry("vitium", 4), Map.entry("perditio", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/taintshovel.json", new ResearchSpec("forbidden:taintshovel", -8, 1, 2, 0, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of(), List.of("TaintShovel"), List.of("forbidden.research_page.TAINTSHOVEL.1", "forbidden.research_page.TAINTSHOVEL.2"), List.of("forbidden.research_page.TAINTSHOVEL.1", "forbidden.research_page.TAINTSHOVEL.2", "TaintShovel"), Map.ofEntries(Map.entry("vitreus", 3), Map.entry("vitium", 2), Map.entry("instrumentum", 1)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/taintstone.json", new ResearchSpec("forbidden:taintstone", -10, 3, 2, 0, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of(), List.of("TaintStone", "TaintBrick"), List.of("forbidden.research_page.TAINTSTONE.1"), List.of("forbidden.research_page.TAINTSTONE.1", "TaintStone", "TaintBrick"), Map.ofEntries(Map.entry("terra", 4), Map.entry("vitium", 6), Map.entry("ordo", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/data/forbidden/thaumcraft/research/tainttree.json", new ResearchSpec("forbidden:tainttree", -11, 3, 3, 1, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of(), List.of("TaintTree", "TaintPlank"), List.of("forbidden.research_page.TAINTTREE.1"), List.of("forbidden.research_page.TAINTTREE.1", "TaintTree", "TaintPlank"), Map.ofEntries(Map.entry("arbor", 4), Map.entry("vitium", 6), Map.entry("venenum", 2), Map.entry("herba", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/emerald/data/forbidden/thaumcraft/research/transemerald.json", new ResearchSpec("forbidden:transemerald", 0, -6, 3, 0, List.of("TRANSGOLD"), List.of(), List.of("TransEmerald"), List.of("forbidden.research_page.TRANSEMERALD.1"), List.of("forbidden.research_page.TRANSEMERALD.1", "TransEmerald"), Map.ofEntries(Map.entry("vitreus", 2), Map.entry("permutatio", 5), Map.entry("lucrum", 4)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/voidtouched.json", new ResearchSpec("forbidden:voidtouched", -8, -1, 3, 3, List.of("forbidden:morphtools", "VOIDMETAL"), List.of(), List.of("Voidtouched"), List.of("forbidden.research_page.VOIDTOUCHED.1"), List.of("forbidden.research_page.VOIDTOUCHED.1", "Voidtouched"), Map.ofEntries(Map.entry("alienis", 16), Map.entry("instrumentum", 10), Map.entry("fabrico", 8), Map.entry("forbidden:invidia", 32)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/enchanting/data/forbidden/thaumcraft/research/wrath.json", new ResearchSpec("forbidden:wrath", -6, -3, 3, 3, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of(), List.of("Wrath"), List.of("forbidden.research_page.WRATH.1"), List.of("forbidden.research_page.WRATH.1", "Wrath"), Map.ofEntries(Map.entry("forbidden:ira", 16), Map.entry("telum", 20), Map.entry("praecantatio", 10)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/cage/data/forbidden/thaumcraft/research/wrathcage.json", new ResearchSpec("forbidden:wrathcage", -6, -9, 3, 3, List.of("forbidden:fork"), List.of("INFUSION", "THAUMIUM", "DISTILESSENTIA"), List.of("WrathCage", "MobCrystal"), List.of("forbidden.research_page.WRATHCAGE.1", "forbidden.research_page.WRATHCAGE.2", "forbidden.research_page.WRATHCAGE.3"), List.of("forbidden.research_page.WRATHCAGE.1", "forbidden.research_page.WRATHCAGE.2", "forbidden.research_page.WRATHCAGE.3", "WrathCage", "MobCrystal"), Map.ofEntries(Map.entry("forbidden:ira", 5), Map.entry("machina", 3), Map.entry("bestia", 2)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodmagic.json", new ResearchSpec("forbidden:bloodmagic", -1, -1, 0, 0, List.of("forbidden:schools"), List.of(), List.of(), List.of("forbidden.research_page.BLOODMAGIC.1"), List.of("forbidden.research_page.BLOODMAGIC.1"), Map.of(), true, true, false, false, true, false, List.of())),
            Map.entry("/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodrapier.json", new ResearchSpec("forbidden:bloodrapier", 1, -2, 3, 2, List.of("forbidden:bloodmagic", "INFUSION", "VOIDMETAL"), List.of(), List.of("forbidden:blood_rapier"), List.of("forbidden.research_page.BLOODRAPIER.1"), List.of("forbidden.research_page.BLOODRAPIER.1", "forbidden:blood_rapier"), Map.ofEntries(Map.entry("victus", 16), Map.entry("fames", 12), Map.entry("telum", 12), Map.entry("praecantatio", 12)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/bloodthorn/data/forbidden/thaumcraft/research/bloodthorn.json", new ResearchSpec("forbidden:bloodthorn", -2, -2, 2, 0, List.of("forbidden:bloodmagic"), List.of(), List.of("forbidden:bloodthorn"), List.of("forbidden.research_page.BLOODTHORN.1"), List.of("forbidden.research_page.BLOODTHORN.1", "forbidden:bloodthorn"), Map.ofEntries(Map.entry("herba", 8), Map.entry("victus", 8), Map.entry("telum", 4)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/bloodmagic/data/forbidden/thaumcraft/research/bloodwell.json", new ResearchSpec("forbidden:bloodwell", 1, -1, 1, 0, List.of("forbidden:bloodmagic"), List.of(), List.of("forbidden:bloodwell"), List.of("forbidden.research_page.BLOODWELL.1"), List.of("forbidden.research_page.BLOODWELL.1", "forbidden:bloodwell"), Map.ofEntries(Map.entry("victus", 5), Map.entry("cognitio", 4), Map.entry("sensus", 3)), false, false, false, false, false, false, List.of())),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/cap_alchemical.json", new ResearchSpec("forbidden:cap_alchemical", -3, -3, 2, 0, List.of("forbidden:rod_blood", "CAP_gold"), List.of(), List.of("forbidden:wand_cap_alchemical"), List.of("forbidden.research_page.CAP_alchemical.1"), List.of("forbidden.research_page.CAP_alchemical.1", "forbidden:wand_cap_alchemical"), Map.ofEntries(Map.entry("victus", 4), Map.entry("instrumentum", 1), Map.entry("aqua", 3)), false, false, true, true, false, false, List.of())),
            Map.entry("/packs/bloodmagic/data/forbidden/thaumcraft/research/eldritch_orb.json", new ResearchSpec("forbidden:eldritch_orb", 1, -3, 3, 3, List.of("forbidden:bloodmagic", "INFUSION", "PRIMPEARL", "VOIDMETAL"), List.of(), List.of("forbidden:eldritch_orb"), List.of("forbidden.research_page.ELDRITCHORB.1"), List.of("forbidden.research_page.ELDRITCHORB.1", "forbidden:eldritch_orb"), Map.ofEntries(Map.entry("victus", 24), Map.entry("vacuos", 32), Map.entry("vitreus", 24), Map.entry("alienis", 12)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/rod_blood.json", new ResearchSpec("forbidden:rod_blood", -1, -3, 3, 2, List.of("ROD_silverwood", "INFUSION", "forbidden:bloodmagic"), List.of(), List.of("forbidden:wand_rod_blood"), List.of("forbidden.research_page.ROD_blood.1", "forbidden.research_page.ROD_blood.2"), List.of("forbidden.research_page.ROD_blood.1", "forbidden:wand_rod_blood", "forbidden.research_page.ROD_blood.2"), Map.ofEntries(Map.entry("victus", 5), Map.entry("aqua", 4), Map.entry("telum", 3)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/bloodmagic_wands/data/forbidden/thaumcraft/research/rod_blood_staff.json", new ResearchSpec("forbidden:rod_blood_staff", -3, -2, 2, 0, List.of("ROD_silverwood_staff", "forbidden:rod_blood"), List.of(), List.of("forbidden:wand_rod_blood_staff"), List.of("forbidden.research_page.ROD_blood_staff.1"), List.of("forbidden.research_page.ROD_blood_staff.1", "forbidden:wand_rod_blood_staff"), Map.ofEntries(Map.entry("victus", 8), Map.entry("aqua", 7), Map.entry("telum", 6)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/botania/data/forbidden/thaumcraft/research/botany.json", new ResearchSpec("forbidden:botany", -3, 3, 0, 0, List.of("forbidden:schools"), List.of(), List.of(), List.of("forbidden.research_page.BOTANY.1"), List.of("forbidden.research_page.BOTANY.1"), Map.of(), true, true, false, false, true, false, List.of())),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/cap_elementium.json", new ResearchSpec("forbidden:cap_elementium", 3, 3, 3, 0, List.of("forbidden:rod_dreamwood_staff"), List.of(), List.of("forbidden:wand_cap_elementium_inert", "forbidden:wand_cap_elementium"), List.of("forbidden.research_page.CAP_elementium.1"), List.of("forbidden.research_page.CAP_elementium.1", "forbidden:wand_cap_elementium_inert", "forbidden:wand_cap_elementium"), Map.ofEntries(Map.entry("ignis", 8), Map.entry("praecantatio", 12), Map.entry("aqua", 8), Map.entry("aer", 8), Map.entry("terra", 8)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/cap_manasteel.json", new ResearchSpec("forbidden:cap_manasteel", 1, 2, 2, 0, List.of("forbidden:rod_livingwood"), List.of(), List.of("forbidden:wand_cap_manasteel_inert"), List.of("forbidden.research_page.CAP_manasteel.1"), List.of("forbidden.research_page.CAP_manasteel.1", "forbidden:wand_cap_manasteel_inert"), Map.ofEntries(Map.entry("auram", 6), Map.entry("praecantatio", 4), Map.entry("metallum", 4)), false, false, true, true, false, false, List.of())),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/cap_terrasteel.json", new ResearchSpec("forbidden:cap_terrasteel", 3, 2, 1, 0, List.of("forbidden:cap_manasteel"), List.of(), List.of("forbidden:wand_cap_terrasteel"), List.of("forbidden.research_page.CAP_terrasteel.1"), List.of("forbidden.research_page.CAP_terrasteel.1", "forbidden:wand_cap_terrasteel"), Map.ofEntries(Map.entry("potentia", 1), Map.entry("metallum", 1)), false, false, true, true, false, false, List.of())),
            Map.entry("/packs/botania/data/forbidden/thaumcraft/research/euclidaisy.json", new ResearchSpec("forbidden:euclidaisy", -3, 5, 2, 0, List.of("forbidden:botany", "INFUSION"), List.of(), List.of("forbidden:euclidaisy"), List.of("forbidden.research_page.EUCLIDAISY.1"), List.of("forbidden.research_page.EUCLIDAISY.1", "forbidden:euclidaisy"), Map.ofEntries(Map.entry("herba", 8), Map.entry("praecantatio", 4), Map.entry("auram", 12)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/rod_dreamwood.json", new ResearchSpec("forbidden:rod_dreamwood", 1, 4, 2, 0, List.of("forbidden:rod_livingwood"), List.of(), List.of("forbidden:wand_rod_dreamwood"), List.of(), List.of("forbidden:wand_rod_dreamwood"), Map.ofEntries(Map.entry("arbor", 8), Map.entry("praecantatio", 4), Map.entry("alienis", 6)), false, false, false, false, false, false, List.of())),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/rod_dreamwood_staff.json", new ResearchSpec("forbidden:rod_dreamwood_staff", 1, 3, 2, 0, List.of("forbidden:rod_livingwood", "ROD_silverwood_staff"), List.of(), List.of("forbidden:wand_rod_dreamwood_staff", "forbidden:wand_rod_dreamwood"), List.of("forbidden.research_page.ROD_dreamwood_staff.1"), List.of("forbidden.research_page.ROD_dreamwood_staff.1", "forbidden:wand_rod_dreamwood_staff", "forbidden:wand_rod_dreamwood"), Map.ofEntries(Map.entry("arbor", 16), Map.entry("praecantatio", 8), Map.entry("alienis", 6)), false, false, true, false, false, false, List.of("forbidden:rod_dreamwood"))),
            Map.entry("/packs/botania_wands/data/forbidden/thaumcraft/research/rod_livingwood.json", new ResearchSpec("forbidden:rod_livingwood", -1, 3, 3, 2, List.of("forbidden:botany", "ROD_silverwood", "INFUSION"), List.of(), List.of("forbidden:wand_rod_livingwood"), List.of("forbidden.research_page.ROD_livingwood.1"), List.of("forbidden.research_page.ROD_livingwood.1", "forbidden:wand_rod_livingwood"), Map.ofEntries(Map.entry("arbor", 8), Map.entry("praecantatio", 4), Map.entry("herba", 6)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/botania/data/forbidden/thaumcraft/research/tainthistle.json", new ResearchSpec("forbidden:tainthistle", -2, 5, 2, 0, List.of("forbidden:botany", "INFUSION"), List.of(), List.of("forbidden:tainthistle"), List.of("forbidden.research_page.TAINTHISTLE.1"), List.of("forbidden.research_page.TAINTHISTLE.1", "forbidden:tainthistle"), Map.ofEntries(Map.entry("herba", 8), Map.entry("vitium", 10), Map.entry("praecantatio", 4)), false, false, true, false, false, false, List.of())),
            Map.entry("/packs/botania/data/forbidden/thaumcraft/research/whisperweed.json", new ResearchSpec("forbidden:whisperweed", -4, 5, 1, 0, List.of("forbidden:botany"), List.of(), List.of("forbidden:whisperweed"), List.of("forbidden.research_page.WHISPERWEED.1"), List.of("forbidden.research_page.WHISPERWEED.1", "forbidden:whisperweed"), Map.ofEntries(Map.entry("herba", 8), Map.entry("cognitio", 10), Map.entry("sensus", 4)), false, false, true, false, false, false, List.of()))
    );

    public static void main(String[] args) throws Exception {
        testV02Recipes();
        testV03Research();
        testNegativeRecipeRegressions();
        testNegativeResearchRegressions();

        System.out.println("FORBIDDEN_SEMANTIC_DATA_PASS checks=" + checks);
    }

    private static void testV02Recipes() throws Exception {
        check(RECIPE_FILES.size() == 65, "all 65 recipe definition files indexed");

        int arcaneCount = 0;
        int crucibleCount = 0;
        int infusionCount = 0;
        int craftingCount = 0;
        int smeltingCount = 0;
        int manaInfusionCount = 0;
        int altarCount = 0;

        for (String file : RECIPE_FILES) {
            try (var in = SemanticDataCheck.class.getResourceAsStream(file)) {
                check(in != null, "recipe resource found: " + file);
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                check(json.has("type"), "recipe has type: " + file);
                String type = json.get("type").getAsString();
                check(!type.isBlank(), "recipe type non-blank: " + file);

                validateRecipeJson(json, file);

                if (type.startsWith("thaumcraft:arcane")) arcaneCount++;
                else if (type.equals("thaumcraft:crucible")) crucibleCount++;
                else if (type.startsWith("thaumcraft:infusion")) infusionCount++;
                else if (type.startsWith("minecraft:crafting")) craftingCount++;
                else if (type.equals("minecraft:smelting")) smeltingCount++;
                else if (type.equals("botania:mana_infusion")) manaInfusionCount++;
                else if (type.equals("bloodmagic:altar")) altarCount++;
            }
        }

        check(arcaneCount == 11, "exactly 11 arcane workbench recipe variants verified");
        check(crucibleCount == 4, "exactly 4 crucible recipes verified");
        check(infusionCount == 32, "exactly 32 infusion/infusion_enchantment recipes verified");
        check(craftingCount == 12, "exactly 12 crafting recipes verified");
        check(smeltingCount == 1, "exactly 1 smelting recipe verified");
        check(manaInfusionCount == 3, "exactly 3 mana infusion recipes verified");
        check(altarCount == 2, "exactly 2 altar recipes verified");
    }

    private static JsonObject getJson(String resPath) {
        try (var in = SemanticDataCheck.class.getResourceAsStream(resPath)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + resPath);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateRecipeJson(JsonObject json, String file) {
        String type = json.get("type").getAsString();
        RecipeSpec spec = RECIPE_SPECS.get(file);
        check(spec != null, "recipe spec exists for: " + file);
        check(type.equals(spec.type), "recipe type matches spec: " + type + " vs " + spec.type + " in " + file);

        // 1. Result verification
        if (!type.equals("thaumcraft:infusion_enchantment")) {
            check(json.has("result") || json.has("output"), "recipe has result/output: " + file);
            var resultObj = json.has("result") ? json.get("result") : json.get("output");
            String actualResultId = "";
            int actualCount = 1;
            if (resultObj.isJsonObject()) {
                JsonObject r = resultObj.getAsJsonObject();
                check(r.has("id") || r.has("item"), "result has id/item: " + file);
                actualResultId = r.has("id") ? r.get("id").getAsString() : r.get("item").getAsString();
                if (r.has("count")) {
                    actualCount = r.get("count").getAsInt();
                    check(actualCount > 0, "result count positive: " + file);
                }
            } else if (resultObj.isJsonPrimitive()) {
                actualResultId = resultObj.getAsString();
                check(!actualResultId.isBlank(), "result id non-blank: " + file);
            }
            check(spec.resultId.equals(actualResultId), "result matches spec: " + spec.resultId + " vs " + actualResultId + " in " + file);
            check(spec.resultCount == actualCount, "result count matches spec: " + spec.resultCount + " vs " + actualCount + " in " + file);
        }

        // 2. Research & Instability verification against spec
        if (spec.research != null) {
            check(json.has("research"), "recipe has research: " + file);
            check(spec.research.equals(json.get("research").getAsString()), "research key matches spec: " + spec.research + " in " + file);
        } else {
            check(!json.has("research") || json.get("research").getAsString().isBlank(), "recipe without research spec has none: " + file);
        }

        int actualInstability = json.has("instability") ? json.get("instability").getAsInt() : 0;
        check(actualInstability == spec.instability, "instability matches spec: " + spec.instability + " vs " + actualInstability + " in " + file);

        // 3. Central input verification
        if (spec.centralInput != null) {
            check(json.has("input") || json.has("central_item") || json.has("central") || json.has("catalyst"), "has central item: " + file);
            var cObj = json.has("input") ? json.get("input") : (json.has("central_item") ? json.get("central_item") : (json.has("central") ? json.get("central") : json.get("catalyst")));
            String actualCentral = "";
            if (cObj.isJsonObject()) {
                JsonObject co = cObj.getAsJsonObject();
                if (co.has("item")) actualCentral = co.get("item").getAsString();
                else if (co.has("id")) actualCentral = co.get("id").getAsString();
                else if (co.has("tag")) actualCentral = "#" + co.get("tag").getAsString();
            } else if (cObj.isJsonPrimitive()) {
                actualCentral = cObj.getAsString();
            }
            check(spec.centralInput.equals(actualCentral), "central input matches spec: " + spec.centralInput + " vs " + actualCentral + " in " + file);
        }

        // 4. Components verification
        if (!spec.components.isEmpty()) {
            check(json.has("components"), "recipe has components: " + file);
            List<String> actualComps = new ArrayList<>();
            for (var elem : json.getAsJsonArray("components")) {
                if (elem.isJsonObject()) {
                    JsonObject co = elem.getAsJsonObject();
                    if (co.has("item")) actualComps.add(co.get("item").getAsString());
                    else if (co.has("id")) actualComps.add(co.get("id").getAsString());
                    else if (co.has("tag")) actualComps.add("#" + co.get("tag").getAsString());
                } else if (elem.isJsonPrimitive()) {
                    actualComps.add(elem.getAsString());
                }
            }
            check(actualComps.size() == spec.components.size(), "components count matches spec: " + spec.components.size() + " vs " + actualComps.size() + " in " + file);
            List<String> sortedActual = new ArrayList<>(actualComps);
            List<String> sortedExpected = new ArrayList<>(spec.components);
            Collections.sort(sortedActual);
            Collections.sort(sortedExpected);
            check(sortedActual.equals(sortedExpected), "components match spec in: " + file);
        }

        // 5. Aspects verification (infusion / crucible)
        if (!spec.aspects.isEmpty()) {
            check(json.has("aspects"), "recipe has aspects: " + file);
            JsonObject aspObj = json.getAsJsonObject("aspects");
            Map<String, Integer> actualAsp = new HashMap<>();
            for (var entry : aspObj.entrySet()) {
                actualAsp.put(entry.getKey(), entry.getValue().getAsInt());
            }
            check(spec.aspects.equals(actualAsp), "aspects match spec: " + spec.aspects + " vs " + actualAsp + " in " + file);
        }

        // 6. Vis verification (arcane)
        if (!spec.vis.isEmpty()) {
            check(json.has("vis") || json.has("aspects"), "arcane recipe has vis: " + file);
            JsonObject visObj = json.has("vis") ? json.getAsJsonObject("vis") : json.getAsJsonObject("aspects");
            Map<String, Integer> actualVis = new HashMap<>();
            for (var entry : visObj.entrySet()) {
                actualVis.put(entry.getKey(), entry.getValue().getAsInt());
            }
            check(spec.vis.equals(actualVis), "vis matches spec: " + spec.vis + " vs " + actualVis + " in " + file);
        }

        // 7. Pattern & Key verification (shaped)
        if (!spec.pattern.isEmpty()) {
            check(json.has("pattern"), "shaped recipe has pattern: " + file);
            List<String> actualPat = new ArrayList<>();
            for (var p : json.getAsJsonArray("pattern")) actualPat.add(p.getAsString());
            check(spec.pattern.equals(actualPat), "pattern matches spec in: " + file);

            check(json.has("key"), "shaped recipe has key map: " + file);
            Map<String, String> actualKey = new HashMap<>();
            for (var entry : json.getAsJsonObject("key").entrySet()) {
                String k = entry.getKey();
                var v = entry.getValue();
                String valStr = "";
                if (v.isJsonObject()) {
                    var vo = v.getAsJsonObject();
                    if (vo.has("item")) valStr = vo.get("item").getAsString();
                    else if (vo.has("tag")) valStr = "#" + vo.get("tag").getAsString();
                    else if (vo.has("id")) valStr = vo.get("id").getAsString();
                } else if (v.isJsonPrimitive()) {
                    valStr = v.getAsString();
                }
                actualKey.put(k, valStr);
            }
            check(spec.key.equals(actualKey), "key map matches spec: " + spec.key + " vs " + actualKey + " in " + file);
        }

        // 8. Ingredients verification (shapeless)
        if (!spec.ingredients.isEmpty()) {
            check(json.has("ingredients"), "shapeless recipe has ingredients: " + file);
            List<String> actualIngrs = new ArrayList<>();
            for (var elem : json.getAsJsonArray("ingredients")) {
                if (elem.isJsonObject()) {
                    var eo = elem.getAsJsonObject();
                    if (eo.has("item")) actualIngrs.add(eo.get("item").getAsString());
                    else if (eo.has("tag")) actualIngrs.add("#" + eo.get("tag").getAsString());
                    else if (eo.has("id")) actualIngrs.add(eo.get("id").getAsString());
                } else if (elem.isJsonPrimitive()) {
                    actualIngrs.add(elem.getAsString());
                }
            }
            List<String> sortedActual = new ArrayList<>(actualIngrs);
            List<String> sortedExpected = new ArrayList<>(spec.ingredients);
            Collections.sort(sortedActual);
            Collections.sort(sortedExpected);
            check(sortedActual.equals(sortedExpected), "ingredients match spec in: " + file);
        }

        // 9. Mana Infusion verification
        if (spec.mana > 0) {
            check(json.has("mana"), "mana infusion has mana: " + file);
            check(json.get("mana").getAsInt() == spec.mana, "mana matches spec: " + spec.mana + " vs " + json.get("mana").getAsInt() + " in " + file);
        }

        // 10. Blood Altar verification
        if (spec.altarSyphon > 0) {
            check(json.has("altarSyphon"), "altar recipe has altarSyphon: " + file);
            check(json.get("altarSyphon").getAsInt() == spec.altarSyphon, "altarSyphon matches spec in: " + file);
            check(json.has("consumptionRate"), "altar recipe has consumptionRate: " + file);
            check(json.get("consumptionRate").getAsInt() == spec.consumptionRate, "consumptionRate matches spec in: " + file);
            check(json.has("drainRate"), "altar recipe has drainRate: " + file);
            check(json.get("drainRate").getAsInt() == spec.drainRate, "drainRate matches spec in: " + file);
            check(json.has("upgradeLevel"), "altar recipe has upgradeLevel: " + file);
            check(json.get("upgradeLevel").getAsInt() == spec.upgradeLevel, "upgradeLevel matches spec in: " + file);
        }
    }

    private static void validateResearchJson(JsonObject json, String file) {
        check("research".equals(json.get("kind").getAsString()), "kind is research: " + file);
        check(json.has("key"), "has key: " + file);
        String key = json.get("key").getAsString();
        check(key.startsWith("forbidden:"), "key has forbidden namespace: " + key);
        check("forbidden:forbidden".equals(json.get("category").getAsString()), "category is forbidden:forbidden: " + file);

        ResearchSpec spec = RESEARCH_SPECS.get(file.equals("/packs/gluttony_hard/data/forbidden/thaumcraft/research/arcanecake.json")
                ? "/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json" : file);
        check(spec != null, "research spec exists for: " + file);
        check(spec.key.equals(key), "research key matches spec: " + spec.key + " in " + file);

        int col = json.get("column").getAsInt();
        int row = json.get("row").getAsInt();
        check(col == spec.col, "col matches: " + spec.col + " vs " + col + " in " + file);
        check(row == spec.row, "row matches: " + spec.row + " vs " + row + " in " + file);

        int comp = json.get("complexity").getAsInt();
        check(comp == spec.comp, "comp matches: " + spec.comp + " vs " + comp + " in " + file);

        int warp = json.has("warp") ? json.get("warp").getAsInt() : 0;
        check(warp == spec.warp, "warp matches: " + spec.warp + " vs " + warp + " in " + file);

        // Parents
        List<String> actualParents = new ArrayList<>();
        if (json.has("parents")) {
            for (var p : json.getAsJsonArray("parents")) actualParents.add(p.getAsString());
        }
        check(actualParents.equals(spec.parents), "parents match spec in: " + file);

        // Hidden Parents
        List<String> actualHiddenParents = new ArrayList<>();
        if (json.has("hidden_parents")) {
            for (var p : json.getAsJsonArray("hidden_parents")) actualHiddenParents.add(p.getAsString());
        }
        check(actualHiddenParents.equals(spec.hiddenParents), "hidden parents match spec in: " + file);

        // Recipe Pages
        List<String> actualRecipePages = new ArrayList<>();
        if (json.has("recipe_pages")) {
            for (var p : json.getAsJsonArray("recipe_pages")) actualRecipePages.add(p.getAsString());
        }
        var expectedConcretePages = new ArrayList<String>();
        var groups = json.has("recipe_groups") ? json.getAsJsonObject("recipe_groups") : new JsonObject();
        for (var legacyPage : spec.recipePages) {
            if (groups.has(legacyPage)) {
                var variants = groups.getAsJsonArray(legacyPage);
                check(!variants.isEmpty(), "legacy recipe group is nonempty: " + legacyPage);
                for (var variant : variants) {
                    check(variant.getAsString().contains(":"), "native recipe variant has namespace: " + variant);
                    expectedConcretePages.add(variant.getAsString());
                }
            } else expectedConcretePages.add(legacyPage);
        }
        check(actualRecipePages.equals(expectedConcretePages), "native recipe pages retain legacy groups and order in: " + file);
        if (key.equals("forbidden:arcanecake")) check(actualRecipePages.equals(List.of(
                file.contains("gluttony_hard/") ? "forbidden:arcane_cake_hardcore" : "forbidden:arcane_cake_normal")),
                "Arcane Cake displays only the recipe enabled by its startup pack");

        // Text Pages
        List<String> actualTextPages = new ArrayList<>();
        if (json.has("text_pages")) {
            for (var p : json.getAsJsonArray("text_pages")) actualTextPages.add(p.getAsString());
        }
        check(actualTextPages.equals(spec.textPages), "text pages match spec in: " + file);

        // Page Order
        List<String> actualPageOrder = new ArrayList<>();
        if (json.has("page_order")) {
            for (var p : json.getAsJsonArray("page_order")) actualPageOrder.add(p.getAsString());
        }
        check(actualPageOrder.equals(spec.pageOrder), "page_order matches spec in: " + file);
        if (spec.key.equals("forbidden:fork")) {
            check(json.has("page_requirements")
                    && json.getAsJsonObject("page_requirements").has("forbidden.research_page.FORK.wc")
                    && json.getAsJsonObject("page_requirements").get("forbidden.research_page.FORK.wc").getAsString().equals("forbidden:wrathcage"),
                    "legacy gated Fork page requires Wrath Cage research");
        }

        // Aspects
        Map<String, Integer> actualAspects = new HashMap<>();
        if (json.has("aspects")) {
            for (var entry : json.getAsJsonObject("aspects").entrySet()) {
                actualAspects.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        check(spec.aspects.equals(actualAspects), "research aspects match spec in: " + file);

        // Flags
        boolean actualAuto = json.has("auto_unlock") && json.get("auto_unlock").getAsBoolean();
        boolean actualStub = json.has("stub") && json.get("stub").getAsBoolean();
        boolean actualConcealed = json.has("concealed") && json.get("concealed").getAsBoolean();
        boolean actualSecondary = json.has("secondary") && json.get("secondary").getAsBoolean();
        boolean actualRound = json.has("round") && json.get("round").getAsBoolean();
        boolean actualHidden = json.has("hidden") && json.get("hidden").getAsBoolean();
        check(actualAuto == spec.autoUnlock, "auto_unlock matches spec in: " + file);
        check(actualStub == spec.stub, "stub matches spec in: " + file);
        check(actualConcealed == spec.concealed, "concealed matches spec in: " + file);
        check(actualSecondary == spec.secondary, "secondary matches spec in: " + file);
        check(actualRound == spec.round, "round matches spec in: " + file);
        check(actualHidden == spec.hidden, "hidden matches spec in: " + file);

        // Siblings
        List<String> actualSiblings = new ArrayList<>();
        if (json.has("siblings")) {
            for (var p : json.getAsJsonArray("siblings")) actualSiblings.add(p.getAsString());
        }
        check(actualSiblings.equals(spec.siblings), "siblings match spec in: " + file);
    }

    private static void testV03Research() throws Exception {
        check(RESEARCH_FILES.size() == 50, "49 research definitions and hard Cake override indexed");

        Set<String> uniqueKeys = new HashSet<>();
        Set<String> uniqueCoordinates = new HashSet<>();

        for (String file : RESEARCH_FILES) {
            JsonObject json = getJson(file);
            if (file.contains("gluttony_hard/")) {
                validateResearchJson(json, file);
                check(json.get("key").equals(getJson("/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json").get("key")),
                        "hard Cake overrides the same research key");
                continue;
            }
            String key = json.get("key").getAsString();
            check(uniqueKeys.add(key), "key is globally unique: " + key);

            int col = json.get("column").getAsInt();
            int row = json.get("row").getAsInt();
            String coord = col + "," + row;
            check(uniqueCoordinates.add(coord), "coordinate collision free: " + coord + " (" + key + ")");

            validateResearchJson(json, file);
        }

        check(uniqueKeys.size() == 49, "exactly 49 unique research entries verified");
        check(uniqueCoordinates.size() == 49, "exactly 49 unique coordinate slots occupied");
    }

    private static void testNegativeRecipeRegressions() {
        String target = "/data/forbidden/recipe/wrath_cage.json";

        // Mutation 1: Alter recipe research key
        JsonObject m1 = getJson(target);
        m1.addProperty("research", "forbidden:invalid_research");
        assertThrows(() -> validateRecipeJson(m1, target), "altered recipe research key");

        // Mutation 2: Alter recipe instability
        JsonObject m2 = getJson(target);
        m2.addProperty("instability", 0);
        assertThrows(() -> validateRecipeJson(m2, target), "altered recipe instability");

        // Mutation 3: Alter recipe result
        JsonObject m3 = getJson(target);
        JsonObject fakeRes = new JsonObject();
        fakeRes.addProperty("id", "minecraft:dirt");
        m3.add("result", fakeRes);
        assertThrows(() -> validateRecipeJson(m3, target), "altered recipe result");

        // Mutation 4: Alter result count
        JsonObject m4 = getJson("/data/forbidden/recipe/emerald_nuggets.json");
        JsonObject badCountRes = m4.getAsJsonObject("result").deepCopy();
        badCountRes.addProperty("count", 1);
        m4.add("result", badCountRes);
        assertThrows(() -> validateRecipeJson(m4, "/data/forbidden/recipe/emerald_nuggets.json"), "altered result count");

        // Mutation 5: Alter central input
        JsonObject m5 = getJson(target);
        JsonObject fakeCentral = new JsonObject();
        fakeCentral.addProperty("item", "minecraft:dirt");
        m5.add("input", fakeCentral);
        assertThrows(() -> validateRecipeJson(m5, target), "altered central input");

        // Mutation 6: Alter components
        JsonObject m6 = getJson(target);
        JsonArray badComps = m6.getAsJsonArray("components").deepCopy();
        badComps.remove(0);
        m6.add("components", badComps);
        assertThrows(() -> validateRecipeJson(m6, target), "altered components count");

        // Mutation 7: Alter aspect amounts
        JsonObject m7 = getJson(target);
        JsonObject badAsp = m7.getAsJsonObject("aspects").deepCopy();
        badAsp.addProperty("forbidden:ira", 999);
        m7.add("aspects", badAsp);
        assertThrows(() -> validateRecipeJson(m7, target), "altered aspect amount");

        // Mutation 8: Alter vis cost
        JsonObject m8 = getJson("/data/forbidden/recipe/taint_stone.json");
        JsonObject badVis = (m8.has("vis") ? m8.getAsJsonObject("vis") : m8.getAsJsonObject("aspects")).deepCopy();
        badVis.addProperty("ordo", 999);
        if (m8.has("vis")) m8.add("vis", badVis); else m8.add("aspects", badVis);
        assertThrows(() -> validateRecipeJson(m8, "/data/forbidden/recipe/taint_stone.json"), "altered vis cost");

        // S04 Mutation 9: Alter pattern on shaped recipe
        JsonObject m9 = getJson("/data/forbidden/recipe/emerald.json");
        JsonArray badPattern = new JsonArray();
        badPattern.add("###");
        m9.add("pattern", badPattern);
        assertThrows(() -> validateRecipeJson(m9, "/data/forbidden/recipe/emerald.json"), "altered pattern");

        // S04 Mutation 10: Alter key mapping on shaped recipe
        JsonObject m10 = getJson("/data/forbidden/recipe/emerald.json");
        JsonObject badKey = new JsonObject();
        JsonObject badKeyVal = new JsonObject();
        badKeyVal.addProperty("item", "minecraft:dirt");
        badKey.add("#", badKeyVal);
        m10.add("key", badKey);
        assertThrows(() -> validateRecipeJson(m10, "/data/forbidden/recipe/emerald.json"), "altered key");

        // S04 Mutation 11: Alter ingredients on shapeless recipe
        JsonObject m11 = getJson("/data/forbidden/recipe/crystalwell.json");
        JsonArray badIngrs = m11.getAsJsonArray("ingredients").deepCopy();
        badIngrs.remove(0);
        m11.add("ingredients", badIngrs);
        assertThrows(() -> validateRecipeJson(m11, "/data/forbidden/recipe/crystalwell.json"), "altered ingredients");

        // S04 Mutation 12: Alter mana on botania mana infusion
        JsonObject m12 = getJson("/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json");
        m12.addProperty("mana", 9999);
        assertThrows(() -> validateRecipeJson(m12, "/packs/botania_wands/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json"), "altered mana");

        // S04 Mutation 13: Alter altarSyphon on blood magic altar
        JsonObject m13 = getJson("/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json");
        m13.addProperty("altarSyphon", 14999);
        assertThrows(() -> validateRecipeJson(m13, "/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json"), "altered altarSyphon");

        // S04 Mutation 14: Alter upgradeLevel on blood magic altar
        JsonObject m14 = getJson("/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json");
        m14.addProperty("upgradeLevel", 2);
        assertThrows(() -> validateRecipeJson(m14, "/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json"), "altered upgradeLevel");
    }

    private static void testNegativeResearchRegressions() {
        String target = "/packs/cage/data/forbidden/thaumcraft/research/wrathcage.json";

        // Mutation 1: Mutate coordinates
        JsonObject m1 = getJson(target);
        m1.addProperty("column", 0);
        assertThrows(() -> validateResearchJson(m1, target), "mutated column coordinate");

        // Mutation 2: Mutate parents
        JsonObject m2 = getJson(target);
        m2.add("parents", new JsonArray());
        assertThrows(() -> validateResearchJson(m2, target), "mutated parents");

        // Mutation 3: Mutate hidden parents
        JsonObject m3 = getJson(target);
        m3.add("hidden_parents", new JsonArray());
        assertThrows(() -> validateResearchJson(m3, target), "mutated hidden parents");

        // Mutation 4: Mutate complexity
        JsonObject m4 = getJson(target);
        m4.addProperty("complexity", 1);
        assertThrows(() -> validateResearchJson(m4, target), "mutated complexity");

        // Mutation 5: Mutate warp
        JsonObject m5 = getJson(target);
        m5.addProperty("warp", 0);
        assertThrows(() -> validateResearchJson(m5, target), "mutated warp");

        // Mutation 6: Mutate recipe pages
        JsonObject m6 = getJson(target);
        m6.add("recipe_pages", new JsonArray());
        assertThrows(() -> validateResearchJson(m6, target), "mutated recipe pages");

        // Mutation 7: Mutate research aspects
        JsonObject m7 = getJson(target);
        JsonObject badAsp = m7.getAsJsonObject("aspects").deepCopy();
        badAsp.addProperty("ira", 999);
        m7.add("aspects", badAsp);
        assertThrows(() -> validateResearchJson(m7, target), "mutated research aspects");

        // Mutation 8: Missing namespace
        JsonObject m8 = getJson(target);
        m8.addProperty("key", "unnamespaced_key");
        assertThrows(() -> validateResearchJson(m8, target), "unnamespaced key");

        // S05 Mutation 9: Mutate text_pages contents
        JsonObject m9 = getJson(target);
        JsonArray badTextPages = new JsonArray();
        badTextPages.add("forbidden.research_page.WRATHCAGE.FAKE");
        m9.add("text_pages", badTextPages);
        assertThrows(() -> validateResearchJson(m9, target), "mutated text pages");

        // S05 Mutation 10: Mutate page_order sequence
        JsonObject m10 = getJson(target);
        JsonArray badPageOrder = new JsonArray();
        badPageOrder.add("WrathCage");
        badPageOrder.add("forbidden.research_page.WRATHCAGE.1");
        m10.add("page_order", badPageOrder);
        assertThrows(() -> validateResearchJson(m10, target), "mutated page order");

        // S05 Mutation 11: Mutate concealed flag
        JsonObject m11 = getJson("/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json");
        m11.addProperty("concealed", false);
        assertThrows(() -> validateResearchJson(m11, "/packs/gluttony/data/forbidden/thaumcraft/research/arcanecake.json"), "mutated concealed flag");

        // S05 Mutation 12: Mutate siblings
        JsonObject m12 = getJson(target);
        JsonArray badSiblings = new JsonArray();
        badSiblings.add("forbidden:fake_sibling");
        m12.add("siblings", badSiblings);
        assertThrows(() -> validateResearchJson(m12, target), "mutated siblings");
    }

    private static void assertThrows(Runnable runnable, String description) {
        boolean caught = false;
        try {
            runnable.run();
        } catch (AssertionError e) {
            caught = true;
        }
        check(caught, "mutation correctly detected and rejected: " + description);
    }

    private SemanticDataCheck() {}
}
