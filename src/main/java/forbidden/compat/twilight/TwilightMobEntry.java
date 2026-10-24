package forbidden.compat.twilight;

import forbidden.content.CageMobCatalog.SpawnCondition;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/**
 * Metadata entry for a Twilight Forest entity in the Wrath Cage catalog.
 */
public record TwilightMobEntry(
        ResourceLocation entityId,
        String defaultAspectTag,
        SpawnCondition spawnCondition,
        boolean conditionalLust,
        String noLustAspectTag
) {
    public TwilightMobEntry {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(defaultAspectTag, "defaultAspectTag cannot be null");
        if (spawnCondition == null) spawnCondition = SpawnCondition.GROUND;
        if (noLustAspectTag == null) noLustAspectTag = defaultAspectTag;
    }

    public TwilightMobEntry(ResourceLocation entityId, String defaultAspectTag, SpawnCondition spawnCondition) {
        this(entityId, defaultAspectTag, spawnCondition, false, defaultAspectTag);
    }

    public String getAspectTag(boolean noLust) {
        if (conditionalLust && noLust) {
            return noLustAspectTag;
        }
        return defaultAspectTag;
    }

    public Aspect getAspect(boolean noLust) {
        return Aspect.getAspect(getAspectTag(noLust));
    }
}
