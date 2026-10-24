package forbidden.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Loaded before registrations; changes require a full restart (D5). */
public final class ForbiddenConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue NO_LUST, SILVERFISH_EMERALDS, TAG_RESEARCH,
        WRATH_CAGE, SPORK, GREEDY, EMERALD_TRANSMUTATION, CRIES_HAVOC, ENCHANTING,
        CROSS_MOD, CROSS_WAND, BOTANIA, BLOOD_MAGIC, ARS_MAGICA, TINKERER, TINKERS_CONSTRUCT,
        SPECIAL_MOBS, TWILIGHT_FOREST, EE3;
    public static final ModConfigSpec.IntValue GLUTTONY, WRATH_COST, WRATH_EFFICIENCY, BLOOD_VIS,
        MANA_VIS, HELLFIRE_UPGRADE, PANDEMONIUM_UPGRADE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GARBAGE_TAGS;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("general");
        NO_LUST = b.define("noLust", false);
        SILVERFISH_EMERALDS = b.define("silverfishDropEmeraldNuggets", true);
        TAG_RESEARCH = b.define("tagResearchItems", true);
        WRATH_CAGE = b.define("wrathCageEnabled", true);
        GREEDY = b.define("capitalistEnchantment", true);
        EMERALD_TRANSMUTATION = b.define("emeraldTransmutation", true);
        CRIES_HAVOC = b.define("wrathCageCriesHavoc", false);
        ENCHANTING = b.define("enchantmentsEnabled", true);
        GLUTTONY = b.defineInRange("gluttony", 0, 0, 2);
        WRATH_COST = b.comment("Legacy default 5; values above 64 can prevent fueling. Zero means free cycles.")
            .defineInRange("wrathCageFuelCost", 5, Integer.MIN_VALUE, Integer.MAX_VALUE);
        WRATH_EFFICIENCY = b.defineInRange("wrathCageFuelEfficiency", 4, 0, Integer.MAX_VALUE);
        GARBAGE_TAGS = b.defineListAllowEmpty("garbageTags", List.of("c:dirt", "c:sands", "c:gravels", "c:cobblestones", "c:netherracks"),
            x -> x instanceof String s && net.minecraft.resources.ResourceLocation.tryParse(s) != null);
        b.pop().push("silly");
        SPORK = b.define("sporkOfDoom", false);
        b.pop().push("compatibility");
        CROSS_MOD = b.define("crossModInteraction", true);
        CROSS_WAND = b.define("conversionWands", true);
        BOTANIA = b.define("botania", true);
        BLOOD_MAGIC = b.define("bloodMagic", true);
        ARS_MAGICA = b.define("arsMagica2", true);
        TINKERER = b.define("thaumicTinkerer", true);
        TINKERS_CONSTRUCT = b.comment("Legacy unused flag; does not enable invented integrations.").define("tinkersConstruct", true);
        SPECIAL_MOBS = b.define("specialMobs", true);
        TWILIGHT_FOREST = b.define("twilightForest", true);
        EE3 = b.define("equivalentExchange3", true);
        b.pop().push("powerConverters");
        BLOOD_VIS = b.defineInRange("lpPerCentivis", 5, Integer.MIN_VALUE, Integer.MAX_VALUE);
        MANA_VIS = b.defineInRange("manaPerCentivis", 8, Integer.MIN_VALUE, Integer.MAX_VALUE);
        b.pop().push("focusUpgrades");
        HELLFIRE_UPGRADE = b.defineInRange("hellfire", 42, 0, 32767);
        PANDEMONIUM_UPGRADE = b.defineInRange("pandemonium", 43, 0, 32767);
        b.pop();
        SPEC = b.build();
    }
    public static ContentGates gates() {
        return new ContentGates(NO_LUST.get(), WRATH_CAGE.get(), ENCHANTING.get(), GREEDY.get(),
            EMERALD_TRANSMUTATION.get(), GLUTTONY.get(), CROSS_MOD.get(), CROSS_WAND.get());
    }
    private ForbiddenConfig() { }
}
