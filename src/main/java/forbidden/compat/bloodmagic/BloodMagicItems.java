package forbidden.compat.bloodmagic;

import forbidden.ForbiddenMagic;
import forbidden.compat.botania.BotaniaCompat;
import forbidden.compat.bloodmagic.effects.BloodSealEffect;
import forbidden.compat.bloodmagic.flowers.BloodthornBlock;
import forbidden.compat.bloodmagic.flowers.BloodthornBlockEntity;
import forbidden.compat.bloodmagic.items.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Deferred registers for Blood Magic compatibility (C05–C08).
 * Provides ClassLoader-safe fallback implementations when Blood Magic is absent.
 */
public final class BloodMagicItems {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ForbiddenMagic.ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForbiddenMagic.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMagic.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForbiddenMagic.ID);

    public static final Map<String, Supplier<? extends Item>> ITEMS_MAP = new LinkedHashMap<>();

    // Mob Effects
    public static final DeferredHolder<MobEffect, MobEffect> BLOOD_SEAL = EFFECTS.register("blood_seal", BloodSealEffect::new);

    // Blocks
    public static final DeferredHolder<Block, Block> BLOODTHORN = BLOCKS.register("bloodthorn", () -> new BloodthornBlock());

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloodthornBlockEntity>> BLOODTHORN_BE = ENTITIES.register(
            "bloodthorn",
            () -> BlockEntityType.Builder.of(BloodthornBlockEntity::new, BLOODTHORN.get()).build(null)
    );

    // Items
    public static final DeferredHolder<Item, Item> BLOODTHORN_ITEM = registerGatedItem("bloodthorn", () -> new BlockItem(BLOODTHORN.get(), new Item.Properties()), BloodMagicCompat.isAvailable() && BotaniaCompat.isAvailable());
    public static final DeferredHolder<Item, Item> BLOODWELL = registerBloodMagicItem("bloodwell", () -> {
        if (BloodMagicCompat.isLoaded()) {
            return BloodMagicItemBridge.createBloodwell();
        }
        return new BaseBloodwellItem();
    });
    public static final DeferredHolder<Item, Item> BOUNDWELL = registerBloodMagicItem("boundwell", () -> {
        if (BloodMagicCompat.isLoaded()) {
            return BloodMagicItemBridge.createBoundwell();
        }
        return new BaseBoundwellItem();
    });
    public static final DeferredHolder<Item, Item> BLOOD_RAPIER = registerBloodMagicItem("blood_rapier", () -> {
        if (BloodMagicCompat.isLoaded()) {
            return BloodMagicItemBridge.createBloodRapier();
        }
        return new BaseBloodRapierItem();
    });
    public static final DeferredHolder<Item, Item> ELDRITCH_ORB = registerBloodMagicItem("eldritch_orb", () -> {
        if (BloodMagicCompat.isLoaded()) {
            return BloodMagicItemBridge.createEldritchOrb();
        }
        return new BaseEldritchOrbItem();
    });

    private static DeferredHolder<Item, Item> registerBloodMagicItem(String name, Supplier<Item> supplier) {
        // The release registers these four items only inside the Compat.bm branch.
        return registerGatedItem(name, supplier, BloodMagicCompat.isAvailable());
    }

    private static DeferredHolder<Item, Item> registerGatedItem(String name, Supplier<Item> supplier, boolean enabled) {
        if (!enabled) {
            return DeferredHolder.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID, name));
        }
        return registerItem(name, supplier);
    }

    private static DeferredHolder<Item, Item> registerItem(String name, Supplier<Item> supplier) {
        var holder = ITEMS.register(name, supplier);
        ITEMS_MAP.put(name, holder);
        return holder;
    }

    public static void register(IEventBus bus) {
        if (BloodMagicCompat.isAvailable()) EFFECTS.register(bus);
        // Bloodthorn is a Botania subtile nested inside its Blood Magic branch.
        if (BloodMagicCompat.isAvailable() && BotaniaCompat.isAvailable()) {
            BLOCKS.register(bus);
            ENTITIES.register(bus);
        }
        ITEMS.register(bus);
    }

    private BloodMagicItems() {}
}
