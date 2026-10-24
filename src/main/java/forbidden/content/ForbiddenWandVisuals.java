package forbidden.content;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.wands.*;

/** Uses the core's public composite model pipeline and native TC4 geometry. */
public final class ForbiddenWandVisuals {
    public static void register(){
        for(var rod:List.of("tainted","infernal","profane","profaned","neutronium"))for(var cap:List.of("iron","gold","copper","silver","thaumium","void")){
            for(var kind:List.of("wand","sceptre"))WandVisuals.register(kind,new WandParts("forbidden:"+rod,cap),ResourceLocation.fromNamespaceAndPath("forbidden",kind+"/"+rod+"_"+cap));
            if(rod.equals("neutronium"))WandVisuals.register("staff",new WandParts("forbidden:neutronium_staff",cap),ResourceLocation.fromNamespaceAndPath("forbidden","staff/neutronium_"+cap));
        }
    }
    private ForbiddenWandVisuals(){}
}
