package forbidden.content;

import forbidden.compat.bloodmagic.BloodMagicCompat;
import forbidden.compat.botania.BotaniaCompat;
import forbidden.content.wands.ForbiddenWandMechanics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.api.wands.*;

/** Standard part definitions and P02 wand updates, caps and foci. */
public final class ForbiddenWands {
    public static void register(){
        ForbiddenWandMechanics.registerAll();
        BloodMagicCompat.registerWandParts();
        BotaniaCompat.registerWandParts();
    }
    public static void creative(CreativeModeTab.Output output){
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("thaumcraft:casting_wand")).orElseThrow();
        for(var rod:java.util.List.of("tainted","infernal","profane")){
            var stack=new ItemStack(item);WandApi.parts(stack,new WandParts("forbidden:"+rod,rod.equals("profane")?"iron":"thaumium"));
            if(rod.equals("profane"))for(var aspect:thaumcraft.api.aspects.Aspect.all().values())if(aspect.isPrimal())WandApi.set(stack,aspect,5000);
            output.accept(stack);
        }
        var creativeWand = new ItemStack(item);
        WandApi.parts(creativeWand, new WandParts("forbidden:neutronium", "forbidden:orichalcum"));
        for (var aspect : thaumcraft.api.aspects.Aspect.all().values()) if (aspect.isPrimal()) WandApi.set(creativeWand, aspect, WandApi.capacity(creativeWand));
        output.accept(creativeWand);

        var staffItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("thaumcraft:casting_staff")).orElse(item);
        var creativeStaff = new ItemStack(staffItem);
        WandApi.parts(creativeStaff, new WandParts("forbidden:neutronium_staff", "forbidden:orichalcum"));
        for (var aspect : thaumcraft.api.aspects.Aspect.all().values()) if (aspect.isPrimal()) WandApi.set(creativeStaff, aspect, WandApi.capacity(creativeStaff));
        output.accept(creativeStaff);

        if(BloodMagicCompat.isAvailable()&&forbidden.config.ForbiddenConfig.CROSS_WAND.get()){
            for(String rod:java.util.List.of("blood","blood_staff")){
                var stack=new ItemStack(rod.endsWith("_staff")?staffItem:item);
                WandApi.parts(stack,new WandParts("forbidden:"+rod,"forbidden:alchemical"));output.accept(stack);
            }
        }
        if(BotaniaCompat.isAvailable()&&forbidden.config.ForbiddenConfig.CROSS_WAND.get()){
            for(String rod:java.util.List.of("livingwood","dreamwood","dreamwood_staff")){
                var stack=new ItemStack(rod.endsWith("_staff")?staffItem:item);
                WandApi.parts(stack,new WandParts("forbidden:"+rod,"forbidden:"+(rod.equals("livingwood")?"manasteel":"elementium")));output.accept(stack);
            }
        }
    }
    private ForbiddenWands(){}
}
