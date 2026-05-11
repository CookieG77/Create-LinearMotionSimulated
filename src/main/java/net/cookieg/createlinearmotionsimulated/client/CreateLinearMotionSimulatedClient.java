package net.cookieg.createlinearmotionsimulated.client;

import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderRenderer;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadRenderer;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockEntityRegistriesCLM;
import net.cookieg.createlinearmotionsimulated.common.registries.PartialModelRegistriesCLM;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


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
    }

    @SubscribeEvent
    static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER.get(),
                PneumaticCylinderRenderer::new
        );

        event.registerBlockEntityRenderer(
                BlockEntityRegistriesCLM.PNEUMATIC_CYLINDER_PISTON_HEAD.get(),
                PneumaticCylinderPistonHeadRenderer::new
        );
    }
}
