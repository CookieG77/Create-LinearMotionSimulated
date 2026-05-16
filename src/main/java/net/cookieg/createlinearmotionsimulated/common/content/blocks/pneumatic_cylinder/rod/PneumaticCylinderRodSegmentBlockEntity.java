package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlock;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PneumaticCylinderRodSegmentBlockEntity extends SmartBlockEntity {

    private BlockPos headPos;
    private int indexBehindHead;
    private boolean assembling;

    public PneumaticCylinderRodSegmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PneumaticCylinderRodSegmentBlockEntity(BlockPos pos, BlockState blockState) {
        this(BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide && !assembling && headPos != null) {
            if (level.getBlockState(headPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                level.destroyBlock(headPos, false);
        }

        super.remove();
    }

    public void setHead(BlockPos headPos) {
        this.headPos = headPos;
        setChanged();
        sendData();
    }

    public BlockPos getHeadPos() {
        return headPos;
    }

    public void setIndexBehindHead(int indexBehindHead) {
        this.indexBehindHead = Math.max(1, indexBehindHead);
        setChanged();
        sendData();
    }

    public int getIndexBehindHead() {
        return indexBehindHead;
    }

    public void setAssembling(boolean assembling) {
        this.assembling = assembling;
        setChanged();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        if (headPos != null)
            compound.put("HeadPos", NbtUtils.writeBlockPos(headPos));

        compound.putInt("IndexBehindHead", indexBehindHead);
        compound.putBoolean("Assembling", assembling);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        headPos = compound.contains("HeadPos")
                ? NbtUtils.readBlockPos(compound, "HeadPos").orElse(null)
                : null;

        indexBehindHead = compound.getInt("IndexBehindHead");
        assembling = compound.getBoolean("Assembling");
    }

    public void beforeAssembly() {
        this.assembling = true;
        setChanged();
    }

    public void afterMovedBySubLevel() {
        this.assembling = false;
        setChanged();
        sendData();
    }

    public static boolean shouldRender(float adjustedDistance, int indexBehindHead) {
        return getLocalRodAmount(adjustedDistance, indexBehindHead) >= 0.5f - 0.001f;
    }

    public static boolean shouldRenderFull(float adjustedDistance, int indexBehindHead) {
        return getLocalRodAmount(adjustedDistance, indexBehindHead) >= 1.0f - 0.001f;
    }

    public static float getLocalRodAmount(float adjustedDistance, int indexBehindHead) {
        float holdMargin = 1f / 16f * PneumaticCylinderPistonHeadBlock.HEAD_PIXELS;
        float visibleLength = 0.5f + Math.max(0, adjustedDistance);
        float segmentStart = Math.max(1, indexBehindHead);
        float local = visibleLength - segmentStart + holdMargin;

        if (local >= 1.0f - 0.001f)
            return 1.0f;

        if (local >= 0.5f - 0.001f)
            return 0.5f;

        return 0.0f;
    }

    public void updateRodDisplay(float adjustedDistance) {
        boolean full = shouldRenderFull(adjustedDistance, indexBehindHead);
        updateFullState(full);

        setChanged();
        sendData();
    }

    private void updateFullState(boolean full) {
        if (level == null || level.isClientSide)
            return;

        BlockState oldState = level.getBlockState(worldPosition);
        if (!(oldState.getBlock() instanceof PneumaticCylinderRodSegmentBlock))
            return;

        if (oldState.getValue(PneumaticCylinderRodSegmentBlock.FULL) == full)
            return;

        BlockState newState = oldState.setValue(PneumaticCylinderRodSegmentBlock.FULL, full);
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, oldState, newState, Block.UPDATE_ALL);
    }


}