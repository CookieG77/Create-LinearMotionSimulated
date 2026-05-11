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

        BlockState state = be.getBlockState();
        VertexConsumer vb = bufferSource.getBuffer(RenderType.cutoutMipped());

        renderBackShaft(be, state, ms, vb, light);

        PneumaticCylinderBlockEntity controller = be.getControllerBE();
        if (controller == null || !controller.isController())
            return;

        if (be != controller)
            return;

        if (!controller.isAssembled())
            return;

        float extension = controller.getRenderedExtension(partialTicks);
        if (extension <= 0.001f)
            return;

        renderRod(controller, state, extension, ms, vb, light);
    }

    private void renderBackShaft(PneumaticCylinderBlockEntity be,
                                 BlockState state,
                                 PoseStack ms,
                                 VertexConsumer vb,
                                 int light) {

        if (!state.getValue(PneumaticCylinderBlock.HAS_SHAFT))
            return;

        Direction facing = state.getValue(PneumaticCylinderBlock.FACING);
        Direction back = facing.getOpposite();

        ms.pushPose();

        /*
         * AllPartialModels.SHAFT_HALF is a Create partial.
         * Source orientation is vertical/Y by default, so we rotate from UP to the wanted back direction.
         *
         * The model is placed around the block center, then moved toward the rear face.
         * A half-shaft should occupy roughly half a block behind/inside the rear side.
         */
        CachedBuffers.partial(AllPartialModels.SHAFT_HALF, state)
                .rotateToFace(back)
                .translate(
                        0.5 + back.getStepX() * 0.25,
                        0.5 + back.getStepY() * 0.25,
                        0.5 + back.getStepZ() * 0.25
                )
                .light(light)
                .renderInto(ms, vb);

        ms.popPose();
    }

    private void renderRod(PneumaticCylinderBlockEntity controller,
                           BlockState state,
                           float extension,
                           PoseStack ms,
                           VertexConsumer vb,
                           int light) {

        Direction facing = controller.getFacing();

        /*
         * Rod start:
         * - The body occupies `height` blocks.
         * - The rod exits from the front side of the FRONT/TOP block.
         *
         * Since the renderer origin is the controller block position, we need the offset
         * from controller to the logical front.
         */
        int height = controller.getHeight();
        BlockPos back = controller.getBackOrigin();
        BlockPos front = back.relative(facing, height - 1);

        double localFrontX = front.getX() - controller.getBlockPos().getX();
        double localFrontY = front.getY() - controller.getBlockPos().getY();
        double localFrontZ = front.getZ() - controller.getBlockPos().getZ();

        /*
         * Start at center of the front block, then move to its front face.
         */
        double startX = localFrontX + 0.5 + facing.getStepX() * 0.5;
        double startY = localFrontY + 0.5 + facing.getStepY() * 0.5;
        double startZ = localFrontZ + 0.5 + facing.getStepZ() * 0.5;

        int fullSegments = (int) Math.floor(extension);
        float partial = extension - fullSegments;

        ms.pushPose();

        for (int i = 0; i < fullSegments; i++) {
            renderRodFullBlock(state, facing, startX, startY, startZ, i, ms, vb, light);
        }

        if (partial > 0.001f) {
            renderRodPartialBlock(state, facing, startX, startY, startZ, fullSegments, partial, ms, vb, light);
        }

        ms.popPose();
    }

    private void renderRodFullBlock(BlockState state,
                                    Direction facing,
                                    double startX,
                                    double startY,
                                    double startZ,
                                    int index,
                                    PoseStack ms,
                                    VertexConsumer vb,
                                    int light) {

        double x = startX + facing.getStepX() * index;
        double y = startY + facing.getStepY() * index;
        double z = startZ + facing.getStepZ() * index;

        /*
         * A complete one-block rod is made of the two half models:
         * - block_extended_1: back half, y=0..8
         * - block_extended_0: front half, y=8..16
         */
        renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_BACK_HALF,
                facing, x, y, z, ms, vb, light);

        renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_FRONT_HALF,
                facing, x, y, z, ms, vb, light);
    }

    private void renderRodPartialBlock(BlockState state,
                                       Direction facing,
                                       double startX,
                                       double startY,
                                       double startZ,
                                       int index,
                                       float partial,
                                       PoseStack ms,
                                       VertexConsumer vb,
                                       int light) {

        double x = startX + facing.getStepX() * index;
        double y = startY + facing.getStepY() * index;
        double z = startZ + facing.getStepZ() * index;

        /*
         * Simple version:
         * - 0.0 -> 0.5 : render only the back half
         * - 0.5 -> 1.0 : render back half + front half
         */
        renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_BACK_HALF,
                facing, x, y, z, ms, vb, light);

        if (partial > 0.5f) {
            renderRodHalf(state, PartialModelRegistriesCLM.PNEUMATIC_CYLINDER_ROD_FRONT_HALF,
                    facing, x, y, z, ms, vb, light);
        }
    }

    private void renderRodHalf(BlockState state,
                               dev.engine_room.flywheel.lib.model.baked.PartialModel model,
                               Direction facing,
                               double x,
                               double y,
                               double z,
                               PoseStack ms,
                               VertexConsumer vb,
                               int light) {

        CachedBuffers.partial(model, state)
                .rotateToFace(facing)
                .translate(x, y, z)
                .light(light)
                .renderInto(ms, vb);
    }

    @Override
    public boolean shouldRenderOffScreen(PneumaticCylinderBlockEntity be) {
        PneumaticCylinderBlockEntity controller = be.getControllerBE();
        return controller != null && controller.isController();
    }
}