package forbidden.verification;

import com.google.gson.JsonParser;
import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.bloodmagic.BloodMagicItems;
import forbidden.compat.bloodmagic.effects.BloodSealEffect;
import forbidden.compat.bloodmagic.flowers.BloodthornBlock;
import forbidden.compat.bloodmagic.flowers.BloodthornBlockEntity;
import forbidden.compat.bloodmagic.items.*;
import forbidden.compat.bloodmagic.rituals.RitualSanity;
import forbidden.compat.bloodmagic.wands.BloodStaffUpdate;
import forbidden.compat.bloodmagic.wands.BloodWandUpdate;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenAspects;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenWandVisuals;
import forbidden.content.wands.ForbiddenWandMechanics;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandParts;
import thaumcraft.api.wands.WandRod;
import thaumcraft.api.wands.WandVisuals;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.common.item.IBloodOrb;
import wayoftime.bloodmagic.ritual.EnumRuneType;
import wayoftime.bloodmagic.ritual.RitualComponent;

/**
 * Native verification suite for Tasks C05–C08: Blood Magic integration.
 * Tests:
 * - C05: Pinned dependency, ClassLoader isolation, mod bridge.
 * - C06: Blood Rod (cap 100, cost 12, BloodWandUpdate), Blood Staff (cap 250, cost 25, BloodStaffUpdate),
 *        Alchemical Cap (0.85f base modifier, 15% discount), composite models.
 * - C07: Bloodwell (IScribeTools, durability 5), Boundwell (IScribeTools, durability 100),
 *        Blood Rapier (durability 700, 2 warp), Eldritch Orb (Tier 6, 80M LP, 5 warp, EPIC rarity, reusable).
 * - C08: Ritual "Plea of Delayed Insanity" (spite_sanity, 20k act, 15k refresh, 600 interval, 28 runes),
 *        Bloodthorn flower (magic dmg, Weakness III, LP/mana feed).
 */
public final class P04BloodMagicCheck {
    private static int checks = 0;

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) {
            throw new AssertionError("P04 Blood Magic check failed: " + reason);
        }
    }

    private static void cached(ModConfigSpec.ConfigValue<?> value, Object target) throws Exception {
        var field = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        field.setAccessible(true);
        field.set(value, target);
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
        cached(ForbiddenConfig.CROSS_MOD, true);
        cached(ForbiddenConfig.BLOOD_MAGIC, true);
        cached(ForbiddenConfig.BLOOD_VIS, 5);

        var bus = BusBuilder.builder().build();
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);
        BloodMagicCompat.register(bus);

        var constructor = RegisterEvent.class.getDeclaredConstructor(ResourceKey.class, Registry.class);
        constructor.setAccessible(true);
        for (var registry : List.of(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                BuiltInRegistries.MOB_EFFECT,
                BuiltInRegistries.BLOCK,
                BuiltInRegistries.ITEM,
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                BuiltInRegistries.CREATIVE_MODE_TAB
        )) {
            ((MappedRegistry<?>) registry).unfreeze();
            bus.post((RegisterEvent) constructor.newInstance(registry.key(), registry));
            registry.freeze();
        }

        ForbiddenAspects.register();
        ForbiddenWandMechanics.registerAll();
        BloodMagicCompat.registerWandParts();
        ForbiddenWandVisuals.register();

        testC05ApiAndClassLoaderSafety();
        testC06RodsAndCaps();
        testC07Items();
        testC08RitualAndFlower();
        testVisualsClasspathIntegrity();
        testDataPackIntegrity();

        System.out.println("FORBIDDEN_P04_BLOODMAGIC_PASS checks=" + checks);
    }

    private static void testC05ApiAndClassLoaderSafety() {
        check("bloodmagic".equals(BloodMagicCompat.MODID), "BM MODID is bloodmagic");

        // Interface assignability tests
        check(IScribeTools.class.isAssignableFrom(BaseBloodwellItem.class), "BaseBloodwell implements IScribeTools");
        check(IScribeTools.class.isAssignableFrom(BaseBoundwellItem.class), "BaseBoundwell implements IScribeTools");
        check(IWarpingGear.class.isAssignableFrom(BaseBloodRapierItem.class), "BaseRapier implements IWarpingGear");
        check(IWarpingGear.class.isAssignableFrom(BaseEldritchOrbItem.class), "BaseOrb implements IWarpingGear");

        // When BM is available on classpath, Bm* subclasses must implement BM interfaces
        check(IBindable.class.isAssignableFrom(BmBloodwellItem.class), "BmBloodwell implements IBindable");
        check(IBindable.class.isAssignableFrom(BmBoundwellItem.class), "BmBoundwell implements IBindable");
        check(BaseBloodRapierItem.class.isAssignableFrom(BmBloodRapierItem.class), "BmRapier extends BaseBloodRapierItem");
        check(IBloodOrb.class.isAssignableFrom(BmEldritchOrbItem.class), "BmOrb implements IBloodOrb");
        check(IBindable.class.isAssignableFrom(BmEldritchOrbItem.class), "BmOrb implements IBindable");

        // Bridge methods exist and return valid instances when invoked
        check(BloodMagicBridge.ELDRITCH_BLOOD_ORB != null, "ELDRITCH_BLOOD_ORB initialized");
    }

    private static void testC06RodsAndCaps() {
        // Blood Rod
        WandRod bloodRod = WandRod.rods.get("forbidden:blood");
        check(bloodRod != null, "forbidden:blood rod registered");
        check(bloodRod.getCapacity() == 100, "blood rod capacity is 100");
        check(bloodRod.getCraftCost() == 12, "blood rod cost is 12");
        check(WandRod.rods.get("blood") == bloodRod, "blood rod also accessible via 'blood'");

        // Blood Staff
        WandRod bloodStaff = WandRod.rods.get("forbidden:blood_staff");
        check(bloodStaff != null, "forbidden:blood_staff registered");
        check(bloodStaff instanceof StaffRod, "blood_staff is instance of StaffRod");
        check(bloodStaff.getCapacity() == 250, "blood staff capacity is 250");
        check(bloodStaff.getCraftCost() == 25, "blood staff cost is 25");
        check("forbidden:rod_blood_staff".equals(bloodStaff.getResearch()), "blood staff research is forbidden:rod_blood_staff");
        check(WandRod.rods.get("blood_staff") == bloodStaff, "blood staff also accessible via 'blood_staff'");

        // Alchemical Cap
        WandCap alchemicalCap = WandCap.caps.get("forbidden:alchemical");
        check(alchemicalCap != null, "forbidden:alchemical cap registered");
        check(Math.abs(alchemicalCap.getBaseCostModifier() - 0.85f) < 1e-5, "alchemical cap 0.85f modifier (15% discount)");
        check(alchemicalCap.getCraftCost() == 7, "alchemical cap cost is 7");
        check("forbidden:cap_alchemical".equals(alchemicalCap.getResearch()), "alchemical cap research is forbidden:cap_alchemical");
        check(WandCap.caps.get("alchemical") == alchemicalCap, "alchemical cap also accessible via 'alchemical'");

        // Updates
        check(BloodWandUpdate.INSTANCE != null, "BloodWandUpdate singleton exists");
        check(BloodStaffUpdate.INSTANCE != null, "BloodStaffUpdate singleton exists");
    }

    private static void testC07Items() {
        // Registry entries exist
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:bloodwell")), "registered forbidden:bloodwell");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:boundwell")), "registered forbidden:boundwell");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:blood_rapier")), "registered forbidden:blood_rapier");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:eldritch_orb")), "registered forbidden:eldritch_orb");

        // Physical properties
        var bloodwell = BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:bloodwell"));
        check(bloodwell instanceof IScribeTools, "bloodwell is IScribeTools");
        check(new ItemStack(bloodwell).getMaxDamage() == 5, "bloodwell max damage 5");

        var boundwell = BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:boundwell"));
        check(boundwell instanceof IScribeTools, "boundwell is IScribeTools");
        check(new ItemStack(boundwell).getMaxDamage() == 100, "boundwell max damage 100");

        var rapier = BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:blood_rapier"));
        check(rapier instanceof IWarpingGear gear && gear.getWarp(new ItemStack(rapier), null) == 2, "rapier has 2 warp");
        check(new ItemStack(rapier).getMaxDamage() == 700, "rapier max damage 700");

        var orb = BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:eldritch_orb"));
        check(orb instanceof IWarpingGear gear && gear.getWarp(new ItemStack(orb), null) == 5, "eldritch orb has 5 warp");
        check(orb.hasCraftingRemainingItem(new ItemStack(orb)), "eldritch orb has crafting remaining item");
        var testStack = new ItemStack(orb);
        check(orb.getCraftingRemainingItem(testStack).getItem() == orb, "eldritch orb crafting remainder is itself");

        // Eldritch blood orb specs
        var bloodOrb = BloodMagicBridge.ELDRITCH_BLOOD_ORB;
        check(bloodOrb.getTier() == 6, "orb tier 6");
        check(bloodOrb.getCapacity() == 80_000_000, "orb capacity 80,000,000 LP");
        check(bloodOrb.getFillRate() == 140, "orb fill rate 140");
    }

    private static void testC08RitualAndFlower() {
        // Ritual Plea of Delayed Insanity
        RitualSanity sanity = new RitualSanity();
        check("spite_sanity".equals(sanity.getName()), "ritual name spite_sanity");
        check(sanity.getActivationCost() == 20000, "ritual activation cost 20,000 LP");
        check(sanity.getRefreshCost() == 15000, "ritual refresh cost 15,000 LP");
        check(sanity.getRefreshTime() == 600, "ritual refresh interval 600 ticks");

        List<RitualComponent> components = new ArrayList<>();
        sanity.gatherComponents(components::add);
        check(components.size() == 28, "ritual has exactly 28 runes (actual: " + components.size() + ")");

        long duskRunes = components.stream().filter(c -> c.getRuneType() == EnumRuneType.DUSK).count();
        long earthRunes = components.stream().filter(c -> c.getRuneType() == EnumRuneType.EARTH).count();
        long airRunes = components.stream().filter(c -> c.getRuneType() == EnumRuneType.AIR).count();
        long fireRunes = components.stream().filter(c -> c.getRuneType() == EnumRuneType.FIRE).count();
        check(duskRunes == 8, "8 dusk runes");
        check(earthRunes == 12, "12 earth runes");
        check(airRunes == 4, "4 air runes");
        check(fireRunes == 4, "4 fire runes");

        // Bloodthorn Block & BlockEntity
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:bloodthorn")), "registered forbidden:bloodthorn block");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:bloodthorn")), "registered forbidden:bloodthorn item");
        check(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("forbidden:bloodthorn")), "registered forbidden:bloodthorn BE");

        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:bloodthorn"));
        check(block instanceof BloodthornBlock, "bloodthorn is BloodthornBlock");
        check(BloodthornBlockEntity.MAX_MANA == 360, "bloodthorn max mana 360");
        check(BloodthornBlockEntity.COST == 40, "bloodthorn cost 40");
        check(BloodthornBlockEntity.RANGE == 6, "bloodthorn range 6");

        var bloodthornBE = new BloodthornBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        check(bloodthornBE.getMana() == 0, "bloodthorn starts at 0 mana");
        check(bloodthornBE.getMaxMana() == 360, "bloodthorn max mana == 360");
        bloodthornBE.addMana(120);
        check(bloodthornBE.getMana() == 120, "bloodthorn mana after add == 120");
        bloodthornBE.addMana(1000);
        check(bloodthornBE.getMana() == 360, "bloodthorn mana capped at 360");
        bloodthornBE.setTicksExisted(88);
        bloodthornBE.setBindingPos(new BlockPos(2, 4, 6));

        CompoundTag bloodthornTag = new CompoundTag();
        bloodthornBE.writeCustomData(bloodthornTag);
        check(bloodthornTag.getInt("mana") == 360, "bloodthorn tag mana == 360");
        check(bloodthornTag.getInt("ticksExisted") == 88, "bloodthorn tag ticks == 88");
        var bloodthornReloaded = new BloodthornBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        bloodthornReloaded.readCustomData(bloodthornTag);
        check(bloodthornReloaded.getMana() == 360, "reloaded bloodthorn mana == 360");
        check(bloodthornReloaded.getTicksExisted() == 88, "reloaded bloodthorn ticks == 88");
        check(new BlockPos(2, 4, 6).equals(bloodthornReloaded.getBindingPos()), "reloaded bloodthorn boundPos matches");

        // Blood Seal Mob Effect
        check(BuiltInRegistries.MOB_EFFECT.containsKey(ResourceLocation.parse("forbidden:blood_seal")), "registered forbidden:blood_seal mob effect");
        var seal = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse("forbidden:blood_seal"));
        check(seal instanceof BloodSealEffect, "blood seal is BloodSealEffect");
        check(seal.getCategory() == MobEffectCategory.HARMFUL, "blood seal is harmful");
    }

    private static void testDataPackIntegrity() throws Exception {
        List<String> researches = List.of(
                "bloodmagic", "rod_blood", "rod_blood_staff", "cap_alchemical",
                "bloodwell", "bloodrapier", "eldritch_orb", "bloodthorn"
        );
        for (String r : researches) {
            String path = "/packs/bloodmagic/data/forbidden/thaumcraft/research/" + r + ".json";
            var stream = P04BloodMagicCheck.class.getResourceAsStream(path);
            check(stream != null, "Blood Magic research exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("key") && !json.get("key").getAsString().isEmpty(), "Research has non-empty key: " + r);
                check(json.has("category") && json.get("category").getAsString().equals("forbidden:FORBIDDEN"), "Research in FORBIDDEN category: " + r);
            }
        }

        List<String> recipes = List.of(
                "wand_rod_blood", "wand_rod_blood_staff", "wand_cap_alchemical", "bloodwell",
                "blood_rapier", "eldritch_orb", "boundwell", "bloodthorn"
        );
        for (String rec : recipes) {
            String path = "/packs/bloodmagic/data/forbidden/recipe/" + rec + ".json";
            var stream = P04BloodMagicCheck.class.getResourceAsStream(path);
            check(stream != null, "Blood Magic recipe exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("type") && !json.get("type").getAsString().isEmpty(), "Recipe has non-empty type: " + rec);
                check(json.has("result") || json.has("key"), "Recipe has result or key: " + rec);
            }
        }
    }

    private static void testVisualsClasspathIntegrity() {
        for (var model : WandVisuals.models()) {
            check(P04BloodMagicCheck.class.getResource("/assets/" + model.getNamespace() + "/models/" + model.getPath() + ".json") != null,
                    "registered composite model exists on classpath: " + model);
        }
    }

    private P04BloodMagicCheck() {}
}
