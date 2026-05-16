package net.cookieg.createlinearmotionsimulated.common.registries;

import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.cookieg.createlinearmotionsimulated.common.content.items.block_items.PneumaticCylinderBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistriesCLM {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateLinearMotionSimulated.ID);


    public static final DeferredHolder<Item, PneumaticCylinderBlockItem> PNEUMATIC_CYLINDER_ITEM =
            ITEMS.register("pneumatic_cylinder",
                    () -> new PneumaticCylinderBlockItem(
                            BlockRegistriesCLM.PNEUMATIC_CYLINDER.get(),
                            new Item.Properties()
                    ));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
