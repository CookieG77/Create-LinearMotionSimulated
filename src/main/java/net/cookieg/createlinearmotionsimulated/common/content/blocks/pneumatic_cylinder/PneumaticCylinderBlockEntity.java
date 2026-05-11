package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PneumaticCylinderBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer {

    /** V1: 1x1xMAX_LENGTH. Later this can become a server config. */
    private static final int MAX_WIDTH = 1;
    private static final int MAX_LENGTH = 16;

    protected BlockPos controller;
    protected BlockPos lastKnownPos;

    /** Create connectivity vocabulary: width = tube diameter, height = length on main axis. */
    protected int width = 1;
    protected int height = 1;

    protected boolean updateConnectivity;

    /** SwivelBearing/Sable-like state. */
    protected boolean assembled;
    protected BlockPos pistonHeadPos;
    protected UUID pistonHeadSubLevelId;

    /** Future 2x2/3x3 support: only one shaft on the back face of the whole multiblock. */
    protected BlockPos shaftPos;

    /** Linear motion scaffold. */
    protected float extension;
    protected float prevExtension;
    protected float targetExtension;

    public PneumaticCylinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        lastKnownPos = pos;
        width = 1;
        height = 1;
    }

    public PneumaticCylinderBlockEntity(BlockPos pos, BlockState blockState) {
        this(BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // Add Create behaviours here later: display link, value box, filtering, etc.
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null)
            return;

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        prevExtension = extension;

        if (!level.isClientSide) {
            if (updateConnectivity)
                updateConnectivity();

            if (isController())
                tickControllerServer();
        }
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    public void queueConnectivityUpdate() {
        updateConnectivity = true;
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide)
            return;
        if (!isController())
            return;

        ConnectivityHandler.formMulti(this);
    }

    private void tickControllerServer() {
        // Extension rules will become:
        // - redstone powered => target = height
        // - unpowered => target = 0
        // - only move if valid Create rotation is received on shaftPos/back face.
        boolean powered = level.hasNeighborSignal(worldPosition);
        targetExtension = powered && assembled ? getMaxExtension() : 0;

        float speed = 0.08f; // blocks/tick, temporary until tied to Create speed
        if (extension < targetExtension)
            extension = Math.min(targetExtension, extension + speed);
        else if (extension > targetExtension)
            extension = Math.max(targetExtension, extension - speed);
        else
            return;

        setChanged();
        sendData();
    }

    public float getMaxExtension() {
        return Math.max(0, height - 1);
    }

    public float getRenderedExtension(float partialTicks) {
        return prevExtension + (extension - prevExtension) * partialTicks;
    }

    public boolean isAssembled() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        return controllerBE != null && controllerBE.assembled;
    }

    /** Dev helper until HoneyGlue/Sable assembly is implemented. */
    public void toggleAssembledDebug() {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.toggleAssembledDebug();
            return;
        }
        setAssembled(!assembled);
    }

    public void setAssembled(boolean assembled) {
        if (level == null || level.isClientSide)
            return;
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.setAssembled(assembled);
            return;
        }

        this.assembled = assembled;
        if (!assembled) {
            pistonHeadPos = null;
            pistonHeadSubLevelId = null;
            extension = 0;
            targetExtension = 0;
        }

        updateAllPartStates();
        setChanged();
        sendData();
    }

    /**
     * TODO: call this from your Sable/HoneyGlue assembly code once the mobile head actor exists.
     */
    public void setPistonHeadActor(@Nullable BlockPos headPos, @Nullable UUID subLevelId) {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.setPistonHeadActor(headPos, subLevelId);
            return;
        }
        this.pistonHeadPos = headPos;
        this.pistonHeadSubLevelId = subLevelId;
        this.assembled = headPos != null;
        updateAllPartStates();
        setChanged();
        sendData();
    }

    public Direction getFacing() {
        return getBlockState().getValue(PneumaticCylinderBlock.FACING);
    }

    public Direction getBackDirection() {
        return getFacing().getOpposite();
    }

    /**
     * Assumes ConnectivityHandler chooses the minimum coordinate as the controller origin,
     * like Create tanks/vaults do. This is correct for V1 1x1 lines.
     */
    public BlockPos getBackOrigin() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return worldPosition;

        Direction facing = controllerBE.getFacing();
        BlockPos origin = controllerBE.getBlockPos();

        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            origin = origin.relative(facing.getOpposite(), controllerBE.height - 1);

        return origin;
    }

    public int getIndexFromBack(BlockPos pos) {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return 0;

        Direction facing = controllerBE.getFacing();
        BlockPos back = controllerBE.getBackOrigin();

        return switch (facing.getAxis()) {
            case X -> facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? pos.getX() - back.getX()
                    : back.getX() - pos.getX();
            case Y -> facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? pos.getY() - back.getY()
                    : back.getY() - pos.getY();
            case Z -> facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? pos.getZ() - back.getZ()
                    : back.getZ() - pos.getZ();
        };
    }

    private CylinderPart computePart() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null || controllerBE.height <= 1)
            return CylinderPart.SINGLE;

        int index = controllerBE.getIndexFromBack(worldPosition);
        if (index <= 0)
            return CylinderPart.BACK;
        if (index >= controllerBE.height - 1)
            return CylinderPart.FRONT;
        return CylinderPart.MIDDLE;
    }

    private boolean shouldHaveShaft() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;

        if (controllerBE.shaftPos == null)
            controllerBE.shaftPos = controllerBE.getBackOrigin(); // V1 automatic input on 1x1 back block

        return worldPosition.equals(controllerBE.shaftPos);
    }

    public boolean isOnBackFace(BlockPos pos) {
        return getIndexFromBack(pos) == 0;
    }

    public void setShaftPos(@Nullable BlockPos pos) {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.setShaftPos(pos);
            return;
        }

        if (pos != null && !isOnBackFace(pos))
            return;

        shaftPos = pos;
        updateAllPartStates();
        setChanged();
        sendData();
    }

    private void updateAllPartStates() {
        if (level == null || level.isClientSide)
            return;

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return;

        Direction facing = controllerBE.getFacing();
        BlockPos origin = controllerBE.getBlockPos();

        // V1 width = 1. For future width > 1, iterate the width x width slice perpendicular to facing.
        for (int i = 0; i < controllerBE.height; i++) {
            BlockPos pos = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? origin.relative(facing, i)
                    : origin.relative(facing.getOpposite(), controllerBE.height - 1 - i);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PneumaticCylinderBlockEntity cylinderPart)
                cylinderPart.notifyMultiUpdated();
        }
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE() {
        if (level == null)
            return (T) this;
        if (isController())
            return (T) this;

        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof PneumaticCylinderBlockEntity cylinder)
            return (T) cylinder;
        return null;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.equals(controller);
    }

    @Override
    public void setController(BlockPos controller) {
        if (level != null && level.isClientSide && !isVirtual())
            return;
        if (controller != null && controller.equals(this.controller))
            return;

        this.controller = controller;
        setChanged();
        sendData();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level != null && level.isClientSide)
            return;

        updateConnectivity = true;
        controller = null;
        width = 1;
        height = 1;
        shaftPos = null;
        assembled = false;
        pistonHeadPos = null;
        pistonHeadSubLevelId = null;
        extension = 0;
        targetExtension = 0;

        BlockState state = getBlockState();
        if (level != null && PneumaticCylinderBlock.isPneumaticCylinder(state)) {
            level.setBlock(worldPosition, state
                    .setValue(PneumaticCylinderBlock.PART, CylinderPart.SINGLE)
                    .setValue(PneumaticCylinderBlock.HAS_SHAFT, false)
                    .setValue(PneumaticCylinderBlock.ASSEMBLED, false),
                    Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }

        setChanged();
        sendData();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = getBlockState();
        if (level == null || level.isClientSide || !PneumaticCylinderBlock.isPneumaticCylinder(state))
            return;

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        boolean controllerAssembled = controllerBE != null && controllerBE.assembled;

        BlockState newState = state
                .setValue(PneumaticCylinderBlock.PART, computePart())
                .setValue(PneumaticCylinderBlock.HAS_SHAFT, shouldHaveShaft())
                .setValue(PneumaticCylinderBlock.ASSEMBLED, controllerAssembled);

        if (newState != state)
            level.setBlock(worldPosition, newState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);

        setChanged();
        sendData();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return getBlockState().getValue(PneumaticCylinderBlock.FACING).getAxis();
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return MAX_LENGTH;
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = Math.max(1, height);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = Math.max(1, width);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!isController())
            return super.createRenderBoundingBox();

        Direction facing = getFacing();
        double dx = facing.getStepX() * Math.max(1, height + extension + 1);
        double dy = facing.getStepY() * Math.max(1, height + extension + 1);
        double dz = facing.getStepZ() * Math.max(1, height + extension + 1);
        return super.createRenderBoundingBox().expandTowards(dx, dy, dz);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        updateConnectivity = compound.contains("Uninitialized");
        lastKnownPos = compound.contains("LastKnownPos") ? NBTHelper.readBlockPos(compound, "LastKnownPos") : null;
        controller = compound.contains("Controller") ? NBTHelper.readBlockPos(compound, "Controller") : null;

        if (compound.contains("Size"))
            width = compound.getInt("Size");
        if (compound.contains("Height"))
            height = compound.getInt("Height");

        assembled = compound.getBoolean("Assembled");
        if (compound.contains("ShaftPos"))
            shaftPos = NbtUtils.readBlockPos(compound, "ShaftPos").orElse(null);
        else
            shaftPos = null;

        if (compound.contains("PistonHeadPos"))
            pistonHeadPos = NbtUtils.readBlockPos(compound, "PistonHeadPos").orElse(null);
        else
            pistonHeadPos = null;

        pistonHeadSubLevelId = compound.hasUUID("PistonHeadSubLevelId")
                ? compound.getUUID("PistonHeadSubLevelId")
                : null;

        extension = compound.getFloat("Extension");
        prevExtension = extension;
        targetExtension = compound.getFloat("TargetExtension");

        if (clientPacket && hasLevel())
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        if (!isController() && controller != null)
            compound.put("Controller", NbtUtils.writeBlockPos(controller));

        if (isController()) {
            compound.putInt("Size", width);
            compound.putInt("Height", height);
            compound.putBoolean("Assembled", assembled);
            if (shaftPos != null)
                compound.put("ShaftPos", NbtUtils.writeBlockPos(shaftPos));
            if (pistonHeadPos != null)
                compound.put("PistonHeadPos", NbtUtils.writeBlockPos(pistonHeadPos));
            if (pistonHeadSubLevelId != null)
                compound.putUUID("PistonHeadSubLevelId", pistonHeadSubLevelId);
            compound.putFloat("Extension", extension);
            compound.putFloat("TargetExtension", targetExtension);
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        if (isController()) {
            compound.putInt("Size", width);
            compound.putInt("Height", height);
            compound.putBoolean("Assembled", assembled);
        }
        super.writeSafe(compound, registries);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;

        tooltip.add(Component.literal("Pneumatic Cylinder"));
        tooltip.add(Component.literal("Length: " + controllerBE.height));
        tooltip.add(Component.literal("Assembled: " + controllerBE.assembled));
        tooltip.add(Component.literal("Extension: " + String.format(java.util.Locale.ROOT, "%.2f", controllerBE.extension)));
        return true;
    }
}
