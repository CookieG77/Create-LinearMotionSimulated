package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.cookieg.createlinearmotionsimulated.common.registries.PartialModelRegistriesCLM;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

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

        float amount = be.getLocalExtensionAmount(partialTicks);
        if (amount <= 0.001f)
            return;

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(FACING);
        VertexConsumer vb = bufferSource.getBuffer(RenderType.cutoutMipped());

        float backOffset = be.getVisualBackOffset();

        ms.pushPose();

        ms.translate(
                -facing.getStepX() * backOffset,
                -facing.getStepY() * backOffset,
                -facing.getStepZ() * backOffset
        );

        renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_BACK_HALF, facing, ms, vb, light);

        if (amount > 0.5f)
            renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_FRONT_HALF, facing, ms, vb, light);

        ms.popPose();
    }

    private void renderRodHalf(BlockState state,
                               dev.engine_room.flywheel.lib.model.baked.PartialModel model,
                               Direction facing,
                               PoseStack ms,
                               VertexConsumer vb,
                               int light) {

        SuperByteBuffer buffer = CachedBuffers.partial(model, state)
                .center();

        rotateFromUpTo(buffer, facing);

        buffer.uncenter()
                .light(light)
                .renderInto(ms, vb);
    }

    private void rotateFromUpTo(SuperByteBuffer buffer, Direction facing) {
        switch (facing) {
            case UP -> {
            }
            case DOWN -> buffer.rotateXDegrees(180);
            case NORTH -> buffer.rotateXDegrees(-90);
            case SOUTH -> buffer.rotateXDegrees(90);
            case EAST -> buffer.rotateZDegrees(-90);
            case WEST -> buffer.rotateZDegrees(90);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(PneumaticCylinderRodSegmentBlockEntity be) {
        return be.getLocalExtensionAmount(1.0f) > 0.001f;
    }
}