package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlock;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod.PneumaticCylinderRodSegmentBlock;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod.PneumaticCylinderRodSegmentBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadRenderer.BASE_VISIBLE_ROD;

public class PneumaticCylinderBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer, BlockEntitySubLevelActor {

    private static final int MAX_WIDTH = 1;
    private static final int MAX_LENGTH = 16;

    private static final Set<String> RECENT_SUBLEVEL_MOVES = ConcurrentHashMap.newKeySet();

    protected BlockPos controller;
    protected BlockPos lastKnownPos;

    protected int width = 1;
    protected int height = 1;

    protected boolean updateConnectivity;
    protected boolean blockBeingRemoved;

    protected boolean assembled;
    protected BlockPos pistonHeadPos;
    protected UUID pistonHeadSubLevelId;

    @Nullable
    protected AssemblyException lastAssemblyException;
    /**
     * True while Sable is moving this block into/out of a sublevel.
     * Same purpose as SwivelBearingBlockEntity#assembling.
     */
    protected boolean assembling;
    /**
     * Delayed rebuild after Sable moved this cylinder block.
     * We wait a few ticks because all blocks / sublevels may not be fully relinked
     * in the same tick as afterMove(...).
     */
    protected int subLevelMoveRebuildDelay;

    /**
     * Saved mobile head data while Create connectivity is being rebuilt.
     * Only useful for the block that still knows about the attached head.
     */
    @Nullable
    protected BlockPos pendingMovedHeadPos;

    @Nullable
    protected UUID pendingMovedHeadSubLevelId;

    protected boolean pendingMovedAssembled;
    protected float pendingMovedExtension;

    @Nullable
    protected GenericConstraintHandle pistonConstraint;
    protected boolean rebuildConstraintOnLoad;

    protected BlockPos shaftPos;
    // Flag to change the multiblock behavior between free and constrained movement
    protected boolean shaftInstalled = true;

    protected float extension;
    protected float prevExtension;
    protected float targetExtension;

    protected int redstonePower;
    protected int previousRedstonePower;
    protected int stableRedstonePower;
    protected boolean redstoneChangePending;
    /**
     * Set after a manual disassembly while redstone is still high.
     * Prevents instant re-assembly until the signal goes back to 0 once.
     */
    protected boolean redstoneResetRequired;

    protected boolean assembleRequested;
    protected boolean disassembleRequested;



    public PneumaticCylinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        lastKnownPos = pos;
        width = 1;
        height = 1;
        shaftInstalled = true;
        shaftPos = pos;
    }

    public PneumaticCylinderBlockEntity(BlockPos pos, BlockState blockState) {
        this(BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER.get(), pos, blockState);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null)
            return;

        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        } else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        prevExtension = extension;

        if (!level.isClientSide) {
            if (updateConnectivity)
                updateConnectivity();

            if (subLevelMoveRebuildDelay > 0) {
                subLevelMoveRebuildDelay--;

                if (subLevelMoveRebuildDelay == 0)
                    rebuildAfterSubLevelMove();
            }

            if (isController())
                tickControllerServer();
        }
    }

    private void onPositionChanged() {
        /*
         * Do not reset Create connectivity while Sable is moving the block.
         * Do not translate controller/head positions here either: during Sable plot moves,
         * old/new positions may not be a simple world-space translation.
         *
         * Swivel Bearing does not repair links from onPositionChanged; it uses
         * beforeMove/afterMove instead.
         */
        if (assembling || assembled || Sable.HELPER.getContaining(this) != null) {
            lastKnownPos = worldPosition;
            setChanged();
            sendData();
            return;
        }

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

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return;

        ConnectivityHandler.formMulti(controllerBE);
        controllerBE.updateAllPartStates();
    }

    private void tickControllerServer() {
        if (assembled)
            ensureAttachedHeadConstraint();

        if (assembleRequested) {
            assembleRequested = false;
            redstoneResetRequired = false;
            assembleHead();
        }

        int oldRedstonePower = redstonePower;
        int sampledRedstonePower = computeRedstonePower();

        if (sampledRedstonePower <= 0)
            redstoneResetRequired = false;

        redstonePower = sampledRedstonePower;

        if ((oldRedstonePower > 0) != (redstonePower > 0))
            updateAllPartStates();

        boolean hasRotation = hasKineticInput();
        boolean powered = redstonePower > 0 && !redstoneResetRequired;

        if (!assembled && powered) {
            assembleHead();

            if (!assembled) {
                setChanged();
                sendData();
                return;
            }
        }

        if (disassembleRequested)
            targetExtension = 0;
        else
            targetExtension = powered && hasRotation && assembled ? getMaxExtension() : 0;

        float speed = disassembleRequested
                ? getForcedReturnSpeedBlocksPerTick()
                : getLinearSpeedBlocksPerTick();

        if (speed <= 0.0001f) {
            if (assembled) {
                syncHeadActorData();
                syncRodSegments();
                updateAttachedHeadConstraint(extension);
            }

            setChanged();
            sendData();
            return;
        }

        float previous = extension;

        if (extension < targetExtension)
            extension = Math.min(targetExtension, extension + speed);
        else if (extension > targetExtension)
            extension = Math.max(targetExtension, extension - speed);

        if (assembled) {
            syncHeadActorData();
            syncRodSegments();
            updateAttachedHeadConstraint(extension);
        }

        if (disassembleRequested && extension <= 0.001f) {
            extension = 0;
            disassembleRequested = false;
            disassembleHead();
            return;
        }

        if (extension != previous)
            onExtensionChanged(previous, extension);

        setChanged();
        sendData();
    }

    public float getMaxExtension() {
        return Math.max(0, height);
    }

    public void requestToggleAssembly() {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.requestToggleAssembly();
            return;
        }

        if (assembled) {
            disassembleRequested = true;
            assembleRequested = false;
            targetExtension = 0;

            redstoneResetRequired = computeRedstonePower() > 0;

            rebuildAttachedHeadConstraint();
            updateAttachedHeadConstraint(extension);
        } else {
            redstoneResetRequired = false;
            assembleRequested = true;
        }

        setChanged();
        sendData();
    }

    private void assembleHead() {
        if (level == null || level.isClientSide || assembled)
            return;

        if (!(level instanceof ServerLevel serverLevel))
            return;

        Direction facing = getFacing();

        BlockPos front = getFrontBlockPos();
        BlockPos temporaryHeadWorldPos = front.relative(facing);

        BlockState headState = BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()
                .defaultBlockState()
                .setValue(PneumaticCylinderPistonHeadBlock.FACING, facing);

        BlockState existingState = level.getBlockState(temporaryHeadWorldPos);

        final SimAssemblyHelper.AssemblyResult result;
        final BlockPos assembledHeadPos;

        try {
            if (existingState.canBeReplaced()
                    || existingState.is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get())) {

                if (!existingState.is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                    level.setBlockAndUpdate(temporaryHeadWorldPos, headState);

                BlockEntity createdBE = level.getBlockEntity(temporaryHeadWorldPos);
                if (createdBE instanceof PneumaticCylinderPistonHeadBlockEntity headBE) {
                    headBE.setAssembling(true);
                    headBE.setParent(this);
                }

                result = SimAssemblyHelper.assembleFromSingleBlock(serverLevel, front, temporaryHeadWorldPos, false, false);
                lastAssemblyException = null;

                if (result == null || !(result.subLevel() instanceof ServerSubLevel)) {
                    if (level.getBlockState(temporaryHeadWorldPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                        level.setBlockAndUpdate(temporaryHeadWorldPos, Blocks.AIR.defaultBlockState());

                    setChanged();
                    sendData();
                    return;
                }

                assembledHeadPos = temporaryHeadWorldPos.offset(result.offset());

            } else {
                result = SimAssemblyHelper.assembleFromSingleBlock(serverLevel, front, temporaryHeadWorldPos, false, false);
                lastAssemblyException = null;

                if (result == null || !(result.subLevel() instanceof ServerSubLevel)) {
                    setChanged();
                    sendData();
                    return;
                }

                assembledHeadPos = front.offset(result.offset());

                level.setBlock(assembledHeadPos, headState, Block.UPDATE_ALL);
            }
        } catch (AssemblyException e) {
            lastAssemblyException = e;

            if (level.getBlockState(temporaryHeadWorldPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                level.setBlockAndUpdate(temporaryHeadWorldPos, Blocks.AIR.defaultBlockState());

            setChanged();
            sendData();
            return;
        }

        if (!(result.subLevel() instanceof ServerSubLevel assembledSubLevel)) {
            setChanged();
            sendData();
            return;
        }

        BlockEntity assembledBE = level.getBlockEntity(assembledHeadPos);
        if (assembledBE instanceof PneumaticCylinderPistonHeadBlockEntity headBE) {
            headBE.setParent(this);
            headBE.setAssembling(false);
        }

        pistonHeadPos = assembledHeadPos;
        pistonHeadSubLevelId = assembledSubLevel.getUniqueId();

        assembled = true;
        extension = 0;
        prevExtension = 0;
        targetExtension = 0;

        createAttachedHeadConstraint(assembledSubLevel, assembledHeadPos);

        updateAllPartStates();
        syncHeadActorData();
        syncRodSegments();
        updateAttachedHeadConstraint(0);

        setChanged();
        sendData();
    }

    private void disassembleHead() {
        if (level == null || level.isClientSide || !assembled)
            return;

        SubLevel subLevel = getAttachedHeadSubLevel();
        BlockPos headPos = pistonHeadPos;

        if (subLevel != null && headPos != null) {
            destroyAttachedHeadConstraint();
            destroyRodSegments();
            destroyHeadActor();

            if (!subLevel.isRemoved())
                SimAssemblyHelper.disassembleSubLevel(level, subLevel, headPos, getFrontBlockPos(), Rotation.NONE, true);
        }

        pistonHeadPos = null;
        pistonHeadSubLevelId = null;

        assembled = false;
        extension = 0;
        prevExtension = 0;
        targetExtension = 0;
        disassembleRequested = false;

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
        return true;
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

        /*
         * Temporary shaft-only mode: the cylinder always has a shaft.
         * Keep this method for future mode switching, but do not allow clearing
         * the shaft while the free-sliding mode is disabled.
         */
        shaftInstalled = true;
        shaftPos = pos != null && isOnBackFace(pos) ? pos : getBackOrigin();

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
        BlockPos back = controllerBE.getBackOrigin();

        for (int i = 0; i < controllerBE.height; i++) {
            BlockPos pos = back.relative(facing, i);

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

        boolean wasController = isController();

        if (wasController) {
            destroyAttachedHeadConstraint();
            destroyRodSegments();
        }

        updateConnectivity = false;
        controller = null;
        width = 1;
        height = 1;
        shaftPos = getBackOrigin();
        shaftInstalled = true;
        assembled = false;

        pistonHeadPos = null;
        pistonHeadSubLevelId = null;
        extension = 0;
        prevExtension = 0;
        targetExtension = 0;

        redstonePower = 0;
        previousRedstonePower = 0;
        stableRedstonePower = 0;
        redstoneChangePending = false;
        redstoneResetRequired = false;

        assembleRequested = false;
        disassembleRequested = false;
        lastAssemblyException = null;

        if (blockBeingRemoved) {
            setChanged();
            sendData();
            return;
        }

        BlockState state = getBlockState();
        if (level != null && PneumaticCylinderBlock.isPneumaticCylinder(state)) {
            level.setBlock(worldPosition, state
                            .setValue(PneumaticCylinderBlock.PART, CylinderPart.SINGLE)
                            .setValue(PneumaticCylinderBlock.HAS_SHAFT, true)
                            .setValue(PneumaticCylinderBlock.POWERED, false)
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

        boolean oldHasShaft = state.getValue(PneumaticCylinderBlock.HAS_SHAFT);

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        boolean controllerAssembled = controllerBE != null && controllerBE.assembled;
        boolean controllerHasShaft = controllerBE != null && controllerBE.shaftInstalled;
        boolean controllerPowered = controllerHasShaft && controllerBE.redstonePower > 0;

        BlockState newState = state
                .setValue(PneumaticCylinderBlock.PART, computePart())
                .setValue(PneumaticCylinderBlock.HAS_SHAFT, controllerHasShaft)
                .setValue(PneumaticCylinderBlock.POWERED, controllerPowered)
                .setValue(PneumaticCylinderBlock.ASSEMBLED, controllerAssembled);

        if (newState != state) {
            level.setBlock(worldPosition, newState, Block.UPDATE_ALL);

            boolean newHasShaft = newState.getValue(PneumaticCylinderBlock.HAS_SHAFT);
            if (oldHasShaft != newHasShaft) {
                Direction back = newState.getValue(PneumaticCylinderBlock.FACING).getOpposite();
                BlockPos neighbour = worldPosition.relative(back);

                level.neighborChanged(neighbour, newState.getBlock(), worldPosition);
                level.neighborChanged(worldPosition, newState.getBlock(), neighbour);
            }
        }

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

        shaftPos = compound.contains("ShaftPos")
                ? NbtUtils.readBlockPos(compound, "ShaftPos").orElse(null)
                : null;

        shaftInstalled = true;
        if (shaftPos == null)
            shaftPos = getBackOrigin();

        pistonHeadPos = compound.contains("PistonHeadPos")
                ? NbtUtils.readBlockPos(compound, "PistonHeadPos").orElse(null)
                : null;

        pistonHeadSubLevelId = compound.hasUUID("PistonHeadSubLevelId")
                ? compound.getUUID("PistonHeadSubLevelId")
                : null;

        rebuildConstraintOnLoad = assembled && pistonHeadPos != null && pistonHeadSubLevelId != null;

        lastAssemblyException = AssemblyException.read(compound, registries);

        extension = compound.getFloat("Extension");
        prevExtension = extension;
        targetExtension = compound.getFloat("TargetExtension");
        redstonePower = compound.getInt("RedstonePower");
        previousRedstonePower = compound.getInt("PreviousRedstonePower");
        stableRedstonePower = compound.contains("StableRedstonePower")
                ? compound.getInt("StableRedstonePower")
                : redstonePower;
        redstoneChangePending = compound.getBoolean("RedstoneChangePending");
        redstoneResetRequired = compound.getBoolean("RedstoneResetRequired");
        assembleRequested = compound.getBoolean("AssembleRequested");
        disassembleRequested = compound.getBoolean("DisassembleRequested");

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
            compound.putBoolean("ShaftInstalled", true);

            if (shaftPos != null)
                compound.put("ShaftPos", NbtUtils.writeBlockPos(shaftPos));
            if (pistonHeadPos != null)
                compound.put("PistonHeadPos", NbtUtils.writeBlockPos(pistonHeadPos));
            if (pistonHeadSubLevelId != null)
                compound.putUUID("PistonHeadSubLevelId", pistonHeadSubLevelId);

            AssemblyException.write(compound, registries, lastAssemblyException);

            compound.putFloat("Extension", extension);
            compound.putFloat("TargetExtension", targetExtension);
            compound.putInt("RedstonePower", redstonePower);
            compound.putInt("PreviousRedstonePower", previousRedstonePower);
            compound.putInt("StableRedstonePower", stableRedstonePower);
            compound.putBoolean("RedstoneChangePending", redstoneChangePending);
            compound.putBoolean("RedstoneResetRequired", redstoneResetRequired);
            compound.putBoolean("AssembleRequested", assembleRequested);
            compound.putBoolean("DisassembleRequested", disassembleRequested);
        }

        super.write(compound, registries, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        if (isController()) {
            compound.putInt("Size", width);
            compound.putInt("Height", height);
            compound.putBoolean("Assembled", assembled);
            compound.putBoolean("ShaftInstalled", shaftInstalled);
            compound.putInt("RedstonePower", redstonePower);
        }

        super.writeSafe(compound, registries);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;

        tooltip.add(Component.literal("    ")
                .append(Component.translatable("block.create_linear_motion_simulated.google_tooltip.title")));

        tooltip.add(Component.translatable(
                "block.create_linear_motion_simulated.google_tooltip.max_extension",
                Component.literal(String.format("%.2f", controllerBE.getMaxExtension())).withStyle(ChatFormatting.GOLD)
        ));

        tooltip.add(Component.translatable(
                "block.create_linear_motion_simulated.google_tooltip.mode",
                Component.translatable("block.create_linear_motion_simulated.google_tooltip.mode.locked").withStyle(ChatFormatting.GOLD)
        ));

        tooltip.add(Component.translatable(
                "block.create_linear_motion_simulated.google_tooltip.extension",
                Component.literal(String.format("%.2f", controllerBE.extension)).withStyle(ChatFormatting.GOLD)
        ));

        tooltip.add(Component.translatable(
                "block.create_linear_motion_simulated.google_tooltip.powered",
                controllerBE.redstonePower > 0
                        ? Component.translatable("block.create_linear_motion_simulated.google_tooltip.powered.yes").withStyle(ChatFormatting.GOLD)
                        : Component.translatable("block.create_linear_motion_simulated.google_tooltip.powered.no").withStyle(ChatFormatting.GOLD)
        ));

        return true;
    }

    public void markBlockBeingRemoved() {
        blockBeingRemoved = true;
    }

    public BlockPos getShaftPosForRendering() {
        return getShaftInputPos();
    }

    public float getInputSpeed() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return 0;

        if (level == null)
            return 0;

        BlockPos inputPos = controllerBE.getShaftInputPos();
        if (inputPos == null)
            return 0;

        BlockEntity be = level.getBlockEntity(inputPos);
        if (be instanceof PneumaticCylinderBlockEntity cylinder) {
            float speed = cylinder.getSpeed();
            if (Math.abs(speed) > 0.001f)
                return speed;
        }

        return controllerBE.getSpeed();
    }

    public float getRenderedShaftAngle(float partialTicks) {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return 0;

        if (level == null)
            return 0;

        BlockPos inputPos = controllerBE.getShaftInputPos();
        if (inputPos == null)
            return 0;

        BlockEntity be = level.getBlockEntity(inputPos);
        PneumaticCylinderBlockEntity input = be instanceof PneumaticCylinderBlockEntity cylinder
                ? cylinder
                : controllerBE;

        float radians = KineticBlockEntityRenderer.getAngleForBe(input, inputPos, getFacing().getAxis());
        return (float) Math.toDegrees(radians);
    }

    @Nullable
    public BlockPos getShaftInputPos() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return null;

        if (controllerBE.getWidth() <= 1)
            return controllerBE.getBackOrigin();

        return controllerBE.shaftPos;
    }

    public boolean hasKineticInput() {
        return Math.abs(getInputSpeed()) > 0.001f;
    }

    private float getLinearSpeedBlocksPerTick() {
        float rpm = Math.abs(getInputSpeed());
        return (rpm / 256f) * (2f / 20f);
    }

    private float getForcedReturnSpeedBlocksPerTick() {
        return 2f / 20f;
    }

    private void onExtensionChanged(float oldExtension, float newExtension) {
        if (!assembled)
            return;

        syncHeadActorData();
        syncRodSegments();
        updateAttachedHeadConstraint(newExtension);
    }

    private int computeRedstonePower() {
        if (level == null)
            return 0;

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return 0;

        Direction facing = controllerBE.getFacing();
        BlockPos back = controllerBE.getBackOrigin();

        int max = 0;

        for (int i = 0; i < controllerBE.height; i++) {
            BlockPos pos = back.relative(facing, i);
            max = Math.max(max, level.getBestNeighborSignal(pos));

            if (max >= 15)
                return 15;
        }

        return max;
    }

    @Nullable
    private SubLevel getAttachedHeadSubLevel() {
        if (level == null || pistonHeadSubLevelId == null)
            return null;

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null)
            return null;

        return container.getSubLevel(pistonHeadSubLevelId);
    }

    private void destroyHeadActor() {
        if (level == null || pistonHeadPos == null)
            return;

        if (!level.getBlockState(pistonHeadPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
            return;

        BlockEntity be = level.getBlockEntity(pistonHeadPos);
        if (be instanceof PneumaticCylinderPistonHeadBlockEntity headBE)
            headBE.setAssembling(true);

        level.setBlock(pistonHeadPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        /*
         * Important:
         * The cylinder body/controller must NOT declare the piston head sublevel as
         * a connection dependency.
         *
         * Swivel Bearing does it the other way around:
         * the moving link/plate depends on its parent, not the parent on the moving
         * link. If the controller depends on the head, Sable can keep the head
         * sublevel anchored when the body is assembled into another sublevel.
         */
        return null;
    }

    public BlockPos getFrontBlockPos() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return worldPosition;

        BlockPos back = controllerBE.getBackOrigin();
        return back.relative(controllerBE.getFacing(), controllerBE.height - 1);
    }

    public BlockPos getHeadZeroWorldPos() {
        return getFrontBlockPos();
    }

    private void syncHeadActorData() {
        if (level == null || pistonHeadPos == null)
            return;

        BlockEntity be = level.getBlockEntity(pistonHeadPos);
        if (be instanceof PneumaticCylinderPistonHeadBlockEntity headBE) {
            headBE.setExtensionData(getVisualExtension(), getMaxExtension());
        }
    }

    private void syncRodSegments() {
        if (level == null || level.isClientSide || pistonHeadPos == null)
            return;

        Direction facing = getFacing();
        Direction back = facing.getOpposite();

        int maxSegments = (int) Math.ceil(getMaxExtension());
        float visualExtension = getVisualExtension();
        int wantedSegments = Math.max(0, (int) Math.ceil(visualExtension + BASE_VISIBLE_ROD - 0.001f) - 1);

        for (int i = 1; i <= maxSegments; i++) {
            BlockPos segmentPos = pistonHeadPos.relative(back, i);
            boolean shouldExist = i <= wantedSegments;

            if (shouldExist) {
                if (!level.getBlockState(segmentPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get())) {
                    BlockState segmentState = BlockRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get()
                            .defaultBlockState()
                            .setValue(PneumaticCylinderRodSegmentBlock.FACING, facing);

                    level.setBlock(segmentPos, segmentState, Block.UPDATE_ALL);
                }

                BlockEntity be = level.getBlockEntity(segmentPos);
                if (be instanceof PneumaticCylinderRodSegmentBlockEntity segmentBE) {
                    segmentBE.setHead(pistonHeadPos);
                    segmentBE.setIndexBehindHead(i);
                    segmentBE.setAssembling(false);
                    segmentBE.setForceFullRender(false);
                    segmentBE.setExtensionData(visualExtension, prevExtension, getMaxExtension());
                }

                BlockState state = level.getBlockState(segmentPos);
                level.sendBlockUpdated(segmentPos, state, state, Block.UPDATE_CLIENTS);
            } else {
                BlockEntity be = level.getBlockEntity(segmentPos);
                if (be instanceof PneumaticCylinderRodSegmentBlockEntity segmentBE) {
                    segmentBE.setAssembling(true);
                    segmentBE.setForceFullRender(false);
                }

                if (level.getBlockState(segmentPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get()))
                    level.setBlock(segmentPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private float getVisualExtension() {
        return clampExtension(extension);
    }

    private float clampExtension(float value) {
        return Math.max(0, Math.min(getMaxExtension(), value));
    }

    private void destroyRodSegments() {
        if (level == null || pistonHeadPos == null)
            return;

        Direction back = getFacing().getOpposite();
        int maxSegments = (int) Math.ceil(getMaxExtension());

        for (int i = 1; i <= maxSegments; i++) {
            BlockPos segmentPos = pistonHeadPos.relative(back, i);

            BlockEntity be = level.getBlockEntity(segmentPos);
            if (be instanceof PneumaticCylinderRodSegmentBlockEntity segmentBE)
                segmentBE.setAssembling(true);

            if (level.getBlockState(segmentPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get()))
                level.setBlock(segmentPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void createAttachedHeadConstraint(ServerSubLevel headSubLevel, BlockPos assembledHeadPos) {
        if (level == null || level.isClientSide)
            return;

        if (!(level instanceof ServerLevel serverLevel))
            return;

        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);

        if (container == null)
            return;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();

        SubLevel containing = Sable.HELPER.getContaining(this);
        ServerSubLevel baseSubLevel = containing instanceof ServerSubLevel serverSubLevel
                ? serverSubLevel
                : null;

        Direction facing = getFacing();

        Vector3d frame1Pos = getConstraintBaseFramePosition(0);

        Vector3d frame2Pos = new Vector3d(
                assembledHeadPos.getX() + 0.5,
                assembledHeadPos.getY() + 0.5,
                assembledHeadPos.getZ() + 0.5
        );

        Quaterniond frameOrientation = orientationForFacing(facing);

        GenericConstraintConfiguration config = new GenericConstraintConfiguration(
                frame1Pos,
                frame2Pos,
                frameOrientation,
                frameOrientation,
                getLockedConstraintAxes()
        );

        pistonConstraint = pipeline.addConstraint(baseSubLevel, headSubLevel, config);
        rebuildConstraintOnLoad = false;
        pistonConstraint.setContactsEnabled(false);

        if (baseSubLevel != null)
            pipeline.wakeUp(baseSubLevel);

        pipeline.wakeUp(headSubLevel);
    }

    private Vector3d getConstraintBaseFramePosition(float extension) {
        Direction facing = getFacing();
        BlockPos front = getFrontBlockPos();

        return new Vector3d(
                front.getX() + 0.5 + facing.getStepX() * extension,
                front.getY() + 0.5 + facing.getStepY() * extension,
                front.getZ() + 0.5 + facing.getStepZ() * extension
        );
    }

    private Quaterniond orientationForFacing(Direction facing) {
        return new Quaterniond();
    }

    private Set<ConstraintJointAxis> getLockedConstraintAxes() {
        return EnumSet.allOf(ConstraintJointAxis.class);
    }

    private void updateAttachedHeadConstraint(float requestedExtension) {
        if (pistonConstraint == null || !pistonConstraint.isValid())
            return;

        if (level == null || !(level instanceof ServerLevel serverLevel))
            return;

        float clampedExtension = clampExtension(requestedExtension);
        Direction facing = getFacing();

        pistonConstraint.setFrame1(
                getConstraintBaseFramePosition(clampedExtension),
                orientationForFacing(facing)
        );

        ServerSubLevelContainer container =
                (ServerSubLevelContainer) SubLevelContainer.getContainer(serverLevel);

        if (container == null)
            return;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();

        SubLevel containing = Sable.HELPER.getContaining(this);
        if (containing instanceof ServerSubLevel baseSubLevel)
            pipeline.wakeUp(baseSubLevel);

        SubLevel attached = getAttachedHeadSubLevel();
        if (attached instanceof ServerSubLevel headSubLevel)
            pipeline.wakeUp(headSubLevel);
    }

    private void rebuildAttachedHeadConstraint() {
        if (level == null || level.isClientSide || !assembled || pistonHeadSubLevelId == null)
            return;

        SubLevel attached = getAttachedHeadSubLevel();
        if (!(attached instanceof ServerSubLevel headSubLevel))
            return;

        destroyAttachedHeadConstraint();
        createAttachedHeadConstraint(headSubLevel, pistonHeadPos != null ? pistonHeadPos : getFrontBlockPos());
        updateAttachedHeadConstraint(extension);

        rebuildConstraintOnLoad = false;

        setChanged();
        sendData();
    }

    private void destroyAttachedHeadConstraint() {
        if (pistonConstraint != null) {
            if (pistonConstraint.isValid())
                pistonConstraint.remove();

            pistonConstraint = null;
        }

        rebuildConstraintOnLoad = false;
    }

    public void onPistonHeadBroken() {
        if (level == null || level.isClientSide)
            return;

        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.onPistonHeadBroken();
            return;
        }

        destroyAttachedHeadConstraint();
        destroyRodSegments();

        pistonHeadPos = null;
        pistonHeadSubLevelId = null;
        assembled = false;
        extension = 0;
        prevExtension = 0;
        targetExtension = 0;
        assembleRequested = false;
        disassembleRequested = false;

        redstoneResetRequired = computeRedstonePower() > 0;

        updateAllPartStates();

        setChanged();
        sendData();
    }

    public void destroyMovingPartFromCylinderBreak() {
        if (level == null || level.isClientSide)
            return;

        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.destroyMovingPartFromCylinderBreak();
            return;
        }

        destroyAttachedHeadConstraint();
        destroyRodSegments();
        destroyHeadActor();

        pistonHeadPos = null;
        pistonHeadSubLevelId = null;
        assembled = false;
        extension = 0;
        prevExtension = 0;
        targetExtension = 0;
        redstoneResetRequired = false;

        setChanged();
        sendData();
    }

    private void ensureAttachedHeadConstraint() {
        if (level == null || level.isClientSide)
            return;

        if (!assembled || pistonHeadPos == null || pistonHeadSubLevelId == null)
            return;

        if (pistonConstraint != null && pistonConstraint.isValid())
            return;

        SubLevel attached = getAttachedHeadSubLevel();

        if (!(attached instanceof ServerSubLevel headSubLevel))
            return;

        createAttachedHeadConstraint(headSubLevel, pistonHeadPos);
        updateAttachedHeadConstraint(extension);
        syncHeadActorData();
        syncRodSegments();

        rebuildConstraintOnLoad = false;

        setChanged();
        sendData();
    }

    public boolean isAssembled() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        return controllerBE != null && controllerBE.assembled;
    }

    public boolean hasShaftInstalled() {
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        return controllerBE == null || controllerBE.shaftInstalled;
    }

    public void installShaft() {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.installShaft();
            return;
        }

        shaftInstalled = true;

        if (width <= 1 || shaftPos == null)
            shaftPos = getBackOrigin();

        updateAllPartStates();
        setChanged();
        sendData();
    }

    public void removeShaft() {
        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.removeShaft();
            return;
        }

        /*
         * Temporarily disabled. The cylinder must remain shaft-driven until Sable
         * exposes a stable limited prismatic constraint API for the free mode.
         *
         * Future restore point:
         *   shaftInstalled = false;
         *   shaftPos = null;
         *   redstonePower = 0;
         *   previousRedstonePower = 0;
         *   stableRedstonePower = 0;
         *   redstoneChangePending = false;
         *   updateAllPartStates();
         *   setChanged();
         *   sendData();
         */
    }

    public void beforeAssembly() {
        this.assembling = true;

        /*
         * Critical:
         * If this block is not the controller, forward the move notification to the
         * controller while the old multiblock links still exist.
         *
         * Sable may call beforeMove(...) on any cylinder part. If the old controller
         * keeps its constraint alive while the body is being assembled into another
         * sublevel, the piston head sublevel can remain anchored at the old world
         * position.
         */
        PneumaticCylinderBlockEntity controllerBE = getControllerBE();

        if (controllerBE != null && controllerBE != this) {
            controllerBE.beforeControllerSubLevelMove();
        } else {
            beforeControllerSubLevelMove();
        }

        setChanged();
    }

    public boolean isAssemblingForSubLevelMove() {
        return assembling;
    }

    public void afterMovedBySubLevel() {
        /*
         * Sable finished moving this block. Do not immediately reattach the
         * constraint here: the other blocks and the head sublevel may still be
         * finishing their move during this same tick.
         */
        this.assembling = false;
        this.lastKnownPos = worldPosition;

        /*
         * Do not overwrite pendingMovedHeadPos here.
         * It was captured in beforeControllerSubLevelMove(), while the old
         * controller/head links were still reliable.
         */

        /*
         * After Sable moves blocks, absolute controller references can be stale.
         * Do NOT call removeController(), because that wipes the piston state.
         */
        this.controller = null;
        this.width = 1;
        this.height = 1;

        this.updateConnectivity = true;
        this.subLevelMoveRebuildDelay = 3;

        setChanged();
        sendData();
    }

    public void associateHeadWithParent() {
        if (level == null || level.isClientSide)
            return;

        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.associateHeadWithParent();
            return;
        }

        if (pistonHeadPos == null)
            return;

        BlockEntity be = level.getBlockEntity(pistonHeadPos);
        if (be instanceof PneumaticCylinderPistonHeadBlockEntity headBE) {
            /*
             * Rewrites parent + parentSubLevelId from the current controller.
             * After the body is assembled into a sublevel, Sable.HELPER.getContaining(this)
             * should now point to the new body sublevel.
             */
            headBE.setParent(this);
        }
    }

    public void setPistonHeadActor(@Nullable BlockPos headPos, @Nullable UUID headSubLevelId) {
        if (level != null && level.isClientSide)
            return;

        if (!isController()) {
            PneumaticCylinderBlockEntity controllerBE = getControllerBE();
            if (controllerBE != null)
                controllerBE.setPistonHeadActor(headPos, headSubLevelId);
            return;
        }

        this.pistonHeadPos = headPos;
        this.pistonHeadSubLevelId = headSubLevelId;
        this.assembled = headPos != null && headSubLevelId != null;

        /*
         * The head was moved/relinked, so the old constraint must not be trusted.
         */
        destroyAttachedHeadConstraint();

        if (!this.assembled) {
            destroyRodSegments();

            extension = 0;
            prevExtension = 0;
            targetExtension = 0;
            assembleRequested = false;
            disassembleRequested = false;
        } else {
            rebuildConstraintOnLoad = true;
            rebuildAttachedHeadConstraint();
            updateAttachedHeadConstraint(extension);
            syncHeadActorData();
            syncRodSegments();
        }

        updateAllPartStates();

        setChanged();
        sendData();
    }

    private void rebuildAfterSubLevelMove() {
        if (level == null || level.isClientSide)
            return;

        /*
         * Rebuild the static Create multiblock.
         */
        ConnectivityHandler.formMulti(this);

        PneumaticCylinderBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            controllerBE = this;

        /*
         * If any moved part still has the saved head state, transfer it to the
         * rebuilt controller. This is needed because ConnectivityHandler may choose
         * or reconfirm a controller after the move.
         */
        if (pendingMovedAssembled && pendingMovedHeadPos != null && pendingMovedHeadSubLevelId != null) {
            controllerBE.pistonHeadPos = pendingMovedHeadPos;
            controllerBE.pistonHeadSubLevelId = pendingMovedHeadSubLevelId;
            controllerBE.assembled = true;
            controllerBE.extension = pendingMovedExtension;
            controllerBE.prevExtension = pendingMovedExtension;
            controllerBE.targetExtension = pendingMovedExtension;
            controllerBE.rebuildConstraintOnLoad = true;
        }

        pendingMovedHeadPos = null;
        pendingMovedHeadSubLevelId = null;
        pendingMovedAssembled = false;
        pendingMovedExtension = 0;

        if (controllerBE.isController()) {
            controllerBE.updateAllPartStates();

            /*
             * Refresh the head's parentSubLevelId now that the body is inside its
             * new containing sublevel.
             */
            controllerBE.associateHeadWithParent();

            /*
             * Force a new constraint. The old one was intentionally removed before
             * the move, so this one should bind:
             *   new body sublevel -> existing piston head sublevel
             */
            controllerBE.destroyAttachedHeadConstraint();
            controllerBE.rebuildAttachedHeadConstraint();
            controllerBE.updateAttachedHeadConstraint(controllerBE.extension);

            controllerBE.syncHeadActorData();
            controllerBE.syncRodSegments();

            controllerBE.setChanged();
            controllerBE.sendData();
        }

        setChanged();
        sendData();
    }

    private void beforeControllerSubLevelMove() {
        this.assembling = true;

        /*
         * Save the mobile head state before Sable starts moving the body.
         * afterMovedBySubLevel()/rebuildAfterSubLevelMove() will restore this on the
         * rebuilt controller.
         */
        this.pendingMovedHeadPos = this.pistonHeadPos;
        this.pendingMovedHeadSubLevelId = this.pistonHeadSubLevelId;
        this.pendingMovedAssembled = this.assembled;
        this.pendingMovedExtension = this.extension;

        /*
         * This is the important part.
         *
         * The old constraint may still be valid but bound to the old containing
         * sublevel, or to the world/null body if the cylinder body was not already
         * in a sublevel. Keeping it alive during Sable's move can pin the piston
         * head sublevel in place.
         */
        destroyAttachedHeadConstraint();

        rebuildConstraintOnLoad = true;

        setChanged();
        sendData();
    }
}