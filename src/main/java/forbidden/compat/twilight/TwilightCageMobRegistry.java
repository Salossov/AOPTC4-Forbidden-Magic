package forbidden.compat.twilight;

import forbidden.content.CageMobCatalog.SpawnCondition;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/**
 * Registry of the 36 Twilight Forest mob entries for the Wrath Cage spawner.
 * Faithfully mirrors 1.7.10 Compat.java (lines 343–381) with modern namespaced ResourceLocations.
 */
public final class TwilightCageMobRegistry {

    private static TwilightMobEntry mob(String path, String aspect, SpawnCondition condition) {
        return new TwilightMobEntry(ResourceLocation.parse("twilightforest:" + path), aspect, condition);
    }

    private static TwilightMobEntry mobLust(String path, String defaultAspect, SpawnCondition condition, String noLustAspect) {
        return new TwilightMobEntry(ResourceLocation.parse("twilightforest:" + path), defaultAspect, condition, true, noLustAspect);
    }

    public static final TwilightMobEntry WILD_BOAR = mob("wild_boar", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry BIGHORN_SHEEP = mob("bighorn_sheep", "pannus", SpawnCondition.GROUND);
    public static final TwilightMobEntry WILD_DEER = mob("deer", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry REDCAP = mob("redcap", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry SWARM_SPIDER = mob("swarm_spider", "pannus", SpawnCondition.GROUND);
    public static final TwilightMobEntry SKELETON_DRUID = mob("skeleton_druid", "mortuus", SpawnCondition.GROUND);
    public static final TwilightMobEntry HOSTILE_WOLF = mob("hostile_wolf", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry TWILIGHT_WRAITH = mob("wraith", "exanimis", SpawnCondition.AIR);
    public static final TwilightMobEntry HEDGE_SPIDER = mob("hedge_spider", "pannus", SpawnCondition.GROUND);
    public static final TwilightMobEntry PENGUIN = mob("penguin", "volatus", SpawnCondition.GROUND);
    public static final TwilightMobEntry TINY_BIRD = mob("tiny_bird", "volatus", SpawnCondition.AIR);
    public static final TwilightMobEntry FOREST_SQUIRREL = mob("squirrel", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry FOREST_BUNNY = mob("bunny", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry FOREST_RAVEN = mob("raven", "volatus", SpawnCondition.AIR);
    public static final TwilightMobEntry TWILIGHT_KOBOLD = mob("kobold", "lucrum", SpawnCondition.GROUND);
    public static final TwilightMobEntry MOSQUITO_SWARM = mob("mosquito_swarm", "forbidden:gula", SpawnCondition.AIR);
    public static final TwilightMobEntry DEATH_TOME = mob("death_tome", "cognitio", SpawnCondition.GROUND);
    public static final TwilightMobEntry MINOTAUR = mob("minotaur", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry MINOSHROOM = mob("minoshroom", "herba", SpawnCondition.GROUND);
    public static final TwilightMobEntry FIRE_BEETLE = mob("fire_beetle", "ignis", SpawnCondition.GROUND);
    public static final TwilightMobEntry SLIME_BEETLE = mob("slime_beetle", "limus", SpawnCondition.GROUND);
    public static final TwilightMobEntry PINCH_BEETLE = mobLust("pinch_beetle", "forbidden:luxuria", SpawnCondition.GROUND, "bestia");
    public static final TwilightMobEntry MAZE_SLIME = mob("maze_slime", "limus", SpawnCondition.GROUND);
    public static final TwilightMobEntry MIST_WOLF = mob("mist_wolf", "bestia", SpawnCondition.GROUND);
    public static final TwilightMobEntry KING_SPIDER = mob("king_spider", "forbidden:superbia", SpawnCondition.GROUND);
    public static final TwilightMobEntry MINI_GHAST = mob("mini_ghast", "forbidden:infernus", SpawnCondition.AIR);
    public static final TwilightMobEntry TOWER_GHAST = mob("tower_ghast", "forbidden:infernus", SpawnCondition.AIR);
    public static final TwilightMobEntry TOWER_GOLEM = mob("tower_golem", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry TOWER_TERMITE = mob("tower_termite", "praecantatio", SpawnCondition.GROUND);
    public static final TwilightMobEntry UPPER_GOBLIN_KNIGHT = mob("upper_goblin_knight", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry LOWER_GOBLIN_KNIGHT = mob("lower_goblin_knight", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry HELMET_CRAB = mob("helmet_crab", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry KNIGHT_PHANTOM = mob("knight_phantom", "metallum", SpawnCondition.GROUND);
    public static final TwilightMobEntry YETI = mob("yeti", "gelum", SpawnCondition.GROUND);
    public static final TwilightMobEntry WINTER_WOLF = mob("winter_wolf", "gelum", SpawnCondition.GROUND);

    public static final List<TwilightMobEntry> ENTRIES = List.of(
            WILD_BOAR,
            BIGHORN_SHEEP,
            WILD_DEER,
            REDCAP,
            SWARM_SPIDER,
            SKELETON_DRUID,
            HOSTILE_WOLF,
            TWILIGHT_WRAITH,
            HEDGE_SPIDER,
            PENGUIN,
            TINY_BIRD,
            FOREST_SQUIRREL,
            FOREST_BUNNY,
            FOREST_RAVEN,
            TWILIGHT_KOBOLD,
            MOSQUITO_SWARM,
            DEATH_TOME,
            MINOTAUR,
            MINOSHROOM,
            FIRE_BEETLE,
            SLIME_BEETLE,
            PINCH_BEETLE,
            MAZE_SLIME,
            MIST_WOLF,
            KING_SPIDER,
            MINI_GHAST,
            TOWER_GHAST,
            TOWER_GOLEM,
            TOWER_TERMITE,
            UPPER_GOBLIN_KNIGHT,
            LOWER_GOBLIN_KNIGHT,
            HELMET_CRAB,
            KNIGHT_PHANTOM,
            YETI,
            WINTER_WOLF
    );

    private static final Map<ResourceLocation, TwilightMobEntry> BY_ID;

    static {
        var map = new LinkedHashMap<ResourceLocation, TwilightMobEntry>();
        for (var entry : ENTRIES) {
            map.put(entry.entityId(), entry);
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    public static List<TwilightMobEntry> getAll() {
        return ENTRIES;
    }

    public static TwilightMobEntry find(ResourceLocation id) {
        if (id == null) return null;
        return BY_ID.get(id);
    }

    public static boolean isTwilightMob(ResourceLocation id) {
        return id != null && BY_ID.containsKey(id);
    }

    public static Aspect getAspectForMob(ResourceLocation id, boolean noLust) {
        TwilightMobEntry entry = find(id);
        if (entry != null) {
            return entry.getAspect(noLust);
        }
        return null;
    }

    public static SpawnCondition getSpawnCondition(ResourceLocation id) {
        TwilightMobEntry entry = find(id);
        if (entry != null) {
            return entry.spawnCondition();
        }
        return SpawnCondition.GROUND;
    }

    private TwilightCageMobRegistry() {}
}
