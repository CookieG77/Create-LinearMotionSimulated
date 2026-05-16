package net.cookieg.createlinearmotionsimulated.client;

import net.cookieg.createlinearmotionsimulated.client.ponder.CLMPonderPlugin;
import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderRenderer;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.PartialModelRegistriesCLM;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(
    value = CreateLinearMotionSimulated.ID,
    dist = Dist.CLIENT
)
@EventBusSubscriber(
    modid = CreateLinearMotionSimulated.ID,
    value = Dist.CLIENT
)
public class CreateLinearMotionSimulatedClient {

    // Required for the mod configs to appear in the mods option menu
    public CreateLinearMotionSimulatedClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PartialModelRegistriesCLM.register();
        PonderIndex.addPlugin(new CLMPonderPlugin());
    }

    /// Registering custom block entity renderers
    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER.get(),
                PneumaticCylinderRenderer::new
        );
    }
}
