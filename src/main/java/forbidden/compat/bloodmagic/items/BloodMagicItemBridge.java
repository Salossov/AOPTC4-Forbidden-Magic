package forbidden.compat.bloodmagic.items;

import net.minecraft.world.item.Item;

/**
 * Isolated bridge for instantiating Blood Magic-dependent item subclasses (C05–C07).
 * This class directly references Bm* classes and must NEVER be referenced when Blood Magic is absent.
 */
public final class BloodMagicItemBridge {

    public static Item createBloodwell() {
        return new BmBloodwellItem();
    }

    public static Item createBoundwell() {
        return new BmBoundwellItem();
    }

    public static Item createBloodRapier() {
        return new BmBloodRapierItem();
    }

    public static Item createEldritchOrb() {
        return new BmEldritchOrbItem();
    }

    private BloodMagicItemBridge() {}
}
