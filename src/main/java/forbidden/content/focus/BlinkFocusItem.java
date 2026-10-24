package forbidden.content.focus;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;

/**
 * Blink Focus (B21):
 * Teleports player towards look direction / clicked block face.
 * Base cost: 3 Perditio (300 centivis). Vis is deducted ONLY upon successful teleportation.
 * Upgrades: Potency (increases range: 16 + 8 * level), Frugal, Enlarge, Hellfire (id 42), Pandemonium (id 43).
 */
public class BlinkFocusItem extends ItemFocusBasic {
    public BlinkFocusItem() {
        super();
    }

    public BlinkFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public Map<Aspect, Integer> getVisCost(ItemStack stack) {
        ForbiddenFoci.registerUpgrades();
        if (isUpgradedWith(stack, ForbiddenFoci.HELLFIRE)) {
            return Map.of(
                    Aspect.getAspect("perditio"), 300,
                    Aspect.getAspect("ignis"), 100
            );
        } else if (isUpgradedWith(stack, ForbiddenFoci.PANDEMONIUM)) {
            return Map.of(
                    Aspect.getAspect("perditio"), 300,
                    Aspect.getAspect("ordo"), 100
            );
        }
        return Map.of(Aspect.getAspect("perditio"), 300);
    }

    @Override
    public int getFocusColor(ItemStack stack) {
        return 9474192;
    }

    @Override
    public int getActivationCooldown(ItemStack stack) {
        return 333;
    }

    @Override
    public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack, int rank) {
        ForbiddenFoci.registerUpgrades();
        return switch (rank) {
            case 1 -> List.of(FocusUpgradeType.frugal);
            case 2 -> List.of(ForbiddenFoci.HELLFIRE, ForbiddenFoci.PANDEMONIUM, FocusUpgradeType.frugal);
            case 3, 4, 5 -> {
                if (isUpgradedWith(stack, ForbiddenFoci.HELLFIRE)) {
                    yield List.of(FocusUpgradeType.potency, FocusUpgradeType.frugal, FocusUpgradeType.enlarge);
                } else if (isUpgradedWith(stack, ForbiddenFoci.PANDEMONIUM)) {
                    yield List.of(FocusUpgradeType.frugal, FocusUpgradeType.enlarge);
                } else {
                    yield List.of(FocusUpgradeType.frugal);
                }
            }
            default -> List.of();
        };
    }
}
