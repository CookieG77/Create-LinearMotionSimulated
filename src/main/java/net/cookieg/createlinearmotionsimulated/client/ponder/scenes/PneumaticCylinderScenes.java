package net.cookieg.createlinearmotionsimulated.client.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.simulated_team.simulated.index.SimItems;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.CylinderPart;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlock;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.PneumaticCylinderBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.link_block.PneumaticCylinderPistonHeadBlock;
import net.cookieg.createlinearmotionsimulated.common.content.blocks.pneumatic_cylinder.rod.PneumaticCylinderRodSegmentBlockEntity;
import net.cookieg.createlinearmotionsimulated.common.registries.BlockRegistriesCLM;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class PneumaticCylinderScenes {

    public static void setPneumaticCylinderSpeed(final CreateSceneBuilder scene, final SceneBuildingUtil util, final BlockPos pneumaticCylinderPos, final int rpm) {
        scene.world().modifyBlock(pneumaticCylinderPos, s -> s.setValue(PneumaticCylinderBlock.ASSEMBLED, true), false);

        scene.world().modifyBlockEntityNBT(util.select().position(pneumaticCylinderPos), PneumaticCylinderBlockEntity.class, nbt -> {
            nbt.getCompound("").putFloat("Speed", rpm);
        });
    }

    public static void basicUsage(SceneBuilder builder, SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();
        final EffectInstructions effects = scene.effects();

        // Defining areas inside the Ponder
        final BlockPos pneumaticCylinder = new BlockPos(2, 2, 2);
        final BlockPos pneumaticCylinderHead = new BlockPos(2, 4, 2);

        final Selection largeCogs = select.fromTo(2, 1, 2, 5, 1, 3);
        final Selection invertedLargeCogs = select.position(5, 0, 2);
        final Selection smallCogs = select.position(3, 1, 1);
        final Selection cogs = largeCogs.add(invertedLargeCogs).add(smallCogs);

        final Selection sofa = select.fromTo(0, 3, 2, 4, 3, 3);

        final Selection leverPlatform = select.fromTo(0, 1, 2, 1, 1, 2);
        final Selection leverAndRedstone = select.fromTo(0, 2, 2, 1, 2, 2);
        final BlockPos  lever = new BlockPos(0, 2, 2);

        final Vec3 catStartPos = new Vec3(2.5, 3.5, 2.5);
        final Vec2 catStartHeadPos = new Vec2( 0, 180);

        final BlockPos otherPneumaticCylinder = new BlockPos(2, 1, 0);

        // Basic setup
        scene.title("pneumatic_cylinder", "Moving Structures using the Pneumatic Cylinder");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-1f);
        world.showSection(select.position(5, 0, 2), Direction.UP);

        //TODO : Display the shaft part here before the rest

        scene.idle(10);
        world.showSection(cogs, Direction.DOWN);
        scene.idle(10);
        world.showSection(select.position(pneumaticCylinder), Direction.DOWN);

        scene.idle(20);

        overlay.showText(80)
                    .text("Pneumatic Cylinder attach to the block in front of them")
                    .pointAt(vector.topOf(pneumaticCylinder))
                    .colored(PonderPalette.GREEN)
                    .placeNearTarget();

        final AABB bb1 = AABB.unitCubeFromLowerCorner(new Vec3(2, 3, 2));
        overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, bb1, bb1, 90);

        scene.idle(70);

        final ElementLink<WorldSectionElement> contraption =
                scene.world().showIndependentSectionImmediately(select.position(2, 3, 2));

        scene.world().showSectionAndMerge(select.position(2, 3, 2), Direction.DOWN, contraption);

        scene.idle(10);
        scene.effects().superGlue(pneumaticCylinder.above(), Direction.DOWN, true);
        world.showSectionAndMerge(sofa.substract(select.fromTo(2, 3, 2, 2, 4, 2)), Direction.DOWN, contraption);
        final ElementLink<EntityElement> cat = scene.world().createEntity(level -> {
            Cat catEntity = EntityType.CAT.create(level);
            if (catEntity == null)
                return null;

            catEntity.setSilent(true);
            catEntity.setInvulnerable(true);
            catEntity.setPersistenceRequired();
            catEntity.setInSittingPose(true);
            catEntity.setOrderedToSit(true);
            catEntity.setNoAi(true);

            catEntity.setPos(
                catStartPos.x,
                catStartPos.y,
                catStartPos.z
            );
            catEntity.setYRot(180);
            catEntity.setYHeadRot(catStartHeadPos.y);
            catEntity.setXRot(catStartHeadPos.x);

            level.registryAccess()
                    .registryOrThrow(Registries.CAT_VARIANT)
                    .getHolder(ResourceKey.create(
                            Registries.CAT_VARIANT,
                            ResourceLocation.withDefaultNamespace("calico")
                    ))
                    .ifPresent(catEntity::setVariant);

            return catEntity;
        });
        scene.idle(10);

        scene.overlay().showControls(util.vector().centerOf(4, 3, 3), Pointing.RIGHT, 40)
                .withItem(SimItems.HONEY_GLUE.asStack())
                .rightClick();
        scene.idle(5);
        final AABB bb2 = new AABB(util.grid().at(4, 3, 3));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb2, bb2, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, bb2, bb2.expandTowards(-4, 0, -1), 80);

        scene.idle(10);

        overlay.showText(70)
                .text("Use Super Glue or Honey Glue to select a group of blocks")
                .pointAt(vector.centerOf(0, 3, 2))
                .colored(PonderPalette.OUTPUT)
                .attachKeyFrame()
                .placeNearTarget();

        scene.idle(90);

        world.setKineticSpeed(largeCogs, 64);
        world.setKineticSpeed(invertedLargeCogs, -64);
        world.setKineticSpeed(smallCogs, -128);

        setPneumaticCylinderSpeed(scene, util, pneumaticCylinder, 64);

        ElementLink<WorldSectionElement> pistonHead =
                scene.world().showIndependentSectionImmediately(
                        util.select().position(pneumaticCylinderHead)
                );
        world.moveSection(pistonHead, util.vector().of(0, -2, 0), 0);

        scene.idle(10);

        world.showSection(leverPlatform, Direction.UP);

        scene.idle(10);

        world.showSection(leverAndRedstone, Direction.DOWN);

        scene.idle(20);

        world.toggleRedstonePower(leverAndRedstone.add(select.position(pneumaticCylinder)));

        effects.indicateRedstone(lever);

        world.modifyBlock(
                new BlockPos(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ()),
                blockState -> blockState.setValue(PneumaticCylinderBlock.POWERED, true),
                false
        );

        final int duration = 40;
        final Vec3 movement = new Vec3(0, 1, 0);
        final Vec2 targetCatHeadPos = new Vec2(-35, 145);

        overlay.showText(80)
                .text("While powered with rotation, the Pneumatic Cylinder will extend if it receive a redstone signal")
                .pointAt(vector.topOf(pneumaticCylinder.above()))
                .attachKeyFrame()
                .placeNearTarget();


        world.moveSection(contraption, movement, duration);
        world.moveSection(pistonHead, movement, duration);

        // Moving the cat with the rest
        for (int tick = 0; tick < duration; tick++) {
            double t = tick / (double) duration;

            scene.world().modifyEntity(cat, entity -> {
                entity.setPos(
                        catStartPos.x + movement.x * t,
                        catStartPos.y + movement.y * t,
                        catStartPos.z + movement.z * t
                );

                entity.setDeltaMovement(0, 0, 0);

                if (entity instanceof Cat catEntity) {

                    catEntity.setYRot(180);
                    catEntity.setYHeadRot(catStartHeadPos.y);
                    catEntity.setXRot(catStartHeadPos.x);

                    catEntity.setInSittingPose(true);
                    catEntity.setOrderedToSit(true);
                }
            });

            scene.idle(1);
        }

        scene.idle(100); // 120 ticks - 20 (from the cat moving loop)

        final int catHeadRotationDuration = 60;

        boolean textShowed = false;
        for (int tick = 0; tick < catHeadRotationDuration; tick++) {
            double x = easedLerp(catStartHeadPos.x, targetCatHeadPos.x, 0, catHeadRotationDuration, tick);
            double y = easedLerp(catStartHeadPos.y, targetCatHeadPos.y, 0, catHeadRotationDuration, tick);

            scene.world().modifyEntity(cat, entity -> {
                if (entity instanceof Cat catEntity) {

                    catEntity.setYRot(180);
                    catEntity.setYHeadRot((float) y);
                    catEntity.setXRot((float) x);

                    catEntity.setInSittingPose(true);
                    catEntity.setOrderedToSit(true);
                }
            });

            if (tick >= catHeadRotationDuration/2 && !textShowed) {
                textShowed = true;
                overlay.showText(60)
                        .text("I know what you did...")
                        .colored(PonderPalette.RED)
                        .pointAt(catStartPos.add(movement).add(new Vec3(0, 0.25, 0)))
                        .placeNearTarget();
            }

            scene.idle(1);
        }

        scene.idle(40);

        scene.markAsFinished();
    }

    public static void multiBlock(SceneBuilder builder, SceneBuildingUtil util) {
        final CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        final CreateSceneBuilder.WorldInstructions world = scene.world();
        final OverlayInstructions overlay = scene.overlay();
        final SelectionUtil select = util.select();
        final VectorUtil vector = util.vector();
        final EffectInstructions effects = scene.effects();

        // Defining areas inside the Ponder
        final Selection largeCogwheel = select.position(5, 0, 9);
        final Selection smallCogwheelAndShaft = select.fromTo(4, 1, 8, 5, 1, 9);
        final BlockPos pneumaticCylinder = new BlockPos(4, 1, 7);
        final BlockPos pistonHead = new BlockPos(5, 1, 4);
        final int pistonLength = 3; // size of the piston without the head
        final Selection piston = select.fromTo(
                pistonHead.getX(), pistonHead.getY(), pistonHead.getZ(),
                pistonHead.getX(), pistonHead.getY(), pistonHead.getZ() + pistonLength
        );
        final BlockPos lever = new BlockPos(3, 1, 7);

        // Update each segments in the piston to force them to be fully rendered
        for (int i = 0; i < pistonLength; i++) {
            world.modifyBlockEntity(
                    new BlockPos(pistonHead.getX(), pistonHead.getY(), pistonHead.getZ() + i + 1),
                    PneumaticCylinderRodSegmentBlockEntity.class,
                    be -> {
                        be.setIndexBehindHead(1);
                        be.setForceFullRender(true);
                    }
            );
        }

        // Basic setup
        scene.title("extending_pneumatic_cylinder", "Extending a Pneumatic Cylinder");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        world.showSection(largeCogwheel, Direction.SOUTH);

        scene.idle(10);

        world.showSection(smallCogwheelAndShaft, Direction.SOUTH);

        scene.idle(10);

        world.showSection(select.position(pneumaticCylinder), Direction.DOWN);

        scene.idle(10);

        overlay.showText(60)
                .text("Pneumatic Cylinder can be extended to move sub levels further")
                .pointAt(pneumaticCylinder.getCenter())
                .placeNearTarget();

        scene.idle(10);


        for (int i = 0; i < pistonLength; i++) {

            scene.world().setBlocks(
                    select.position(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i - 1),
                    BlockRegistriesCLM.PNEUMATIC_CYLINDER.get()
                            .defaultBlockState()
                            .setValue(PneumaticCylinderBlock.FACING, Direction.NORTH)
                            .setValue(PneumaticCylinderBlock.HAS_SHAFT, true),
                    false
            );

            world.showSection(select.position(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i - 1), Direction.DOWN);

            scene.idle(10);

            // update blocks once placed
            world.modifyBlock( // Current block
                    new BlockPos(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i - 1),
                    blockState -> blockState.setValue(
                            PneumaticCylinderBlock.PART,
                            CylinderPart.FRONT
                        )
                    ,
                    false
            );
            int finalI = i;
            world.modifyBlock( // Previous block
                    new BlockPos(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i),
                    blockState -> blockState.setValue(
                            PneumaticCylinderBlock.PART,
                            (finalI == 0) ? CylinderPart.BACK : CylinderPart.MIDDLE
                        ),
                    false
            );

            scene.idle(5);
        }


        final ElementLink<WorldSectionElement> contraption = scene.world().showIndependentSectionImmediately(piston);
        world.moveSection(contraption, new Vec3(-1, 0, 0), 0);

        // Set the pneumatic cylinder to assembled to hide the head
        for (int i = 0; i < pistonLength+1; i++) {
            world.modifyBlock(
                    new BlockPos(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i),
                    blockState -> blockState.setValue(PneumaticCylinderBlock.ASSEMBLED, true),
                    false
            );
        }

        world.setKineticSpeed(largeCogwheel, -128);
        world.setKineticSpeed(smallCogwheelAndShaft, 256);
        world.setKineticSpeed(select.fromTo(
                pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - (pistonLength),
                pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ()
        ), 256);

        scene.addKeyframe();

        scene.idle(20);

        world.showSection(select.position(lever), Direction.DOWN);

        scene.idle(20);

        world.toggleRedstonePower(select.fromTo(
                pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - (pistonLength),
                pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ()
        ).add(select.position(lever)));
        effects.indicateRedstone(lever);

        // Power the pneumatic cylinder
        for (int i = 0; i < pistonLength+1; i++) {
            world.modifyBlock(
                    new BlockPos(pneumaticCylinder.getX(), pneumaticCylinder.getY(), pneumaticCylinder.getZ() - i),
                    blockState -> blockState.setValue(PneumaticCylinderBlock.POWERED, true),
                    false
            );
        }

        world.moveSection(contraption, new Vec3(0, 0, -(pistonLength + 1)), 40);

        overlay.showText(60)
                .text("The whole piston, including the head, will move together when the cylinder is powered")
                .pointAt(new Vec3(pneumaticCylinder.getX() + 0.5, pneumaticCylinder.getY()+ 0.5, pneumaticCylinder.getZ() + 0.5 - ((double) (pistonLength + 1) / 2)))
                .placeNearTarget();

        scene.idle(80);

        scene.markAsFinished();
    }

    /// Easing methods for animating the cat

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double easeInOut(double t) {
        t = clamp01(t);

        // Smoothstep
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double easedLerp(double a, double b, double min, double max, double value) {
        if (max == min)
            return b;

        double t = (value - min) / (max - min);
        return lerp(a, b, easeInOut(t));
    }
}