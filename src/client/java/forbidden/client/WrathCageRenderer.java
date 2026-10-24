package forbidden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import forbidden.content.WrathCageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Client block entity renderer for the Wrath Cage.
 * Implements inner entity display with redstone rotation pause, scale tuning (Ghast, Slime, Enderman),
 * and 30-degree X tilt matching 0.575 TileEntityWrathCageRenderer.
 */
public class WrathCageRenderer implements BlockEntityRenderer<WrathCageEntity> {
    private final EntityRenderDispatcher entityRenderer;
    private Entity cachedEntity;
    private EntityType<?> cachedType;
    private net.minecraft.world.level.Level cachedLevel;
    private net.minecraft.resources.ResourceLocation cachedMobId;

    public WrathCageRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(WrathCageEntity cage, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var logic = cage.getSpawnerLogic();
        if (logic == null || !logic.isMobSet()) return;
        var mobId = logic.getMobID();
        if (mobId == null) return;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(mobId);
        if (type == null) return;

        var level = cage.getLevel();
        if (level == null) return;

        if (cachedEntity == null || cachedType != type || cachedLevel != level || !mobId.equals(cachedMobId)) {
            cachedEntity = type.create(level);
            cachedType = type;
            cachedLevel = level;
            cachedMobId = mobId;
        }
        if (cachedEntity == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        float scale = 0.4375F;
        String path = mobId.getPath();
        if (path.contains("ghast")) {
            scale = 0.1F;
        } else if (path.contains("slime")) {
            scale = 0.4F;
        } else if (path.contains("enderman")) {
            scale = 0.3F;
        }

        poseStack.translate(0.0D, 0.4D, 0.0D);
        // Pause rotation if powered by redstone
        boolean powered = level.hasNeighborSignal(cage.getBlockPos());
        if (!powered) {
            float rot = (float) (logic.prevSpin + (logic.spin - logic.prevSpin) * (double) partialTick) * 10.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.translate(0.0D, -0.4D, 0.0D);
        poseStack.scale(scale, scale, scale);

        cachedEntity.setYRot(0.0F);
        cachedEntity.setXRot(0.0F);
        entityRenderer.render(cachedEntity, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
