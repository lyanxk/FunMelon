package org.lyy.funmelon;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.funmelon.block.MelonVineBlock;
import org.lyy.funmelon.item.MelonSeedsItem;

@Mod(Funmelon.MODID)
public final class Funmelon {
    public static final String MODID = "funmelon";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<MelonVineBlock> MELON_VINE = BLOCKS.register("melon_vine",
            () -> new MelonVineBlock(Blocks.MELON, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                    .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<MelonSeedsItem> MELON_SEEDS = ITEMS.register("melon_seeds",
            () -> new MelonSeedsItem(MELON_VINE.get(), new Item.Properties()));
    public static final Map<String, DeferredBlock<MelonVineBlock>> CULTIVAR_VINES = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<MelonSeedsItem>> CULTIVAR_SEEDS = new LinkedHashMap<>();
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FUNMELON_TAB =
            CREATIVE_MODE_TABS.register("funmelon", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.funmelon"))
                    .icon(() -> MELON_SEEDS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(MELON_SEEDS.get());
                        CULTIVAR_SEEDS.values().forEach(seeds -> output.accept(seeds.get()));
                    })
                    .build());

    public Funmelon(IEventBus modEventBus) {
        if (ModList.get().isLoaded("qa_better_melon")) {
            for (String cultivar : new String[] {"yellow", "unripe", "enchanted", "ender", "glistering"}) {
                registerCultivar(cultivar);
            }
        }
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(Funmelon::protectMelonFarmland);
    }

    // Same scoped protection as Tea the Story; vanilla melon stems are unaffected.
    private static void protectMelonFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel().getBlockState(event.getPos().above()).getBlock() instanceof MelonVineBlock) {
            event.setCanceled(true);
        }
    }

    private static void registerCultivar(String cultivar) {
        ResourceLocation fruit = ResourceLocation.fromNamespaceAndPath("qa_better_melon", cultivar + "_melon_block");
        DeferredBlock<MelonVineBlock> vine = BLOCKS.register(cultivar + "_melon_vine",
                () -> new MelonVineBlock(fruit, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                        .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
        DeferredItem<MelonSeedsItem> seeds = ITEMS.register(cultivar + "_melon_seeds",
                () -> new MelonSeedsItem(vine.get(), new Item.Properties()));
        CULTIVAR_VINES.put(cultivar, vine);
        CULTIVAR_SEEDS.put(cultivar, seeds);
    }
}
