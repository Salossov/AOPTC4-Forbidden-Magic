package forbidden.verification;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import forbidden.content.focus.*;
import forbidden.content.wands.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.*;

public final class P02WandCheck {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + description);
        }
    }

    public static void main(String[] args) throws Exception {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        for (var field : ForbiddenConfig.class.getFields()) {
            if (field.get(null) instanceof ModConfigSpec.ConfigValue<?> value) {
                var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
                cache.setAccessible(true);
                cache.set(value, value.getDefault());
            }
        }

        var bus = BusBuilder.builder().build();
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);

        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        for (var registry : List.of(BuiltInRegistries.DATA_COMPONENT_TYPE, BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM, BuiltInRegistries.BLOCK_ENTITY_TYPE, BuiltInRegistries.CREATIVE_MODE_TAB)) {
            ((MappedRegistry<?>) registry).unfreeze();
            bus.post((RegisterEvent) constructor.newInstance(registry.key(), registry));
            registry.freeze();
        }

        ForbiddenAspects.register();
        ForbiddenWandMechanics.registerAll();
        ForbiddenWandVisuals.register();

        // B18: Tainted wand
        var taintedRod = WandRod.rods.get("forbidden:tainted");
        check(taintedRod != null, "forbidden:tainted rod registered");
        check(taintedRod.getCapacity() == 150, "tainted rod capacity 150");
        check(taintedRod.getCraftCost() == 12, "tainted rod cost 12");
        check(TaintedWandUpdate.INSTANCE != null, "TaintedWandUpdate singleton");

        // B18: Infernal wand
        var infernalRod = WandRod.rods.get("forbidden:infernal");
        check(infernalRod != null, "forbidden:infernal rod registered");
        check(infernalRod.getCapacity() == 150, "infernal rod capacity 150");
        check(infernalRod.getCraftCost() == 12, "infernal rod cost 12");
        check(InfernalWandUpdate.INSTANCE != null, "InfernalWandUpdate singleton");

        // B19: Profane wand
        var profaneRod = WandRod.rods.get("forbidden:profane");
        check(profaneRod != null, "forbidden:profane rod registered");
        check(profaneRod.getCapacity() == 50, "profane rod capacity 50");
        check(profaneRod.getCraftCost() == 12, "profane rod cost 12");
        var profanedRod = WandRod.rods.get("forbidden:profaned");
        check(profanedRod != null, "forbidden:profaned rod registered");
        check(profanedRod.getCapacity() == 50, "profaned rod capacity 50");
        check(profanedRod.getCraftCost() == 1000, "profaned rod cost 1000");
        check(ProfaneWandUpdate.CONTRACT_TOTAL == 25000, "profane contract total is 25000");

        // B20: Creative wand and staff
        var neutroniumRod = WandRod.rods.get("forbidden:neutronium");
        check(neutroniumRod != null, "forbidden:neutronium rod registered");
        check(neutroniumRod.getCapacity() == 9001, "neutronium rod capacity 9001");
        check(neutroniumRod.isGlowing(), "neutronium rod glowing is true");
        var neutroniumStaff = WandRod.rods.get("forbidden:neutronium_staff");
        check(neutroniumStaff != null, "forbidden:neutronium_staff rod registered");
        check(neutroniumStaff.getCapacity() == 9002, "neutronium staff capacity 9002");
        check(neutroniumStaff.isGlowing(), "neutronium staff glowing is true");

        // B20: Orichalcum cap
        var orichalcumCap = WandCap.caps.get("forbidden:orichalcum");
        check(orichalcumCap != null, "forbidden:orichalcum cap registered");
        check(orichalcumCap.getBaseCostModifier() == 0.0f, "orichalcum cost modifier 0.0");
        check(orichalcumCap.getCraftCost() == 1000, "orichalcum craft cost 1000");
        check(WandCap.caps.containsKey("orichalcum"), "orichalcum cap alias registered");

        // Visuals check
        for (var model : WandVisuals.models()) {
            check(P02WandCheck.class.getResource("/assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json") != null,
                    "registered composite model exists: " + model);
        }

        // B21: Blink Focus
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:blink_focus")), "blink_focus item registered");
        var blinkItem = (BlinkFocusItem) BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:blink_focus"));
        check(blinkItem != null, "blink focus instance");
        check(blinkItem.getFocusColor(ItemStack.EMPTY) == 9474192, "blink focus color 9474192");

        // Upgrades 42 and 43
        check(FocusUpgradeType.get(42) != null, "upgrade 42 registered");
        check(FocusUpgradeType.get(42).key().equals("forbidden:hellfire"), "upgrade 42 is hellfire");
        check(FocusUpgradeType.get(43) != null, "upgrade 43 registered");
        check(FocusUpgradeType.get(43).key().equals("forbidden:pandemonium"), "upgrade 43 is pandemonium");

        // Blink focus behavior
        var behaviorReg = FocusBehaviors.get(new ItemStack(blinkItem));
        check(behaviorReg != null, "blink focus behavior registered in FocusBehaviors");
        check(behaviorReg.behavior().managesVis(), "blink focus managesVis is true (delayed payment)");

        // Costs
        var focusStack = new ItemStack(blinkItem);
        var baseCost = blinkItem.getVisCost(focusStack);
        check(baseCost.get(Aspect.getAspect("perditio")) == 300 && baseCost.size() == 1, "blink base cost: 300 perditio");

        // Upgrades by rank
        var rank1 = blinkItem.getPossibleUpgradesByRank(focusStack, 1);
        check(rank1.contains(FocusUpgradeType.frugal) && rank1.size() == 1, "rank 1 frugal only");
        var rank2 = blinkItem.getPossibleUpgradesByRank(focusStack, 2);
        check(rank2.contains(ForbiddenFoci.HELLFIRE) && rank2.contains(ForbiddenFoci.PANDEMONIUM) && rank2.contains(FocusUpgradeType.frugal), "rank 2 upgrades");

        // Upgrade application
        check(blinkItem.applyUpgrade(focusStack, FocusUpgradeType.frugal, 1), "apply frugal rank 1");
        check(blinkItem.applyUpgrade(focusStack, ForbiddenFoci.HELLFIRE, 2), "apply hellfire rank 2");
        var hellfireCost = blinkItem.getVisCost(focusStack);
        check(hellfireCost.get(Aspect.getAspect("perditio")) == 300 && hellfireCost.get(Aspect.getAspect("ignis")) == 100, "hellfire cost: 300 perditio + 100 ignis");

        var rank3Hellfire = blinkItem.getPossibleUpgradesByRank(focusStack, 3);
        check(rank3Hellfire.contains(FocusUpgradeType.potency) && rank3Hellfire.contains(FocusUpgradeType.frugal) && rank3Hellfire.contains(FocusUpgradeType.enlarge), "rank 3 hellfire upgrades include potency/frugal/enlarge");

        // Pandemonium variant
        var pandemoniumStack = new ItemStack(blinkItem);
        check(pandemoniumStack.getItem() instanceof ItemFocusBasic, "item focus basic");
        check(blinkItem.applyUpgrade(pandemoniumStack, FocusUpgradeType.frugal, 1), "apply frugal rank 1");
        check(blinkItem.applyUpgrade(pandemoniumStack, ForbiddenFoci.PANDEMONIUM, 2), "apply pandemonium rank 2");
        var pandemoniumCost = blinkItem.getVisCost(pandemoniumStack);
        check(pandemoniumCost.get(Aspect.getAspect("perditio")) == 300 && pandemoniumCost.get(Aspect.getAspect("ordo")) == 100, "pandemonium cost: 300 perditio + 100 ordo");

        System.out.println("FORBIDDEN_P02_WAND_PASS checks=" + checks);
    }
}
