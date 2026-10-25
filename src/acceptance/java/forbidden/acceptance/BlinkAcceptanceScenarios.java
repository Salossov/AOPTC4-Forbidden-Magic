package forbidden.acceptance;

import com.mojang.authlib.GameProfile;
import forbidden.content.focus.BlinkFocusBehavior;
import forbidden.content.focus.BlinkFocusItem;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;

/** Dedicated-server scenarios for Blink focus movement and its vis transaction. */
final class BlinkAcceptanceScenarios {
    private static final double START_X = 24.5;
    private static final double START_Y = 80.0;
    private static final double START_Z = 8.5;
    private static final BlockPos TARGET = new BlockPos(24, 81, 13);
    private static final BlockPos BLOCKING_CEILING = new BlockPos(24, 83, 12);
    private static final ResourceLocation CASTING_WAND = ResourceLocation.parse("thaumcraft:casting_wand");
    private static final ResourceLocation BLINK_FOCUS = ResourceLocation.parse("forbidden:blink_focus");
    private static final Aspect PERDITIO = Aspect.getAspect("perditio");

    static void run(ServerLevel level, BiConsumer<String, Boolean> check) {
        Item focusItem = BuiltInRegistries.ITEM.getOptional(BLINK_FOCUS).orElseThrow();
        if (!(focusItem instanceof BlinkFocusItem blink)) {
            throw new AssertionError("Registered Blink focus has unexpected type: " + focusItem);
        }
        var behavior = new BlinkFocusBehavior(blink);

        clearFixture(level);
        level.setBlockAndUpdate(TARGET, Blocks.STONE.defaultBlockState());
        var successfulPlayer = player(level, "blink-success");
        var successfulWand = wand(600);
        var successfulFocus = new ItemStack(focusItem);
        var quoteWand = wand(600);
        boolean nativeQuoteAccepted = WandApi.consume(quoteWand, successfulPlayer, blink.getVisCost(successfulFocus), true);
        int nativeQuoteRemaining = WandApi.stored(quoteWand, PERDITIO);
        var origin = successfulPlayer.position();
        boolean castSucceeded = behavior.cast(successfulPlayer, successfulWand, successfulFocus);
        check.accept("BLINK_success_moves_player", castSucceeded && successfulPlayer.position().distanceToSqr(origin) > 1.0);
        int successfulRemaining = WandApi.stored(successfulWand, PERDITIO);
        check.accept("BLINK_native_quote_accepts_cost", nativeQuoteAccepted);
        check.accept("BLINK_success_matches_native_vis_quote", successfulRemaining == nativeQuoteRemaining);

        clearFixture(level);
        var noHitPlayer = player(level, "blink-no-hit");
        var noHitWand = wand(600);
        var noHitOrigin = noHitPlayer.position();
        boolean noHitResult = behavior.cast(noHitPlayer, noHitWand, new ItemStack(focusItem));
        check.accept("BLINK_no_block_hit_fails_without_movement_or_payment",
            !noHitResult && noHitPlayer.position().equals(noHitOrigin)
                && WandApi.stored(noHitWand, PERDITIO) == 600);

        clearFixture(level);
        level.setBlockAndUpdate(TARGET, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(BLOCKING_CEILING, Blocks.STONE.defaultBlockState());
        var blockedPlayer = player(level, "blink-blocked");
        var blockedWand = wand(600);
        var blockedOrigin = blockedPlayer.position();
        boolean blockedResult = behavior.cast(blockedPlayer, blockedWand, new ItemStack(focusItem));
        check.accept("BLINK_colliding_destination_fails_without_movement_or_payment",
            !blockedResult && blockedPlayer.position().equals(blockedOrigin)
                && WandApi.stored(blockedWand, PERDITIO) == 600);

        clearFixture(level);
        level.setBlockAndUpdate(TARGET, Blocks.STONE.defaultBlockState());
        var canceledPlayer = player(level, "blink-canceled");
        var canceledWand = wand(600);
        var canceledOrigin = canceledPlayer.position();
        var cancel = new TeleportHook(canceledPlayer, canceledWand, true);
        NeoForge.EVENT_BUS.register(cancel);
        boolean canceledResult;
        try {
            canceledResult = behavior.cast(canceledPlayer, canceledWand, new ItemStack(focusItem));
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        check.accept("BLINK_canceled_teleport_keeps_position_and_vis",
            cancel.seen && !canceledResult && canceledPlayer.position().equals(canceledOrigin)
                && WandApi.stored(canceledWand, PERDITIO) == 600);

        clearFixture(level);
        level.setBlockAndUpdate(TARGET, Blocks.STONE.defaultBlockState());
        var rollbackPlayer = player(level, "blink-payment-rollback");
        var rollbackWand = wand(600);
        var rollbackOrigin = rollbackPlayer.position();
        var drain = new TeleportHook(rollbackPlayer, rollbackWand, false);
        NeoForge.EVENT_BUS.register(drain);
        boolean rollbackResult;
        try {
            rollbackResult = behavior.cast(rollbackPlayer, rollbackWand, new ItemStack(focusItem));
        } finally {
            NeoForge.EVENT_BUS.unregister(drain);
        }
        check.accept("BLINK_payment_failure_event_seen", drain.seen);
        check.accept("BLINK_payment_failure_cast_rejected", !rollbackResult);
        check.accept("BLINK_payment_failure_position_restored", rollbackPlayer.position().equals(rollbackOrigin));
        int rollbackRemaining = WandApi.stored(rollbackWand, PERDITIO);
        check.accept("BLINK_payment_failure_remaining_perditio=" + rollbackRemaining, rollbackRemaining == 0);
    }

    private static void clearFixture(ServerLevel level) {
        level.setBlockAndUpdate(TARGET, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(BLOCKING_CEILING, Blocks.AIR.defaultBlockState());
    }

    private static ServerPlayer player(ServerLevel level, String name) {
        var profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(level.getServer(),
            new Connection(PacketFlow.SERVERBOUND), player,
            new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false));
        player.setPos(START_X, START_Y, START_Z);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        return player;
    }

    private static ItemStack wand(int perditio) {
        var item = BuiltInRegistries.ITEM.getOptional(CASTING_WAND).orElseThrow();
        var wand = new ItemStack(item);
        WandApi.parts(wand, new WandParts("forbidden:tainted", "iron"));
        WandApi.set(wand, PERDITIO, perditio);
        return wand;
    }

    private static final class TeleportHook {
        private final ServerPlayer player;
        private final ItemStack wand;
        private final boolean cancel;
        private boolean seen;

        private TeleportHook(ServerPlayer player, ItemStack wand, boolean cancel) {
            this.player = player;
            this.wand = wand;
            this.cancel = cancel;
        }

        @SubscribeEvent
        public void onTeleport(EntityTeleportEvent event) {
            if (event.getEntity() != player) return;
            seen = true;
            if (cancel) event.setCanceled(true);
            else WandApi.set(wand, PERDITIO, 0);
        }
    }

    private BlinkAcceptanceScenarios() {}
}
