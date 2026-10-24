package forbidden.compat.bloodmagic.flowers;

import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.bloodmagic.BloodMagicItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Bloodthorn flower BlockEntity (C08).
 * Periodically searches a 6-block radius for living entities (excluding players),
 * inflicts 4.0 magic damage and Weakness III for 60 ticks, and feeds LP to the nearest player's soul network.
 */
public class BloodthornBlockEntity extends BlockEntity {

    public BloodthornBlockEntity(BlockPos pos, BlockState state) {
        super(BloodMagicItems.BLOODTHORN_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || level.getGameTime() % 10 != 0) {
            return;
        }

        AABB box = new AABB(pos).inflate(6.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && !(e instanceof Player)
        );

        for (LivingEntity target : targets) {
            boolean hurt = target.hurt(level.damageSources().magic(), 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));

            if (hurt && BloodMagicCompat.isLoaded()) {
                BloodMagicCompat.addNearestPlayerLP(level, pos, 16.0D, 20);
            }
        }
    }
}
