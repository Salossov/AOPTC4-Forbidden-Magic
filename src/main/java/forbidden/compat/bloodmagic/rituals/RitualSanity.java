package forbidden.compat.bloodmagic.rituals;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.research.ResearchApi;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.ritual.EnumRuneType;
import wayoftime.bloodmagic.ritual.IMasterRitualStone;
import wayoftime.bloodmagic.ritual.Ritual;
import wayoftime.bloodmagic.ritual.RitualComponent;
import wayoftime.bloodmagic.ritual.RitualRegister;

/**
 * Ritual: Plea of Delayed Insanity (C08).
 * Wards players within range against Warp effects while continuously draining LP from the owner's Soul Network.
 */
@RitualRegister("spite_sanity")
public class RitualSanity extends Ritual {
    public static final String RITUAL_ID = "spite_sanity";
    private static final int REFRESH_COST = 15000;
    private static final int TIME_DELAY = 600;

    public RitualSanity() {
        super(RITUAL_ID, 1, 20000, "ritual.forbidden.sanity");
    }

    @Override
    public void performRitual(IMasterRitualStone stone) {
        if (stone == null) return;
        Level level = stone.getWorldObj();
        if (level == null || level.isClientSide()) return;

        BlockPos pos = stone.getMasterBlockPos();
        SoulNetwork network = stone.getOwnerNetwork();

        int range = 15;
        AABB aabb = new AABB(pos).inflate(range, range, range);
        List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
        if (players.isEmpty()) return;

        int cost = getRefreshCost();
        int totalCost = cost * players.size();

        if (network == null || network.getCurrentEssence() < totalCost) {
            if (network != null) {
                network.causeNausea();
            }
            return;
        }

        Holder<MobEffect> warpWard = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("thaumcraft", "warp_ward")).orElse(null);
        boolean crazy = level.getGameTime() % (TIME_DELAY * 4) == 0;
        int entityCount = 0;

        for (Player player : players) {
            if (warpWard != null) {
                MobEffectInstance current = player.getEffect(warpWard);
                if (current == null || current.getDuration() <= TIME_DELAY) {
                    player.addEffect(new MobEffectInstance(warpWard, TIME_DELAY + 2, 0, false, false, true));
                    entityCount++;
                    if (crazy && level.random.nextInt(35) <= 3 && player instanceof ServerPlayer serverPlayer) {
                        ResearchApi.addWarp(serverPlayer, ResearchApi.WarpKind.TEMPORARY, 1 + level.random.nextInt(3));
                    }
                }
            }
        }

        if (entityCount > 0) {
            network.syphon(cost * entityCount);
        }
    }

    @Override
    public int getRefreshCost() {
        return REFRESH_COST;
    }

    @Override
    public int getRefreshTime() {
        return TIME_DELAY;
    }

    @Override
    public void gatherComponents(Consumer<RitualComponent> components) {
        addRune(components, 4, 1, 0, EnumRuneType.DUSK);
        addRune(components, -4, 1, 0, EnumRuneType.DUSK);
        addRune(components, 0, 1, 4, EnumRuneType.DUSK);
        addRune(components, 0, 1, -4, EnumRuneType.DUSK);

        addRune(components, 4, 2, 0, EnumRuneType.EARTH);
        addRune(components, -4, 2, 0, EnumRuneType.EARTH);
        addRune(components, 0, 2, 4, EnumRuneType.EARTH);
        addRune(components, 0, 2, -4, EnumRuneType.EARTH);

        addRune(components, 4, 3, 0, EnumRuneType.DUSK);
        addRune(components, -4, 3, 0, EnumRuneType.DUSK);
        addRune(components, 0, 3, 4, EnumRuneType.DUSK);
        addRune(components, 0, 3, -4, EnumRuneType.DUSK);

        addRune(components, 4, 4, 0, EnumRuneType.AIR);
        addRune(components, -4, 4, 0, EnumRuneType.AIR);
        addRune(components, 0, 4, 4, EnumRuneType.AIR);
        addRune(components, 0, 4, -4, EnumRuneType.AIR);

        addRune(components, 4, 4, 1, EnumRuneType.EARTH);
        addRune(components, -4, 4, 1, EnumRuneType.EARTH);
        addRune(components, 1, 4, 4, EnumRuneType.EARTH);
        addRune(components, 1, 4, -4, EnumRuneType.EARTH);
        addRune(components, 4, 4, -1, EnumRuneType.EARTH);
        addRune(components, -4, 4, -1, EnumRuneType.EARTH);
        addRune(components, -1, 4, 4, EnumRuneType.EARTH);
        addRune(components, -1, 4, -4, EnumRuneType.EARTH);

        addRune(components, 3, 4, 0, EnumRuneType.FIRE);
        addRune(components, -3, 4, 0, EnumRuneType.FIRE);
        addRune(components, 0, 4, 3, EnumRuneType.FIRE);
        addRune(components, 0, 4, -3, EnumRuneType.FIRE);
    }

    @Override
    public Ritual getNewCopy() {
        return new RitualSanity();
    }
}
