package net.cookieg.createlinearmotionsimulated.common.registries;

import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlock;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistriesCLM {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateLinearMotionSimulated.ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateLinearMotionSimulated.ID);

    public static final DeferredBlock<PneumaticCylinderBlock> PNEUMATIC_CYLINDER =
            BLOCKS.registerBlock(
                    "pneumatic_cylinder",
                    PneumaticCylinderBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(5.0f)
                            .noOcclusion()
            );

    public static final DeferredBlock<PneumaticCylinderPistonHeadBlock> PNEUMATIC_CYLINDER_PISTON_HEAD =
            BLOCKS.registerBlock(
                    "pneumatic_cylinder_piston_head",
                    PneumaticCylinderPistonHeadBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(5.0f)
                            .noOcclusion()
            );

    public static final DeferredHolder<Item, BlockItem> PNEUMATIC_CYLINDER_ITEM =
            ITEMS.register("pneumatic_cylinder",
                    () -> new BlockItem(PNEUMATIC_CYLINDER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> PNEUMATIC_CYLINDER_PISTON_HEAD_ITEM =
            ITEMS.register("pneumatic_cylinder_piston_head",
                    () -> new BlockItem(PNEUMATIC_CYLINDER_PISTON_HEAD.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}