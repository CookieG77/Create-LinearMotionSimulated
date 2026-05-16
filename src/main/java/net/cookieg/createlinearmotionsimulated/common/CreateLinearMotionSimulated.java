package net.cookieg.createlinearmotionsimulated.common;

import com.mojang.logging.LogUtils;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.ItemRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.SimulatedCreativeTabIntegrationCLM;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateLinearMotionSimulated.ID)
public class CreateLinearMotionSimulated {
    public static final String ID = "create_linear_motion_simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateLinearMotionSimulated(IEventBus modEventBus, ModContainer modContainer) {
        BlockRegistriesCLM.register(modEventBus);
        BlockEntityRegistriesCLM.register(modEventBus);
        ItemRegistriesCLM.register(modEventBus);

        SimulatedCreativeTabIntegrationCLM.registerItems();
    }

    public static ResourceLocation path(final String path) {
        return ResourceLocation.tryBuild(ID, path);
    }
}
