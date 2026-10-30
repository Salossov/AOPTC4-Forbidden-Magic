package forbidden.compat.viewers;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Compatibility handler for Recipe Viewers (JEI / EMI / AOP:TC4 Compact - Task C16).
 * Fulfills Decision D14: integrates with the shared 'thaumcraft_viewers' module,
 * exposing Forbidden Magic recipes to existing Thaumcraft recipe categories without duplicating categories.
 */
public final class ViewersCompat {
    public static final String VIEWERS_MODID = "thaumcraft_compact";
    public static final String JEI_MODID = "jei";
    public static final String EMI_MODID = "emi";

    /**
     * Standard Thaumcraft recipe categories supported by the shared viewer bridge.
     */
    public static final ResourceLocation CATEGORY_ARCANE = ResourceLocation.parse("thaumcraft:arcane");
    public static final ResourceLocation CATEGORY_ARCANE_SHAPELESS = ResourceLocation.parse("thaumcraft:arcane_shapeless");
    public static final ResourceLocation CATEGORY_CRUCIBLE = ResourceLocation.parse("thaumcraft:crucible");
    public static final ResourceLocation CATEGORY_INFUSION = ResourceLocation.parse("thaumcraft:infusion");
    public static final ResourceLocation CATEGORY_INFERNAL = ResourceLocation.parse("thaumcraft:infernal_bonus");

    public static final List<ResourceLocation> SUPPORTED_CATEGORIES = List.of(
            CATEGORY_ARCANE,
            CATEGORY_ARCANE_SHAPELESS,
            CATEGORY_CRUCIBLE,
            CATEGORY_INFUSION,
            CATEGORY_INFERNAL
    );

    public static boolean isAvailable() {
        return isModLoaded(VIEWERS_MODID) || isModLoaded(JEI_MODID) || isModLoaded(EMI_MODID);
    }

    public static boolean isThaumcraftViewersLoaded() {
        return isModLoaded(VIEWERS_MODID);
    }

    public static void init() {
        if (!isAvailable()) {
            return;
        }
        // Bridge registration: recipes with namespace 'forbidden' using standard
        // thaumcraft recipe types are automatically indexed by thaumcraft_viewers.
    }

    public static boolean isSupportedCategory(ResourceLocation categoryId) {
        return categoryId != null && SUPPORTED_CATEGORIES.contains(categoryId);
    }

    private static boolean isModLoaded(String modId) {
        try {
            var list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

    private ViewersCompat() {}
}
