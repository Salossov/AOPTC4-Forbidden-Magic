[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$ResourcesDir = '',
    [string]$TargetJava = ''
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}

if ([string]::IsNullOrWhiteSpace($ResourcesDir)) {
    $ResourcesDir = Join-Path $ProjectRoot 'src/main/resources'
}

if ([string]::IsNullOrWhiteSpace($TargetJava)) {
    $TargetJava = Join-Path $ProjectRoot 'src/nativeVerification/java/forbidden/verification/SemanticDataCheck.java'
}

Write-Host "Scanning resources in: $ResourcesDir" -ForegroundColor Cyan
Write-Host "Target Java file:     $TargetJava" -ForegroundColor Cyan

$recipeFiles = Get-ChildItem -Recurse -Path $ResourcesDir -Filter *.json | Where-Object { $_.FullName -like "*\recipe\*" } | Sort-Object FullName
$researchFiles = Get-ChildItem -Recurse -Path $ResourcesDir -Filter *.json | Where-Object { $_.FullName -like "*\research\*" } | Sort-Object FullName

$recipeListEntries = @()
$recipeMapEntries = @()
$arcaneRecipeCount = 0

foreach ($f in $recipeFiles) {
    $j = Get-Content $f.FullName -Raw | ConvertFrom-Json
    $rel = $f.FullName.Substring($f.FullName.IndexOf("src\main\resources")).Replace("\", "/")
    $resPath = "/" + $rel.Substring("src/main/resources/".Length)
    $recipeListEntries += "`"$resPath`""

    $rtype = $j.type
    if ($rtype -like 'thaumcraft:arcane*') { $arcaneRecipeCount++ }
    $research = if ($j.research) { "`"$($j.research)`"" } else { "null" }
    $resid = if ($j.result.id) { $j.result.id } elseif ($j.result.item) { $j.result.item } elseif ($j.result -is [string]) { $j.result } elseif ($j.output.id) { $j.output.id } elseif ($j.output.item) { $j.output.item } elseif ($j.output -is [string]) { $j.output } else { "" }
    $rescnt = if ($j.result.count) { $j.result.count } elseif ($j.output.count) { $j.output.count } else { 1 }
    $instab = if ($j.instability) { $j.instability } else { 0 }
    
    $central = if ($j.central_item) { $j.central_item } elseif ($j.input) { $j.input } elseif ($j.catalyst) { $j.catalyst } else { $null }
    $centralStr = if ($central -is [string]) { $central } elseif ($central.item) { $central.item } elseif ($central.tag) { "#" + $central.tag } elseif ($central.id) { $central.id } else { $null }
    $cVal = if ($centralStr) { "`"$centralStr`"" } else { "null" }

    $comps = @()
    if ($j.components) {
        foreach ($c in $j.components) {
            $cStr = if ($c -is [string]) { $c } elseif ($c.item) { $c.item } elseif ($c.tag) { "#" + $c.tag } elseif ($c.id) { $c.id } else { "" }
            $comps += "`"$cStr`""
        }
    }
    $compsStr = if ($comps.Count -gt 0) { "List.of(" + ($comps -join ", ") + ")" } else { "List.of()" }

    $asps = @()
    if ($j.aspects -and -not $rtype.StartsWith("thaumcraft:arcane")) {
        foreach ($prop in $j.aspects.PSObject.Properties) {
            $asps += "Map.entry(`"$($prop.Name)`", $($prop.Value))"
        }
    }
    $aspsStr = if ($asps.Count -gt 0) { "Map.ofEntries(" + ($asps -join ", ") + ")" } else { "Map.of()" }

    $vis = @()
    $vObj = if ($j.vis) { $j.vis } elseif ($rtype.StartsWith("thaumcraft:arcane") -and $j.aspects) { $j.aspects } else { $null }
    if ($vObj) {
        foreach ($prop in $vObj.PSObject.Properties) {
            $vis += "Map.entry(`"$($prop.Name)`", $($prop.Value))"
        }
    }
    $visStr = if ($vis.Count -gt 0) { "Map.ofEntries(" + ($vis -join ", ") + ")" } else { "Map.of()" }

    # Pattern (shaped)
    $pattern = @()
    if ($j.pattern) {
        foreach ($p in $j.pattern) { $pattern += "`"$p`"" }
    }
    $patStr = if ($pattern.Count -gt 0) { "List.of(" + ($pattern -join ", ") + ")" } else { "List.of()" }

    # Key (shaped)
    $keys = @()
    if ($j.key) {
        foreach ($prop in $j.key.PSObject.Properties) {
            $v = $prop.Value
            $vStr = if ($v -is [string]) { $v } elseif ($v.item) { $v.item } elseif ($v.tag) { "#" + $v.tag } elseif ($v.id) { $v.id } else { "" }
            $keys += "Map.entry(`"$($prop.Name)`", `"$vStr`")"
        }
    }
    $keyStr = if ($keys.Count -gt 0) { "Map.ofEntries(" + ($keys -join ", ") + ")" } else { "Map.of()" }

    # Ingredients (shapeless)
    $ingrs = @()
    if ($j.ingredients) {
        foreach ($ing in $j.ingredients) {
            $iStr = if ($ing -is [string]) { $ing } elseif ($ing.item) { $ing.item } elseif ($ing.tag) { "#" + $ing.tag } elseif ($ing.id) { $ing.id } else { "" }
            $ingrs += "`"$iStr`""
        }
    }
    $ingrsStr = if ($ingrs.Count -gt 0) { "List.of(" + ($ingrs -join ", ") + ")" } else { "List.of()" }

    # Mana (mana_infusion)
    $mana = if ($j.mana) { [int]$j.mana } else { 0 }

    # Altar properties (bloodmagic:altar)
    $altarSyphon = if ($j.altarSyphon) { [int]$j.altarSyphon } else { 0 }
    $consumptionRate = if ($j.consumptionRate) { [int]$j.consumptionRate } else { 0 }
    $drainRate = if ($j.drainRate) { [int]$j.drainRate } else { 0 }
    $upgradeLevel = if ($j.upgradeLevel) { [int]$j.upgradeLevel } else { 0 }

    $recipeMapEntries += "            Map.entry(`"$resPath`", new RecipeSpec(`"$rtype`", $research, `"$resid`", $rescnt, $instab, $cVal, $compsStr, $aspsStr, $visStr, $patStr, $keyStr, $ingrsStr, $mana, $altarSyphon, $consumptionRate, $drainRate, $upgradeLevel))"
}

$researchListEntries = @()
$researchMapEntries = @()

foreach ($f in $researchFiles) {
    $j = Get-Content $f.FullName -Raw | ConvertFrom-Json
    $rel = $f.FullName.Substring($f.FullName.IndexOf("src\main\resources")).Replace("\", "/")
    $resPath = "/" + $rel.Substring("src/main/resources/".Length)
    $researchListEntries += "`"$resPath`""

    $key = $j.key
    $col = $j.column
    $row = $j.row
    $comp = $j.complexity
    $warp = if ($j.warp) { $j.warp } else { 0 }
    
    $parents = @()
    if ($j.parents) {
        foreach ($p in $j.parents) { $parents += "`"$p`"" }
    }
    $pStr = if ($parents.Count -gt 0) { "List.of(" + ($parents -join ", ") + ")" } else { "List.of()" }

    $hparents = @()
    if ($j.hidden_parents) {
        foreach ($p in $j.hidden_parents) { $hparents += "`"$p`"" }
    }
    $hpStr = if ($hparents.Count -gt 0) { "List.of(" + ($hparents -join ", ") + ")" } else { "List.of()" }

    $rpages = @()
    if ($j.recipe_pages) {
        foreach ($p in $j.recipe_pages) { $rpages += "`"$p`"" }
    }
    $rpStr = if ($rpages.Count -gt 0) { "List.of(" + ($rpages -join ", ") + ")" } else { "List.of()" }

    $tpages = @()
    if ($j.text_pages) {
        foreach ($p in $j.text_pages) { $tpages += "`"$p`"" }
    }
    $tpStr = if ($tpages.Count -gt 0) { "List.of(" + ($tpages -join ", ") + ")" } else { "List.of()" }

    $porder = @()
    if ($j.page_order) {
        foreach ($p in $j.page_order) { $porder += "`"$p`"" }
    }
    $poStr = if ($porder.Count -gt 0) { "List.of(" + ($porder -join ", ") + ")" } else { "List.of()" }

    $asps = @()
    if ($j.aspects) {
        foreach ($prop in $j.aspects.PSObject.Properties) {
            $asps += "Map.entry(`"$($prop.Name)`", $($prop.Value))"
        }
    }
    $aspsStr = if ($asps.Count -gt 0) { "Map.ofEntries(" + ($asps -join ", ") + ")" } else { "Map.of()" }

    $auto = if ($j.auto_unlock) { "true" } else { "false" }
    $stub = if ($j.stub) { "true" } else { "false" }
    $concealed = if ($j.concealed) { "true" } else { "false" }
    $secondary = if ($j.secondary) { "true" } else { "false" }
    $round = if ($j.round) { "true" } else { "false" }
    $hidden = if ($j.hidden) { "true" } else { "false" }

    $sibs = @()
    if ($j.siblings) {
        foreach ($p in $j.siblings) { $sibs += "`"$p`"" }
    }
    $sibsStr = if ($sibs.Count -gt 0) { "List.of(" + ($sibs -join ", ") + ")" } else { "List.of()" }

    $researchMapEntries += "            Map.entry(`"$resPath`", new ResearchSpec(`"$key`", $col, $row, $comp, $warp, $pStr, $hpStr, $rpStr, $tpStr, $poStr, $aspsStr, $auto, $stub, $concealed, $secondary, $round, $hidden, $sibsStr))"
}

$sb = [System.Text.StringBuilder]::new()
$sb.AppendLine("package forbidden.verification;") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("import com.google.gson.*;") | Out-Null
$sb.AppendLine("import java.io.InputStreamReader;") | Out-Null
$sb.AppendLine("import java.nio.charset.StandardCharsets;") | Out-Null
$sb.AppendLine("import java.util.*;") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("/**") | Out-Null
$sb.AppendLine(" * Dedicated semantic data verification suite for V02 (Recipes) and V03 (Research).") | Out-Null
$sb.AppendLine(" * Structural snapshot validation of $($recipeFiles.Count) recipes and $($researchFiles.Count) research files.") | Out-Null
$sb.AppendLine(" * Generated from current data; independent legacy parity requires separate review.") | Out-Null
$sb.AppendLine(" * across base and integration datapacks (Botania, Blood Magic, Enchanting).") | Out-Null
$sb.AppendLine(" * Verifies exact types, results, counts, inputs, components, aspect costs, vis costs,") | Out-Null
$sb.AppendLine(" * coordinates, complexity, warp, parents, hidden parents, recipe pages, text pages, page order,") | Out-Null
$sb.AppendLine(" * pattern, key, ingredients, mana, altarSyphon, consumptionRate, drainRate, upgradeLevel,") | Out-Null
$sb.AppendLine(" * and research flags (concealed, secondary, round, hidden, siblings).") | Out-Null
$sb.AppendLine(" * Includes extensive negative mutation regressions for all key semantic properties.") | Out-Null
$sb.AppendLine(" */") | Out-Null
$sb.AppendLine("public final class SemanticDataCheck {") | Out-Null
$sb.AppendLine("    private static int checks = 0;") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void check(boolean ok, String reason) {") | Out-Null
$sb.AppendLine("        checks++;") | Out-Null
$sb.AppendLine("        if (!ok) {") | Out-Null
$sb.AppendLine("            throw new AssertionError(""Semantic data check failed: "" + reason);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    public record RecipeSpec(") | Out-Null
$sb.AppendLine("            String type,") | Out-Null
$sb.AppendLine("            String research,") | Out-Null
$sb.AppendLine("            String resultId,") | Out-Null
$sb.AppendLine("            int resultCount,") | Out-Null
$sb.AppendLine("            int instability,") | Out-Null
$sb.AppendLine("            String centralInput,") | Out-Null
$sb.AppendLine("            List<String> components,") | Out-Null
$sb.AppendLine("            Map<String, Integer> aspects,") | Out-Null
$sb.AppendLine("            Map<String, Integer> vis,") | Out-Null
$sb.AppendLine("            List<String> pattern,") | Out-Null
$sb.AppendLine("            Map<String, String> key,") | Out-Null
$sb.AppendLine("            List<String> ingredients,") | Out-Null
$sb.AppendLine("            int mana,") | Out-Null
$sb.AppendLine("            int altarSyphon,") | Out-Null
$sb.AppendLine("            int consumptionRate,") | Out-Null
$sb.AppendLine("            int drainRate,") | Out-Null
$sb.AppendLine("            int upgradeLevel") | Out-Null
$sb.AppendLine("    ) {}") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    public record ResearchSpec(") | Out-Null
$sb.AppendLine("            String key,") | Out-Null
$sb.AppendLine("            int col,") | Out-Null
$sb.AppendLine("            int row,") | Out-Null
$sb.AppendLine("            int comp,") | Out-Null
$sb.AppendLine("            int warp,") | Out-Null
$sb.AppendLine("            List<String> parents,") | Out-Null
$sb.AppendLine("            List<String> hiddenParents,") | Out-Null
$sb.AppendLine("            List<String> recipePages,") | Out-Null
$sb.AppendLine("            List<String> textPages,") | Out-Null
$sb.AppendLine("            List<String> pageOrder,") | Out-Null
$sb.AppendLine("            Map<String, Integer> aspects,") | Out-Null
$sb.AppendLine("            boolean autoUnlock,") | Out-Null
$sb.AppendLine("            boolean stub,") | Out-Null
$sb.AppendLine("            boolean concealed,") | Out-Null
$sb.AppendLine("            boolean secondary,") | Out-Null
$sb.AppendLine("            boolean round,") | Out-Null
$sb.AppendLine("            boolean hidden,") | Out-Null
$sb.AppendLine("            List<String> siblings") | Out-Null
$sb.AppendLine("    ) {}") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static final List<String> RECIPE_FILES = List.of(") | Out-Null
$sb.AppendLine("            " + ($recipeListEntries -join ",`n            ")) | Out-Null
$sb.AppendLine("    );") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static final List<String> RESEARCH_FILES = List.of(") | Out-Null
$sb.AppendLine("            " + ($researchListEntries -join ",`n            ")) | Out-Null
$sb.AppendLine("    );") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static final Map<String, RecipeSpec> RECIPE_SPECS = Map.ofEntries(") | Out-Null
$sb.AppendLine(($recipeMapEntries -join ",`n")) | Out-Null
$sb.AppendLine("    );") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static final Map<String, ResearchSpec> RESEARCH_SPECS = Map.ofEntries(") | Out-Null
$sb.AppendLine(($researchMapEntries -join ",`n")) | Out-Null
$sb.AppendLine("    );") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    public static void main(String[] args) throws Exception {") | Out-Null
$sb.AppendLine("        testV02Recipes();") | Out-Null
$sb.AppendLine("        testV03Research();") | Out-Null
$sb.AppendLine("        testNegativeRecipeRegressions();") | Out-Null
$sb.AppendLine("        testNegativeResearchRegressions();") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        System.out.println(""FORBIDDEN_SEMANTIC_DATA_PASS checks="" + checks);") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void testV02Recipes() throws Exception {") | Out-Null
$sb.AppendLine("        check(RECIPE_FILES.size() == $($recipeFiles.Count), ""all recipe definition files indexed"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        int arcaneCount = 0;") | Out-Null
$sb.AppendLine("        int crucibleCount = 0;") | Out-Null
$sb.AppendLine("        int infusionCount = 0;") | Out-Null
$sb.AppendLine("        int craftingCount = 0;") | Out-Null
$sb.AppendLine("        int smeltingCount = 0;") | Out-Null
$sb.AppendLine("        int manaInfusionCount = 0;") | Out-Null
$sb.AppendLine("        int altarCount = 0;") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        for (String file : RECIPE_FILES) {") | Out-Null
$sb.AppendLine("            try (var in = SemanticDataCheck.class.getResourceAsStream(file)) {") | Out-Null
$sb.AppendLine("                check(in != null, ""recipe resource found: "" + file);") | Out-Null
$sb.AppendLine("                var reader = new InputStreamReader(in, StandardCharsets.UTF_8);") | Out-Null
$sb.AppendLine("                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("                check(json.has(""type""), ""recipe has type: "" + file);") | Out-Null
$sb.AppendLine("                String type = json.get(""type"").getAsString();") | Out-Null
$sb.AppendLine("                check(!type.isBlank(), ""recipe type non-blank: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("                validateRecipeJson(json, file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("                if (type.startsWith(""thaumcraft:arcane"")) arcaneCount++;") | Out-Null
$sb.AppendLine("                else if (type.equals(""thaumcraft:crucible"")) crucibleCount++;") | Out-Null
$sb.AppendLine("                else if (type.startsWith(""thaumcraft:infusion"")) infusionCount++;") | Out-Null
$sb.AppendLine("                else if (type.startsWith(""minecraft:crafting"")) craftingCount++;") | Out-Null
$sb.AppendLine("                else if (type.equals(""minecraft:smelting"")) smeltingCount++;") | Out-Null
$sb.AppendLine("                else if (type.equals(""botania:mana_infusion"")) manaInfusionCount++;") | Out-Null
$sb.AppendLine("                else if (type.equals(""bloodmagic:altar"")) altarCount++;") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        check(arcaneCount == $arcaneRecipeCount, ""arcane workbench recipe variants indexed"");") | Out-Null
$sb.AppendLine("        check(crucibleCount == 4, ""exactly 4 crucible recipes verified"");") | Out-Null
$sb.AppendLine("        check(infusionCount == 32, ""exactly 32 infusion/infusion_enchantment recipes verified"");") | Out-Null
$sb.AppendLine("        check(craftingCount == 12, ""exactly 12 crafting recipes verified"");") | Out-Null
$sb.AppendLine("        check(smeltingCount == 1, ""exactly 1 smelting recipe verified"");") | Out-Null
$sb.AppendLine("        check(manaInfusionCount == 3, ""exactly 3 mana infusion recipes verified"");") | Out-Null
$sb.AppendLine("        check(altarCount == 2, ""exactly 2 altar recipes verified"");") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static JsonObject getJson(String resPath) {") | Out-Null
$sb.AppendLine("        try (var in = SemanticDataCheck.class.getResourceAsStream(resPath)) {") | Out-Null
$sb.AppendLine("            if (in == null) throw new IllegalArgumentException(""Resource not found: "" + resPath);") | Out-Null
$sb.AppendLine("            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();") | Out-Null
$sb.AppendLine("        } catch (Exception e) {") | Out-Null
$sb.AppendLine("            throw new RuntimeException(e);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void validateRecipeJson(JsonObject json, String file) {") | Out-Null
$sb.AppendLine("        String type = json.get(""type"").getAsString();") | Out-Null
$sb.AppendLine("        RecipeSpec spec = RECIPE_SPECS.get(file);") | Out-Null
$sb.AppendLine("        check(spec != null, ""recipe spec exists for: "" + file);") | Out-Null
$sb.AppendLine("        check(type.equals(spec.type), ""recipe type matches spec: "" + type + "" vs "" + spec.type + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 1. Result verification") | Out-Null
$sb.AppendLine("        if (!type.equals(""thaumcraft:infusion_enchantment"")) {") | Out-Null
$sb.AppendLine("            check(json.has(""result"") || json.has(""output""), ""recipe has result/output: "" + file);") | Out-Null
$sb.AppendLine("            var resultObj = json.has(""result"") ? json.get(""result"") : json.get(""output"");") | Out-Null
$sb.AppendLine("            String actualResultId = """";") | Out-Null
$sb.AppendLine("            int actualCount = 1;") | Out-Null
$sb.AppendLine("            if (resultObj.isJsonObject()) {") | Out-Null
$sb.AppendLine("                JsonObject r = resultObj.getAsJsonObject();") | Out-Null
$sb.AppendLine("                check(r.has(""id"") || r.has(""item""), ""result has id/item: "" + file);") | Out-Null
$sb.AppendLine("                actualResultId = r.has(""id"") ? r.get(""id"").getAsString() : r.get(""item"").getAsString();") | Out-Null
$sb.AppendLine("                if (r.has(""count"")) {") | Out-Null
$sb.AppendLine("                    actualCount = r.get(""count"").getAsInt();") | Out-Null
$sb.AppendLine("                    check(actualCount > 0, ""result count positive: "" + file);") | Out-Null
$sb.AppendLine("                }") | Out-Null
$sb.AppendLine("            } else if (resultObj.isJsonPrimitive()) {") | Out-Null
$sb.AppendLine("                actualResultId = resultObj.getAsString();") | Out-Null
$sb.AppendLine("                check(!actualResultId.isBlank(), ""result id non-blank: "" + file);") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(spec.resultId.equals(actualResultId), ""result matches spec: "" + spec.resultId + "" vs "" + actualResultId + "" in "" + file);") | Out-Null
$sb.AppendLine("            check(spec.resultCount == actualCount, ""result count matches spec: "" + spec.resultCount + "" vs "" + actualCount + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 2. Research & Instability verification against spec") | Out-Null
$sb.AppendLine("        if (spec.research != null) {") | Out-Null
$sb.AppendLine("            check(json.has(""research""), ""recipe has research: "" + file);") | Out-Null
$sb.AppendLine("            check(spec.research.equals(json.get(""research"").getAsString()), ""research key matches spec: "" + spec.research + "" in "" + file);") | Out-Null
$sb.AppendLine("        } else {") | Out-Null
$sb.AppendLine("            check(!json.has(""research"") || json.get(""research"").getAsString().isBlank(), ""recipe without research spec has none: "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        int actualInstability = json.has(""instability"") ? json.get(""instability"").getAsInt() : 0;") | Out-Null
$sb.AppendLine("        check(actualInstability == spec.instability, ""instability matches spec: "" + spec.instability + "" vs "" + actualInstability + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 3. Central input verification") | Out-Null
$sb.AppendLine("        if (spec.centralInput != null) {") | Out-Null
$sb.AppendLine("            check(json.has(""input"") || json.has(""central_item"") || json.has(""central"") || json.has(""catalyst""), ""has central item: "" + file);") | Out-Null
$sb.AppendLine("            var cObj = json.has(""input"") ? json.get(""input"") : (json.has(""central_item"") ? json.get(""central_item"") : (json.has(""central"") ? json.get(""central"") : json.get(""catalyst"")));") | Out-Null
$sb.AppendLine("            String actualCentral = """";") | Out-Null
$sb.AppendLine("            if (cObj.isJsonObject()) {") | Out-Null
$sb.AppendLine("                JsonObject co = cObj.getAsJsonObject();") | Out-Null
$sb.AppendLine("                if (co.has(""item"")) actualCentral = co.get(""item"").getAsString();") | Out-Null
$sb.AppendLine("                else if (co.has(""id"")) actualCentral = co.get(""id"").getAsString();") | Out-Null
$sb.AppendLine("                else if (co.has(""tag"")) actualCentral = ""#"" + co.get(""tag"").getAsString();") | Out-Null
$sb.AppendLine("            } else if (cObj.isJsonPrimitive()) {") | Out-Null
$sb.AppendLine("                actualCentral = cObj.getAsString();") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(spec.centralInput.equals(actualCentral), ""central input matches spec: "" + spec.centralInput + "" vs "" + actualCentral + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 4. Components verification") | Out-Null
$sb.AppendLine("        if (!spec.components.isEmpty()) {") | Out-Null
$sb.AppendLine("            check(json.has(""components""), ""recipe has components: "" + file);") | Out-Null
$sb.AppendLine("            List<String> actualComps = new ArrayList<>();") | Out-Null
$sb.AppendLine("            for (var elem : json.getAsJsonArray(""components"")) {") | Out-Null
$sb.AppendLine("                if (elem.isJsonObject()) {") | Out-Null
$sb.AppendLine("                    JsonObject co = elem.getAsJsonObject();") | Out-Null
$sb.AppendLine("                    if (co.has(""item"")) actualComps.add(co.get(""item"").getAsString());") | Out-Null
$sb.AppendLine("                    else if (co.has(""id"")) actualComps.add(co.get(""id"").getAsString());") | Out-Null
$sb.AppendLine("                    else if (co.has(""tag"")) actualComps.add(""#"" + co.get(""tag"").getAsString());") | Out-Null
$sb.AppendLine("                } else if (elem.isJsonPrimitive()) {") | Out-Null
$sb.AppendLine("                    actualComps.add(elem.getAsString());") | Out-Null
$sb.AppendLine("                }") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(actualComps.size() == spec.components.size(), ""components count matches spec: "" + spec.components.size() + "" vs "" + actualComps.size() + "" in "" + file);") | Out-Null
$sb.AppendLine("            List<String> sortedActual = new ArrayList<>(actualComps);") | Out-Null
$sb.AppendLine("            List<String> sortedExpected = new ArrayList<>(spec.components);") | Out-Null
$sb.AppendLine("            Collections.sort(sortedActual);") | Out-Null
$sb.AppendLine("            Collections.sort(sortedExpected);") | Out-Null
$sb.AppendLine("            check(sortedActual.equals(sortedExpected), ""components match spec in: "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 5. Aspects verification (infusion / crucible)") | Out-Null
$sb.AppendLine("        if (!spec.aspects.isEmpty()) {") | Out-Null
$sb.AppendLine("            check(json.has(""aspects""), ""recipe has aspects: "" + file);") | Out-Null
$sb.AppendLine("            JsonObject aspObj = json.getAsJsonObject(""aspects"");") | Out-Null
$sb.AppendLine("            Map<String, Integer> actualAsp = new HashMap<>();") | Out-Null
$sb.AppendLine("            for (var entry : aspObj.entrySet()) {") | Out-Null
$sb.AppendLine("                actualAsp.put(entry.getKey(), entry.getValue().getAsInt());") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(spec.aspects.equals(actualAsp), ""aspects match spec: "" + spec.aspects + "" vs "" + actualAsp + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 6. Vis verification (arcane)") | Out-Null
$sb.AppendLine("        if (!spec.vis.isEmpty()) {") | Out-Null
$sb.AppendLine("            check(json.has(""vis"") || json.has(""aspects""), ""arcane recipe has vis: "" + file);") | Out-Null
$sb.AppendLine("            JsonObject visObj = json.has(""vis"") ? json.getAsJsonObject(""vis"") : json.getAsJsonObject(""aspects"");") | Out-Null
$sb.AppendLine("            Map<String, Integer> actualVis = new HashMap<>();") | Out-Null
$sb.AppendLine("            for (var entry : visObj.entrySet()) {") | Out-Null
$sb.AppendLine("                actualVis.put(entry.getKey(), entry.getValue().getAsInt());") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(spec.vis.equals(actualVis), ""vis matches spec: "" + spec.vis + "" vs "" + actualVis + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 7. Pattern & Key verification (shaped)") | Out-Null
$sb.AppendLine("        if (!spec.pattern.isEmpty()) {") | Out-Null
$sb.AppendLine("            check(json.has(""pattern""), ""shaped recipe has pattern: "" + file);") | Out-Null
$sb.AppendLine("            List<String> actualPat = new ArrayList<>();") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""pattern"")) actualPat.add(p.getAsString());") | Out-Null
$sb.AppendLine("            check(spec.pattern.equals(actualPat), ""pattern matches spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("            check(json.has(""key""), ""shaped recipe has key map: "" + file);") | Out-Null
$sb.AppendLine("            Map<String, String> actualKey = new HashMap<>();") | Out-Null
$sb.AppendLine("            for (var entry : json.getAsJsonObject(""key"").entrySet()) {") | Out-Null
$sb.AppendLine("                String k = entry.getKey();") | Out-Null
$sb.AppendLine("                var v = entry.getValue();") | Out-Null
$sb.AppendLine("                String valStr = """";") | Out-Null
$sb.AppendLine("                if (v.isJsonObject()) {") | Out-Null
$sb.AppendLine("                    var vo = v.getAsJsonObject();") | Out-Null
$sb.AppendLine("                    if (vo.has(""item"")) valStr = vo.get(""item"").getAsString();") | Out-Null
$sb.AppendLine("                    else if (vo.has(""tag"")) valStr = ""#"" + vo.get(""tag"").getAsString();") | Out-Null
$sb.AppendLine("                    else if (vo.has(""id"")) valStr = vo.get(""id"").getAsString();") | Out-Null
$sb.AppendLine("                } else if (v.isJsonPrimitive()) {") | Out-Null
$sb.AppendLine("                    valStr = v.getAsString();") | Out-Null
$sb.AppendLine("                }") | Out-Null
$sb.AppendLine("                actualKey.put(k, valStr);") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            check(spec.key.equals(actualKey), ""key map matches spec: "" + spec.key + "" vs "" + actualKey + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 8. Ingredients verification (shapeless)") | Out-Null
$sb.AppendLine("        if (!spec.ingredients.isEmpty()) {") | Out-Null
$sb.AppendLine("            check(json.has(""ingredients""), ""shapeless recipe has ingredients: "" + file);") | Out-Null
$sb.AppendLine("            List<String> actualIngrs = new ArrayList<>();") | Out-Null
$sb.AppendLine("            for (var elem : json.getAsJsonArray(""ingredients"")) {") | Out-Null
$sb.AppendLine("                if (elem.isJsonObject()) {") | Out-Null
$sb.AppendLine("                    var eo = elem.getAsJsonObject();") | Out-Null
$sb.AppendLine("                    if (eo.has(""item"")) actualIngrs.add(eo.get(""item"").getAsString());") | Out-Null
$sb.AppendLine("                    else if (eo.has(""tag"")) actualIngrs.add(""#"" + eo.get(""tag"").getAsString());") | Out-Null
$sb.AppendLine("                    else if (eo.has(""id"")) actualIngrs.add(eo.get(""id"").getAsString());") | Out-Null
$sb.AppendLine("                } else if (elem.isJsonPrimitive()) {") | Out-Null
$sb.AppendLine("                    actualIngrs.add(elem.getAsString());") | Out-Null
$sb.AppendLine("                }") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("            List<String> sortedActual = new ArrayList<>(actualIngrs);") | Out-Null
$sb.AppendLine("            List<String> sortedExpected = new ArrayList<>(spec.ingredients);") | Out-Null
$sb.AppendLine("            Collections.sort(sortedActual);") | Out-Null
$sb.AppendLine("            Collections.sort(sortedExpected);") | Out-Null
$sb.AppendLine("            check(sortedActual.equals(sortedExpected), ""ingredients match spec in: "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 9. Mana Infusion verification") | Out-Null
$sb.AppendLine("        if (spec.mana > 0) {") | Out-Null
$sb.AppendLine("            check(json.has(""mana""), ""mana infusion has mana: "" + file);") | Out-Null
$sb.AppendLine("            check(json.get(""mana"").getAsInt() == spec.mana, ""mana matches spec: "" + spec.mana + "" vs "" + json.get(""mana"").getAsInt() + "" in "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // 10. Blood Altar verification") | Out-Null
$sb.AppendLine("        if (spec.altarSyphon > 0) {") | Out-Null
$sb.AppendLine("            check(json.has(""altarSyphon""), ""altar recipe has altarSyphon: "" + file);") | Out-Null
$sb.AppendLine("            check(json.get(""altarSyphon"").getAsInt() == spec.altarSyphon, ""altarSyphon matches spec in: "" + file);") | Out-Null
$sb.AppendLine("            check(json.has(""consumptionRate""), ""altar recipe has consumptionRate: "" + file);") | Out-Null
$sb.AppendLine("            check(json.get(""consumptionRate"").getAsInt() == spec.consumptionRate, ""consumptionRate matches spec in: "" + file);") | Out-Null
$sb.AppendLine("            check(json.has(""drainRate""), ""altar recipe has drainRate: "" + file);") | Out-Null
$sb.AppendLine("            check(json.get(""drainRate"").getAsInt() == spec.drainRate, ""drainRate matches spec in: "" + file);") | Out-Null
$sb.AppendLine("            check(json.has(""upgradeLevel""), ""altar recipe has upgradeLevel: "" + file);") | Out-Null
$sb.AppendLine("            check(json.get(""upgradeLevel"").getAsInt() == spec.upgradeLevel, ""upgradeLevel matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void validateResearchJson(JsonObject json, String file) {") | Out-Null
$sb.AppendLine("        check(""research"".equals(json.get(""kind"").getAsString()), ""kind is research: "" + file);") | Out-Null
$sb.AppendLine("        check(json.has(""key""), ""has key: "" + file);") | Out-Null
$sb.AppendLine("        String key = json.get(""key"").getAsString();") | Out-Null
$sb.AppendLine("        check(key.startsWith(""forbidden:""), ""key has forbidden namespace: "" + key);") | Out-Null
$sb.AppendLine("        check(""forbidden:forbidden"".equals(json.get(""category"").getAsString()), ""category is forbidden:forbidden: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        ResearchSpec spec = RESEARCH_SPECS.get(file);") | Out-Null
$sb.AppendLine("        check(spec != null, ""research spec exists for: "" + file);") | Out-Null
$sb.AppendLine("        check(spec.key.equals(key), ""research key matches spec: "" + spec.key + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        int col = json.get(""column"").getAsInt();") | Out-Null
$sb.AppendLine("        int row = json.get(""row"").getAsInt();") | Out-Null
$sb.AppendLine("        check(col == spec.col, ""col matches: "" + spec.col + "" vs "" + col + "" in "" + file);") | Out-Null
$sb.AppendLine("        check(row == spec.row, ""row matches: "" + spec.row + "" vs "" + row + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        int comp = json.get(""complexity"").getAsInt();") | Out-Null
$sb.AppendLine("        check(comp == spec.comp, ""comp matches: "" + spec.comp + "" vs "" + comp + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        int warp = json.has(""warp"") ? json.get(""warp"").getAsInt() : 0;") | Out-Null
$sb.AppendLine("        check(warp == spec.warp, ""warp matches: "" + spec.warp + "" vs "" + warp + "" in "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Parents") | Out-Null
$sb.AppendLine("        List<String> actualParents = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""parents"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""parents"")) actualParents.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualParents.equals(spec.parents), ""parents match spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Hidden Parents") | Out-Null
$sb.AppendLine("        List<String> actualHiddenParents = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""hidden_parents"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""hidden_parents"")) actualHiddenParents.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualHiddenParents.equals(spec.hiddenParents), ""hidden parents match spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Recipe Pages") | Out-Null
$sb.AppendLine("        List<String> actualRecipePages = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""recipe_pages"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""recipe_pages"")) actualRecipePages.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualRecipePages.equals(spec.recipePages), ""recipe pages match spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Text Pages") | Out-Null
$sb.AppendLine("        List<String> actualTextPages = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""text_pages"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""text_pages"")) actualTextPages.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualTextPages.equals(spec.textPages), ""text pages match spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Page Order") | Out-Null
$sb.AppendLine("        List<String> actualPageOrder = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""page_order"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""page_order"")) actualPageOrder.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualPageOrder.equals(spec.pageOrder), ""page_order matches spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Aspects") | Out-Null
$sb.AppendLine("        Map<String, Integer> actualAspects = new HashMap<>();") | Out-Null
$sb.AppendLine("        if (json.has(""aspects"")) {") | Out-Null
$sb.AppendLine("            for (var entry : json.getAsJsonObject(""aspects"").entrySet()) {") | Out-Null
$sb.AppendLine("                actualAspects.put(entry.getKey(), entry.getValue().getAsInt());") | Out-Null
$sb.AppendLine("            }") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(spec.aspects.equals(actualAspects), ""research aspects match spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Flags") | Out-Null
$sb.AppendLine("        boolean actualAuto = json.has(""auto_unlock"") && json.get(""auto_unlock"").getAsBoolean();") | Out-Null
$sb.AppendLine("        boolean actualStub = json.has(""stub"") && json.get(""stub"").getAsBoolean();") | Out-Null
$sb.AppendLine("        boolean actualConcealed = json.has(""concealed"") && json.get(""concealed"").getAsBoolean();") | Out-Null
$sb.AppendLine("        boolean actualSecondary = json.has(""secondary"") && json.get(""secondary"").getAsBoolean();") | Out-Null
$sb.AppendLine("        boolean actualRound = json.has(""round"") && json.get(""round"").getAsBoolean();") | Out-Null
$sb.AppendLine("        boolean actualHidden = json.has(""hidden"") && json.get(""hidden"").getAsBoolean();") | Out-Null
$sb.AppendLine("        check(actualAuto == spec.autoUnlock, ""auto_unlock matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        check(actualStub == spec.stub, ""stub matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        check(actualConcealed == spec.concealed, ""concealed matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        check(actualSecondary == spec.secondary, ""secondary matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        check(actualRound == spec.round, ""round matches spec in: "" + file);") | Out-Null
$sb.AppendLine("        check(actualHidden == spec.hidden, ""hidden matches spec in: "" + file);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Siblings") | Out-Null
$sb.AppendLine("        List<String> actualSiblings = new ArrayList<>();") | Out-Null
$sb.AppendLine("        if (json.has(""siblings"")) {") | Out-Null
$sb.AppendLine("            for (var p : json.getAsJsonArray(""siblings"")) actualSiblings.add(p.getAsString());") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(actualSiblings.equals(spec.siblings), ""siblings match spec in: "" + file);") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void testV03Research() throws Exception {") | Out-Null
$sb.AppendLine("        check(RESEARCH_FILES.size() == $($researchFiles.Count), ""all research definition files indexed"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        Set<String> uniqueKeys = new HashSet<>();") | Out-Null
$sb.AppendLine("        Set<String> uniqueCoordinates = new HashSet<>();") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        for (String file : RESEARCH_FILES) {") | Out-Null
$sb.AppendLine("            JsonObject json = getJson(file);") | Out-Null
$sb.AppendLine("            String key = json.get(""key"").getAsString();") | Out-Null
$sb.AppendLine("            check(uniqueKeys.add(key), ""key is globally unique: "" + key);") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("            int col = json.get(""column"").getAsInt();") | Out-Null
$sb.AppendLine("            int row = json.get(""row"").getAsInt();") | Out-Null
$sb.AppendLine("            String coord = col + "","" + row;") | Out-Null
$sb.AppendLine("            check(uniqueCoordinates.add(coord), ""coordinate collision free: "" + coord + "" ("" + key + "")"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("            validateResearchJson(json, file);") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        check(uniqueKeys.size() == 49, ""exactly 49 unique research entries verified"");") | Out-Null
$sb.AppendLine("        check(uniqueCoordinates.size() == 49, ""exactly 49 unique coordinate slots occupied"");") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void testNegativeRecipeRegressions() {") | Out-Null
$sb.AppendLine("        String target = ""/data/forbidden/recipe/wrath_cage.json"";") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 1: Alter recipe research key") | Out-Null
$sb.AppendLine("        JsonObject m1 = getJson(target);") | Out-Null
$sb.AppendLine("        m1.addProperty(""research"", ""forbidden:invalid_research"");") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m1, target), ""altered recipe research key"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 2: Alter recipe instability") | Out-Null
$sb.AppendLine("        JsonObject m2 = getJson(target);") | Out-Null
$sb.AppendLine("        m2.addProperty(""instability"", 0);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m2, target), ""altered recipe instability"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 3: Alter recipe result") | Out-Null
$sb.AppendLine("        JsonObject m3 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonObject fakeRes = new JsonObject();") | Out-Null
$sb.AppendLine("        fakeRes.addProperty(""id"", ""minecraft:dirt"");") | Out-Null
$sb.AppendLine("        m3.add(""result"", fakeRes);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m3, target), ""altered recipe result"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 4: Alter result count") | Out-Null
$sb.AppendLine("        JsonObject m4 = getJson(""/data/forbidden/recipe/emerald_nuggets.json"");") | Out-Null
$sb.AppendLine("        JsonObject badCountRes = m4.getAsJsonObject(""result"").deepCopy();") | Out-Null
$sb.AppendLine("        badCountRes.addProperty(""count"", 1);") | Out-Null
$sb.AppendLine("        m4.add(""result"", badCountRes);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m4, ""/data/forbidden/recipe/emerald_nuggets.json""), ""altered result count"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 5: Alter central input") | Out-Null
$sb.AppendLine("        JsonObject m5 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonObject fakeCentral = new JsonObject();") | Out-Null
$sb.AppendLine("        fakeCentral.addProperty(""item"", ""minecraft:dirt"");") | Out-Null
$sb.AppendLine("        m5.add(""input"", fakeCentral);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m5, target), ""altered central input"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 6: Alter components") | Out-Null
$sb.AppendLine("        JsonObject m6 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonArray badComps = m6.getAsJsonArray(""components"").deepCopy();") | Out-Null
$sb.AppendLine("        badComps.remove(0);") | Out-Null
$sb.AppendLine("        m6.add(""components"", badComps);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m6, target), ""altered components count"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 7: Alter aspect amounts") | Out-Null
$sb.AppendLine("        JsonObject m7 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonObject badAsp = m7.getAsJsonObject(""aspects"").deepCopy();") | Out-Null
$sb.AppendLine("        badAsp.addProperty(""forbidden:ira"", 999);") | Out-Null
$sb.AppendLine("        m7.add(""aspects"", badAsp);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m7, target), ""altered aspect amount"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 8: Alter vis cost") | Out-Null
$sb.AppendLine("        JsonObject m8 = getJson(""/data/forbidden/recipe/taint_stone.json"");") | Out-Null
$sb.AppendLine("        JsonObject badVis = (m8.has(""vis"") ? m8.getAsJsonObject(""vis"") : m8.getAsJsonObject(""aspects"")).deepCopy();") | Out-Null
$sb.AppendLine("        badVis.addProperty(""ordo"", 999);") | Out-Null
$sb.AppendLine("        if (m8.has(""vis"")) m8.add(""vis"", badVis); else m8.add(""aspects"", badVis);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m8, ""/data/forbidden/recipe/taint_stone.json""), ""altered vis cost"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 9: Alter pattern on shaped recipe") | Out-Null
$sb.AppendLine("        JsonObject m9 = getJson(""/data/forbidden/recipe/emerald.json"");") | Out-Null
$sb.AppendLine("        JsonArray badPattern = new JsonArray();") | Out-Null
$sb.AppendLine("        badPattern.add(""###"");") | Out-Null
$sb.AppendLine("        m9.add(""pattern"", badPattern);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m9, ""/data/forbidden/recipe/emerald.json""), ""altered pattern"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 10: Alter key mapping on shaped recipe") | Out-Null
$sb.AppendLine("        JsonObject m10 = getJson(""/data/forbidden/recipe/emerald.json"");") | Out-Null
$sb.AppendLine("        JsonObject badKey = new JsonObject();") | Out-Null
$sb.AppendLine("        JsonObject badKeyVal = new JsonObject();") | Out-Null
$sb.AppendLine("        badKeyVal.addProperty(""item"", ""minecraft:dirt"");") | Out-Null
$sb.AppendLine("        badKey.add(""#"", badKeyVal);") | Out-Null
$sb.AppendLine("        m10.add(""key"", badKey);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m10, ""/data/forbidden/recipe/emerald.json""), ""altered key"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 11: Alter ingredients on shapeless recipe") | Out-Null
$sb.AppendLine("        JsonObject m11 = getJson(""/data/forbidden/recipe/crystalwell.json"");") | Out-Null
$sb.AppendLine("        JsonArray badIngrs = m11.getAsJsonArray(""ingredients"").deepCopy();") | Out-Null
$sb.AppendLine("        badIngrs.remove(0);") | Out-Null
$sb.AppendLine("        m11.add(""ingredients"", badIngrs);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m11, ""/data/forbidden/recipe/crystalwell.json""), ""altered ingredients"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 12: Alter mana on botania mana infusion") | Out-Null
$sb.AppendLine("        JsonObject m12 = getJson(""/packs/botania/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json"");") | Out-Null
$sb.AppendLine("        m12.addProperty(""mana"", 9999);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m12, ""/packs/botania/data/forbidden/recipe/mana_infusion_wand_rod_livingwood.json""), ""altered mana"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 13: Alter altarSyphon on blood magic altar") | Out-Null
$sb.AppendLine("        JsonObject m13 = getJson(""/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json"");") | Out-Null
$sb.AppendLine("        m13.addProperty(""altarSyphon"", 14999);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m13, ""/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json""), ""altered altarSyphon"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S04 Mutation 14: Alter upgradeLevel on blood magic altar") | Out-Null
$sb.AppendLine("        JsonObject m14 = getJson(""/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json"");") | Out-Null
$sb.AppendLine("        m14.addProperty(""upgradeLevel"", 2);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateRecipeJson(m14, ""/packs/bloodmagic/data/forbidden/recipe/bloodthorn.json""), ""altered upgradeLevel"");") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void testNegativeResearchRegressions() {") | Out-Null
$sb.AppendLine("        String target = ""/data/forbidden/thaumcraft/research/wrathcage.json"";") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 1: Mutate coordinates") | Out-Null
$sb.AppendLine("        JsonObject m1 = getJson(target);") | Out-Null
$sb.AppendLine("        m1.addProperty(""column"", 0);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m1, target), ""mutated column coordinate"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 2: Mutate parents") | Out-Null
$sb.AppendLine("        JsonObject m2 = getJson(target);") | Out-Null
$sb.AppendLine("        m2.add(""parents"", new JsonArray());") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m2, target), ""mutated parents"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 3: Mutate hidden parents") | Out-Null
$sb.AppendLine("        JsonObject m3 = getJson(target);") | Out-Null
$sb.AppendLine("        m3.add(""hidden_parents"", new JsonArray());") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m3, target), ""mutated hidden parents"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 4: Mutate complexity") | Out-Null
$sb.AppendLine("        JsonObject m4 = getJson(target);") | Out-Null
$sb.AppendLine("        m4.addProperty(""complexity"", 1);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m4, target), ""mutated complexity"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 5: Mutate warp") | Out-Null
$sb.AppendLine("        JsonObject m5 = getJson(target);") | Out-Null
$sb.AppendLine("        m5.addProperty(""warp"", 0);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m5, target), ""mutated warp"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 6: Mutate recipe pages") | Out-Null
$sb.AppendLine("        JsonObject m6 = getJson(target);") | Out-Null
$sb.AppendLine("        m6.add(""recipe_pages"", new JsonArray());") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m6, target), ""mutated recipe pages"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 7: Mutate research aspects") | Out-Null
$sb.AppendLine("        JsonObject m7 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonObject badAsp = m7.getAsJsonObject(""aspects"").deepCopy();") | Out-Null
$sb.AppendLine("        badAsp.addProperty(""ira"", 999);") | Out-Null
$sb.AppendLine("        m7.add(""aspects"", badAsp);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m7, target), ""mutated research aspects"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // Mutation 8: Missing namespace") | Out-Null
$sb.AppendLine("        JsonObject m8 = getJson(target);") | Out-Null
$sb.AppendLine("        m8.addProperty(""key"", ""unnamespaced_key"");") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m8, target), ""unnamespaced key"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S05 Mutation 9: Mutate text_pages contents") | Out-Null
$sb.AppendLine("        JsonObject m9 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonArray badTextPages = new JsonArray();") | Out-Null
$sb.AppendLine("        badTextPages.add(""forbidden.research_page.WRATHCAGE.FAKE"");") | Out-Null
$sb.AppendLine("        m9.add(""text_pages"", badTextPages);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m9, target), ""mutated text pages"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S05 Mutation 10: Mutate page_order sequence") | Out-Null
$sb.AppendLine("        JsonObject m10 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonArray badPageOrder = new JsonArray();") | Out-Null
$sb.AppendLine("        badPageOrder.add(""WrathCage"");") | Out-Null
$sb.AppendLine("        badPageOrder.add(""forbidden.research_page.WRATHCAGE.1"");") | Out-Null
$sb.AppendLine("        m10.add(""page_order"", badPageOrder);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m10, target), ""mutated page order"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S05 Mutation 11: Mutate concealed flag") | Out-Null
$sb.AppendLine("        JsonObject m11 = getJson(""/data/forbidden/thaumcraft/research/arcanecake.json"");") | Out-Null
$sb.AppendLine("        m11.addProperty(""concealed"", false);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m11, ""/data/forbidden/thaumcraft/research/arcanecake.json""), ""mutated concealed flag"");") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("        // S05 Mutation 12: Mutate siblings") | Out-Null
$sb.AppendLine("        JsonObject m12 = getJson(target);") | Out-Null
$sb.AppendLine("        JsonArray badSiblings = new JsonArray();") | Out-Null
$sb.AppendLine("        badSiblings.add(""forbidden:fake_sibling"");") | Out-Null
$sb.AppendLine("        m12.add(""siblings"", badSiblings);") | Out-Null
$sb.AppendLine("        assertThrows(() -> validateResearchJson(m12, target), ""mutated siblings"");") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private static void assertThrows(Runnable runnable, String description) {") | Out-Null
$sb.AppendLine("        boolean caught = false;") | Out-Null
$sb.AppendLine("        try {") | Out-Null
$sb.AppendLine("            runnable.run();") | Out-Null
$sb.AppendLine("        } catch (AssertionError e) {") | Out-Null
$sb.AppendLine("            caught = true;") | Out-Null
$sb.AppendLine("        }") | Out-Null
$sb.AppendLine("        check(caught, ""mutation correctly detected and rejected: "" + description);") | Out-Null
$sb.AppendLine("    }") | Out-Null
$sb.AppendLine("") | Out-Null
$sb.AppendLine("    private SemanticDataCheck() {}") | Out-Null
$sb.AppendLine("}") | Out-Null

[System.IO.File]::WriteAllText($TargetJava, $sb.ToString(), [System.Text.UTF8Encoding]::new($false))
Write-Host "[+] Generated $TargetJava successfully ($($recipeFiles.Count) recipes, $($researchFiles.Count) researches)" -ForegroundColor Green
