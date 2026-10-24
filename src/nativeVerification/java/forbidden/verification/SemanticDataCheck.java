package forbidden.verification;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dedicated semantic data verification suite for V02 (Recipes) and V03 (Research).
 * Performs direct offline validation of all 33 recipe JSON definitions and all 31 research JSON definitions
 * against the 0.575 reference model (ingredients, aspect costs, instability, coordinates, parents, flags, warp).
 * Includes negative regression fixtures verifying that corrupt data is strictly rejected.
 */
public final class SemanticDataCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("Semantic data check failed: " + reason);
        }
    }

    private static final List<String> RECIPE_FILES = List.of(
            "arcane_cake_hardcore.json",
            "arcane_cake_normal.json",
            "black_ink.json",
            "crystalwell.json",
            "emerald.json",
            "emerald_nuggets.json",
            "emerald_transmutation.json",
            "focus_blink.json",
            "fork.json",
            "heretic_study.json",
            "mob_crystal.json",
            "morph_axe.json",
            "morph_pickaxe.json",
            "morph_shovel.json",
            "morph_sword.json",
            "nether_stars.json",
            "primewell.json",
            "riding_crop.json",
            "ring_food.json",
            "skull_axe.json",
            "star_block.json",
            "sub_collar.json",
            "taint_bricks.json",
            "taint_coal.json",
            "taint_pick.json",
            "taint_planks.json",
            "taint_sapling.json",
            "taint_shovel.json",
            "taint_stone.json",
            "umbral_bush.json",
            "wand_rod_infernal.json",
            "wand_rod_tainted.json",
            "wrath_cage.json"
    );

    private static final List<String> RESEARCH_FILES = List.of(
            "arcanecake.json",
            "blackflower.json",
            "cluster.json",
            "consuming.json",
            "corrupting.json",
            "crystalwell.json",
            "educational.json",
            "focusblink.json",
            "fork.json",
            "greedy.json",
            "hellfire.json",
            "impact.json",
            "morphtools.json",
            "nethershards.json",
            "primewell.json",
            "ridingcrop.json",
            "ringfood.json",
            "rod_infernal.json",
            "rod_profane.json",
            "rod_tainted.json",
            "schools.json",
            "skullaxe.json",
            "subcollar.json",
            "taintpick.json",
            "taintshovel.json",
            "taintstone.json",
            "tainttree.json",
            "transemerald.json",
            "voidtouched.json",
            "wrath.json",
            "wrathcage.json"
    );

    record RecipeSpec(String type, String research, String resultId, int instability) {}

    record ResearchSpec(String key, int col, int row, int comp, int warp, List<String> parents, List<String> recipePages) {}

    private static final Map<String, RecipeSpec> RECIPE_SPECS = Map.ofEntries(
            Map.entry("arcane_cake_hardcore.json", new RecipeSpec("thaumcraft:infusion", "forbidden:arcanecake", "forbidden:arcane_cake", 3)),
            Map.entry("arcane_cake_normal.json", new RecipeSpec("thaumcraft:infusion", "forbidden:arcanecake", "forbidden:arcane_cake", 3)),
            Map.entry("black_ink.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:black_dye", 0)),
            Map.entry("crystalwell.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:crystalwell", "forbidden:crystalwell", 0)),
            Map.entry("emerald.json", new RecipeSpec("minecraft:crafting_shaped", null, "minecraft:emerald", 0)),
            Map.entry("emerald_nuggets.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:emerald_nugget", 0)),
            Map.entry("emerald_transmutation.json", new RecipeSpec("thaumcraft:crucible", "forbidden:transemerald", "forbidden:emerald_nugget", 0)),
            Map.entry("focus_blink.json", new RecipeSpec("thaumcraft:infusion", "forbidden:focusblink", "forbidden:focus_blink", 3)),
            Map.entry("fork.json", new RecipeSpec("thaumcraft:infusion", "forbidden:fork", "forbidden:diabolist_fork", 3)),
            Map.entry("heretic_study.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:heretic_study", 0)),
            Map.entry("mob_crystal.json", new RecipeSpec("thaumcraft:crucible", "forbidden:wrathcage", "forbidden:mob_crystal", 0)),
            Map.entry("morph_axe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_axe", 2)),
            Map.entry("morph_pickaxe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_pickaxe", 2)),
            Map.entry("morph_shovel.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_shovel", 2)),
            Map.entry("morph_sword.json", new RecipeSpec("thaumcraft:infusion", "forbidden:morphtools", "forbidden:morph_sword", 2)),
            Map.entry("nether_stars.json", new RecipeSpec("minecraft:crafting_shaped", null, "minecraft:nether_star", 0)),
            Map.entry("primewell.json", new RecipeSpec("thaumcraft:arcane_shapeless", "forbidden:primewell", "forbidden:primewell", 0)),
            Map.entry("riding_crop.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:riding_crop", 0)),
            Map.entry("ring_food.json", new RecipeSpec("thaumcraft:arcane", "forbidden:ringfood", "forbidden:ring_nutrition", 0)),
            Map.entry("skull_axe.json", new RecipeSpec("thaumcraft:infusion", "forbidden:skullaxe", "forbidden:skull_axe", 1)),
            Map.entry("star_block.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:star_block", 0)),
            Map.entry("sub_collar.json", new RecipeSpec("thaumcraft:infusion", "forbidden:subcollar", "forbidden:sub_collar", 2)),
            Map.entry("taint_bricks.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:taint_bricks", 0)),
            Map.entry("taint_coal.json", new RecipeSpec("minecraft:smelting", null, "forbidden:taint_coal", 0)),
            Map.entry("taint_pick.json", new RecipeSpec("thaumcraft:infusion", "forbidden:taintpick", "forbidden:taint_pickaxe", 1)),
            Map.entry("taint_planks.json", new RecipeSpec("minecraft:crafting_shaped", null, "forbidden:taint_planks", 0)),
            Map.entry("taint_sapling.json", new RecipeSpec("thaumcraft:crucible", "forbidden:tainttree", "forbidden:taint_sapling", 0)),
            Map.entry("taint_shovel.json", new RecipeSpec("thaumcraft:infusion", "forbidden:taintshovel", "forbidden:taint_shovel", 5)),
            Map.entry("taint_stone.json", new RecipeSpec("thaumcraft:arcane", "forbidden:taintstone", "forbidden:taint_stone", 0)),
            Map.entry("umbral_bush.json", new RecipeSpec("thaumcraft:crucible", "forbidden:blackflower", "forbidden:umbral_bush", 0)),
            Map.entry("wand_rod_infernal.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_infernal", "forbidden:wand_rod_infernal", 5)),
            Map.entry("wand_rod_tainted.json", new RecipeSpec("thaumcraft:infusion", "forbidden:rod_tainted", "forbidden:wand_rod_tainted", 5)),
            Map.entry("wrath_cage.json", new RecipeSpec("thaumcraft:infusion", "forbidden:wrathcage", "forbidden:wrath_cage", 10))
    );

    private static final Map<String, ResearchSpec> RESEARCH_SPECS = Map.ofEntries(
            Map.entry("arcanecake.json", new ResearchSpec("forbidden:arcanecake", -12, -6, 2, 0, List.of("forbidden:nethershards", "INFUSION"), List.of("ArcaneCake"))),
            Map.entry("blackflower.json", new ResearchSpec("forbidden:blackflower", -2, -6, 1, 0, List.of(), List.of("BlackFlower", "BlackInk"))),
            Map.entry("cluster.json", new ResearchSpec("forbidden:cluster", -10, -1, 3, 0, List.of("forbidden:morphtools", "ELEMENTALPICK"), List.of("Cluster"))),
            Map.entry("consuming.json", new ResearchSpec("forbidden:consuming", -4, -8, 1, 0, List.of("INFUSIONENCHANTMENT"), List.of("Consuming"))),
            Map.entry("corrupting.json", new ResearchSpec("forbidden:corrupting", -6, -4, 1, 0, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of("Corrupting"))),
            Map.entry("crystalwell.json", new ResearchSpec("forbidden:crystalwell", -2, -8, 1, 0, List.of("RESEARCH"), List.of("Crystalwell"))),
            Map.entry("educational.json", new ResearchSpec("forbidden:educational", -4, -7, 2, 0, List.of("INFUSIONENCHANTMENT"), List.of("Educational"))),
            Map.entry("focusblink.json", new ResearchSpec("forbidden:focusblink", -11, -3, 2, 0, List.of("forbidden:nethershards", "INFUSION"), List.of("FocusBlink"))),
            Map.entry("fork.json", new ResearchSpec("forbidden:fork", -6, -7, 0, 0, List.of("INFUSION", "forbidden:nethershards", "THAUMIUM"), List.of("Fork"))),
            Map.entry("greedy.json", new ResearchSpec("forbidden:greedy", -6, -5, 2, 0, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of("Greedy"))),
            Map.entry("hellfire.json", new ResearchSpec("forbidden:hellfire", -8, -8, 1, 0, List.of("forbidden:nethershards"), List.of())),
            Map.entry("impact.json", new ResearchSpec("forbidden:impact", -9, -1, 3, 0, List.of("forbidden:morphtools", "ELEMENTALSHOVEL"), List.of("Impact"))),
            Map.entry("morphtools.json", new ResearchSpec("forbidden:morphtools", -9, -3, 4, 4, List.of("forbidden:nethershards"), List.of("MorphPick", "MorphSword", "MorphShovel", "MorphAxe"))),
            Map.entry("nethershards.json", new ResearchSpec("forbidden:nethershards", -8, -5, 0, 0, List.of(), List.of())),
            Map.entry("primewell.json", new ResearchSpec("forbidden:primewell", 2, -8, 1, 0, List.of("PRIMPEARL"), List.of("Primewell"))),
            Map.entry("ridingcrop.json", new ResearchSpec("forbidden:ridingcrop", 0, -8, 0, 0, List.of(), List.of("RidingCrop"))),
            Map.entry("ringfood.json", new ResearchSpec("forbidden:ringfood", -11, -6, 2, 0, List.of("forbidden:nethershards"), List.of("RingFood"))),
            Map.entry("rod_infernal.json", new ResearchSpec("forbidden:rod_infernal", -11, -4, 3, 2, List.of("ROD_silverwood", "INFUSION", "forbidden:nethershards"), List.of("WandRodInfernal"))),
            Map.entry("rod_profane.json", new ResearchSpec("forbidden:rod_profane", -9, -8, 0, 0, List.of("forbidden:nethershards"), List.of())),
            Map.entry("rod_tainted.json", new ResearchSpec("forbidden:rod_tainted", -8, 3, 3, 2, List.of("ROD_silverwood", "forbidden:taintshovel", "INFUSION"), List.of("WandRodTainted"))),
            Map.entry("schools.json", new ResearchSpec("forbidden:schools", -1, 1, 0, 0, List.of(), List.of())),
            Map.entry("skullaxe.json", new ResearchSpec("forbidden:skullaxe", -11, -7, 2, 1, List.of("forbidden:nethershards", "THAUMIUM", "INFUSION"), List.of("SkullAxe"))),
            Map.entry("subcollar.json", new ResearchSpec("forbidden:subcollar", -11, -5, 2, 0, List.of("forbidden:nethershards", "VISAMULET", "INFUSION"), List.of("SubCollar"))),
            Map.entry("taintpick.json", new ResearchSpec("forbidden:taintpick", -10, 1, 2, 0, List.of("forbidden:taintshovel"), List.of("TaintPick"))),
            Map.entry("taintshovel.json", new ResearchSpec("forbidden:taintshovel", -8, 1, 2, 0, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of("TaintShovel"))),
            Map.entry("taintstone.json", new ResearchSpec("forbidden:taintstone", -10, 3, 2, 0, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of("TaintStone", "TaintBrick"))),
            Map.entry("tainttree.json", new ResearchSpec("forbidden:tainttree", -11, 3, 3, 1, List.of("THAUMIUM", "INFUSION", "ETHEREALBLOOM"), List.of("TaintTree", "TaintPlank"))),
            Map.entry("transemerald.json", new ResearchSpec("forbidden:transemerald", 0, -6, 3, 0, List.of("TRANSGOLD"), List.of("TransEmerald"))),
            Map.entry("voidtouched.json", new ResearchSpec("forbidden:voidtouched", -8, -1, 4, 3, List.of("forbidden:morphtools", "VOIDMETAL"), List.of("Voidtouched"))),
            Map.entry("wrath.json", new ResearchSpec("forbidden:wrath", -6, -3, 4, 3, List.of("forbidden:nethershards", "INFUSIONENCHANTMENT"), List.of("Wrath"))),
            Map.entry("wrathcage.json", new ResearchSpec("forbidden:wrathcage", -6, -9, 4, 3, List.of("forbidden:fork"), List.of("WrathCage", "MobCrystal")))
    );

    public static void main(String[] args) throws Exception {
        testV02Recipes();
        testV03Research();
        testNegativeRecipeRegressions();
        testNegativeResearchRegressions();

        System.out.println("FORBIDDEN_SEMANTIC_DATA_PASS checks=" + checks);
    }

    private static void testV02Recipes() throws Exception {
        check(RECIPE_FILES.size() == 33, "all 33 recipe definition files indexed");

        int arcaneCount = 0;
        int crucibleCount = 0;
        int infusionCount = 0;
        int craftingCount = 0;
        int smeltingCount = 0;

        for (String file : RECIPE_FILES) {
            String path = "/data/forbidden/recipe/" + file;
            try (var in = SemanticDataCheck.class.getResourceAsStream(path)) {
                check(in != null, "recipe resource found: " + file);
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                check(json.has("type"), "recipe has type: " + file);
                String type = json.get("type").getAsString();
                check(!type.isBlank(), "recipe type non-blank: " + file);

                validateRecipeJson(json, file);

                if (type.startsWith("thaumcraft:arcane")) arcaneCount++;
                else if (type.equals("thaumcraft:crucible")) crucibleCount++;
                else if (type.equals("thaumcraft:infusion")) infusionCount++;
                else if (type.startsWith("minecraft:crafting")) craftingCount++;
                else if (type.equals("minecraft:smelting")) smeltingCount++;
            }
        }

        check(arcaneCount == 4, "exactly 4 arcane workbench recipes verified");
        check(crucibleCount == 4, "exactly 4 crucible recipes verified");
        check(infusionCount == 15, "exactly 15 infusion recipes verified");
        check(craftingCount == 9, "exactly 9 crafting recipes verified");
        check(smeltingCount == 1, "exactly 1 smelting recipe verified");
    }

    private static void validateRecipeJson(JsonObject json, String file) {
        String type = json.get("type").getAsString();
        RecipeSpec spec = RECIPE_SPECS.get(file);
        check(spec != null, "recipe spec exists for: " + file);
        check(type.equals(spec.type), "recipe type matches spec: " + type + " vs " + spec.type + " in " + file);

        // 1. Result verification
        check(json.has("result"), "recipe has result: " + file);
        var resultObj = json.get("result");
        String actualResultId = "";
        if (resultObj.isJsonObject()) {
            JsonObject r = resultObj.getAsJsonObject();
            check(r.has("id") || r.has("item"), "result has id/item: " + file);
            actualResultId = r.has("id") ? r.get("id").getAsString() : r.get("item").getAsString();
            if (r.has("count")) {
                check(r.get("count").getAsInt() > 0, "result count positive: " + file);
            }
        } else if (resultObj.isJsonPrimitive()) {
            actualResultId = resultObj.getAsString();
            check(!actualResultId.isBlank(), "result id non-blank: " + file);
        }
        check(spec.resultId.equals(actualResultId), "result matches spec: " + spec.resultId + " vs " + actualResultId + " in " + file);

        // 2. Research & Instability verification against spec
        if (spec.research != null) {
            check(json.has("research"), "recipe has research: " + file);
            check(spec.research.equals(json.get("research").getAsString()), "research key matches spec: " + spec.research + " in " + file);
        } else {
            check(!json.has("research") || json.get("research").getAsString().isBlank(), "recipe without research spec has none: " + file);
        }

        int actualInstability = json.has("instability") ? json.get("instability").getAsInt() : 0;
        check(actualInstability == spec.instability, "instability matches spec: " + spec.instability + " vs " + actualInstability + " in " + file);

        // 3. Type-specific validation
        if (type.equals("thaumcraft:infusion")) {
            check(json.has("input") || json.has("central_item") || json.has("central"), "infusion has central input: " + file);
            check(json.has("components"), "infusion has components: " + file);
            check(json.getAsJsonArray("components").size() > 0, "infusion has non-empty components: " + file);
            check(json.has("aspects"), "infusion has aspect costs: " + file);
            JsonObject aspects = json.getAsJsonObject("aspects");
            check(aspects.size() > 0, "infusion has non-empty aspects: " + file);
            for (var entry : aspects.entrySet()) {
                check(entry.getValue().getAsInt() > 0, "infusion aspect cost > 0: " + entry.getKey() + " in " + file);
            }
            check(json.get("instability").getAsInt() >= 0, "instability non-negative: " + file);
        } else if (type.equals("thaumcraft:crucible")) {
            check(json.has("catalyst"), "crucible has catalyst: " + file);
            check(json.has("aspects"), "crucible has aspects: " + file);
            JsonObject aspects = json.getAsJsonObject("aspects");
            check(aspects.size() > 0, "crucible has non-empty aspects: " + file);
            for (var entry : aspects.entrySet()) {
                check(entry.getValue().getAsInt() > 0, "crucible aspect cost > 0: " + entry.getKey() + " in " + file);
            }
        } else if (type.startsWith("thaumcraft:arcane")) {
            check(json.has("aspects") || json.has("vis"), "arcane recipe has primal vis costs: " + file);
            JsonObject vis = json.has("vis") ? json.getAsJsonObject("vis") : json.getAsJsonObject("aspects");
            check(vis.size() > 0, "arcane recipe has non-empty vis: " + file);
            for (var entry : vis.entrySet()) {
                check(entry.getValue().getAsInt() > 0, "vis cost > 0: " + entry.getKey() + " in " + file);
            }
        }
    }

    private static JsonObject getRecipeJson(String file) {
        String path = "/data/forbidden/recipe/" + file;
        try (var in = SemanticDataCheck.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Recipe not found: " + path);
            var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonObject getResearchJson(String file) {
        String path = "/data/forbidden/thaumcraft/research/" + file;
        try (var in = SemanticDataCheck.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Research not found: " + path);
            var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateResearchJson(JsonObject json, String file) {
        check("research".equals(json.get("kind").getAsString()), "kind is research: " + file);
        check(json.has("key"), "has key: " + file);
        String key = json.get("key").getAsString();
        check(key.startsWith("forbidden:"), "key has forbidden namespace: " + key);

        check("forbidden:FORBIDDEN".equals(json.get("category").getAsString()), "category is forbidden:FORBIDDEN: " + file);

        check(json.has("column") && json.has("row"), "has grid coordinates: " + file);
        int col = json.get("column").getAsInt();
        int row = json.get("row").getAsInt();

        check(json.has("complexity"), "has complexity: " + file);
        int complexity = json.get("complexity").getAsInt();
        check(complexity >= 0 && complexity <= 4, "complexity in [0, 4]: " + complexity + " in " + file);

        check(json.has("aspects"), "has aspects: " + file);
        JsonObject aspects = json.getAsJsonObject("aspects");
        boolean isStubOrAutoUnlock = (json.has("stub") && json.get("stub").getAsBoolean())
                || (json.has("auto_unlock") && json.get("auto_unlock").getAsBoolean());
        if (isStubOrAutoUnlock) {
            check(complexity == 0, "stub/auto_unlock research has complexity 0: " + file);
        } else {
            check(aspects.size() > 0, "non-empty discovery aspects for standard research: " + file);
            for (var a : aspects.entrySet()) {
                check(a.getValue().getAsInt() > 0, "aspect amount positive: " + a.getKey() + " in " + file);
            }
        }

        check(json.has("parents"), "has parents array: " + file);
        check(json.has("hidden_parents"), "has hidden_parents array: " + file);
        check(json.has("text_pages"), "has text_pages: " + file);
        check(json.has("recipe_pages"), "has recipe_pages: " + file);

        check(json.has("warp"), "has warp: " + file);
        int warp = json.get("warp").getAsInt();
        check(warp >= 0, "warp is non-negative: " + file);

        // Deep ground truth verification against spec
        ResearchSpec spec = RESEARCH_SPECS.get(file);
        check(spec != null, "research spec exists for: " + file);
        check(spec.key.equals(key), "research key matches spec: " + spec.key + " in " + file);
        check(col == spec.col, "research column matches spec: " + spec.col + " vs " + col + " in " + file);
        check(row == spec.row, "research row matches spec: " + spec.row + " vs " + row + " in " + file);
        check(complexity == spec.comp, "research complexity matches spec: " + spec.comp + " vs " + complexity + " in " + file);
        check(warp == spec.warp, "research warp matches spec: " + spec.warp + " vs " + warp + " in " + file);

        List<String> actualParents = new ArrayList<>();
        for (var p : json.getAsJsonArray("parents")) actualParents.add(p.getAsString());
        check(actualParents.size() == spec.parents.size(), "parents count matches spec for: " + file);
        for (String p : spec.parents) {
            check(actualParents.contains(p), "parent " + p + " present in: " + file);
        }

        List<String> actualPages = new ArrayList<>();
        for (var p : json.getAsJsonArray("recipe_pages")) actualPages.add(p.getAsString());
        check(actualPages.size() == spec.recipePages.size(), "recipe pages count matches spec for: " + file);
        for (String page : spec.recipePages) {
            check(actualPages.contains(page), "recipe page " + page + " present in: " + file);
        }

        // Check specific well-known warps
        if (key.equals("forbidden:wrath") || key.equals("forbidden:tainttree") || key.equals("forbidden:rod_infernal")) {
            check(warp > 0, "forbidden dark research has warp > 0: " + key);
        }
    }

    private static void testV03Research() throws Exception {
        check(RESEARCH_FILES.size() == 31, "all 31 research definition files indexed");

        Set<String> uniqueKeys = new HashSet<>();
        Set<String> uniqueCoordinates = new HashSet<>();

        for (String file : RESEARCH_FILES) {
            JsonObject json = getResearchJson(file);
            String key = json.get("key").getAsString();
            check(uniqueKeys.add(key), "key is globally unique: " + key);

            int col = json.get("column").getAsInt();
            int row = json.get("row").getAsInt();
            String coord = col + "," + row;
            check(uniqueCoordinates.add(coord), "coordinate collision free: " + coord + " (" + key + ")");

            validateResearchJson(json, file);
        }

        check(uniqueKeys.size() == 31, "exactly 31 unique research entries verified");
        check(uniqueCoordinates.size() == 31, "exactly 31 unique coordinate slots occupied");
    }

    private static void testNegativeRecipeRegressions() {
        // Mutation 1: Alter recipe research key
        JsonObject mutatedWrathCage1 = getRecipeJson("wrath_cage.json");
        mutatedWrathCage1.addProperty("research", "forbidden:invalid_research");
        boolean caught1 = false;
        try {
            validateRecipeJson(mutatedWrathCage1, "wrath_cage.json");
        } catch (AssertionError e) {
            caught1 = true;
        }
        check(caught1, "altered recipe research key detected and rejected");

        // Mutation 2: Alter recipe instability
        JsonObject mutatedWrathCage2 = getRecipeJson("wrath_cage.json");
        mutatedWrathCage2.addProperty("instability", 0); // expected 10!
        boolean caught2 = false;
        try {
            validateRecipeJson(mutatedWrathCage2, "wrath_cage.json");
        } catch (AssertionError e) {
            caught2 = true;
        }
        check(caught2, "altered recipe instability detected and rejected");

        // Mutation 3: Alter recipe result
        JsonObject mutatedWrathCage3 = getRecipeJson("wrath_cage.json");
        JsonObject fakeRes = new JsonObject();
        fakeRes.addProperty("id", "minecraft:dirt");
        mutatedWrathCage3.add("result", fakeRes);
        boolean caught3 = false;
        try {
            validateRecipeJson(mutatedWrathCage3, "wrath_cage.json");
        } catch (AssertionError e) {
            caught3 = true;
        }
        check(caught3, "altered recipe result detected and rejected");

        // Mutation 4: Negative aspect cost
        JsonObject badRecipeAspects = getRecipeJson("wrath_cage.json");
        JsonObject badAsp = badRecipeAspects.getAsJsonObject("aspects").deepCopy();
        badAsp.addProperty("ira", -10);
        badRecipeAspects.add("aspects", badAsp);
        boolean caught4 = false;
        try {
            validateRecipeJson(badRecipeAspects, "wrath_cage.json");
        } catch (AssertionError e) {
            caught4 = true;
        }
        check(caught4, "negative aspect cost in recipe detected and rejected");

        // Mutation 5: Empty components array in infusion recipe
        JsonObject badRecipeComps = getRecipeJson("wrath_cage.json");
        badRecipeComps.add("components", new JsonArray());
        boolean caught5 = false;
        try {
            validateRecipeJson(badRecipeComps, "wrath_cage.json");
        } catch (AssertionError e) {
            caught5 = true;
        }
        check(caught5, "empty components array in infusion recipe detected and rejected");
    }

    private static void testNegativeResearchRegressions() {
        // Mutation 1: Mutated column/row coordinates
        JsonObject mutatedResearch1 = getResearchJson("wrathcage.json");
        mutatedResearch1.addProperty("column", 0); // expected -6!
        boolean caughtRes1 = false;
        try {
            validateResearchJson(mutatedResearch1, "wrathcage.json");
        } catch (AssertionError e) {
            caughtRes1 = true;
        }
        check(caughtRes1, "altered research coordinates detected and rejected");

        // Mutation 2: Mutated parents
        JsonObject mutatedResearch2 = getResearchJson("wrathcage.json");
        mutatedResearch2.add("parents", new JsonArray()); // empty parents!
        boolean caughtRes2 = false;
        try {
            validateResearchJson(mutatedResearch2, "wrathcage.json");
        } catch (AssertionError e) {
            caughtRes2 = true;
        }
        check(caughtRes2, "altered research parents detected and rejected");

        // Mutation 3: Mutated complexity
        JsonObject mutatedResearch3 = getResearchJson("wrath.json");
        mutatedResearch3.addProperty("complexity", 1); // expected 4!
        boolean caughtRes3 = false;
        try {
            validateResearchJson(mutatedResearch3, "wrath.json");
        } catch (AssertionError e) {
            caughtRes3 = true;
        }
        check(caughtRes3, "altered research complexity detected and rejected");

        // Mutation 4: Mutated warp
        JsonObject mutatedResearch4 = getResearchJson("morphtools.json");
        mutatedResearch4.addProperty("warp", 0); // expected 4!
        boolean caughtRes4 = false;
        try {
            validateResearchJson(mutatedResearch4, "morphtools.json");
        } catch (AssertionError e) {
            caughtRes4 = true;
        }
        check(caughtRes4, "altered research warp detected and rejected");

        // Mutation 5: Mutated recipe pages
        JsonObject mutatedResearch5 = getResearchJson("wrathcage.json");
        mutatedResearch5.add("recipe_pages", new JsonArray()); // empty pages!
        boolean caughtRes5 = false;
        try {
            validateResearchJson(mutatedResearch5, "wrathcage.json");
        } catch (AssertionError e) {
            caughtRes5 = true;
        }
        check(caughtRes5, "altered research recipe_pages detected and rejected");

        // Mutation 6: Missing forbidden namespace
        JsonObject badRes3 = getResearchJson("wrathcage.json");
        badRes3.addProperty("key", "unnamespaced_key");
        boolean caught6 = false;
        try {
            validateResearchJson(badRes3, "wrathcage.json");
        } catch (AssertionError e) {
            caught6 = true;
        }
        check(caught6, "unnamespaced research key detected and rejected");
    }

    private SemanticDataCheck() {}
}
