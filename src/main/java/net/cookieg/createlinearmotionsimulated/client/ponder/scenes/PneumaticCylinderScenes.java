package net.cookieg.createlinearmotionsimulated.client.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

public class PneumaticCylinderScenes {

    public static void basicUsage(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pneumatic_cylinder", "Using the Pneumatic Cylinder");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        scene.idle(10);

        scene.world().showSection(util.select().layer(1), Direction.DOWN);

        scene.overlay().showText(80)
                .text("The Pneumatic Cylinder is a linear Create multiblock.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.UP));

        scene.idle(90);

        scene.overlay().showText(90)
                .text("Place multiple cylinder blocks in a straight line to increase its maximum extension.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.NORTH));

        scene.idle(100);

        scene.overlay().showText(90)
                .text("The rear side receives rotational power from Create.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.NORTH));

        scene.idle(100);

        scene.overlay().showText(90)
                .text("When powered by redstone, the piston head assembles and extends forward.")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 4), Direction.SOUTH));

        scene.idle(100);

        scene.markAsFinished();
    }
}