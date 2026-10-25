/*
 * Adapted from Tea the Story: Recount, MelonVineBlock.java (BSD-3-Clause).
 * Authors: joe_vettek (jianzoushihu), RoitoLeonine, Gloridifice, MalayP.
 * Source commit: cf0ffb99aee08a139d4272c3776d3ab9161fb7e1
 * https://github.com/TeamTeaMC-Misc/Tea-the-Story
 * See META-INF/licenses/tea-the-story-code.txt for attribution and license.
 */
package org.lyy.funmelon.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriState;

public class MelonVineBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<MelonVineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("melon").forGetter(vine -> vine.melonId), propertiesCodec())
            .apply(instance, MelonVineBlock::new));
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape SHAPE_MELON = Block.box(0, 0, 0, 16, 13, 16);
    private final ResourceLocation melonId;

    public MelonVineBlock(Block melon, Properties properties) {
        this(BuiltInRegistries.BLOCK.getKey(melon), properties);
    }

    public MelonVineBlock(ResourceLocation melonId, Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AGE, 0).setValue(DISTANCE, 1));
        this.melonId = melonId;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 7 ? SHAPE_MELON : SHAPE;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Tags.Items.TOOLS_SHEAR) && state.getValue(AGE) == 7) {
            if (!level.isClientSide()) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                harvest(level, pos, player, true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // This fallback can also be called while holding an item. Require an actually empty hand.
        if (state.getValue(AGE) == 7 && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide()) {
                harvest(level, pos, player, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    private void harvest(Level level, BlockPos pos, Player player, boolean sheared) {
        // Tea resets both age and propagation distance when harvesting.
        level.setBlockAndUpdate(pos, defaultBlockState());
        // Resolve optional Better Melon fruit after all block registries are populated.
        Block.popResource(level, pos, new ItemStack(BuiltInRegistries.BLOCK.get(melonId)));
        level.playSound(null, pos, sheared ? SoundEvents.SHEEP_SHEAR : SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, defaultBlockState()));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.getBlock() instanceof FarmBlock || canSupportRigidBlock(level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || !isValidBonemealTarget(level, pos, state) || hasNearMelon(level, pos, this)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) >= 9) {
            float chance = getGrowthChance(state, level, pos);
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / chance) + 1) == 0)) {
                int age = state.getValue(AGE);
                if (age + 1 < 7) {
                    level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
                    if (state.getValue(DISTANCE) < 7) {
                        BlockPos nextPos = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
                        if (level.getBlockState(nextPos).isAir()
                                && mayPlaceOn(level.getBlockState(nextPos.below()), level, nextPos.below())) {
                            level.setBlockAndUpdate(nextPos, defaultBlockState().setValue(DISTANCE, state.getValue(DISTANCE) + 1));
                        }
                    }
                } else {
                    level.setBlock(pos, state.setValue(AGE, 7), Block.UPDATE_CLIENTS);
                }
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
    }

    public static boolean hasNearMelon(ServerLevel level, BlockPos pos, Block melon) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(melon) && neighbor.getValue(AGE) == 7) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 7 && level.getBlockState(pos.below()).getBlock() instanceof FarmBlock;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(7, state.getValue(AGE) + Mth.nextInt(level.getRandom(), 2, 5));
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(AGE, DISTANCE));
    }

    protected static float getGrowthChance(BlockState state, ServerLevel level, BlockPos pos) {
        float chance = 1.0F;
        BlockPos soilPos = pos.below();
        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                float soilChance = 0.0F;
                BlockState soil = level.getBlockState(soilPos.offset(x, 0, z));
                if (soil.canSustainPlant(level, soilPos.offset(x, 0, z), Direction.UP, state) != TriState.FALSE) {
                    soilChance = 1.0F;
                    if (soil.isFertile(level, pos.offset(x, 0, z))) {
                        soilChance = 3.0F;
                    }
                }
                if (x != 0 || z != 0) {
                    soilChance /= 4.0F;
                }
                chance += soilChance;
            }
        }
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        boolean horizontal = state.getBlock() == level.getBlockState(west).getBlock()
                || state.getBlock() == level.getBlockState(east).getBlock();
        boolean vertical = state.getBlock() == level.getBlockState(north).getBlock()
                || state.getBlock() == level.getBlockState(south).getBlock();
        if (horizontal && vertical) {
            chance /= 2.0F;
        } else if (state.getBlock() == level.getBlockState(west.north()).getBlock()
                || state.getBlock() == level.getBlockState(east.north()).getBlock()
                || state.getBlock() == level.getBlockState(east.south()).getBlock()
                || state.getBlock() == level.getBlockState(west.south()).getBlock()) {
            chance /= 2.0F;
        }
        return chance;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(asItem());
    }
}
