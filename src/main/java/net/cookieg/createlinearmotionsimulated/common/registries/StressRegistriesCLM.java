package net.cookieg.createlinearmotionsimulated.common.registries;

import com.simibubi.create.api.stress.BlockStressValues;

public class StressRegistriesCLM {

    public static final double PNEUMATIC_CYLINDER_STRESS_IMPACT = 4.0;

    public static void register() {
        BlockStressValues.IMPACTS.register(
                BlockRegistriesCLM.PNEUMATIC_CYLINDER.get(),
                () -> PNEUMATIC_CYLINDER_STRESS_IMPACT
        );
    }
}