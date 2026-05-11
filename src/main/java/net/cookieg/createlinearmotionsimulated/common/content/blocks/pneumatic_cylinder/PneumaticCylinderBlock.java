package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PneumaticCylinderBlock extends DirectionalKineticBlock implements IWrenchable, IBE<PneumaticCylinderBlockEntity> {

    public static final MapCodec<PneumaticCylinderBlock> CODEC = simpleCodec(PneumaticCylinderBlock::new);

    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    public static final EnumProperty<CylinderPart> PART = EnumProperty.create("part", CylinderPart.class);
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
    protected @NotNull MapCodec<? extends DirectionalKineticBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ASSEMBLED, PART, HAS_SHAFT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = getPlacementFacing(context);

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ASSEMBLED, false)
                .setValue(PART, CylinderPart.SINGLE)
                .setValue(HAS_SHAFT, false);
    }

    private Direction getPlacementFacing(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        BlockPos attachedPos = placePos.relative(clickedFace.getOpposite());

        Direction inherited = getInheritedFacingFrom(level, attachedPos, clickedFace);
        if (inherited != null)
            return applySneakInvert(context, inherited);

        inherited = getInheritedFacingFrom(level, placePos, clickedFace);
        if (inherited != null)
            return applySneakInvert(context, inherited);

        Direction facing = context.getNearestLookingDirection().getOpposite();
        return applySneakInvert(context, facing);
    }

    @Nullable
    private Direction getInheritedFacingFrom(Level level, BlockPos pos, Direction clickedFace) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PneumaticCylinderBlock))
            return null;

        Direction existingFacing = state.getValue(FACING);
        boolean clickedFrontOrBack = clickedFace == existingFacing || clickedFace == existingFacing.getOpposite();

        if (!clickedFrontOrBack)
            return null;

        return existingFacing;
    }

    private Direction applySneakInvert(BlockPlaceContext context, Direction facing) {
        if (context.getPlayer() != null && context.getPlayer().isCrouching())
            return facing.getOpposite();

        return facing;
    }

    @Override
    public void onPlace(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide || oldState.getBlock() == state.getBlock())
            return;

        withBlockEntityDo(level, pos, PneumaticCylinderBlockEntity::queueConnectivityUpdate);

        Direction facing = state.getValue(FACING);
        queueCylinderUpdateAt(level, pos.relative(facing));
        queueCylinderUpdateAt(level, pos.relative(facing.getOpposite()));

        level.scheduleTick(pos, this, 1);
    }

    private void queueCylinderUpdateAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PneumaticCylinderBlockEntity cylinder)
            cylinder.queueConnectivityUpdate();
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() == newState.getBlock())
            return;

        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, be -> {
                PneumaticCylinderBlockEntity controller = be.getControllerBE();

                if (controller != null)
                    controller.destroyMovingPartFromCylinderBreak();

                be.markBlockBeingRemoved();
                ConnectivityHandler.splitMulti(be);
            });
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state,
                                Level level,
                                @NotNull BlockPos pos,
                                @NotNull Block block,
                                @NotNull BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide)
            return;

        /*
         * Quand le vérin est assemblé, le controller relit déjà la redstone chaque tick.
         * Ne pas reformer la connectivité ici, sinon un signal redstone pendant un split
         * Sable peut fragmenter le multiblock visuel en plusieurs SINGLE.
         */
        if (state.getValue(ASSEMBLED))
            return;

        withBlockEntityDo(level, pos, PneumaticCylinderBlockEntity::queueConnectivityUpdate);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        withBlockEntityDo(level, pos, be -> {
            PneumaticCylinderBlockEntity controller = be.getControllerBE();
            if (controller != null)
                controller.requestToggleAssembly();
        });
        return InteractionResult.CONSUME;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack,
                                                       @NotNull BlockState state,
                                                       Level level,
                                                       @NotNull BlockPos pos,
                                                       @NotNull Player player,
                                                       @NotNull InteractionHand hand,
                                                       @NotNull BlockHitResult hitResult) {

        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!AllBlocks.SHAFT.isIn(stack))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        PneumaticCylinderBlockEntity be = getBlockEntity(level, pos);
        if (be == null)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        PneumaticCylinderBlockEntity controller = be.getControllerBE();
        if (controller == null)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        // V1: automatic shaft on 1x1 cylinders. Manual shaft placement is reserved for 2x2/3x3.
        if (controller.getWidth() <= 1)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        Direction facing = controller.getFacing();
        Direction back = facing.getOpposite();

        if (hitResult.getDirection() != back)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        if (!controller.isOnBackFace(pos))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        controller.setShaftPos(pos);

        if (!player.isCreative())
            stack.shrink(1);

        return ItemInteractionResult.CONSUME;
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

    private static final int HEAD_PIXELS = 3;

    private static VoxelShape shortenedFromFront(Direction facing) {
        double cut = HEAD_PIXELS;

        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 16;
        double maxY = 16;
        double maxZ = 16;

        switch (facing) {
            case EAST -> maxX = 16 - cut;
            case WEST -> minX = cut;
            case UP -> maxY = 16 - cut;
            case DOWN -> minY = cut;
            case SOUTH -> maxZ = 16 - cut;
            case NORTH -> minZ = cut;
        }

        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return getCylinderShape(state);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return getCylinderShape(state);
    }

    private VoxelShape getCylinderShape(BlockState state) {
        if (!state.getValue(ASSEMBLED))
            return Shapes.block();

        CylinderPart part = state.getValue(PART);
        if (part == CylinderPart.FRONT || part == CylinderPart.SINGLE)
            return shortenedFromFront(state.getValue(FACING));

        return Shapes.block();
    }

    @Nullable
    private PneumaticCylinderBlockEntity getBlockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PneumaticCylinderBlockEntity be ? be : null;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        if (level.getBlockEntity(pos) instanceof PneumaticCylinderBlockEntity be) {
            PneumaticCylinderBlockEntity controller = be.getControllerBE();

            if (controller != null) {
                BlockPos inputPos = controller.getShaftInputPos();
                if (inputPos != null) {
                    return inputPos.equals(pos)
                            && face == controller.getBackDirection();
                }
            }
        }

        return state.getValue(HAS_SHAFT)
                && face == state.getValue(FACING).getOpposite();
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        withBlockEntityDo(level, pos, PneumaticCylinderBlockEntity::queueConnectivityUpdate);
    }
}
