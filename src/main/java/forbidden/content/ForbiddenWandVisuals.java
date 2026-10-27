package forbidden.content;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.wands.*;

/** Uses the core's public composite model pipeline and native TC4 geometry. */
public final class ForbiddenWandVisuals {
    public static void register(){
        var rods = List.of("tainted","infernal","equivalent","profane","profaned","neutronium","blood","livingwood","dreamwood");
        var baseCaps = List.of("iron","gold","copper","silver","thaumium","void");
        var customCaps = List.of(
            List.of("forbidden:orichalcum","orichalcum"),
            List.of("forbidden:alchemical","alchemical"),
            List.of("forbidden:manasteel","manasteel"),
            List.of("forbidden:elementium","elementium"),
            List.of("forbidden:terrasteel","terrasteel")
        );

        for(var rod : rods){
            for(var cap : baseCaps){
                for(var kind : List.of("wand","sceptre")) {
                    WandVisuals.register(kind, new WandParts("forbidden:"+rod, cap), ResourceLocation.fromNamespaceAndPath("forbidden", kind+"/"+rod+"_"+cap));
                }
                if(rod.equals("neutronium")) WandVisuals.register("staff", new WandParts("forbidden:neutronium_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/neutronium_"+cap));
                if(rod.equals("blood")) WandVisuals.register("staff", new WandParts("forbidden:blood_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/blood_"+cap));
                if(rod.equals("dreamwood")) WandVisuals.register("staff", new WandParts("forbidden:dreamwood_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/dreamwood_"+cap));
            }

            for(var capAliases : customCaps){
                String capModel = capAliases.get(1);
                for(var cap : capAliases){
                    for(var kind : List.of("wand","sceptre")) {
                        WandVisuals.register(kind, new WandParts("forbidden:"+rod, cap), ResourceLocation.fromNamespaceAndPath("forbidden", kind+"/"+rod+"_"+capModel));
                    }
                    if(rod.equals("neutronium")) WandVisuals.register("staff", new WandParts("forbidden:neutronium_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/neutronium_"+capModel));
                    if(rod.equals("blood")) WandVisuals.register("staff", new WandParts("forbidden:blood_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/blood_"+capModel));
                    if(rod.equals("dreamwood")) WandVisuals.register("staff", new WandParts("forbidden:dreamwood_staff", cap), ResourceLocation.fromNamespaceAndPath("forbidden", "staff/dreamwood_"+capModel));
                }
            }
        }
    }
    private ForbiddenWandVisuals(){}
}
