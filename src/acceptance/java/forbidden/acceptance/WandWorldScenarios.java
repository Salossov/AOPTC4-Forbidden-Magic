package forbidden.acceptance;

import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.*;

/** Native wand inventory callbacks, registered event handlers and saved gameplay storage. */
final class WandWorldScenarios {
    static void prepare(ServerLevel level, ChestBlockEntity chest, BiConsumer<String, Boolean> check) {
        var player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        player.setPos(8.5, 84, 8.5);
        var tainted = wand("tainted");
        check.accept("V24_tainted_native_capacity", WandApi.capacity(tainted) == 15000);
        tick(tainted, player, 100);
        check.accept("V24_tainted_no_charge_in_plains", total(tainted) == 0);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(
            ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:tainted_land")));
        var chunk = level.getChunkAt(player.blockPosition());
        chunk.fillBiomesFromNoise((x,y,z,sampler) -> biome,level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        check.accept("V24_fixture_real_taint_biome", level.getBiome(player.blockPosition()).is(ResourceLocation.parse("thaumcraft:tainted_land")));
        tick(tainted, player, 50);
        check.accept("V24_tainted_no_charge_at_fifty", total(tainted) == 0);
        tick(tainted, player, 100);
        check.accept("V24_tainted_one_whole_vis_per_primal", Aspects.PRIMALS.stream().allMatch(a -> WandApi.stored(tainted,a) == 100));
        for (var a : Aspects.PRIMALS) WandApi.set(tainted,a,1499);
        tick(tainted, player, 200);
        check.accept("V24_tainted_last_step_can_cross_tenth", Aspects.PRIMALS.stream().allMatch(a -> WandApi.stored(tainted,a) == 1599));
        tick(tainted, player, 300);
        check.accept("V24_tainted_stops_above_tenth", Aspects.PRIMALS.stream().allMatch(a -> WandApi.stored(tainted,a) == 1599));
        for (var a : Aspects.PRIMALS) WandApi.set(tainted,a,0);
        player.setItemInHand(InteractionHand.MAIN_HAND,tainted);
        var taint = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(
            ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumcraft:taint"))));
        NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(player,new DamageContainer(taint,4f)));
        check.accept("V24_taint_damage_three_whole_vis_total", total(tainted) == 300);

        var infernal = wand("infernal");
        player.setRemainingFireTicks(100);
        player.addEffect(new MobEffectInstance(MobEffects.WITHER,100));
        tick(infernal,player,99);
        check.accept("V24_protection_without_charge_tick", player.getRemainingFireTicks() == 0 && !player.hasEffect(MobEffects.WITHER) && total(infernal) == 0);
        player.setItemInHand(InteractionHand.MAIN_HAND,infernal);
        for (var source : java.util.List.of(player.damageSources().inFire(),player.damageSources().wither())) {
            var damage=new LivingIncomingDamageEvent(player,new DamageContainer(source,4f));
            NeoForge.EVENT_BUS.post(damage);
            check.accept("V24_infernal_cancels_"+source.getMsgId(),damage.isCanceled());
        }
        var normalDamage=new LivingIncomingDamageEvent(player,new DamageContainer(player.damageSources().generic(),4f));
        NeoForge.EVENT_BUS.post(normalDamage);
        check.accept("V24_infernal_keeps_ordinary_damage",!normalDamage.isCanceled());
        tick(infernal,player,100);
        var ignis = Aspect.getAspect("ignis");
        check.accept("V24_overworld_only_ignis_one_vis", total(infernal) == 100 && WandApi.stored(infernal,ignis) == 100);
        WandApi.set(infernal,ignis,2999);
        tick(infernal,player,200);
        check.accept("V24_ignis_last_step_crosses_fifth", WandApi.stored(infernal,ignis) == 3099);
        tick(infernal,player,300);
        check.accept("V24_ignis_stops_above_fifth", WandApi.stored(infernal,ignis) == 3099);
        var netherPlayer = FakePlayerFactory.getMinecraft(level.getServer().getLevel(Level.NETHER));
        var netherWand = wand("infernal");
        tick(netherWand,netherPlayer,100);
        check.accept("V24_nether_all_primals_one_vis", Aspects.PRIMALS.stream().allMatch(a -> WandApi.stored(netherWand,a) == 100));

        var contract = wand("profane");
        for (var a : Aspects.PRIMALS) WandApi.set(contract,a,5000);
        tick(contract,player,20);
        check.accept("V24_contract_grants_actual_research", ResearchApi.knows(player,"forbidden:rod_profane"));
        check.accept("V24_initial_contract_does_not_spend_on_full_wand", contract(contract) == 25000);
        for (var a : Aspects.PRIMALS) WandApi.set(contract,a,2500);
        tick(contract,player,40);
        check.accept("V24_contract_charges_exact_deficit", contract(contract) == 10000 && total(contract) == 30000);
        chest.setItem(4,contract.copy());
        for (var a : Aspects.PRIMALS) WandApi.set(contract,a,0);
        int warp = ResearchApi.knowledge(player).stickyWarp();
        tick(contract,player,60);
        check.accept("V24_contract_conserves_last_ten_thousand", contract(contract) == 0 && total(contract) == 10000);
        check.accept("V24_contract_transforms_to_profaned", WandApi.parts(contract).rod().equals("forbidden:profaned"));
        check.accept("V24_contract_adds_sticky_warp", ResearchApi.knowledge(player).stickyWarp() > warp);
        tick(contract,player,80);
        check.accept("V24_exhausted_rod_does_not_refill", total(contract) == 10000 && contract(contract) == 0);
        chest.setItem(5,contract.copy()); chest.setChanged();

        var creative = wand("neutronium");
        tick(creative,player,1);
        check.accept("V24_creative_fills_all_primals", Aspects.PRIMALS.stream().allMatch(a -> WandApi.stored(creative,a) == WandApi.capacity(creative)));

        var hellfire = wand("infernal");
        player.setItemInHand(InteractionHand.MAIN_HAND,hellfire);
        var pos = new BlockPos(24,84,0);
        level.setBlockAndUpdate(pos,Blocks.NETHERRACK.defaultBlockState());
        WandApi.set(hellfire,ignis,1000);
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_requires_research", level.getBlockState(pos.above()).isAir() && WandApi.stored(hellfire,ignis) == 1000);
        ResearchApi.grant(player,"forbidden:hellfire");
        int quoted = WandApi.quote(hellfire,player,java.util.Map.of(ignis,200)).get(ignis);
        var fired = click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_researched_native_event_ignites", fired.isCanceled() && level.getBlockState(pos.above()).is(Blocks.FIRE));
        check.accept("V26_hellfire_charges_native_quote", WandApi.stored(hellfire,ignis) == 1000-quoted);
        int afterFire=WandApi.stored(hellfire,ignis);
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_blocked_space_keeps_vis",WandApi.stored(hellfire,ignis) == afterFire);
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        int before = WandApi.stored(hellfire,ignis);
        click(level,player,pos,Direction.NORTH);
        check.accept("V26_hellfire_rejects_side_face", level.getBlockState(pos.above()).isAir() && WandApi.stored(hellfire,ignis) == before);
        WandApi.focus(hellfire,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:focus_fire"))));
        check.accept("V26_fixture_native_focus_attached",!WandApi.focus(hellfire).isEmpty());
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_rejects_attached_focus",level.getBlockState(pos.above()).isAir() && WandApi.stored(hellfire,ignis) == before);
        WandApi.focus(hellfire,ItemStack.EMPTY);
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_rejects_stone",level.getBlockState(pos.above()).isAir() && WandApi.stored(hellfire,ignis) == before);
        level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_accepts_obsidian",level.getBlockState(pos.above()).is(Blocks.FIRE) && WandApi.stored(hellfire,ignis) == before-quoted);
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        WandApi.set(hellfire,ignis,0);
        click(level,player,pos,Direction.UP);
        check.accept("V26_hellfire_rejects_empty_vis", level.getBlockState(pos.above()).isAir());
        player.getInventory().clearContent();
    }
    static void verifyStorage(ServerLevel level, ChestBlockEntity chest, BiConsumer<String, Boolean> check) {
        var partial = chest.getItem(4);
        check.accept("V24_persisted_partial_contract", contract(partial) == 10000 && total(partial) == 30000 && WandApi.parts(partial).rod().equals("forbidden:profane"));
        var exhausted = chest.getItem(5);
        check.accept("V24_persisted_exhausted_contract", contract(exhausted) == 0 && total(exhausted) == 10000 && WandApi.parts(exhausted).rod().equals("forbidden:profaned"));
        var player=FakePlayerFactory.getMinecraft(level);
        check.accept("V62_persisted_profane_research",ResearchApi.knows(player,"forbidden:rod_profane"));
        check.accept("V62_persisted_hellfire_research",ResearchApi.knows(player,"forbidden:hellfire"));
        check.accept("V62_persisted_sticky_warp",ResearchApi.knowledge(player).stickyWarp() > 0);
    }
    private static ItemStack wand(String rod) {
        var result = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:casting_wand")));
        WandApi.parts(result,new WandParts("forbidden:"+rod,"iron"));
        return result;
    }
    private static void tick(ItemStack wand, net.minecraft.server.level.ServerPlayer player, int tick) {
        player.tickCount=tick;
        wand.getItem().inventoryTick(wand,player.level(),player,0,true);
    }
    private static int total(ItemStack wand) { return Aspects.PRIMALS.stream().mapToInt(a -> WandApi.stored(wand,a)).sum(); }
    private static int contract(ItemStack wand) { return wand.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("contract"); }
    private static PlayerInteractEvent.RightClickBlock click(ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, Direction face) {
        var event = new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }
    private WandWorldScenarios() { }
}
