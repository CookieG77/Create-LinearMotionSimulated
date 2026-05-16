package net.cookieg.createlinearmotionsimulated.client.ponder;

import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/// Ponder plugin for Create Linear Motion Simulated<br>
/// Required to register our ponders
public class CLMPonderPlugin implements PonderPlugin {

    @Override
    public @NotNull String getModId() {
        return CreateLinearMotionSimulated.ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CLMPonderScenes.register(helper);
    }

    @Override
    public void registerTags(@NotNull PonderTagRegistrationHelper<ResourceLocation> helper) {
        CLMPonderTags.register(helper);
    }
}