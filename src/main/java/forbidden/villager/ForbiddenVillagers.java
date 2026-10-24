package forbidden.villager;

import com.google.common.collect.ImmutableSet;
import forbidden.ForbiddenMagic;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class ForbiddenVillagers {
    public static final ResourceLocation HERETIC_ID = ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, "heretic");
    public static final ResourceKey<PoiType> HERETIC_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, HERETIC_ID);
    public static final ResourceKey<VillagerProfession> HERETIC_PROFESSION_KEY = ResourceKey.create(Registries.VILLAGER_PROFESSION, HERETIC_ID);

    public static VillagerProfession HERETIC_PROFESSION;

    @EventBusSubscriber(modid = ForbiddenMagic.ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            if (event.getRegistryKey().equals(Registries.POINT_OF_INTEREST_TYPE)) {
                event.register(Registries.POINT_OF_INTEREST_TYPE, HERETIC_ID, () ->
                    new PoiType(Set.copyOf(ForbiddenContent.HERETIC_STUDY.get().getStateDefinition().getPossibleStates()), 1, 1)
                );
            }
            if (event.getRegistryKey().equals(Registries.VILLAGER_PROFESSION)) {
                Predicate<Holder<PoiType>> poiPredicate = holder -> holder.is(HERETIC_POI_KEY);
                event.register(Registries.VILLAGER_PROFESSION, HERETIC_ID, () -> {
                    HERETIC_PROFESSION = new VillagerProfession(
                        HERETIC_ID.toString(),
                        poiPredicate,
                        poiPredicate,
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        SoundEvents.VILLAGER_WORK_CLERIC
                    );
                    return HERETIC_PROFESSION;
                });
            }
        }
    }

    @EventBusSubscriber(modid = ForbiddenMagic.ID, bus = EventBusSubscriber.Bus.GAME)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onVillagerTrades(VillagerTradesEvent event) {
            if (event.getType() != HERETIC_PROFESSION && !event.getType().name().equals(HERETIC_ID.toString()) && !event.getType().name().equals("heretic")) {
                return;
            }

            var trades = event.getTrades().computeIfAbsent(1, k -> new java.util.ArrayList<>());

            // Trade 1: Emerald (1-2) -> Greed Shards (3-4)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1 + random.nextInt(2)),
                new ItemStack(ForbiddenContent.item("greed_shard"), 3 + random.nextInt(2)),
                12, 2, 0.05F
            ));

            // Trade 2: Lust Shards (2-5) -> Emerald (1) [if !noLust]
            if (!ForbiddenConfig.NO_LUST.get()) {
                trades.add((entity, random) -> new MerchantOffer(
                    new ItemCost(ForbiddenContent.item("lust_shard"), 2 + random.nextInt(4)),
                    new ItemStack(Items.EMERALD, 1),
                    12, 2, 0.05F
                ));
            }

            // Trade 3: Wrath Shards (4-9) -> Emerald (1)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(ForbiddenContent.item("wrath_shard"), 4 + random.nextInt(6)),
                new ItemStack(Items.EMERALD, 1),
                12, 2, 0.05F
            ));

            // Trade 4: Gluttony Shards (4-9) -> Emerald (1)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(ForbiddenContent.item("gluttony_shard"), 4 + random.nextInt(6)),
                new ItemStack(Items.EMERALD, 1),
                12, 2, 0.05F
            ));

            // Trade 5: Emerald (1-3) -> Skeleton Skull (1)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1 + random.nextInt(3)),
                new ItemStack(Items.SKELETON_SKULL, 1),
                12, 2, 0.05F
            ));

            // Trade 6: Emerald (4-7) -> Ghast Tear (1)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 4 + random.nextInt(4)),
                new ItemStack(Items.GHAST_TEAR, 1),
                12, 2, 0.05F
            ));

            // Trade 7: Emerald (1) -> Glowstone Dust (8-16)
            trades.add((entity, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(Items.GLOWSTONE_DUST, 8 + random.nextInt(9)),
                12, 2, 0.05F
            ));

            // Trade 8: Emerald (8-12) + Stick (1) -> Charged Profane Wand (maxUses = 2)
            trades.add((entity, random) -> {
                ItemStack wand = createChargedProfaneWand();
                return new MerchantOffer(
                    new ItemCost(Items.EMERALD, 8 + random.nextInt(5)),
                    Optional.of(new ItemCost(Items.STICK, 1)),
                    wand,
                    2, // maxUses = 2
                    10, 0.05F
                );
            });

            // Trade 9: Emerald (4-8) + Diamond (1) -> Fire Focus (maxUses = 2)
            trades.add((entity, random) -> {
                var fireFocusItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:focus_fire"));
                ItemStack fireFocus = new ItemStack(fireFocusItem);
                return new MerchantOffer(
                    new ItemCost(Items.EMERALD, 4 + random.nextInt(5)),
                    Optional.of(new ItemCost(Items.DIAMOND, 1)),
                    fireFocus,
                    2, // maxUses = 2
                    10, 0.05F
                );
            });
        }
    }

    public static ItemStack createChargedProfaneWand() {
        var wandItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:casting_wand"));
        ItemStack wand = wandItem != Items.AIR ? new ItemStack(wandItem) : ItemStack.EMPTY;
        if (!wand.isEmpty()) {
            WandApi.parts(wand, new WandParts("forbidden:profane", "iron"));
            for (var aspect : thaumcraft.api.aspects.Aspects.PRIMALS) {
                WandApi.set(wand, aspect, 5000);
            }
        }
        return wand;
    }

    private ForbiddenVillagers() {}
}
