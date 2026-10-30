package forbidden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import forbidden.content.ForbiddenTools;
import forbidden.content.tools.ThrownDiabolistFork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** One red trident mesh for held, inventory, dropped and projectile views. */
@EventBusSubscriber(modid = "forbidden_aop", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DiabolistForkRenderers {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("forbidden", "textures/entity/diabolist_fork.png");
    private static TridentModel itemModel;

    @SubscribeEvent
    public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> itemModel = null);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    var mc = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                                MultiBufferSource buffers, int light, int overlay) {
                            if (itemModel == null) itemModel = new TridentModel(mc.getEntityModels().bakeLayer(ModelLayers.TRIDENT));
                            poses.pushPose();
                            poses.scale(1, -1, -1);
                            itemModel.renderToBuffer(poses, ItemRenderer.getFoilBufferDirect(buffers,
                                itemModel.renderType(TEXTURE), false, stack.hasFoil()), light, overlay);
                            poses.popPose();
                        }
                    };
                }
                return renderer;
            }
        }, ForbiddenTools.DIABOLIST_FORK.get());
    }

    @SubscribeEvent
    public static void entities(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ForbiddenTools.THROWN_FORK.get(), ForkProjectileRenderer::new);
    }

    private static final class ForkProjectileRenderer extends EntityRenderer<ThrownDiabolistFork> {
        private final TridentModel model;
        ForkProjectileRenderer(EntityRendererProvider.Context context) {
            super(context);
            model = new TridentModel(context.bakeLayer(ModelLayers.TRIDENT));
        }
        @Override
        public void render(ThrownDiabolistFork fork, float yaw, float partialTick, PoseStack poses,
                MultiBufferSource buffers, int light) {
            poses.pushPose();
            poses.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, fork.yRotO, fork.getYRot()) - 90));
            poses.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, fork.xRotO, fork.getXRot()) + 90));
            model.renderToBuffer(poses, ItemRenderer.getFoilBufferDirect(buffers,
                model.renderType(TEXTURE), false, fork.hasFoil()), light, OverlayTexture.NO_OVERLAY);
            poses.popPose();
            super.render(fork, yaw, partialTick, poses, buffers, light);
        }
        @Override public ResourceLocation getTextureLocation(ThrownDiabolistFork fork) { return TEXTURE; }
    }
}
