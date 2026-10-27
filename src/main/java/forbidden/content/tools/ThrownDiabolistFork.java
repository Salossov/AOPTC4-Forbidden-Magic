package forbidden.content.tools;

import forbidden.content.ForbiddenTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Trident flight and pickup, with the existing fork stack and melee damage. */
public final class ThrownDiabolistFork extends AbstractArrow {
    private boolean dealtDamage;

    public ThrownDiabolistFork(EntityType<? extends ThrownDiabolistFork> type, Level level) {
        super(type, level);
    }

    public ThrownDiabolistFork(Level level, LivingEntity owner, ItemStack stack) {
        super(ForbiddenTools.THROWN_FORK.get(), owner, level, stack, null);
        pickup = Pickup.ALLOWED;
        setSoundEvent(SoundEvents.TRIDENT_HIT_GROUND);
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) dealtDamage = true;
        super.tick();
    }

    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        var source = damageSources().trident(this, owner == null ? this : owner);
        // Existing sword attack: base player 1 + sword 3 + FORK tier bonus 2.
        float damage = 1 + 3 + ForbiddenToolTiers.FORK.getAttackDamageBonus();
        if (level() instanceof ServerLevel server) {
            damage = EnchantmentHelper.modifyDamage(server, getWeaponItem(), target, source, damage);
        }
        dealtDamage = true;
        if (target.hurt(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) return;
            if (level() instanceof ServerLevel server) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(server, target, source, getWeaponItem());
            }
            if (target instanceof LivingEntity living) {
                doKnockback(living, source);
                doPostHurtEffects(living);
            }
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1, 1);
    }

    @Override public ItemStack getWeaponItem() { return getPickupItemStackOrigin(); }
    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(ForbiddenTools.DIABOLIST_FORK.get()); }
    @Override protected float getWaterInertia() { return 0.99F; }

    @Override
    public void playerTouch(Player player) {
        if (ownedBy(player) || getOwner() == null) super.playerTouch(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("DealtDamage");
    }
}
