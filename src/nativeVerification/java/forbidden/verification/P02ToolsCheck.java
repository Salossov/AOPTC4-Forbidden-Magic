package forbidden.verification;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenTools;
import forbidden.content.baubles.RingNutritionItem;
import forbidden.content.baubles.SubCollarItem;
import forbidden.content.scribes.CrystalwellItem;
import forbidden.content.scribes.PrimewellItem;
import forbidden.content.tools.*;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.IWarpingGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class P02ToolsCheck {
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
        ForbiddenTools.register(bus);

        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        for (var registry : List.of(BuiltInRegistries.DATA_COMPONENT_TYPE, BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM, BuiltInRegistries.BLOCK_ENTITY_TYPE, BuiltInRegistries.CREATIVE_MODE_TAB)) {
            ((MappedRegistry<?>) registry).unfreeze();
            bus.post((RegisterEvent) constructor.newInstance(registry.key(), registry));
            registry.freeze();
        }

        // 1. Check all tool registrations
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:taint_shovel")), "taint_shovel registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:taint_pickaxe")), "taint_pickaxe registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:skull_axe")), "skull_axe registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:morph_pickaxe")), "morph_pickaxe registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:morph_sword")), "morph_sword registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:morph_shovel")), "morph_shovel registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:morph_axe")), "morph_axe registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:diabolist_fork")), "diabolist_fork registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:riding_crop")), "riding_crop registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:crystalwell")), "crystalwell registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:primewell")), "primewell registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:ring_nutrition")), "ring_nutrition registered");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:sub_collar")), "sub_collar registered");

        // 2. B11: TaintShovel
        var taintShovel = ForbiddenTools.TAINT_SHOVEL.get();
        check(taintShovel instanceof IRepairable, "taint shovel is repairable");
        check(taintShovel.isValidRepairItem(new ItemStack(taintShovel), new ItemStack(Items.IRON_INGOT)) == false, "not iron repairable");

        // 3. B12: TaintPickaxe speed calculation formula (4 tiers)
        check(TaintPickaxeItem.calculateTaintPickSpeed(0.0F) == 0.0F, "tier 0: hardness 0 -> speed 0");
        check(TaintPickaxeItem.calculateTaintPickSpeed(2.0F) == 0.1F, "tier 1: hardness < 5 -> speed 0.1");
        check(TaintPickaxeItem.calculateTaintPickSpeed(4.99F) == 0.1F, "tier 1: hardness < 5 -> speed 0.1");
        check(TaintPickaxeItem.calculateTaintPickSpeed(10.0F) == 5.0F, "tier 2: hardness 10 -> speed 5 (hardness/2)");
        check(TaintPickaxeItem.calculateTaintPickSpeed(18.0F) == 9.0F, "tier 2: hardness 18 -> speed 9 (hardness/2)");
        check(TaintPickaxeItem.calculateTaintPickSpeed(20.0F) == 25.0F, "tier 3: hardness 20 -> speed 25 (5+hardness)");
        check(TaintPickaxeItem.calculateTaintPickSpeed(50.0F) == 55.0F, "tier 3: hardness 50 -> speed 55 (5+hardness)");

        // 4. B13: SkullAxe
        var skullAxe = ForbiddenTools.SKULL_AXE.get();
        check(skullAxe instanceof IRepairable, "skull axe is repairable");
        check(new ItemStack(skullAxe).getRarity() == Rarity.UNCOMMON, "skull axe rarity is uncommon");

        // 5. B14: Morph tools
        var morphPick = ForbiddenTools.MORPH_PICKAXE.get();
        var morphAxe = ForbiddenTools.MORPH_AXE.get();
        var morphShovel = ForbiddenTools.MORPH_SHOVEL.get();
        var morphSword = ForbiddenTools.MORPH_SWORD.get();
        check(morphPick instanceof IWarpingGear && ((IWarpingGear) morphPick).getWarp(new ItemStack(morphPick), null) == 0, "morph pick base warp is 0");
        check(morphAxe instanceof IWarpingGear && ((IWarpingGear) morphAxe).getWarp(new ItemStack(morphAxe), null) == 0, "morph axe base warp is 0");
        check(morphShovel instanceof IWarpingGear && ((IWarpingGear) morphShovel).getWarp(new ItemStack(morphShovel), null) == 0, "morph shovel base warp is 0");
        check(morphSword instanceof IWarpingGear && ((IWarpingGear) morphSword).getWarp(new ItemStack(morphSword), null) == 0, "morph sword base warp is 0");

        // 6. B15: DiabolistFork
        var fork = ForbiddenTools.DIABOLIST_FORK.get();
        check(fork.isValidRepairItem(new ItemStack(fork), new ItemStack(Items.NETHER_BRICK)), "fork repaired with nether brick");

        // 7. B16: RidingCrop
        var crop = ForbiddenTools.RIDING_CROP.get();
        check(crop.isValidRepairItem(new ItemStack(crop), new ItemStack(Items.LEATHER)), "crop repaired with leather");

        // 8. B23: Crystalwell
        var crystalwell = ForbiddenTools.CRYSTALWELL.get();
        check(crystalwell instanceof IScribeTools, "crystalwell is scribe tool");
        check(crystalwell.getMaxDamage(new ItemStack(crystalwell)) == 100, "crystalwell normal mode max damage is 100");

        // 9. B24: Primewell
        var primewell = ForbiddenTools.PRIMEWELL.get();
        check(primewell instanceof IScribeTools, "primewell is scribe tool");
        check(new ItemStack(primewell).getRarity() == Rarity.EPIC, "primewell rarity is epic");
        ItemStack primeStack = new ItemStack(primewell);
        check(primeStack.getMaxDamage() == 100, "primewell max damage is 100 for usableInk");
        primeStack.setDamageValue(10);
        check(primeStack.getDamageValue() == 0, "primewell damage is locked to 0");

        // 10. B25: RingNutrition
        var ring = ForbiddenTools.RING_NUTRITION.get();
        check(ring instanceof ICurioItem, "ring is curio item");
        check(ring instanceof IRunicArmor && ((IRunicArmor) ring).getRunicCharge(new ItemStack(ring)) == 0, "ring runic charge is 0");

        // 11. B26: SubCollar
        var collar = ForbiddenTools.SUB_COLLAR.get();
        check(collar instanceof ICurioItem, "collar is curio item");
        check(SubCollarItem.maxVis() == 25000, "collar max vis is 25000");

        System.out.println("FORBIDDEN_P02_TOOLS_PASS checks=" + checks);
    }
}
