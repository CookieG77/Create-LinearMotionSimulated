package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class PneumaticCylinderRodSegmentBlock extends DirectionalBlock implements IBE<PneumaticCylinderRodSegmentBlockEntity> {

    public static final MapCodec<PneumaticCylinderRodSegmentBlock> CODEC =
            simpleCodec(PneumaticCylinderRodSegmentBlock::new);

    public PneumaticCylinderRodSegmentBlock(Properties properties) {
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
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean canBeReplaced(@NotNull BlockState state, @NotNull BlockPlaceContext useContext) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticCylinderRodSegmentBlockEntity be))
            return Shapes.empty();

        float amount = be.getLocalExtensionAmount(1.0f);
        if (amount <= 0.001f)
            return Shapes.empty();

        return rodShape(state.getValue(FACING), Math.max(0.25f, Math.min(1.0f, amount)));
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof PneumaticCylinderRodSegmentBlockEntity be))
            return Shapes.empty();

        float amount = be.getLocalExtensionAmount(1.0f);
        if (amount <= 0.001f)
            return Shapes.empty();

        return rodShape(state.getValue(FACING), Math.min(1.0f, amount));
    }

    private static VoxelShape rodShape(Direction facing, float amount) {
        double length = 16.0 * amount;

        double minX = 6;
        double minY = 6;
        double minZ = 6;
        double maxX = 10;
        double maxY = 10;
        double maxZ = 10;

        /*
         * Le segment est derrière la tête.
         * Il pousse depuis le côté vers la tête, donc côté FACING.
         */
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
    public Class<PneumaticCylinderRodSegmentBlockEntity> getBlockEntityClass() {
        return PneumaticCylinderRodSegmentBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticCylinderRodSegmentBlockEntity> getBlockEntityType() {
        return BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_ROD_SEGMENT.get();
    }
}