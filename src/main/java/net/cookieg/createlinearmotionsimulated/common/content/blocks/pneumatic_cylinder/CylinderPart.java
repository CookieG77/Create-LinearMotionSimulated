package net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Visual/logical position of a PneumaticCylinderBlock inside the static cylinder body.
 * <p>
 * This is deliberately independent of ASSEMBLED:
 * - PART describes where this block is in the tube multiblock.
 * - ASSEMBLED describes whether the mobile piston head has been extracted into a Sable SubLevel.
 */
public enum CylinderPart implements StringRepresentable {
    /// only block: Create mechanical input and rod/head output side
    SINGLE("single"),
    /// rear side: Create mechanical input side
    BACK("back"),
    /// middle side(s): purely visual, no input or output side
    MIDDLE("middle"),
    /// front side: rod/head output side
    FRONT("front");

    private final String serializedName;

    CylinderPart(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}
