package forbidden.content.focus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.FocusBehavior;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;

/**
 * FocusBehavior implementation for BlinkFocus (B21):
 * Teleports player towards look direction / clicked block face up to 128 blocks.
 * Deducts vis ONLY upon successful teleportation.
 */
public class BlinkFocusBehavior implements FocusBehavior {
    private final BlinkFocusItem focusItem;

    public BlinkFocusBehavior(BlinkFocusItem focusItem) {
        this.focusItem = focusItem;
    }

    @Override
    public boolean managesVis() {
        return true;
    }

    @Override
    public boolean cast(ServerPlayer player, ItemStack wand, ItemStack focus) {
        var costs = focusItem.getVisCost(focus);
        // Verify affordability before executing action
        if (!WandApi.consume(wand, player, costs, false)) {
            return false;
        }

        int potency = focusItem.getUpgradeLevel(focus, FocusUpgradeType.potency);
        double maxDistance = 128.0;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetVec = eyePos.add(lookVec.scale(maxDistance));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos, targetVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        Vec3 hitLocation = hit.getLocation();
        double ex = hitLocation.x;
        double wy = hitLocation.y;
        double zee = hitLocation.z;
        switch (hit.getDirection()) {
            case DOWN -> wy -= 2.0;
            case UP -> {}
            case NORTH -> zee -= 0.5;
            case SOUTH -> zee += 0.5;
            case WEST -> ex -= 0.5;
            case EAST -> ex += 0.5;
        }

        AABB targetBox = player.getDimensions(player.getPose()).makeBoundingBox(ex, wy, zee);
        if (!player.level().noCollision(player, targetBox)) {
            if (player.level().noCollision(player, targetBox.move(0, 0.5, 0))) {
                wy += 0.5;
            } else {
                return false;
            }
        }

        if (!player.level().mayInteract(player, hit.getBlockPos())) {
            return false;
        }

        // Commit vis payment only upon successful verification
        if (!WandApi.consume(wand, player, costs, true)) {
            return false;
        }

        double prevX = player.getX();
        double prevY = player.getY();
        double prevZ = player.getZ();

        // Departure sound and smoke
        player.serverLevel().levelEvent(1009, player.blockPosition(), 0);
        for (int k = 0; k < 8; k++) {
            double px = prevX + (player.getRandom().nextDouble() - 0.5) * player.getBbWidth();
            double py = prevY + player.getRandom().nextDouble() * player.getBbHeight() - 0.25;
            double pz = prevZ + (player.getRandom().nextDouble() - 0.5) * player.getBbWidth();
            player.serverLevel().sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0, 0, 0);
        }

        // Upgrades: Hellfire (id 42) & Pandemonium (id 43)
        int enlarge = focusItem.getUpgradeLevel(focus, FocusUpgradeType.enlarge);
        double aoe = 3.0 + 1.5 * enlarge;
        AABB aoeBox = new AABB(ex - aoe, wy - aoe, zee - aoe, ex + aoe, wy + aoe, zee + aoe);

        ForbiddenFoci.registerUpgrades();
        if (focusItem.isUpgradedWith(focus, ForbiddenFoci.HELLFIRE)) {
            for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, aoeBox)) {
                if (entity != player) {
                    for (int k = 0; k < 8; k++) {
                        double ex2 = entity.getX() + (player.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                        double ey2 = entity.getY() + player.getRandom().nextDouble() * entity.getBbHeight() - 0.25;
                        double ez2 = entity.getZ() + (player.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                        player.serverLevel().sendParticles(ParticleTypes.FLAME, ex2, ey2, ez2, 1, 0, 0, 0, 0);
                    }
                    int dmg = 3 + 3 * potency;
                    entity.hurt(player.damageSources().playerAttack(player), dmg);
                    entity.igniteForSeconds(dmg);
                }
            }
        } else if (focusItem.isUpgradedWith(focus, ForbiddenFoci.PANDEMONIUM)) {
            for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, aoeBox)) {
                if (entity instanceof Enemy && entity != player) {
                    for (int k = 0; k < 8; k++) {
                        double ex2 = entity.getX() + (player.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                        double ey2 = entity.getY() + player.getRandom().nextDouble() * entity.getBbHeight() - 0.25;
                        double ez2 = entity.getZ() + (player.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                        player.serverLevel().sendParticles(ParticleTypes.SMOKE, ex2, ey2, ez2, 1, 0, 0, 0, 0);
                    }
                    entity.teleportTo(prevX, prevY, prevZ);
                }
            }
        }

        // Teleport player
        player.teleportTo(ex, wy, zee);

        // Arrival sound and flame
        player.serverLevel().levelEvent(1009, player.blockPosition(), 0);
        for (int k = 0; k < 8; k++) {
            double px = player.getX() + (player.getRandom().nextDouble() - 0.5) * player.getBbWidth();
            double py = player.getY() + player.getRandom().nextDouble() * player.getBbHeight() - 0.25;
            double pz = player.getZ() + (player.getRandom().nextDouble() - 0.5) * player.getBbWidth();
            player.serverLevel().sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0, 0, 0, 0);
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }
}
