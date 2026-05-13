package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PneumaticCylinderRodSegmentRenderer extends SafeBlockEntityRenderer<PneumaticCylinderRodSegmentBlockEntity> {

    public PneumaticCylinderRodSegmentRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PneumaticCylinderRodSegmentBlockEntity be,
                              float partialTicks,
                              PoseStack ms,
                              MultiBufferSource bufferSource,
                              int light,
                              int overlay) {
        /*
         * Rendering is now handled by the blockstate/model so rod segments are
         * visible in contraption diagrams and other static model contexts.
         */
    }
}