package forbidden.content;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** The release consumes ink even for creative players. */
public final class ShadowInkItem extends Item {
    public ShadowInkItem(Properties properties) { super(properties); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Sheep sheep)) return InteractionResult.PASS;
        if (!player.level().isClientSide && !sheep.isSheared() && sheep.getColor() != DyeColor.BLACK) {
            sheep.setColor(DyeColor.BLACK); stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
