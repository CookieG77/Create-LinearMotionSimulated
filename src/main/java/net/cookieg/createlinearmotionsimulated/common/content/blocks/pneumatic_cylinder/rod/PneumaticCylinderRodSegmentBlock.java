package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PneumaticCylinderRodSegmentBlock extends DirectionalBlock implements IBE<PneumaticCylinderRodSegmentBlockEntity>, BlockSubLevelAssemblyListener {

    public static final MapCodec<PneumaticCylinderRodSegmentBlock> CODEC =
            simpleCodec(PneumaticCylinderRodSegmentBlock::new);

    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public PneumaticCylinderRodSegmentBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FULL, false));
    }

    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FULL);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        float amount = state.getValue(FULL) ? 1.0f : 0.5f;
        return rodShape(state.getValue(FACING), amount);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        return Shapes.empty(); // Required to have no block collision otherwise the piston get stuck with itself
    }

    @Override
    protected @NotNull VoxelShape getBlockSupportShape(@NotNull BlockState state,
                                                       @NotNull BlockGetter level,
                                                       @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected @NotNull VoxelShape getVisualShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(@NotNull BlockState state, @NotNull BlockPlaceContext useContext) {
        return false;
    }

    private static VoxelShape rodShape(Direction facing, float amount) {
        double length = 16.0 * amount;

        double minX = 6;
        double minY = 6;
        double minZ = 6;
        double maxX = 10;
        double maxY = 10;
        double maxZ = 10;

        switch (facing) {
            case EAST -> {
                minX = 16 - length;
                maxX = 16;
            }
            case WEST -> {
                minX = 0;
                maxX = length;
            }
            case UP -> {
                minY = 16 - length;
                maxY = 16;
            }
            case DOWN -> {
                minY = 0;
                maxY = length;
            }
            case SOUTH -> {
                minZ = 16 - length;
                maxZ = 16;
            }
            case NORTH -> {
                minZ = 0;
                maxZ = length;
            }
        }

        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void beforeMove(ServerLevel originLevel,
                           ServerLevel resultingLevel,
                           BlockState newState,
                           BlockPos oldPos,
                           BlockPos newPos) {
        withBlockEntityDo(originLevel, oldPos, PneumaticCylinderRodSegmentBlockEntity::beforeAssembly);
    }

    @Override
    public void afterMove(ServerLevel originLevel,
                          ServerLevel resultingLevel,
                          BlockState newState,
                          BlockPos oldPos,
                          BlockPos newPos) {
        withBlockEntityDo(resultingLevel, newPos, PneumaticCylinderRodSegmentBlockEntity::afterMovedBySubLevel);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PneumaticCylinderRodSegmentBlockEntity segmentBE) {
            BlockPos linkedHeadPos = segmentBE.getHeadPos();
            if (linkedHeadPos != null && level.getBlockState(linkedHeadPos).is(BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()))
                level.destroyBlock(linkedHeadPos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public Class<PneumaticCylinderRodSegmentBlockEntity> getBlockEntityClass() {
        return PneumaticCylinderRodSegmentBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticCylinderRodSegmentBlockEntity> getBlockEntityType() {
        return BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get();
    }
}