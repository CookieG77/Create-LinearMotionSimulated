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

    /*
     * Root/default body models.
     * These are also the no-shaft visual variant when HAS_SHAFT is false.
     */
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

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_SHAFT_HOLE =
            block("pneumatic_cylinder/block_single_wo_shaft_hole");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD_WO_SHAFT_HOLE =
            block("pneumatic_cylinder/block_single_wo_head_wo_shaft_hole");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM_WO_SHAFT_HOLE =
            block("pneumatic_cylinder/block_bottom_wo_shaft_hole");

    /*
     * Unpowered shaft-driven body models.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_single_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_single_wo_head_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_SHAFT_HOLE_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_single_wo_shaft_hole_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD_WO_SHAFT_HOLE_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_single_wo_head_wo_shaft_hole_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_bottom_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM_WO_SHAFT_HOLE_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_bottom_wo_shaft_hole_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_top_unpowered");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP_WO_HEAD_UNPOWERED =
            block("pneumatic_cylinder/unpowered/block_top_wo_head_unpowered");

    /*
     * Powered shaft-driven body models.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_POWERED =
            block("pneumatic_cylinder/powered/block_single_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD_POWERED =
            block("pneumatic_cylinder/powered/block_single_wo_head_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_SHAFT_HOLE_POWERED =
            block("pneumatic_cylinder/powered/block_single_wo_shaft_hole_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_SINGLE_WO_HEAD_WO_SHAFT_HOLE_POWERED =
            block("pneumatic_cylinder/powered/block_single_wo_head_wo_shaft_hole_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM_POWERED =
            block("pneumatic_cylinder/powered/block_bottom_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_BOTTOM_WO_SHAFT_HOLE_POWERED =
            block("pneumatic_cylinder/powered/block_bottom_wo_shaft_hole_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP_POWERED =
            block("pneumatic_cylinder/powered/block_top_powered");

    public static final PartialModel PNEUMATIC_CYLINDER_TOP_WO_HEAD_POWERED =
            block("pneumatic_cylinder/powered/block_top_wo_head_powered");

    /**
     * The separated piston head / link block.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_PISTON_HEAD =
            block("pneumatic_cylinder/head/head");

    /**
     * Front half of one rod block.
     * Model file: block_extended_0.json
     * Default model orientation: Y axis, y = 8..16.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_ROD_FRONT_HALF =
            block("pneumatic_cylinder/link_block/block_extended_0");

    /**
     * Back half of one rod block.
     * Model file: block_extended_1.json
     * Default model orientation: Y axis, y = 0..8.
     */
    public static final PartialModel PNEUMATIC_CYLINDER_ROD_BACK_HALF =
            block("pneumatic_cylinder/link_block/block_extended_1");

    private static PartialModel block(String path) {
        return PartialModel.of(CreateLinearMotionSimulated.path("block/" + path));
    }

    public static void register() {
        // Force classloading of static partial model entries.
    }
}
