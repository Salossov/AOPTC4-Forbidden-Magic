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

        var bloodWand = new ItemStack(item);
        WandApi.parts(bloodWand, new WandParts("forbidden:blood", "forbidden:alchemical"));
        output.accept(bloodWand);

        var bloodStaff = new ItemStack(staffItem);
        WandApi.parts(bloodStaff, new WandParts("forbidden:blood_staff", "forbidden:alchemical"));
        output.accept(bloodStaff);

        var livingwoodWand = new ItemStack(item);
        WandApi.parts(livingwoodWand, new WandParts("forbidden:livingwood", "forbidden:manasteel"));
        output.accept(livingwoodWand);

        var dreamwoodStaff = new ItemStack(staffItem);
        WandApi.parts(dreamwoodStaff, new WandParts("forbidden:dreamwood_staff", "forbidden:elementium"));
        output.accept(dreamwoodStaff);
    }
    private ForbiddenWands(){}
}
