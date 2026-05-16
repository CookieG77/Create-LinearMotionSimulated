package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

import static net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlock.HEAD_PIXELS;

public class PneumaticCylinderPistonHeadBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {

    private BlockPos parent;
    private UUID parentSubLevelId;
    private boolean assembling;

    public PneumaticCylinderPistonHeadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PneumaticCylinderPistonHeadBlockEntity(BlockPos pos, BlockState blockState) {
        this(BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get(), pos, blockState);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void remove() {
        /*
         * Same behavior as SwivelBearingPlateBlockEntity:
         * if Sable is moving this BE, do not treat removal as a real break.
         */
        if (level != null && !level.isClientSide && !assembling)
            destroyPneumaticParent();

        super.remove();
    }

    public void notifyPneumaticParentHeadBroken(Level currentLevel) {
        if (parent == null || currentLevel == null)
            return;

        BlockEntity be = currentLevel.getBlockEntity(parent);
        if (be instanceof PneumaticCylinderBlockEntity cylinder) {
            PneumaticCylinderBlockEntity controller = cylinder.getControllerBE();
            if (controller != null)
                controller.onPistonHeadBroken();
        }
    }

    public void beforeAssembly() {
        this.assembling = true;
        setChanged();
    }

    public void setParent(final PneumaticCylinderBlockEntity be) {
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        this.parent = be.getBlockPos();
        this.parentSubLevelId = subLevel != null ? subLevel.getUniqueId() : null;
        setChanged();
        sendData();
    }

    public void setAssembling(boolean assembling) {
        this.assembling = assembling;
        setChanged();
    }

    public PneumaticCylinderBlockEntity getParentBEInCurrentLevel() {
        if (level == null || parent == null)
            return null;
        BlockEntity be = level.getBlockEntity(parent);
        return be instanceof PneumaticCylinderBlockEntity cylinder ? cylinder : null;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        if (parent != null)
            compound.put("ParentPos", NbtUtils.writeBlockPos(parent));
        if (parentSubLevelId != null)
            compound.putUUID("ParentSubLevelId", parentSubLevelId);
        compound.putBoolean("Assembling", assembling);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        if (compound.contains("Parent"))
            parent = NbtUtils.readBlockPos(compound, "Parent").orElse(null);
        if (compound.contains("ParentPos"))
            parent = NbtUtils.readBlockPos(compound, "ParentPos").orElse(null);

        parentSubLevelId = compound.hasUUID("ParentSubLevelId")
                ? compound.getUUID("ParentSubLevelId")
                : null;
        assembling = compound.getBoolean("Assembling");
    }

    public void updateRodDisplay(float adjustedDistance) {
        boolean full = getLocalRodAmount(adjustedDistance) >= 1.0f - 0.001f;
        updateFullState(full);

        setChanged();
        sendData();
    }

    private float getLocalRodAmount(float adjustedDistance) {
        float amount = 0.5f + Math.max(0, adjustedDistance) + (1f / 16f * PneumaticCylinderPistonHeadBlock.HEAD_PIXELS);
        return amount >= 1.0f - 0.001f ? 1.0f : 0.5f;
    }

    private void updateFullState(boolean full) {
        if (level == null || level.isClientSide)
            return;

        BlockState oldState = level.getBlockState(worldPosition);
        if (!(oldState.getBlock() instanceof PneumaticCylinderPistonHeadBlock))
            return;

        if (oldState.getValue(PneumaticCylinderPistonHeadBlock.FULL) == full)
            return;

        BlockState newState = oldState.setValue(PneumaticCylinderPistonHeadBlock.FULL, full);
        level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        level.sendBlockUpdated(worldPosition, oldState, newState, Block.UPDATE_ALL);
    }

    public void fixParentLinkingWhenMoved() {
        if (level == null || level.isClientSide || parent == null)
            return;

        BlockEntity be = level.getBlockEntity(parent);
        if (be instanceof PneumaticCylinderBlockEntity cylinder) {
            PneumaticCylinderBlockEntity controller = cylinder.getControllerBE();
            if (controller != null) {
                setParent(controller);

                SubLevel containing = Sable.HELPER.getContaining(this);
                controller.setPistonHeadActor(
                        getBlockPos(),
                        containing != null ? containing.getUniqueId() : null
                );
                controller.associateHeadWithParent();
            }
        }

        this.assembling = false;
        setChanged();
        sendData();
    }

    private void destroyPneumaticParent() {
        if (level == null || level.isClientSide || parent == null)
            return;

        BlockEntity be = level.getBlockEntity(parent);
        if (!(be instanceof PneumaticCylinderBlockEntity cylinder))
            return;

        PneumaticCylinderBlockEntity controller = cylinder.getControllerBE();
        if (controller == null)
            return;

        controller.onPistonHeadBroken();
    }

    @Override
    public @org.jetbrains.annotations.Nullable Iterable<@org.jetbrains.annotations.NotNull SubLevel> sable$getConnectionDependencies() {
        if (level == null || parentSubLevelId == null)
            return null;

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null)
            return null;

        SubLevel parentSubLevel = container.getSubLevel(parentSubLevelId);
        if (parentSubLevel == null)
            return null;

        return List.of(parentSubLevel);
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel,
                                  final RigidBodyHandle handle,
                                  final double timeStep) {
        if (parent == null || level == null || level.isClientSide)
            return;

        BlockEntity be = level.getBlockEntity(parent);
        if (be instanceof PneumaticCylinderBlockEntity cylinder) {
            PneumaticCylinderBlockEntity controller = cylinder.getControllerBE();
            if (controller != null)
                controller.updatePistonMotorCoefficients();
        }
    }
}
