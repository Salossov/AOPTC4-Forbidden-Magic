package forbidden.content;

import com.mojang.serialization.*;
import forbidden.config.ForbiddenConfig;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;

/** Recipe gates remain independent of registration gates. */
public record ContentCondition(String gate) implements ICondition {
    public static final MapCodec<ContentCondition> CODEC=Codec.STRING.fieldOf("gate").xmap(ContentCondition::new,ContentCondition::gate);
    private static final DeferredRegister<MapCodec<? extends ICondition>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,"forbidden");
    static { TYPES.register("content",()->CODEC); }
    public static void register(IEventBus bus){TYPES.register(bus);}
    @Override public MapCodec<? extends ICondition> codec(){return CODEC;}
    @Override public boolean test(IContext context){return switch(gate){
        case "greedy"->ForbiddenConfig.gates().greedyAvailable();
        case "gluttony_normal"->ForbiddenConfig.GLUTTONY.get()==0;
        case "gluttony_hardcore"->ForbiddenConfig.GLUTTONY.get()==2;
        case "emerald_transmutation"->ForbiddenConfig.EMERALD_TRANSMUTATION.get();
        case "wrath_cage"->ForbiddenConfig.WRATH_CAGE.get();
        case "botania"->ForbiddenConfig.CROSS_MOD.get()&&ForbiddenConfig.BOTANIA.get()&&net.neoforged.fml.ModList.get().isLoaded("botania");
        case "bloodmagic"->ForbiddenConfig.CROSS_MOD.get()&&ForbiddenConfig.BLOOD_MAGIC.get()&&net.neoforged.fml.ModList.get().isLoaded("bloodmagic");
        default->throw new IllegalArgumentException("Unknown Forbidden Magic gate "+gate);
    };}
}
