package forbidden.content;

import com.google.gson.*;
import forbidden.config.ForbiddenConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/** Translated vanilla/core entries; unresolved optional mod mappings remain C work. */
public final class CageMobCatalog {
    private static final List<JsonObject> RECORDS=load();
    private static List<JsonObject> load(){
        try(var input=CageMobCatalog.class.getResourceAsStream("/data/forbidden/cage_mobs.json")){
            if(input==null)throw new IllegalStateException("Missing cage mob catalog");
            var result=new ArrayList<JsonObject>();
            for(var value:JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray())result.add(value.getAsJsonObject());
            return List.copyOf(result);
        }catch(IOException error){throw new UncheckedIOException(error);}
    }
    public static Map<ResourceLocation,Aspect> entries(){
        var result=new LinkedHashMap<ResourceLocation,Aspect>();
        for(var row:RECORDS){if(row.has("silverfishEmeralds")&&row.get("silverfishEmeralds").getAsBoolean()!=ForbiddenConfig.SILVERFISH_EMERALDS.get())continue;
            result.put(ResourceLocation.parse(row.get("mob").getAsString()),Aspect.getAspect(row.get("aspect").getAsString()));
        }
        return Collections.unmodifiableMap(result);
    }
    private CageMobCatalog(){}
}
