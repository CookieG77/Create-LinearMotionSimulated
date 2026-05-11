package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class PneumaticCylinderBlock extends DirectionalBlock implements IWrenchable, IBE<PneumaticCylinderBlockEntity> {

    public static final MapCodec<PneumaticCylinderBlock> CODEC = simpleCodec(PneumaticCylinderBlock::new);

    /**
     * True when the mobile head/attached glued blocks have been assembled into a Sable SubLevel.
     * This is NOT the same thing as the static Create multiblock connectivity.
     */
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    /** Position of this block in the static tube body. */
    public static final EnumProperty<CylinderPart> PART = EnumProperty.create("part", CylinderPart.class);

    /**
     * Visual/mechanical marker for the single Create shaft input.
     * V1: automatically true on the back controller block for 1x1 cylinders.
     * Future: set manually on one block of the back face for 2x2/3x3 cylinders.
     */
    public static final BooleanProperty HAS_SHAFT = BooleanProperty.create("has_shaft");

    public PneumaticCylinderBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(ASSEMBLED, false)
                .setValue(PART, CylinderPart.SINGLE)
                .setValue(HAS_SHAFT, false));
    }



    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ASSEMBLED, PART, HAS_SHAFT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (context.getPlayer() != null && context.getPlayer().isCrouching())
            facing = facing.getOpposite();

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ASSEMBLED, false)
                .setValue(PART, CylinderPart.SINGLE)
                .setValue(HAS_SHAFT, false);
    }

    @Override
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide || oldState.getBlock() == state.getBlock())
            return;

        withBlockEntityDo(level, pos, PneumaticCylinderBlockEntity::queueConnectivityUpdate);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() == newState.getBlock())
            return;

        if (!level.isClientSide)
            withBlockEntityDo(level, pos, ConnectivityHandler::splitMulti);

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide)
            withBlockEntityDo(level, pos, PneumaticCylinderBlockEntity::queueConnectivityUpdate);
    }

    /**
     * Temporary dev interaction:
     * - normal right click on controller toggles assembled state scaffold.
     * You can remove this once real HoneyGlue/Sable assembly is wired.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        withBlockEntityDo(level, pos, be -> {
            PneumaticCylinderBlockEntity controller = be.getControllerBE();
            if (controller != null)
                controller.toggleAssembledDebug();
        });
        return InteractionResult.CONSUME;
    }

    @Override
    public Class<PneumaticCylinderBlockEntity> getBlockEntityClass() {
        return PneumaticCylinderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticCylinderBlockEntity> getBlockEntityType() {
        return BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER.get();
    }

    public static boolean isPneumaticCylinder(BlockState state) {
        return state.getBlock() instanceof PneumaticCylinderBlock;
    }
}
