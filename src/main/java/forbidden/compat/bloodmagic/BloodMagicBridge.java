package forbidden.compat.bloodmagic;

import forbidden.compat.bloodmagic.rituals.RitualSanity;
import forbidden.config.ForbiddenConfig;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.common.item.BloodOrb;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.core.data.SoulTicket;
import wayoftime.bloodmagic.util.helper.NBTHelper;
import wayoftime.bloodmagic.util.helper.NetworkHelper;

/**
 * Isolated bridge to Blood Magic 3.4.0 API.
 * This class directly imports wayoftime.bloodmagic.* and must ONLY be referenced
 * when ModList.get().isLoaded("bloodmagic") is true.
 */
public final class BloodMagicBridge {
    public static final BloodOrb ELDRITCH_BLOOD_ORB = new BloodOrb(
            ResourceLocation.fromNamespaceAndPath("forbidden", "eldritch"),
            6,
            80_000_000,
            140
    );

    public static boolean syphonFromPlayerNetwork(Player player, int amount) {
        if (player == null || amount <= 0) return true;
        SoulNetwork network = NetworkHelper.getSoulNetwork(player);
        if (network == null) return false;
        return network.syphon(amount) >= amount;
    }

    public static boolean syphonFromStackOrNetwork(ItemStack stack, Player player, int amount) {
        if (amount <= 0) return true;
        if (stack != null && !stack.isEmpty()) {
            Binding binding = Binding.fromStack(stack);
            if (binding != null) {
                SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
                if (network != null && network.syphon(amount) >= amount) {
                    return true;
                }
            }
        }
        return player != null && syphonFromPlayerNetwork(player, amount);
    }

    public static boolean syphonAndDamage(Player player, int amount) {
        if (player == null) return false;
        SoulNetwork network = NetworkHelper.getSoulNetwork(player);
        if (network == null) return false;
        return network.syphonAndDamage(player, amount);
    }

    public static void addPlayerLP(Player player, int amount, int max) {
        if (player == null || amount <= 0) return;
        SoulNetwork network = NetworkHelper.getSoulNetwork(player);
        if (network != null) {
            network.addLifeEssence(amount, max);
        }
    }

    public static void drainVictimLP(Player victim, int amount) {
        if (victim == null || amount <= 0) return;
        SoulNetwork network = NetworkHelper.getSoulNetwork(victim);
        if (network != null) {
            int current = network.getCurrentEssence();
            network.setCurrentEssence(Math.max(0, current - amount));
        }
    }

    public static int getPlayerLP(Player player) {
        if (player == null) return 0;
        SoulNetwork network = NetworkHelper.getSoulNetwork(player);
        return network != null ? network.getCurrentEssence() : 0;
    }

    /** Legacy recharge uses the stack's bound owner, accepting any positive partial debit. */
    private static int syphonBoundNetwork(ItemStack stack, int amount) {
        Binding binding = Binding.fromStack(stack);
        if (binding == null) return 0;
        SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
        return network == null ? 0 : network.syphon(amount);
    }

    public static void bindItem(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null) return;
        Binding binding = new Binding(player.getGameProfile().getId(), player.getGameProfile().getName());
        CompoundTag tag = NBTHelper.getOrCreateTag(stack);
        tag.put("binding", binding.serializeNBT());
    }

    public static String getBindingOwner(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Binding binding = Binding.fromStack(stack);
        return binding != null ? binding.getOwnerName() : null;
    }

    public static UUID getBindingOwnerId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Binding binding = Binding.fromStack(stack);
        return binding != null ? binding.getOwnerId() : null;
    }

    public static void checkAndSetOwner(ItemStack stack, Player player) {
        if (getBindingOwner(stack) == null && player != null) {
            bindItem(stack, player);
        }
    }

    public static InteractionResultHolder<ItemStack> useBloodOrb(ItemStack stack, Level level, Player player, InteractionHand hand) {
        checkAndSetOwner(stack, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f,
                2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        SoulNetwork network = NetworkHelper.getSoulNetwork(player);
        if (network != null) {
            NetworkHelper.setMaxOrb(network, 6);
            network.addLifeEssence(200, 80_000_000);
            network.hurtPlayer(player, 2.0f);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    public static void updateBloodWand(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.tickCount % 100 != 0) return;
        checkAndSetOwner(wand, player);

        int bloodvis = ForbiddenConfig.BLOOD_VIS.get();
        boolean alchem = WandApi.parts(wand).cap().contains("alchemical");
        int cost = Math.max(0, alchem ? bloodvis - 1 : bloodvis);

        for (var aspect : Aspects.PRIMALS) {
            int deficit = WandApi.capacity(wand) - WandApi.stored(wand, aspect);
            if (deficit > 0) {
                int toAdd = Math.min(deficit, 100);
                int lpCost = cost * toAdd;
                if (player.isCreative()) {
                    WandApi.set(wand, aspect, WandApi.stored(wand, aspect) + toAdd);
                } else if (syphonBoundNetwork(wand, lpCost) > 0) {
                    WandApi.set(wand, aspect, WandApi.stored(wand, aspect) + toAdd);
                } else if (player.getHealth() > 6.0f) {
                    player.setHealth(player.getHealth() - 3.0f);
                    WandApi.set(wand, aspect, WandApi.stored(wand, aspect) + toAdd);
                    return;
                } else {
                    return;
                }
            }
        }
    }

    public static void updateBloodStaff(ItemStack staff, Player player) {
        if (player.level().isClientSide() || player.tickCount % 25 != 0) return;
        if (!isHotbar(staff, player)) return;
        checkAndSetOwner(staff, player);

        int bloodvis = ForbiddenConfig.BLOOD_VIS.get();
        boolean alchem = WandApi.parts(staff).cap().contains("alchemical");
        int cost = Math.max(0, alchem ? bloodvis - 1 : bloodvis);

        for (var aspect : Aspects.PRIMALS) {
            int deficit = WandApi.capacity(staff) - WandApi.stored(staff, aspect);
            if (deficit > 0) {
                int toAdd = Math.min(deficit, 100);
                int lpCost = cost * toAdd;
                if (player.isCreative()) {
                    WandApi.set(staff, aspect, WandApi.stored(staff, aspect) + toAdd);
                } else if (syphonBoundNetwork(staff, lpCost) > 0) {
                    WandApi.set(staff, aspect, WandApi.stored(staff, aspect) + toAdd);
                } else if (player.getHealth() > 3.0f) {
                    player.setHealth(player.getHealth() - 3.0f);
                    WandApi.set(staff, aspect, WandApi.stored(wandStaff(staff), aspect) + toAdd);
                    return;
                } else if (player.getHealth() > 0.0f) {
                    player.hurt(player.damageSources().magic(), 3.0f);
                    WandApi.set(staff, aspect, WandApi.stored(wandStaff(staff), aspect) + toAdd);
                    return;
                } else {
                    return;
                }
            }
        }
    }

    public static void addNearestPlayerLP(Level level, BlockPos pos, double radius, int amount) {
        if (level.isClientSide()) return;
        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), radius, false);
        if (player != null) {
            addPlayerLP(player, amount, 80_000_000);
        }
    }

    public static void registerRituals() {
        try {
            if (BloodMagic.RITUAL_MANAGER != null) {
                BloodMagic.RITUAL_MANAGER.discover();
            }
        } catch (Throwable ignored) {}
    }

    private static ItemStack wandStaff(ItemStack staff) {
        return staff;
    }

    private static boolean isHotbar(ItemStack stack, Player player) {
        if (player.getMainHandItem() == stack || player.getOffhandItem() == stack) return true;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i) == stack) return true;
        }
        return false;
    }

    private BloodMagicBridge() {}
}
