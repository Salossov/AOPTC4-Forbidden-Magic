package forbidden.events;

import forbidden.ForbiddenMagic;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;
import forbidden.content.tools.MorphAxeItem;
import forbidden.content.tools.MorphPickaxeItem;
import forbidden.content.tools.MorphShovelItem;
import forbidden.content.tools.MorphSwordItem;
import forbidden.enchantments.DarkEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.SpecialMiningApi;

import java.util.*;

/**
 * Core event handler for Forbidden Magic mechanics:
 * B28 (Cluster, Consuming, Corrupting), B29 (Wrath, Greedy, Educational, Voidtouched, Impact),
 * B30 (Nether mob drops, Silverfish, Gluttony eating, Envy piglin, Tooltips).
 */
@EventBusSubscriber(modid = ForbiddenMagic.ID)
public final class ForbiddenEvents {
    private static final Map<UUID, Direction> LAST_CLICKED_FACE = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> IMPACT_MINING = ThreadLocal.withInitial(() -> false);

    // ==========================================
    // B28: Block Drops (Cluster, Consuming, Corrupting, Quartz Greed)
    // ==========================================

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getLevel().isClientSide()) return;
        ItemStack tool = event.getTool();
        if (tool.isEmpty()) return;

        var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int fortune = tool.getEnchantmentLevel(registry.getOrThrow(Enchantments.FORTUNE));
        RandomSource rand = event.getLevel().getRandom();

        // 1. Cluster: Incompatible with Fortune (if Fortune present, Cluster disabled).
        int clusterLevel = DarkEnchantments.getLevel(tool, DarkEnchantments.CLUSTER);
        if (clusterLevel > 0 && fortune == 0 && !event.getDrops().isEmpty()) {
            float chance = 0.2F + clusterLevel * 0.075F;
            for (ItemEntity dropEntity : event.getDrops()) {
                ItemStack is = dropEntity.getItem();
                if (rand.nextFloat() <= chance) {
                    try {
                        ItemStack smr = SpecialMiningApi.specialMiningResult(event.getLevel(), event.getState(), is);
                        if (!smr.isEmpty() && !ItemStack.isSameItemSameComponents(is, smr)) {
                            smr.setCount(is.getCount());
                            dropEntity.setItem(smr);
                            event.getLevel().playSound(
                                null, event.getPos(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                                0.2F, 0.7F + rand.nextFloat() * 0.2F
                            );
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // 2. Consuming (F04): delete all matching trash drops.
        int consumingLevel = DarkEnchantments.getLevel(tool, DarkEnchantments.CONSUMING);
        if (consumingLevel > 0) {
            event.getDrops().removeIf(entity -> isGarbage(entity.getItem()));
        }

        // 3. Corrupting: turns infused stone drops into sin shards.
        int corruptingLevel = DarkEnchantments.getLevel(tool, DarkEnchantments.CORRUPTING);
        if (corruptingLevel > 0 && isCorruptibleOre(event.getState()) && rand.nextInt(3) == 1) {
            List<ItemEntity> newDrops = new ArrayList<>();
            for (ItemEntity entity : event.getDrops()) {
                ItemStack is = entity.getItem();
                if (isThaumcraftShard(is)) {
                    int count = is.getCount();
                    for (int i = 0; i < count; i++) {
                        ItemStack sinShard = getSinShard(rand);
                        if (!sinShard.isEmpty()) {
                            ItemEntity newEntity = new ItemEntity(
                                event.getLevel(),
                                entity.getX(), entity.getY(), entity.getZ(),
                                sinShard
                            );
                            newEntity.setDefaultPickUpDelay();
                            newDrops.add(newEntity);
                        }
                    }
                    entity.discard();
                }
            }
            event.getDrops().removeIf(Entity::isRemoved);
            event.getDrops().addAll(newDrops);
        }

        // 4. Nether Quartz greed shard drop
        if (event.getLevel().dimension() == Level.NETHER && event.getState().is(Blocks.NETHER_QUARTZ_ORE)) {
            if (fortune > 0 && rand.nextInt(60) <= fortune) {
                BlockPos pos = event.getPos();
                ItemEntity greedDrop = new ItemEntity(
                    event.getLevel(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ForbiddenContent.item("greed_shard"))
                );
                greedDrop.setDefaultPickUpDelay();
                event.getDrops().add(greedDrop);
            }
        }
    }

    private static boolean isGarbage(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String tagStr : ForbiddenConfig.GARBAGE_TAGS.get()) {
            var tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagStr));
            if (stack.is(tagKey)) return true;
            if (stack.getItem() instanceof BlockItem bi) {
                var blockTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tagStr));
                if (bi.getBlock().defaultBlockState().is(blockTag)) return true;
            }
        }
        return false;
    }

    private static boolean isCorruptibleOre(BlockState state) {
        // The release checks blockCustomOre by identity, including its cinnabar
        // and amber variants. A name substring also matched unrelated masonry.
        return state.is(BlockTags.create(ResourceLocation.parse("forbidden:corruptible_ores")));
    }

    private static boolean isThaumcraftShard(ItemStack stack) {
        // The seven flattened variants of the release's itemShard.
        return stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse("forbidden:research_shards")));
    }

    public static ItemStack getSinShard(RandomSource rand) {
        int x = rand.nextInt(7);
        return switch (x) {
            case 0 -> new ItemStack(ForbiddenContent.item("wrath_shard"));
            case 1 -> new ItemStack(ForbiddenContent.item("envy_shard"));
            case 2 -> new ItemStack(ForbiddenContent.item("gluttony_shard"));
            case 3 -> new ItemStack(ForbiddenContent.item("pride_shard"));
            case 4 -> {
                if (ForbiddenConfig.NO_LUST.get()) {
                    yield getSinShard(rand);
                }
                yield new ItemStack(ForbiddenContent.item("lust_shard"));
            }
            case 5 -> new ItemStack(ForbiddenContent.item("sloth_shard"));
            case 6 -> new ItemStack(ForbiddenContent.item("greed_shard"));
            default -> ItemStack.EMPTY;
        };
    }

    // ==========================================
    // B29: Impact (3x3 Mining)
    // ==========================================

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() != null) {
            LAST_CLICKED_FACE.put(event.getEntity().getUUID(), event.getFace());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (IMPACT_MINING.get()) return;
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack tool = player.getMainHandItem();
        int impactLevel = DarkEnchantments.getLevel(tool, DarkEnchantments.IMPACT);
        if (impactLevel <= 0) return;

        Direction face = LAST_CLICKED_FACE.getOrDefault(player.getUUID(), Direction.UP);
        ServerLevel level = serverPlayer.serverLevel();
        BlockPos center = event.getPos();

        int xOff = face.getStepX() != 0 ? 0 : 1;
        int yOff = face.getStepY() != 0 ? 0 : 1;
        int zOff = face.getStepZ() != 0 ? 0 : 1;

        IMPACT_MINING.set(true);
        try {
            for (int dx = -xOff; dx <= xOff; dx++) {
                for (int dy = -yOff; dy <= yOff; dy++) {
                    for (int dz = -zOff; dz <= zOff; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos target = center.offset(dx, dy, dz);
                        if (!level.mayInteract(serverPlayer, target)) continue;
                        if (serverPlayer.blockActionRestricted(level, target, serverPlayer.gameMode.getGameModeForPlayer())) continue;

                        BlockState state = level.getBlockState(target);
                        if (state.isAir() || state.getDestroySpeed(level, target) < 0) continue;
                        if (!tool.isCorrectToolForDrops(state)) continue;

                        serverPlayer.gameMode.destroyBlock(target);
                    }
                }
            }
        } finally {
            IMPACT_MINING.set(false);
        }
    }

    // ==========================================
    // B29: Wrath Damage Bonus & Wand Immunities
    // ==========================================

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();

        // Defensive wand effects are handled once by ForbiddenWandEvents at
        // LivingIncomingDamageEvent. Repeating them here charged tainted rods
        // twice and also incorrectly charged them for ordinary magic damage.

        // Attacker processing
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = player.getMainHandItem();

            // Wrath extra damage to hostile mobs: wrathLevel * 1.25F
            int wrathLevel = DarkEnchantments.getLevel(weapon, DarkEnchantments.WRATH);
            if (wrathLevel > 0 && victim instanceof Enemy) {
                event.setNewDamage(event.getNewDamage() + wrathLevel * 1.25F);
            }

            // Alchemical cap + blood rod gives Weakness II
            if (WandApi.kind(weapon) != WandApi.Kind.NONE) {
                var parts = WandApi.parts(weapon);
                if (parts != null && parts.cap() != null && parts.cap().contains("alchemical")
                    && parts.rod() != null && parts.rod().contains("blood")) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
                }
            }
        }
    }

    // ==========================================
    // B29 & B30: Living Drops (Greedy, Silverfish, Nether Shards)
    // ==========================================

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        RandomSource rand = level.getRandom();

        // 1. Tainted mob weakness blood shard
        if (event.getSource().getEntity() instanceof Player && isTaintedMob(victim)) {
            var weakness = victim.getEffect(MobEffects.WEAKNESS);
            if (weakness != null && weakness.getAmplifier() >= 2 && rand.nextFloat() < 0.5F) {
                addDrop(event, new ItemStack(ForbiddenContent.item("tainted_blood_shard")));
            }
        }

        // 2. Silverfish emerald nuggets
        if (ForbiddenConfig.SILVERFISH_EMERALDS.get() && victim instanceof Silverfish) {
            if (event.isRecentlyHit() && event.getSource().getEntity() instanceof Player && !(event.getSource().getEntity() instanceof FakePlayer)) {
                var biome = level.getBiome(victim.blockPosition());
                boolean isMountain = biome.is(BiomeTags.IS_MOUNTAIN);
                int chance = isMountain ? (2 + getLootingLevel(event) * 2) : (1 + getLootingLevel(event) * 3);
                int bound = isMountain ? 30 : 70;
                if (rand.nextInt(bound) <= chance) {
                    addDrop(event, new ItemStack(ForbiddenContent.item("emerald_nugget")));
                }
            }
        }

        // 3. Greedy Enchantment (incompatible with Looting)
        if (ForbiddenConfig.GREEDY.get() && event.isRecentlyHit() && event.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack weapon = player.getMainHandItem();
            int greedyLevel = DarkEnchantments.getLevel(weapon, DarkEnchantments.GREEDY);
            if (greedyLevel > 0 && getLootingLevel(event) <= 0) {
                if (victim instanceof Villager) {
                    addDrop(event, new ItemStack(Items.EMERALD, 1));
                } else if (victim instanceof Enemy && rand.nextInt(35) < 3) {
                    addDrop(event, new ItemStack(ForbiddenContent.item("emerald_nugget"), 1));
                }
            }
        }

        // 4. Nether Shards Matrix
        if (level.dimension() == Level.NETHER) {
            boolean isRealPlayerKill = event.isRecentlyHit()
                && event.getSource().getEntity() instanceof ServerPlayer
                && !(event.getSource().getEntity() instanceof FakePlayer);

            // Passive sloth shard drop
            if (!isRealPlayerKill && rand.nextInt(30) < 4) {
                addDrop(event, new ItemStack(ForbiddenContent.item("sloth_shard"), 1 + rand.nextInt(3)));
            }

            // Active player kill in Nether
            if (isRealPlayerKill) {
                ServerPlayer player = (ServerPlayer) event.getSource().getEntity();
                if (victim instanceof Enemy) {
                    int wrath = 2;
                    int greed = 0;
                    ItemStack held = player.getMainHandItem();
                    if (!held.isEmpty()) {
                        wrath += (int) player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1;

                        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                        wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.SHARPNESS));
                        wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.POWER));
                        if (!victim.fireImmune()) {
                            wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.FIRE_ASPECT));
                            wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.FLAME));
                        }
                        if (victim.isInvertedHealAndHarm()) {
                            wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.SMITE));
                        }
                        if (victim.getType().is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)) {
                            wrath += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.BANE_OF_ARTHROPODS));
                        }
                        greed += held.getEnchantmentLevel(registry.getOrThrow(Enchantments.LOOTING));
                    }

                    // Wrath roll
                    if (rand.nextInt(61) <= wrath) {
                        addDrop(event, new ItemStack(ForbiddenContent.item("wrath_shard")));
                    }

                    // Boss pride roll
                    if (victim.getType().is(Tags.EntityTypes.BOSSES)) {
                        addDrop(event, new ItemStack(ForbiddenContent.item("pride_shard"), 2 + rand.nextInt(1 + getLootingLevel(event))));
                    }

                    // Greed roll
                    if (greed > 0 && rand.nextInt(20) <= greed) {
                        addDrop(event, new ItemStack(ForbiddenContent.item("greed_shard")));
                    }
                }

                // Envy shard on Zombified Piglin holding envy shard
                if (victim instanceof ZombifiedPiglin piglin) {
                    ItemStack offHand = piglin.getMainHandItem();
                    if (offHand.is(ForbiddenContent.item("envy_shard"))) {
                        addDrop(event, new ItemStack(ForbiddenContent.item("envy_shard")));
                    }
                }
            }
        }
    }

    private static int getLootingLevel(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            var registry = attacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return attacker.getMainHandItem().getEnchantmentLevel(registry.getOrThrow(Enchantments.LOOTING));
        }
        return 0;
    }

    private static boolean isTaintedMob(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getNamespace().equals("thaumcraft") && id.getPath().contains("taint");
    }

    private static void addDrop(LivingDropsEvent event, ItemStack drop) {
        LivingEntity victim = event.getEntity();
        ItemEntity entity = new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), drop);
        entity.setDefaultPickUpDelay();
        event.getDrops().add(entity);
    }

    // ==========================================
    // B29: Educational (3x XP Multiplier)
    // ==========================================

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        Player attackingPlayer = event.getAttackingPlayer();
        if (attackingPlayer != null) {
            ItemStack weapon = attackingPlayer.getMainHandItem();
            int edu = DarkEnchantments.getLevel(weapon, DarkEnchantments.EDUCATIONAL);
            var registry = attackingPlayer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int looting = weapon.getEnchantmentLevel(registry.getOrThrow(Enchantments.LOOTING));
            if (edu > 0 && looting == 0) {
                int base = event.getOriginalExperience();
                event.setDroppedExperience(event.getDroppedExperience() + 3 * base * edu);
            }
        }
    }

    // ==========================================
    // B29: Voidtouched (Durability Repair Every 10 Ticks)
    // ==========================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // Morph tools already repair in their native inventoryTick (including non-player holders).
            if (stack.getItem() instanceof MorphAxeItem || stack.getItem() instanceof MorphPickaxeItem
                    || stack.getItem() instanceof MorphShovelItem || stack.getItem() instanceof MorphSwordItem) continue;
            if (!stack.isEmpty() && stack.isDamaged() && DarkEnchantments.getLevel(stack, DarkEnchantments.VOIDTOUCHED) > 0) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    // ==========================================
    // B30: Gluttony Shard on Eat
    // ==========================================

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity instanceof Player player && event.getItem().has(DataComponents.FOOD)) {
            if (player.level().dimension() == Level.NETHER
                && !event.getItem().is(ForbiddenContent.item("gluttony_shard"))
                && player.getRandom().nextInt(10) < 2) {
                ItemEntity ent = new ItemEntity(
                    player.level(), player.getX(), player.getY() + 0.5, player.getZ(),
                    new ItemStack(ForbiddenContent.item("gluttony_shard"))
                );
                var rand = player.getRandom();
                ent.setDeltaMovement(
                    (rand.nextFloat() - rand.nextFloat()) * 0.1,
                    rand.nextFloat() * 0.05,
                    (rand.nextFloat() - rand.nextFloat()) * 0.1
                );
                player.level().addFreshEntity(ent);
            }
        }
    }

    // ==========================================
    // B30: Envy Shard Spawn on Zombified Piglin
    // ==========================================

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().getLevel().dimension() == Level.NETHER
            && event.getEntity() instanceof ZombifiedPiglin piglin
            && event.getLevel().getRandom().nextInt(175) == 1) {
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ForbiddenContent.item("envy_shard")));
        }
    }

    // ==========================================
    // B30: Tooltips
    // ==========================================

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (WandApi.kind(stack) != WandApi.Kind.NONE) {
            var parts = WandApi.parts(stack);
            if (parts != null && parts.rod() != null) {
                if (parts.rod().contains("neutronium")) {
                    event.getToolTip().add(Component.translatable("tooltip.creativeonly").withStyle(ChatFormatting.DARK_PURPLE));
                }
                var customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (parts.rod().contains("blood") && customData.contains("ownerName")) {
                    event.getToolTip().add(Component.translatable("tooltip.currentowner").append(" ").append(customData.getString("ownerName")).withStyle(ChatFormatting.RED));
                }
                if (parts.rod().contains("profane")) {
                    event.getToolTip().add(Component.literal("Contract: " + customData.getInt("contract")).withStyle(ChatFormatting.DARK_RED));
                }
            }
        }
    }



    private ForbiddenEvents() {}
}
