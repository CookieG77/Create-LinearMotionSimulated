package net.cookieg.createlinearmotionsimulated.common.registries;

import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.minecraft.resources.ResourceLocation;

public class SimulatedCreativeTabIntegrationCLM {

    public static final ResourceLocation LINEAR_MOTION_SECTION =
            CreateLinearMotionSimulated.path("linear_motion");

    private static final SimulatedRegistrate SIMULATED_TAB_HELPER =
            new SimulatedRegistrate(LINEAR_MOTION_SECTION, CreateLinearMotionSimulated.ID);

    public static void registerItems() {
        SIMULATED_TAB_HELPER.addExtraItem(CreateLinearMotionSimulated.path("pneumatic_cylinder"));

    }
}