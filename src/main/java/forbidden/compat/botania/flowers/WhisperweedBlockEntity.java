package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.botania.BotaniaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.research.ResearchApi;

/**
 * Whisperweed BlockEntity (C03).
 * Consumes 6000 Mana every 300 ticks to grant aspect knowledge points and occasional warp to nearby players.
 */
public class WhisperweedBlockEntity extends BlockEntity {
    public static final int COST = 6000;
    public static final int INTERVAL = 300;
    public static final int RANGE = 3;

    private int ticksExisted = 0;
    private int mana = 0;
    private BlockPos boundPos = null;

    public WhisperweedBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.WHISPERWEED_BE.get(), pos, state);
    }

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        return COST;
    }

    public void addMana(int amount) {
        this.mana = Math.clamp(this.mana + amount, 0, COST);
        setChanged();
    }

    public BlockPos getBindingPos() {
        return boundPos;
    }

    public void setBindingPos(BlockPos pos) {
        this.boundPos = pos;
        setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        ticksExisted++;

        if (level.hasNeighborSignal(pos)) {
            return;
        }

        if (mana < COST) {
            int drawn = BotaniaCompat.drawMana(level, pos, boundPos, COST - mana);
            if (drawn > 0) {
                mana += drawn;
                setChanged();
            }
        }

        if (ticksExisted % INTERVAL == 0) {
            AABB searchBox = new AABB(pos).inflate(RANGE);
            List<Player> players = level.getEntitiesOfClass(Player.class, searchBox, Player::isAlive);
            if (!players.isEmpty() && mana >= COST) {
                mana -= COST;
                Player player = players.get(level.random.nextInt(players.size()));

                int amt = 1 + level.random.nextInt(3);
                int warp = 0;
                if (level.random.nextInt(10) < 2) {
                    amt += 1 + level.random.nextInt(3);
                    warp = 1 + level.random.nextInt(5);
                }

                List<Aspect> primals = Aspects.PRIMALS;
                List<Aspect> compounds = Aspect.all().values().stream()
                        .filter(a -> !primals.contains(a))
                        .toList();

                for (int a = 0; a < amt; ++a) {
                    Aspect aspect;
                    if (level.random.nextInt(25) > 2 || compounds.isEmpty()) {
                        aspect = primals.get(level.random.nextInt(primals.size()));
                    } else {
                        aspect = compounds.get(level.random.nextInt(compounds.size()));
                    }

                    if (player instanceof ServerPlayer sp) {
                        try {
                            short current = (short) ResearchApi.knowledge(sp).pool(aspect);
                            ResearchApi.completeAspect(sp, aspect, (short) (current + 1));
                        } catch (Throwable ignored) {
                        }
                    }
                }

                if (warp > 0 && player instanceof ServerPlayer sp) {
                    try {
                        ResearchApi.addWarp(sp, ResearchApi.WarpKind.TEMPORARY, warp);
                    } catch (Throwable ignored) {
                    }
                }

                player.displayClientMessage(Component.translatable("forbidden.whisperweed.whisper"), true);
                setChanged();
            }
        }
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public void setTicksExisted(int ticks) {
        this.ticksExisted = ticks;
    }

    public void writeCustomData(CompoundTag tag) {
        tag.putInt("mana", mana);
        tag.putInt("ticksExisted", ticksExisted);
        if (boundPos != null) {
            tag.putInt("boundX", boundPos.getX());
            tag.putInt("boundY", boundPos.getY());
            tag.putInt("boundZ", boundPos.getZ());
            tag.putBoolean("hasBoundPos", true);
        }
    }

    public void readCustomData(CompoundTag tag) {
        this.mana = Math.clamp(tag.getInt("mana"), 0, COST);
        this.ticksExisted = tag.getInt("ticksExisted");
        if (tag.getBoolean("hasBoundPos")) {
            this.boundPos = new BlockPos(tag.getInt("boundX"), tag.getInt("boundY"), tag.getInt("boundZ"));
        } else {
            this.boundPos = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeCustomData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readCustomData(tag);
    }
}
