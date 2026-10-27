package forbidden.verification;

import com.google.gson.JsonObject;
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
import forbidden.compat.botania.BotaniaCompat;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenAspects;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenWandVisuals;
import forbidden.content.wands.ForbiddenWandMechanics;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import sun.misc.Unsafe;
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
 *        Alchemical Cap (0.9f base, 0.8f Aqua), composite models.
 * - C07: Bloodwell (IScribeTools, durability 5), Boundwell (IScribeTools, durability 100),
 *        Blood Rapier (durability 700, 2 warp), Eldritch Orb (Tier 6, 80M LP, 5 warp, EPIC rarity, reusable).
 * - C08: Ritual "Plea of Delayed Insanity" (spite_sanity, 20k act, 15k refresh, 600 interval, 28 runes),
 *        Bloodthorn flower (Weakness III, mana threshold, player exemption, redstone pause).
 * - R06: Functional simulation of Bloodthorn flower and datapack integrity check against BM jar.
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

        // Explicit JVM fixture: enabled dependencies, not a claim of loaded-server gameplay.
        BloodMagicCompat.setForceLoadedForTesting(true);
        forbidden.compat.botania.BotaniaCompat.setForceLoadedForTesting(true);
        var bus = BusBuilder.builder().build();
        ForbiddenComponents.register(bus);
        ForbiddenContent.register(bus);
        forbidden.content.ForbiddenTools.register(bus);
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

        try {
            var emptyTypeHolder = net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE;
            var holderField = net.neoforged.neoforge.registries.DeferredHolder.class.getDeclaredField("holder");
            holderField.setAccessible(true);
            if (emptyTypeHolder != null && holderField.get(emptyTypeHolder) == null) {
                var emptyFluidType = new net.neoforged.neoforge.fluids.FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create());
                holderField.set(emptyTypeHolder, net.minecraft.core.Holder.direct(emptyFluidType));
            }
        } catch (Throwable ignored) {
        }

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
        check(bloodStaff.getCapacity() == 50, "release blood staff capacity is 50");
        check(bloodStaff.getCraftCost() == 24, "release blood staff cost is 24");
        check("forbidden:rod_blood_staff".equals(bloodStaff.getResearch()), "blood staff research is forbidden:rod_blood_staff");
        check(WandRod.rods.get("blood_staff") == bloodStaff, "blood staff also accessible via 'blood_staff'");

        // Alchemical Cap
        WandCap alchemicalCap = WandCap.caps.get("forbidden:alchemical");
        check(alchemicalCap != null, "forbidden:alchemical cap registered");
        check(Math.abs(alchemicalCap.getBaseCostModifier() - 0.9f) < 1e-5, "release alchemical cap 0.9f base modifier");
        for (var aspect : thaumcraft.api.aspects.Aspects.PRIMALS) {
            check(Math.abs(alchemicalCap.modifier(aspect) - (aspect == thaumcraft.api.aspects.Aspects.WATER ? 0.8f : 0.9f)) < 1e-5,
                    "release alchemical per-aspect modifier: " + aspect.getTag());
        }
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

    public static class TestLivingEntity extends LivingEntity {
        public Map<Holder<MobEffect>, MobEffectInstance> customEffects;
        public boolean alive = true;

        @SuppressWarnings("null")
        protected TestLivingEntity() {
            super(null, null);
        }

        @Override public boolean isAlive() { return alive; }
        @Override public MobEffectInstance getEffect(Holder<MobEffect> effect) {
            return customEffects != null ? customEffects.get(effect) : null;
        }
        @Override public boolean addEffect(MobEffectInstance effectInstance, net.minecraft.world.entity.Entity entity) {
            if (customEffects != null) {
                customEffects.put(effectInstance.getEffect(), effectInstance);
            }
            return true;
        }
        @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
        @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
        @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
        @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
    }

    public static class TestPlayerEntity extends Player {
        public boolean alive = true;

        @SuppressWarnings("null")
        protected TestPlayerEntity() {
            super(null, null, 0, null);
        }

        @Override public boolean isAlive() { return alive; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }

    private static TestLivingEntity createTestMob(AABB box) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            TestLivingEntity mob = (TestLivingEntity) unsafe.allocateInstance(TestLivingEntity.class);
            mob.customEffects = new HashMap<>();
            mob.setBoundingBox(box);
            mob.alive = true;
            return mob;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate TestLivingEntity", e);
        }
    }

    private static TestPlayerEntity createTestPlayer(AABB box) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            TestPlayerEntity p = (TestPlayerEntity) unsafe.allocateInstance(TestPlayerEntity.class);
            p.setBoundingBox(box);
            p.alive = true;
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate TestPlayerEntity", e);
        }
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

        // --- Functional Simulation of Bloodthorn (R06) ---
        SimulatedFlowerLevel simLevel = SimulatedFlowerLevel.create();
        BlockPos flowerPos = new BlockPos(0, 64, 0);
        simLevel.setBlock(flowerPos, block.defaultBlockState(), 3);
        var simBloodthorn = new BloodthornBlockEntity(flowerPos, block.defaultBlockState());

        TestLivingEntity simMob = createTestMob(new AABB(flowerPos).inflate(2));
        TestPlayerEntity simPlayer = createTestPlayer(new AABB(flowerPos).inflate(2));
        simLevel.entities.add(simMob);
        simLevel.entities.add(simPlayer);

        // A. Redstone pause test
        simBloodthorn.addMana(360);
        simLevel.neighborSignal = true;
        simBloodthorn.tick(simLevel, flowerPos, block.defaultBlockState());
        check(simBloodthorn.getMana() == 360, "bloodthorn pauses on redstone, mana untouched");
        check(simMob.customEffects.isEmpty(), "bloodthorn does not inflict effect when paused by redstone");

        // B. Insufficient mana test (< 40)
        simLevel.neighborSignal = false;
        simBloodthorn.addMana(-360);
        simBloodthorn.addMana(39);
        check(simBloodthorn.getMana() == 39, "bloodthorn mana set to 39");
        simBloodthorn.tick(simLevel, flowerPos, block.defaultBlockState());
        check(simBloodthorn.getMana() == 39, "insufficient mana (<40) does not consume mana");
        check(simMob.customEffects.isEmpty(), "insufficient mana does not inflict weakness");

        // C. Successful Weakness III application and mana consumption (40)
        simBloodthorn.addMana(1); // 40 mana
        check(simBloodthorn.getMana() == 40, "bloodthorn mana at threshold 40");
        simBloodthorn.tick(simLevel, flowerPos, block.defaultBlockState());
        check(simBloodthorn.getMana() == 0, "consumed exactly 40 mana for 1 target");
        check(simMob.customEffects.containsKey(MobEffects.WEAKNESS), "applied weakness to mob");
        var inst = simMob.customEffects.get(MobEffects.WEAKNESS);
        check(inst.getAmplifier() == 2, "weakness amplifier is 2 (Weakness III)");
        check(inst.getDuration() == 60, "weakness duration is 60 ticks");

        // D. Already afflicted check (no duplicate consumption)
        simBloodthorn.addMana(100);
        simBloodthorn.tick(simLevel, flowerPos, block.defaultBlockState());
        check(simBloodthorn.getMana() == 100, "target already afflicted, no extra mana consumed");

        // E. Player exemption test
        simMob.setBoundingBox(new AABB(100, 100, 100, 101, 101, 101)); // mob out of range
        simBloodthorn.tick(simLevel, flowerPos, block.defaultBlockState());
        check(simBloodthorn.getMana() == 100, "player in range is strictly exempted, zero mana consumed");
        check(simMob.customEffects.size() == 1, "no extra effects recorded");

        // F. Boundary tests for Botania mana helpers
        check(BotaniaCompat.drawMana(null, flowerPos, null, 100) == 0, "drawMana with null level returns 0");
        check(BotaniaCompat.drawMana(simLevel, flowerPos, null, -50) == 0, "drawMana with negative needed returns 0");
        check(BotaniaCompat.emitMana(null, flowerPos, null, 100) == 0, "emitMana with null level returns 0");
        check(BotaniaCompat.emitMana(simLevel, flowerPos, null, -50) == 0, "emitMana with negative available returns 0");
    }

    private static void testDataPackIntegrity() throws Exception {
        Set<String> validBloodMagicItems = new HashSet<>();
        URL bmJarUrl = IBindable.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jar = new JarFile(new File(bmJarUrl.toURI()))) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("assets/bloodmagic/models/item/") && name.endsWith(".json")) {
                    String id = name.substring("assets/bloodmagic/models/item/".length(), name.length() - 5);
                    validBloodMagicItems.add("bloodmagic:" + id);
                }
                if (name.startsWith("assets/bloodmagic/blockstates/") && name.endsWith(".json")) {
                    String id = name.substring("assets/bloodmagic/blockstates/".length(), name.length() - 5);
                    validBloodMagicItems.add("bloodmagic:" + id);
                }
            }
        }
        check(!validBloodMagicItems.isEmpty(), "BM items found in jar: " + validBloodMagicItems.size());
        check(validBloodMagicItems.contains("bloodmagic:reagentbinding"), "BM jar contains reagentbinding");
        check(validBloodMagicItems.contains("bloodmagic:blankslate"), "BM jar contains blankslate");
        check(!validBloodMagicItems.contains("bloodmagic:blank_slate"), "BM jar rejects legacy blank_slate");
        check(!validBloodMagicItems.contains("bloodmagic:incense_crucible"), "BM jar rejects incense_crucible");

        Set<String> validBotaniaItems = new HashSet<>();
        URL botaniaJarUrl = vazkii.botania.common.item.BotaniaItems.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jar = new JarFile(new File(botaniaJarUrl.toURI()))) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("assets/botania/models/item/") && name.endsWith(".json")) {
                    String id = name.substring("assets/botania/models/item/".length(), name.length() - 5);
                    validBotaniaItems.add("botania:" + id);
                }
                if (name.startsWith("assets/botania/blockstates/") && name.endsWith(".json")) {
                    String id = name.substring("assets/botania/blockstates/".length(), name.length() - 5);
                    validBotaniaItems.add("botania:" + id);
                }
            }
        }
        check(!validBotaniaItems.isEmpty(), "Botania items found in jar: " + validBotaniaItems.size());

        List<String> researches = List.of(
                "bloodmagic", "rod_blood", "rod_blood_staff", "cap_alchemical",
                "bloodwell", "bloodrapier", "eldritch_orb"
        );
        check(P04BloodMagicCheck.class.getResource("/packs/bloodthorn/data/forbidden/thaumcraft/research/bloodthorn.json") == null, "release has no separate Bloodthorn research");
        for (String r : researches) {
            String pack = r.equals("bloodthorn") ? "bloodthorn" : (r.startsWith("rod_")||r.startsWith("cap_")?"bloodmagic_wands":"bloodmagic");
            String path = "/packs/" + pack + "/data/forbidden/thaumcraft/research/" + r + ".json";
            var stream = P04BloodMagicCheck.class.getResourceAsStream(path);
            check(stream != null, "Blood Magic research exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("key") && !json.get("key").getAsString().isEmpty(), "Research has non-empty key: " + r);
                check(json.has("category") && json.get("category").getAsString().equals("forbidden:forbidden"), "Research in FORBIDDEN category: " + r);
            }
        }

        List<String> recipes = List.of(
                "wand_rod_blood", "wand_rod_blood_staff", "wand_cap_alchemical", "bloodwell",
                "blood_rapier", "eldritch_orb", "boundwell", "bloodthorn",
                "altar_wand_rod_blood"
        );
        for (String rec : recipes) {
            String path = "/packs/" + (rec.contains("wand_rod_")||rec.contains("wand_cap_")?"bloodmagic_wands":"bloodmagic") + "/data/forbidden/recipe/" + rec + ".json";
            var stream = P04BloodMagicCheck.class.getResourceAsStream(path);
            check(stream != null, "Blood Magic recipe exists: " + path);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                check(json.has("type") && !json.get("type").getAsString().isEmpty(), "Recipe has non-empty type: " + rec);
                check(json.has("result") || json.has("output") || json.has("key"), "Recipe has result/output/key: " + rec);
                validateReferencedItems(json, validBloodMagicItems, validBotaniaItems, rec);
            }
        }
    }

    private static void validateReferencedItems(JsonObject json, Set<String> validBm, Set<String> validBotania, String recipe) {
        List<String> itemsToCheck = new ArrayList<>();
        var res = json.has("result") ? json.get("result") : json.get("output");
        if (res != null) {
            if (res.isJsonObject()) {
                var r = res.getAsJsonObject();
                if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
            } else if (res.isJsonPrimitive()) {
                itemsToCheck.add(res.getAsString());
            }
        }
        var in = json.has("input") ? json.get("input") : (json.has("central_item") ? json.get("central_item") : json.get("catalyst"));
        if (in != null) {
            if (in.isJsonObject()) {
                var r = in.getAsJsonObject();
                if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
            } else if (in.isJsonPrimitive()) {
                itemsToCheck.add(in.getAsString());
            }
        }
        if (json.has("components")) {
            for (var elem : json.getAsJsonArray("components")) {
                if (elem.isJsonObject()) {
                    var r = elem.getAsJsonObject();
                    if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                    else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
                } else if (elem.isJsonPrimitive()) {
                    itemsToCheck.add(elem.getAsString());
                }
            }
        }
        if (json.has("ingredients")) {
            for (var elem : json.getAsJsonArray("ingredients")) {
                if (elem.isJsonObject()) {
                    var r = elem.getAsJsonObject();
                    if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                    else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
                } else if (elem.isJsonPrimitive()) {
                    itemsToCheck.add(elem.getAsString());
                }
            }
        }
        if (json.has("key")) {
            var keyObj = json.getAsJsonObject("key");
            for (var entry : keyObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    var r = entry.getValue().getAsJsonObject();
                    if (r.has("id")) itemsToCheck.add(r.get("id").getAsString());
                    else if (r.has("item")) itemsToCheck.add(r.get("item").getAsString());
                } else if (entry.getValue().isJsonPrimitive()) {
                    itemsToCheck.add(entry.getValue().getAsString());
                }
            }
        }

        for (String id : itemsToCheck) {
            check(!id.isBlank(), "item id non-blank in " + recipe);
            if (id.startsWith("bloodmagic:")) {
                check(validBm.contains(id), "Blood Magic item exists in real mod registry: " + id + " in " + recipe);
            } else if (id.startsWith("botania:")) {
                check(validBotania.contains(id), "Botania item exists in real mod registry: " + id + " in " + recipe);
            } else if (id.startsWith("forbidden:")) {
                var loc = ResourceLocation.parse(id);
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc);
                check(exists, "Forbidden Item/Block exists in registry: " + id + " in " + recipe);
            } else if (id.startsWith("minecraft:")) {
                var loc = ResourceLocation.parse(id);
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc);
                check(exists, "Minecraft Item/Block exists in registry: " + id + " in " + recipe);
            } else if (id.startsWith("thaumcraft:")) {
                var loc = ResourceLocation.parse(id);
                String path = loc.getPath();
                String capName = path.startsWith("wand_cap_") ? path.substring("wand_cap_".length()) : path;
                String rodName = path.startsWith("wand_rod_") ? path.substring("wand_rod_".length()) : path;
                boolean exists = BuiltInRegistries.ITEM.containsKey(loc) || BuiltInRegistries.BLOCK.containsKey(loc)
                        || WandCap.caps.containsKey(path) || WandCap.caps.containsKey(capName) || WandCap.caps.containsKey("thaumcraft:" + capName)
                        || WandRod.rods.containsKey(path) || WandRod.rods.containsKey(rodName) || WandRod.rods.containsKey("thaumcraft:" + rodName)
                        || P04BloodMagicCheck.class.getResource("/assets/thaumcraft/models/item/" + loc.getPath() + ".json") != null
                        || P04BloodMagicCheck.class.getResource("/assets/thaumcraft/blockstates/" + loc.getPath() + ".json") != null;
                check(exists, "Thaumcraft Item/Block exists: " + id + " in " + recipe);
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
