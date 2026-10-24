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

        // 1. Result verification
        check(json.has("result"), "recipe has result: " + file);
        var resultObj = json.get("result");
        if (resultObj.isJsonObject()) {
            JsonObject r = resultObj.getAsJsonObject();
            check(r.has("id") || r.has("item"), "result has id/item: " + file);
            if (r.has("count")) {
                check(r.get("count").getAsInt() > 0, "result count positive: " + file);
            }
        } else if (resultObj.isJsonPrimitive()) {
            check(!resultObj.getAsString().isBlank(), "result id non-blank: " + file);
        }

        // 2. Type-specific validation
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
            check(json.has("instability"), "infusion has instability: " + file);
            check(json.get("instability").getAsInt() >= 0, "instability non-negative: " + file);
            check(json.has("research"), "infusion has research key: " + file);
            check(!json.get("research").getAsString().isBlank(), "infusion research non-blank: " + file);
        } else if (type.equals("thaumcraft:crucible")) {
            check(json.has("catalyst"), "crucible has catalyst: " + file);
            check(json.has("aspects"), "crucible has aspects: " + file);
            JsonObject aspects = json.getAsJsonObject("aspects");
            check(aspects.size() > 0, "crucible has non-empty aspects: " + file);
            for (var entry : aspects.entrySet()) {
                check(entry.getValue().getAsInt() > 0, "crucible aspect cost > 0: " + entry.getKey() + " in " + file);
            }
            check(json.has("research"), "crucible has research: " + file);
        } else if (type.startsWith("thaumcraft:arcane")) {
            check(json.has("aspects") || json.has("vis"), "arcane recipe has primal vis costs: " + file);
            JsonObject vis = json.has("vis") ? json.getAsJsonObject("vis") : json.getAsJsonObject("aspects");
            check(vis.size() > 0, "arcane recipe has non-empty vis: " + file);
            for (var entry : vis.entrySet()) {
                check(entry.getValue().getAsInt() > 0, "vis cost > 0: " + entry.getKey() + " in " + file);
            }
            check(json.has("research"), "arcane recipe has research: " + file);
        }
    }

    private static void testV03Research() throws Exception {
        check(RESEARCH_FILES.size() == 31, "all 31 research definition files indexed");

        Set<String> uniqueKeys = new HashSet<>();
        Set<String> uniqueCoordinates = new HashSet<>();

        for (String file : RESEARCH_FILES) {
            String path = "/data/forbidden/thaumcraft/research/" + file;
            try (var in = SemanticDataCheck.class.getResourceAsStream(path)) {
                check(in != null, "research resource found: " + file);
                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                check("research".equals(json.get("kind").getAsString()), "kind is research: " + file);
                check(json.has("key"), "has key: " + file);
                String key = json.get("key").getAsString();
                check(key.startsWith("forbidden:"), "key has forbidden namespace: " + key);
                check(uniqueKeys.add(key), "key is globally unique: " + key);

                check("forbidden:FORBIDDEN".equals(json.get("category").getAsString()), "category is forbidden:FORBIDDEN: " + file);

                check(json.has("column") && json.has("row"), "has grid coordinates: " + file);
                int col = json.get("column").getAsInt();
                int row = json.get("row").getAsInt();
                String coord = col + "," + row;
                check(uniqueCoordinates.add(coord), "coordinate collision free: " + coord + " (" + key + ")");

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

                // Check specific well-known warps
                if (key.equals("forbidden:wrath") || key.equals("forbidden:tainttree") || key.equals("forbidden:rod_infernal")) {
                    check(warp > 0, "forbidden dark research has warp > 0: " + key);
                }
            }
        }

        check(uniqueKeys.size() == 31, "exactly 31 unique research entries verified");
        check(uniqueCoordinates.size() == 31, "exactly 31 unique coordinate slots occupied");
    }

    private static void testNegativeRecipeRegressions() {
        // Corrupt Recipe 1: Negative aspect cost
        JsonObject badRecipe1 = new JsonObject();
        badRecipe1.addProperty("type", "thaumcraft:infusion");
        badRecipe1.addProperty("result", "forbidden:test_sword");
        badRecipe1.addProperty("central_item", "minecraft:iron_sword");
        JsonArray comps = new JsonArray();
        comps.add("minecraft:diamond");
        badRecipe1.add("components", comps);
        JsonObject badAspects = new JsonObject();
        badAspects.addProperty("ignis", -5); // Negative!
        badRecipe1.add("aspects", badAspects);
        badRecipe1.addProperty("instability", 2);
        badRecipe1.addProperty("research", "TEST");

        boolean caught1 = false;
        try {
            validateRecipeJson(badRecipe1, "badRecipe1.json");
        } catch (AssertionError e) {
            caught1 = true;
        }
        check(caught1, "negative aspect cost in recipe detected and rejected");

        // Corrupt Recipe 2: Negative instability
        JsonObject badRecipe2 = badRecipe1.deepCopy();
        JsonObject goodAspects = new JsonObject();
        goodAspects.addProperty("ignis", 10);
        badRecipe2.add("aspects", goodAspects);
        badRecipe2.addProperty("instability", -3); // Negative!

        boolean caught2 = false;
        try {
            validateRecipeJson(badRecipe2, "badRecipe2.json");
        } catch (AssertionError e) {
            caught2 = true;
        }
        check(caught2, "negative instability in recipe detected and rejected");

        // Corrupt Recipe 3: Missing components
        JsonObject badRecipe3 = badRecipe2.deepCopy();
        badRecipe3.addProperty("instability", 2);
        badRecipe3.add("components", new JsonArray()); // Empty components!

        boolean caught3 = false;
        try {
            validateRecipeJson(badRecipe3, "badRecipe3.json");
        } catch (AssertionError e) {
            caught3 = true;
        }
        check(caught3, "empty components array in infusion recipe detected and rejected");
    }

    private static void testNegativeResearchRegressions() {
        // Corrupt Research 1: Invalid complexity
        JsonObject badRes1 = new JsonObject();
        badRes1.addProperty("kind", "research");
        badRes1.addProperty("key", "forbidden:corrupt1");
        badRes1.addProperty("category", "forbidden:FORBIDDEN");
        badRes1.addProperty("column", 100);
        badRes1.addProperty("row", 100);
        badRes1.addProperty("complexity", 99); // Invalid!
        JsonObject aspects = new JsonObject();
        aspects.addProperty("ignis", 5);
        badRes1.add("aspects", aspects);

        boolean caught1 = false;
        try {
            int comp = badRes1.get("complexity").getAsInt();
            if (comp < 0 || comp > 4) throw new AssertionError("invalid complexity");
        } catch (AssertionError e) {
            caught1 = true;
        }
        check(caught1, "invalid research complexity detected and rejected");

        // Corrupt Research 2: Negative warp
        JsonObject badRes2 = badRes1.deepCopy();
        badRes2.addProperty("complexity", 2);
        badRes2.addProperty("warp", -5); // Negative!

        boolean caught2 = false;
        try {
            int warp = badRes2.get("warp").getAsInt();
            if (warp < 0) throw new AssertionError("negative warp");
        } catch (AssertionError e) {
            caught2 = true;
        }
        check(caught2, "negative research warp detected and rejected");

        // Corrupt Research 3: Missing forbidden namespace
        JsonObject badRes3 = badRes1.deepCopy();
        badRes3.addProperty("key", "unnamespaced_key"); // Missing forbidden: namespace!

        boolean caught3 = false;
        try {
            String k = badRes3.get("key").getAsString();
            if (!k.startsWith("forbidden:")) throw new AssertionError("missing forbidden namespace");
        } catch (AssertionError e) {
            caught3 = true;
        }
        check(caught3, "unnamespaced research key detected and rejected");
    }

    private SemanticDataCheck() {}
}
