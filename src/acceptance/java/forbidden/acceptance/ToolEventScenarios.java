package forbidden.acceptance;

import com.mojang.authlib.GameProfile;
import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.CageMobCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Native event-bus coverage for the diablist fork and skull axe. */
final class ToolEventScenarios {
    private static final GameProfile PROFILE = new GameProfile(
        UUID.fromString("18fbbdd1-8780-4ef0-b984-d3314def4e3a"), "ToolEventAcceptance");

    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = nativePlayer(level);
        player.getInventory().clearContent();
        player.getInventory().selected = 8;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
            new ItemStack(WorldScenarios.item("diabolist_fork")));

        var firstCrystal = new ItemStack(WorldScenarios.item("mob_crystal"));
        var secondCrystal = firstCrystal.copy();
        player.getInventory().setItem(0, firstCrystal);
        player.getInventory().setItem(1, secondCrystal);
        check.accept("FORK_fixture_weapon_is_fork", player.getMainHandItem().is(WorldScenarios.item("diabolist_fork")));
        check.accept("FORK_fixture_crystal_supplier_present", forbidden.content.ForbiddenContent.MOB_CRYSTAL != null);
        var acceptedMob = CageMobCatalog.entries().keySet().stream().findFirst().orElseThrow();
        var acceptedEntity = BuiltInRegistries.ENTITY_TYPE.get(acceptedMob).create(level);
        check.accept("FORK_catalog_has_acceptance_mob", acceptedEntity instanceof LivingEntity);
        check.accept("FORK_fixture_catalog_entry_registered", CageMobCatalog.entries().containsKey(acceptedMob));
        if (acceptedEntity instanceof LivingEntity acceptedVictim) {
            postDeath(player, acceptedVictim);
            check.accept("FORK_fills_first_empty_crystal", acceptedMob.equals(player.getInventory().getItem(0).get(ForbiddenComponents.MOB.get())));
            check.accept("FORK_leaves_second_empty_crystal", !player.getInventory().getItem(1).has(ForbiddenComponents.MOB.get()));
            postDeath(player, acceptedVictim);
            check.accept("FORK_fills_only_next_empty_crystal", acceptedMob.equals(player.getInventory().getItem(1).get(ForbiddenComponents.MOB.get())));
        }

        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
            new ItemStack(WorldScenarios.item("diabolist_fork")));
        var bossCrystal = new ItemStack(WorldScenarios.item("mob_crystal"));
        player.getInventory().setItem(0, bossCrystal);
        WitherBoss wither = EntityType.WITHER.create(level);
        postDeath(player, wither);
        check.accept("FORK_never_captures_boss", !bossCrystal.has(ForbiddenComponents.MOB.get()));

        if (ForbiddenConfig.CRIES_HAVOC.get()) {
            var customCrystal = new ItemStack(WorldScenarios.item("mob_crystal"));
            player.getInventory().setItem(0, customCrystal);
            LivingEntity nonCatalog = EntityType.SHEEP.create(level);
            ResourceLocation sheepId = BuiltInRegistries.ENTITY_TYPE.getKey(nonCatalog.getType());
            if (!CageMobCatalog.entries().containsKey(sheepId)) {
                postDeath(player, nonCatalog);
                check.accept("FORK_CriesHavoc_allows_non_catalog_non_boss", sheepId.equals(customCrystal.get(ForbiddenComponents.MOB.get())));
            }
        } else {
            var nonCatalogType = List.of(EntityType.SHEEP, EntityType.COW, EntityType.DONKEY).stream()
                .filter(type -> !CageMobCatalog.entries().containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(type)))
                .findFirst().orElseThrow();
            var customCrystal = new ItemStack(WorldScenarios.item("mob_crystal"));
            player.getInventory().setItem(0, customCrystal);
            LivingEntity nonCatalog = nonCatalogType.create(level);
            postDeath(player, nonCatalog);
            check.accept("FORK_without_CriesHavoc_rejects_non_catalog", !customCrystal.has(ForbiddenComponents.MOB.get()));
        }

        player.getInventory().clearContent();
        var axe = new ItemStack(WorldScenarios.item("skull_axe"));
        axe.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 100);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
        List<SkullTarget> targets = List.of(
            new SkullTarget(EntityType.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL, "wither_skeleton"),
            new SkullTarget(EntityType.SKELETON, Items.SKELETON_SKULL, "skeleton"),
            new SkullTarget(EntityType.ZOMBIE, Items.ZOMBIE_HEAD, "zombie"),
            new SkullTarget(EntityType.CREEPER, Items.CREEPER_HEAD, "creeper"));
        for (SkullTarget target : targets) {
            var victim = target.type.create(level);
            var drops = new ArrayList<ItemEntity>();
            NeoForge.EVENT_BUS.post(new LivingDropsEvent(victim, player.damageSources().playerAttack(player), drops, true));
            long heads = drops.stream().filter(drop -> drop.getItem().is(target.skull)).count();
            check.accept("SKULL_guaranteed_" + target.name + "_drop", heads == 1);
            drops.forEach(ItemEntity::discard);
        }

        var recentlyHitFalse = EntityType.SKELETON.create(level);
        var noDrops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(recentlyHitFalse,
            player.damageSources().playerAttack(player), noDrops, false));
        check.accept("SKULL_requires_recent_hit", noDrops.isEmpty());

        var playerVictim = nativePlayer(level);
        var playerDrops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(playerVictim,
            player.damageSources().playerAttack(player), playerDrops, true));
        long playerHeads = playerDrops.stream().filter(drop -> drop.getItem().is(Items.PLAYER_HEAD)).count();
        check.accept("SKULL_guaranteed_player_head", playerHeads == 1);
        check.accept("SKULL_player_head_profile_preserved", playerDrops.stream()
            .filter(drop -> drop.getItem().is(Items.PLAYER_HEAD))
            .anyMatch(drop -> drop.getItem().get(net.minecraft.core.component.DataComponents.PROFILE)
                .gameProfile().getId().equals(playerVictim.getGameProfile().getId())));
        playerDrops.forEach(ItemEntity::discard);

        var duplicateVictim = EntityType.ZOMBIE.create(level);
        var existing = new ItemEntity(level, 0, 0, 0, new ItemStack(Items.ZOMBIE_HEAD));
        var duplicateDrops = new ArrayList<ItemEntity>(List.of(existing));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(duplicateVictim,
            player.damageSources().playerAttack(player), duplicateDrops, true));
        check.accept("SKULL_does_not_duplicate_existing_head", duplicateDrops.size() == 1);
        existing.discard();
        player.getInventory().clearContent();
    }

    private static void postDeath(ServerPlayer player, LivingEntity victim) {
        DamageSource source = player.damageSources().playerAttack(player);
        if (!source.getEntity().equals(player)) throw new AssertionError("Fork event fixture has no player attacker");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, source));
    }

    private static ServerPlayer nativePlayer(ServerLevel level) {
        var player = new ServerPlayer(level.getServer(), level, PROFILE, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(level.getServer(),
            new Connection(PacketFlow.SERVERBOUND), player,
            new CommonListenerCookie(PROFILE, 0, ClientInformation.createDefault(), false));
        return player;
    }

    private record SkullTarget(EntityType<? extends LivingEntity> type, net.minecraft.world.item.Item skull, String name) { }

    private ToolEventScenarios() { }
}
