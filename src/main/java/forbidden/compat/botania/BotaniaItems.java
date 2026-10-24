package forbidden.compat.botania;

import forbidden.ForbiddenMagic;
import forbidden.compat.botania.flowers.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Deferred registers for Botania compatibility (C01–C04).
 */
public final class BotaniaItems {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForbiddenMagic.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMagic.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForbiddenMagic.ID);

    public static final Map<String, Supplier<? extends Item>> ITEMS_MAP = new LinkedHashMap<>();

    // Blocks
    public static final DeferredHolder<Block, Block> EUCLIDAISY = BLOCKS.register("euclidaisy", () -> new EuclidaisyBlock());
    public static final DeferredHolder<Block, Block> WHISPERWEED = BLOCKS.register("whisperweed", () -> new WhisperweedBlock());
    public static final DeferredHolder<Block, Block> TAINTHISTLE = BLOCKS.register("tainthistle", () -> new TainthistleBlock());

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EuclidaisyBlockEntity>> EUCLIDAISY_BE = ENTITIES.register(
            "euclidaisy",
            () -> BlockEntityType.Builder.of(EuclidaisyBlockEntity::new, EUCLIDAISY.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WhisperweedBlockEntity>> WHISPERWEED_BE = ENTITIES.register(
            "whisperweed",
            () -> BlockEntityType.Builder.of(WhisperweedBlockEntity::new, WHISPERWEED.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TainthistleBlockEntity>> TAINTHISTLE_BE = ENTITIES.register(
            "tainthistle",
            () -> BlockEntityType.Builder.of(TainthistleBlockEntity::new, TAINTHISTLE.get()).build(null)
    );

    // Items
    public static final DeferredHolder<Item, Item> EUCLIDAISY_ITEM = registerItem("euclidaisy", () -> new BlockItem(EUCLIDAISY.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WHISPERWEED_ITEM = registerItem("whisperweed", () -> new BlockItem(WHISPERWEED.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TAINTHISTLE_ITEM = registerItem("tainthistle", () -> new BlockItem(TAINTHISTLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BOTANIST_EMBLEM = registerItem("botanist_emblem", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private static DeferredHolder<Item, Item> registerItem(String name, Supplier<Item> supplier) {
        var holder = ITEMS.register(name, supplier);
        ITEMS_MAP.put(name, holder);
        return holder;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }

    private BotaniaItems() {}
}
