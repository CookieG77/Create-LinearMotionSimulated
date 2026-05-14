package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlockEntity.BASE_VISIBLE_ROD;

public class PneumaticCylinderRodSegmentBlockEntity extends SmartBlockEntity {

    private BlockPos headPos;
    private int indexBehindHead;
    private boolean assembling;
    private boolean forceFullRender;

    private float extension;
    private float prevExtension;
    private float maxExtension;

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
        /*
         * If Sable is moving this technical block, ignore the transient removal.
         * If it is a real break, keep the old behavior: destroy the linked head.
         */
        if (level != null && !level.isClientSide && !assembling && headPos != null) {
            if (level.getBlockState(headPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                level.destroyBlock(headPos, false);
        }

        super.remove();
    }

    public void onMovedBySubLevel(BlockPos oldPos, BlockPos newPos) {
        BlockPos delta = newPos.subtract(oldPos);

        if (headPos != null)
            headPos = headPos.offset(delta);

        setChanged();
        sendData();
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

    public void setForceFullRender(boolean forceFullRender) {
        this.forceFullRender = forceFullRender;
        setChanged();
        sendData();
    }

    public boolean isForceFullRender() {
        return forceFullRender;
    }

    public void setExtensionData(float extension, float prevExtension, float maxExtension) {
        this.extension = Math.max(0, Math.min(maxExtension, extension));
        this.prevExtension = Math.max(0, Math.min(maxExtension, prevExtension));
        this.maxExtension = Math.max(0, maxExtension);

        setChanged();
        sendData();
    }

    public PneumaticCylinderPistonHeadBlockEntity getHeadBE() {
        if (level == null || headPos == null)
            return null;

        BlockEntity be = level.getBlockEntity(headPos);
        return be instanceof PneumaticCylinderPistonHeadBlockEntity headBE ? headBE : null;
    }

    public float getRenderedExtension(float partialTicks) {
        return prevExtension + (extension - prevExtension) * partialTicks;
    }

    public float getLocalExtensionAmount(float partialTicks) {
        if (forceFullRender)
            return 1.0f;

        /*
         * The rod block model is no longer smoothly interpolated.
         * It is quantized by the controller:
         *   0.5 -> rod_half
         *   1.0 -> rod_full
         */
        return Math.max(0, Math.min(1, getRenderedExtension(partialTicks)));
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        if (headPos != null)
            compound.put("HeadPos", NbtUtils.writeBlockPos(headPos));

        compound.putInt("IndexBehindHead", indexBehindHead);
        compound.putBoolean("Assembling", assembling);
        compound.putBoolean("ForceFullRender", forceFullRender);

        compound.putFloat("Extension", extension);
        compound.putFloat("PrevExtension", prevExtension);
        compound.putFloat("MaxExtension", maxExtension);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        headPos = compound.contains("HeadPos")
                ? NbtUtils.readBlockPos(compound, "HeadPos").orElse(null)
                : null;

        indexBehindHead = compound.getInt("IndexBehindHead");
        assembling = compound.getBoolean("Assembling");
        forceFullRender = compound.getBoolean("ForceFullRender");

        extension = compound.getFloat("Extension");
        prevExtension = compound.getFloat("PrevExtension");
        maxExtension = compound.getFloat("MaxExtension");
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
}