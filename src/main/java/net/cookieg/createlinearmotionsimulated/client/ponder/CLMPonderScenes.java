package net.cookieg.createlinearmotionsimulated.client.ponder;

import net.cookieg.createlinearmotionsimulated.client.ponder.scenes.PneumaticCylinderScenes;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CLMPonderScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(BlockRegistriesCLM.PNEUMATIC_CYLINDER.getId())
                .addStoryBoard(
                        "pneumatic_cylinder",
                        PneumaticCylinderScenes::basicUsage,
                        CLMPonderTags.LINEAR_MOTION
                );
    }
}