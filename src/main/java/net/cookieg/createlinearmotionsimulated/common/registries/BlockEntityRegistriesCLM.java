package net.cookieg.createlinearmotionsimulated.common.registries;

import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityRegistriesCLM {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateLinearMotionSimulated.ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticCylinderBlockEntity>> PNEUMATIC_CYLINDER =
            BLOCK_ENTITY_TYPES.register("pneumatic_cylinder",
                    () -> BlockEntityType.Builder.of(
                            PneumaticCylinderBlockEntity::new,
                            BlockRegistriesCLM.PNEUMATIC_CYLINDER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticCylinderPistonHeadBlockEntity>> PNEUMATIC_CYLINDER_PISTON_HEAD =
            BLOCK_ENTITY_TYPES.register("pneumatic_cylinder_piston_head",
                    () -> BlockEntityType.Builder.of(
                            PneumaticCylinderPistonHeadBlockEntity::new,
                            BlockRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get()
                    ).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}