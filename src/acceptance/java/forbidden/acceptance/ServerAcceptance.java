package forbidden.acceptance;

import com.google.gson.GsonBuilder;
import forbidden.component.*;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import forbidden.content.tools.MorphToolHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ResearchApi;

/** Runs inside a real dedicated server against the packaged gameplay JAR; never ships in it. */
@Mod("forbidden_acceptance")
public final class ServerAcceptance {
    private static final BlockPos STORAGE = new BlockPos(0, 81, 0);
    private static final BlockPos CAGE = new BlockPos(2, 81, 0);
    private static final BlockPos ACTIVE = new BlockPos(16, 81, 0);
    private static final UUID OWNER = UUID.fromString("98e10829-7cf6-4f3b-9baf-4d52c53915b6");
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final String phase = System.getProperty("forbidden.acceptance.phase", "write");
    private final String runId = System.getProperty("forbidden.acceptance.runId", "unassigned");
    private final String profile = System.getProperty("forbidden.acceptance.profile", "base");
    private MinecraftServer server;
    private int ticks;
    private boolean complete;
    private Throwable failure;
    public ServerAcceptance() {
        try {
            var process = ProcessHandle.current();
            Files.writeString(Path.of("acceptance-process.json"), new GsonBuilder().create().toJson(Map.of(
                "runId", runId, "profile", profile, "phase", phase, "pid", process.pid(),
                "startedAt", process.info().startInstant().orElseThrow().toEpochMilli())));
        } catch (Exception error) { throw new IllegalStateException("Cannot record acceptance process ownership", error); }
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopped);
    }
    private void check(String id, boolean ok) {
        record(id, ok);
        if (!ok) throw new AssertionError(id);
    }
    private void record(String id, boolean ok) { checks.add(Map.of("id", id, "passed", ok)); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("forbidden", path); }
    private static Item item(String path) {
        var key = id(path);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new AssertionError("Missing item " + key);
        return BuiltInRegistries.ITEM.get(key);
    }
    private void started(ServerStartedEvent event) {
        server = event.getServer();
        try {
            var level = server.overworld();
            level.setChunkForced(0, 0, true);
            level.setChunkForced(1, 0, true);
            // Force actual data/tag/recipe snapshots and prove no main output shadows the JAR.
            String origin = ForbiddenContent.class.getProtectionDomain().getCodeSource().getLocation().toString();
            check("packaged_gameplay_origin", origin.contains("ForbiddenMagic-1.21.1-") && origin.contains(".jar"));
            check("server_thread", server.isSameThread());
            check("requested_no_lust_profile", ForbiddenConfig.NO_LUST.get() == profile.equals("no-lust"));
            check("requested_no_cage_profile", ForbiddenConfig.WRATH_CAGE.get() != profile.equals("no-cage"));
            check("requested_no_enchantments_profile", ForbiddenConfig.ENCHANTING.get() != profile.equals("no-enchantments"));
            check("requested_free_cage_profile", ForbiddenConfig.WRATH_COST.get() == (profile.equals("free-cage") ? 0 : 5));
            check("cage_registered_even_if_disabled", BuiltInRegistries.BLOCK.containsKey(id("wrath_cage")));
            check("crystal_startup_gate", BuiltInRegistries.ITEM.containsKey(id("mob_crystal")) == ForbiddenConfig.WRATH_CAGE.get());
            check("collar_startup_gate", BuiltInRegistries.ITEM.containsKey(id("sub_collar")) != ForbiddenConfig.NO_LUST.get());
            check("luxuria_startup_gate", (Aspect.getAspect("forbidden:luxuria") != null) != ForbiddenConfig.NO_LUST.get());
            check("lust_shard_stable_id", BuiltInRegistries.ITEM.containsKey(id("lust_shard")));
            check("lust_shard_display_switch", new ItemStack(item("lust_shard")).getItem().getName(new ItemStack(item("lust_shard"))).getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("item.forbidden." + (ForbiddenConfig.NO_LUST.get() ? "despair" : "lust") + "_shard"));
            check("gluttony_items_survive_config", BuiltInRegistries.ITEM.containsKey(id("ring_nutrition")) && BuiltInRegistries.ITEM.containsKey(id("arcane_cake")));
            var enchantments = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            check("enchantment_registry_wrath", enchantments.containsKey(id("wrath")) == ForbiddenConfig.ENCHANTING.get());
            check("recipe_manager_populated", server.getRecipeManager().getRecipes().stream().anyMatch(r -> r.id().getNamespace().equals("forbidden")));
            check("requested_gluttony_profile", ForbiddenConfig.GLUTTONY.get() == (profile.equals("gluttony-off") ? 1 : profile.equals("gluttony-hard") ? 2 : 0));
            check("collar_recipe_gate", server.getRecipeManager().byKey(id("sub_collar")).isPresent() != ForbiddenConfig.NO_LUST.get());
            check("cage_recipe_gate", server.getRecipeManager().byKey(id("wrath_cage")).isPresent() == ForbiddenConfig.WRATH_CAGE.get());
            check("ring_recipe_gate", server.getRecipeManager().byKey(id("ring_food")).isPresent() == (ForbiddenConfig.GLUTTONY.get() != 1));
            if (ForbiddenConfig.GLUTTONY.get() != 1) {
                var ring = (thaumcraft.api.crafting.IArcaneRecipe) server.getRecipeManager().byKey(id("ring_food")).orElseThrow().value();
                check("ring_original_vis", ring.vis().equals(Map.of("perditio", 10, "aqua", 20, "terra", 20)));
                var inputs = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
                inputs.set(1, new ItemStack(ForbiddenConfig.GLUTTONY.get() == 2 ? item("star_block") : Items.DIAMOND));
                inputs.set(4, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ring_blank"))));
                for (int slot : List.of(3, 5, 7)) inputs.set(slot, new ItemStack(item("gluttony_shard")));
                var crafting = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
                check("ring_original_pattern_and_ingredients", ring.matches(crafting, level));
                check("ring_original_output", ring.assemble(crafting, level.registryAccess()).is(item("ring_nutrition")));
                inputs.set(1, new ItemStack(ForbiddenConfig.GLUTTONY.get() == 2 ? Items.DIAMOND : item("star_block")));
                check("ring_rejects_wrong_gluttony_variant", !ring.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs), level));
            }
            var definitions = ResearchApi.definitions(FakePlayerFactory.getMinecraft(level));
            check("collar_research_gate", definitions.containsKey("forbidden:subcollar") != ForbiddenConfig.NO_LUST.get());
            check("cage_research_gate", definitions.containsKey("forbidden:wrathcage") == ForbiddenConfig.WRATH_CAGE.get());
            check("fork_research_gate", definitions.containsKey("forbidden:fork") == ForbiddenConfig.WRATH_CAGE.get());
            check("gluttony_research_gate", definitions.containsKey("forbidden:ringfood") == (ForbiddenConfig.GLUTTONY.get() != 1));
            check("enchantment_research_gate", definitions.containsKey("forbidden:wrath") == ForbiddenConfig.ENCHANTING.get());
            boolean schools = forbidden.compat.botania.BotaniaCompat.isAvailable() || forbidden.compat.bloodmagic.BloodMagicCompat.isAvailable();
            check("schools_research_gate", definitions.containsKey("forbidden:schools") == schools);
            check("no_lust_book_pages", definitions.get("forbidden:nethershards").textPages().contains("forbidden.research_page.NETHERSHARDS." + (ForbiddenConfig.NO_LUST.get() ? "2b" : "2")));
            if (net.neoforged.fml.ModList.get().isLoaded("botania")) {
                if (phase.equals("write")) BotaniaScenarios.run(level, this::check);
                else BotaniaScenarios.verifyPersisted(level, this::check);
            }
            check("bloodthorn_recipe_requires_both_mods", server.getRecipeManager().byKey(id("bloodthorn")).isPresent()
                == (forbidden.compat.botania.BotaniaCompat.isAvailable() && forbidden.compat.bloodmagic.BloodMagicCompat.isAvailable()));
            if (net.neoforged.fml.ModList.get().isLoaded("thaumictinkerer_aop")) {
                var catalog = Class.forName("thaumic.tinkerer.enchantment.EnchanterCatalog");
                for (String name : List.of("wrath", "cluster", "greedy", "consuming", "educational", "corrupting", "voidtouched", "impact"))
                    check("T01_live_receiver_" + name, catalog.getMethod("find", ResourceLocation.class).invoke(null, id(name)) != null);
            }
            var aspects = AspectApi.aspects(level, new ItemStack(Items.GHAST_TEAR));
            check("core_aspects_survive_additions", aspects.getOrDefault("spiritus", 0) > 0);
            check("FM_aspect_addition_loaded", aspects.getOrDefault("forbidden:ira", 0) >= 4);
            for (String aspect : List.of("infernus", "superbia", "gula", "invidia", "desidia", "ira"))
                check("aspect_" + aspect, Aspect.getAspect("forbidden:" + aspect) != null);
            if (phase.equals("write")) prepare(level);
            else if (!phase.equals("read")) throw new IllegalArgumentException("Unknown phase " + phase);
        } catch (Throwable error) { finish(error); }
    }
    private void prepare(ServerLevel level) {
        for (int x = -8; x <= 24; x++) for (int z = -8; z <= 8; z++)
            level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(STORAGE, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) level.getBlockEntity(STORAGE);
        var pick = new ItemStack(item("morph_pickaxe"));
        var phaseOne = new MorphState.Phase(Map.of(ResourceLocation.parse("minecraft:silk_touch"), 1), Optional.of(Component.literal("Survey Pick")));
        ForbiddenComponents.morph(level, pick, new MorphState(1, List.of(MorphState.Phase.EMPTY, phaseOne, MorphState.Phase.EMPTY)));
        pick.set(DataComponents.CUSTOM_NAME, Component.literal("Survey Pick"));
        pick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        pick.setDamageValue(17);
        chest.setItem(0, pick);
        var contract = new ItemStack(item("wand_rod_profane"));
        ForbiddenComponents.contract(level, contract, 12345);
        chest.setItem(1, contract);
        if (!ForbiddenConfig.NO_LUST.get()) {
            var collar = new ItemStack(item("sub_collar"));
            ForbiddenComponents.collar(level, collar, new CollarState(Optional.of(OWNER), "Acceptance Owner", Map.of("ignis", 1234, "aqua", 25000)));
            chest.setItem(2, collar);
        }
        if (ForbiddenConfig.WRATH_CAGE.get()) {
            var crystal = new ItemStack(item("mob_crystal"));
            ForbiddenComponents.mob(level, crystal, ResourceLocation.parse("minecraft:blaze"));
            chest.setItem(3, crystal);
        }
        chest.setChanged();
        level.setBlockAndUpdate(CAGE, ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
        level.setBlockAndUpdate(CAGE.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var cage = (WrathCageEntity) level.getBlockEntity(CAGE);
        cage.setMob(ResourceLocation.parse("minecraft:blaze"));
        cage.setWrath(16); cage.setSloth(32); cage.setSpecial(7);
        cage.getSpawnerLogic().setFuel(3);
        cage.getSpawnerLogic().setSpawnDelay(123);
        cage.cycleMode();
        cage.sync();
        check("cage_redstone_paused", level.hasNeighborSignal(CAGE));
        // Exercise F01 with the real registered transport and world capability adapter.
        var transport = level.getCapability(EssentiaCapabilities.TRANSPORT, CAGE, Direction.UP);
        check("cage_transport_capability", transport == cage);
        var ira = Aspect.getAspect("forbidden:ira");
        cage.setWrath(63);
        if (ForbiddenConfig.WRATH_COST.get() > 0)
            check("cage_F01_accepts_only_one", cage.addEssentia(ira, 4, Direction.UP) == 1 && cage.getWrath() == 64);
        else check("V37_free_cage_rejects_essentia",cage.addEssentia(ira,4,Direction.UP) == 0 && cage.getWrath() == 63);
        cage.setWrath(16);
        if (ForbiddenConfig.WRATH_CAGE.get()) {
            level.setBlockAndUpdate(ACTIVE, ForbiddenContent.WRATH_CAGE.get().defaultBlockState());
            var active = (WrathCageEntity) level.getBlockEntity(ACTIVE);
            active.setMob(ResourceLocation.parse("minecraft:pig"));
            active.setWrath(32);
            active.getSpawnerLogic().setSpawnDelay(0);
            active.sync();
        }
        // Use the real Morph transition, registry-backed enchantment and server-side player.
        var fake = FakePlayerFactory.getMinecraft(level);
        fake.setShiftKeyDown(true);
        fake.setItemInHand(InteractionHand.MAIN_HAND, pick.copy());
        var cycled = fake.getMainHandItem();
        int damage = cycled.getDamageValue();
        MorphToolHelper.cyclePhase(cycled, fake, level, InteractionHand.MAIN_HAND);
        check("morph_advances_phase", cycled.get(ForbiddenComponents.MORPH.get()).phase() == 2);
        check("morph_charges_five_damage", cycled.getDamageValue() == damage + 5);
        check("morph_retains_inactive_name_and_enchants", cycled.get(ForbiddenComponents.MORPH.get()).phases().get(1).enchantments().get(ResourceLocation.parse("minecraft:silk_touch")) == 1);
        if (profile.equals("base")) {
            WorldScenarios.prepare(level, this::record);
            ToolWorldScenarios.run(level, this::record);
            WandWorldScenarios.prepare(level, chest, this::record);
            CageWorldScenarios.prepare(level, this::record);
        }
        if (profile.equals("free-cage")) CageWorldScenarios.prepareFlow(level,true,this::record);
    }
    private void verifyStorage(ServerLevel level) {
        var chest = (ChestBlockEntity) level.getBlockEntity(STORAGE);
        check("chest_exists_after_world_load", chest != null);
        var pick = chest.getItem(0);
        if (profile.equals("base")) WandWorldScenarios.verifyStorage(level, chest, this::record);
        check("morph_stored_phase", pick.get(ForbiddenComponents.MORPH.get()).phase() == 1);
        check("morph_stored_damage", pick.getDamageValue() == 17);
        check("morph_stored_name", pick.getHoverName().getString().equals("Survey Pick"));
        check("morph_stored_enchantment", pick.getEnchantments().getLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)) == 1);
        check("contract_stored", chest.getItem(1).get(ForbiddenComponents.CONTRACT.get()) == 12345);
        if (!ForbiddenConfig.NO_LUST.get()) {
            var collar = chest.getItem(2).get(ForbiddenComponents.COLLAR.get());
            check("collar_stored_owner", collar.owner().orElseThrow().equals(OWNER));
            check("collar_stored_vis", collar.vis().equals(Map.of("ignis", 1234, "aqua", 25000)));
        }
        if (ForbiddenConfig.WRATH_CAGE.get()) check("crystal_stored_mob", chest.getItem(3).get(ForbiddenComponents.MOB.get()).equals(ResourceLocation.parse("minecraft:blaze")));
        var cage = (WrathCageEntity) level.getBlockEntity(CAGE);
        check("cage_stored_mob", ResourceLocation.parse("minecraft:blaze").equals(cage.mob()));
        check("cage_stored_buffers", cage.getWrath() == 16 && cage.getSloth() == 32 && cage.getSpecial() == 7);
        check("cage_stored_mode", cage.mode() == 1);
        check("cage_stored_delay_fuel", cage.getSpawnerLogic().getSpawnDelay() == 123 && cage.getSpawnerLogic().getFuel() == 3);
    }
    private void tick(ServerTickEvent.Post event) {
        if (server == null || complete) return;
        if (++ticks != (phase.equals("write") && profile.equals("base") ? 260 : 100)) return;
        try {
            var level = server.overworld();
            verifyStorage(level);
            if (profile.equals("base") && phase.equals("write")) WorldScenarios.afterTicks(level, this::record);
            if (profile.equals("base") || profile.equals("free-cage")) CageWorldScenarios.afterTicks(level,profile.equals("free-cage"),this::record);
            if (ForbiddenConfig.WRATH_CAGE.get() && phase.equals("write")) {
                int pigs = level.getEntities(EntityType.PIG, new AABB(ACTIVE).inflate(12), e -> true).size();
                check("cage_spawns_on_live_server_without_players", pigs > 0);
                if (profile.equals("free-cage")) {
                    var active=(WrathCageEntity)level.getBlockEntity(ACTIVE);
                    check("V37_free_cage_spawns_without_fuel_or_consumption",active.getWrath() == 32 && active.getSpawnerLogic().getFuel() <= 0);
                }
                check("no_real_players_needed", server.getPlayerList().getPlayerCount() == 0);
                // Freeze the dynamic scene before lifecycle restart assertions.
                level.setBlockAndUpdate(ACTIVE.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
            server.saveEverything(false, true, true);
            check("world_save_flushed", Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("level.dat")));
            finish(null);
        } catch (Throwable error) { finish(error); }
    }
    private void finish(Throwable error) {
        if (error == null && checks.stream().anyMatch(c -> Boolean.FALSE.equals(c.get("passed"))))
            error = new AssertionError("Failed checks: " + checks.stream().filter(c -> Boolean.FALSE.equals(c.get("passed"))).map(c -> c.get("id")).toList());
        failure = error;
        complete = true;
        if (error != null) error.printStackTrace();
        writeReport(false);
        server.halt(false);
    }
    private void stopped(ServerStoppedEvent event) {
        if (!complete && failure == null) failure = new IllegalStateException("Server stopped before acceptance completed");
        writeReport(true);
    }
    private void writeReport(boolean stopped) {
        try {
            var report = new LinkedHashMap<String, Object>();
            report.put("runId", runId); report.put("profile", profile); report.put("phase", phase);
            report.put("serverStopped", stopped); report.put("passed", complete && failure == null);
            report.put("processId", ProcessHandle.current().pid()); report.put("ticks", ticks);
            report.put("checks", checks); report.put("error", failure == null ? null : failure.toString());
            report.put("gameplayOrigin", ForbiddenContent.class.getProtectionDomain().getCodeSource().getLocation().toString());
            report.put("gameplaySha256", HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(Path.of(System.getProperty("forbidden.acceptance.jar"))))));
            report.put("loadedMods", net.neoforged.fml.ModList.get().getMods().stream()
                .map(m -> m.getModId() + ":" + m.getVersion()).sorted().toList());
            Path target = Path.of("acceptance-" + phase + ".json").toAbsolutePath();
            Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            System.out.println("FM_ACCEPTANCE_" + (failure == null ? "PASS" : "FAIL") + " profile=" + profile + " phase=" + phase + " checks=" + checks.size() + " stopped=" + stopped);
        } catch (Exception error) { error.printStackTrace(); }
    }
}
