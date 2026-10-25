package forbidden.acceptance;

import forbidden.config.ForbiddenConfig;
import forbidden.enchantments.DarkEnchantments;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Registered Impact event-bus scenarios in a real dedicated-server level. */
final class ImpactAcceptanceScenarios {
    private static final BlockPos CENTER = new BlockPos(4, 90, 4);

    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var impactHolder = enchantments.get(DarkEnchantments.IMPACT);
        check.accept("impact_enchantment_registered", impactHolder.isPresent() && ForbiddenConfig.ENCHANTING.get());
        if (impactHolder.isEmpty() || !ForbiddenConfig.ENCHANTING.get()) return;

        ServerPlayer player = FakePlayerFactory.getMinecraft(level);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;

        // Keep this event-path check independent from MorphPickaxeItem's separate mineBlock callback.
        var impactPick = new ItemStack(Items.DIAMOND_PICKAXE);
        impactPick.enchant(impactHolder.orElseThrow(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, impactPick);

        facePlane(level, player, check, Direction.UP, "up");
        facePlane(level, player, check, Direction.NORTH, "north");
        facePlane(level, player, check, Direction.EAST, "east");

        // The click event is still posted, but a tool without Impact must not mine neighbors.
        var noImpactCenter = new BlockPos(4, 93, 4);
        fillPlane(level, noImpactCenter, Direction.UP);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        postBreak(player, noImpactCenter, Direction.UP);
        check.accept("impact_missing_enchantment_keeps_neighbors", countSolidNeighbors(level, noImpactCenter, Direction.UP) == 8);

        // Impact on a shovel is filtered by the vanilla correct-tool-for-drops check for stone.
        var wrongToolCenter = new BlockPos(10, 90, 4);
        fillPlane(level, wrongToolCenter, Direction.UP);
        var impactShovel = new ItemStack(WorldScenarios.item("morph_shovel"));
        impactShovel.enchant(impactHolder.orElseThrow(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, impactShovel);
        postBreak(player, wrongToolCenter, Direction.UP);
        check.accept("impact_incorrect_tool_keeps_neighbors", countSolidNeighbors(level, wrongToolCenter, Direction.UP) == 8);

        // Air cells are skipped while the seven stone neighbors in the plane are mined.
        var airCenter = new BlockPos(16, 90, 4);
        fillPlane(level, airCenter, Direction.UP);
        var airCell = airCenter.offset(-1, 0, -1);
        level.setBlockAndUpdate(airCell, Blocks.AIR.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, impactPick.copy());
        postBreak(player, airCenter, Direction.UP);
        check.accept("impact_air_cell_ignored", level.getBlockState(airCell).isAir()
            && countSolidNeighbors(level, airCenter, Direction.UP) == 0);

        // Adventure mode makes adjacent blocks protected unless explicitly listed as breakable.
        var protectedCenter = new BlockPos(22, 90, 4);
        fillPlane(level, protectedCenter, Direction.UP);
        player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
        postBreak(player, protectedCenter, Direction.UP);
        check.accept("impact_adventure_protected_neighbors_retained", countSolidNeighbors(level, protectedCenter, Direction.UP) == 8);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.getInventory().clearContent();
    }

    private static void facePlane(ServerLevel level, ServerPlayer player, BiConsumer<String, Boolean> check,
                                  Direction face, String label) {
        var center = face == Direction.UP ? CENTER
            : face == Direction.NORTH ? new BlockPos(12, 90, 4)
            : new BlockPos(20, 90, 4);
        fillPlane(level, center, face);
        BlockPos outOfRange = face == Direction.EAST ? center.offset(0, 2, 0) : center.offset(2, 0, 0);
        level.setBlockAndUpdate(outOfRange, Blocks.STONE.defaultBlockState());
        check.accept("impact_" + label + "_outside_fixture_placed",
            level.getBlockState(outOfRange).is(Blocks.STONE));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.getMainHandItem().enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(DarkEnchantments.IMPACT), 1);
        postBreak(player, center, face);
        check.accept("impact_" + label + "_plane_cleared", countSolidNeighbors(level, center, face) == 0);
        check.accept("impact_" + label + "_outside_3x3_retained", level.getBlockState(outOfRange).is(Blocks.STONE));
    }

    private static void fillPlane(ServerLevel level, BlockPos center, Direction face) {
        forEachNeighbor(center, face, pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
        level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
    }

    private static void postBreak(ServerPlayer player, BlockPos center, Direction face) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(
            player, center, face, PlayerInteractEvent.LeftClickBlock.Action.START));
        player.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        player.gameMode.destroyBlock(center);
    }

    private static int countSolidNeighbors(ServerLevel level, BlockPos center, Direction face) {
        var count = new int[1];
        forEachNeighbor(center, face, pos -> {
            if (!level.getBlockState(pos).isAir()) count[0]++;
        });
        return count[0];
    }

    private static void forEachNeighbor(BlockPos center, Direction face, java.util.function.Consumer<BlockPos> action) {
        int xOffset = face.getStepX() == 0 ? 1 : 0;
        int yOffset = face.getStepY() == 0 ? 1 : 0;
        int zOffset = face.getStepZ() == 0 ? 1 : 0;
        for (int dx = -xOffset; dx <= xOffset; dx++) {
            for (int dy = -yOffset; dy <= yOffset; dy++) {
                for (int dz = -zOffset; dz <= zOffset; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) action.accept(center.offset(dx, dy, dz));
                }
            }
        }
    }

    private ImpactAcceptanceScenarios() {}
}
