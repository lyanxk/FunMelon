package org.lyy.funmelon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.funmelon.block.MelonVineBlock;

/** Server integration tests, excluded from the distributable mod. */
@GameTestHolder(Funmelon.MODID)
@PrefixGameTestTemplate(false)
public final class MelonGameTests {
    private static final BlockPos CROP = new BlockPos(4, 2, 4);
    private static final List<String> CULTIVARS = List.of("yellow", "unripe", "glistering", "enchanted", "ender");

    private MelonGameTests() {}

    @GameTest(template = "empty")
    public static void independentSeedsAndVanillaPlanting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos vanilla = new BlockPos(2, 2, 2);
        prepareSoil(helper, CROP);
        prepareSoil(helper, vanilla);
        plant(helper, player, CROP, new ItemStack(Funmelon.MELON_SEEDS.get(), 2));
        helper.assertBlockPresent(Funmelon.MELON_VINE.get(), CROP);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Custom seed consumption");
        plant(helper, player, vanilla, new ItemStack(Items.MELON_SEEDS, 2));
        helper.assertBlockPresent(Blocks.MELON_STEM, vanilla);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Vanilla seed consumption");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void customSeedsRequireFarmland(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(CROP.below(), Blocks.DIRT);
        plant(helper, player, CROP, new ItemStack(Funmelon.MELON_SEEDS.get(), 2));
        helper.assertBlockPresent(Blocks.AIR, CROP);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "Failed planting must not consume a seed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativePlantingDoesNotConsumeSeeds(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        // The helper overrides isCreative(), but does not initialize creative abilities.
        // BlockItem consumes through Player.hasInfiniteMaterials(), which reads instabuild.
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        prepareSoil(helper, CROP);
        plant(helper, player, CROP, new ItemStack(Funmelon.MELON_SEEDS.get(), 2));
        helper.assertBlockPresent(Funmelon.MELON_VINE.get(), CROP);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "Creative planting keeps seeds");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seedRecipeIsOneToOneAndShapeless(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(Funmelon.MODID, "melon_seeds"))
                .orElseThrow(() -> new AssertionError("Missing independent seed recipe"));
        helper.assertTrue(holder.value() instanceof ShapelessRecipe, "Seed recipe must be shapeless");
        ShapelessRecipe recipe = (ShapelessRecipe) holder.value();
        for (int slot = 0; slot < 9; slot++) {
            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
            grid.set(slot, new ItemStack(Items.MELON_SEEDS));
            CraftingInput input = CraftingInput.of(3, 3, grid);
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Recipe must accept slot " + slot);
            ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(output.is(Funmelon.MELON_SEEDS.get()) && output.getCount() == 1,
                    "Exactly one custom seed must be produced");
        }
        helper.assertFalse(recipe.matches(CraftingInput.of(2, 1,
                List.of(new ItemStack(Items.MELON_SEEDS), new ItemStack(Items.MELON_SEEDS))), helper.getLevel()),
                "Two occupied ingredient slots must not match the one-seed recipe");
        assertSelectedRecipe(helper, Items.MELON_SLICE, Items.MELON_SEEDS);
        assertSelectedRecipe(helper, Items.MELON_SEEDS, Funmelon.MELON_SEEDS.get());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void optionalCultivarsAndActualCraftingSelection(GameTestHelper helper) {
        boolean loaded = betterMelonExpected(helper);
        helper.assertValueEqual(Funmelon.CULTIVAR_VINES.size(), loaded ? 5 : 0, "Optional vine registration count");
        helper.assertValueEqual(Funmelon.CULTIVAR_SEEDS.size(), loaded ? 5 : 0, "Optional seed registration count");
        for (String cultivar : CULTIVARS) {
            ResourceLocation seedId = id("funmelon", cultivar + "_melon_seeds");
            ResourceLocation vineId = id("funmelon", cultivar + "_melon_vine");
            helper.assertValueEqual(BuiltInRegistries.ITEM.containsKey(seedId), loaded, cultivar + " optional seed registration");
            helper.assertValueEqual(BuiltInRegistries.BLOCK.containsKey(vineId), loaded, cultivar + " optional vine registration");
            ResourceLocation recipeId = switch (cultivar) {
                case "ender" -> id("qa_better_melon", "ender_slice_to_seeds");
                default -> seedId;
            };
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
            helper.assertValueEqual(recipe.isPresent(), loaded, cultivar + " optional recipe loading");
            if (loaded) {
                if (!cultivar.equals("ender")) {
                    helper.assertTrue(recipe.orElseThrow().value() instanceof ShapelessRecipe,
                            cultivar + " seed recipe must be shapeless");
                }
                Item ingredient = cultivar.equals("glistering") ? Items.GLISTERING_MELON_SLICE
                        : requiredItem(helper, "qa_better_melon", cultivar + "_melon_slice");
                Item seeds = requiredItem(helper, cultivar.equals("ender") ? "qa_better_melon" : "funmelon",
                        cultivar + "_melon_seeds");
                // Ask the manager so conflicts with upstream crafting recipes are visible.
                if (cultivar.equals("yellow")) {
                    assertSelectedRecipe(helper, ingredient, Items.MELON_SEEDS);
                    assertSelectedTwoIngredientRecipe(helper, ingredient, Items.MELON_SEEDS, seeds);
                } else {
                    assertSelectedRecipe(helper, ingredient, seeds);
                }
            }
        }
        ResourceLocation migration = id("funmelon", "ender_melon_seeds_from_legacy");
        helper.assertValueEqual(helper.getLevel().getRecipeManager().byKey(migration).isPresent(), loaded,
                "Legacy seed migration is conditional");
        if (loaded) {
            assertSelectedRecipe(helper, requiredItem(helper, "qa_better_melon", "ender_melon_seeds"),
                    requiredItem(helper, "funmelon", "ender_melon_seeds"));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void yellowCultivarLifecycle(GameTestHelper helper) {
        cultivarLifecycle(helper, "yellow");
    }

    @GameTest(template = "empty")
    public static void unripeCultivarLifecycle(GameTestHelper helper) {
        cultivarLifecycle(helper, "unripe");
    }

    @GameTest(template = "empty")
    public static void glisteringCultivarLifecycle(GameTestHelper helper) {
        cultivarLifecycle(helper, "glistering");
    }

    @GameTest(template = "empty")
    public static void enchantedCultivarLifecycle(GameTestHelper helper) {
        cultivarLifecycle(helper, "enchanted");
    }

    @GameTest(template = "empty")
    public static void enderCultivarLifecycle(GameTestHelper helper) {
        cultivarLifecycle(helper, "ender");
    }

    @GameTest(template = "empty")
    public static void emptyHandHarvestRegrowsWithoutDuplicateDrops(GameTestHelper helper) {
        putVine(helper, CROP, 7, 5);
        Player first = helper.makeMockPlayer(GameType.SURVIVAL);
        Player second = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(CROP, first);
        helper.useBlock(CROP, second);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        helper.assertBlockProperty(CROP, MelonVineBlock.DISTANCE, 1);
        helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shearsHarvestConsumesOneDurability(GameTestHelper helper) {
        putVine(helper, CROP, 7, 4);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        helper.useBlock(CROP, player);
        helper.useBlock(CROP, player);
        helper.assertValueEqual(shears.getDamageValue(), 1, "Only successful harvesting damages shears");
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immatureVineCannotBeHarvested(GameTestHelper helper) {
        putVine(helper, CROP, 6, 3);
        helper.useBlock(CROP, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 6);
        helper.assertItemEntityNotPresent(Items.MELON);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heldItemsAndBothHandsDoNotDuplicateHarvest(GameTestHelper helper) {
        putVine(helper, CROP, 7, 4);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.useBlock(CROP, player);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 7);
        helper.assertItemEntityNotPresent(Items.MELON);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.OFF_HAND, shears);
        helper.getBlockState(CROP).useItemOn(shears, helper.getLevel(), player, InteractionHand.OFF_HAND,
                hit(helper.absolutePos(CROP)));
        helper.useBlock(CROP, player);
        helper.getBlockState(CROP).useItemOn(shears, helper.getLevel(), player, InteractionHand.OFF_HAND,
                hit(helper.absolutePos(CROP)));
        helper.assertValueEqual(shears.getDamageValue(), 1, "Offhand shears damage once");
        helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boneMealMaturesWithoutSpreading(GameTestHelper helper) {
        putVine(helper, CROP, 6, 2);
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 2);
        helper.assertTrue(BoneMealItem.applyBonemeal(meal, helper.getLevel(), helper.absolutePos(CROP),
                helper.makeMockPlayer(GameType.SURVIVAL)), "Bone meal must apply to farmland vine");
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 7);
        helper.assertValueEqual(meal.getCount(), 1, "Bone meal consumption");
        assertNoAdjacentVines(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boneMealOnMatureOrNonFarmlandVineConsumesWithoutGrowth(GameTestHelper helper) {
        putVine(helper, CROP, 7, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 3);
        BoneMealItem.applyBonemeal(meal, helper.getLevel(), helper.absolutePos(CROP), player);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 7);
        helper.assertValueEqual(meal.getCount(), 2, "Tea's valid-target behavior consumes meal on mature vines");
        helper.setBlock(CROP.below(), Blocks.DIRT);
        helper.setBlock(CROP, vine(2, 3));
        BoneMealItem.applyBonemeal(meal, helper.getLevel(), helper.absolutePos(CROP), player);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 2);
        helper.assertBlockProperty(CROP, MelonVineBlock.DISTANCE, 3);
        helper.assertValueEqual(meal.getCount(), 1, "Nonfarmland vines do not grow but still consume meal");
        assertNoAdjacentVines(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalGrowthSpreadsOneGeneration(GameTestHelper helper) {
        putVine(helper, CROP, 0, 1);
        helper.setDayTime(6000);
        guaranteedRandomTick(helper);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 1);
        int found = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = CROP.relative(direction);
            if (helper.getBlockState(neighbor).is(Funmelon.MELON_VINE.get())) {
                found++;
                helper.assertBlockProperty(neighbor, MelonVineBlock.AGE, 0);
                helper.assertBlockProperty(neighbor, MelonVineBlock.DISTANCE, 2);
            }
        }
        helper.assertValueEqual(found, 1, "A successful young growth tick spreads to one neighbor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void establishedVinesCanSpreadOntoRigidSupport(GameTestHelper helper) {
        putVine(helper, CROP, 0, 1);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(CROP.relative(direction).below(), Blocks.STONE);
        }
        helper.setDayTime(6000);
        guaranteedRandomTick(helper);
        int children = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = CROP.relative(direction);
            if (helper.getBlockState(neighbor).is(Funmelon.MELON_VINE.get())) {
                children++;
                helper.assertBlockPresent(Blocks.STONE, neighbor.below());
                helper.assertBlockProperty(neighbor, MelonVineBlock.DISTANCE, 2);
            }
        }
        helper.assertValueEqual(children, 1, "Established vines spread onto rigid support without planting seeds there");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matureNeighborInhibitsNaturalGrowth(GameTestHelper helper) {
        putVine(helper, CROP, 3, 1);
        helper.setBlock(CROP.east(), vine(7, 2));
        helper.setDayTime(6000);
        guaranteedRandomTick(helper);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 3);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != Direction.EAST) {
                helper.assertBlockPresent(Blocks.AIR, CROP.relative(direction));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalDistanceAndMaturingTickDoNotSpread(GameTestHelper helper) {
        putVine(helper, CROP, 0, 7);
        helper.setDayTime(6000);
        guaranteedRandomTick(helper);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 1);
        assertNoAdjacentVines(helper);
        helper.setBlock(CROP, vine(6, 1));
        guaranteedRandomTick(helper);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 7);
        assertNoAdjacentVines(helper);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matureBreakingDropsVanillaMelonAndCustomSeeds(GameTestHelper helper) {
        putVine(helper, CROP, 7, 1);
        helper.getLevel().destroyBlock(helper.absolutePos(CROP), true);
        helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        helper.assertItemEntityPresent(Funmelon.MELON_SEEDS.get());
        helper.assertItemEntityNotPresent(Items.MELON_SEEDS);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immatureBreakingReturnsCustomSeed(GameTestHelper helper) {
        putVine(helper, CROP, 3, 1);
        helper.getLevel().destroyBlock(helper.absolutePos(CROP), true);
        helper.assertItemEntityCountIs(Funmelon.MELON_SEEDS.get(), CROP, 2, 1);
        helper.assertItemEntityNotPresent(Items.MELON);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unsupportedMatureVineDropsProduce(GameTestHelper helper) {
        putVine(helper, CROP, 7, 1);
        helper.setBlock(CROP.below(), Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, CROP);
            helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        });
    }

    @GameTest(template = "empty")
    public static void vineProtectsOnlyItsOwnFarmland(GameTestHelper helper) {
        putVine(helper, CROP, 0, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos ground = helper.absolutePos(CROP.below());
        var protectedEvent = new BlockEvent.FarmlandTrampleEvent(helper.getLevel(), ground,
                helper.getLevel().getBlockState(ground), 2, player);
        NeoForge.EVENT_BUS.post(protectedEvent);
        helper.assertTrue(protectedEvent.isCanceled(), "Custom vine must protect its farmland");
        BlockPos bare = helper.absolutePos(CROP.below().east());
        var bareEvent = new BlockEvent.FarmlandTrampleEvent(helper.getLevel(), bare,
                helper.getLevel().getBlockState(bare), 2, player);
        NeoForge.EVENT_BUS.post(bareEvent);
        helper.assertFalse(bareEvent.isCanceled(), "Unrelated farmland must retain vanilla behavior");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void harvestedMelonWorksWithBetterMelonKnife(GameTestHelper helper) {
        boolean loaded = betterMelonExpected(helper);
        if (!loaded) {
            helper.succeed();
            return;
        }
        putVine(helper, CROP, 7, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(CROP, player);
        helper.assertItemEntityCountIs(Items.MELON, CROP, 2, 1);
        var harvested = helper.getEntities(net.minecraft.world.entity.EntityType.ITEM, CROP, 2).stream()
                .filter(entity -> entity.getItem().is(Items.MELON)).findFirst().orElseThrow();
        player.setItemInHand(InteractionHand.MAIN_HAND, harvested.getItem().copy());
        harvested.discard();
        BlockPos placed = new BlockPos(2, 2, 2);
        helper.setBlock(placed.below(), Blocks.STONE);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit(helper.absolutePos(placed.below()))));
        helper.assertBlockPresent(Blocks.MELON, placed);
        Item knife = BuiltInRegistries.ITEM.get(ResourceLocation.parse("qa_better_melon:hq_melon_knife"));
        helper.assertTrue(knife != Items.AIR, "Better Melon knife must exist");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(knife));
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                helper.absolutePos(placed), hit(helper.absolutePos(placed)));
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "Better Melon must handle cutting harvested vanilla melon");
        helper.assertBlockPresent(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("qa_better_melon:melon_stairs")), placed);
        helper.assertItemEntityCountIs(BuiltInRegistries.ITEM.get(ResourceLocation.parse("qa_better_melon:melon_quarter")), placed, 2, 1);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        helper.succeed();
    }

    private static void cultivarLifecycle(GameTestHelper helper, String cultivar) {
        if (!betterMelonExpected(helper)) {
            helper.succeed();
            return;
        }
        Item seeds = requiredItem(helper, "funmelon", cultivar + "_melon_seeds");
        Item fruit = requiredItem(helper, "qa_better_melon", cultivar + "_melon_block");
        Block block = BuiltInRegistries.BLOCK.get(id("funmelon", cultivar + "_melon_vine"));
        helper.assertTrue(block instanceof MelonVineBlock, cultivar + " vine must exist");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(CROP.below(), Blocks.DIRT);
        plant(helper, player, CROP, new ItemStack(seeds, 2));
        helper.assertBlockPresent(Blocks.AIR, CROP);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, cultivar + " planting requires farmland");
        prepareSoil(helper, CROP);
        plant(helper, player, CROP, new ItemStack(seeds, 2));
        helper.assertBlockPresent(block, CROP);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, cultivar + " consumes one seed");
        helper.setDayTime(6000);
        guaranteedRandomTick(helper);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 1);
        int children = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = CROP.relative(direction);
            if (helper.getBlockState(neighbor).is(block)) {
                children++;
                helper.assertBlockProperty(neighbor, MelonVineBlock.AGE, 0);
                helper.assertBlockProperty(neighbor, MelonVineBlock.DISTANCE, 2);
                helper.setBlock(neighbor, Blocks.AIR);
            } else {
                helper.assertBlockPresent(Blocks.AIR, neighbor);
            }
        }
        helper.assertValueEqual(children, 1, cultivar + " spreads its own cultivar");
        helper.getLevel().destroyBlock(helper.absolutePos(CROP), true);
        assertOnlyDrops(helper, seeds, 1, null, 0);
        clearDrops(helper);

        BlockState mature = block.defaultBlockState().setValue(MelonVineBlock.AGE, 7).setValue(MelonVineBlock.DISTANCE, 6);
        helper.setBlock(CROP, mature);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(CROP, player);
        helper.useBlock(CROP, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertBlockPresent(block, CROP);
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        helper.assertBlockProperty(CROP, MelonVineBlock.DISTANCE, 1);
        assertOnlyDrops(helper, fruit, 1, null, 0);
        clearDrops(helper);

        helper.setBlock(CROP, mature);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        helper.useBlock(CROP, player);
        helper.useBlock(CROP, player);
        helper.assertValueEqual(shears.getDamageValue(), 1, cultivar + " shear durability");
        helper.assertBlockProperty(CROP, MelonVineBlock.AGE, 0);
        assertOnlyDrops(helper, fruit, 1, null, 0);
        clearDrops(helper);

        helper.setBlock(CROP, mature);
        helper.getLevel().destroyBlock(helper.absolutePos(CROP), true);
        // Without fortune, mature seed loot may contain 1..4 seeds. The fruit is exact.
        helper.assertItemEntityCountIs(fruit, CROP, 2, 1);
        int seedCount = helper.getEntities(EntityType.ITEM, CROP, 2).stream()
                .filter(entity -> entity.getItem().is(seeds)).mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(seedCount >= 1 && seedCount <= 4, cultivar + " mature custom seed loot");
        assertOnlyDrops(helper, fruit, 1, seeds, seedCount);
        helper.succeed();
    }

    private static void assertOnlyDrops(GameTestHelper helper, Item first, int firstCount, Item second, int secondCount) {
        helper.assertItemEntityCountIs(first, CROP, 2, firstCount);
        if (second != null) {
            helper.assertItemEntityCountIs(second, CROP, 2, secondCount);
        }
        for (var entity : helper.getEntities(EntityType.ITEM, CROP, 2)) {
            helper.assertTrue(entity.getItem().is(first) || (second != null && entity.getItem().is(second)),
                    "Unexpected cultivar drop: " + entity.getItem());
        }
    }

    private static void clearDrops(GameTestHelper helper) {
        helper.getEntities(EntityType.ITEM, CROP, 2).forEach(entity -> entity.discard());
    }

    private static boolean betterMelonExpected(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded("qa_better_melon");
        helper.assertValueEqual(loaded, Boolean.getBoolean("funmelon.test.expectBetterMelon"),
                "Better Melon must match this test run's requested dependency state");
        return loaded;
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static Item requiredItem(GameTestHelper helper, String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(id(namespace, path));
        helper.assertTrue(item != Items.AIR, "Missing item " + namespace + ":" + path);
        return item;
    }

    private static void assertSelectedRecipe(GameTestHelper helper, Item ingredient, Item expected) {
        for (int slot = 0; slot < 9; slot++) {
            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
            grid.set(slot, new ItemStack(ingredient));
            CraftingInput input = CraftingInput.of(3, 3, grid);
            assertSelectedOutput(helper, input, expected);
        }
    }

    private static void assertSelectedTwoIngredientRecipe(GameTestHelper helper, Item first, Item second, Item expected) {
        for (int firstSlot = 0; firstSlot < 9; firstSlot++) {
            for (int secondSlot = 0; secondSlot < 9; secondSlot++) {
                if (firstSlot == secondSlot) {
                    continue;
                }
                List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
                grid.set(firstSlot, new ItemStack(first));
                grid.set(secondSlot, new ItemStack(second));
                assertSelectedOutput(helper, CraftingInput.of(3, 3, grid), expected);
            }
        }
    }

    private static void assertSelectedOutput(GameTestHelper helper, CraftingInput input, Item expected) {
        var selected = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(selected.isPresent(), "No crafting recipe for " + input.items());
        ItemStack result = selected.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(expected) && result.getCount() == 1,
                "Actual selected recipe " + selected.orElseThrow().id() + " must produce one " + expected + ", got " + result);
    }

    private static void prepareSoil(GameTestHelper helper, BlockPos crop) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(crop.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            }
        }
    }

    private static void plant(GameTestHelper helper, Player player, BlockPos crop, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        seeds.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper.absolutePos(crop.below()))));
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
    }

    private static BlockState vine(int age, int distance) {
        return Funmelon.MELON_VINE.get().defaultBlockState()
                .setValue(MelonVineBlock.AGE, age).setValue(MelonVineBlock.DISTANCE, distance);
    }

    private static void putVine(GameTestHelper helper, BlockPos crop, int age, int distance) {
        prepareSoil(helper, crop);
        helper.setBlock(crop, vine(age, distance));
    }

    private static void guaranteedRandomTick(GameTestHelper helper) {
        helper.getBlockState(CROP).randomTick(helper.getLevel(), helper.absolutePos(CROP), new LegacyRandomSource(0) {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        });
    }

    private static void assertNoAdjacentVines(GameTestHelper helper) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.assertBlockPresent(Blocks.AIR, CROP.relative(direction));
        }
    }
}
