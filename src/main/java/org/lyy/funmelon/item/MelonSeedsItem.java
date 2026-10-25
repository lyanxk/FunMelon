package org.lyy.funmelon.item;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Planting is restricted to farmland; established vines may spread onto other supports. */
public final class MelonSeedsItem extends ItemNameBlockItem {
    public MelonSeedsItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (!(context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof FarmBlock)) {
            return null;
        }
        return super.getPlacementState(context);
    }
}
