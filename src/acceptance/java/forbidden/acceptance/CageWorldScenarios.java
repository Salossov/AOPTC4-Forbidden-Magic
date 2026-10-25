package forbidden.acceptance;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.api.aspects.*;

/** Block interactions, native jar/tube capability flow and spawner decisions in a loaded world. */
final class CageWorldScenarios {
    private static final BlockPos FLOW = new BlockPos(20,84,-5);
    private static final BlockPos TUBE = FLOW.below(), JAR = FLOW.below(2);
    static void prepare(ServerLevel level, BiConsumer<String,Boolean> check) {
        var pos = new BlockPos(0,84,-5);
        level.setBlockAndUpdate(pos,ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        var cage = (WrathCageEntity) level.getBlockEntity(pos);
        var player = FakePlayerFactory.getMinecraft(level);
        var pig = ResourceLocation.parse("minecraft:pig");
        var blaze = ResourceLocation.parse("minecraft:blaze");
        var first = crystal(pig);
        player.setItemInHand(InteractionHand.MAIN_HAND,first);
        click(level,pos,player);
        check.accept("V35_first_crystal_consumed_once", player.getMainHandItem().isEmpty() && pig.equals(cage.mob()));
        cage.setWrath(9); cage.setSloth(11); cage.setSpecial(13); cage.getSpawnerLogic().setFuel(3);
        player.setItemInHand(InteractionHand.MAIN_HAND,crystal(blaze));
        click(level,pos,player);
        check.accept("V35_exchange_returns_old_crystal_once", player.getMainHandItem().getCount() == 1 && pig.equals(player.getMainHandItem().get(ForbiddenComponents.MOB.get())) && blaze.equals(cage.mob()));
        check.accept("V36_exchange_clears_special_and_fuel_only", cage.getSpecial() == 0 && cage.getSpawnerLogic().getFuel() == 0 && cage.getWrath() == 9 && cage.getSloth() == 11);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WorldScenarios.item("diabolist_fork")));
        for (int mode=1; mode<=3; mode++) {
            click(level,pos,player);
            check.accept("V36_fork_mode_"+mode, cage.mode() == mode%3);
        }
        var logic = cage.getSpawnerLogic();
        int cost=ForbiddenConfig.WRATH_COST.get(), efficiency=ForbiddenConfig.WRATH_EFFICIENCY.get();
        cage.setSpecial(cost); cage.setWrath(cost); cage.setSloth(cost);
        logic.setSpawnDelay(100); cage.serverTick();
        check.accept("V37_special_paid_before_ira_and_desidia", cage.getSpecial() == 0 && cage.getWrath() == cost && cage.getSloth() == cost && logic.getFuel() == efficiency && !logic.isSlothful());
        logic.setFuel(0); cage.serverTick();
        check.accept("V37_ira_paid_before_desidia", cage.getWrath() == 0 && cage.getSloth() == cost && logic.getFuel() == efficiency && !logic.isSlothful());
        logic.setFuel(0); cage.serverTick();
        check.accept("V37_desidia_sets_slothful_cycle", cage.getSloth() == 0 && logic.getFuel() == efficiency && logic.isSlothful());
        logic.updateDelay();
        check.accept("V37_desidia_delay_includes_two_hundred", logic.getSpawnDelay() >= 400 && logic.getSpawnDelay() < 500);
        level.setBlockAndUpdate(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        logic.setSpawnDelay(123); cage.serverTick();
        check.accept("V38_redstone_stops_delay_and_fuel", logic.getSpawnDelay() == 123 && logic.getFuel() == efficiency);
        var testPig=EntityType.PIG.create(level);
        var collision=pos.offset(3,0,0);
        level.setBlockAndUpdate(collision,Blocks.STONE.defaultBlockState());
        testPig.setPos(collision.getX()+.5,collision.getY(),collision.getZ()+.5);
        check.accept("V38_collision_rejects_spawn", !logic.entityCanSpawn(testPig));
        level.setBlockAndUpdate(collision,Blocks.WATER.defaultBlockState());
        check.accept("V38_land_mob_rejects_liquid", !logic.entityCanSpawn(testPig));
        level.setBlockAndUpdate(collision,Blocks.AIR.defaultBlockState());
        check.accept("V38_land_mob_accepts_free_space", logic.entityCanSpawn(testPig));
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1),e -> blaze.equals(e.getItem().get(ForbiddenComponents.MOB.get())));
        check.accept("V35_break_drops_single_current_crystal", drops.size() == 1 && drops.getFirst().getItem().getCount() == 1);
        drops.forEach(ItemEntity::discard);
        player.getInventory().clearContent();
        prepareFlow(level,false,check);
    }
    static void prepareFlow(ServerLevel level, boolean free, BiConsumer<String,Boolean> check) {
        level.setBlockAndUpdate(FLOW,ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        level.setBlockAndUpdate(FLOW.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(TUBE,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:essentia_tube")).defaultBlockState());
        level.setBlockAndUpdate(JAR,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar")).defaultBlockState());
        var flow=(WrathCageEntity) level.getBlockEntity(FLOW);
        flow.setMob(ResourceLocation.parse("minecraft:blaze")); flow.cycleMode(); flow.sync();
        var source=level.getCapability(EssentiaCapabilities.CONTAINER,JAR);
        var pipe=level.getCapability(EssentiaCapabilities.TRANSPORT,TUBE,Direction.UP);
        var consumer=level.getCapability(EssentiaCapabilities.TRANSPORT,FLOW,Direction.DOWN);
        check.accept("V36_native_jar_tube_cage_capabilities", source != null && pipe != null && consumer == flow);
        check.accept(free ? "V37_free_cage_has_no_suction" : "V36_native_cage_suction_256_ira", consumer.getSuctionAmount(Direction.DOWN) == (free ? 0 : 256) && consumer.getSuctionType(Direction.DOWN) == (free ? null : Aspect.getAspect("forbidden:ira")));
        check.accept("V36_native_jar_accepts_eight", source.addToContainer(Aspect.getAspect("forbidden:ira"),8) == 0);
    }
    static void afterTicks(ServerLevel level, boolean free, BiConsumer<String,Boolean> check) {
        var cage=(WrathCageEntity) level.getBlockEntity(FLOW);
        var source=level.getCapability(EssentiaCapabilities.CONTAINER,JAR);
        var pipe=level.getCapability(EssentiaCapabilities.TRANSPORT,TUBE,Direction.UP);
        int remaining=source.containerContains(Aspect.getAspect("forbidden:ira"))+pipe.getEssentiaAmount(Direction.UP);
        check.accept("V36_native_flow_conserves_eight", remaining+cage.getWrath() == 8);
        check.accept(free ? "V37_free_cage_does_not_draw_essentia" : "V36_native_flow_reaches_cage", cage.getWrath() == (free ? 0 : 8) && remaining == (free ? 8 : 0));
        check.accept("V36_redstone_keeps_flow_but_stops_spawn", level.hasNeighborSignal(FLOW) && cage.getSpawnerLogic().getSpawnDelay() == 20);
    }
    private static ItemStack crystal(ResourceLocation mob) {
        var stack=new ItemStack(WorldScenarios.item("mob_crystal")); stack.set(ForbiddenComponents.MOB.get(),mob); return stack;
    }
    private static void click(ServerLevel level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
        level.getBlockState(pos).useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private CageWorldScenarios() { }
}
