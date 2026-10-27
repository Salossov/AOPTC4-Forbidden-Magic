package forbidden.content.wands;

import forbidden.content.ForbiddenContent;
import forbidden.content.focus.BlinkFocusItem;
import forbidden.content.focus.ForbiddenFoci;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * P02 Wands, Foci & Triggers registration and orchestration.
 */
public final class ForbiddenWandMechanics {
    private static boolean registered;

    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;
        registerRods();
        registerCaps();
        registerFoci();
        registerEvents();
    }

    public static void registerRods() {
        rod("tainted", 150, 12, () -> new ItemStack(ForbiddenContent.item("wand_rod_tainted")), TaintedWandUpdate.INSTANCE);
        rod("infernal", 150, 12, () -> new ItemStack(ForbiddenContent.item("wand_rod_infernal")), InfernalWandUpdate.INSTANCE);
        // Release registration shares WandCores:11 with dreamwood and has no updater.
        rod("equivalent", 100, 12, () -> new ItemStack(ForbiddenContent.item("wand_rod_dreamwood")), null);
        rod("profane", 50, 12, () -> new ItemStack(ForbiddenContent.item("wand_rod_profane")), ProfaneWandUpdate.INSTANCE);
        rod("profaned", 50, 1000, () -> new ItemStack(Items.BEDROCK), null);
        var neutronium = rod("neutronium", 9001, 1000, () -> new ItemStack(Items.BEDROCK), CreativeWandUpdate.INSTANCE);
        neutronium.setGlowing(true);
        var neutroniumStaff = new StaffRod("forbidden:neutronium", 9002, () -> new ItemStack(Items.BEDROCK), 1000, CreativeWandUpdate.INSTANCE);
        neutroniumStaff.setGlowing(true);
    }

    public static void registerCaps() {
        var orichalcum = new WandCap(
                ResourceLocation.fromNamespaceAndPath("forbidden", "orichalcum"),
                "CAP_orichalcum",
                0.0f,
                Set.of(),
                0.0f,
                () -> new ItemStack(Items.COMMAND_BLOCK),
                1000
        );
        WandCap.caps.putIfAbsent("orichalcum", orichalcum);
    }

    public static void registerFoci() {
        ForbiddenFoci.registerUpgrades();
        var blinkItem = ForbiddenContent.BLINK_FOCUS != null ? ForbiddenContent.BLINK_FOCUS.get() : null;
        if (blinkItem instanceof BlinkFocusItem focus) {
            ForbiddenFoci.registerBehavior(focus);
        }
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.register(ForbiddenWandEvents.class);
    }

    private static WandRod rod(String name, int capacity, int cost, Supplier<ItemStack> item, BiConsumer<ItemStack, Player> onUpdate) {
        return new WandRod(ResourceLocation.fromNamespaceAndPath("forbidden", name), "forbidden:rod_" + name, capacity, item, cost, onUpdate);
    }

    private ForbiddenWandMechanics() {}
}
