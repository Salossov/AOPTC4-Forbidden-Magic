package forbidden.acceptance;

import forbidden.config.ForbiddenConfig;
import forbidden.enchantments.DarkEnchantments;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Deterministic server event-bus checks for the combat enchantments and Voidtouched. */
final class EnchantmentAcceptanceScenarios {
    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // Voidtouched must repair a regular damaged item once per ten server ticks.
        player.getInventory().clearContent();
        var voidtouched = new ItemStack(Items.DIAMOND_PICKAXE);
        voidtouched.enchant(enchantments.getOrThrow(DarkEnchantments.VOIDTOUCHED), 1);
        voidtouched.setDamageValue(40);
        var plainTool = new ItemStack(Items.IRON_PICKAXE);
        plainTool.setDamageValue(40);
        player.getInventory().setItem(4, voidtouched);
        player.getInventory().setItem(5, plainTool);
        player.tickCount = 19;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        check.accept("VOIDTOUCHED_regular_tool_no_early_repair", player.getInventory().getItem(4).getDamageValue() == 40);
        player.tickCount = 20;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        check.accept("VOIDTOUCHED_regular_tool_repairs_one", player.getInventory().getItem(4).getDamageValue() == 39);
        check.accept("VOIDTOUCHED_unenchanted_tool_unchanged", player.getInventory().getItem(5).getDamageValue() == 40);

        // Wrath adds 1.25 damage per level only when the victim is hostile.
        player.getInventory().clearContent();
        var wrathWeapon = new ItemStack(Items.DIAMOND_SWORD);
        wrathWeapon.enchant(enchantments.getOrThrow(DarkEnchantments.WRATH), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrathWeapon);
        var hostile = EntityType.ZOMBIE.create(level);
        var hostileDamage = new LivingDamageEvent.Pre(hostile,
            new DamageContainer(player.damageSources().playerAttack(player), 4.0F));
        NeoForge.EVENT_BUS.post(hostileDamage);
        check.accept("WRATH_hostile_damage_adds_1_25_per_level", Math.abs(hostileDamage.getNewDamage() - 6.5F) < 0.001F);
        var passive = EntityType.SHEEP.create(level);
        var passiveDamage = new LivingDamageEvent.Pre(passive,
            new DamageContainer(player.damageSources().playerAttack(player), 4.0F));
        NeoForge.EVENT_BUS.post(passiveDamage);
        check.accept("WRATH_passive_damage_unchanged", Math.abs(passiveDamage.getNewDamage() - 4.0F) < 0.001F);

        // Greedy's Villager output is deterministic; Looting suppresses the enchantment.
        player.getInventory().clearContent();
        check.accept("GREEDY_config_enabled", ForbiddenConfig.GREEDY.get());
        var greedyWeapon = new ItemStack(Items.DIAMOND_SWORD);
        greedyWeapon.enchant(enchantments.getOrThrow(DarkEnchantments.GREEDY), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, greedyWeapon);
        var villager = EntityType.VILLAGER.create(level);
        var villagerDrops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(villager,
            player.damageSources().playerAttack(player), villagerDrops, true));
        long emeralds = villagerDrops.stream().filter(drop -> drop.getItem().is(Items.EMERALD)).count();
        check.accept("GREEDY_villager_drops_one_emerald", emeralds == 1);
        villagerDrops.forEach(ItemEntity::discard);

        greedyWeapon.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 1);
        var lootingVillager = EntityType.VILLAGER.create(level);
        var lootingDrops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(lootingVillager,
            player.damageSources().playerAttack(player), lootingDrops, true));
        check.accept("GREEDY_looting_suppresses_emerald", lootingDrops.stream()
            .noneMatch(drop -> drop.getItem().is(Items.EMERALD)));
        lootingDrops.forEach(ItemEntity::discard);

        // Educational adds three times the original XP per level, except with Looting.
        player.getInventory().clearContent();
        var educationalWeapon = new ItemStack(Items.DIAMOND_SWORD);
        educationalWeapon.enchant(enchantments.getOrThrow(DarkEnchantments.EDUCATIONAL), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, educationalWeapon);
        var educationalVictim = EntityType.ZOMBIE.create(level);
        var educationalXp = new LivingExperienceDropEvent(educationalVictim, player, 5);
        NeoForge.EVENT_BUS.post(educationalXp);
        check.accept("EDUCATIONAL_adds_three_times_original_per_level", educationalXp.getDroppedExperience() == 35);

        educationalWeapon.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 1);
        var lootingVictim = EntityType.ZOMBIE.create(level);
        var lootingXp = new LivingExperienceDropEvent(lootingVictim, player, 5);
        NeoForge.EVENT_BUS.post(lootingXp);
        check.accept("EDUCATIONAL_looting_suppresses_bonus", lootingXp.getDroppedExperience() == 5);
        player.getInventory().clearContent();
    }

    private EnchantmentAcceptanceScenarios() { }
}
