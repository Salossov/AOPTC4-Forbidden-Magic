package forbidden.compat.tinkerer;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry of the 8 Forbidden Magic enchantments and their approved Osmotic Enchanter aspect costs (Contract T01).
 */
public final class TinkererEnchantmentRegistry {
    public static final OsmoticCostEntry WRATH = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:wrath"),
            5,
            Map.of("perditio", 6, "ignis", 6),
            "forbidden:textures/misc/wrath.png",
            "WRATH"
    );

    public static final OsmoticCostEntry CLUSTER = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:cluster"),
            4,
            Map.of("ignis", 5, "terra", 5, "ordo", 5),
            "forbidden:textures/misc/lucrative.png",
            "CLUSTER"
    );

    public static final OsmoticCostEntry GREEDY = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:greedy"),
            1,
            Map.of("perditio", 50, "terra", 30, "ordo", 20),
            "forbidden:textures/misc/greedy.png",
            "GREEDY"
    );

    public static final OsmoticCostEntry EDUCATIONAL = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:educational"),
            5,
            Map.of("aer", 10, "ignis", 10, "aqua", 10, "terra", 10, "ordo", 15, "perditio", 15),
            "forbidden:textures/misc/educational.png",
            "EDUCATIONAL"
    );

    public static final OsmoticCostEntry CONSUMING = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:consuming"),
            1,
            Map.of("perditio", 10),
            "forbidden:textures/misc/consuming.png",
            "CONSUMING"
    );

    public static final OsmoticCostEntry CORRUPTING = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:corrupting"),
            1,
            Map.of("perditio", 20, "ignis", 15, "terra", 10),
            "forbidden:textures/misc/corrupting.png",
            "CORRUPTING"
    );

    public static final OsmoticCostEntry VOIDTOUCHED = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:voidtouched"),
            1,
            Map.of("perditio", 70),
            "forbidden:textures/misc/voidtouched.png",
            "VOIDTOUCHED"
    );

    public static final OsmoticCostEntry IMPACT = new OsmoticCostEntry(
            ResourceLocation.parse("forbidden:impact"),
            1,
            Map.of("perditio", 30, "terra", 30),
            "forbidden:textures/misc/impact.png",
            "IMPACT"
    );

    public static final List<OsmoticCostEntry> ENTRIES = List.of(
            WRATH,
            CLUSTER,
            GREEDY,
            EDUCATIONAL,
            CONSUMING,
            CORRUPTING,
            VOIDTOUCHED,
            IMPACT
    );

    private static final Map<ResourceLocation, OsmoticCostEntry> BY_ID;

    static {
        var map = new LinkedHashMap<ResourceLocation, OsmoticCostEntry>();
        for (var entry : ENTRIES) {
            map.put(entry.id(), entry);
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    public static List<OsmoticCostEntry> getAll() {
        return ENTRIES;
    }

    public static OsmoticCostEntry find(ResourceLocation id) {
        if (id == null) return null;
        return BY_ID.get(id);
    }

    public static OsmoticCostEntry find(String id) {
        if (id == null) return null;
        return find(ResourceLocation.tryParse(id));
    }

    public static boolean contains(ResourceLocation id) {
        return id != null && BY_ID.containsKey(id);
    }

    private TinkererEnchantmentRegistry() {}
}
