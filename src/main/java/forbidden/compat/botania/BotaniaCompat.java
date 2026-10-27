package forbidden.compat.botania;

import forbidden.compat.botania.wands.YandereWandUpdate;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * Top-level coordinator for Botania integration (Tasks C01–C04).
 * Provides ClassLoader-safe boundaries and ensures no vazkii.botania.* classes
 * are resolved unless Botania is actively present.
 */
public final class BotaniaCompat {
    public static final String MODID = "botania";
    public static final String MOD_ID = MODID;
    private static boolean forceLoadedForTesting = false;
    private static boolean wandPartsRegistered = false;

    public static void setForceLoadedForTesting(boolean force) {
        forceLoadedForTesting = force;
    }

    public static boolean isAvailable() {
        try {
            boolean loaded = forceLoadedForTesting || (ModList.get() != null && ModList.get().isLoaded(MODID));
            return loaded && ForbiddenConfig.CROSS_MOD.get() && ForbiddenConfig.BOTANIA.get();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isLoaded() {
        return isAvailable();
    }

    public static void register(IEventBus bus) {
        BotaniaItems.register(bus);
    }

    public static synchronized void registerWandParts() {
        if (wandPartsRegistered) return;
        wandPartsRegistered = true;

        // C02: Livingwood Rod (cap 100, cost 12, YandereWandUpdate)
        var livingwood = new WandRod(
                ResourceLocation.fromNamespaceAndPath("forbidden", "livingwood"),
                "forbidden:rod_livingwood",
                100,
                () -> new ItemStack(ForbiddenContent.item("wand_rod_livingwood")),
                12,
                YandereWandUpdate.INSTANCE
        );
        WandRod.rods.putIfAbsent("livingwood", livingwood);

        // C02: Dreamwood Rod (cap 100, cost 12, YandereWandUpdate)
        var dreamwood = new WandRod(
                ResourceLocation.fromNamespaceAndPath("forbidden", "dreamwood"),
                "forbidden:rod_dreamwood",
                100,
                () -> new ItemStack(ForbiddenContent.item("wand_rod_dreamwood")),
                12,
                YandereWandUpdate.INSTANCE
        );
        WandRod.rods.putIfAbsent("dreamwood", dreamwood);

        // C02: Dreamwood Staff (cap 250, cost 25, YandereWandUpdate)
        var dreamwoodStaff = new StaffRod(
                ResourceLocation.fromNamespaceAndPath("forbidden", "dreamwood"),
                "forbidden:rod_dreamwood_staff",
                250,
                () -> new ItemStack(ForbiddenContent.item("wand_rod_dreamwood_staff")),
                25,
                YandereWandUpdate.INSTANCE
        );
        dreamwoodStaff.setRunes(true);
        WandRod.rods.putIfAbsent("dreamwood_staff", dreamwoodStaff);

        // C02: Manasteel Cap (discount 10% -> 0.90f base modifier, cost 6, research "forbidden:cap_manasteel")
        var manasteel = new WandCap(
                ResourceLocation.fromNamespaceAndPath("forbidden", "manasteel"),
                "forbidden:cap_manasteel",
                0.9f,
                Set.of(),
                0.0f,
                () -> new ItemStack(ForbiddenContent.item("wand_cap_manasteel")),
                6
        );
        WandCap.caps.putIfAbsent("manasteel", manasteel);

        // C02: Elementium Cap (discount 20% -> 0.80f base modifier, cost 9, research "forbidden:cap_elementium")
        var elementium = new WandCap(
                ResourceLocation.fromNamespaceAndPath("forbidden", "elementium"),
                "forbidden:cap_elementium",
                0.8f,
                Set.of(),
                0.0f,
                () -> new ItemStack(ForbiddenContent.item("wand_cap_elementium")),
                9
        );
        WandCap.caps.putIfAbsent("elementium", elementium);

        // C02: Terrasteel Cap (1.8f base modifier, cost 1, research "forbidden:cap_terrasteel")
        var terrasteel = new WandCap(
                ResourceLocation.fromNamespaceAndPath("forbidden", "terrasteel"),
                "forbidden:cap_terrasteel",
                1.8f,
                Set.of(),
                0.0f,
                () -> new ItemStack(ForbiddenContent.item("wand_cap_terrasteel")),
                1
        );
        WandCap.caps.putIfAbsent("terrasteel", terrasteel);
    }

    public static void commonSetup() {
        registerWandParts();
    }

    public static boolean requestManaExact(ItemStack stack, Player player, int mana) {
        if (!isLoaded()) return false;
        try {
            return BotaniaBridge.requestManaExactForTool(stack, player, mana);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int drawMana(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos boundPos, int needed) {
        if (!isLoaded()) return 0;
        try {
            return BotaniaBridge.drawManaFromPool(level, pos, boundPos, needed);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int emitMana(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos boundPos, int available) {
        if (!isLoaded()) return 0;
        try {
            return BotaniaBridge.emptyManaIntoCollector(level, pos, boundPos, available);
        } catch (Throwable t) {
            return 0;
        }
    }

    private BotaniaCompat() {}
}
