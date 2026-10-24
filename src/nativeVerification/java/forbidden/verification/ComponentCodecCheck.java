package forbidden.verification;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import forbidden.component.CollarState;
import forbidden.component.MorphState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Persistence/validation checks against the real 1.21.1 codecs; no world or EULA. */
public final class ComponentCodecCheck {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("Codec regression #" + checks); }
    private static <T> T roundTrip(Codec<T> codec, T value) {
        return codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow()).getOrThrow();
    }
    public static void main(String[] args) {
        // Isolated codec process: initialize the loader's empty discovery list for feature flags.
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var named = new MorphState.Phase(Map.of(ResourceLocation.parse("minecraft:sharpness"), 5),
            Optional.of(Component.literal("Phase one")));
        var state = new MorphState(2, List.of(named, MorphState.Phase.EMPTY, new MorphState.Phase(
            Map.of(ResourceLocation.parse("forbidden:voidtouched"), 1), Optional.of(Component.literal("Third phase")))));
        check(roundTrip(MorphState.CODEC, state).equals(state));
        check(roundTrip(MorphState.CODEC, MorphState.empty()).equals(MorphState.empty()));
        var callerName = Component.literal("immutable");
        var phase = new MorphState.Phase(Map.of(), Optional.of(callerName));
        callerName.append(" changed");
        check(phase.name().orElseThrow().getString().equals("immutable"));
        var collar = new CollarState(Optional.of(UUID.fromString("98e10829-7cf6-4f3b-9baf-4d52c53915b6")), "Owner", Map.of("ignis", 25000, "aqua", 0));
        check(roundTrip(CollarState.CODEC, collar).equals(collar));
        check(roundTrip(CollarState.CODEC, CollarState.EMPTY).equals(CollarState.EMPTY));
        check(MorphState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"phase\":3,\"phases\":[]}")).error().isPresent());
        check(MorphState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"phase\":0,\"phases\":[]}")).error().isPresent());
        for (String bad : List.of("{\"vis\":{\"ignis\":25001}}", "{\"vis\":{\"ignis\":-1}}"))
            check(CollarState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error().isPresent());
        check(roundTrip(ResourceLocation.CODEC, ResourceLocation.parse("minecraft:zombie")).toString().equals("minecraft:zombie"));
        // Match the component's synchronized codec, not just its JSON persistence.
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        try {
            var stream = net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(MorphState.CODEC);
            stream.encode(buffer, state);
            check(stream.decode(buffer).equals(state));
            check(buffer.readableBytes() == 0);
        } finally { buffer.release(); }
        System.out.println("FORBIDDEN_COMPONENT_CODECS_PASS checks=" + checks);
    }
}
