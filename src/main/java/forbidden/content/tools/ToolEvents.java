package forbidden.content.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.CuriosApi;
import forbidden.ForbiddenMagic;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.CageMobCatalog;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenTools;
import forbidden.content.baubles.SubCollarItem;

/** Event subscribers handling tools and accessories mechanics. */
@EventBusSubscriber(modid = ForbiddenMagic.ID)
public final class ToolEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (stack.is(ForbiddenTools.TAINT_PICKAXE.get())) {
            BlockPos pos = event.getPosition().orElse(null);
            float hardness = pos != null
                ? event.getState().getDestroySpeed(event.getEntity().level(), pos)
                : event.getState().getDestroySpeed(event.getEntity().level(), BlockPos.ZERO);
            event.setNewSpeed(TaintPickaxeItem.calculateTaintPickSpeed(hardness));
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        BlockState state = event.getState();
        if (tool.is(ForbiddenTools.TAINT_SHOVEL.get())
            && TaintShovelItem.isTaintMaterial(state)
            && event.getDrops().isEmpty()
            && !state.is(ForbiddenContent.TAINT_LEAVES.get())) {
            var registry = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int fortune = tool.getEnchantmentLevel(registry.getOrThrow(Enchantments.FORTUNE));
            boolean silkTouch = tool.getEnchantmentLevel(registry.getOrThrow(Enchantments.SILK_TOUCH)) > 0;
            int chance = 1 + 2 * fortune + (silkTouch ? 6 : 0);
            if (event.getLevel().getRandom().nextInt(31) <= chance) {
                BlockPos pos = event.getPos();
                event.getDrops().add(new ItemEntity(
                    event.getLevel(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    new ItemStack(ForbiddenContent.item("taint_shard"))
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.isRecentlyHit()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ItemStack weap = player.getMainHandItem();
        if (!weap.is(ForbiddenTools.SKULL_AXE.get())) return;

        var server = player.serverLevel();
        var registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int looting = weap.getEnchantmentLevel(registry.getOrThrow(Enchantments.LOOTING));
        RandomSource rand = server.getRandom();
        Entity victim = event.getEntity();
        Item skullItem = null;
        ItemStack customSkull = null;

        if (victim instanceof WitherSkeleton) {
            if (rand.nextInt(26) <= 3 + looting) {
                skullItem = Items.WITHER_SKELETON_SKULL;
            }
        } else if (victim instanceof Skeleton) {
            if (rand.nextInt(26) <= 3 + looting) {
                skullItem = Items.SKELETON_SKULL;
            }
        } else if (victim.getType() == EntityType.ZOMBIE) {
            if (rand.nextInt(26) <= 2 + 2 * looting) {
                skullItem = Items.ZOMBIE_HEAD;
            }
        } else if (victim instanceof Creeper) {
            if (rand.nextInt(26) <= 2 + 2 * looting) {
                skullItem = Items.CREEPER_HEAD;
            }
        } else if (victim instanceof Player victimPlayer) {
            if (rand.nextInt(11) <= 1 + looting) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, new ResolvableProfile(victimPlayer.getGameProfile()));
                customSkull = head;
            }
        }

        // The release adds this bonus independently of natural head drops.
        if (customSkull != null) {
            event.getDrops().add(new ItemEntity(server, victim.getX(), victim.getY() + victim.getEyeHeight(), victim.getZ(), customSkull));
        } else if (skullItem != null) {
            event.getDrops().add(new ItemEntity(server, victim.getX(), victim.getY() + victim.getEyeHeight(), victim.getZ(), new ItemStack(skullItem)));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        ItemStack weap = player.getMainHandItem();
        if (!weap.is(ForbiddenTools.DIABOLIST_FORK.get())) return;

        LivingEntity victim = event.getEntity();
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        boolean isBoss = victim.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || victim instanceof EnderDragon || victim instanceof WitherBoss;
        boolean allowed = CageMobCatalog.entries().containsKey(mobId) || (ForbiddenConfig.CRIES_HAVOC.get() && !isBoss);
        if (allowed && ForbiddenContent.MOB_CRYSTAL != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(ForbiddenContent.MOB_CRYSTAL.get()) && !stack.has(ForbiddenComponents.MOB.get())) {
                    ForbiddenComponents.mob(player.level(), stack, mobId);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (ForbiddenConfig.NO_LUST.get()) return;
        if (event.getNewDamage() <= 0.0F) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (ForbiddenTools.SUB_COLLAR == null) return;

        var optInv = CuriosApi.getCuriosInventory(player);
        if (optInv.isEmpty()) return;
        var curios = optInv.get().findCurios(ForbiddenTools.SUB_COLLAR.get());
        if (curios.isEmpty()) return;

        ItemStack collarStack = curios.getFirst().stack();
        int doses = 3 * (int) event.getNewDamage();

        if (event.getSource().getEntity() instanceof ServerPlayer dom) {
            int chance = 1;
            if (dom.getMainHandItem().is(ForbiddenTools.RIDING_CROP.get())) {
                doses += 3;
                chance += 3;
            }
            if (player.level().dimension() == Level.NETHER && player.getRandom().nextInt(30) < chance) {
                ItemEntity ent = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    new ItemStack(ForbiddenContent.item("lust_shard"))
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

        if (doses > 0) {
            SubCollarItem.addVis(player.level(), collarStack, doses);
        }
    }

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getItem().has(DataComponents.FOOD)) {
            boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(inv -> !inv.findCurios(ForbiddenTools.RING_NUTRITION.get()).isEmpty())
                .orElse(false);
            if (hasRing) {
                player.getFoodData().eat(2, 2.0F);
            }
        }
    }

    private ToolEvents() {}
}
