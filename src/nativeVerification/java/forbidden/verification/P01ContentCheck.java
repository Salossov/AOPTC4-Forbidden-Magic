package forbidden.verification;

import forbidden.component.ForbiddenComponents;
import forbidden.config.ForbiddenConfig;
import forbidden.content.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandRod;

/** Real registry/config/item/BE serialization fixtures in a JVM without a game loop. */
public final class P01ContentCheck {
    private static int checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        boolean noLust=Boolean.parseBoolean(args[0]),cage=Boolean.parseBoolean(args[1]);
        for(var field:ForbiddenConfig.class.getFields())if(field.get(null) instanceof ModConfigSpec.ConfigValue<?> value){
            var cache=ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");cache.setAccessible(true);cache.set(value,value.getDefault());
        }
        cached(ForbiddenConfig.NO_LUST,noLust);cached(ForbiddenConfig.WRATH_CAGE,cage);
        var bus=BusBuilder.builder().build();ForbiddenComponents.register(bus);ForbiddenContent.register(bus);
        var constructor=RegisterEvent.class.getDeclaredConstructor(ResourceKey.class,Registry.class);constructor.setAccessible(true);
        for(var registry:List.of(BuiltInRegistries.DATA_COMPONENT_TYPE,BuiltInRegistries.BLOCK,BuiltInRegistries.ITEM,BuiltInRegistries.BLOCK_ENTITY_TYPE,BuiltInRegistries.CREATIVE_MODE_TAB)){
            // NeoForge normally opens registries around RegisterEvent. Reproduce
            // that lifecycle locally; no loader, level or server is launched.
            ((MappedRegistry<?>)registry).unfreeze();
            bus.post((RegisterEvent)constructor.newInstance(registry.key(),registry));registry.freeze();
        }
        ForbiddenAspects.register();ForbiddenWands.register();ForbiddenWandVisuals.register();
        for(var model:thaumcraft.api.wands.WandVisuals.models())check(P01ContentCheck.class.getResource("/assets/"+model.getNamespace()+"/models/"+model.getPath()+".json")!=null,"registered composite model exists: "+model);
        check(CageMobCatalog.entries().get(ResourceLocation.parse("minecraft:silverfish"))==Aspect.getAspect("lucrum"),"original silverfish emerald aspect gate");
        cached(ForbiddenConfig.SILVERFISH_EMERALDS,false);
        check(CageMobCatalog.entries().get(ResourceLocation.parse("minecraft:silverfish"))==Aspect.getAspect("bestia"),"original silverfish fallback aspect");
        check(Aspect.all().keySet().stream().filter(id->id.startsWith("forbidden:")).count()==(noLust?6:7),"conditional Luxuria registration");
        check(Aspect.getAspect("forbidden:infernus").getBlend()==771,"release infernus blend");
        check(Aspect.getAspect("forbidden:ira").getComponents().equals(List.of(Aspect.getAspect("telum"),Aspect.getAspect("ignis"))),"release ira composition");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:lust_shard")),"lust ID remains registered in noLust");
        check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:mob_crystal"))==cage,"conditional crystal registration");
        check(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("forbidden:wrath_cage")),"cage remains registered when disabled");
        check(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(ResourceLocation.parse("forbidden:mob_crystals"))==cage,"conditional crystal tab");
        for(var id:List.of("gluttony_shard","arcane_cake","wand_rod_soul","wand_rod_blood","wand_cap_elementium_inert"))check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("forbidden:"+id)),"physical variant "+id);
        var gluttony=ForbiddenContent.item("gluttony_shard").components().get(DataComponents.FOOD);
        var fruit=ForbiddenContent.TAINT_FRUIT.get().components().get(DataComponents.FOOD);
        check(gluttony.nutrition()==2&&gluttony.saturation()==.4F,"gluttony release food");
        check(fruit.nutrition()==4&&fruit.saturation()==6.4F,"taint release food");
        var coal=ForbiddenContent.TAINT_COAL.get();var randomField=TaintCoalItem.class.getDeclaredField("random");randomField.setAccessible(true);((Random)randomField.get(coal)).setSeed(917);
        var expected=new Random(917);for(int i=0;i<32;i++)check(coal.getBurnTime(new ItemStack(coal),null)==1+expected.nextInt(2400),"fuel resamples on every query");
        check(WandRod.rods.get("forbidden:tainted").getCapacity()==150&&WandRod.rods.get("forbidden:tainted").getCraftCost()==12,"tainted rod definition");
        check(WandRod.rods.get("forbidden:neutronium").getCapacity()==9001&&WandRod.rods.get("forbidden:neutronium_staff").getCapacity()==9002,"creative rod capacities");
        check(!WandRod.rods.containsKey("forbidden:soul"),"physical soul core does not activate dead rod");
        var cake=ForbiddenContent.ARCANE_CAKE.get();check(cake.getStateDefinition().getPossibleStates().size()==12,"twelve cake states");
        for(var state:cake.getStateDefinition().getPossibleStates()){
            var shape=state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
            check(!shape.isEmpty(),"all twelve cake portions remain visible/selectable");
            var bounds=shape.bounds();int remaining=12-state.getValue(ArcaneCakeBlock.BITES);
            check(Math.abs(bounds.getXsize()-remaining*14.0/192)<1e-9&&bounds.maxX==15.0/16&&bounds.minZ==1.0/16&&bounds.maxZ==15.0/16,"equal cake slices retain the fixed right edge");
            var hit=shape.clip(new net.minecraft.world.phys.Vec3(-1,.25,.5),new net.minecraft.world.phys.Vec3(2,.25,.5),BlockPos.ZERO);
            check(hit!=null&&Math.abs(hit.getLocation().x-bounds.minX)<1e-9,"each remaining portion can be targeted from its cut face");
            var collision=state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO).bounds();
            check(collision.minX==bounds.minX&&collision.maxX==bounds.maxX&&collision.maxY==7.0/16&&bounds.maxY==.5,"cake collision follows the remaining width and retains release heights");
        }
        var be=new WrathCageEntity(BlockPos.ZERO,ForbiddenContent.WRATH_CAGE.get().defaultBlockState());var data=new CompoundTag();
        data.putBoolean("MobSet",true);data.putString("EntityId","minecraft:creeper");data.putShort("Wrath",(short)63);data.putShort("Sloth",(short)62);data.putShort("Special",(short)61);data.putByte("Mode",(byte)2);data.putShort("Delay",(short)219);data.putShort("Fuel",(short)9);data.putBoolean("Slothful",true);
        be.loadWithComponents(data,RegistryAccess.EMPTY);var saved=be.saveWithoutMetadata(RegistryAccess.EMPTY);
        for(var key:data.getAllKeys())check(data.get(key).equals(saved.get(key)),"cage persistent field "+key);
        check(be.getUpdateTag(RegistryAccess.EMPTY).equals(saved),"cage sync carries the persistent snapshot");
        if(cage){var crystal=be.crystal();check(crystal.get(ForbiddenComponents.MOB.get()).equals(ResourceLocation.parse("minecraft:creeper")),"portable crystal carries entity ID");crystal.remove(ForbiddenComponents.MOB.get());check(be.mob().equals(ResourceLocation.parse("minecraft:creeper")),"crystal copy cannot mutate cage");}
        try{be.exchange(ResourceLocation.parse("minecraft:pig"));throw new AssertionError("unguarded mutation");}catch(IllegalStateException expectedGuard){check(true,"server thread guard");}
        System.out.println("FORBIDDEN_P01_CONTENT_PASS noLust="+noLust+" cage="+cage+" checks="+checks);
    }
    private static void cached(ModConfigSpec.ConfigValue<?> value,Object setting)throws Exception{var f=ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");f.setAccessible(true);f.set(value,setting);}
}
