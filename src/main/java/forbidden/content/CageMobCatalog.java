package forbidden.content;

import com.google.gson.*;
import forbidden.config.ForbiddenConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;

/**
 * Catalog of mob bindings and spawn conditions for the Wrath Cage.
 * Supports native spawn condition queries, safe unknown entity handling,
 * and optional mod mappings without classloading missing mods.
 */
public final class CageMobCatalog {
    public enum SpawnCondition {
        GROUND,
        WATER,
        AIR
    }

    private static final List<JsonObject> RECORDS = load();

    private static List<JsonObject> load() {
        try (var input = CageMobCatalog.class.getResourceAsStream("/data/forbidden/cage_mobs.json")) {
            if (input == null) throw new IllegalStateException("Missing cage mob catalog");
            var result = new ArrayList<JsonObject>();
            for (var value : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray()) {
                result.add(value.getAsJsonObject());
            }
            return List.copyOf(result);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    public static Map<ResourceLocation, Aspect> entries() {
        var result = new LinkedHashMap<ResourceLocation, Aspect>();
        for (var row : RECORDS) {
            if (row.has("silverfishEmeralds") && row.get("silverfishEmeralds").getAsBoolean() != ForbiddenConfig.SILVERFISH_EMERALDS.get()) {
                continue;
            }
            if (row.has("noLust") && row.get("noLust").getAsBoolean() != ForbiddenConfig.NO_LUST.get()) {
                continue;
            }
            if (row.has("config")) {
                String cfg = row.get("config").getAsString();
                if ("twilightForest".equals(cfg) && !ForbiddenConfig.TWILIGHT_FOREST.get()) continue;
                if ("specialMobs".equals(cfg) && !ForbiddenConfig.SPECIAL_MOBS.get()) continue;
            }
            var aspect = Aspect.getAspect(row.get("aspect").getAsString());
            if (aspect == null) continue;
            result.put(ResourceLocation.parse(row.get("mob").getAsString()), aspect);
        }
        return Collections.unmodifiableMap(result);
    }

    public static Aspect getAspect(ResourceLocation mob) {
        if (mob == null) return Aspect.getAspect("lucrum");
        Aspect aspect = entries().get(mob);
        return aspect != null ? aspect : Aspect.getAspect("lucrum");
    }

    public static boolean isKnown(ResourceLocation mob) {
        if (mob == null) return false;
        return entries().containsKey(mob);
    }

    public static SpawnCondition getSpawnCondition(ResourceLocation mob) {
        if (mob == null) return SpawnCondition.GROUND;
        String mobStr = mob.toString();
        for (var row : RECORDS) {
            if (mobStr.equals(row.get("mob").getAsString())) {
                if (row.has("condition")) {
                    String cond = row.get("condition").getAsString().toLowerCase(Locale.ROOT);
                    if ("water".equals(cond)) return SpawnCondition.WATER;
                    if ("air".equals(cond)) return SpawnCondition.AIR;
                    return SpawnCondition.GROUND;
                }
            }
        }
        var optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(mob);
        if (optionalType.isPresent()) {
            var category = optionalType.get().getCategory();
            if (category == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                    || category == net.minecraft.world.entity.MobCategory.WATER_AMBIENT
                    || category == net.minecraft.world.entity.MobCategory.AXOLOTLS) {
                return SpawnCondition.WATER;
            }
        }
        return SpawnCondition.GROUND;
    }

    public static boolean checkNativeSpawnConditions(EntityType<?> type, Level level, BlockPos pos, boolean checkLight) {
        if (type == null || level == null || pos == null) return false;
        Entity entity = type.create(level);
        if (entity == null) return false;
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);

        if (!level.noCollision(entity)) return false;

        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        SpawnCondition condition = getSpawnCondition(mobId);
        boolean inLiquid = level.containsAnyLiquid(entity.getBoundingBox());

        if (condition == SpawnCondition.WATER || entity instanceof WaterAnimal) {
            if (!inLiquid) return false;
        } else {
            if (inLiquid) return false;
            if (condition == SpawnCondition.GROUND) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isAir()) {
                    return false;
                }
            }
        }

        if (checkLight) {
            if (entity instanceof Monster) {
                if (level.getMaxLocalRawBrightness(pos) > 7) return false;
            } else if (entity instanceof Animal) {
                if (level.getRawBrightness(pos, 0) < 9) return false;
            }
        }

        return true;
    }

    private CageMobCatalog() {}
}
