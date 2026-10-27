package forbidden.client;

import forbidden.component.ForbiddenComponents;
import forbidden.content.ForbiddenContent;
import forbidden.content.ForbiddenTools;
import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;

@EventBusSubscriber(modid="forbidden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class P01Models {
    private static final java.util.Map<String,String> RESEARCH_PREFIXES=java.util.Map.ofEntries(
        java.util.Map.entry("forbidden:bloodmagic","[BM] "),
        java.util.Map.entry("forbidden:bloodrapier","[BM] "),
        java.util.Map.entry("forbidden:bloodwell","[BM] "),
        java.util.Map.entry("forbidden:botany","[B] "),
        java.util.Map.entry("forbidden:cap_alchemical","[BM] "),
        java.util.Map.entry("forbidden:cap_elementium","[B] "),
        java.util.Map.entry("forbidden:cap_manasteel","[B] "),
        java.util.Map.entry("forbidden:cap_terrasteel","[B] "),
        java.util.Map.entry("forbidden:cap_vinteum","[AM2] "),
        java.util.Map.entry("forbidden:eldritch_orb","[BM] "),
        java.util.Map.entry("forbidden:eldritchorb","[BM] "),
        java.util.Map.entry("forbidden:euclidaisy","[B] "),
        java.util.Map.entry("forbidden:rod_blood","[BM] "),
        java.util.Map.entry("forbidden:rod_blood_staff","[BM] "),
        java.util.Map.entry("forbidden:rod_dreamwood_staff","[B] "),
        java.util.Map.entry("forbidden:rod_livingwood","[B] "),
        java.util.Map.entry("forbidden:rod_witchwood","[AM2] "),
        java.util.Map.entry("forbidden:rod_witchwood_staff","[AM2] "),
        java.util.Map.entry("forbidden:tainthistle","[B] "),
        java.util.Map.entry("forbidden:whisperweed","[B] ")
    );
    private static final ModelProperty<BlockPos> POSITION=new ModelProperty<>();
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("forbidden",path);}

    @SubscribeEvent
    public static void client(FMLClientSetupEvent event){
        event.enqueueWork(()->{
            thaumcraft.api.client.ResearchTextFormatters.register("forbidden",research->{
                var text=net.minecraft.network.chat.Component.translatable("tc.research_text."+research.key());
                if(!forbidden.config.ForbiddenConfig.TAG_RESEARCH.get())return text;
                String key=research.key();
                String integration=RESEARCH_PREFIXES.getOrDefault(research.key(),"");
                return net.minecraft.network.chat.Component.literal("[FM] "+integration).append(text);
            });
            // ItemDiabolistFork.registerIcons selects the original Spork texture at startup.
            net.minecraft.client.renderer.item.ItemProperties.register(
                ForbiddenTools.DIABOLIST_FORK.get(), id("spork"),
                (stack,level,entity,seed)->forbidden.config.ForbiddenConfig.SPORK.get()?1:0
            );
            net.minecraft.client.renderer.item.ItemProperties.register(
                ForbiddenTools.DIABOLIST_FORK.get(), id("throwing"),
                (stack,level,entity,seed)->entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0
            );
            if(ForbiddenContent.MOB_CRYSTAL!=null) {
                net.minecraft.client.renderer.item.ItemProperties.register(
                    ForbiddenContent.MOB_CRYSTAL.get(),
                    id("filled"),
                    (stack,level,entity,seed)->stack.has(ForbiddenComponents.MOB.get())?1:0
                );
            }
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (ForbiddenContent.CAGE_ENTITY != null) {
            event.registerBlockEntityRenderer(ForbiddenContent.CAGE_ENTITY.get(), WrathCageRenderer::new);
        }
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Morph tools second-layer (tintIndex 1) phase colors: 0=0x980000 (red), 1=0x0010CC (blue), 2=0xE5DA00 (yellow)
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return -1;
            var morph = stack.get(ForbiddenComponents.MORPH.get());
            int phase = morph != null ? morph.phase() : 0;
            return switch (phase) {
                case 1 -> 0xFF0010CC;
                case 2 -> 0xFFE5DA00;
                default -> 0xFF980000;
            };
        },
        ForbiddenTools.MORPH_PICKAXE.get(),
        ForbiddenTools.MORPH_SWORD.get(),
        ForbiddenTools.MORPH_SHOVEL.get(),
        ForbiddenTools.MORPH_AXE.get());
    }

    @SubscribeEvent
    public static void additional(ModelEvent.RegisterAdditional event){
        for(var skin:List.of("gross","gross2"))event.register(ModelResourceLocation.standalone(id("block/taint_log_"+skin)));
    }

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event){
        var models=event.getModels();var sprites=new ArrayList<TextureAtlasSprite>();
        for(var skin:List.of("gross","gross2")){
            var model=models.get(ModelResourceLocation.standalone(id("block/taint_log_"+skin)));
            if(model==null)return;
            var quads=model.getQuads(null,Direction.NORTH,RandomSource.create());if(quads.isEmpty())return;sprites.add(quads.getFirst().getSprite());
        }
        for(var entry:new ArrayList<>(models.entrySet()))if(entry.getKey().id().equals(id("taint_log"))&&!entry.getKey().getVariant().equals("inventory")){
            var original=entry.getValue();models.put(entry.getKey(),new BakedModelWrapper<BakedModel>(original){
                @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return original.getModelData(level,pos,state,data).derive().with(POSITION,pos.immutable()).build();}
                @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
                    var quads=original.getQuads(state,side,random,data,type);var pos=data.get(POSITION);if(pos==null)return quads;
                    return quads.stream().map(quad->{
                        if(!quad.getSprite().contents().name().equals(id("blocks/taint_log_side")))return quad;
                        var selector=new Random(quad.getDirection().ordinal()+pos.getY()+pos.getX()*pos.getZ());
                        if(selector.nextInt(100)<75)return quad;
                        var sprite=sprites.get(selector.nextBoolean()?0:1);return retexture(quad,sprite);
                    }).toList();
                }
            });
        }
    }

    private static BakedQuad retexture(BakedQuad quad,TextureAtlasSprite sprite){
        var old=quad.getSprite();int[] vertices=quad.getVertices().clone();
        for(int i=0;i<vertices.length;i+=8){float u=Float.intBitsToFloat(vertices[i+4]),v=Float.intBitsToFloat(vertices[i+5]);
            vertices[i+4]=Float.floatToRawIntBits(sprite.getU0()+(u-old.getU0())/(old.getU1()-old.getU0())*(sprite.getU1()-sprite.getU0()));
            vertices[i+5]=Float.floatToRawIntBits(sprite.getV0()+(v-old.getV0())/(old.getV1()-old.getV0())*(sprite.getV1()-sprite.getV0()));
        }
        return new BakedQuad(vertices,quad.getTintIndex(),quad.getDirection(),sprite,quad.isShade(),quad.hasAmbientOcclusion());
    }

    private P01Models(){}
}
