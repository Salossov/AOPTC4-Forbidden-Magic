package forbidden.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.api.wands.*;

/** Standard part definitions; update behavior is the separate P02 work. */
public final class ForbiddenWands {
    public static void register(){
        rod("tainted",150,12,()->new ItemStack(ForbiddenContent.item("wand_rod_tainted")));
        rod("infernal",150,12,()->new ItemStack(ForbiddenContent.item("wand_rod_infernal")));
        rod("profane",50,12,()->new ItemStack(ForbiddenContent.item("wand_rod_profane")));
        rod("profaned",50,1000,()->new ItemStack(Items.BEDROCK));
        rod("neutronium",9001,1000,()->new ItemStack(Items.BEDROCK));
        new StaffRod("forbidden:neutronium",9002,()->new ItemStack(Items.BEDROCK),1000,null);
    }
    private static void rod(String name,int capacity,int cost,java.util.function.Supplier<ItemStack> item){new WandRod(ResourceLocation.fromNamespaceAndPath("forbidden",name),"forbidden:rod_"+name,capacity,item,cost,null);}
    public static void creative(CreativeModeTab.Output output){
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("thaumcraft:casting_wand")).orElseThrow();
        for(var rod:java.util.List.of("tainted","infernal","profane")){
            var stack=new ItemStack(item);WandApi.parts(stack,new WandParts("forbidden:"+rod,rod.equals("profane")?"iron":"thaumium"));
            if(rod.equals("profane"))for(var aspect:thaumcraft.api.aspects.Aspect.all().values())if(aspect.isPrimal())WandApi.set(stack,aspect,5000);
            output.accept(stack);
        }
    }
    private ForbiddenWands(){}
}
