package forbidden.content.tools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import forbidden.config.ForbiddenConfig;
import forbidden.content.ForbiddenContent;

/** Riding Crop: buffs targets/mounts/golems, mounted acceleration, Nether lust shard drops. */
public class RidingCropItem extends SwordItem {
    public RidingCropItem() {
        super(Tiers.WOOD, new Properties().attributes(
            SwordItem.createAttributes(Tiers.WOOD, 1, -2.4F)
        ));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.LEATHER);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        if (victim instanceof AbstractHorse || victim instanceof Pig) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 5));
        } else if (victim instanceof Player || victim instanceof AbstractGolem || victim instanceof thaumcraft.api.entities.IControllableGolem) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        }

        Level level = attacker.level();
        if (!level.isClientSide()
            && !ForbiddenConfig.NO_LUST.get()
            && level.dimension() == Level.NETHER
            && attacker.getRandom().nextInt(15) == 1) {
            var rand = attacker.getRandom();
            ItemEntity ent = new ItemEntity(
                level,
                victim.getX(),
                victim.getY() + 0.5,
                victim.getZ(),
                new ItemStack(ForbiddenContent.item("lust_shard"))
            );
            ent.setDeltaMovement(
                (rand.nextFloat() - rand.nextFloat()) * 0.1,
                rand.nextFloat() * 0.05,
                (rand.nextFloat() - rand.nextFloat()) * 0.1
            );
            level.addFreshEntity(ent);
        }

        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getVehicle() instanceof LivingEntity mount) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            player.swing(hand);
            mount.hurt(player.damageSources().generic(), 1.0F);
            mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 5));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}
