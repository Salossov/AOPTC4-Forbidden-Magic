package forbidden.acceptance;

import forbidden.component.*;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Item methods and the registered player-tick bus on a real dedicated-server level. */
final class ToolWorldScenarios {
    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        player.getAbilities().instabuild = false;
        var center = new BlockPos(8, 86, 0);
        var flux = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:flux_gas"));
        var inside = List.of(center.offset(-5,0,0),center.offset(5,0,0),center.offset(0,-4,0),center.offset(0,4,0),center.offset(0,0,-5),center.offset(0,0,5));
        var outside = List.of(center.offset(-6,0,0),center.offset(6,0,0),center.offset(0,-5,0),center.offset(0,5,0),center.offset(0,0,-6),center.offset(0,0,6));
        for (var pos : inside) level.setBlock(pos, flux.defaultBlockState(), 2);
        for (var pos : outside) level.setBlock(pos, flux.defaultBlockState(), 2);
        var shovel = new ItemStack(WorldScenarios.item("taint_shovel"));
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
        shovel.getItem().useOn(context);
        check.accept("V17_all_six_inclusive_boundaries_clear", inside.stream().allMatch(p -> level.getBlockState(p).isAir()));
        check.accept("V17_all_six_outside_boundaries_retained", outside.stream().allMatch(p -> level.getBlockState(p).is(flux)));
        check.accept("V17_six_blocks_cost_six", shovel.getDamageValue() == 6);
        for (var pos : outside) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 20; x++) level.setBlock(center.offset(x%5, x/5, 0), flux.defaultBlockState(), 2);
        shovel.getItem().useOn(context);
        check.accept("V17_twenty_blocks_cost_capped_at_fifteen", shovel.getDamageValue() == 21);
        check.accept("V17_twenty_blocks_really_removed", BlockPos.betweenClosedStream(center, center.offset(4,3,0)).allMatch(p -> level.getBlockState(p).isAir()));
        shovel.getItem().useOn(context);
        check.accept("V17_empty_scan_does_not_cost_damage", shovel.getDamageValue() == 21);
        var pick = new ItemStack(WorldScenarios.item("taint_pickaxe"));
        check.accept("V18_zero_hardness_speed", pick.getDestroySpeed(Blocks.SHORT_GRASS.defaultBlockState()) == 0f);
        check.accept("V18_soft_block_speed", pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == .1f);
        check.accept("V18_middle_hardness_speed", pick.getDestroySpeed(Blocks.IRON_BLOCK.defaultBlockState()) == 2.5f);
        check.accept("V18_hard_block_speed", pick.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == 55f);
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (String name : List.of("morph_pickaxe","morph_axe","morph_shovel","morph_sword")) {
            var tool = new ItemStack(WorldScenarios.item(name));
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setShiftKeyDown(true);
            for (int phase = 0; phase < 3; phase++) {
                tool.set(DataComponents.CUSTOM_NAME, Component.literal(name + "-" + phase));
                tool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                tool.enchant(registry.getOrThrow(phase == 0 ? Enchantments.UNBREAKING : phase == 1 ? Enchantments.MENDING : Enchantments.VANISHING_CURSE), 1);
                tool.getItem().use(level, player, InteractionHand.MAIN_HAND);
            }
            check.accept("V20_"+name+"_cycles_to_zero", tool.get(ForbiddenComponents.MORPH.get()).phase() == 0);
            for (int phase = 0; phase < 3; phase++) {
                check.accept("V20_"+name+"_restores_name_"+phase, tool.getHoverName().getString().equals(name + "-" + phase));
                check.accept("V20_"+name+"_restores_enchants_"+phase, tool.getEnchantments().getLevel(registry.getOrThrow(phase == 0 ? Enchantments.UNBREAKING : phase == 1 ? Enchantments.MENDING : Enchantments.VANISHING_CURSE)) == 1);
                // Remove Unbreaking before costing the transition: test the fixed base cost, not RNG reduction.
                tool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                tool.getItem().use(level, player, InteractionHand.MAIN_HAND);
            }
            tool.setDamageValue(tool.getMaxDamage() - 5);
            int phase = tool.get(ForbiddenComponents.MORPH.get()).phase();
            tool.getItem().use(level, player, InteractionHand.MAIN_HAND);
            check.accept("V20_"+name+"_refuses_last_five", tool.getDamageValue() == tool.getMaxDamage()-5 && tool.get(ForbiddenComponents.MORPH.get()).phase() == phase);
            if (ForbiddenConfig.ENCHANTING.get()) {
                tool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                tool.enchant(registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse("forbidden:voidtouched"))),1);
                tool.setDamageValue(40); player.tickCount=19;
                tool.getItem().inventoryTick(tool,level,player,0,true);
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                check.accept("V20_"+name+"_no_early_repair", tool.getDamageValue()==40);
                player.tickCount=20;
                tool.getItem().inventoryTick(tool,level,player,0,true);
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                check.accept("V20_"+name+"_one_repair_per_ten_ticks", tool.getDamageValue()==39);
            }
        }
        player.getInventory().clearContent(); player.setShiftKeyDown(false);
        var crop = new ItemStack(WorldScenarios.item("riding_crop"));
        player.setItemInHand(InteractionHand.MAIN_HAND,crop);
        var pig = EntityType.PIG.create(level);
        crop.getItem().hurtEnemy(crop,pig,player);
        check.accept("V22_pig_speed_six",pig.hasEffect(MobEffects.MOVEMENT_SPEED) && pig.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==5);
        var golem = EntityType.IRON_GOLEM.create(level);
        crop.getItem().hurtEnemy(crop,golem,player);
        check.accept("V22_golem_three_buffs",golem.hasEffect(MobEffects.MOVEMENT_SPEED) && golem.hasEffect(MobEffects.DIG_SPEED) && golem.hasEffect(MobEffects.DAMAGE_BOOST));
        // FakePlayer deliberately refuses mounting; use a native ServerPlayer with an inert connection.
        var profile = new GameProfile(UUID.fromString("f355a3bd-238c-413a-97a8-3b0c74d58f82"), "CropAcceptance");
        var rider = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        rider.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), rider,
            new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false));
        rider.setItemInHand(InteractionHand.MAIN_HAND,crop);
        check.accept("V22_native_player_mounts_pig", rider.startRiding(pig,true) && rider.getVehicle() == pig);
        crop.getItem().use(level,rider,InteractionHand.MAIN_HAND);
        check.accept("V22_mounted_use_costs_one",crop.getDamageValue()==3 && pig.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==200);
        rider.stopRiding(); player.getInventory().clearContent();
    }
    private ToolWorldScenarios() { }
}
