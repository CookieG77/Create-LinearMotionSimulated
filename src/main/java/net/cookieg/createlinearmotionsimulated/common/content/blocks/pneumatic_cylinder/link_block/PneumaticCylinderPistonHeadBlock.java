package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

/**
 * Mobile head actor used after the cylinder assembles HoneyGlue/Sable content.
 * It is intentionally not a Create kinetic block: the cylinder body receives kinetic power,
 * the head is locked to the cylinder axis and moved linearly by the parent.
 */
public class PneumaticCylinderPistonHeadBlock extends DirectionalBlock implements IBE<PneumaticCylinderPistonHeadBlockEntity>, BlockSubLevelAssemblyListener {

    public static final MapCodec<PneumaticCylinderPistonHeadBlock> CODEC = simpleCodec(PneumaticCylinderPistonHeadBlock::new);

    public PneumaticCylinderPistonHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
        // Hook point for Sable: keep parent data valid after sublevel/world movement.
        // The BE keeps its parent position in NBT, so no work is required for now.
    }

    @Override
    public Class<PneumaticCylinderPistonHeadBlockEntity> getBlockEntityClass() {
        return PneumaticCylinderPistonHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticCylinderPistonHeadBlockEntity> getBlockEntityType() {
        return BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get();
    }
}
