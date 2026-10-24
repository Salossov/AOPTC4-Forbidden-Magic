package forbidden.component;

import com.mojang.serialization.Codec;
import forbidden.ForbiddenMagic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent and network-synchronized state. No client-to-server mutation payload is registered. */
public final class ForbiddenComponents {
    private static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ForbiddenMagic.ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MorphState>> MORPH = type("morph", MorphState.CODEC);
    // Absence is the original empty crystal / uninitialized contract state.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MOB = type("mob", ResourceLocation.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CONTRACT = type("contract", Codec.intRange(0, 25000));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CollarState>> COLLAR = type("collar", CollarState.CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLOOD_OWNER = type("blood_owner", Codec.STRING);
    // Cosmetic stack serialization and rich text are native vanilla components, not raw legacy NBT.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> COSMETIC = type("cosmetic", ItemStack.OPTIONAL_CODEC);

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> type(String id, Codec<T> codec) {
        return TYPES.register(id, () -> DataComponentType.<T>builder().persistent(codec)
            .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec)).build());
    }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private static <T> void set(Level level, ItemStack stack, DataComponentType<T> type, T value) {
        requireServer(level);
        if (stack.isEmpty()) throw new IllegalArgumentException("Empty state target");
        stack.set(type, value);
    }
    public static void morph(Level level, ItemStack stack, MorphState value) { set(level, stack, MORPH.get(), value); }
    public static void collar(Level level, ItemStack stack, CollarState value) { set(level, stack, COLLAR.get(), value); }
    public static void contract(Level level, ItemStack stack, int remaining) {
        if (remaining < 0 || remaining > 25000) throw new IllegalArgumentException("Invalid contract");
        set(level, stack, CONTRACT.get(), remaining);
    }
    public static void mob(Level level, ItemStack stack, ResourceLocation mob) {
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(mob)) throw new IllegalArgumentException("Unknown mob " + mob);
        set(level, stack, MOB.get(), mob);
    }
    public static void bloodOwner(Level level, ItemStack stack, String owner) {
        if (owner.length() > 1024) throw new IllegalArgumentException("Owner name too long");
        set(level, stack, BLOOD_OWNER.get(), owner);
    }
    public static void cosmetic(Level level, ItemStack stack, ItemStack cosmetic) { set(level, stack, COSMETIC.get(), cosmetic.copy()); }
    public static ItemStack cosmetic(ItemStack stack) { return stack.getOrDefault(COSMETIC.get(), ItemStack.EMPTY).copy(); }
    public static <T> void remove(Level level, ItemStack stack, DataComponentType<T> type) {
        requireServer(level);
        stack.remove(type);
    }
    private static void requireServer(Level level) {
        if (level.isClientSide() || level.getServer() == null || !level.getServer().isSameThread())
            throw new IllegalStateException("Server thread required");
    }
    private ForbiddenComponents() { }
}
