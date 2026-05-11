package net.cookieg.createlinearmotionsimulated.common.registries;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.cookieg.createlinearmotionsimulated.common.CreateLinearMotionSimulated;

/**
 * Partial models used by BlockEntityRenderers / Flywheel visuals.
 *
 * Paths resolve under:
 * assets/create_linear_motion_simulated/models/block/...
 */
public class PartialModelRegistriesCLM {

    // ---------------------------------------------------------------------
    // Pneumatic Cylinder - static body models
    // ---------------------------------------------------------------------
    // Mostly rendered by blockstates, but registered here for future BER/Flywheel use.

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE =
            block("pneumatic_cylinder/block_single");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD =
            block("pneumatic_cylinder/block_single_wo_head");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM =
            block("pneumatic_cylinder/block_bottom");

    public static final PartialModel PNEUMATIC_CYLINDER_MIDDLE =
            block("pneumatic_cylinder/block_middle");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP =
            block("pneumatic_cylinder/block_top");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP_WO_HEAD =
            block("pneumatic_cylinder/block_top_wo_head");

    // ---------------------------------------------------------------------
    // Pneumatic Cylinder - mobile head / rod partials
    // ---------------------------------------------------------------------
    // Expected files:
    //
    // assets/create_linear_motion_simulated/models/block/pneumatic_cylinder/piston_head/block.json
    // assets/create_linear_motion_simulated/models/block/pneumatic_cylinder/piston_head/block_extended_0.json
    // assets/create_linear_motion_simulated/models/block/pneumatic_cylinder/piston_head/block_extended_1.json

    /**
     * The separated piston head / link block.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_PISTON_HEAD =
            block("pneumatic_cylinder/piston_head/block");

    /**
     * Front half of one rod block.
     * Model file: block_extended_0.json
     * Default model orientation: Y axis, y = 8..16.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_ROD_FRONT_HALF =
            block("pneumatic_cylinder/piston_head/block_extended_0");

    /**
     * Back half of one rod block.
     * Model file: block_extended_1.json
     * Default model orientation: Y axis, y = 0..8.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_ROD_BACK_HALF =
            block("pneumatic_cylinder/piston_head/block_extended_1");

    private static PartialModel block(String path) {
        return PartialModel.of(CreateLinearMotionSimulated.path("block/" + path));
    }

    public static void register() {
        // Force classloading of static partial model entries.
    }
}