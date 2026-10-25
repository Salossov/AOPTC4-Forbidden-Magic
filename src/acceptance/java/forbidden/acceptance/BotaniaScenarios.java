package forbidden.acceptance;

import forbidden.compat.botania.BotaniaBridge;
import forbidden.compat.botania.flowers.*;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import vazkii.botania.api.mana.*;

/** Real Botania block capabilities in a live server, loaded only when Botania is present. */
final class BotaniaScenarios {
    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var poolPos = new BlockPos(4, 91, 4);
        var spreaderPos = new BlockPos(6, 91, 4);
        level.setBlockAndUpdate(poolPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("botania:mana_pool")).defaultBlockState());
        level.setBlockAndUpdate(spreaderPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("botania:mana_spreader")).defaultBlockState());
        var pool = ManaReceiver.LOOKUP.find(level, poolPos, null);
        var collector = ManaReceiver.LOOKUP.find(level, spreaderPos, null);
        check.accept("Botania_live_pool_capability", pool instanceof ManaPool);
        check.accept("Botania_live_collector_capability", collector instanceof ManaCollector);
        pool.receiveMana(10000);
        var whisperPos = poolPos.offset(0, 0, 2);
        level.setBlockAndUpdate(whisperPos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(whisperPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:whisperweed")).defaultBlockState());
        var whisper = (WhisperweedBlockEntity) level.getBlockEntity(whisperPos);
        whisper.setBindingPos(poolPos);
        int before = pool.getCurrentMana();
        whisper.tick(level, whisperPos, level.getBlockState(whisperPos));
        check.accept("Botania_whisper_draws_exactly_6000", before - pool.getCurrentMana() == 6000 && whisper.getMana() == 6000);
        var thistlePos = spreaderPos.offset(0, 0, 2);
        level.setBlockAndUpdate(thistlePos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(thistlePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("forbidden:tainthistle")).defaultBlockState());
        var thistle = (TainthistleBlockEntity) level.getBlockEntity(thistlePos);
        thistle.setBindingPos(spreaderPos);
        thistle.setBurnTime(12);
        int initial = collector.getCurrentMana();
        for (int n = 0; n < 12; n++) thistle.tick(level, thistlePos, level.getBlockState(thistlePos));
        check.accept("Botania_thistle_emits_18_to_real_collector", collector.getCurrentMana() - initial == 18 && thistle.getMana() == 0 && thistle.getBurnTime() == 0);
        check.accept("Botania_radius_lookup_real_pool", BotaniaBridge.findClosestPool(level, whisperPos, 10).equals(poolPos));
        check.accept("Botania_radius_lookup_real_collector", BotaniaBridge.findClosestCollector(level, thistlePos, 6).equals(spreaderPos));
        // This is a capability/transfer check; no simulated GPU/rendering or player knowledge claim.
    }
    static void verifyPersisted(ServerLevel level, BiConsumer<String, Boolean> check) {
        var poolPos = new BlockPos(4, 91, 4);
        var spreaderPos = new BlockPos(6, 91, 4);
        var whisper = (WhisperweedBlockEntity) level.getBlockEntity(poolPos.offset(0, 0, 2));
        var thistle = (TainthistleBlockEntity) level.getBlockEntity(spreaderPos.offset(0, 0, 2));
        check.accept("Botania_saved_whisper_mana", whisper != null && whisper.getMana() == 6000);
        check.accept("Botania_saved_whisper_binding", poolPos.equals(whisper.getBindingPos()));
        check.accept("Botania_saved_thistle_burn_and_mana", thistle != null && thistle.getBurnTime() == 0 && thistle.getMana() == 0);
        check.accept("Botania_saved_thistle_binding", spreaderPos.equals(thistle.getBindingPos()));
        var pool = ManaReceiver.LOOKUP.find(level, poolPos, null);
        var collector = ManaReceiver.LOOKUP.find(level, spreaderPos, null);
        check.accept("Botania_saved_native_pool_mana", pool != null && pool.getCurrentMana() == 4000);
        check.accept("Botania_saved_native_collector_mana", collector != null && collector.getCurrentMana() == 18);
    }
    private BotaniaScenarios() { }
}
