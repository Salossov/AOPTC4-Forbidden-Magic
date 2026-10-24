package forbidden.compat.bloodmagic;

import forbidden.compat.bloodmagic.events.BloodMagicEvents;
import forbidden.compat.bloodmagic.wands.BloodStaffUpdate;
import forbidden.compat.bloodmagic.wands.BloodWandUpdate;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * Top-level coordinator for Blood Magic integration (Tasks C05–C08).
 * Provides ClassLoader-safe boundaries and ensures no wayoftime.bloodmagic.* classes
 * are resolved unless Blood Magic is actively present.
 */
public final class BloodMagicCompat {
    public static final String MODID = "bloodmagic";
    public static final String MOD_ID = MODID;
    private static boolean forceLoadedForTesting = false;
    private static boolean wandPartsRegistered = false;

    public static void setForceLoadedForTesting(boolean force) {
        forceLoadedForTesting = force;
    }

    public static boolean isAvailable() {
        try {
            boolean loaded = forceLoadedForTesting || (ModList.get() != null && ModList.get().isLoaded(MODID));
            return loaded && ForbiddenConfig.CROSS_MOD.get() && ForbiddenConfig.BLOOD_MAGIC.get();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isLoaded() {
        return isAvailable();
    }

    public static void register(IEventBus bus) {
        BloodMagicItems.register(bus);
    }

    public static synchronized void registerWandParts() {
        if (wandPartsRegistered) return;
        wandPartsRegistered = true;

        // C06: Blood Rod (cap 100, cost 12, BloodWandUpdate)
        var blood = new WandRod(
                ResourceLocation.fromNamespaceAndPath("forbidden", "blood"),
                "forbidden:rod_blood",
                100,
                () -> new ItemStack(ForbiddenContent.item("wand_rod_blood")),
                12,
                BloodWandUpdate.INSTANCE
        );
        WandRod.rods.putIfAbsent("blood", blood);

        // C06: Blood Staff (cap 250, cost 25, BloodStaffUpdate)
        var bloodStaff = new StaffRod(
                ResourceLocation.fromNamespaceAndPath("forbidden", "blood"),
                "forbidden:rod_blood_staff",
                250,
                () -> new ItemStack(ForbiddenContent.item("wand_rod_blood_staff")),
                25,
                BloodStaffUpdate.INSTANCE
        );
        WandRod.rods.putIfAbsent("blood_staff", bloodStaff);

        // C06: Alchemical Cap (discount 15% -> 0.85f base modifier, cost 7)
        var alchemical = new WandCap(
                ResourceLocation.fromNamespaceAndPath("forbidden", "alchemical"),
                "forbidden:cap_alchemical",
                0.85f,
                Set.of(),
                0.0f,
                () -> new ItemStack(ForbiddenContent.item("wand_cap_alchemical")),
                7
        );
        WandCap.caps.putIfAbsent("alchemical", alchemical);
    }

    public static void commonSetup() {
        registerWandParts();
        NeoForge.EVENT_BUS.register(BloodMagicEvents.class);

        if (isLoaded()) {
            try {
                BloodMagicBridge.registerRituals();
            } catch (Throwable ignored) {}
        }
    }

    public static void addNearestPlayerLP(Level level, BlockPos pos, double radius, int amount) {
        if (isLoaded()) {
            BloodMagicBridge.addNearestPlayerLP(level, pos, radius, amount);
        }
    }

    public static boolean syphonFromPlayer(Player player, int amount) {
        return isLoaded() && BloodMagicBridge.syphonFromPlayerNetwork(player, amount);
    }

    private BloodMagicCompat() {}
}
