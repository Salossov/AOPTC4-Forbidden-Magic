package forbidden.compat.botania.flowers;

import forbidden.compat.botania.BotaniaItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Whisperweed BlockEntity (C03).
 * Periodically searches a 3-block radius for players, granting research whisper insights.
 */
public class WhisperweedBlockEntity extends BlockEntity {
    public static final int COST = 6000;
    public static final int INTERVAL = 300;
    public static final int RANGE = 3;

    private int ticksExisted = 0;
    private int mana = COST;

    public WhisperweedBlockEntity(BlockPos pos, BlockState state) {
        super(BotaniaItems.WHISPERWEED_BE.get(), pos, state);
    }

    public int getMana() {
        return mana;
    }

    public void addMana(int amount) {
        this.mana = Math.min(COST, this.mana + amount);
        setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        ticksExisted++;

        if (level.hasNeighborSignal(pos)) {
            return;
        }

        if (ticksExisted % INTERVAL == 0) {
            AABB searchBox = new AABB(pos).inflate(RANGE);
            List<Player> players = level.getEntitiesOfClass(Player.class, searchBox, Player::isAlive);
            if (!players.isEmpty() && mana >= COST) {
                mana -= COST;
                Player player = players.get(level.random.nextInt(players.size()));
                player.displayClientMessage(Component.translatable("forbidden.whisperweed.whisper"), true);
                setChanged();
            }
        }
    }
}
