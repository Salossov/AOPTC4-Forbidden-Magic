package forbidden.content.baubles;

import java.text.DecimalFormat;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.WandApi;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import forbidden.component.CollarState;
import forbidden.component.ForbiddenComponents;
import forbidden.content.ForbiddenTools;
import forbidden.events.EventRandom;

/** Collar of Pain: 250 vis capacity, equips to another player's necklace slot, charges from damage/crop. */
public class SubCollarItem extends Item implements ICurioItem {
    private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");

    public SubCollarItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static int maxVis() {
        return 25000;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide() || !(target instanceof ServerPlayer sub)) {
            return InteractionResult.PASS;
        }

        var invOpt = CuriosApi.getCuriosInventory(sub);
        if (invOpt.isEmpty()) {
            return InteractionResult.PASS;
        }

        var inv = invOpt.get();
        if (ForbiddenTools.SUB_COLLAR != null && !inv.findCurios(ForbiddenTools.SUB_COLLAR.get()).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.collar.alreadywearing", sub.getDisplayName()));
            return InteractionResult.SUCCESS;
        }

        var necklaceSlots = inv.findCurios("necklace");
        var emptySlot = necklaceSlots.stream().filter(s -> s.stack().isEmpty()).findFirst();
        if (emptySlot.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.collar.alreadywearing", sub.getDisplayName()));
            return InteractionResult.SUCCESS;
        }

        ItemStack equipped = stack.copyWithCount(1);
        ForbiddenComponents.collar(player.level(), equipped, new CollarState(Optional.of(player.getUUID()), player.getDisplayName().getString(), Map.of()));
        inv.setEquippedCurio(emptySlot.get().slotContext().identifier(), emptySlot.get().slotContext().index(), equipped);
        stack.shrink(1);

        sub.sendSystemMessage(Component.translatable("message.collar.placescollar", player.getDisplayName()));
        player.sendSystemMessage(Component.translatable("message.collar.youplacecollar", sub.getDisplayName()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!(context.entity() instanceof ServerPlayer player) || player.level().isClientSide() || player.tickCount % 5 != 0) {
            return;
        }

        CollarState state = stack.getOrDefault(ForbiddenComponents.COLLAR.get(), CollarState.EMPTY);
        Map<String, Integer> visMap = new LinkedHashMap<>(state.vis());
        boolean modified = false;

        // 1. Discharge into held casting wand/staff
        ItemStack wand = player.getMainHandItem();
        if (WandApi.kind(wand) != WandApi.Kind.NONE) {
            for (Aspect aspect : Aspects.PRIMALS) {
                String tag = aspect.getTag();
                int stored = visMap.getOrDefault(tag, 0);
                int wandStored = WandApi.stored(wand, aspect);
                int room = WandApi.capacity(wand) - wandStored;
                if (stored > 0 && room > 0) {
                    int transfer = Math.min(5, Math.min(stored, room));
                    visMap.put(tag, stored - transfer);
                    WandApi.set(wand, aspect, wandStored + transfer);
                    modified = true;
                }
            }
        }

        // 2. Recharge from nearby vis network
        for (Aspect aspect : Aspects.PRIMALS) {
            String tag = aspect.getTag();
            int stored = visMap.getOrDefault(tag, 0);
            int room = maxVis() - stored;
            if (room > 0) {
                int drained = VisNetHandler.drainVis(player.level(), player.blockPosition(), aspect, Math.min(5, room));
                if (drained > 0) {
                    visMap.put(tag, stored + drained);
                    modified = true;
                }
            }
        }

        if (modified) {
            ForbiddenComponents.collar(player.level(), stack, new CollarState(state.owner(), state.ownerName(), visMap));
        }
    }

    public static void addVis(Level level, ItemStack stack, int doses) {
        CollarState state = stack.getOrDefault(ForbiddenComponents.COLLAR.get(), CollarState.EMPTY);
        Map<String, Integer> visMap = new LinkedHashMap<>(state.vis());
        for (int i = 0; i < doses; i++) {
            String p = EventRandom.nextPrimal().getTag();
            int current = visMap.getOrDefault(p, 0);
            if (current < maxVis()) {
                visMap.put(p, Math.min(maxVis(), current + WandApi.centivis(1)));
            }
        }
        ForbiddenComponents.collar(level, stack, new CollarState(state.owner(), state.ownerName(), visMap));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flags) {
        super.appendHoverText(stack, context, list, flags);
        list.add(Component.translatable("item.capacity.text").withStyle(ChatFormatting.GOLD).append(" " + (maxVis() / 100)));

        CollarState state = stack.getOrDefault(ForbiddenComponents.COLLAR.get(), CollarState.EMPTY);
        for (Aspect aspect : Aspects.PRIMALS) {
            int stored = state.vis().getOrDefault(aspect.getTag(), 0);
            if (stored > 0) {
                String amount = FORMAT.format(stored / 100.0F);
                list.add(Component.literal(aspect.getName() + " x " + amount).withColor(aspect.getColor()));
            }
        }

        if (!state.ownerName().isEmpty()) {
            list.add(Component.translatable("tooltip.collar.owner").append(" " + state.ownerName()));
        }
    }
}
