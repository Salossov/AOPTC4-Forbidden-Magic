package forbidden.acceptance;

import forbidden.content.*;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Deterministic actions in an actual loaded world, plus delayed furnace/neighbor updates. */
final class WorldScenarios {
    private static final BlockPos FURNACE = new BlockPos(8, 81, 2);
    static Item item(String name) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("forbidden:" + name)); }
    static void prepare(ServerLevel level, BiConsumer<String, Boolean> check) {
        var random = RandomSource.create(73519);
        var tree = new BlockPos(8, 80, 6);
        check.accept("V11_tree_grows_with_space", TaintTree.grow(level, tree, random));
        check.accept("V11_tree_has_native_log", level.getBlockState(tree).is(ForbiddenContent.TAINT_LOG.get()));
        var blocked = new BlockPos(20, 80, 6);
        level.setBlockAndUpdate(blocked.above(), Blocks.BEDROCK.defaultBlockState());
        check.accept("V11_tree_refuses_obstruction", !TaintTree.grow(level, blocked, RandomSource.create(73519)));
        var bush = new BlockPos(4, 80, 5);
        DoublePlantBlock.placeAt(level, ForbiddenContent.UMBRAL_BUSH.get().defaultBlockState(), bush, 3);
        check.accept("V12_bush_two_halves", level.getBlockState(bush).is(ForbiddenContent.UMBRAL_BUSH.get()) && level.getBlockState(bush.above()).is(ForbiddenContent.UMBRAL_BUSH.get()));
        level.removeBlock(bush.above(), false);
        check.accept("V12_removing_upper_removes_lower", level.getBlockState(bush).isAir());
        var fake = FakePlayerFactory.getMinecraft(level);
        fake.setShiftKeyDown(false);
        fake.getAbilities().instabuild = false;
        var sheep = EntityType.SHEEP.create(level);
        sheep.setColor(DyeColor.WHITE);
        var dye = new ItemStack(item("black_dye"), 3);
        dye.getItem().interactLivingEntity(dye, fake, sheep, InteractionHand.MAIN_HAND);
        check.accept("V15_ink_dyes_sheep_once", sheep.getColor() == DyeColor.BLACK && dye.getCount() == 2);
        dye.getItem().interactLivingEntity(dye, fake, sheep, InteractionHand.MAIN_HAND);
        check.accept("V15_black_sheep_does_not_consume_again", dye.getCount() == 2);
        sheep.setColor(DyeColor.WHITE); sheep.setSheared(true);
        dye.getItem().interactLivingEntity(dye, fake, sheep, InteractionHand.MAIN_HAND);
        check.accept("V15_sheared_sheep_unchanged", sheep.getColor() == DyeColor.WHITE && dye.getCount() == 2);
        var cakePos = new BlockPos(6, 80, 5);
        level.setBlockAndUpdate(cakePos, ForbiddenContent.ARCANE_CAKE.get().defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(cakePos), Direction.UP, cakePos, false);
        check.accept("V13_cake_has_no_drops", Block.getDrops(level.getBlockState(cakePos), level, cakePos, null, fake, ItemStack.EMPTY).isEmpty());
        fake.getFoodData().setFoodLevel(0);
        for (int n = 0; n < 12; n++) {
            fake.getFoodData().setFoodLevel(0);
            level.getBlockState(cakePos).useWithoutItem(level, fake, hit);
            check.accept("V13_cake_slice_" + n, fake.getFoodData().getFoodLevel() == 2);
        }
        check.accept("V13_cake_disappears_after_twelve_slices", level.getBlockState(cakePos).isAir());
        level.setBlockAndUpdate(cakePos, ForbiddenContent.ARCANE_CAKE.get().defaultBlockState().setValue(ArcaneCakeBlock.BITES, 5));
        level.getBlockState(cakePos).randomTick(level, cakePos, random);
        check.accept("V13_cake_regenerates_slice", level.getBlockState(cakePos).getValue(ArcaneCakeBlock.BITES) == 4);
        level.removeBlock(cakePos.below(), false);
        check.accept("V13_cake_requires_support", level.getBlockState(cakePos).isAir());
        var shovel = new ItemStack(item("taint_shovel"));
        fake.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        var center = new BlockPos(8, 86, 0);
        var goo = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:flux_goo"));
        var gas = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:flux_gas"));
        var inside = center.offset(-5, -4, -5);
        var edge = center.offset(5, 4, 5);
        var outside = center.offset(6, 0, 0);
        level.setBlock(inside, goo.defaultBlockState(), 2);
        level.setBlock(edge, gas.defaultBlockState(), 2);
        level.setBlock(outside, gas.defaultBlockState(), 2);
        check.accept("V17_fixture_flux_registered", goo != Blocks.AIR && gas != Blocks.AIR && level.getBlockState(inside).is(goo) && level.getBlockState(edge).is(gas));
        var shovelContext = new UseOnContext(fake, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
        check.accept("V17_context_center", shovelContext.getClickedPos().equals(center));
        shovel.getItem().useOn(shovelContext);
        check.accept("V17_shovel_clears_inclusive_bounds inside=" + level.getBlockState(inside) + " edge=" + level.getBlockState(edge), level.getBlockState(inside).isAir() && level.getBlockState(edge).isAir());
        check.accept("V17_shovel_preserves_outside_radius", level.getBlockState(outside).is(gas));
        check.accept("V17_shovel_cost_matches_two_blocks", shovel.getDamageValue() == 2);
        // Clean up the fixture directly, independently of the shovel-under-test.
        for (var target : List.of(inside, edge, outside)) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        var stars = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int n = 0; n < 9; n++) stars.set(n, new ItemStack(Items.NETHER_STAR));
        var craft = CraftingInput.of(3, 3, stars);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craft, level);
        check.accept("V14_star_block_crafting", recipe.isPresent() && recipe.get().value().assemble(craft, level.registryAccess()).is(item("star_block")));
        var inverse = CraftingInput.of(1, 1, List.of(new ItemStack(item("star_block"))));
        var unpack = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inverse, level).orElseThrow().value().assemble(inverse, level.registryAccess());
        check.accept("V14_nine_stars_returned", unpack.is(Items.NETHER_STAR) && unpack.getCount() == 9);
        var base = ForbiddenContent.STAR_BLOCK.get().defaultBlockState();
        check.accept("V14_star_block_beacon_tag", base.is(net.minecraft.tags.BlockTags.BEACON_BASE_BLOCKS));
        level.setBlockAndUpdate(FURNACE, Blocks.FURNACE.defaultBlockState());
        var furnace = (FurnaceBlockEntity) level.getBlockEntity(FURNACE);
        furnace.setItem(0, new ItemStack(item("taint_log")));
        furnace.setItem(1, new ItemStack(item("taint_coal"), 64));
        furnace.setChanged();
    }
    static void afterTicks(ServerLevel level, BiConsumer<String, Boolean> check) {
        var furnace = (FurnaceBlockEntity) level.getBlockEntity(FURNACE);
        check.accept("V16_live_furnace_smelts_log_to_coal", furnace != null && furnace.getItem(2).is(item("taint_coal")) && furnace.getItem(0).isEmpty());
        check.accept("V16_native_furnace_consumed_taint_fuel", furnace.getItem(1).getCount() < 64);
    }
    private WorldScenarios() { }
}
