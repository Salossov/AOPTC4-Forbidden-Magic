package forbidden.compat.bloodmagic.items;

import forbidden.compat.bloodmagic.BloodMagicBridge;
import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.bloodmagic.BloodMagicItems;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;

/**
 * Blood Rapier: Rapier of the Strix (C07).
 * Preserves the release's low sword damage, player LP drain and self-repair.
 * Base class safe for ClassLoader when Blood Magic is not installed.
 */
public class BaseBloodRapierItem extends SwordItem implements IRepairable, IWarpingGear {

    public static final Tier BLOODSUCKER_TIER = new Tier() {
        @Override public int getUses() { return 700; }
        @Override public float getSpeed() { return 10.0F; }
        @Override public float getAttackDamageBonus() { return -3.0F; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:void_ingot"));
            if (item != Items.AIR) return Ingredient.of(item);
            return Ingredient.of(ItemTags.create(ResourceLocation.parse("c:ingots/void")));
        }
    };

    public BaseBloodRapierItem() {
        // Release ItemSword used 4 + material damage (-3), plus player base 1.
        super(BLOODSUCKER_TIER, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(BLOODSUCKER_TIER, 4, -2.0F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        victim.setDeltaMovement(victim.getDeltaMovement().multiply(1.0, 0.8, 1.0));
        if (victim.invulnerableTime > 18) {
            victim.invulnerableTime -= 5;
        }

        // The release drains player victims only; ordinary mobs grant no LP.
        if (attacker instanceof Player && BloodMagicCompat.isLoaded()) {
            if (victim instanceof Player victimPlayer) {
                int lp = BloodMagicBridge.getPlayerLP(victimPlayer);
                int drain = Math.max(4000, lp / 4);
                BloodMagicBridge.drainVictimLP(victimPlayer, drain);
                if (BloodMagicItems.BLOOD_SEAL != null && BloodMagicItems.BLOOD_SEAL.isBound()) {
                    victimPlayer.addEffect(new MobEffectInstance(BloodMagicItems.BLOOD_SEAL, 1200));
                }
            }
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && stack.isDamaged() && entity.tickCount % 20 == 0 && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 2;
    }
}
