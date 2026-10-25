package org.lyy.funmelon.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lyy.funmelon.Funmelon;
import org.lyy.funmelon.block.MelonVineBlock;
import org.slf4j.Logger;

/** Opt-in client resource integration check; compiled only in the development test source set. */
@EventBusSubscriber(modid = Funmelon.MODID, value = Dist.CLIENT)
public final class ClientAssetSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean completed;
    private static int settledTicks;

    private ClientAssetSmoke() {}

    @SubscribeEvent
    public static void afterClientTick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("funmelon.clientSmoke") || completed) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null || minecraft.screen == null) {
            settledTicks = 0;
            return;
        }
        if (++settledTicks < 10) {
            return;
        }
        completed = true;
        try {
            runChecks(minecraft);
        } catch (Exception | AssertionError failure) {
            LOGGER.error("FUNMELON_CLIENT_SMOKE FAIL", failure);
        } finally {
            // Use normal client cleanup; the launcher must inspect the PASS/FAIL log marker.
            minecraft.stop();
        }
    }

    private static void runChecks(Minecraft minecraft) throws IOException {
        boolean loaded = ModList.get().isLoaded("qa_better_melon");
        require(loaded == Boolean.getBoolean("funmelon.test.expectBetterMelon"), "Unexpected Better Melon dependency state");
        require(Funmelon.CULTIVAR_VINES.size() == (loaded ? 5 : 0), "Unexpected optional vine count");
        Map<Block, Item> crops = new LinkedHashMap<>();
        crops.put(Funmelon.MELON_VINE.get(), Funmelon.MELON_SEEDS.get());
        Funmelon.CULTIVAR_VINES.forEach((name, vine) -> crops.put(vine.get(), Funmelon.CULTIVAR_SEEDS.get(name).get()));
        int creativeEntries = checkCreativeTab(crops, loaded);
        ModelManager manager = minecraft.getModelManager();
        Set<ResourceLocation> sprites = new HashSet<>();
        int totalStates = 0;
        int totalQuads = 0;
        int animatedSprites;
        for (var entry : crops.entrySet()) {
            Block block = entry.getKey();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            List<Long> choices = fruitSelectionSeeds(minecraft, blockId);
            int states = 0;
            int quads = 0;
            int immatureQuads = -1;
            int matureQuads = -1;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel model = manager.getBlockModelShaper().getBlockModel(state);
                require(model != null && model != manager.getMissingModel(), "Missing block model for " + state);
                checkSprite(model.getParticleIcon(ModelData.EMPTY), sprites, state.toString());
                for (long choice : choices) {
                    int count = checkQuads(model, state, choice, sprites);
                    require(count > 0, "Empty block model for " + state);
                    if (state.getValue(MelonVineBlock.AGE) == 7) {
                        matureQuads = count;
                    } else {
                        immatureQuads = count;
                    }
                    quads += count;
                }
                states++;
            }
            require(states == 56, "Expected all 8 ages x 7 distances for " + blockId);
            require(matureQuads > immatureQuads, "Mature fruit geometry missing for " + blockId);
            Item seed = entry.getValue();
            BakedModel itemModel = minecraft.getItemRenderer().getModel(new ItemStack(seed), null, null, 0);
            require(itemModel != null && itemModel != manager.getMissingModel(), "Missing seed item model for " + seed);
            checkSprite(itemModel.getParticleIcon(ModelData.EMPTY), sprites, seed.toString());
            int itemQuads = checkQuads(itemModel, null, 0, sprites);
            require(itemQuads > 0, "Empty seed item model for " + seed);
            LOGGER.info("FUNMELON_CLIENT_SMOKE crop={} states={} fruitChoices={} immatureQuads={} matureQuads={} seedQuads={} checkedQuads={}",
                    blockId, states, choices.size(), immatureQuads, matureQuads, itemQuads, quads + itemQuads);
            totalStates += states;
            totalQuads += quads + itemQuads;
        }
        animatedSprites = (int) sprites.stream().filter(id -> manager.getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                .getSprite(id).contents().getUniqueFrames().count() > 1).count();
        if (loaded) {
            for (String face : List.of("side", "top")) {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Funmelon.MODID, "block/enchanted_watermelon_" + face);
                var contents = manager.getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).getSprite(texture).contents();
                require(sprites.contains(texture), "Enchanted fruit does not use " + texture);
                require(contents.getUniqueFrames().count() == 20 && contents.width() == 16 && contents.height() == 16,
                        "Enchanted fruit animation must contain twenty 16x16 frames: " + texture);
            }
        }
        LOGGER.info("FUNMELON_CLIENT_SMOKE PASS crops={} blockStates={} seedModels={} uniqueSprites={} animatedSprites={} checkedQuads={} creativeEntries={} screen={}",
                crops.size(), totalStates, crops.size(), sprites.size(), animatedSprites, totalQuads,
                creativeEntries, minecraft.screen.getClass().getSimpleName());
    }

    private static int checkCreativeTab(Map<Block, Item> crops, boolean betterMelonLoaded) {
        CreativeModeTab tab = Funmelon.FUNMELON_TAB.get();
        require(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)
                .equals(ResourceLocation.fromNamespaceAndPath(Funmelon.MODID, "funmelon")),
                "FunMelon creative tab is not registered under its own ID");
        require(tab.getIconItem().is(Funmelon.MELON_SEEDS.get()), "Unexpected FunMelon tab icon");
        require(!tab.getDisplayName().getString().equals("itemGroup.funmelon"), "Missing creative tab title translation");
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        List<Item> entries = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        List<Item> expected = new ArrayList<>(crops.values());
        require(entries.equals(expected), "Creative tab seeds differ from registered crops: " + entries);
        require(entries.size() == (betterMelonLoaded ? 6 : 1), "Unexpected creative seed count");
        require(tab.shouldDisplay(), "FunMelon creative tab is hidden");
        require(tab.getSearchTabDisplayItems().stream().map(ItemStack::getItem).toList().equals(expected),
                "FunMelon seeds are missing from creative search");
        return entries.size();
    }

    private static int checkQuads(BakedModel model, BlockState state, long choice, Set<ResourceLocation> sprites) {
        int count = 0;
        int leafQuads = 0;
        boolean ordinaryVine = state != null && state.is(Funmelon.MELON_VINE.get());
        ResourceLocation leafTexture = state == null ? null : ResourceLocation.fromNamespaceAndPath(Funmelon.MODID,
                "block/" + (ordinaryVine ? "watermelon_leaves"
                        : BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace("_melon_vine", "_watermelon_leaves")));
        // Multipart seeds its weighted child from nextLong(). Reset this value for each face,
        // as the renderer does, so all seven face queries select the same fruit variant.
        for (int face = -1; face < Direction.values().length; face++) {
            Direction side = face < 0 ? null : Direction.values()[face];
            RandomSource random = new LegacyRandomSource(0) {
                @Override
                public long nextLong() {
                    return choice;
                }
            };
            for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                checkSprite(quad.getSprite(), sprites, String.valueOf(state));
                if (quad.getSprite().contents().name().equals(leafTexture)) {
                    require(ordinaryVine ? quad.getTintIndex() == 0 : !quad.isTinted(),
                            "Unexpected leaf tint for " + state + ": " + quad.getTintIndex());
                    leafQuads++;
                }
                count++;
            }
        }
        require(state == null || leafQuads > 0, "Missing cultivar leaf texture in " + state + ": " + leafTexture);
        return count;
    }

    private static void checkSprite(TextureAtlasSprite sprite, Set<ResourceLocation> sprites, String context) {
        require(sprite != null && !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()),
                "Missing texture in " + context);
        sprites.add(sprite.contents().name());
    }

    private static List<Long> fruitSelectionSeeds(Minecraft minecraft, ResourceLocation blockId) throws IOException {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json");
        int totalWeight = 0;
        try (Reader reader = minecraft.getResourceManager().getResourceOrThrow(path).openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement part : json.getAsJsonArray("multipart")) {
                JsonElement apply = part.getAsJsonObject().get("apply");
                if (apply.isJsonArray()) {
                    require(totalWeight == 0, "Smoke test expects one weighted fruit selector in " + path);
                    for (JsonElement variant : apply.getAsJsonArray()) {
                        JsonObject object = variant.getAsJsonObject();
                        totalWeight += object.has("weight") ? object.get("weight").getAsInt() : 1;
                    }
                }
            }
        }
        require(totalWeight > 0 && totalWeight <= 256, "Unexpected fruit model selection count in " + path);
        // WeightedBakedModel selects abs((int) random.nextLong()) % totalWeight.
        // Find a multipart seed for every weight ticket, guaranteeing every declared
        // rotated fruit model is inspected instead of relying on random sampling.
        List<Long> seeds = new ArrayList<>();
        Set<Integer> covered = new HashSet<>();
        for (long candidate = 0; covered.size() < totalWeight && candidate < 100000; candidate++) {
            int ticket = Math.abs((int) RandomSource.create(candidate).nextLong()) % totalWeight;
            if (ticket >= 0 && covered.add(ticket)) {
                seeds.add(candidate);
            }
        }
        require(covered.size() == totalWeight, "Could not cover weighted fruit models in " + path);
        return seeds;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
