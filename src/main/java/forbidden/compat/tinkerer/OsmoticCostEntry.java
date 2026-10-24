package forbidden.compat.tinkerer;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Contract T01: Represents aspect cost and metadata of a Forbidden Magic enchantment
 * for the Osmotic Enchanter in Thaumic Tinkerer.
 */
public record OsmoticCostEntry(
        ResourceLocation id,
        int legacyId,
        String legacyType,
        int maxLevel,
        Map<String, Integer> baseAspects,
        String texture,
        String research
) {
    public OsmoticCostEntry {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(baseAspects, "baseAspects cannot be null");
        baseAspects = Collections.unmodifiableMap(baseAspects);
        if (texture == null) texture = "";
        if (research == null) research = "";
        if (legacyType == null || legacyType.isBlank()) legacyType = defaultLegacyType(id);
    }

    public OsmoticCostEntry(
            ResourceLocation id,
            int maxLevel,
            Map<String, Integer> baseAspects,
            String texture,
            String research
    ) {
        this(id, 0, defaultLegacyType(id), maxLevel, baseAspects, texture, research);
    }

    private static String defaultLegacyType(ResourceLocation id) {
        if (id == null) return "all";
        String path = id.getPath();
        return switch (path) {
            case "wrath", "educational" -> "weapon";
            case "cluster", "consuming", "corrupting", "impact" -> "digger";
            case "greedy", "voidtouched" -> "breakable";
            default -> "all";
        };
    }

    /**
     * Calculates the AspectList cost for a given level according to the standard
     * Thaumic Tinkerer Osmotic Enchanter formula:
     * cost = floor(baseAmount * (level * (1.0 + level * 0.2)))
     */
    public AspectList cost(int level) {
        AspectList result = new AspectList();
        if (level <= 0) return result;
        for (Map.Entry<String, Integer> entry : baseAspects.entrySet()) {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect != null) {
                int amount = (int) (entry.getValue() * (level * (1.0 + level * 0.2)));
                result.add(aspect, amount);
            }
        }
        return result;
    }

    public boolean hasAspect(String aspectTag) {
        return baseAspects.containsKey(aspectTag);
    }

    public int getBaseCost(String aspectTag) {
        return baseAspects.getOrDefault(aspectTag, 0);
    }
}
