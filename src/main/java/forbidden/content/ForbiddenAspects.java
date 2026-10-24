package forbidden.content;

import forbidden.config.ForbiddenConfig;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

public final class ForbiddenAspects {
    public static void register(){
        if(!ForbiddenConfig.NO_LUST.get())compound("luxuria",16761294,1,"corpus","fames");
        compound("infernus",16711680,771,"ignis","praecantatio");
        compound("superbia",9845247,1,"volatus","vacuos");
        compound("gula",13999174,1,"fames","vacuos");
        compound("invidia",47616,1,"sensus","fames");
        compound("desidia",7237230,771,"vinculum","spiritus");
        compound("ira",8848388,771,"telum","ignis");
    }
    private static void compound(String name,int color,int blend,String first,String second){Aspect.registerCompound(ResourceLocation.fromNamespaceAndPath("forbidden",name),color,ResourceLocation.fromNamespaceAndPath("forbidden","textures/aspects/"+name+".png"),"aspect.forbidden."+name,blend,Aspect.getAspect(first),Aspect.getAspect(second));}
    private ForbiddenAspects(){}
}
