package forbidden.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Six primal stores in centivis; the original display name is retained alongside modern identity. */
public record CollarState(Optional<UUID> owner, String ownerName, Map<String, Integer> vis) {
    private static final Set<String> PRIMALS = Set.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    public static final CollarState EMPTY = new CollarState(Optional.empty(), "", Map.of());
    public static final Codec<CollarState> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(CollarState::owner),
        Codec.STRING.optionalFieldOf("owner_name", "").forGetter(CollarState::ownerName),
        Codec.unboundedMap(Codec.STRING, Codec.intRange(0, 25000)).fieldOf("vis").forGetter(CollarState::vis)
    ).apply(i, CollarState::new));
    public CollarState {
        vis = Map.copyOf(vis);
        if (!PRIMALS.containsAll(vis.keySet()) || vis.values().stream().anyMatch(n -> n < 0 || n > 25000))
            throw new IllegalArgumentException("Invalid collar vis");
        if (ownerName.length() > 1024) throw new IllegalArgumentException("Owner name too long");
    }
}
