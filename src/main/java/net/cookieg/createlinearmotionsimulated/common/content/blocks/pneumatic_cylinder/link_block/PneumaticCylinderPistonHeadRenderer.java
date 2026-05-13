package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class PneumaticCylinderPistonHeadRenderer extends SafeBlockEntityRenderer<PneumaticCylinderPistonHeadBlockEntity> {


    public PneumaticCylinderPistonHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PneumaticCylinderPistonHeadBlockEntity be,
                              float partialTicks,
                              PoseStack ms,
                              MultiBufferSource bufferSource,
                              int light,
                              int overlay) {
        /*
         * Rendering is now handled by the blockstate/model so the piston head
         * is visible in contraption diagrams and static model contexts.
         */
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull PneumaticCylinderPistonHeadBlockEntity be) {
        return false;
    }
}