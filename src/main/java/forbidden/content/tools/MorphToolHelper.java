package forbidden.content.tools;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import forbidden.component.ForbiddenComponents;
import forbidden.component.MorphState;
import forbidden.content.ForbiddenContent;

/** Helper methods for Chameleon (Morph) tools. */
public final class MorphToolHelper {
    private static final ThreadLocal<Boolean> MINING_3X3 = ThreadLocal.withInitial(() -> false);

    public static boolean isValidRepair(ItemStack repair) {
        return repair.is(ForbiddenContent.item("envy_shard"));
    }

    public static InteractionResultHolder<ItemStack> cyclePhase(ItemStack stack, Player player, Level level, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (stack.getMaxDamage() - stack.getDamageValue() <= 5) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            MorphState current = stack.getOrDefault(ForbiddenComponents.MORPH.get(), MorphState.empty());
            int currentPhase = current.phase();
            List<MorphState.Phase> phases = new ArrayList<>(current.phases());
            while (phases.size() < 3) phases.add(MorphState.Phase.EMPTY);

            // Save current phase enchantments and custom name
            Map<ResourceLocation, Integer> currentEnchMap = new LinkedHashMap<>();
            ItemEnchantments currentEnchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : currentEnchants.entrySet()) {
                ResourceLocation id = entry.getKey().unwrapKey().map(ResourceKey::location).orElse(null);
                if (id != null) {
                    currentEnchMap.put(id, entry.getIntValue());
                }
            }
            Optional<Component> currentName = Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME));
            phases.set(currentPhase, new MorphState.Phase(currentEnchMap, currentName));

            // Advance phase 0 -> 1 -> 2 -> 0
            int nextPhase = (currentPhase + 1) % 3;
            MorphState.Phase nextData = phases.get(nextPhase);

            // Restore custom name
            if (nextData.name().isPresent()) {
                stack.set(DataComponents.CUSTOM_NAME, nextData.name().get());
            } else {
                stack.remove(DataComponents.CUSTOM_NAME);
            }

            // Restore enchantments
            var registryLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemEnchantments.Mutable newEnchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            for (var entry : nextData.enchantments().entrySet()) {
                var enchHolder = registryLookup.get(ResourceKey.create(Registries.ENCHANTMENT, entry.getKey()));
                enchHolder.ifPresent(h -> newEnchants.set(h, entry.getValue()));
            }
            if (newEnchants.keySet().isEmpty()) {
                stack.remove(DataComponents.ENCHANTMENTS);
            } else {
                stack.set(DataComponents.ENCHANTMENTS, newEnchants.toImmutable());
            }

            ForbiddenComponents.morph(level, stack, new MorphState(nextPhase, phases));
            stack.hurtAndBreak(5, player, LivingEntity.getSlotForHand(hand));
        }

        player.swing(hand);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
            SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wandfail")),
            SoundSource.PLAYERS, 0.2F, 0.2F + level.random.nextFloat() * 0.2F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void inventoryTick(ItemStack stack, Level level, Entity entity) {
        if (!level.isClientSide() && entity.tickCount % 10 == 0 && stack.isDamaged() && entity instanceof LivingEntity) {
            if (hasEnchantment(stack, "forbidden:voidtouched")) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            }
        }
    }

    public static int getWarp(ItemStack stack, Player player) {
        if (hasEnchantment(stack, "forbidden:voidtouched")) {
            return 1;
        }
        return 0;
    }

    public static boolean hasEnchantment(ItemStack stack, String enchantmentId) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ResourceLocation target = ResourceLocation.parse(enchantmentId);
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().unwrapKey().map(ResourceKey::location).filter(target::equals).isPresent()) {
                return entry.getIntValue() > 0;
            }
        }
        return false;
    }

    public static void mineImpact3x3(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity, Direction side, TagKey<Block> effectiveTag) {
        if (level.isClientSide() || MINING_3X3.get()) return;
        if (!(miningEntity instanceof ServerPlayer player)) return;
        if (!hasEnchantment(stack, "forbidden:impact")) return;
        if (!state.is(effectiveTag)) return;

        Direction face = side != null ? side : player.getDirection().getOpposite();
        Direction.Axis axis = face.getAxis();

        MINING_3X3.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos targetPos;
                    if (axis == Direction.Axis.Y) {
                        targetPos = pos.offset(a, 0, b);
                    } else if (axis == Direction.Axis.Z) {
                        targetPos = pos.offset(a, b, 0);
                    } else {
                        targetPos = pos.offset(0, b, a);
                    }

                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.getDestroySpeed(level, targetPos) >= 0.0F
                        && targetState.is(effectiveTag)
                        && level.mayInteract(player, targetPos)) {
                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
                        player.gameMode.destroyBlock(targetPos);
                    }
                }
            }
        } finally {
            MINING_3X3.set(false);
        }
    }

    private MorphToolHelper() {}
}
