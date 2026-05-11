package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class PneumaticCylinderPistonHeadBlock extends DirectionalBlock implements IBE<PneumaticCylinderPistonHeadBlockEntity>, BlockSubLevelAssemblyListener {

    public static final MapCodec<PneumaticCylinderPistonHeadBlock> CODEC =
            simpleCodec(PneumaticCylinderPistonHeadBlock::new);

    private static final int HEAD_PIXELS = 3;

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
    }

    @Override
    public Class<PneumaticCylinderPistonHeadBlockEntity> getBlockEntityClass() {
        return PneumaticCylinderPistonHeadBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PneumaticCylinderPistonHeadBlockEntity> getBlockEntityType() {
        return BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get();
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = headShape(facing);

        if (level.getBlockEntity(pos) instanceof PneumaticCylinderPistonHeadBlockEntity headBE) {
            float localAmount = Math.min(1.0f, 0.5f + headBE.getExtension());
            shape = Shapes.or(shape, headRodShape(facing, localAmount));
        }

        return shape;
    }

    /*
     * IMPORTANT:
     * La tête est à extension 0 dans le même espace que le block FRONT du cylindre.
     * Elle complète donc la partie manquante côté FRONT du cylindre.
     *
     * Si FACING = EAST, le front du block est côté +X, donc x=13..16.
     */
    private static VoxelShape headShape(Direction facing) {
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 16;
        double maxY = 16;
        double maxZ = 16;

        switch (facing) {
            case EAST -> minX = 16 - HEAD_PIXELS;
            case WEST -> maxX = HEAD_PIXELS;
            case UP -> minY = 16 - HEAD_PIXELS;
            case DOWN -> maxY = HEAD_PIXELS;
            case SOUTH -> minZ = 16 - HEAD_PIXELS;
            case NORTH -> maxZ = HEAD_PIXELS;
        }

        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /*
     * Premier morceau de tige rendu/collisionné par la tête elle-même.
     * Il pousse depuis le côté back de la tête vers son centre.
     */
    private static VoxelShape headRodShape(Direction facing, float amount) {
        if (amount <= 0.001f)
            return Shapes.empty();

        double length = 16.0 * Math.min(1.0f, amount);

        double minX = 6;
        double minY = 6;
        double minZ = 6;
        double maxX = 10;
        double maxY = 10;
        double maxZ = 10;

        switch (facing) {
            case EAST -> {
                minX = 0;
                maxX = length;
            }
            case WEST -> {
                minX = 16 - length;
                maxX = 16;
            }
            case UP -> {
                minY = 0;
                maxY = length;
            }
            case DOWN -> {
                minY = 16 - length;
                maxY = 16;
            }
            case SOUTH -> {
                minZ = 0;
                maxZ = length;
            }
            case NORTH -> {
                minZ = 16 - length;
                maxZ = 16;
            }
        }

        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state,
                                                        Level level,
                                                        @NotNull BlockPos pos,
                                                        @NotNull Player player,
                                                        @NotNull BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof PneumaticCylinderPistonHeadBlockEntity headBE))
            return InteractionResult.PASS;

        PneumaticCylinderBlockEntity parent = headBE.getParentBEInCurrentLevel();
        if (parent == null)
            return InteractionResult.PASS;

        PneumaticCylinderBlockEntity controller = parent.getControllerBE();
        if (controller == null)
            return InteractionResult.PASS;

        controller.requestToggleAssembly();
        return InteractionResult.CONSUME;
    }
}