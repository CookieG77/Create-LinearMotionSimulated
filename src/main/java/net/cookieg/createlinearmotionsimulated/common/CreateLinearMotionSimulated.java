package net.cookieg.createlinearmotionsimulated.common;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.PartialModelRegistriesCLM;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateLinearMotionSimulated.ID)
public class CreateLinearMotionSimulated {
    public static final String ID = "create_linear_motion_simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final NonNullSupplier<Registrate> REGISTRATE = NonNullSupplier.lazy(() ->
            Registrate.create(ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
    );

    public CreateLinearMotionSimulated(IEventBus modEventBus, ModContainer modContainer) {
        BlockRegistriesCLM.register(modEventBus);
        BlockEntityRegistriesCLM.register(modEventBus);

        REGISTRATE.get().registerEventListeners(modEventBus);
    }

    public static Registrate getRegistrate() {
        return REGISTRATE.get();
    }

    public static ResourceLocation path(final String path) {
        return ResourceLocation.tryBuild(ID, path);
    }
}
