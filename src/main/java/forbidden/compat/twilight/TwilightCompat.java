package forbidden.compat.twilight;

import forbidden.config.ForbiddenConfig;
import forbidden.content.CageMobCatalog;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import thaumcraft.api.aspects.Aspect;

/**
 * Integration for Twilight Forest (Task C10).
 * Registers the 35 mobs (36 source writes including the No Lust alternative).
 * Zero hard dependency: will never throw NoClassDefFoundError or NoSuchElementException if Twilight Forest is absent.
 */
public final class TwilightCompat {
    public static final String MODID = "twilightforest";

    public static boolean isAvailable() {
        try {
            return ForbiddenConfig.CROSS_MOD.get()
                    && ForbiddenConfig.WRATH_CAGE.get()
                    && ForbiddenConfig.TWILIGHT_FOREST.get()
                    && isModLoaded(MODID);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void init() {
        if (!isAvailable()) return;
        boolean noLust = ForbiddenConfig.NO_LUST.get();
        for (TwilightMobEntry entry : TwilightCageMobRegistry.getAll()) {
            Aspect aspect = entry.getAspect(noLust);
            if (aspect != null) {
                CageMobCatalog.registerMob(entry.entityId(), aspect, entry.spawnCondition());
            }
        }
    }

    public static boolean isTwilightMob(ResourceLocation id) {
        return TwilightCageMobRegistry.isTwilightMob(id);
    }

    public static Aspect getAspect(ResourceLocation id, boolean noLust) {
        return TwilightCageMobRegistry.getAspectForMob(id, noLust);
    }

    private static boolean isModLoaded(String modId) {
        try {
            var list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

    private TwilightCompat() {}
}
