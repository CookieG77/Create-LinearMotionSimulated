package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

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
        if (level != null && !level.isClientSide && !assembling)
            destroyPneumaticParent();
        super.remove();
    }

    private void destroyPneumaticParent() {
        if (parent == null || level == null)
            return;

        if (level.getBlockState(parent).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER.get()))
            level.destroyBlock(parent, false);
    }

    public void setParent(final PneumaticCylinderBlockEntity be) {
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        this.parent = be.getBlockPos();
        this.parentSubLevelId = subLevel != null ? subLevel.getUniqueId() : null;
        setChanged();
        sendData();
    }

    public BlockPos getParent() {
        return parent;
    }

    public UUID getParentSubLevelId() {
        return parentSubLevelId;
    }

    public void setAssembling(boolean assembling) {
        this.assembling = assembling;
        setChanged();
    }

    public boolean isAssembling() {
        return assembling;
    }

    /**
     * Optional helper: resolves the parent if it exists in the same level.
     * If the parent lives outside this SubLevel, use Sable's APIs to resolve parentSubLevelId.
     */
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

        // Backward compatibility with your older lowercase key.
        if (compound.contains("parent"))
            parent = NbtUtils.readBlockPos(compound, "parent").orElse(null);
        if (compound.contains("ParentPos"))
            parent = NbtUtils.readBlockPos(compound, "ParentPos").orElse(null);

        parentSubLevelId = compound.hasUUID("ParentSubLevelId")
                ? compound.getUUID("ParentSubLevelId")
                : null;
        assembling = compound.getBoolean("Assembling");
    }
}
