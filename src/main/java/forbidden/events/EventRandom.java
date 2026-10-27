package forbidden.events;

import java.util.List;
import java.util.Random;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;

/** The single random stream owned by the release's FMEventHandler instance. */
public final class EventRandom {
    private static final Random RANDOM = new Random();
    private static final List<Aspect> PRIMALS = List.of(
        Aspects.ENTROPY, Aspects.ORDER, Aspects.FIRE, Aspects.WATER, Aspects.EARTH, Aspects.AIR
    );

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static float nextFloat() {
        return RANDOM.nextFloat();
    }

    public static Aspect nextPrimal() {
        return PRIMALS.get(nextInt(PRIMALS.size()));
    }

    private EventRandom() {}
}
