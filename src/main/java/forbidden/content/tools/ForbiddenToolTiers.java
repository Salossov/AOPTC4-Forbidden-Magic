package forbidden.content.tools;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import forbidden.content.ForbiddenContent;

/** Tool tiers for Forbidden Magic equipment. */
public enum ForbiddenToolTiers implements Tier {
    TAINT(1500, 10.0F, 3.0F, 18, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot"));
        if (item != Items.AIR) return Ingredient.of(item);
        return Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")));
    }),
    TAINT_PICKAXE(1500, 10.0F, 3.0F, 18, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, () -> {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot"));
        if (item != Items.AIR) return Ingredient.of(item);
        return Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")));
    }),
    SKULL_AXE(1500, 10.0F, 3.0F, 18, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot"));
        if (item != Items.AIR) return Ingredient.of(item);
        return Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/thaumium")));
    }),
    MORPH(1500, 10.0F, 3.0F, 18, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, () -> Ingredient.of(ForbiddenContent.item("envy_shard"))),
    FORK(400, 7.0F, 2.0F, 22, BlockTags.INCORRECT_FOR_IRON_TOOL, () -> Ingredient.of(Items.NETHER_BRICK));

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final Supplier<Ingredient> repairIngredient;

    ForbiddenToolTiers(int uses, float speed, float attackDamageBonus, int enchantmentValue,
                       TagKey<Block> incorrectBlocksForDrops, Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.repairIngredient = repairIngredient;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return attackDamageBonus; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrectBlocksForDrops; }
    @Override public int getEnchantmentValue() { return enchantmentValue; }
    @Override public Ingredient getRepairIngredient() { return repairIngredient.get(); }
}
