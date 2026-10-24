package forbidden.content;

import forbidden.ForbiddenMagic;
import forbidden.config.ForbiddenConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** P01 registrations. Physical legacy metadata variants retain independent IDs. */
public final class ForbiddenContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForbiddenMagic.ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForbiddenMagic.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForbiddenMagic.ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForbiddenMagic.ID);
    public static final Map<String, Supplier<? extends Item>> MAIN = new LinkedHashMap<>();
    public static final Supplier<Block> INK_FLOWER = block("ink_flower", () -> new InkFlowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)));
    public static final Supplier<Block> UMBRAL_BUSH = block("umbral_bush", () -> new UmbralBushBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ROSE_BUSH)));
    public static final Supplier<Block> TAINT_LOG = block("taint_log", () -> new TaintLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2).sound(SoundType.SLIME_BLOCK)));
    public static final Supplier<Block> TAINT_PLANKS = block("taint_planks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2, 5)));
    public static final Supplier<Block> TAINT_LEAVES = block("taint_leaves", () -> new TaintLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).sound(SoundType.SLIME_BLOCK)));
    public static final Supplier<Block> TAINT_SAPLING = block("taint_sapling", () -> new TaintSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).sound(SoundType.SLIME_BLOCK)));
    public static final Supplier<Block> TAINT_STONE = block("taint_stone", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2, 10)));
    public static final Supplier<Block> TAINT_BRICKS = block("taint_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2, 10)));
    public static final Supplier<Block> STAR_BLOCK = block("star_block", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5)));
    public static final Supplier<Block> ARCANE_CAKE = BLOCKS.register("arcane_cake", () -> new ArcaneCakeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE).strength(.5F).randomTicks()));
    public static final Supplier<Block> WRATH_CAGE = block("wrath_cage", () -> new WrathCageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPAWNER).strength(5, 2000).sound(SoundType.METAL).noOcclusion()));
    public static final Supplier<Block> HERETIC_STUDY = block("heretic_study", () -> new HereticStudyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN).strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WrathCageEntity>> CAGE_ENTITY = ENTITIES.register("wrath_cage", () -> BlockEntityType.Builder.of(WrathCageEntity::new, WRATH_CAGE.get()).build(null));
    public static final Supplier<Item> TAINT_FRUIT = item("taint_fruit", () -> new TaintFruitItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(.8F).build())));
    public static final Supplier<Item> TAINT_COAL = item("taint_coal", () -> new TaintCoalItem(new Item.Properties()));
    public static final Supplier<Item> BLINK_FOCUS = item("blink_focus", () -> new forbidden.content.focus.BlinkFocusItem());
    public static Supplier<Item> MOB_CRYSTAL;

    private static Supplier<Block> block(String id, Supplier<Block> factory) {
        var block = BLOCKS.register(id, factory);
        item(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    private static Supplier<Item> item(String id, Supplier<Item> factory) {
        var item = ITEMS.register(id, factory); MAIN.put(id, item); return item;
    }
    public static Item item(String id) { return MAIN.get(id).get(); }
    public static void register(IEventBus bus) {
        for (String vice : new String[]{"wrath", "envy", "taint", "pride", "lust", "sloth", "greed"}) {
            item(vice + "_shard", () -> new Item(new Item.Properties()) {
                @Override public Component getName(ItemStack stack) {
                    return Component.translatable("item.forbidden." + (vice.equals("lust") && ForbiddenConfig.NO_LUST.get() ? "despair" : vice) + "_shard");
                }
            });
        }
        item("gluttony_shard", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build())));
        for (String id : new String[]{"emerald_nugget", "manasteel_nugget", "elementium_nugget", "tainted_blood_shard"}) item(id, () -> new Item(new Item.Properties()));
        item("black_dye", () -> new ShadowInkItem(new Item.Properties()));
        for (String type : new String[]{"tainted", "infernal", "soul", "blood", "witchwood", "profane", "blood_inert", "livingwood", "livingwood_inert", "blood_staff", "witchwood_staff", "dreamwood", "dreamwood_inert", "dreamwood_staff"}) item("wand_rod_" + type, () -> new Item(new Item.Properties()));
        for (String type : new String[]{"alchemical", "vinteum", "terrasteel", "manasteel", "manasteel_inert", "elementium", "elementium_inert"}) item("wand_cap_" + type, () -> new Item(new Item.Properties()));
        item("arcane_cake", () -> new BlockItem(ARCANE_CAKE.get(), new Item.Properties()));
        if (ForbiddenConfig.WRATH_CAGE.get()) MOB_CRYSTAL = ITEMS.register("mob_crystal", () -> new MobCrystalItem(new Item.Properties().stacksTo(1)));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.forbidden")).icon(() -> new ItemStack(item("wrath_shard")))
            .displayItems((parameters, output) -> {MAIN.values().forEach(i -> output.accept(i.get()));ForbiddenWands.creative(output);}).build());
        if (ForbiddenConfig.WRATH_CAGE.get()) TABS.register("mob_crystals", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.forbidden.mobcrystal"))
            .icon(() -> new ItemStack(MOB_CRYSTAL.get())).displayItems((parameters, output) -> {
                output.accept(MOB_CRYSTAL.get());
                for(var id:CageMobCatalog.entries().keySet())if(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)){
                    var crystal=new ItemStack(MOB_CRYSTAL.get());crystal.set(forbidden.component.ForbiddenComponents.MOB.get(),id);output.accept(crystal);
                }
            }).build());
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(thaumcraft.api.aspects.EssentiaCapabilities.TRANSPORT, CAGE_ENTITY.get(), (be, side) -> be);
            event.registerBlockEntity(thaumcraft.api.aspects.EssentiaCapabilities.CONTAINER, CAGE_ENTITY.get(), (be, side) -> be);
        });
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
    }
    private ForbiddenContent() { }
}
