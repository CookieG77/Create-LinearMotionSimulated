package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.cookieg.createlinearmotionsimulated.common.registries.PartialModelRegistriesCLM;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PneumaticCylinderRenderer extends SafeBlockEntityRenderer<PneumaticCylinderBlockEntity> {

    public PneumaticCylinderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(PneumaticCylinderBlockEntity be,
                              float partialTicks,
                              PoseStack ms,
                              MultiBufferSource bufferSource,
                              int light,
                              int overlay) {

        PneumaticCylinderBlockEntity controller = be.getControllerBE();
        if (controller == null || !controller.isController())
            return;

        if (be != controller)
            return;

        BlockState state = controller.getBlockState();
        VertexConsumer vb = bufferSource.getBuffer(RenderType.cutoutMipped());

        renderBackShaft(controller, state, partialTicks, ms, vb, light);
    }

    private void renderBackShaft(PneumaticCylinderBlockEntity controller,
                                 BlockState state,
                                 float partialTicks,
                                 PoseStack ms,
                                 VertexConsumer vb,
                                 int light) {

        if (!controller.hasShaftInstalled())
            return;

        BlockPos shaftPos = controller.getShaftPosForRendering();
        if (shaftPos == null)
            return;

        Direction facing = controller.getFacing();

        double localX = shaftPos.getX() - controller.getBlockPos().getX();
        double localY = shaftPos.getY() - controller.getBlockPos().getY();
        double localZ = shaftPos.getZ() - controller.getBlockPos().getZ();

        float shaftAngleDegrees = -controller.getRenderedShaftAngle(partialTicks);

        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            shaftAngleDegrees = -shaftAngleDegrees;

        ms.pushPose();
        ms.translate(localX, localY, localZ);

        CachedBuffers.partial(AllPartialModels.SHAFT_HALF, state)
                .center()
                .rotateToFace(facing)
                .rotateZDegrees(shaftAngleDegrees)
                .uncenter()
                .light(light)
                .renderInto(ms, vb);

        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PneumaticCylinderBlockEntity be) {
        PneumaticCylinderBlockEntity controller = be.getControllerBE();
        return controller != null && controller.isController();
    }
}