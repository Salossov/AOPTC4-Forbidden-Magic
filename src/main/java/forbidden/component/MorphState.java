package forbidden.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/** Inactive phases retain independent enchantments and rich custom names. Damage stays on the stack. */
public record MorphState(int phase, List<Phase> phases) {
    public static final Codec<MorphState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.intRange(0, 2).fieldOf("phase").forGetter(MorphState::phase),
        Phase.CODEC.listOf(3, 3).fieldOf("phases").forGetter(MorphState::phases)
    ).apply(i, MorphState::new));
    public MorphState {
        if (phase < 0 || phase > 2 || phases.size() != 3) throw new IllegalArgumentException("Three Morph phases required");
        phases = List.copyOf(phases);
    }
    public static MorphState empty() { return new MorphState(0, List.of(Phase.EMPTY, Phase.EMPTY, Phase.EMPTY)); }
    public record Phase(Map<ResourceLocation, Integer> enchantments, Optional<Component> name) {
        public static final Phase EMPTY = new Phase(Map.of(), Optional.empty());
        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.intRange(1, 255)).fieldOf("enchantments").forGetter(Phase::enchantments),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Phase::name)
        ).apply(i, Phase::new));
        public Phase {
            enchantments = Map.copyOf(enchantments);
            if (enchantments.size() > 256 || enchantments.values().stream().anyMatch(n -> n < 1 || n > 255))
                throw new IllegalArgumentException("Invalid phase enchantments");
            name = name.map(Component::copy);
        }
        @Override public Optional<Component> name() { return name.map(Component::copy); }
    }
}
