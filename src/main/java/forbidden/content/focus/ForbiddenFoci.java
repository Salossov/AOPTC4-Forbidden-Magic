package forbidden.content.focus;

import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusBehaviors;
import thaumcraft.api.wands.FocusUpgradeType;

public final class ForbiddenFoci {
    public static FocusUpgradeType HELLFIRE;
    public static FocusUpgradeType PANDEMONIUM;

    public static synchronized void registerUpgrades() {
        if (HELLFIRE == null) {
            HELLFIRE = FocusUpgradeType.get(42);
            if (HELLFIRE == null) {
                var infernus = Aspect.getAspect("forbidden:infernus");
                if (infernus == null) infernus = Aspect.getAspect("ignis");
                HELLFIRE = FocusUpgradeType.register(
                        42,
                        ResourceLocation.fromNamespaceAndPath("forbidden", "hellfire"),
                        ResourceLocation.fromNamespaceAndPath("forbidden", "textures/misc/hellfire.png"),
                        Map.of(Objects.requireNonNull(infernus), 1)
                );
            }
        }

        if (PANDEMONIUM == null) {
            PANDEMONIUM = FocusUpgradeType.get(43);
            if (PANDEMONIUM == null) {
                var tenebrae = Aspect.getAspect("tenebrae");
                PANDEMONIUM = FocusUpgradeType.register(
                        43,
                        ResourceLocation.fromNamespaceAndPath("forbidden", "pandemonium"),
                        ResourceLocation.fromNamespaceAndPath("forbidden", "textures/misc/pandemonium.png"),
                        Map.of(Objects.requireNonNull(tenebrae), 1)
                );
            }
        }
    }

    public static void registerBehavior(BlinkFocusItem item) {
        registerUpgrades();
        FocusBehaviors.register(
                item,
                new BlinkFocusBehavior(item),
                ResourceLocation.fromNamespaceAndPath("forbidden", "item/blink_focus"),
                ResourceLocation.fromNamespaceAndPath("forbidden", "item/blink_focus_orn")
        );
    }

    private ForbiddenFoci() {}
}
