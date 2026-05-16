package net.cookieg.createlinearmotionsimulated.client.ponder;

import dev.simulated_team.simulated.index.SimPonderTags;
import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CLMPonderTags {

    public static final ResourceLocation LINEAR_MOTION =
            CreateLinearMotionSimulated.path("linear_motion");

    /// Create ponder group (tag)
    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.addToTag(SimPonderTags.PHYSICS_BEHAVIOR)
                .add(CreateLinearMotionSimulated.path("pneumatic_cylinder"));
    }
}