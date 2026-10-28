package forbidden.content;

import forbidden.ForbiddenMagic;
import forbidden.config.ForbiddenConfig;
import forbidden.content.baubles.RingNutritionItem;
import forbidden.content.baubles.SubCollarItem;
import forbidden.content.scribes.CrystalwellItem;
import forbidden.content.scribes.PrimewellItem;
import forbidden.content.tools.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registration class for P02 tools and accessories. */
@EventBusSubscriber(modid = ForbiddenMagic.ID, bus = EventBusSubscriber.Bus.MOD)
public final class ForbiddenTools {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, ForbiddenMagic.ID);
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<ThrownDiabolistFork>> THROWN_FORK =
        ENTITIES.register("diabolist_fork", () -> net.minecraft.world.entity.EntityType.Builder
            .<ThrownDiabolistFork>of(ThrownDiabolistFork::new, net.minecraft.world.entity.MobCategory.MISC)
            .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).build("forbidden:diabolist_fork"));

    public static final DeferredItem<TaintShovelItem> TAINT_SHOVEL = ITEMS.register("taint_shovel", TaintShovelItem::new);
    public static final DeferredItem<TaintPickaxeItem> TAINT_PICKAXE = ITEMS.register("taint_pickaxe", TaintPickaxeItem::new);
    public static final DeferredItem<SkullAxeItem> SKULL_AXE = ITEMS.register("skull_axe", SkullAxeItem::new);
    public static final DeferredItem<MorphPickaxeItem> MORPH_PICKAXE = ITEMS.register("morph_pickaxe", MorphPickaxeItem::new);
    public static final DeferredItem<MorphSwordItem> MORPH_SWORD = ITEMS.register("morph_sword", MorphSwordItem::new);
    public static final DeferredItem<MorphShovelItem> MORPH_SHOVEL = ITEMS.register("morph_shovel", MorphShovelItem::new);
    public static final DeferredItem<MorphAxeItem> MORPH_AXE = ITEMS.register("morph_axe", MorphAxeItem::new);
    public static final DeferredItem<DiabolistForkItem> DIABOLIST_FORK = ITEMS.register("diabolist_fork", DiabolistForkItem::new);
    public static final DeferredItem<RidingCropItem> RIDING_CROP = ITEMS.register("riding_crop", RidingCropItem::new);
    public static final DeferredItem<CrystalwellItem> CRYSTALWELL = ITEMS.register("crystalwell", CrystalwellItem::new);
    public static final DeferredItem<PrimewellItem> PRIMEWELL = ITEMS.register("primewell", PrimewellItem::new);
    public static final DeferredItem<RingNutritionItem> RING_NUTRITION = ITEMS.register("ring_nutrition", ForbiddenTools::ringNutrition);
    public static DeferredItem<SubCollarItem> SUB_COLLAR;

    private static boolean registeredToBus = false;

    static {
        initConditional();
    }

    public static synchronized void initConditional() {
        if (SUB_COLLAR == null) {
            try {
                if (!ForbiddenConfig.NO_LUST.get()) {
                    SUB_COLLAR = ITEMS.register("sub_collar", ForbiddenTools::subCollar);
                }
            } catch (Throwable ignored) {
                // Config not loaded yet; will be re-attempted on registration
            }
        }
    }

    public static synchronized void register(IEventBus bus) {
        initConditional();
        if (!registeredToBus) {
            ITEMS.register(bus);
            ENTITIES.register(bus);
            registeredToBus = true;
        }
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            initConditional();
            if (!registeredToBus) {
                event.register(Registries.ITEM, helper -> {
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "taint_shovel"), new TaintShovelItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "taint_pickaxe"), new TaintPickaxeItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "skull_axe"), new SkullAxeItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "morph_pickaxe"), new MorphPickaxeItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "morph_sword"), new MorphSwordItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "morph_shovel"), new MorphShovelItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "morph_axe"), new MorphAxeItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "diabolist_fork"), new DiabolistForkItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "riding_crop"), new RidingCropItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "crystalwell"), new CrystalwellItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "primewell"), new PrimewellItem());
                    helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "ring_nutrition"), ringNutrition());
                    if (!ForbiddenConfig.NO_LUST.get()) {
                        helper.register(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "sub_collar"), subCollar());
                    }
                });
            }
        }
    }

    private static RingNutritionItem ringNutrition() {
        return net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("botania")
            ? forbidden.compat.botania.BotaniaBaubleBridge.ring() : new RingNutritionItem();
    }

    private static SubCollarItem subCollar() {
        return net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("botania")
            ? forbidden.compat.botania.BotaniaBaubleBridge.collar() : new SubCollarItem();
    }

    private ForbiddenTools() {}
}
