package net.cookieg.createlinearmotionsimulated.common.content.items.block_items;

import com.simibubi.create.foundation.item.KineticStats;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PneumaticCylinderBlockItem extends BlockItem {

    public PneumaticCylinderBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull Item.TooltipContext context,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        /*
         * Use Create's real kinetic tooltip logic.
         *
         * This gives the same behavior as Create blocks:
         * - without Engineer's Goggles: generic stress category
         * - with Engineer's Goggles: exact "4x RPM"
         */
        if (Minecraft.getInstance().player == null)
            return;

        List<Component> kineticStats = KineticStats.getKineticStats(getBlock(), Minecraft.getInstance().player);

        if (!kineticStats.isEmpty()) {
            tooltip.add(CommonComponents.EMPTY);
            tooltip.addAll(kineticStats);
        }
    }
}